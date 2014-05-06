/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Brent Salisbury, Sam Hague
 */
package org.opendaylight.ovsdb.neutron;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.opendaylight.controller.networkconfig.neutron.NeutronNetwork;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.controller.switchmanager.IInventoryListener;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.table.Interface;
import org.opendaylight.ovsdb.lib.table.Open_vSwitch;
import org.opendaylight.ovsdb.lib.table.Port;
import org.opendaylight.ovsdb.lib.table.internal.Table;
import org.opendaylight.ovsdb.neutron.provider.ProviderNetworkManager;
import org.opendaylight.ovsdb.plugin.OVSDBInventoryListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SouthboundHandler extends BaseHandler implements OVSDBInventoryListener, IInventoryListener {
    static final Logger logger = LoggerFactory.getLogger(SouthboundHandler.class);
    //private Thread eventThread;
    private ExecutorService eventHandler;
    private BlockingQueue<SouthboundEvent> events;
    List<Node> nodeCache;

    void init() {
        eventHandler = Executors.newSingleThreadExecutor();
        this.events = new LinkedBlockingQueue<SouthboundEvent>();
        nodeCache = new ArrayList<>();
    }

    void start() {
        eventHandler.submit(new Runnable()  {
            @Override
            public void run() {
                while (true) {
                    SouthboundEvent ev;
                    try {
                        ev = events.take();
                    } catch (InterruptedException e) {
                        logger.info("The event handler thread was interrupted, shutting down", e);
                        return;
                    }
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
                            processRowUpdate(ev.getNode(), ev.getTableName(), ev.getUuid(), ev.getRow(), ev.getContext(), ev.getAction());
                        } catch (Exception e) {
                            logger.error("Exception caught in ProcessRowUpdate for node " + ev.getNode(), e);
                        }
                        break;
                    default:
                        logger.warn("Unable to process action " + ev.getAction() + " for node " + ev.getNode());
                    }
                }
            }
        });
        this.triggerUpdates();
    }

    void stop() {
        eventHandler.shutdownNow();
    }

    @Override
    public void nodeAdded(Node node) {
        this.enqueueEvent(new SouthboundEvent(node, SouthboundEvent.Action.ADD));
    }

    @Override
    public void nodeRemoved(Node node) {
        this.enqueueEvent(new SouthboundEvent(node, SouthboundEvent.Action.DELETE));
    }

    @Override
    public void rowAdded(Node node, String tableName, String uuid, Table<?> row) {
        this.enqueueEvent(new SouthboundEvent(node, tableName, uuid, row, SouthboundEvent.Action.ADD));
    }

    @Override
    public void rowUpdated(Node node, String tableName, String uuid, Table<?> oldRow, Table<?> newRow) {
        if (this.isUpdateOfInterest(node, oldRow, newRow)) {
            this.enqueueEvent(new SouthboundEvent(node, tableName, uuid, newRow, SouthboundEvent.Action.UPDATE));
        }
    }

    /*
     * Ignore unneccesary updates to be even considered for processing.
     * (Especially stats update are fast and furious).
     */

    private boolean isUpdateOfInterest(Node node, Table<?> oldRow, Table<?> newRow) {
        if (oldRow == null) return true;
        if (newRow.getTableName().equals(Interface.NAME)) {
            // We are NOT interested in Stats only updates
            Interface oldIntf = (Interface)oldRow;
            if (oldIntf.getName() == null && oldIntf.getExternal_ids() == null && oldIntf.getMac() == null &&
                oldIntf.getOfport() == null && oldIntf.getOptions() == null && oldIntf.getOther_config() == null &&
                oldIntf.getType() == null) {
                logger.trace("IGNORING Interface Update: node {}, row: {}", node, newRow);
                return false;
            }
        } else if (newRow.getTableName().equals(Port.NAME)) {
            // We are NOT interested in Stats only updates
            Port oldPort = (Port)oldRow;
            if (oldPort.getName() == null && oldPort.getExternal_ids() == null && oldPort.getMac() == null &&
                oldPort.getInterfaces() == null && oldPort.getTag() == null && oldPort.getTrunks() == null) {
                logger.trace("IGNORING Port Update: node {}, row: {}", node, newRow);
                return false;
            }
        } else if (newRow.getTableName().equals(Open_vSwitch.NAME)) {
    /* print the row for now */
            Open_vSwitch oldOpenvSwitch = (Open_vSwitch) oldRow;
            if (oldOpenvSwitch.getOther_config()== null) {
                /* we are only interested in other_config field change */
                return false;
            }
        }
        return true;
    }

    @Override
    public void rowRemoved(Node node, String tableName, String uuid, Table<?> row, Object context) {
        this.enqueueEvent(new SouthboundEvent(node, tableName, uuid, row, context, SouthboundEvent.Action.DELETE));
    }

    private void enqueueEvent (SouthboundEvent event) {
        try {
            events.put(event);
        } catch (InterruptedException e) {
            logger.error("Thread was interrupted while trying to enqueue event ", e);
        }
    }

    public void processNodeUpdate(Node node, SouthboundEvent.Action action) {
        if (action == SouthboundEvent.Action.DELETE) return;
        logger.trace("Process Node added {}", node);
        InternalNetworkManager.getManager().prepareInternalNetwork(node);
    }

    private void processRowUpdate(Node node, String tableName, String uuid, Table<?> row,
                                  Object context, SouthboundEvent.Action action) {
        if (action == SouthboundEvent.Action.DELETE) {
            if (Interface.NAME.getName().equalsIgnoreCase(tableName)) {
                logger.debug("processRowUpdate: {} Deleted node: {}, uuid: {}, row: {}", tableName, node, uuid, row);
                Interface deletedIntf = (Interface)row;
                NeutronNetwork network = null;
                if (context == null) {
                    network = TenantNetworkManager.getManager().getTenantNetworkForInterface(deletedIntf);
                } else {
                    network = (NeutronNetwork)context;
                }
                logger.info("Delete interface " + deletedIntf.getName());
                if (deletedIntf.getType().equalsIgnoreCase("vxlan") ||
                    deletedIntf.getType().equalsIgnoreCase("gre")) {
                    /* delete tunnel interfaces */
                    this.handleInterfaceDelete(node, uuid, deletedIntf, false, null);
                } else if (network != null && !network.getRouterExternal()) {
                    try {
                        ConcurrentMap<String, Table<?>> interfaces = this.ovsdbConfigService.getRows(node, Interface.NAME.getName());
                        if (interfaces != null) {
                            boolean isLastInstanceOnNode = true;
                            for (String intfUUID : interfaces.keySet()) {
                                if (intfUUID.equals(uuid)) continue;
                                Interface intf = (Interface) interfaces.get(intfUUID);
                                NeutronNetwork neutronNetwork = TenantNetworkManager.getManager().getTenantNetworkForInterface(intf);
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
        else if (Interface.NAME.getName().equalsIgnoreCase(tableName)) {
            logger.debug("processRowUpdate: {} Added / Updated node: {}, uuid: {}, row: {}", tableName, node, uuid, row);
            Interface intf = (Interface)row;
            NeutronNetwork network = TenantNetworkManager.getManager().getTenantNetworkForInterface(intf);
            if (network != null && !network.getRouterExternal()) {
                if (ProviderNetworkManager.getManager().hasPerTenantTunneling()) {
                    int vlan = TenantNetworkManager.getManager().networkCreated(node, network.getID());
                    logger.trace("Neutron Network {}:{} Created with Internal Vlan: {}", network.getNetworkUUID(), network.getNetworkName(), vlan);

                    String portUUID = this.getPortIdForInterface(node, uuid, intf);
                    if (portUUID != null) {
                        TenantNetworkManager.getManager().programTenantNetworkInternalVlan(node, portUUID, network);
                    }
                }
                this.handleInterfaceUpdate(node, uuid, intf);
            }
        } else if (Port.NAME.getName().equalsIgnoreCase(tableName)) {
            logger.debug("processRowUpdate: {} Added / Updated node: {}, uuid: {}, row: {}", tableName, node, uuid, row);
            Port port = (Port)row;
            Set<UUID> interfaceUUIDs = port.getInterfaces();
            for (UUID intfUUID : interfaceUUIDs) {
                logger.trace("Scanning interface "+intfUUID);
                try {
                    Interface intf = (Interface)this.ovsdbConfigService.getRow(node, Interface.NAME.getName(), intfUUID.toString());
                    NeutronNetwork network = TenantNetworkManager.getManager().getTenantNetworkForInterface(intf);
                    if (network != null && !network.getRouterExternal()) {
                        TenantNetworkManager.getManager().programTenantNetworkInternalVlan(node, uuid, network);
                    } else {
                        logger.trace("ignore update because there is not a neutron network.");
                    }
                } catch (Exception e) {
                    logger.error("Failed to process row update", e);
                }
            }
        } else if (Open_vSwitch.NAME.getName().equalsIgnoreCase(tableName)) {
            logger.debug("processRowUpdate: {} Added / Updated node: {}, uuid: {}, row: {}", tableName, node, uuid, row);
            try {
                ConcurrentMap<String, Table<?>> interfaces = this.ovsdbConfigService.getRows(node, Interface.NAME.getName());
                if (interfaces != null) {
                    for (String intfUUID : interfaces.keySet()) {
                        Interface intf = (Interface) interfaces.get(intfUUID);
                        this.handleInterfaceUpdate(node, intfUUID, intf);
                    }
                }
            } catch (Exception e) {
                logger.error("Error fetching Interface Rows for node " + node, e);
            }
        }
    }

    private void handleInterfaceUpdate (Node node, String uuid, Interface intf) {
        logger.debug("handleInterfaceUpdate: node: {}, uuid: {}", node, uuid);
        NeutronNetwork network = TenantNetworkManager.getManager().getTenantNetworkForInterface(intf);
        if (network != null) {
            if (InternalNetworkManager.getManager().checkAndCreateNetwork(node, network)) {
                ProviderNetworkManager.getManager().handleInterfaceUpdate(network, node, intf);
            }
        }
    }

    private void handleInterfaceDelete (Node node, String uuid, Interface intf, boolean isLastInstanceOnNode,
                                        NeutronNetwork network) {
        logger.debug("handleInterfaceDelete: node: {}, uuid: {}, isLastInstanceOnNode: {}, interface: {}",
                node, uuid, isLastInstanceOnNode, intf);

        if (intf.getType().equalsIgnoreCase("vxlan") || intf.getType().equalsIgnoreCase("gre")) {
            /* delete tunnel interfaces */
            ProviderNetworkManager.getManager().handleInterfaceDelete(intf.getType(), null, node, intf, isLastInstanceOnNode);
        } else if (network != null) {
            if (!network.getProviderNetworkType().equalsIgnoreCase("vlan")) { /* vlan doesn't need a tunnel endpoint */
                if (AdminConfigManager.getManager().getTunnelEndPoint(node) == null) {
                    logger.error("Tunnel end-point configuration missing. Please configure it in Open_vSwitch Table");
                    return;
                }
            }
            if (isLastInstanceOnNode & ProviderNetworkManager.getManager().hasPerTenantTunneling()) {
                TenantNetworkManager.getManager().reclaimTennantNetworkInternalVlan(node, uuid, network);
            }
            ProviderNetworkManager.getManager().handleInterfaceDelete(network.getProviderNetworkType(), network, node, intf, isLastInstanceOnNode);
        }
    }

    private String getPortIdForInterface (Node node, String uuid, Interface intf) {
        try {
            Map<String, Table<?>> ports = this.ovsdbConfigService.getRows(node, Port.NAME.getName());
            if (ports == null) return null;
            for (String portUUID : ports.keySet()) {
                Port port = (Port)ports.get(portUUID);
                Set<UUID> interfaceUUIDs = port.getInterfaces();
                logger.trace("Scanning Port {} to identify interface : {} ",port, uuid);
                for (UUID intfUUID : interfaceUUIDs) {
                    if (intfUUID.toString().equalsIgnoreCase(uuid)) {
                        logger.trace("Found Interface {} -> {}", uuid, portUUID);
                        return portUUID;
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Failed to add Port tag for for Intf {}",intf, e);
        }
        return null;
    }

    @Override
    public void notifyNode(Node node, UpdateType type, Map<String, Property> propMap) {
        logger.debug("notifyNode: Node {} update {} from Controller's inventory Service", node, type);

        // Add the Node Type check back once the Consistency issue is resolved between MD-SAL and AD-SAL
        if (!type.equals(UpdateType.REMOVED) && !nodeCache.contains(node)) {
            nodeCache.add(node);
            ProviderNetworkManager.getManager().initializeOFFlowRules(node);
        } else if (type.equals(UpdateType.REMOVED)){
            nodeCache.remove(node);
        }
    }

    @Override
    public void notifyNodeConnector(NodeConnector nodeConnector, UpdateType type, Map<String, Property> propMap) {
        //We are not interested in the nodeConnectors at this moment
    }

    private void triggerUpdates() {
        List<Node> nodes = this.getConnectionService().getNodes();
        if (nodes == null) return;
        for (Node node : nodes) {
            try {
                List<String> tableNames = this.getOVSDBConfigService().getTables(node);
                if (tableNames == null) continue;
                for (String tableName : tableNames) {
                    Map<String, Table<?>> rows = this.getOVSDBConfigService().getRows(node, tableName);
                    if (rows == null) continue;
                    for (String uuid : rows.keySet()) {
                        Table<?> row = rows.get(uuid);
                        this.rowAdded(node, tableName, uuid, row);
                    }
                }
            } catch (Exception e) {
                logger.error("Exception during OVSDB Southbound update trigger", e);
            }
        }
    }
}
