/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.openstack.netvirt;

import java.util.List;

import org.opendaylight.neutron.spi.NeutronNetwork;
import org.opendaylight.ovsdb.openstack.netvirt.api.*;
import org.opendaylight.ovsdb.openstack.netvirt.impl.MdsalUtils;
import org.opendaylight.ovsdb.openstack.netvirt.impl.NeutronL3Adapter;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Madhu Venugopal
 * @author Brent Salisbury
 * @author Dave Tucker
 * @author Sam Hague (shague@redhat.com)
 */
public class SouthboundHandler extends AbstractHandler
        implements NodeCacheListener, OvsdbInventoryListener {
    static final Logger logger = LoggerFactory.getLogger(SouthboundHandler.class);

    // The implementation for each of these services is resolved by the OSGi Service Manager
    private volatile ConfigurationService configurationService;
    private volatile BridgeConfigurationManager bridgeConfigurationManager;
    private volatile TenantNetworkManager tenantNetworkManager;
    private volatile NetworkingProviderManager networkingProviderManager;
    private volatile OvsdbConnectionService connectionService;
    private volatile NeutronL3Adapter neutronL3Adapter;

    void start() {
        this.triggerUpdates();
    }

    void init() {
        logger.info(">>>>>> init {}", this.getClass());
    }

    private SouthboundEvent.Type ovsdbTypeToSouthboundEventType(OvsdbType ovsdbType) {
        SouthboundEvent.Type type = SouthboundEvent.Type.NODE;

        switch (ovsdbType) {
            case NODE:
                type = SouthboundEvent.Type.NODE;
                break;
            case BRIDGE:
                type = SouthboundEvent.Type.BRIDGE;
                break;
            case PORT:
                type = SouthboundEvent.Type.PORT;
                break;
            case CONTROLLER:
                type = SouthboundEvent.Type.CONTROLLER;
                break;
            case OPENVSWITCH:
                type = SouthboundEvent.Type.OPENVSWITCH;
                break;
            default:
                logger.warn("Invalid OvsdbType: {}", ovsdbType);
                break;
        }
        return type;
    }

    @Override
    public void ovsdbUpdate(Node node, DataObject resourceAugmentationData, OvsdbType ovsdbType, Action action) {
        logger.info("ovsdbUpdate: {} - {} - {}", node, ovsdbType, action);
        this.enqueueEvent(new SouthboundEvent(node, ovsdbTypeToSouthboundEventType(ovsdbType), action));
    }

    public void processOvsdbNodeUpdate(Node node, Action action) {
        if (action == Action.ADD) {
            logger.info("processOvsdbNodeUpdate {}", node);
            bridgeConfigurationManager.prepareNode(node);
        } else {
            logger.info("Not implemented yet: {}", action);
        }
    }

    private void handleInterfaceUpdate (Node node, OvsdbTerminationPointAugmentation tp) {
        logger.trace("handleInterfaceUpdate node: {}, tp: {}", node, tp);
        NeutronNetwork network = tenantNetworkManager.getTenantNetwork(tp);
        if (network != null && !network.getRouterExternal()) {
            logger.trace("handleInterfaceUpdate node: {}, tp: {}, network: {}", node, tp, network.getNetworkUUID());
            tenantNetworkManager.programInternalVlan(node, tp, network);
            neutronL3Adapter.handleInterfaceEvent(node, tp, network, Action.UPDATE);
            if (bridgeConfigurationManager.createLocalNetwork(node, network)) {
                networkingProviderManager.getProvider(node).handleInterfaceUpdate(network, node, tp);
            }
        } else {
            logger.debug("No tenant network found on node: {} for interface: {}", node, tp);
        }
    }

    private void handleInterfaceDelete (Node node, OvsdbTerminationPointAugmentation intf,
                                        boolean isLastInstanceOnNode, NeutronNetwork network) {
        logger.debug("handleInterfaceDelete: node: {}, isLastInstanceOnNode: {}, interface: {}",
                node, isLastInstanceOnNode, intf);

        neutronL3Adapter.handleInterfaceEvent(node, intf, network, Action.DELETE);
        List<String> phyIfName = bridgeConfigurationManager.getAllPhysicalInterfaceNames(node);
        if (isInterfaceOfInterest(intf, phyIfName)) {
            // delete tunnel or physical interfaces
            networkingProviderManager.getProvider(node).handleInterfaceDelete(network.getProviderNetworkType(),
                    network, node, intf, isLastInstanceOnNode);
        } else if (network != null) {
            // vlan doesn't need a tunnel endpoint
            if (!network.getProviderNetworkType().equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_VLAN)) {
                if (configurationService.getTunnelEndPoint(node) == null) {
                    logger.error("Tunnel end-point configuration missing. Please configure it in OpenVSwitch Table");
                    return;
                }
            }
            if (isLastInstanceOnNode & networkingProviderManager.getProvider(node).hasPerTenantTunneling()) {
                tenantNetworkManager.reclaimInternalVlan(node, network);
            }
            networkingProviderManager.getProvider(node).handleInterfaceDelete(network.getProviderNetworkType(),
                    network, node, intf, isLastInstanceOnNode);
        }
    }

    private void triggerUpdates() {
        List<Node> nodes = connectionService.getNodes();
        if (nodes == null) return;
        for (Node node : nodes) {
            OvsdbBridgeAugmentation bridge = node.getAugmentation(OvsdbBridgeAugmentation.class);
            if (bridge != null) {
                processBridgeUpdate(node, bridge);
            }

            List<TerminationPoint> tps = MdsalUtils.getTerminationPoints(node);
            for (TerminationPoint tp : tps) {
                OvsdbTerminationPointAugmentation port = tp.getAugmentation(OvsdbTerminationPointAugmentation.class);
                if (port != null) {
                    processPortUpdate(node, port);
                }
            }
        }
    }

    private void processPortDelete(Node node, OvsdbTerminationPointAugmentation ovsdbTerminationPointAugmentation,
                                   Object context) {
        logger.debug("processportDelete {}: {}", node, ovsdbTerminationPointAugmentation);
        NeutronNetwork network = null;
        if (context == null) {
            network = tenantNetworkManager.getTenantNetwork(ovsdbTerminationPointAugmentation);
        } else {
            network = (NeutronNetwork)context;
        }
        List<String> phyIfName = bridgeConfigurationManager.getAllPhysicalInterfaceNames(node);
        if (isInterfaceOfInterest(ovsdbTerminationPointAugmentation, phyIfName)) {
            this.handleInterfaceDelete(node, ovsdbTerminationPointAugmentation, false, null);
        } else if (network != null && !network.getRouterExternal()) {
            logger.debug("Network {} : Delete interface {} attached to bridge {}", network.getNetworkUUID(),
                    ovsdbTerminationPointAugmentation.getInterfaceUuid(), node);
            try {
                OvsdbBridgeAugmentation ovsdbBridgeAugmentation = node.getAugmentation(OvsdbBridgeAugmentation.class);
                if (ovsdbBridgeAugmentation != null) {
                    List<TerminationPoint> terminationPoints = node.getTerminationPoint();
                    if(!terminationPoints.isEmpty()){
                        boolean isLastInstanceOnNode = true;
                        for(TerminationPoint terminationPoint : terminationPoints) {
                            OvsdbTerminationPointAugmentation tpAugmentation =
                                    terminationPoint.getAugmentation( OvsdbTerminationPointAugmentation.class);
                            if(tpAugmentation.getInterfaceUuid().equals(ovsdbTerminationPointAugmentation.getInterfaceUuid())) continue;
                            NeutronNetwork neutronNetwork = tenantNetworkManager.getTenantNetwork(tpAugmentation);
                            if (neutronNetwork != null && neutronNetwork.equals(network)) {
                                isLastInstanceOnNode = false;
                                break;
                            }
                        }
                        this.handleInterfaceDelete(node, ovsdbTerminationPointAugmentation, isLastInstanceOnNode, network);
                    }
                }
            } catch (Exception e) {
                logger.error("Error fetching Interface Rows for node " + node, e);
            }
        }
    }

    private boolean isInterfaceOfInterest(OvsdbTerminationPointAugmentation terminationPoint, List<String> phyIfName) {
        return (SouthboundMapper.createOvsdbInterfaceType(
                terminationPoint.getInterfaceType()).equals(NetworkHandler.NETWORK_TYPE_VXLAN)
                ||
                SouthboundMapper.createOvsdbInterfaceType(
                        terminationPoint.getInterfaceType()).equals(NetworkHandler.NETWORK_TYPE_GRE)
                ||
                phyIfName.contains(terminationPoint.getName()));
    }

    /**
     * Notification about an OpenFlow Node
     *
     * @param openFlowNode the {@link Node Node} of interest in the notification
     * @param action the {@link Action}
     * @see NodeCacheListener#notifyNode
     */
    @Override
    public void notifyNode (Node openFlowNode, Action action) {
        logger.info("notifyNode: Node {} update {}", openFlowNode, action);

        if (action.equals(Action.ADD)) {
            networkingProviderManager.getProvider(openFlowNode).initializeOFFlowRules(openFlowNode);
        }
    }

    /**
     * Process the event.
     *
     * @param abstractEvent the {@link org.opendaylight.ovsdb.openstack.netvirt.AbstractEvent} event to be handled.
     * @see EventDispatcher
     */
    @Override
    public void processEvent(AbstractEvent abstractEvent) {
        if (!(abstractEvent instanceof SouthboundEvent)) {
            logger.error("Unable to process abstract event " + abstractEvent);
            return;
        }
        SouthboundEvent ev = (SouthboundEvent) abstractEvent;
        logger.info("processEvent: {}", ev);
        switch (ev.getType()) {
            case NODE:
                processOvsdbNodeUpdate(ev.getNode(), ev.getAction());
                break;
            case BRIDGE:
                processBridgeUpdate(ev.getNode(), ev.getAction());
                break;

            case PORT:
                processPortUpdate(ev.getNode(), ev.getAction());
                break;

            case OPENVSWITCH:
                processOpenVSwitchUpdate(ev.getNode(), ev.getAction());
                break;

            default:
                logger.warn("Unable to process type " + ev.getType() +
                        " action " + ev.getAction() + " for node " + ev.getNode());
                break;
        }
    }

    private void processPortUpdate(Node node, Action action) {
        switch (action) {
            case ADD:
            case UPDATE:
                processPortUpdate(node);
                break;
            case DELETE:
                processPortDelete(node);
                break;
        }
    }

    private void processPortDelete(Node node) {
        List<TerminationPoint> terminationPoints = MdsalUtils.getTerminationPoints(node);
        for (TerminationPoint terminationPoint : terminationPoints) {
            processPortDelete(node, terminationPoint.getAugmentation(OvsdbTerminationPointAugmentation.class), null);
        }
    }

    private void processPortUpdate(Node node) {
        List<TerminationPoint> terminationPoints = MdsalUtils.getTerminationPoints(node);
        for (TerminationPoint terminationPoint : terminationPoints) {
            processPortUpdate(node, terminationPoint.getAugmentation(OvsdbTerminationPointAugmentation.class));
        }
    }

    private void processPortUpdate(Node node, OvsdbTerminationPointAugmentation port) {
        logger.debug("processPortUpdate {} - {}", node, port);
        NeutronNetwork network = tenantNetworkManager.getTenantNetwork(port);
        if (network != null && !network.getRouterExternal()) {
            this.handleInterfaceUpdate(node, port);
        }

    }

    private void processOpenVSwitchUpdate(Node node, Action action) {
        switch (action) {
            case ADD:
            case UPDATE:
                processOpenVSwitchUpdate(node);
                break;
            case DELETE:
                break;
        }
    }

    private void processOpenVSwitchUpdate(Node node) {
        // TODO this node might be the OvsdbNode and not have termination points
        // Would need to change listener or grab tp nodes in here.
        List<TerminationPoint> terminationPoints = MdsalUtils.getTerminationPoints(node);
        for (TerminationPoint terminationPoint : terminationPoints) {
            processPortUpdate(node, terminationPoint.getAugmentation(OvsdbTerminationPointAugmentation.class));
        }
    }

    private void processBridgeUpdate(Node node, Action action) {
        OvsdbBridgeAugmentation bridge = MdsalUtils.readBridge(node);
        switch (action) {
            case ADD:
            case UPDATE:
                processBridgeUpdate(node, bridge);
                break;
            case DELETE:
                processBridgeDelete(node, bridge);
                break;
        }
    }

    private void processBridgeDelete(Node node, OvsdbBridgeAugmentation bridge) {
        logger.debug("processBridgeUpdate {}, {}", node, bridge);
    }

    private void processBridgeUpdate(Node node, OvsdbBridgeAugmentation bridge) {
        logger.debug("processBridgeUpdate {}, {}", node, bridge);
    }
}
