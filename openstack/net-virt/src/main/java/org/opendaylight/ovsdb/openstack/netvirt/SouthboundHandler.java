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
import org.opendaylight.ovsdb.openstack.netvirt.impl.NeutronL3Adapter;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Madhu Venugopal
 * @author Brent Salisbury
 * @author Dave Tucker
 * @author Sam Hague (shague@redhat.com)
 */
public class SouthboundHandler extends AbstractHandler
        implements ConfigInterface, NodeCacheListener, OvsdbInventoryListener {
    static final Logger logger = LoggerFactory.getLogger(SouthboundHandler.class);

    // The implementation for each of these services is resolved by the OSGi Service Manager
    private volatile ConfigurationService configurationService;
    private volatile BridgeConfigurationManager bridgeConfigurationManager;
    private volatile TenantNetworkManager tenantNetworkManager;
    private volatile NetworkingProviderManager networkingProviderManager;
    private volatile NeutronL3Adapter neutronL3Adapter;
    private volatile NodeCacheManager nodeCacheManager = null;
    private volatile EventDispatcher eventDispatcher;
    private volatile OvsdbInventoryService ovsdbInventoryService;

    void start() {
        this.triggerUpdates();
    }

    /*void init() {
        logger.info(">>>>>> init {}", this.getClass());
    }*/

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
        logger.info("ovsdbUpdate: {} - {} - <<{}>> <<{}>>", ovsdbType, action, node, resourceAugmentationData);
        enqueueEvent(new SouthboundEvent(node, resourceAugmentationData,
                ovsdbTypeToSouthboundEventType(ovsdbType), action));
    }

    private void handleInterfaceUpdate (Node node, OvsdbTerminationPointAugmentation tp) {
        logger.debug("handleInterfaceUpdate <{}> <{}>", node, tp);
        NeutronNetwork network = tenantNetworkManager.getTenantNetwork(tp);
        if (network != null && !network.getRouterExternal()) {
            logger.trace("handleInterfaceUpdate <{}> <{}> network: {}", node, tp, network.getNetworkUUID());
            neutronL3Adapter.handleInterfaceEvent(node, tp, network, Action.UPDATE);
            if (bridgeConfigurationManager.createLocalNetwork(node, network)) {
                networkingProviderManager.getProvider(node).handleInterfaceUpdate(network, node, tp);
            }
        } else {
            logger.debug("No tenant network found on node: <{}> for interface: <{}>", node, tp);
        }
    }

    private void handleInterfaceDelete (Node node, OvsdbTerminationPointAugmentation intf,
                                        boolean isLastInstanceOnNode, NeutronNetwork network) {
        logger.debug("handleInterfaceDelete: node: <{}>, isLastInstanceOnNode: {}, interface: <{}>",
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
            networkingProviderManager.getProvider(node).handleInterfaceDelete(network.getProviderNetworkType(),
                    network, node, intf, isLastInstanceOnNode);
        }
    }

    private void triggerUpdates() {
        List<Node> nodes = null; // nodeCacheManager.getBridgeNodes();
        if (nodes == null) return;
        for (Node node : nodes) {
            OvsdbBridgeAugmentation bridge = node.getAugmentation(OvsdbBridgeAugmentation.class);
            if (bridge != null) {
                processBridgeUpdate(node, bridge);
            }

            List<TerminationPoint> tps = MdsalUtils.extractTerminationPoints(node);
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
        logger.debug("processPortDelete <{}> <{}>", node, ovsdbTerminationPointAugmentation);
        NeutronNetwork network = null;
        if (context == null) {
            network = tenantNetworkManager.getTenantNetwork(ovsdbTerminationPointAugmentation);
        } else {
            network = (NeutronNetwork)context;
        }
        List<String> phyIfName = bridgeConfigurationManager.getAllPhysicalInterfaceNames(node);
        if (isInterfaceOfInterest(ovsdbTerminationPointAugmentation, phyIfName)) {
            if (network != null) {
                this.handleInterfaceDelete(node, ovsdbTerminationPointAugmentation, false, network);
            } else {
                logger.warn("processPortDelete: network was null, ignoring update");
            }
        } else if (network != null && !network.getRouterExternal()) {
            logger.debug("Network {}: Delete interface {} attached to bridge {}", network.getNetworkUUID(),
                    ovsdbTerminationPointAugmentation.getInterfaceUuid(), node.getNodeId());
            try {
                OvsdbBridgeAugmentation ovsdbBridgeAugmentation = MdsalUtils.getBridge(node);
                if (ovsdbBridgeAugmentation != null) {
                    List<TerminationPoint> terminationPoints = node.getTerminationPoint();
                    if (!terminationPoints.isEmpty()){
                        boolean isLastInstanceOnNode = true;
                        for (TerminationPoint terminationPoint : terminationPoints) {
                            OvsdbTerminationPointAugmentation tpAugmentation =
                                    terminationPoint.getAugmentation( OvsdbTerminationPointAugmentation.class);
                            if (tpAugmentation.getInterfaceUuid().equals(
                                    ovsdbTerminationPointAugmentation.getInterfaceUuid())) {
                                continue;
                            }
                            NeutronNetwork neutronNetwork = tenantNetworkManager.getTenantNetwork(tpAugmentation);
                            if (neutronNetwork != null && neutronNetwork.equals(network)) {
                                isLastInstanceOnNode = false;
                                break;
                            }
                        }
                        this.handleInterfaceDelete(node, ovsdbTerminationPointAugmentation,
                                isLastInstanceOnNode, network);
                    }
                }
            } catch (Exception e) {
                logger.error("Error fetching Interface Rows for node " + node, e);
            }
        }
    }

    private boolean isInterfaceOfInterest(OvsdbTerminationPointAugmentation terminationPoint, List<String> phyIfName) {
        logger.trace("SouthboundHandler#isInterfaceOfInterest: Interface : {}", terminationPoint);

        if(terminationPoint.getInterfaceType() == null){
            // This is OK since eth ports don't have an interface type
            logger.info("No type found for the interface : {}", terminationPoint);
            return false;
        }
        return (MdsalHelper.createOvsdbInterfaceType(
                terminationPoint.getInterfaceType()).equals(NetworkHandler.NETWORK_TYPE_VXLAN)
                ||
                MdsalHelper.createOvsdbInterfaceType(
                        terminationPoint.getInterfaceType()).equals(NetworkHandler.NETWORK_TYPE_GRE)
                ||
                phyIfName.contains(terminationPoint.getName()));
    }

    /**
     * Notification about an OpenFlow Node
     *
     * @param node the {@link Node Node} of interest in the notification
     * @param action the {@link Action}
     * @see NodeCacheListener#notifyNode
     */
    @Override
    public void notifyNode (Node node, Action action) {
        logger.info("notifyNode: action: {}, Node <{}>", action, node);

        if (action.equals(Action.ADD)) {
            if (MdsalUtils.getBridge(node) != null) {
                networkingProviderManager.getProvider(node).initializeOFFlowRules(node);
            }
        }
    }

    /**
     * Process the event.
     *
     * @param abstractEvent the {@link org.opendaylight.ovsdb.openstack.netvirt.AbstractEvent} event to be handled.
     * @see org.opendaylight.ovsdb.openstack.netvirt.api.EventDispatcher
     */
    @Override
    public void processEvent(AbstractEvent abstractEvent) {
        if (!(abstractEvent instanceof SouthboundEvent)) {
            logger.error("processEvent: Unable to process abstract event {}", abstractEvent);
            return;
        }
        SouthboundEvent ev = (SouthboundEvent) abstractEvent;
        logger.trace("processEvent: {}", ev);
        switch (ev.getType()) {
            case NODE:
                processOvsdbNodeEvent(ev);
                break;

            case BRIDGE:
                processBridgeEvent(ev);
                break;

            case PORT:
                processPortEvent(ev);
                break;

            case OPENVSWITCH:
                processOpenVSwitchEvent(ev);
                break;

            default:
                logger.warn("Unable to process type " + ev.getType() +
                        " action " + ev.getAction() + " for node " + ev.getNode());
                break;
        }
    }

    private void processOvsdbNodeEvent(SouthboundEvent ev) {
        switch (ev.getAction()) {
            case ADD:
                processOvsdbNodeCreate(ev.getNode(), (OvsdbNodeAugmentation) ev.getAugmentationData());
                break;
            case UPDATE:
                processOvsdbNodeUpdate(ev.getNode(), (OvsdbNodeAugmentation) ev.getAugmentationData());
                break;
            case DELETE:
                processOvsdbNodeDelete(ev.getNode(), (OvsdbNodeAugmentation) ev.getAugmentationData());
                break;
        }
    }

    private void processOvsdbNodeCreate(Node node, OvsdbNodeAugmentation ovsdbNode) {
        logger.info("processOvsdbNodeCreate <{}> <{}>", node, ovsdbNode);
        nodeCacheManager.nodeAdded(node);
        bridgeConfigurationManager.prepareNode(node);
    }

    private void processOvsdbNodeUpdate(Node node, OvsdbNodeAugmentation ovsdbNode) {
        logger.info("processOvsdbNodeUpdate <{}> <{}>", node, ovsdbNode);
        nodeCacheManager.nodeAdded(node);
    }

    private void processOvsdbNodeDelete(Node node, OvsdbNodeAugmentation ovsdbNode) {
        logger.info("processOvsdbNodeDelete <{}> <{}>", node, ovsdbNode);
        nodeCacheManager.nodeRemoved(node);
        /* TODO SB_MIGRATION
        * I don't think we want to do this yet
        InstanceIdentifier<Node> bridgeNodeIid =
                MdsalHelper.createInstanceIdentifier(ovsdbNode.getConnectionInfo(),
                        Constants.INTEGRATION_BRIDGE);
        MdsalUtils.delete(LogicalDatastoreType.CONFIGURATION, bridgeNodeIid);
        */
    }

    private void processPortEvent(SouthboundEvent ev) {
        switch (ev.getAction()) {
            case ADD:
            case UPDATE:
                processPortUpdate(ev.getNode(), (OvsdbTerminationPointAugmentation) ev.getAugmentationData());
                break;
            case DELETE:
                processPortDelete(ev.getNode(), (OvsdbTerminationPointAugmentation) ev.getAugmentationData(), null);
                break;
        }
    }

    private void processPortUpdate(Node node, OvsdbTerminationPointAugmentation port) {
        logger.debug("processPortUpdate <{}> <{}>", node, port);
        NeutronNetwork network = tenantNetworkManager.getTenantNetwork(port);
        if (network != null && !network.getRouterExternal()) {
            this.handleInterfaceUpdate(node, port);
        }
    }

    private void processOpenVSwitchEvent(SouthboundEvent ev) {
        switch (ev.getAction()) {
            case ADD:
            case UPDATE:
                processOpenVSwitchUpdate(ev.getNode());
                break;
            case DELETE:
                break;
        }
    }

    private void processOpenVSwitchUpdate(Node node) {
        logger.debug("processOpenVSwitchUpdate {}", node);
        // TODO this node might be the OvsdbNode and not have termination points
        // Would need to change listener or grab tp nodes in here.
        List<TerminationPoint> terminationPoints = MdsalUtils.extractTerminationPoints(node);
        for (TerminationPoint terminationPoint : terminationPoints) {
            processPortUpdate(node, terminationPoint.getAugmentation(OvsdbTerminationPointAugmentation.class));
        }
    }

    private void processBridgeEvent(SouthboundEvent ev) {
        switch (ev.getAction()) {
            case ADD:
                processBridgeCreate(ev.getNode(), (OvsdbBridgeAugmentation) ev.getAugmentationData());
                break;
            case UPDATE:
                processBridgeUpdate(ev.getNode(), (OvsdbBridgeAugmentation) ev.getAugmentationData());
                break;
            case DELETE:
                processBridgeDelete(ev.getNode(), (OvsdbBridgeAugmentation) ev.getAugmentationData());
                break;
        }
    }

    private boolean isMainBridge(Node node, OvsdbBridgeAugmentation bridge) {
        boolean rv = false;
        String nodeIdStr = node.getNodeId().getValue();
        String bridgeName = nodeIdStr.substring(nodeIdStr.lastIndexOf('/') + 1);
        List<TerminationPoint> terminationPoints = MdsalUtils.extractTerminationPoints(node);
        if (terminationPoints != null && terminationPoints.size() == 1) {
        }
        OvsdbTerminationPointAugmentation port = MdsalUtils.extractTerminationPointAugmentation(node, bridgeName);
        if (port != null) {
            String datapathId = MdsalUtils.getDatapathId(bridge);
            // Having a datapathId means the ovsdb node has connected to ODL
            if (datapathId != null) {
                rv = true;
            } else {
                logger.info("datapathId not found");
            }
        }
        return rv;
    }

    private void processBridgeCreate(Node node, OvsdbBridgeAugmentation bridge) {
        logger.debug("processBridgeCreate <{}> <{}>", node, bridge);
        String datapathId = MdsalUtils.getDatapathId(bridge);
        // Having a datapathId means the ovsdb node has connected to ODL
        if (datapathId != null) {
            nodeCacheManager.nodeAdded(node);
        } else {
            logger.info("processBridgeCreate datapathId not found");
        }
    }

    private void processBridgeUpdate(Node node, OvsdbBridgeAugmentation bridge) {
        logger.debug("processBridgeUpdate <{}> <{}>", node, bridge);
        String datapathId = MdsalUtils.getDatapathId(bridge);
        // Having a datapathId means the ovsdb node has connected to ODL
        if (datapathId != null) {
            nodeCacheManager.nodeAdded(node);
        } else {
            logger.info("processBridgeUpdate datapathId not found");
        }
    }

    private void processBridgeDelete(Node node, OvsdbBridgeAugmentation bridge) {
        logger.debug("processBridgeDelete: Delete bridge from config data store: <{}> <{}>",
                node, bridge);
        nodeCacheManager.nodeRemoved(node);
        // TODO SB_MIGRATION
        // Not sure if we want to do this yet
        MdsalUtils.deleteBridge(node);
    }

    @Override
    public void setDependencies(BundleContext bundleContext, ServiceReference serviceReference) {
        configurationService =
                (ConfigurationService) ServiceHelper.getGlobalInstance(ConfigurationService.class, this);
        networkingProviderManager =
                (NetworkingProviderManager) ServiceHelper.getGlobalInstance(NetworkingProviderManager.class, this);
        tenantNetworkManager =
                (TenantNetworkManager) ServiceHelper.getGlobalInstance(TenantNetworkManager.class, this);
        bridgeConfigurationManager =
                (BridgeConfigurationManager) ServiceHelper.getGlobalInstance(BridgeConfigurationManager.class, this);
        nodeCacheManager =
                (NodeCacheManager) ServiceHelper.getGlobalInstance(NodeCacheManager.class, this);
        nodeCacheManager.cacheListenerAdded(
                bundleContext.getServiceReference(OvsdbInventoryListener.class.getName()), this);
        neutronL3Adapter =
                (NeutronL3Adapter) ServiceHelper.getGlobalInstance(NeutronL3Adapter.class, this);
        eventDispatcher =
                (EventDispatcher) ServiceHelper.getGlobalInstance(EventDispatcher.class, this);
        eventDispatcher.eventHandlerAdded(
                bundleContext.getServiceReference(OvsdbInventoryListener.class.getName()), this);
        super.setDispatcher(eventDispatcher);
        ovsdbInventoryService =
                (OvsdbInventoryService) ServiceHelper.getGlobalInstance(OvsdbInventoryService.class, this);
        ovsdbInventoryService.listenerAdded(this);
    }

    @Override
    public void setDependencies(Object impl) {}
}
