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
import org.opendaylight.ovsdb.openstack.netvirt.api.NetworkingProviderManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.OvsdbTables;
import org.opendaylight.ovsdb.openstack.netvirt.api.Southbound;
import org.opendaylight.ovsdb.utils.config.ConfigProperties;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
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
    static final Logger LOGGER = LoggerFactory.getLogger(BridgeConfigurationManagerImpl.class);

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
    public boolean isPortOnBridge (Node node, String portName) {
        return southbound.extractTerminationPointAugmentation(node, portName) != null;
    }

    @Override
    public boolean isNodeTunnelReady(Node bridgeNode, Node ovsdbNode) {
        Preconditions.checkNotNull(configurationService);
        if (!southbound.isBridgeOnOvsdbNode(ovsdbNode, configurationService.getIntegrationBridgeName())) {
            LOGGER.trace("isNodeVlanReady: node: {}, {} missing",
                    bridgeNode, configurationService.getIntegrationBridgeName());
            return false;
        }

        return isNodeL3Ready(bridgeNode, ovsdbNode);
    }

    public boolean isNodeL3Ready(Node bridgeNode, Node ovsdbNode) {
        Preconditions.checkNotNull(configurationService);
        boolean ready = false;
        if (isL3Enabled()) {
            if (southbound.isBridgeOnOvsdbNode(ovsdbNode, configurationService.getExternalBridgeName())) {
                //this would look better if used a method like isNetworkPatchCreated()
                if(isPortOnBridge(bridgeNode, "extpatchname")) {
                    Node extBridgeNode = southbound.readBridgeNode(ovsdbNode, "br-ext");
                    if (isPortOnBridge(extBridgeNode, "intpatchname")) {
                        ready = true;
                    } else {
                        LOGGER.trace("isNodeL3Ready: node: {}, {} missing",
                                bridgeNode, "intpatchname");
                    }
                } else {
                    LOGGER.trace("isNodeL3Ready: node: {}, {} missing",
                            bridgeNode, "extpatchname");
                }
            } else {
                LOGGER.trace("isNodeL3Ready: node: {}, {} missing",
                        bridgeNode, configurationService.getExternalBridgeName());
            }
        } else {
            ready = true;
        }
        return ready;
    }

    @Override
    public boolean isNodeVlanReady(Node bridgeNode, Node ovsdbNode, NeutronNetwork network) {
        Preconditions.checkNotNull(configurationService);
        boolean ready = false;

        if (!southbound.isBridgeOnOvsdbNode(ovsdbNode, configurationService.getIntegrationBridgeName())) {
            LOGGER.trace("isNodeVlanReady: node: {}, {} missing",
                    bridgeNode, configurationService.getIntegrationBridgeName());
        }

        /* Check if physical device is added to br-int. */
        String phyNetName = getPhysicalInterfaceName(ovsdbNode, network.getProviderPhysicalNetwork());
        if (!isPortOnBridge(bridgeNode, phyNetName)) {
            LOGGER.trace("isNodeVlanReady: node: {}, eth missing", bridgeNode);
            return false;
        }

        return isNodeL3Ready(bridgeNode, ovsdbNode);
    }

    @Override
    public void prepareNode(Node node) {
        //Preconditions.checkNotNull(networkingProviderManager);

        try {
            createIntegrationBridge(node);
        } catch (Exception e) {
            LOGGER.error("Error creating Integration Bridge on {}", node, e);
            return;
        }

        try {
            createExternalBridge(node);
        } catch (Exception e) {
            LOGGER.error("Error creating External Bridge on {}", node, e);
            return;
        }
        // this node is an ovsdb node so it doesn't have a bridge
        // so either look up the bridges or just wait for the bridge update to come in
        // and add the flows there.
        //networkingProviderManager.getProvider(node).initializeFlowRules(node);
    }

    boolean isL3Enabled() {return true;}

    /**
     * Check if the full network setup is available. If not, create it.
     */
    @Override
    public boolean createLocalNetwork (Node bridgeNode, NeutronNetwork network) {
        boolean isCreated = false;
        Node ovsdbNode = null;
        ovsdbNode = southbound.readOvsdbNode(bridgeNode);
        if (ovsdbNode == null) { //this should never happen
            return false;
        }
        if (network.getProviderNetworkType().equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_VLAN)) {
            if (!isNodeVlanReady(bridgeNode, ovsdbNode, network)) {
                try {
                    isCreated = createBridges(bridgeNode, ovsdbNode, network);
                } catch (Exception e) {
                    LOGGER.error("Error creating internal net network " + bridgeNode, e);
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
                    LOGGER.error("Error creating internal net network " + bridgeNode, e);
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
    private void createIntegrationBridge(Node node) throws Exception {
        Preconditions.checkNotNull(configurationService);

        String brIntName = configurationService.getIntegrationBridgeName();

        if (!addBridge(node, brIntName, null, null)) {
            LOGGER.debug("Integration Bridge Creation failed");
        }
    }

    private void createExternalBridge(Node node) throws Exception {
        Preconditions.checkNotNull(configurationService);

        String brExtName = configurationService.getExternalBridgeName();

        if (!addBridge(node, brExtName, null, null)) {
            LOGGER.debug("External Bridge Creation failed");
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
    private boolean createBridges(Node node, Node ovsdbNode, NeutronNetwork network) throws Exception {
        Preconditions.checkNotNull(configurationService);
        Preconditions.checkNotNull(networkingProviderManager);

        LOGGER.debug("createBridges: node: {}, network type: {}", node, network.getProviderNetworkType());

        String brInt = configurationService.getIntegrationBridgeName();
        if (!addBridge(node, brInt, null, null)) {
            LOGGER.debug("{} Bridge creation failed", brInt);
            return false;
        }

        String brExt = configurationService.getExternalBridgeName();
        if (!addBridge(node, brExt, null, null)) {
            LOGGER.debug("{} Bridge creation failed", brExt);
            return false;
        }

        if (isL3Enabled()) {
            //get two patch port names
            if (!addPatchPort(node, brInt, "extpatchport", "intpatchport")) {
                LOGGER.debug("Add Port {} to Bridge {} failed", "extpatchport", brInt);
                return false;
            }
            Node extBridgeNode = southbound.readBridgeNode(ovsdbNode, "br-ext");
            if (!addPatchPort(extBridgeNode, brExt, "extpatchport", "extpatchport")) {
                LOGGER.debug("Add Port {} to Bridge {} failed", "intpatchport", brExt);
                return false;
            }
        }

        /* For vlan network types add physical port to br-int. */
        if (network.getProviderNetworkType().equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_VLAN)) {
            String phyNetName = this.getPhysicalInterfaceName(node, network.getProviderPhysicalNetwork());
            if (!addPortToBridge(node, brInt, phyNetName)) {
                LOGGER.debug("Add Port {} to Bridge {} failed", phyNetName, brInt);
                return false;
            }
        }

        LOGGER.debug("createBridges: node: {}, status: success", node);
        return true;
    }

    /**
     * Add a Port to a Bridge
     */
    private boolean addPortToBridge (Node node, String bridgeName, String portName) throws Exception {
        boolean rv = true;

        if (southbound.extractTerminationPointAugmentation(node, portName) == null) {
            rv = southbound.addTerminationPoint(node, bridgeName, portName, null);
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
        }

        return rv;
    }

    /**
     * Add Bridge to a Node
     */
    private boolean addBridge(Node ovsdbNode, String bridgeName,
                              String localPatchName, String remotePatchName) throws Exception {
        boolean rv = true;
        if (!southbound.isBridgeOnOvsdbNode(ovsdbNode, bridgeName)) {
            rv = southbound.addBridge(ovsdbNode, bridgeName, getControllerTarget(ovsdbNode));
        }
        return rv;
    }

    private InetAddress getControllerIPAddress() {
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

    private String getControllerTarget(Node node) {
        String target = null;
        OvsdbNodeAugmentation ovsdbNodeAugmentation = southbound.extractOvsdbNode(node);
        if (ovsdbNodeAugmentation != null) {
            ConnectionInfo connectionInfo = ovsdbNodeAugmentation.getConnectionInfo();
            String addressStr = new String(connectionInfo.getLocalIp().getValue());
            target = "tcp:" + addressStr + ":6633";
        } else{
            target = getControllerTarget();
        }
        return target;
    }

    private String getControllerTarget() {
        /* TODO SB_MIGRATION
         * hardcoding value, need to find better way to get local ip
         */
        //String target = "tcp:" + getControllerIPAddress() + ":" + getControllerOFPort();
        //TODO: dirty fix, need to remove it once we have proper solution
        String ipaddress = null;
        try{
            for (Enumeration ifaces = NetworkInterface.getNetworkInterfaces();ifaces.hasMoreElements();){
                NetworkInterface iface = (NetworkInterface) ifaces.nextElement();

                for (Enumeration inetAddrs = iface.getInetAddresses(); inetAddrs.hasMoreElements();) {
                    InetAddress inetAddr = (InetAddress) inetAddrs.nextElement();
                    if (!inetAddr.isLoopbackAddress()) {
                        if (inetAddr.isSiteLocalAddress()) {
                            ipaddress = inetAddr.getHostAddress();
                            break;
                        }
                    }
                }
            }
        }catch (Exception e){
            LOGGER.warn("ROYALLY SCREWED : Exception while fetching local host ip address ",e);
        }
        return "tcp:"+ipaddress+":6633";
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
    public void setDependencies(Object impl) {}
}
