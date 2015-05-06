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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class BridgeConfigurationManagerImpl implements BridgeConfigurationManager {
    static final Logger LOGGER = LoggerFactory.getLogger(BridgeConfigurationManagerImpl.class);

    // The implementation for each of these services is resolved by the OSGi Service Manager
    private volatile ConfigurationService configurationService;
    private volatile NetworkingProviderManager networkingProviderManager;

    void init() {
        LOGGER.info(">>>>>> init {}", this.getClass());
    }

    @Override
    public String getBridgeUuid(Node node, String bridgeName) {
        return MdsalUtils.getBridgeUuid(node, bridgeName).toString();
    }

    @Override
    public boolean isNodeNeutronReady(Node node) {
        Preconditions.checkNotNull(configurationService);
        return MdsalUtils.getBridge(node, configurationService.getIntegrationBridgeName()) != null;
    }

    @Override
    public boolean isNodeOverlayReady(Node node) {
        Preconditions.checkNotNull(configurationService);
        return isNodeNeutronReady(node)
                && MdsalUtils.getBridge(node, configurationService.getNetworkBridgeName()) != null;
    }

    @Override
    public boolean isPortOnBridge (Node node, Bridge bridge, String portName) {
        return MdsalUtils.getPort(node, portName) != null;
    }

    @Override
    public boolean isNodeTunnelReady(Node node) {
        Preconditions.checkNotNull(configurationService);
        return MdsalUtils.getBridge(node, configurationService.getIntegrationBridgeName()) != null;
    }

    @Override
    public boolean isNodeVlanReady(Node node, NeutronNetwork network) {
        Preconditions.checkNotNull(networkingProviderManager);

        /* is br-int created */
        OvsdbBridgeAugmentation intBridge = MdsalUtils.getBridge(node, configurationService.getIntegrationBridgeName());
        if (intBridge == null) {
            LOGGER.trace("isNodeVlanReady: node: {}, br-int missing", node);
            return false;
        }

        /* Check if physical device is added to br-int. */
        String phyNetName = this.getPhysicalInterfaceName(node, network.getProviderPhysicalNetwork());
        if (MdsalUtils.getPort(node, phyNetName) == null) {
            LOGGER.trace("isNodeVlanReady: node: {}, eth missing", node);
            return false;
        }

        return true;
    }

    @Override
    public void prepareNode(Node node) {
        Preconditions.checkNotNull(networkingProviderManager);

        try {
            createIntegrationBridge(node);
        } catch (Exception e) {
            LOGGER.error("Error creating Integration Bridge on {}", node, e);
            return;
        }
        // this node is an ovsdb node so it doesn't have a bridge
        // so either look up the bridges or just wait for the bridge update to come in
        // and add the flows there.
        //networkingProviderManager.getProvider(node).initializeFlowRules(node);
    }

    /**
     * Check if the full network setup is available. If not, create it.
     */
    @Override
    public boolean createLocalNetwork (Node node, NeutronNetwork network) {
        boolean isCreated = false;
        if (network.getProviderNetworkType().equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_VLAN)) {
            if (!isNodeVlanReady(node, network)) {
                try {
                    isCreated = createBridges(node, network);
                } catch (Exception e) {
                    LOGGER.error("Error creating internal net network " + node, e);
                }
            } else {
                isCreated = true;
            }
        } else if (network.getProviderNetworkType().equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_VXLAN) ||
                   network.getProviderNetworkType().equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_GRE)) {
            if (!isNodeTunnelReady(node)) {
                try {
                    isCreated = createBridges(node, network);
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
        String providerMaps = MdsalUtils.getOtherConfig(node, OvsdbTables.OPENVSWITCH,
                configurationService.getProviderMappingsKey());
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

        if (phyIf == null) {
            LOGGER.error("Physical interface not found for Node: {}, Network {}",
                         node, physicalNetwork);
        }

        return phyIf;
    }

    @Override
    public List<String> getAllPhysicalInterfaceNames(Node node) {
        List<String> phyIfName = Lists.newArrayList();
        String phyIf = null;
        String providerMaps = MdsalUtils.getOtherConfig(node, OvsdbTables.OPENVSWITCH,
                configurationService.getProviderMappingsKey());
        if (providerMaps == null) {
            providerMaps = configurationService.getDefaultProviderMapping();
        }

        if (providerMaps != null) {
            for (String map : providerMaps.split(",")) {
                String[] pair = map.split(":");
                phyIfName.add(pair[1]);
            }
        }

        return phyIfName;
    }

    /**
     * Returns true if a patch port exists between the Integration Bridge and Network Bridge
     */
    private boolean isNetworkPatchCreated(Node node, Bridge intBridge, Bridge netBridge) {
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
    private void createIntegrationBridge(Node node) throws Exception {
        Preconditions.checkNotNull(configurationService);

        String brIntName = configurationService.getIntegrationBridgeName();

        if (!addBridge(node, brIntName, null, null)) {
            LOGGER.debug("Integration Bridge Creation failed");
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

        String brInt = configurationService.getIntegrationBridgeName();
        if (!addBridge(node, brInt, null, null)) {
            LOGGER.debug("{} Bridge creation failed", brInt);
            return false;
        }

        /* For vlan network types add physical port to br-int. */
        if (network.getProviderNetworkType().equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_VLAN)) {
            String phyNetName = this.getPhysicalInterfaceName(node, network.getProviderPhysicalNetwork());
            if (!addPortToBridge(node, brInt, phyNetName)) {
                LOGGER.debug("Add Port {} to Bridge {} failed", phyNetName, brInt);
                return false;
            }
        }

        LOGGER.debug("createNetNetwork: node: {}, status: success", node);
        return true;
    }

    /**
     * Add a Port to a Bridge
     */
    private boolean addPortToBridge (Node node, String bridgeName, String portName) throws Exception {
        boolean rv = true;

        if (MdsalUtils.getPort(node, portName) == null) {
            rv = MdsalUtils.addPort(node, bridgeName, portName);
        }

        return rv;
    }

    /**
     * Add a Patch Port to a Bridge
     */
    private boolean addPatchPort (Node node, String bridgeName, String portName, String peerPortName) throws Exception {
        boolean rv = true;

        if (MdsalUtils.getPort(node, portName) == null) {
            rv = MdsalUtils.addPatchPort(node, bridgeName, portName, peerPortName);
        }

        return rv;
    }

    /**
     * Add Bridge to a Node
     */
    private boolean addBridge(Node node, String bridgeName,
                              String localPatchName, String remotePatchName) throws Exception {
        Preconditions.checkNotNull(networkingProviderManager);
        //sb will also add port and interface if this is a new bridge
        return MdsalUtils.addBridge(node, bridgeName, getControllerTarget());
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
