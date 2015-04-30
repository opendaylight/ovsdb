/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Brent Salisbury, Sam Hague
 */
package org.opendaylight.ovsdb.openstack.netvirt.impl;

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.opendaylight.neutron.spi.NeutronNetwork;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.openstack.netvirt.NetworkHandler;
import org.opendaylight.ovsdb.openstack.netvirt.api.*;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.Interface;
import org.opendaylight.ovsdb.schema.openvswitch.OpenVSwitch;
import org.opendaylight.ovsdb.schema.openvswitch.Port;
import org.opendaylight.ovsdb.utils.config.ConfigProperties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BridgeConfigurationManagerImpl implements BridgeConfigurationManager {
    static final Logger LOGGER = LoggerFactory.getLogger(BridgeConfigurationManagerImpl.class);

    // The implementation for each of these services is resolved by the OSGi Service Manager
    private volatile ConfigurationService configurationService;
    private volatile NetworkingProviderManager networkingProviderManager;
    private volatile OvsdbConfigurationService ovsdbConfigurationService;

    public BridgeConfigurationManagerImpl() {
    }

    @Override
    public String getBridgeUuid(Node node, String bridgeName) {
        return MdsalUtils.getBridgeUuid(node, bridgeName).toString();
    }

    @Override
    public boolean isNodeNeutronReady(Node node) {
        Preconditions.checkNotNull(configurationService);
        return this.getBridgeUuid(node, configurationService.getIntegrationBridgeName()) != null;
    }

    @Override
    public boolean isNodeOverlayReady(Node node) {
        /* TODO SB_MIGRATION */
        Preconditions.checkNotNull(ovsdbConfigurationService);
        return this.isNodeNeutronReady(node)
               && this.getBridgeUuid(node, configurationService.getNetworkBridgeName()) != null;
    }

    @Override
    public boolean isPortOnBridge (Node node, Bridge bridge, String portName) {
        /* TODO SB_MIGRATION */
        Preconditions.checkNotNull(ovsdbConfigurationService);
        for (UUID portsUUID : bridge.getPortsColumn().getData()) {
            try {
                Row portRow = ovsdbConfigurationService.getRow(node,
                                                        ovsdbConfigurationService.getTableName(node, Port.class),
                                                        portsUUID.toString());

                Port port = ovsdbConfigurationService.getTypedRow(node, Port.class, portRow);
                if ((port != null) && port.getName().equalsIgnoreCase(portName)) {
                    return true;
                }
            } catch (Exception e) {
                LOGGER.error("Error getting port {} for bridge domain {}/{}", portsUUID, node, bridge.getName(), e);
            }
        }
        return false;
    }

    @Override
    public boolean isNodeTunnelReady(Node node) {
        Preconditions.checkNotNull(configurationService);
        Preconditions.checkNotNull(networkingProviderManager);

        /* Is br-int created? */
        Bridge intBridge = this.getBridge(node, configurationService.getIntegrationBridgeName());
        if (intBridge == null) {
            return false;
        }

        if (networkingProviderManager.getProvider(node).hasPerTenantTunneling()) {
            /* Is br-net created? */
            Bridge netBridge = this.getBridge(node, configurationService.getNetworkBridgeName());
            if (netBridge == null) {
                return false;
            }

            if (!isNetworkPatchCreated(node, intBridge, netBridge)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isNodeVlanReady(Node node, NeutronNetwork network) {
        /* TODO SB_MIGRATION */
        Preconditions.checkNotNull(ovsdbConfigurationService);
        Preconditions.checkNotNull(networkingProviderManager);

        /* is br-int created */
        Bridge intBridge = this.getBridge(node, configurationService.getIntegrationBridgeName());
        if (intBridge == null) {
            LOGGER.trace("isNodeVlanReady: node: {}, br-int missing", node);
            return false;
        }

        if (networkingProviderManager.getProvider(node).hasPerTenantTunneling()) {
            /* is br-net created? */
            Bridge netBridge = this.getBridge(node, configurationService.getNetworkBridgeName());

            if (netBridge == null) {
                LOGGER.trace("isNodeVlanReady: node: {}, br-net missing", node);
                return false;
            }

            if (!isNetworkPatchCreated(node, intBridge, netBridge)) {
                LOGGER.trace("isNodeVlanReady: node: {}, patch missing", node);
                return false;
            }

            /* Check if physical device is added to br-net. */
            String phyNetName = this.getPhysicalInterfaceName(node, network.getProviderPhysicalNetwork());
            if (isPortOnBridge(node, netBridge, phyNetName)) {
                return true;
            }
        } else {
            /* Check if physical device is added to br-int. */
            String phyNetName = this.getPhysicalInterfaceName(node, network.getProviderPhysicalNetwork());
            if (isPortOnBridge(node, intBridge, phyNetName)) {
                return true;
            }
        }

        LOGGER.trace("isNodeVlanReady: node: {}, eth missing", node);
        return false;
    }

    @Override
    public void prepareNode(Node node) {
        Preconditions.checkNotNull(networkingProviderManager);

        try {
            this.createIntegrationBridge(node);
        } catch (Exception e) {
            LOGGER.error("Error creating Integration Bridge on {}", node, e);
            return;
        }
        /* TODO SB_MIGRATION
        networkingProviderManager.getProvider(node).initializeFlowRules(node);*/
    }

    /*
     * Check if the full network setup is available. If not, create it.
     */
    @Override
    public boolean createLocalNetwork (Node node, NeutronNetwork network) {
        boolean isCreated = false;
        if (network.getProviderNetworkType().equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_VLAN)) {
            if (!this.isNodeVlanReady(node, network)) {
                try {
                    isCreated = this.createBridges(node, network);
                } catch (Exception e) {
                    LOGGER.error("Error creating internal net network " + node, e);
                }
            } else {
                isCreated = true;
            }
        } else if (network.getProviderNetworkType().equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_VXLAN) ||
                   network.getProviderNetworkType().equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_GRE)) {
            if (!this.isNodeTunnelReady(node)) {
                try {
                    isCreated = this.createBridges(node, network);
                } catch (Exception e) {
                    LOGGER.error("Error creating internal net network " + node, e);
                }
            } else {
                isCreated = true;
            }
        }
        return isCreated;
    }

    @Override
    public String getPhysicalInterfaceName (Node node, String physicalNetwork) {
        String phyIf = null;
        /* TODO SB_MIGRATION */
        try {
            Map<String, Row> ovsTable =
                    ovsdbConfigurationService.getRows(node, ovsdbConfigurationService.getTableName(node, OpenVSwitch.class));

            if (ovsTable == null) {
                LOGGER.error("OpenVSwitch table is null for Node {} ", node);
                return null;
            }

            // Loop through all the Open_vSwitch rows looking for the first occurrence of other_config.
            // The specification does not restrict the number of rows so we choose the first we find.
            for (Row row : ovsTable.values()) {
                String providerMaps;
                OpenVSwitch ovsRow = ovsdbConfigurationService.getTypedRow(node, OpenVSwitch.class, row);
                Map<String, String> configs = ovsRow.getOtherConfigColumn().getData();

                if (configs == null) {
                    LOGGER.debug("OpenVSwitch table is null for Node {} ", node);
                    continue;
                }

                providerMaps = configs.get(configurationService.getProviderMappingsKey());
                if (providerMaps == null) {
                    providerMaps = configurationService.getDefaultProviderMapping();
                }

                if (providerMaps != null) {
                    for (String map : providerMaps.split(",")) {
                        String[] pair = map.split(":");
                        if (pair[0].equals(physicalNetwork)) {
                            phyIf = pair[1];
                            break;
                        }
                    }
                }

                if (phyIf != null) {
                    break;
                }
            }
        } catch (Exception e) {
            LOGGER.error("Unable to find physical interface for Node: {}, Network {}",
                         node, physicalNetwork, e);
        }

        if (phyIf == null) {
            LOGGER.error("Physical interface not found for Node: {}, Network {}",
                         node, physicalNetwork);
        }

        return phyIf;
    }

    @Override
    public List<String> getAllPhysicalInterfaceNames(Node node) {
        List<String> phyIfName = Lists.newArrayList();
        /* TODO SB_MIGRATION */
        try {
            Map<String, Row> ovsTable =
                    ovsdbConfigurationService.getRows(node, ovsdbConfigurationService.getTableName(node, OpenVSwitch.class));

            if (ovsTable == null) {
                LOGGER.error("OpenVSwitch table is null for Node {} ", node);
                return null;
            }

            // While there is only one entry in the HashMap, we can't access it by index...
            for (Row row : ovsTable.values()) {
                String bridgeMaps;
                OpenVSwitch ovsRow = ovsdbConfigurationService.getTypedRow(node, OpenVSwitch.class, row);
                Map<String, String> configs = ovsRow.getOtherConfigColumn().getData();

                if (configs == null) {
                    LOGGER.debug("OpenVSwitch table is null for Node {} ", node);
                    continue;
                }

                bridgeMaps = configs.get(configurationService.getProviderMappingsKey());
                if (bridgeMaps == null) {
                    bridgeMaps = configurationService.getDefaultProviderMapping();
                }

                if (bridgeMaps != null) {
                    for (String map : bridgeMaps.split(",")) {
                        String[] pair = map.split(":");
                        phyIfName.add(pair[1]);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Unable to find physical interface for Node: " + node, e);
        }

        LOGGER.debug("Physical interface for Node: {}, If: {}",
                     node, phyIfName);

        return phyIfName;
    }

    /**
     * Returns the Bridge for a given node and bridgeName
     */
    public Bridge getBridge (Node node, String bridgeName) {
        /* TODO SB_MIGRATION */
        Preconditions.checkNotNull(ovsdbConfigurationService);
        try {
            Map<String, Row> bridgeTable =
                    ovsdbConfigurationService.getRows(node, ovsdbConfigurationService.getTableName(node, Bridge.class));
            if (bridgeTable != null) {
                for (String key : bridgeTable.keySet()) {
                    Bridge bridge = ovsdbConfigurationService.getTypedRow(node, Bridge.class, bridgeTable.get(key));
                    if (bridge.getName().equals(bridgeName)) {
                        return bridge;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error getting Bridge Identifier for {} / {}", node, bridgeName, e);
        }

        return null;
    }

    /**
     * Returns true if a patch port exists between the Integration Bridge and Network Bridge
     */
    private boolean isNetworkPatchCreated (Node node, Bridge intBridge, Bridge netBridge) {
        Preconditions.checkNotNull(configurationService);

        boolean isPatchCreated = false;

        String portName = configurationService.getPatchPortName(new ImmutablePair<>(intBridge, netBridge));
        if (isPortOnBridge(node, intBridge, portName)) {
            portName = configurationService.getPatchPortName(new ImmutablePair<>(netBridge, intBridge));
            if (isPortOnBridge(node, netBridge, portName)) {
                isPatchCreated = true;
            }
        }

        return isPatchCreated;
    }

    /**
     * Creates the Integration Bridge
     */
    private void createIntegrationBridge (Node node) throws Exception {
        Preconditions.checkNotNull(configurationService);

        String brIntName = configurationService.getIntegrationBridgeName();

        Status status = addBridge(node, brIntName, null, null);
        if (!status.isSuccess()) {
            LOGGER.debug("Integration Bridge Creation Status: {}", status);
        }
    }

    /**
     * Create and configure bridges for all network types and OpenFlow versions.
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
    private boolean createBridges(Node node, NeutronNetwork network) throws Exception {
        Preconditions.checkNotNull(configurationService);
        Preconditions.checkNotNull(networkingProviderManager);
        Status status;

        LOGGER.debug("createBridges: node: {}, network type: {}", node, network.getProviderNetworkType());

        if (networkingProviderManager.getProvider(node).hasPerTenantTunneling()) { /* indicates OF 1.0 */
            String brInt = configurationService.getIntegrationBridgeName();
            String brNet = configurationService.getNetworkBridgeName();
            String patchNet = configurationService.getPatchPortName(new ImmutablePair<>(brInt, brNet));
            String patchInt = configurationService.getPatchPortName(new ImmutablePair<>(brNet, brInt));

            status = this.addBridge(node, brInt, patchNet, patchInt);
            if (!status.isSuccess()) {
                LOGGER.debug("{} Bridge Creation Status: {}", brInt, status);
                return false;
            }
            status = this.addBridge(node, brNet, patchInt, patchNet);
            if (!status.isSuccess()) {
                LOGGER.debug("{} Bridge Creation Status: {}", brNet, status);
                return false;
            }

            /* For vlan network types add physical port to br-net. */
            if (network.getProviderNetworkType().equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_VLAN)) {
                String phyNetName = this.getPhysicalInterfaceName(node, network.getProviderPhysicalNetwork());
                status = addPortToBridge(node, brNet, phyNetName);
                if (!status.isSuccess()) {
                    LOGGER.debug("Add Port {} to Bridge {} Status: {}", phyNetName, brNet, status);
                    return false;
                }
            }
        } else {
            String brInt = configurationService.getIntegrationBridgeName();
            status = this.addBridge(node, brInt, null, null);
            if (!status.isSuccess()) {
                LOGGER.debug("{} Bridge Creation Status: {}", brInt, status);
                return false;
            }

            /* For vlan network types add physical port to br-int. */
            if (network.getProviderNetworkType().equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_VLAN)) {
                String phyNetName = this.getPhysicalInterfaceName(node, network.getProviderPhysicalNetwork());
                status = addPortToBridge(node, brInt, phyNetName);
                if (!status.isSuccess()) {
                    LOGGER.debug("Add Port {} to Bridge {} Status: {}", phyNetName, brInt, status);
                    return false;
                }
            }
        }

        LOGGER.debug("createNetNetwork: node: {}, status: success", node);
        return true;
    }

    /**
     * Add a Port to a Bridge
     */
    private Status addPortToBridge (Node node, String bridgeName, String portName) throws Exception {
        /* TODO SB_MIGRATION */
        Preconditions.checkNotNull(ovsdbConfigurationService);

        LOGGER.debug("addPortToBridge: Adding port: {} to Bridge {}, Node {}", portName, bridgeName, node);

        String bridgeUUID = this.getBridgeUuid(node, bridgeName);
        if (bridgeUUID == null) {
            LOGGER.error("addPortToBridge: Could not find Bridge {} in Node {}", bridgeName, node);
            return new Status(StatusCode.NOTFOUND, "Could not find "+bridgeName+" in "+node);
        }

        /* Check if the port already exists. */
        Row row = ovsdbConfigurationService
                .getRow(node, ovsdbConfigurationService.getTableName(node, Bridge.class), bridgeUUID);
        Bridge bridge = ovsdbConfigurationService.getTypedRow(node, Bridge.class, row);
        if (bridge != null) {
            if (isPortOnBridge(node, bridge, portName)) {
                LOGGER.debug("addPortToBridge: Port {} already in Bridge {}, Node {}", portName, bridgeName, node);
                return new Status(StatusCode.SUCCESS);
            }
        } else {
            LOGGER.error("addPortToBridge: Could not find Port {} in Bridge {}, Node {}", portName, bridgeName, node);
            return new Status(StatusCode.NOTFOUND, "Could not find "+portName+" in "+bridgeName);
        }

        Port port = ovsdbConfigurationService.createTypedRow(node, Port.class);
        port.setName(portName);
        StatusWithUuid statusWithUuid =
                ovsdbConfigurationService.insertRow(node, port.getSchema().getName(), bridgeUUID, port.getRow());
        if (!statusWithUuid.isSuccess()) {
            LOGGER.error("addPortToBridge: Failed to add Port {} in Bridge {}, Node {}", portName, bridgeName, node);
            return statusWithUuid;
        }

        String portUUID = statusWithUuid.getUuid().toString();
        String interfaceUUID = null;
        int timeout = 6;
        while ((interfaceUUID == null) && (timeout > 0)) {
            Row portRow = ovsdbConfigurationService.getRow(node, port.getSchema().getName(), portUUID);
            port = ovsdbConfigurationService.getTypedRow(node, Port.class, portRow);
            Set<UUID> interfaces = port.getInterfacesColumn().getData();
            if (interfaces == null || interfaces.size() == 0) {
                // Wait for the OVSDB update to sync up the Local cache.
                Thread.sleep(500);
                timeout--;
                continue;
            }
            interfaceUUID = interfaces.toArray()[0].toString();
            Row intf = ovsdbConfigurationService.getRow(node,
                                                ovsdbConfigurationService.getTableName(node, Interface.class), interfaceUUID);
            if (intf == null) {
                interfaceUUID = null;
            }
        }

        if (interfaceUUID == null) {
            LOGGER.error("addPortToBridge: Cannot identify Interface for port {}/{}", portName, portUUID);
            return new Status(StatusCode.INTERNALERROR);
        }

        return new Status(StatusCode.SUCCESS);
    }

    /**
     * Add a Patch Port to a Bridge
     */
    private Status addPatchPort (Node node, String bridgeUUID, String portName, String peerPortName) throws Exception {
        /* TODO SB_MIGRATION */
        Preconditions.checkNotNull(ovsdbConfigurationService);

        LOGGER.debug("addPatchPort: node: {}, bridgeUUID: {}, port: {}, peer: {}",
                     node, bridgeUUID, portName, peerPortName);

        /* Check if the port already exists. */
        Row bridgeRow = ovsdbConfigurationService.getRow(node,
                                                  ovsdbConfigurationService.getTableName(node, Bridge.class), bridgeUUID);
        Bridge bridge = ovsdbConfigurationService.getTypedRow(node, Bridge.class, bridgeRow);
        if (bridge != null) {
            if (isPortOnBridge(node, bridge, portName)) {
                LOGGER.debug("addPatchPort: Port {} already in Bridge, Node {}", portName, node);
                return new Status(StatusCode.SUCCESS);
            }
        } else {
            LOGGER.error("addPatchPort: Could not find Port {} in Bridge, Node {}", portName, node);
            return new Status(StatusCode.NOTFOUND, "Could not find "+portName+" in Bridge");
        }

        Port patchPort = ovsdbConfigurationService.createTypedRow(node, Port.class);
        patchPort.setName(portName);
        // Create patch port and interface
        StatusWithUuid statusWithUuid =
                ovsdbConfigurationService.insertRow(node, patchPort.getSchema().getName(), bridgeUUID, patchPort.getRow());
        if (!statusWithUuid.isSuccess()) {
            return statusWithUuid;
        }

        String patchPortUUID = statusWithUuid.getUuid().toString();

        String interfaceUUID = null;
        int timeout = 6;
        while ((interfaceUUID == null) && (timeout > 0)) {
            Row portRow = ovsdbConfigurationService.getRow(node, patchPort.getSchema().getName(), patchPortUUID);
            patchPort = ovsdbConfigurationService.getTypedRow(node, Port.class, portRow);
            Set<UUID> interfaces = patchPort.getInterfacesColumn().getData();
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

        Interface intf = ovsdbConfigurationService.createTypedRow(node, Interface.class);
        intf.setType("patch");
        Map<String, String> options = Maps.newHashMap();
        options.put("peer", peerPortName);
        intf.setOptions(options);
        return ovsdbConfigurationService.updateRow(node,
                                            intf.getSchema().getName(),
                                            patchPortUUID,
                                            interfaceUUID,
                                            intf.getRow());

    }

    /**
     * Add Bridge to a Node
     */
    private Status addBridge(Node node, String bridgeName,
                             String localPatchName, String remotePatchName) throws Exception {
        Preconditions.checkNotNull(networkingProviderManager);
        /* TODO SB_MIGRATION */

        //String bridgeUUID = getBridgeUuid(node, bridgeName);
        //sb will also add port and interface if this is a new bridge
        boolean result = MdsalUtils.addBridge(node, bridgeName, getControllerTarget());

        /*// TODO use the bridge it code to add bridge
        Bridge bridge = ovsdbConfigurationService.createTypedRow(node, Bridge.class);
        Set<String> failMode = new HashSet<>();
        failMode.add("secure");
        bridge.setFailMode(failMode);

        Set<String> protocols = new HashSet<>();

        // ToDo: Plugin should expose an easy way to get the OVS Version or Schema Version
        // or, alternatively it should not attempt to add set unsupported fields
        //

        try {
	    protocols.add(Constants.OPENFLOW13);
            bridge.setProtocols(protocols);
        } catch (SchemaVersionMismatchException e) {
            LOGGER.info("Failed to add protocol.", e);
        }

        if (bridgeUUID == null) {
            bridge.setName(bridgeName);

            StatusWithUuid statusWithUuid = ovsdbConfigurationService.insertRow(node,
                                                                         bridge.getSchema().getName(),
                                                                         null,
                                                                         bridge.getRow());
            if (!statusWithUuid.isSuccess()) {
                return statusWithUuid;
            }
            bridgeUUID = statusWithUuid.getUuid().toString();
            Port port = ovsdbConfigurationService.createTypedRow(node, Port.class);
            port.setName(bridgeName);
            Status status = ovsdbConfigurationService.insertRow(node, port.getSchema().getName(), bridgeUUID, port.getRow());
            LOGGER.debug("addBridge: Inserting Bridge {} {} with protocols {} and status {}",
                         bridgeName, bridgeUUID, protocols, status);
        } else {
            Status status = ovsdbConfigurationService.updateRow(node,
                                                         bridge.getSchema().getName(),
                                                         null,
                                                         bridgeUUID,
                                                         bridge.getRow());
            LOGGER.debug("addBridge: Updating Bridge {} {} with protocols {} and status {}",
                         bridgeName, bridgeUUID, protocols, status);
        }

        ovsdbConfigurationService.setOFController(node, bridgeUUID);

        if (localPatchName != null &&
            remotePatchName != null &&
            networkingProviderManager.getProvider(node).hasPerTenantTunneling()) {
            return addPatchPort(node, bridgeUUID, localPatchName, remotePatchName);
        }*/
        return new Status(StatusCode.SUCCESS);
    }

    private InetAddress getControllerIPAddress(/*Connection connection*/) {
        InetAddress controllerIP = null;

        String addressString = ConfigProperties.getProperty(this.getClass(), "ovsdb.controller.address");
        if (addressString != null) {
            try {
                controllerIP = InetAddress.getByName(addressString);
                if (controllerIP != null) {
                    return controllerIP;
                }
            } catch (UnknownHostException e) {
                LOGGER.error("Host {} is invalid", addressString);
            }
        }

        addressString = ConfigProperties.getProperty(this.getClass(), "of.address");
        if (addressString != null) {
            try {
                controllerIP = InetAddress.getByName(addressString);
                if (controllerIP != null) {
                    return controllerIP;
                }
            } catch (UnknownHostException e) {
                LOGGER.error("Host {} is invalid", addressString);
            }
        }

        /*
        try {
            controllerIP = connection.getClient().getConnectionInfo().getLocalAddress();
            return controllerIP;
        } catch (Exception e) {
            LOGGER.debug("Invalid connection provided to getControllerIPAddresses", e);
        }
        */

        if (addressString != null) {
            try {
                controllerIP = InetAddress.getByName(addressString);
                if (controllerIP != null) {
                    return controllerIP;
                }
            } catch (UnknownHostException e) {
                LOGGER.error("Host {} is invalid", addressString);
            }
        }

        return controllerIP;
    }

    private short getControllerOFPort() {
        Short defaultOpenFlowPort = 6633;
        Short openFlowPort = defaultOpenFlowPort;
        String portString = ConfigProperties.getProperty(this.getClass(), "of.listenPort");
        if (portString != null) {
            try {
                openFlowPort = Short.decode(portString).shortValue();
            } catch (NumberFormatException e) {
                LOGGER.warn("Invalid port:{}, use default({})", portString,
                        openFlowPort);
            }
        }
        return openFlowPort;
    }

    private String getControllerTarget() {
        /* TODO SB_MIGRATION
         * hardcoding value, need to find better way to get local ip
         */
        //String target = "tcp:" + getControllerIPAddress() + ":" + getControllerOFPort();
        String target = "tcp:192.168.120.1:6633";
        return target;
    }
}
