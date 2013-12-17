/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Brent Salisbury
 */
package org.opendaylight.ovsdb.neutron;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
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

    void init() {
        eventHandler = Executors.newSingleThreadExecutor();
        this.events = new LinkedBlockingQueue<SouthboundEvent>();
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
                            processRowUpdate(ev.getNode(), ev.getTableName(), ev.getUuid(), ev.getRow(), ev.getAction());
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
        if (this.isUpdateOfInterest(oldRow, newRow)) {
            this.enqueueEvent(new SouthboundEvent(node, tableName, uuid, newRow, SouthboundEvent.Action.UPDATE));
        }
    }

    /*
     * Ignore unneccesary updates to be even considered for processing.
     * (Especially stats update are fast and furious).
     */

    private boolean isUpdateOfInterest(Table<?> oldRow, Table<?> newRow) {
        if (oldRow == null) return true;
        if (newRow.getTableName().equals(Interface.NAME)) {
            // We are NOT interested in Stats only updates
            Interface oldIntf = (Interface)oldRow;
            if (oldIntf.getName() == null && oldIntf.getExternal_ids() == null && oldIntf.getMac() == null &&
                oldIntf.getOfport() == null && oldIntf.getOptions() == null && oldIntf.getOther_config() == null &&
                oldIntf.getType() == null) {
                logger.trace("IGNORING Interface Update : "+newRow.toString());
                return false;
            }
        } else if (newRow.getTableName().equals(Port.NAME)) {
            // We are NOT interested in Stats only updates
            Port oldPort = (Port)oldRow;
            if (oldPort.getName() == null && oldPort.getExternal_ids() == null && oldPort.getMac() == null &&
                oldPort.getInterfaces() == null && oldPort.getTag() == null && oldPort.getTrunks() == null) {
                logger.trace("IGNORING Port Update : "+newRow.toString());
                return false;
            }
        }

        return true;
    }

    @Override
    public void rowRemoved(Node node, String tableName, String uuid, Table<?> row) {
        this.enqueueEvent(new SouthboundEvent(node, tableName, uuid, row, SouthboundEvent.Action.DELETE));
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
                                  SouthboundEvent.Action action) {
        if (action == SouthboundEvent.Action.DELETE) return;

        if (Interface.NAME.getName().equalsIgnoreCase(tableName)) {
            logger.debug("{} Added / Updated {} , {}, {}", tableName, node, uuid, row);
            Interface intf = (Interface)row;
            NeutronNetwork network = TenantNetworkManager.getManager().getTenantNetworkForInterface(intf);
            if (network != null && !network.getRouterExternal()) {
                int vlan = TenantNetworkManager.getManager().networkCreated(network.getID());
                logger.trace("Neutron Network {} Created with Internal Vlan : {}", network.toString(), vlan);

                String portUUID = this.getPortIdForInterface(node, uuid, intf);
                if (portUUID != null) {
                    TenantNetworkManager.getManager().programTenantNetworkInternalVlan(node, portUUID, network);
                }
                this.createTunnels(node, uuid, intf);
            }
        } else if (Port.NAME.getName().equalsIgnoreCase(tableName)) {
            logger.debug("{} Added / Updated {} , {}, {}", tableName, node, uuid, row);
            Port port = (Port)row;
            Set<UUID> interfaceUUIDs = port.getInterfaces();
            for (UUID intfUUID : interfaceUUIDs) {
                logger.trace("Scanning interface "+intfUUID);
                try {
                    Interface intf = (Interface)this.ovsdbConfigService.getRow(node, Interface.NAME.getName(), intfUUID.toString());
                    NeutronNetwork network = TenantNetworkManager.getManager().getTenantNetworkForInterface(intf);
                    if (network != null && !network.getRouterExternal()) {
                        TenantNetworkManager.getManager().programTenantNetworkInternalVlan(node, uuid, network);
                    }
                } catch (Exception e) {
                    logger.error("Failed to process row update", e);
                }
            }
        } else if (Open_vSwitch.NAME.getName().equalsIgnoreCase(tableName)) {
            logger.debug("{} Added / Updated {} , {}, {}", tableName, node, uuid, row);
            AdminConfigManager.getManager().populateTunnelEndpoint(node);
            try {
                Map<String, Table<?>> interfaces = this.ovsdbConfigService.getRows(node, Interface.NAME.getName());
                if (interfaces != null) {
                    for (String intfUUID : interfaces.keySet()) {
                        Interface intf = (Interface) interfaces.get(intfUUID);
                        createTunnels(node, intfUUID, intf);
                    }
                }
            } catch (Exception e) {
                logger.error("Error fetching Interface Rows for node " + node, e);
            }
        }
    }

    private void createTunnels (Node node, String uuid, Interface intf) {
        if (AdminConfigManager.getManager().getTunnelEndPoint(node) == null) {
            logger.error("Tunnel end-point configuration missing. Please configure it in Open_vSwitch Table");
            return;
        }
        NeutronNetwork network = TenantNetworkManager.getManager().getTenantNetworkForInterface(intf);
        if (network != null) {
            ProviderNetworkManager.getManager().createTunnels(network.getProviderNetworkType(),
                    network.getProviderSegmentationID(), node, intf);
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
                        logger.trace("Found Interafce {} -> {}", uuid, portUUID);
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
        if (node.getType().equals(Node.NodeIDType.OPENFLOW) && type.equals(UpdateType.ADDED)) {
            logger.debug("OpenFlow node {} added. Initialize Basic flows", node);
            ProviderNetworkManager.getManager().initializeOFFlowRules(node);
        }
    }

    @Override
    public void notifyNodeConnector(NodeConnector nodeConnector, UpdateType type, Map<String, Property> propMap) {
        //We are not interested in the nodeConnectors at this moment
    }
}
