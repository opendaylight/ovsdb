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

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.opendaylight.ovsdb.lib.notation.OvsDBMap;
import org.opendaylight.ovsdb.lib.notation.OvsDBSet;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.table.Bridge;
import org.opendaylight.ovsdb.lib.table.Interface;
import org.opendaylight.ovsdb.lib.table.Port;
import org.opendaylight.ovsdb.lib.table.internal.Table;
import org.opendaylight.ovsdb.neutron.provider.ProviderNetworkManager;
import org.opendaylight.ovsdb.plugin.IConnectionServiceInternal;
import org.opendaylight.ovsdb.plugin.OVSDBConfigService;
import org.opendaylight.ovsdb.plugin.StatusWithUuid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OpenStack Neutron with the OpenVswitch data plan relies on a typical OVS bridge configurations that
 * consists of br-int (Integration Bridge), br-tun (Tunnel bridge), br-ex (External bridge).
 *
 * In DevStack like setups, the br-tun is not automatically created on the controller nodes.
 * Hence this class attempts to bring all the nodes to be elibible for OpenStack operations.
 *
 */
public class InternalNetworkManager {
    static final Logger logger = LoggerFactory.getLogger(InternalNetworkManager.class);
    private static final int LLDP_PRIORITY = 1000;
    private static final int NORMAL_PRIORITY = 0;

    private static InternalNetworkManager internalNetwork = new InternalNetworkManager();
    private InternalNetworkManager() {
    }

    public static InternalNetworkManager getManager() {
        return internalNetwork;
    }

    public String getInternalBridgeUUID (Node node, String bridgeName) {
        try {
            OVSDBConfigService ovsdbTable = (OVSDBConfigService)ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);
            Map<String, Table<?>> bridgeTable = ovsdbTable.getRows(node, Bridge.NAME.getName());
            if (bridgeTable == null) return null;
            for (String key : bridgeTable.keySet()) {
                Bridge bridge = (Bridge)bridgeTable.get(key);
                if (bridge.getName().equals(bridgeName)) return key;
            }
        } catch (Exception e) {
            logger.error("Error getting Bridge Identifier for {} / {}", node, bridgeName, e);
        }
        return null;
    }

    public Bridge getInternalBridge (Node node, String bridgeName) {
        try {
            OVSDBConfigService ovsdbTable = (OVSDBConfigService)ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);
            Map<String, Table<?>> bridgeTable = ovsdbTable.getRows(node, Bridge.NAME.getName());
            if (bridgeTable == null) return null;
            for (String key : bridgeTable.keySet()) {
                Bridge bridge = (Bridge)bridgeTable.get(key);
                if (bridge.getName().equals(bridgeName)) return bridge;
            }
        } catch (Exception e) {
            logger.error("Error getting Bridge Identifier for {} / {}", node, bridgeName, e);
        }
        return null;
    }

    public boolean isInternalNetworkNeutronReady(Node node) {
        if (this.getInternalBridgeUUID(node, AdminConfigManager.getManager().getIntegrationBridgeName()) != null) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isInternalNetworkOverlayReady(Node node) {
        if (!this.isInternalNetworkNeutronReady(node)) {
            return false;
        }
        if (this.getInternalBridgeUUID(node, AdminConfigManager.getManager().getTunnelBridgeName()) != null) {
            return true;
        } else {
            return false;
        }
    }

    /* Determine if internal network is ready for vlan network type
     * This includes bridge domain creation and physical interface ethx added to bridge domain */
    public boolean isInternalNetworkVlanReady(Node node) {
        OVSDBConfigService ovsdbTable = (OVSDBConfigService)ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);

        /* is br-int created */
        Bridge intBridge = this.getInternalBridge(node, AdminConfigManager.getManager().getIntegrationBridgeName());
        if (intBridge == null) {
            return false;
        }

        if (ProviderNetworkManager.getManager().hasPerTenantTunneling()) {
            /* is br-eth1 created?*/
            /* TODO: change getPhysicalBridgeName to getOverlayBridgeName once we use br-overlay for both br-tun and br-eth1 */
            Bridge physicalBridge = this.getInternalBridge(node, AdminConfigManager.getManager().getPhysicalBridgeName());
            if (physicalBridge == null) {
                return false;
            }

            /* Check if ethx is added to br-overlay */
            for (UUID portsUUID:physicalBridge.getPorts()) {
                try {
                    Port port = (Port)ovsdbTable.getRow(node, Port.NAME.getName(), portsUUID.toString());
                    if (port.getName().startsWith("eth")) {
                        return true;
                    }
                } catch (Exception e) {
                    logger.error("Error getting port {} for bridge domain {}/{}", portsUUID.toString(), node,
                    physicalBridge.getName(), e);
                }
            }
        } else {
            /* Check if ethx is added to br-int */
            for (UUID portsUUID:intBridge.getPorts()) {
                try {
                    Port port = (Port)ovsdbTable.getRow(node, Port.NAME.getName(), portsUUID.toString());
                    if (port.getName().startsWith("eth")) {
                        return true;
                    }
                } catch (Exception e) {
                    logger.error("Error getting port {} for bridge domain {}/{}", portsUUID.toString(), node,
                    intBridge.getName(), e);
                }
            }
        }

        return false;
    }

    /* Determine if internal network is ready for tunnel network type
     * This includes bridge domain creation and interface creation for patch-int and patch-tun */
    public boolean isInternalNetworkTunnelReady(Node node) {
        OVSDBConfigService ovsdbTable = (OVSDBConfigService)ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);

        /* is br-int created */
        Bridge intBridge = this.getInternalBridge(node, AdminConfigManager.getManager().getIntegrationBridgeName());
        if (intBridge == null) {
            return false;
        }

        if (ProviderNetworkManager.getManager().hasPerTenantTunneling()) {
            /* is br-tun created?*/
            /* TODO: change getPhysicalBridgeName to getOverlayBridgeName once we use br-overlay for both br-tun and br-eth1 */
            Bridge tunnelBridge = this.getInternalBridge(node, AdminConfigManager.getManager().getTunnelBridgeName());
            if (tunnelBridge == null) {
                return false;
            }

            boolean isPatchTunAdd = false;
            /* Check if patch-tun is added to br-int */
            for (UUID portsUUID:intBridge.getPorts()) {
                try {
                    Port port = (Port)ovsdbTable.getRow(node, Port.NAME.getName(), portsUUID.toString());
                    if (port.getName().equalsIgnoreCase(AdminConfigManager.getManager().getPatchToTunnel())) {
                        isPatchTunAdd = true;
                        break;
                    }
                } catch (Exception e) {
                    logger.error("Error getting port {} for bridge domain {}/{}", portsUUID.toString(), node,
                    intBridge.getName(), e);
                }
            }
            if (!isPatchTunAdd) {
                return false;
            }
            /* check if patch-int is added to br-tun */
            for (UUID portsUUID:tunnelBridge.getPorts()) {
                try {
                    Port port = (Port)ovsdbTable.getRow(node, Port.NAME.getName(), portsUUID.toString());
                    if (port.getName().equalsIgnoreCase(AdminConfigManager.getManager().getPatchToIntegration())) {
                        return true;
                    }
                } catch (Exception e) {
                    logger.error("Error getting port {} for bridge domain {}/{}", portsUUID.toString(), node,
                    intBridge.getName(), e);
                }
            }
        }

        return false;
    }

    /*
     * Lets create these if not already present :
     *
       Bridge br-int
            Port patch-tun
                Interface patch-tun
                    type: patch
                    options: {peer=patch-int}
            Port br-int
                Interface br-int
                    type: internal
      Bridge br-tun
            Port patch-int
                Interface patch-int
                    type: patch
                    options: {peer=patch-tun}
            Port br-tun
                Interface br-tun
                    type: internal
     */
    public void createInternalNetworkForOverlay(Node node) throws Exception {
        String brTun = AdminConfigManager.getManager().getTunnelBridgeName();
        String brInt = AdminConfigManager.getManager().getIntegrationBridgeName();
        String patchInt = AdminConfigManager.getManager().getPatchToIntegration();
        String patchTun = AdminConfigManager.getManager().getPatchToTunnel();

        Status status = this.addInternalBridge(node, brInt, patchTun, patchInt);
        if (!status.isSuccess()) logger.debug("Integration Bridge Creation Status : "+status.toString());
        if (ProviderNetworkManager.getManager().hasPerTenantTunneling()) {
            status = this.addInternalBridge(node, brTun, patchInt, patchTun);
            if (!status.isSuccess()) logger.debug("Tunnel Bridge Creation Status : "+status.toString());
        }
    }

    /*
     * Lets create these if not already present :
     *
       Bridge br-int
            Port br-int
                Interface br-int
                    type: internal
     */
    public void createInternalNetworkForNeutron(Node node) throws Exception {
        String brInt = AdminConfigManager.getManager().getIntegrationBridgeName();

        Status status = this.addInternalBridge(node, brInt, null, null);
        if (!status.isSuccess()) logger.debug("Integration Bridge Creation Status : "+status.toString());
    }

    /*
     * Create physical network for Neutron VLAN network type for OF 1.0. For OF1.3, eth1 is add to br-int directly
     *
       Bridge br-int
            Port "int-br-eth1"
                Interface "int-br-eth1"
            Port br-int
                Interface br-int
                    type: internal
       Bridge "br-eth1"
            Port "eth1"
                Interface "eth1"
            Port "phy-br-eth1"
                Interface "phy-br-eth1"
            Port "br-eth1"
                Interface "br-eth1"
                    type: internal

     */
    public void createPhysicalNetworkForNeutron(Node node) throws Exception {
        String ethInf;
        OVSDBConfigService ovsdbTable = (OVSDBConfigService)ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);
        String bridgeUUID;
        Port   ethPort = new Port();
        Status  status;
        String bridgeName;

        ethPort.setName(AdminConfigManager.getManager().getEthIntfName(node));
        if (ProviderNetworkManager.getManager().hasPerTenantTunneling()) {
            bridgeName = AdminConfigManager.getManager().getPhysicalBridgeName();
            bridgeUUID = this.getInternalBridgeUUID(node, bridgeName);
        } else {
            /* For OF 1.3, eth1 is added to br-int */
            bridgeName = AdminConfigManager.getManager().getIntegrationBridgeName();
            bridgeUUID = this.getInternalBridgeUUID(node, bridgeName);
        }

        status = ovsdbTable.insertRow(node, Port.NAME.getName(), bridgeUUID, ethPort);
        if (!status.isSuccess()) {
            logger.debug("Add port {} to bridge {} Status : {}", ethPort, bridgeName, status.toString());
        }
        return;
    }

    private Status addInternalBridge (Node node, String bridgeName, String localPathName, String remotePatchName) throws Exception {
        OVSDBConfigService ovsdbTable = (OVSDBConfigService)ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);

        String bridgeUUID = this.getInternalBridgeUUID(node, bridgeName);
        Bridge bridge = new Bridge();
        OvsDBSet<String> failMode = new OvsDBSet<String>();
        failMode.add("secure");
        bridge.setFail_mode(failMode);

        OvsDBSet<String> protocols = new OvsDBSet<String>();
        if (!ProviderNetworkManager.getManager().hasPerTenantTunneling()) {
            protocols.add("OpenFlow13");
        } else {
            protocols.add("OpenFlow10");
        }
        bridge.setProtocols(protocols);

        if (bridgeUUID == null) {
            bridge.setName(bridgeName);

            StatusWithUuid statusWithUuid = ovsdbTable.insertRow(node, Bridge.NAME.getName(), null, bridge);
            if (!statusWithUuid.isSuccess()) return statusWithUuid;
            bridgeUUID = statusWithUuid.getUuid().toString();
            Port port = new Port();
            port.setName(bridgeName);
            Status status = ovsdbTable.insertRow(node, Port.NAME.getName(), bridgeUUID, port);
            logger.debug("addInternalBridge : Inserting Bridge {} with protocols {} and status {}", bridgeUUID, protocols, status);
        } else {
            Status status = ovsdbTable.updateRow(node, Bridge.NAME.getName(), null, bridgeUUID, bridge);
            logger.debug("addInternalBridge : Updating Bridge {} with protocols {} and status {}", bridgeUUID, protocols, status);
        }

        IConnectionServiceInternal connectionService = (IConnectionServiceInternal)ServiceHelper.getGlobalInstance(IConnectionServiceInternal.class, this);
        connectionService.setOFController(node, bridgeUUID);

        if (localPathName != null && remotePatchName != null && ProviderNetworkManager.getManager().hasPerTenantTunneling()) {
            return addPatchPort(node, bridgeUUID, localPathName, remotePatchName);
        }
        return new Status(StatusCode.SUCCESS);
    }

    private Status addPatchPort (Node node, String bridgeUUID, String portName, String patchName) throws Exception {
        OVSDBConfigService ovsdbTable = (OVSDBConfigService)ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);

        Port patchPort = new Port();
        patchPort.setName(portName);
        // Create patch port and interface
        StatusWithUuid statusWithUuid = ovsdbTable.insertRow(node, Port.NAME.getName(), bridgeUUID, patchPort);
        if (!statusWithUuid.isSuccess()) return statusWithUuid;

        String patchPortUUID = statusWithUuid.getUuid().toString();

        String interfaceUUID = null;
        int timeout = 6;
        while ((interfaceUUID == null) && (timeout > 0)) {
            patchPort = (Port)ovsdbTable.getRow(node, Port.NAME.getName(), patchPortUUID);
            OvsDBSet<UUID> interfaces = patchPort.getInterfaces();
            if (interfaces == null || interfaces.size() == 0) {
                // Wait for the OVSDB update to sync up the Local cache.
                Thread.sleep(500);
                timeout--;
                continue;
            }
            interfaceUUID = interfaces.toArray()[0].toString();
        }

        if (interfaceUUID == null) {
            return new Status(StatusCode.INTERNALERROR);
        }

        Interface tunInterface = new Interface();
        tunInterface.setType("patch");
        OvsDBMap<String, String> options = new OvsDBMap<String, String>();
        options.put("peer", patchName);
        tunInterface.setOptions(options);
        return ovsdbTable.updateRow(node, Interface.NAME.getName(), patchPortUUID, interfaceUUID, tunInterface);
    }

    public void prepareInternalNetwork(Node node) {
        try {
            this.createInternalNetworkForNeutron(node);
            /* FIXME hshen: remove following line after complete testing for bridge domain creation
               this.createInternalNetworkForOverlay(node);
               this.createPhysicalNetworkForNeutron(node);
               DEBUG: hshen */
            logger.info("Finish create physical network");
        } catch (Exception e) {
            logger.error("Error creating internal network "+node.toString(), e);
        }
        ProviderNetworkManager.getManager().initializeFlowRules(node);
    }
}
