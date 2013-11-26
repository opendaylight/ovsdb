package org.opendaylight.ovsdb.neutron;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.opendaylight.controller.networkconfig.neutron.NeutronNetwork;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.table.Interface;
import org.opendaylight.ovsdb.lib.table.Open_vSwitch;
import org.opendaylight.ovsdb.lib.table.Port;
import org.opendaylight.ovsdb.lib.table.internal.Table;
import org.opendaylight.ovsdb.neutron.provider.ProviderNetworkManager;
import org.opendaylight.ovsdb.plugin.OVSDBInventoryListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SouthboundHandler extends BaseHandler implements OVSDBInventoryListener {
    static final Logger logger = LoggerFactory.getLogger(SouthboundHandler.class);
    private Thread eventThread;
    private BlockingQueue<SouthboundEvent> events;

    void init() {
        eventThread = new Thread(new EventHandler(), "SouthBound Event Thread");
        this.events = new LinkedBlockingQueue<SouthboundEvent>();
    }

    void start() {
        eventThread.start();
    }

    void stop() {
        eventThread.interrupt();
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
    public void rowUpdated(Node node, String tableName, String uuid, Table<?> row) {
        this.enqueueEvent(new SouthboundEvent(node, tableName, uuid, row, SouthboundEvent.Action.UPDATE));
    }

    @Override
    public void rowRemoved(Node node, String tableName, String uuid, Table<?> row) {
        this.enqueueEvent(new SouthboundEvent(node, tableName, uuid, row, SouthboundEvent.Action.DELETE));
    }

    private void enqueueEvent (SouthboundEvent event) {
        try {
            events.put(event);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
    private class EventHandler implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    SouthboundEvent ev = events.take();
                    switch (ev.getType()) {
                    case NODE:
                        ProcessNodeUpdate(ev.getNode(), ev.getAction());
                    case ROW:
                        ProcessRowUpdate(ev.getNode(), ev.getTableName(), ev.getUuid(), ev.getRow(), ev.getAction());
                        break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void ProcessNodeUpdate(Node node, SouthboundEvent.Action action) {
        if (action == SouthboundEvent.Action.DELETE) return;
        logger.trace("Process Node added {}", node);
        InternalNetworkManager.getManager().prepareInternalNetwork(node);
    }

    private void ProcessRowUpdate(Node node, String tableName, String uuid, Table<?> row,
                                  SouthboundEvent.Action action) {
        if (action == SouthboundEvent.Action.DELETE) return;

        if (Interface.NAME.getName().equalsIgnoreCase(tableName)) {
            logger.debug("trace {} Added / Updated {} , {}, {}", tableName, node, uuid, row);
            Interface intf = (Interface)row;
            NeutronNetwork network = TenantNetworkManager.getManager().getTenantNetworkForInterface(intf);
            if (network != null) {
                int vlan = TenantNetworkManager.getManager().networkCreated(network.getID());
                logger.trace("Neutron Network {} Created with Internal Vlan : {}", network.toString(), vlan);

                String portUUID = this.getPortIdForInterface(node, uuid, intf);
                if (portUUID != null) {
                    TenantNetworkManager.getManager().programTenantNetworkInternalVlan(node, portUUID, network);
                }
                this.createTunnels(node, uuid, intf);
            }
        } else if (Port.NAME.getName().equalsIgnoreCase(tableName)) {
            logger.debug("trace {} Added / Updated {} , {}, {}", tableName, node, uuid, row);
            Port port = (Port)row;
            Set<UUID> interfaceUUIDs = port.getInterfaces();
            for (UUID intfUUID : interfaceUUIDs) {
                logger.trace("Scanning interface "+intfUUID);
                try {
                    Interface intf = (Interface)this.ovsdbConfigService.getRow(node, Interface.NAME.getName(), intfUUID.toString());
                    NeutronNetwork network = TenantNetworkManager.getManager().getTenantNetworkForInterface(intf);
                    if (network != null) {
                        TenantNetworkManager.getManager().programTenantNetworkInternalVlan(node, uuid, network);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else if (Open_vSwitch.NAME.getName().equalsIgnoreCase(tableName)) {
            logger.debug("trace {} Added / Updated {} , {}, {}", tableName, node, uuid, row);
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
                logger.error("Error fetching Interface Rows for node {}", node);
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
}
