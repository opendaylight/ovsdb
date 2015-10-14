/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.openstack.netvirt.impl;

import org.opendaylight.neutron.spi.NeutronNetwork;
import org.opendaylight.ovsdb.openstack.netvirt.ConfigInterface;
import org.opendaylight.ovsdb.openstack.netvirt.NetworkHandler;
import org.opendaylight.ovsdb.openstack.netvirt.api.BridgeConfigurationManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.ConfigurationService;
import org.opendaylight.ovsdb.openstack.netvirt.api.Constants;
import org.opendaylight.ovsdb.openstack.netvirt.api.NetworkingProviderManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.OvsdbTables;
import org.opendaylight.ovsdb.openstack.netvirt.api.Southbound;
import org.opendaylight.ovsdb.utils.config.ConfigProperties;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagerEntry;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Madhu Venugopal
 * @author Brent Salisbury
 * @author Sam Hague (shague@redhat.com)
 */
public class BridgeConfigurationManagerImpl implements BridgeConfigurationManager, ConfigInterface {
    private static final Logger LOGGER = LoggerFactory.getLogger(BridgeConfigurationManagerImpl.class);

    // The implementation for each of these services is resolved by the OSGi Service Manager
    private volatile ConfigurationService configurationService;
    private volatile NetworkingProviderManager networkingProviderManager;
    private volatile Southbound southbound;

    public void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    public void setSouthbound(Southbound southbound) {
        this.southbound = southbound;
    }

    @Override
    public String getBridgeUuid(Node node, String bridgeName) {
        return southbound.getBridgeUuid(node, bridgeName);
    }

    @Override
    public boolean isNodeNeutronReady(Node node) {
        Preconditions.checkNotNull(configurationService);
        return southbound.getBridge(node, configurationService.getIntegrationBridgeName()) != null;
    }

    @Override
    public boolean isNodeOverlayReady(Node node) {
        Preconditions.checkNotNull(configurationService);
        return isNodeNeutronReady(node)
                && southbound.getBridge(node, configurationService.getNetworkBridgeName()) != null;
    }

    @Override
    public boolean isPortOnBridge (Node bridgeNode, String portName) {
        return southbound.extractTerminationPointAugmentation(bridgeNode, portName) != null;
    }

    @Override
    public boolean isNodeTunnelReady(Node bridgeNode, Node ovsdbNode) {
        Preconditions.checkNotNull(configurationService);
        if (!southbound.isBridgeOnOvsdbNode(ovsdbNode, configurationService.getIntegrationBridgeName())) {
            LOGGER.trace("isNodeTunnelReady: node: {}, {} missing",
                    bridgeNode, configurationService.getIntegrationBridgeName());
            return false;
        }

        return isNodeL3Ready(bridgeNode, ovsdbNode);
    }

    @Override
    public boolean isNodeVlanReady(Node bridgeNode, Node ovsdbNode, NeutronNetwork network) {
        Preconditions.checkNotNull(configurationService);

        final String brInt = configurationService.getIntegrationBridgeName();
        if (!southbound.isBridgeOnOvsdbNode(ovsdbNode, brInt)) {
            LOGGER.trace("isNodeVlanReady: node: {}, {} missing", bridgeNode, brInt);
            return false;
        }

        /* Check if physical device is added to br-int. */
        String phyNetName = getPhysicalInterfaceName(ovsdbNode, network.getProviderPhysicalNetwork());
        if (!isPortOnBridge(bridgeNode, phyNetName)) {
            LOGGER.trace("isNodeVlanReady: node: {}, eth missing", bridgeNode);
            return false;
        }

        return isNodeL3Ready(bridgeNode, ovsdbNode);
    }

    public boolean isNodeL3Ready(Node bridgeNode, Node ovsdbNode) {
        Preconditions.checkNotNull(configurationService);
        boolean ready = false;
        if (configurationService.isL3ForwardingEnabled()) {
            final String brInt = configurationService.getIntegrationBridgeName();
            final String brExt = configurationService.getExternalBridgeName();
            final String portNameInt = configurationService.getPatchPortName(new ImmutablePair<>(brInt, brExt));
            final String portNameExt = configurationService.getPatchPortName(new ImmutablePair<>(brExt, brInt));
            Preconditions.checkNotNull(portNameInt);
            Preconditions.checkNotNull(portNameExt);

            if (southbound.isBridgeOnOvsdbNode(ovsdbNode, brExt)) {
                //this would look better if used a method like isNetworkPatchCreated()
                if (isPortOnBridge(bridgeNode, portNameInt)) {
                    Node extBridgeNode = southbound.readBridgeNode(ovsdbNode, brExt);
                    if (isPortOnBridge(extBridgeNode, portNameExt)) {
                        ready = true;
                    } else {
                        LOGGER.trace("isNodeL3Ready: node: {}, {} missing",
                                bridgeNode, portNameExt);
                    }
                } else {
                    LOGGER.trace("isNodeL3Ready: node: {}, {} missing",
                            bridgeNode, portNameInt);
                }
            } else {
                LOGGER.trace("isNodeL3Ready: node: {}, {} missing",
                        bridgeNode, brExt);
            }
        } else {
            ready = true;
        }
        return ready;
    }

    @Override
    public void prepareNode(Node ovsdbNode) {
        Preconditions.checkNotNull(configurationService);

        try {
            createIntegrationBridge(ovsdbNode);
        } catch (Exception e) {
            LOGGER.error("Error creating Integration Bridge on {}", ovsdbNode, e);
            return;
        }

        try {
            if (configurationService.isL3ForwardingEnabled()) {
                createExternalBridge(ovsdbNode);
            }
        } catch (Exception e) {
            LOGGER.error("Error creating External Bridge on {}", ovsdbNode, e);
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
    public boolean createLocalNetwork (Node bridgeNode, NeutronNetwork network) {
        boolean isCreated = false;
        Node ovsdbNode = southbound.readOvsdbNode(bridgeNode);
        if (ovsdbNode == null) {
            //this should never happen
            LOGGER.error("createLocalNetwork could not find ovsdbNode from bridge node " + bridgeNode);
            return false;
        }
        if (network.getProviderNetworkType().equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_VLAN)) {
            if (!isNodeVlanReady(bridgeNode, ovsdbNode, network)) {
                try {
                    isCreated = createBridges(bridgeNode, ovsdbNode, network);
                } catch (Exception e) {
                    LOGGER.error("Error creating internal vlan net network " + bridgeNode, e);
                }
            } else {
                isCreated = true;
            }
        } else if (network.getProviderNetworkType().equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_VXLAN) ||
                   network.getProviderNetworkType().equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_GRE)) {
            if (!isNodeTunnelReady(bridgeNode, ovsdbNode)) {
                try {
                    isCreated = createBridges(bridgeNode, ovsdbNode, network);
                } catch (Exception e) {
                    LOGGER.error("Error creating internal vxlan/gre net network " + bridgeNode, e);
                }
            } else {
                isCreated = true;
            }
        }
        return isCreated;
    }



    @Override
    public String getExternalInterfaceName (Node node, String extNetwork) {
        String phyIf = null;
        String providerMaps = southbound.getOtherConfig(node, OvsdbTables.OPENVSWITCH,
                configurationService.getProviderMappingsKey());
        if (providerMaps != null) {
            for (String map : providerMaps.split(",")) {
                String[] pair = map.split(":");
                if (pair[0].equals(extNetwork)) {
                    phyIf = pair[1];
                    break;
                }
            }
        }
        if (phyIf == null) {
            LOGGER.error("External interface not found for Node: {}, Network {}",
                    node, extNetwork);
        }
        else {
            LOGGER.info("External interface found for Node: {}, Network {} is {}",node,extNetwork,phyIf);
        }
        return phyIf;
    }



    @Override
    public String getPhysicalInterfaceName (Node node, String physicalNetwork) {
        String phyIf = null;
        String providerMaps = southbound.getOtherConfig(node, OvsdbTables.OPENVSWITCH,
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
        String providerMaps = southbound.getOtherConfig(node, OvsdbTables.OPENVSWITCH,
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
    private boolean isNetworkPatchCreated(Node node, Node intBridge, Node netBridge) {
        Preconditions.checkNotNull(configurationService);

        boolean isPatchCreated = false;

        String portName = configurationService.getPatchPortName(new ImmutablePair<>(intBridge, netBridge));
        if (isPortOnBridge(intBridge, portName)) {
            portName = configurationService.getPatchPortName(new ImmutablePair<>(netBridge, intBridge));
            if (isPortOnBridge(netBridge, portName)) {
                isPatchCreated = true;
            }
        }

        return isPatchCreated;
    }

    /**
     * Creates the Integration Bridge
     */
    private boolean createIntegrationBridge(Node ovsdbNode) throws Exception {
        Preconditions.checkNotNull(configurationService);

        if (!addBridge(ovsdbNode, configurationService.getIntegrationBridgeName())) {
            LOGGER.debug("Integration Bridge Creation failed");
            return false;
        }
        return true;
    }

    private boolean createExternalBridge(Node ovsdbNode) throws Exception {
        Preconditions.checkNotNull(configurationService);

        if (!addBridge(ovsdbNode, configurationService.getExternalBridgeName())) {
            LOGGER.debug("External Bridge Creation failed");
            return false;
        }
        return true;
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
    private boolean createBridges(Node bridgeNode, Node ovsdbNode, NeutronNetwork network) throws Exception {
        Preconditions.checkNotNull(configurationService);
        Preconditions.checkNotNull(networkingProviderManager);

        LOGGER.debug("createBridges: node: {}, network type: {}", bridgeNode, network.getProviderNetworkType());

        final String brInt = configurationService.getIntegrationBridgeName();
        if (! createIntegrationBridge(ovsdbNode)) {
            LOGGER.debug("{} Bridge creation failed", brInt);
            return false;
        }

        if (configurationService.isL3ForwardingEnabled()) {
            final String brExt = configurationService.getExternalBridgeName();
            if (! createExternalBridge(ovsdbNode)) {
                LOGGER.error("{} Bridge creation failed", brExt);
                return false;
            }

            //get two patch port names
            final String portNameInt = configurationService.getPatchPortName(new ImmutablePair<>(brInt, brExt));
            final String portNameExt = configurationService.getPatchPortName(new ImmutablePair<>(brExt, brInt));
            Preconditions.checkNotNull(portNameInt);
            Preconditions.checkNotNull(portNameExt);

            if (!addPatchPort(bridgeNode, brInt, portNameInt, portNameExt)) {
                LOGGER.error("Add Port {} to Bridge {} failed", portNameInt, brInt);
                return false;
            }
            Node extBridgeNode = southbound.readBridgeNode(ovsdbNode, brExt);
            Preconditions.checkNotNull(extBridgeNode);
            if (!addPatchPort(extBridgeNode, brExt, portNameExt, portNameInt)) {
                LOGGER.error("Add Port {} to Bridge {} failed", portNameExt, brExt);
                return false;
            }
            String extNetName = getExternalInterfaceName(extBridgeNode, brExt);
            if ( extNetName != null) {
                if (!addPortToBridge(extBridgeNode, brExt, extNetName)) {
                    LOGGER.error("Add External Port {} to Bridge {} failed", extNetName, brExt);
                    return false;
                }
            LOGGER.info("Add External Port {} to Ext Bridge {} success", extNetName, brExt);
            }
        }
        /* For vlan network types add physical port to br-int. */
        if (network.getProviderNetworkType().equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_VLAN)) {
            String phyNetName = this.getPhysicalInterfaceName(bridgeNode, network.getProviderPhysicalNetwork());
            if (!addPortToBridge(bridgeNode, brInt, phyNetName)) {
                LOGGER.debug("Add Port {} to Bridge {} failed", phyNetName, brInt);
                return false;
            }
        }
        LOGGER.info("createBridges: node: {}, status: success", bridgeNode);
        return true;
    }

    /**
     * Add a Port to a Bridge
     */
    private boolean addPortToBridge (Node node, String bridgeName, String portName) throws Exception {
        boolean rv = true;

        if (southbound.extractTerminationPointAugmentation(node, portName) == null) {
            rv = southbound.addTerminationPoint(node, bridgeName, portName, null);

            if (rv) {
                LOGGER.info("addPortToBridge: node: {}, bridge: {}, portname: {} status: success",
                        node.getNodeId().getValue(), bridgeName, portName);
            } else {
                LOGGER.error("addPortToBridge: node: {}, bridge: {}, portname: {} status: FAILED",
                        node.getNodeId().getValue(), bridgeName, portName);
            }
        } else {
            LOGGER.trace("addPortToBridge: node: {}, bridge: {}, portname: {} status: not_needed",
                    node.getNodeId().getValue(), bridgeName, portName);
        }

        return rv;
    }

    /**
     * Add a Patch Port to a Bridge
     */
    private boolean addPatchPort (Node node, String bridgeName, String portName, String peerPortName) throws Exception {
        boolean rv = true;

        if (southbound.extractTerminationPointAugmentation(node, portName) == null) {
            rv = southbound.addPatchTerminationPoint(node, bridgeName, portName, peerPortName);

            if (rv) {
                LOGGER.info("addPatchPort: node: {}, bridge: {}, portname: {} peer: {} status: success",
                        node.getNodeId().getValue(), bridgeName, portName, peerPortName);
            } else {
                LOGGER.error("addPatchPort: node: {}, bridge: {}, portname: {} peer: {} status: FAILED",
                        node.getNodeId().getValue(), bridgeName, portName, peerPortName);
            }
        } else {
            LOGGER.trace("addPatchPort: node: {}, bridge: {}, portname: {} peer: {} status: not_needed",
                    node.getNodeId().getValue(), bridgeName, portName, peerPortName);
        }

        return rv;
    }

    /**
     * Add Bridge to a Node
     */
    private boolean addBridge(Node ovsdbNode, String bridgeName) throws Exception {
        boolean rv = true;
        if ((!southbound.isBridgeOnOvsdbNode(ovsdbNode, bridgeName)) ||
                (southbound.getBridgeFromConfig(ovsdbNode, bridgeName) == null)) {
            rv = southbound.addBridge(ovsdbNode, bridgeName, getControllersFromOvsdbNode(ovsdbNode));
        }
        return rv;
    }

    private String getControllerIPAddress() {
        InetAddress controllerIP = null;

        String addressString = ConfigProperties.getProperty(this.getClass(), "ovsdb.controller.address");
        if (addressString != null) {
            try {
                controllerIP = InetAddress.getByName(addressString);
                if (controllerIP != null) {
                    return addressString;
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
                    return addressString;
                }
            } catch (UnknownHostException e) {
                LOGGER.error("Host {} is invalid", addressString);
            }
        }

        return null;
    }

    private short getControllerOFPort() {
        Short defaultOpenFlowPort = Constants.OPENFLOW_PORT;
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

    private List<String> getControllersFromOvsdbNode(Node node) {
        List<String> controllersStr = new ArrayList<>();

        String controllerIpStr = getControllerIPAddress();
        if (controllerIpStr != null) {
            // If codepath makes it here, the ip address to be used was explicitly provided.
            // Being so, also fetch openflowPort provided via ConfigProperties.
            controllersStr.add(Constants.OPENFLOW_CONNECTION_PROTOCOL
                    + ":" + controllerIpStr + ":" + getControllerOFPort());
        } else {
            // Check if ovsdb node has manager entries
            OvsdbNodeAugmentation ovsdbNodeAugmentation = southbound.extractOvsdbNode(node);
            if (ovsdbNodeAugmentation != null) {
                List<ManagerEntry> managerEntries = ovsdbNodeAugmentation.getManagerEntry();
                if (managerEntries != null && !managerEntries.isEmpty()) {
                    for (ManagerEntry managerEntry : managerEntries) {
                        if (managerEntry == null || managerEntry.getTarget() == null) {
                            continue;
                        }
                        String[] tokens = managerEntry.getTarget().getValue().split(":");
                        if (tokens.length == 3 && tokens[0].equalsIgnoreCase("tcp")) {
                            controllersStr.add(Constants.OPENFLOW_CONNECTION_PROTOCOL
                                    + ":" + tokens[1] + ":" + getControllerOFPort());
                        } else if (tokens[0].equalsIgnoreCase("ptcp")) {
                            ConnectionInfo connectionInfo = ovsdbNodeAugmentation.getConnectionInfo();
                            if (connectionInfo != null && connectionInfo.getLocalIp() != null) {
                                controllerIpStr = new String(connectionInfo.getLocalIp().getValue());
                                controllersStr.add(Constants.OPENFLOW_CONNECTION_PROTOCOL
                                        + ":" + controllerIpStr + ":" + Constants.OPENFLOW_PORT);
                            } else {
                                LOGGER.warn("Ovsdb Node does not contain connection info: {}", node);
                            }
                        } else {
                            LOGGER.trace("Skipping manager entry {} for node {}",
                                    managerEntry.getTarget(), node.getNodeId().getValue());
                        }
                    }
                } else {
                    LOGGER.warn("Ovsdb Node does not contain manager entries : {}", node);
                }
            }
        }

        if (controllersStr.isEmpty()) {
            // Neither user provided ip nor ovsdb node has manager entries. Lets use local machine ip address.
            LOGGER.debug("Use local machine ip address as a OpenFlow Controller ip address");
            controllerIpStr = getLocalControllerHostIpAddress();
            if (controllerIpStr != null) {
                controllersStr.add(Constants.OPENFLOW_CONNECTION_PROTOCOL
                        + ":" + controllerIpStr + ":" + Constants.OPENFLOW_PORT);
            }
        }

        if (controllersStr.isEmpty()) {
            LOGGER.warn("Failed to determine OpenFlow controller ip address");
        } else if (LOGGER.isDebugEnabled()) {
            controllerIpStr = "";
            for (String currControllerIpStr : controllersStr) {
                controllerIpStr += " " + currControllerIpStr;
            }
            LOGGER.debug("Found {} OpenFlow Controller(s) :{}", controllersStr.size(), controllerIpStr);
        }

        return controllersStr;
    }

    private String getLocalControllerHostIpAddress() {
        String ipaddress = null;
        try{
            for (Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();ifaces.hasMoreElements();){
                NetworkInterface iface = (NetworkInterface) ifaces.nextElement();

                for (Enumeration<InetAddress> inetAddrs = iface.getInetAddresses(); inetAddrs.hasMoreElements();) {
                    InetAddress inetAddr = (InetAddress) inetAddrs.nextElement();
                    if (!inetAddr.isLoopbackAddress() && inetAddr.isSiteLocalAddress()) {
                        ipaddress = inetAddr.getHostAddress();
                        break;
                    }
                }
            }
        }catch (Exception e){
            LOGGER.warn("Exception while fetching local host ip address ",e);
        }
        return ipaddress;
    }

    @Override
    public void setDependencies(BundleContext bundleContext, ServiceReference serviceReference) {
        configurationService =
                (ConfigurationService) ServiceHelper.getGlobalInstance(ConfigurationService.class, this);
        networkingProviderManager =
                (NetworkingProviderManager) ServiceHelper.getGlobalInstance(NetworkingProviderManager.class, this);
        southbound =
                (Southbound) ServiceHelper.getGlobalInstance(Southbound.class, this);
    }

    @Override
    public void setDependencies(Object impl) {
    }
}
