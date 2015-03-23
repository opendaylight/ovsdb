/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Brent Salisbury, Sam Hague, Dave Tucker
 */
package org.opendaylight.ovsdb.openstack.netvirt;

import org.opendaylight.neutron.spi.NeutronNetwork;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.openstack.netvirt.api.Action;
import org.opendaylight.ovsdb.openstack.netvirt.api.BridgeConfigurationManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.ConfigurationService;
import org.opendaylight.ovsdb.openstack.netvirt.api.NetworkingProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.NetworkingProviderManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.NodeCacheListener;
import org.opendaylight.ovsdb.openstack.netvirt.api.TenantNetworkManager;
import org.opendaylight.ovsdb.openstack.netvirt.impl.NeutronL3Adapter;
import org.opendaylight.ovsdb.plugin.api.OvsdbConfigurationService;
import org.opendaylight.ovsdb.plugin.api.OvsdbConnectionService;
import org.opendaylight.ovsdb.plugin.api.OvsdbInventoryListener;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.Interface;
import org.opendaylight.ovsdb.schema.openvswitch.OpenVSwitch;
import org.opendaylight.ovsdb.schema.openvswitch.Port;
import org.opendaylight.ovsdb.utils.mdsal.node.StringConvertor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

public class SouthboundHandler extends AbstractHandler
        implements NodeCacheListener, OvsdbInventoryListener {
    static final Logger logger = LoggerFactory.getLogger(SouthboundHandler.class);
    //private Thread eventThread;

    // The implementation for each of these services is resolved by the OSGi Service Manager
    private volatile ConfigurationService configurationService;
    private volatile BridgeConfigurationManager bridgeConfigurationManager;
    private volatile TenantNetworkManager tenantNetworkManager;
    private volatile NetworkingProviderManager networkingProviderManager;
    private volatile OvsdbConfigurationService ovsdbConfigurationService;
    private volatile OvsdbConnectionService connectionService;
    private volatile NeutronL3Adapter neutronL3Adapter;

    void init() {
    }

    void start() {
        this.triggerUpdates();
    }

    @Override
    public void nodeAdded(Node node, InetAddress address, int port) {
        logger.info("nodeAdded: {}", node);
        this.enqueueEvent(new SouthboundEvent(node, Action.ADD));
    }

    @Override
    public void nodeRemoved(Node node) {
        this.enqueueEvent(new SouthboundEvent(node, Action.DELETE));
    }

    @Override
    public void rowAdded(Node node, String tableName, String uuid, Row row) {
        this.enqueueEvent(new SouthboundEvent(node, tableName, uuid, row, Action.ADD));
    }

    @Override
    public void rowUpdated(Node node, String tableName, String uuid, Row oldRow, Row newRow) {
        if (this.isUpdateOfInterest(node, oldRow, newRow)) {
            this.enqueueEvent(new SouthboundEvent(node, tableName, uuid, newRow, Action.UPDATE));
        }
    }

    /*
     * Ignore unnecessary updates to be even considered for processing.
     * (Especially stats update are fast and furious).
     */

    private boolean isUpdateOfInterest(Node node, Row oldRow, Row newRow) {
        if (oldRow == null) return true;
        if (newRow.getTableSchema().getName().equals(ovsdbConfigurationService.getTableName(node, Interface.class))) {
            // We are NOT interested in Stats only updates
            Interface oldIntf = ovsdbConfigurationService.getTypedRow(node, Interface.class, oldRow);
            if (oldIntf.getName() == null && oldIntf.getExternalIdsColumn() == null && oldIntf.getMacColumn() == null &&
                oldIntf.getOpenFlowPortColumn() == null && oldIntf.getOptionsColumn() == null && oldIntf.getOtherConfigColumn() == null &&
                oldIntf.getTypeColumn() == null) {
                logger.trace("IGNORING Interface Update: node {}, row: {}", node, newRow);
                return false;
            }
        } else if (newRow.getTableSchema().getName().equals(ovsdbConfigurationService.getTableName(node, Port.class))) {
            // We are NOT interested in Stats only updates
            Port oldPort = ovsdbConfigurationService.getTypedRow(node, Port.class, oldRow);
            if (oldPort.getName() == null && oldPort.getExternalIdsColumn() == null && oldPort.getMacColumn() == null &&
                oldPort.getInterfacesColumn() == null && oldPort.getTagColumn() == null && oldPort.getTrunksColumn() == null) {
                logger.trace("IGNORING Port Update: node {}, row: {}", node, newRow);
                return false;
            }
        } else if (newRow.getTableSchema().getName().equals(ovsdbConfigurationService.getTableName(node, OpenVSwitch.class))) {
            OpenVSwitch oldOpenvSwitch = ovsdbConfigurationService.getTypedRow(node, OpenVSwitch.class, oldRow);
            if (oldOpenvSwitch.getOtherConfigColumn()== null) {
                /* we are only interested in other_config field change */
                return false;
            }
        }
        return true;
    }

    @Override
    public void rowRemoved(Node node, String tableName, String uuid, Row row, Object context) {
        this.enqueueEvent(new SouthboundEvent(node, tableName, uuid, row, context, Action.DELETE));
    }

    public void processNodeUpdate(Node node, Action action) {
        if (action == Action.DELETE) return;
        logger.trace("Process Node added {}", node);
        bridgeConfigurationManager.prepareNode(node);
    }

    private void processRowUpdate(Node node, String tableName, String uuid, Row row,
                                  Object context, Action action) {
        if (action == Action.DELETE) {
            if (tableName.equalsIgnoreCase(ovsdbConfigurationService.getTableName(node, Interface.class))) {
                logger.debug("Processing update of {}. Deleted node: {}, uuid: {}, row: {}", tableName, node, uuid, row);
                Interface deletedIntf = ovsdbConfigurationService.getTypedRow(node, Interface.class, row);
                NeutronNetwork network = null;
                if (context == null) {
                    network = tenantNetworkManager.getTenantNetwork(deletedIntf);
                } else {
                    network = (NeutronNetwork)context;
                }
                List<String> phyIfName = bridgeConfigurationManager.getAllPhysicalInterfaceNames(node);
                logger.info("Delete interface " + deletedIntf.getName());

                if (deletedIntf.getTypeColumn().getData().equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_VXLAN) ||
                    deletedIntf.getTypeColumn().getData().equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_GRE) ||
                    phyIfName.contains(deletedIntf.getName())) {
                    /* delete tunnel interfaces or physical interfaces */
                    this.handleInterfaceDelete(node, uuid, deletedIntf, false, null);
                } else if (network != null && !network.getRouterExternal()) {
                    logger.debug("Processing update of {}:{} node {} intf {} network {}",
                            tableName, action, node, uuid, network.getNetworkUUID());
                    try {
                        ConcurrentMap<String, Row> interfaces = this.ovsdbConfigurationService
                                .getRows(node, ovsdbConfigurationService.getTableName(node, Interface.class));
                        if (interfaces != null) {
                            boolean isLastInstanceOnNode = true;
                            for (String intfUUID : interfaces.keySet()) {
                                if (intfUUID.equals(uuid)) continue;
                                Interface intf = this.ovsdbConfigurationService.getTypedRow(node, Interface.class, interfaces.get(intfUUID));
                                NeutronNetwork neutronNetwork = tenantNetworkManager.getTenantNetwork(intf);
                                if (neutronNetwork != null && neutronNetwork.equals(network)) isLastInstanceOnNode = false;
                            }
                            this.handleInterfaceDelete(node, uuid, deletedIntf, isLastInstanceOnNode, network);
                        }
                    } catch (Exception e) {
                        logger.error("Error fetching Interface Rows for node " + node, e);
                    }
                }
            }
        }
        else if (tableName.equalsIgnoreCase(ovsdbConfigurationService.getTableName(node, Interface.class))) {
            logger.debug("Processing update of {}:{} node: {}, interface uuid: {}, row: {}",
                    tableName, action, node, uuid, row);
            Interface intf = this.ovsdbConfigurationService.getTypedRow(node, Interface.class, row);
            NeutronNetwork network = tenantNetworkManager.getTenantNetwork(intf);
            if (network != null && !network.getRouterExternal()) {
                if (networkingProviderManager.getProvider(node).hasPerTenantTunneling()) {
                    int vlan = tenantNetworkManager.networkCreated(node, network.getID());
                    String portUUID = this.getPortIdForInterface(node, uuid, intf);
                    if (portUUID != null) {
                        logger.debug("Neutron Network {}:{} Created with Internal vlan {} port {}",
                                 network.getNetworkUUID(), network.getNetworkName(), vlan, portUUID);
                        tenantNetworkManager.programInternalVlan(node, portUUID, network);
                    } else {
                        logger.trace("Neutron Network {}:{} Created with Internal vlan {} but have no portUUID",
                                 network.getNetworkUUID(), network.getNetworkName(), vlan);
                    }
                }
                this.handleInterfaceUpdate(node, uuid, intf);
            }

        } else if (tableName.equalsIgnoreCase(ovsdbConfigurationService.getTableName(node, Port.class))) {
            logger.debug("Processing update of {}:{} node: {}, port uuid: {}, row: {}", tableName, action, node, uuid, row);
            Port port = this.ovsdbConfigurationService.getTypedRow(node, Port.class, row);
            Set<UUID> interfaceUUIDs = port.getInterfacesColumn().getData();
            for (UUID intfUUID : interfaceUUIDs) {
                logger.trace("Scanning interface "+intfUUID);
                try {
                    Row intfRow = this.ovsdbConfigurationService
                            .getRow(node, ovsdbConfigurationService.getTableName(node, Interface.class),
                                    intfUUID.toString());
                    Interface intf = this.ovsdbConfigurationService.getTypedRow(node, Interface.class, intfRow);
                    NeutronNetwork network = tenantNetworkManager.getTenantNetwork(intf);
                    if (network != null && !network.getRouterExternal()) {
                         logger.debug("Processing update of {}:{} node {} intf {} network {}",
                                 tableName, action, node, intfUUID, network.getNetworkUUID());
                        tenantNetworkManager.programInternalVlan(node, uuid, network);
                        this.handleInterfaceUpdate(node, intfUUID.toString(), intf);
                    } else {
                        logger.trace("Ignoring update because there is not a neutron network {} for port {}, interface {}",
                                network, uuid, intfUUID);
                    }
                } catch (Exception e) {
                    logger.error("Failed to process row update", e);
                }
            }
        } else if (tableName.equalsIgnoreCase(ovsdbConfigurationService.getTableName(node, OpenVSwitch.class))) {
            logger.debug("Processing update of {}:{} node: {}, ovs uuid: {}, row: {}", tableName, action, node, uuid, row);
            try {
                ConcurrentMap<String, Row> interfaces = this.ovsdbConfigurationService
                        .getRows(node, ovsdbConfigurationService.getTableName(node, Interface.class));
                if (interfaces != null) {
                    for (String intfUUID : interfaces.keySet()) {
                        Interface intf = ovsdbConfigurationService.getTypedRow(node, Interface.class, interfaces.get(intfUUID));
                        this.handleInterfaceUpdate(node, intfUUID, intf);
                    }
                }
            } catch (Exception e) {
                logger.error("Error fetching Interface Rows for node " + node, e);
            }
        } else if (tableName.equalsIgnoreCase(ovsdbConfigurationService.getTableName(node, Bridge.class))) {
            logger.debug("Processing update of {}:{} node: {}, bridge uuid: {}, row: {}", tableName, action, node, uuid, row);
            Bridge bridge = ovsdbConfigurationService.getTypedRow(node, Bridge.class, row);
            final Set<String> dpids = bridge.getDatapathIdColumn().getData();
            if (dpids != null &&
                    (bridge.getName().equals(configurationService.getIntegrationBridgeName()) ||
                            bridge.getName().equals(configurationService.getExternalBridgeName()))) {
                NetworkingProvider networkingProvider = networkingProviderManager.getProvider(node);
                for (String dpid : dpids) {
                    networkingProvider.notifyFlowCapableNodeEvent(StringConvertor.dpidStringToLong(dpid), action);
                }
            }
        }
    }

    private void handleInterfaceUpdate (Node node, String uuid, Interface intf) {
        logger.trace("Interface update of node: {}, uuid: {}", node, uuid);
        NeutronNetwork network = tenantNetworkManager.getTenantNetwork(intf);
        if (network != null) {
            neutronL3Adapter.handleInterfaceEvent(node, intf, network, Action.UPDATE);
            if (bridgeConfigurationManager.createLocalNetwork(node, network))
                networkingProviderManager.getProvider(node).handleInterfaceUpdate(network, node, intf);
        } else {
            logger.debug("No tenant network found on node: {}, uuid: {} for interface: {}", node, uuid, intf);
        }
    }

    private void handleInterfaceDelete (Node node, String uuid, Interface intf, boolean isLastInstanceOnNode,
                                        NeutronNetwork network) {
        logger.debug("handleInterfaceDelete: node: {}, uuid: {}, isLastInstanceOnNode: {}, interface: {}",
                node, uuid, isLastInstanceOnNode, intf);

        neutronL3Adapter.handleInterfaceEvent(node, intf, network, Action.DELETE);
        List<String> phyIfName = bridgeConfigurationManager.getAllPhysicalInterfaceNames(node);
        if (intf.getTypeColumn().getData().equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_VXLAN) ||
            intf.getTypeColumn().getData().equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_GRE) ||
            phyIfName.contains(intf.getName())) {
            /* delete tunnel or physical interfaces */
            networkingProviderManager.getProvider(node).handleInterfaceDelete(intf.getTypeColumn().getData(), null, node, intf, isLastInstanceOnNode);
        } else if (network != null) {
            if (!network.getProviderNetworkType().equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_VLAN)) { /* vlan doesn't need a tunnel endpoint */
                if (configurationService.getTunnelEndPoint(node) == null) {
                    logger.error("Tunnel end-point configuration missing. Please configure it in OpenVSwitch Table");
                    return;
                }
            }
            if (isLastInstanceOnNode & networkingProviderManager.getProvider(node).hasPerTenantTunneling()) {
                tenantNetworkManager.reclaimInternalVlan(node, uuid, network);
            }
            networkingProviderManager.getProvider(node).handleInterfaceDelete(network.getProviderNetworkType(), network, node, intf, isLastInstanceOnNode);
        }
    }

    private String getPortIdForInterface (Node node, String uuid, Interface intf) {
        try {
            Map<String, Row> ports = this.ovsdbConfigurationService.getRows(node, ovsdbConfigurationService.getTableName(node, Port.class));
            if (ports == null) return null;
            for (String portUUID : ports.keySet()) {
                Port port = ovsdbConfigurationService.getTypedRow(node, Port.class, ports.get(portUUID));
                Set<UUID> interfaceUUIDs = port.getInterfacesColumn().getData();
                logger.trace("Scanning Port {} to identify interface : {} ",port, uuid);
                for (UUID intfUUID : interfaceUUIDs) {
                    if (intfUUID.toString().equalsIgnoreCase(uuid)) {
                        logger.trace("Found Interface {} -> {}", uuid, portUUID);
                        return portUUID;
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Failed to get Port tag for for Intf " + intf, e);
        }
        return null;
    }

    private void triggerUpdates() {
        List<Node> nodes = connectionService.getNodes();
        if (nodes == null) return;
        for (Node node : nodes) {
            try {
                List<String> tableNames = ovsdbConfigurationService.getTables(node);
                if (tableNames == null) continue;
                for (String tableName : tableNames) {
                    Map<String, Row> rows = ovsdbConfigurationService.getRows(node, tableName);
                    if (rows == null) continue;
                    for (String uuid : rows.keySet()) {
                        Row row = rows.get(uuid);
                        this.rowAdded(node, tableName, uuid, row);
                    }
                }
            } catch (Exception e) {
                logger.error("Exception during OVSDB Southbound update trigger", e);
            }
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
        //logger.info("processEvent: {}", ev);
        switch (ev.getType()) {
            case NODE:
                try {
                    processNodeUpdate(ev.getNode(), ev.getAction());
                } catch (Exception e) {
                    logger.error("Exception caught in ProcessNodeUpdate for node " + ev.getNode(), e);
                }
                break;
            case ROW:
                try {
                    processRowUpdate(ev.getNode(), ev.getTableName(), ev.getUuid(), ev.getRow(),
                                     ev.getContext(),ev.getAction());
                } catch (Exception e) {
                    logger.error("Exception caught in ProcessRowUpdate for node " + ev.getNode(), e);
                }
                break;
            default:
                logger.warn("Unable to process type " + ev.getType() +
                            " action " + ev.getAction() + " for node " + ev.getNode());
                break;
        }
    }

    /**
     * Notification about an OpenFlow Node
     *
     * @param openFlowNode the {@link org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node Node} of interest in the notification
     * @param action the {@link Action}
     * @see NodeCacheListener#notifyNode
     */
    @Override
    public void notifyNode (Node openFlowNode, Action action) {
        logger.info("notifyNode: Node {} update {} from Controller's inventory Service",
                openFlowNode, action);

        if (action.equals(Action.ADD)) {
            networkingProviderManager.getProvider(openFlowNode).initializeOFFlowRules(openFlowNode);
        }
    }
}
