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

import java.util.Map;

import org.opendaylight.controller.networkconfig.neutron.NeutronNetwork;
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
 * OpenStack Neutron with the OpenvSwitch data plan relies on a typical OVS bridge configurations that
 * consists of br-int (Integration Bridge), br-tun (Tunnel bridge), br-ex (External bridge).
 *
 * In DevStack like setups, the br-tun is not automatically created on the controller nodes.
 * Hence this class attempts to bring all the nodes to be eligible for OpenStack operations.
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
            OVSDBConfigService ovsdbTable = (OVSDBConfigService) ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);
            Map<String, Table<?>> bridgeTable = ovsdbTable.getRows(node, Bridge.NAME.getName());
            if (bridgeTable != null) {
                for (String key : bridgeTable.keySet()) {
                    Bridge bridge = (Bridge) bridgeTable.get(key);
                    if (bridge.getName().equals(bridgeName)) {
                        return bridge;
                    }
                }
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
        if (this.getInternalBridgeUUID(node, AdminConfigManager.getManager().getNetworkBridgeName()) != null) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isPortOnBridge (Node node, Bridge bridge, String portName) {
        OVSDBConfigService ovsdbTable = (OVSDBConfigService) ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);

        for (UUID portsUUID : bridge.getPorts()) {
            try {
                Port port = (Port) ovsdbTable.getRow(node, Port.NAME.getName(), portsUUID.toString());
                if ((port != null) && port.getName().equalsIgnoreCase(portName)) {
                    return true;
                }
            } catch (Exception e) {
                logger.error("Error getting port {} for bridge domain {}/{}", portsUUID, node, bridge.getName(), e);
            }
        }

        return false;
    }

    public boolean isNetworkPatchCreated (Node node, Bridge intBridge, Bridge netBridge) {
        boolean isPatchCreated = false;

        String portName = AdminConfigManager.getManager().getPatchToNetwork();
        if (isPortOnBridge(node, intBridge, portName)) {
            portName = AdminConfigManager.getManager().getPatchToIntegration();
            if (isPortOnBridge(node, netBridge, portName)) {
                isPatchCreated = true;
            }
        }

        return isPatchCreated;
    }

    /* Determine if internal network is ready for tunnel network types.
     * - OF 1.0 requires br-int, br-net and a patch connecting them.
     * - OF 1.3 requires br-int.
     */
    public boolean isInternalNetworkTunnelReady (Node node) {
        /* Is br-int created? */
        Bridge intBridge = this.getInternalBridge(node, AdminConfigManager.getManager().getIntegrationBridgeName());
        if (intBridge == null) {
            return false;
        }

        if (ProviderNetworkManager.getManager().hasPerTenantTunneling()) {
            /* Is br-net created? */
            Bridge netBridge = this.getInternalBridge(node, AdminConfigManager.getManager().getNetworkBridgeName());
            if (netBridge == null) {
                return false;
            }

            if (!isNetworkPatchCreated(node, intBridge, netBridge)) {
                return false;
            }
        }
        return true;
    }

    /* Determine if internal network is ready for vlan network types.
     * - OF 1.0 requires br-int, br-net, a patch connecting them and
     * physical device added to br-net.
     * - OF 1.3 requires br-int and physical device added to br-int.
     */
    public boolean isInternalNetworkVlanReady (Node node, NeutronNetwork network) {
        /* is br-int created */
        Bridge intBridge = this.getInternalBridge(node, AdminConfigManager.getManager().getIntegrationBridgeName());
        if (intBridge == null) {
            logger.trace("shague isInternalNetworkVlanReady: node: {}, br-int missing", node);
            return false;
        }

        if (ProviderNetworkManager.getManager().hasPerTenantTunneling()) {
            /* is br-net created? */
            Bridge netBridge = this.getInternalBridge(node, AdminConfigManager.getManager().getNetworkBridgeName());

            if (netBridge == null) {
                logger.trace("shague isInternalNetworkVlanReady: node: {}, br-net missing", node);
                return false;
            }

            if (!isNetworkPatchCreated(node, intBridge, netBridge)) {
                logger.trace("shague isInternalNetworkVlanReady: node: {}, patch missing", node);
                return false;
            }

            /* Check if physical device is added to br-net. */
            String phyNetName = AdminConfigManager.getManager().getPhysicalInterfaceName(node, network.getProviderPhysicalNetwork());
            if (isPortOnBridge(node, netBridge, phyNetName)) {
                return true;
            }
        } else {
            /* Check if physical device is added to br-int. */
            String phyNetName = AdminConfigManager.getManager().getPhysicalInterfaceName(node, network.getProviderPhysicalNetwork());
            if (isPortOnBridge(node, intBridge, phyNetName)) {
                return true;
            }
        }

        logger.trace("shague isInternalNetworkVlanReady: node: {}, eth missing", node);
        return false;
    }

    /*
     * Create the integration bridge.
     *
       Bridge br-int
            Port br-int
                Interface br-int
                    type: internal
     */
    public void createIntegrationBridge (Node node) throws Exception {
        String brInt = AdminConfigManager.getManager().getIntegrationBridgeName();

        Status status = this.addInternalBridge(node, brInt, null, null);
        if (!status.isSuccess()) {
            logger.debug("Integration Bridge Creation Status: {}", status);
        }
    }

    /*
     * Create complete network for all network types and OpenFlow versions.
     *
       OF 1.0 vlan:
       Bridge br-int
            Port patch-net
                Interface patch-net
                    type: patch
                    options: {peer=patch-int}
            Port br-int
                Interface br-int
                    type: internal
       Bridge br-net
            Port "eth1"
                Interface "eth1"
            Port patch-int
                Interface patch-int
                    type: patch
                    options: {peer=patch-net}
            Port br-net
                Interface br-net
                    type: internal

       OF 1.0 tunnel:
       Bridge br-int
            Port patch-net
                Interface patch-net
                    type: patch
                    options: {peer=patch-int}
            Port br-int
                Interface br-int
                    type: internal
       Bridge "br-net"
            Port patch-int
                Interface patch-int
                    type: patch
                    options: {peer=patch-net}
            Port br-net
                Interface br-net
                    type: internal

       OF 1.3 vlan:
       Bridge br-int
            Port "eth1"
                Interface "eth1"
            Port br-int
                Interface br-int
                    type: internal

       OF 1.3 tunnel:
       Bridge br-int
            Port br-int
                Interface br-int
                    type: internal
     */
    public boolean createNetNetwork (Node node, NeutronNetwork network) throws Exception {
        Status status;

        logger.debug("createNetNetwork: node: {}, network type: {}", node, network.getProviderNetworkType());

        if (ProviderNetworkManager.getManager().hasPerTenantTunneling()) { /* indicates OF 1.0 */
            String brInt = AdminConfigManager.getManager().getIntegrationBridgeName();
            String brNet = AdminConfigManager.getManager().getNetworkBridgeName();
            String patchNet = AdminConfigManager.getManager().getPatchToNetwork();
            String patchInt = AdminConfigManager.getManager().getPatchToIntegration();

            status = this.addInternalBridge(node, brInt, patchNet, patchInt);
            if (!status.isSuccess()) {
                logger.debug("{} Bridge Creation Status: {}", brInt, status);
                return false;
            }
            status = this.addInternalBridge(node, brNet, patchInt, patchNet);
            if (!status.isSuccess()) {
                logger.debug("{} Bridge Creation Status: {}", brNet, status);
                return false;
            }

            /* For vlan network types add physical port to br-net. */
            if (network.getProviderNetworkType().equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_VLAN)) {
                String phyNetName = AdminConfigManager.getManager().getPhysicalInterfaceName(node, network.getProviderPhysicalNetwork());
                status = addPortToBridge(node, brNet, phyNetName);
                if (!status.isSuccess()) {
                    logger.debug("Add Port {} to Bridge {} Status: {}", phyNetName, brNet, status);
                    return false;
                }
            }
        } else {
            String brInt = AdminConfigManager.getManager().getIntegrationBridgeName();
            status = this.addInternalBridge(node, brInt, null, null);
            if (!status.isSuccess()) {
                logger.debug("{} Bridge Creation Status: {}", brInt, status);
                return false;
            }

            /* For vlan network types add physical port to br-int. */
            if (network.getProviderNetworkType().equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_VLAN)) {
                String phyNetName = AdminConfigManager.getManager().getPhysicalInterfaceName(node, network.getProviderPhysicalNetwork());
                status = addPortToBridge(node, brInt, phyNetName);
                if (!status.isSuccess()) {
                    logger.debug("Add Port {} to Bridge {} Status: {}", phyNetName, brInt, status);
                    return false;
                }
            }
        }

        logger.debug("createNetNetwork: node: {}, status: success", node);
        return true;
    }

    private Status addPortToBridge (Node node, String bridgeName, String portName) throws Exception {
        logger.debug("addPortToBridge: Adding port: {} to Bridge {}, Node {}", portName, bridgeName, node);
        OVSDBConfigService ovsdbTable = (OVSDBConfigService)ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);

        String bridgeUUID = this.getInternalBridgeUUID(node, bridgeName);
        if (bridgeUUID == null) {
            logger.error("addPortToBridge: Could not find Bridge {} in Node {}", bridgeName, node);
            return new Status(StatusCode.NOTFOUND, "Could not find "+bridgeName+" in "+node);
        }

        /* Check if the port already exists. */
        Bridge bridge = (Bridge)ovsdbTable.getRow(node, Bridge.NAME.getName(), bridgeUUID);
        if (bridge != null) {
            if (isPortOnBridge(node, bridge, portName)) {
                logger.debug("addPortToBridge: Port {} already in Bridge {}, Node {}", portName, bridgeName, node);
                return new Status(StatusCode.SUCCESS);
            }
        } else {
            logger.error("addPortToBridge: Could not find Port {} in Bridge {}, Node {}", portName, bridgeName, node);
            return new Status(StatusCode.NOTFOUND, "Could not find "+portName+" in "+bridgeName);
        }

        Port port = new Port();
        port.setName(portName);
        StatusWithUuid statusWithUuid = ovsdbTable.insertRow(node, Port.NAME.getName(), bridgeUUID, port);
        if (!statusWithUuid.isSuccess()) {
            logger.error("addPortToBridge: Failed to add Port {} in Bridge {}, Node {}", portName, bridgeName, node);
            return statusWithUuid;
        }

        String portUUID = statusWithUuid.getUuid().toString();
        String interfaceUUID = null;
        int timeout = 6;
        while ((interfaceUUID == null) && (timeout > 0)) {
            port = (Port)ovsdbTable.getRow(node, Port.NAME.getName(), portUUID);
            OvsDBSet<UUID> interfaces = port.getInterfaces();
            if (interfaces == null || interfaces.size() == 0) {
                // Wait for the OVSDB update to sync up the Local cache.
                Thread.sleep(500);
                timeout--;
                continue;
            }
            interfaceUUID = interfaces.toArray()[0].toString();
            Interface intf = (Interface)ovsdbTable.getRow(node, Interface.NAME.getName(), interfaceUUID);
            if (intf == null) {
                interfaceUUID = null;
            }
        }

        if (interfaceUUID == null) {
            logger.error("addPortToBridge: Cannot identify Interface for port {}/{}", portName, portUUID);
            return new Status(StatusCode.INTERNALERROR);
        }

        return new Status(StatusCode.SUCCESS);
    }

    private Status addPatchPort (Node node, String bridgeUUID, String portName, String peerPortName) throws Exception {
        OVSDBConfigService ovsdbTable = (OVSDBConfigService)ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);

        logger.debug("addPatchPort: node: {}, bridgeUUID: {}, port: {}, peer: {}",
                node, bridgeUUID, portName, peerPortName);

        /* Check if the port already exists. */
        Bridge bridge = (Bridge)ovsdbTable.getRow(node, Bridge.NAME.getName(), bridgeUUID);
        if (bridge != null) {
            if (isPortOnBridge(node, bridge, portName)) {
                logger.debug("addPatchPort: Port {} already in Bridge, Node {}", portName, node);
                return new Status(StatusCode.SUCCESS);
            }
        } else {
            logger.error("addPatchPort: Could not find Port {} in Bridge, Node {}", portName, node);
            return new Status(StatusCode.NOTFOUND, "Could not find "+portName+" in Bridge");
        }

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

        Interface intf = new Interface();
        intf.setType("patch");
        OvsDBMap<String, String> options = new OvsDBMap<String, String>();
        options.put("peer", peerPortName);
        intf.setOptions(options);
        return ovsdbTable.updateRow(node, Interface.NAME.getName(), patchPortUUID, interfaceUUID, intf);
    }

    private Status addInternalBridge (Node node, String bridgeName, String localPatchName, String remotePatchName) throws Exception {
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
            logger.debug("addInternalBridge: Inserting Bridge {} {} with protocols {} and status {}",
                    bridgeName, bridgeUUID, protocols, status);
        } else {
            Status status = ovsdbTable.updateRow(node, Bridge.NAME.getName(), null, bridgeUUID, bridge);
            logger.debug("addInternalBridge: Updating Bridge {} {} with protocols {} and status {}",
                    bridgeName, bridgeUUID, protocols, status);
        }

        IConnectionServiceInternal connectionService = (IConnectionServiceInternal)ServiceHelper.getGlobalInstance(IConnectionServiceInternal.class, this);
        connectionService.setOFController(node, bridgeUUID);

        if (localPatchName != null && remotePatchName != null && ProviderNetworkManager.getManager().hasPerTenantTunneling()) {
            return addPatchPort(node, bridgeUUID, localPatchName, remotePatchName);
        }
        return new Status(StatusCode.SUCCESS);
    }

    public void prepareInternalNetwork(Node node) {
        try {
            this.createIntegrationBridge(node);
        } catch (Exception e) {
            logger.error("Error creating internal network "+node.toString(), e);
            return;
        }
        ProviderNetworkManager.getManager().initializeFlowRules(node);
    }

    /*
     * Check if the full network setup is available. If not, create it.
     */
    public boolean checkAndCreateNetwork (Node node, NeutronNetwork network) {
        boolean isCreated = false;
        if (network.getProviderNetworkType().equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_VLAN)) {
            if (!InternalNetworkManager.getManager().isInternalNetworkVlanReady(node, network)) {
                try {
                    isCreated = InternalNetworkManager.getManager().createNetNetwork(node, network);
                } catch (Exception e) {
                    logger.error("Error creating internal net network ", node, e);
                }
            } else {
                isCreated = true;
            }
        } else if (network.getProviderNetworkType().equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_VXLAN) ||
                network.getProviderNetworkType().equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_GRE)) {
            if (!InternalNetworkManager.getManager().isInternalNetworkTunnelReady(node)) {
                try {
                    isCreated = InternalNetworkManager.getManager().createNetNetwork(node, network);
                } catch (Exception e) {
                    logger.error("Error creating internal net network ", node, e);
                }
            } else {
                isCreated = true;
            }
        }

        return isCreated;
    }
}