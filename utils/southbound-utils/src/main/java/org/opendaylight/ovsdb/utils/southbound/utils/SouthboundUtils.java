/*
 * Copyright (c) 2015, 2018 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.utils.southbound.utils;

import static java.util.Objects.requireNonNull;

import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.ovsdb.utils.config.ConfigProperties;
import org.opendaylight.ovsdb.utils.mdsal.utils.ControllerMdsalUtils;
import org.opendaylight.ovsdb.utils.mdsal.utils.MdsalUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IetfInetUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagerEntry;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SouthboundUtils {
    private abstract static class UtilsProvider {

        abstract <T extends DataObject> T read(LogicalDatastoreType store, InstanceIdentifier<T> path);

        abstract boolean delete(LogicalDatastoreType store, InstanceIdentifier<?> path);

        abstract <T extends DataObject> boolean put(LogicalDatastoreType store, InstanceIdentifier<T> path,
                T createNode);

        abstract <T extends DataObject> boolean merge(LogicalDatastoreType store, InstanceIdentifier<T> path, T data);
    }

    @Deprecated
    private static final class ControllerUtilsProvider extends UtilsProvider {
        private final ControllerMdsalUtils mdsalUtils;

        ControllerUtilsProvider(final ControllerMdsalUtils mdsalUtils) {
            this.mdsalUtils = requireNonNull(mdsalUtils);
        }

        @Override
        <T extends DataObject> T read(LogicalDatastoreType store, InstanceIdentifier<T> path) {
            return mdsalUtils.read(
                org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.fromMdsal(store), path);
        }

        @Override
        <T extends DataObject> boolean put(LogicalDatastoreType store,
                InstanceIdentifier<T> path, T data) {
            return mdsalUtils.put(
                org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.fromMdsal(store), path, data);
        }

        @Override
        boolean delete(LogicalDatastoreType store, InstanceIdentifier<?> path) {
            return mdsalUtils.delete(
                org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.fromMdsal(store), path);
        }

        @Override
        <T extends DataObject> boolean merge(LogicalDatastoreType store, InstanceIdentifier<T> path, T data) {
            return mdsalUtils.merge(
                org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.fromMdsal(store), path, data);
        }
    }

    private static final class MdsalUtilsProvider extends UtilsProvider {
        private final MdsalUtils mdsalUtils;

        MdsalUtilsProvider(final MdsalUtils mdsalUtils) {
            this.mdsalUtils = requireNonNull(mdsalUtils);
        }

        @Override
        <T extends DataObject> T read(LogicalDatastoreType store, InstanceIdentifier<T> path) {
            return mdsalUtils.read(store, path);
        }

        @Override
        <T extends DataObject> boolean put(LogicalDatastoreType store, InstanceIdentifier<T> path, T data) {
            return mdsalUtils.put(store, path, data);
        }

        @Override
        boolean delete(LogicalDatastoreType store, InstanceIdentifier<?> path) {
            return mdsalUtils.delete(store, path);
        }

        @Override
        <T extends DataObject> boolean merge(LogicalDatastoreType store, InstanceIdentifier<T> path, T data) {
            return mdsalUtils.merge(store, path, data);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(SouthboundUtils.class);
    public static final TopologyId OVSDB_TOPOLOGY_ID = new TopologyId(new Uri("ovsdb:1"));
    public static final String OPENFLOW_CONNECTION_PROTOCOL = "tcp";
    public static final String OPENFLOW_SECURE_PROTOCOL = "ssl";
    public static final short OPENFLOW_PORT = 6653;
    public static final String OVSDB_URI_PREFIX = "ovsdb";
    public static final String BRIDGE_URI_PREFIX = "bridge";

    private final UtilsProvider provider;

    @Deprecated
    public SouthboundUtils(ControllerMdsalUtils mdsalUtils) {
        provider = new ControllerUtilsProvider(mdsalUtils);
    }

    public SouthboundUtils(MdsalUtils mdsalUtils) {
        provider = new MdsalUtilsProvider(mdsalUtils);
    }

    public static NodeId createNodeId(IpAddress ip, PortNumber port) {
        String uriString = OVSDB_URI_PREFIX + "://" + ip.stringValue() + ":" + port.getValue();
        Uri uri = new Uri(uriString);
        return new NodeId(uri);
    }

    public static Node createNode(ConnectionInfo key) {
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setNodeId(createNodeId(key.getRemoteIp(), key.getRemotePort()));
        nodeBuilder.addAugmentation(OvsdbNodeAugmentation.class, createOvsdbAugmentation(key));
        return nodeBuilder.build();
    }

    public static OvsdbNodeAugmentation createOvsdbAugmentation(ConnectionInfo key) {
        OvsdbNodeAugmentationBuilder ovsdbNodeBuilder = new OvsdbNodeAugmentationBuilder();
        ovsdbNodeBuilder.setConnectionInfo(key);
        return ovsdbNodeBuilder.build();
    }

    public static InstanceIdentifier<Node> createInstanceIdentifier(NodeId nodeId) {
        return InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(OVSDB_TOPOLOGY_ID))
                .child(Node.class,new NodeKey(nodeId));
    }

    public static InstanceIdentifier<Node> createInstanceIdentifier(NodeKey ovsdbNodeKey, String bridgeName) {
        return createInstanceIdentifier(createManagedNodeId(ovsdbNodeKey.getNodeId(), bridgeName));
    }

    public static InstanceIdentifier<Node> createInstanceIdentifier(ConnectionInfo key) {
        return createInstanceIdentifier(key.getRemoteIp(), key.getRemotePort());
    }

    public static InstanceIdentifier<Node> createInstanceIdentifier(IpAddress ip, PortNumber port) {
        InstanceIdentifier<Node> path = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(OVSDB_TOPOLOGY_ID))
                .child(Node.class,createNodeKey(ip,port));
        LOG.debug("Created ovsdb path: {}",path);
        return path;
    }

    public static InstanceIdentifier<Node> createInstanceIdentifier(ConnectionInfo key, OvsdbBridgeName bridgeName) {
        return createInstanceIdentifier(createManagedNodeId(key, bridgeName));
    }

    public static InstanceIdentifier<Node> createInstanceIdentifier(ConnectionInfo key, String bridgeName) {
        return createInstanceIdentifier(key, new OvsdbBridgeName(bridgeName));
    }

    public static NodeKey createNodeKey(IpAddress ip, PortNumber port) {
        return new NodeKey(createNodeId(ip, port));
    }

    public static NodeId createManagedNodeId(NodeId ovsdbNodeId, String bridgeName) {
        return new NodeId(ovsdbNodeId.getValue()
                + "/" + BRIDGE_URI_PREFIX + "/" + bridgeName);
    }

    public static NodeId createManagedNodeId(ConnectionInfo key, OvsdbBridgeName bridgeName) {
        return createManagedNodeId(key.getRemoteIp(), key.getRemotePort(), bridgeName);
    }

    public static NodeId createManagedNodeId(IpAddress ip, PortNumber port, OvsdbBridgeName bridgeName) {
        return new NodeId(createNodeId(ip,port).getValue()
                + "/" + BRIDGE_URI_PREFIX + "/" + bridgeName.getValue());
    }

    public static NodeId createManagedNodeId(InstanceIdentifier<Node> iid) {
        NodeKey nodeKey = iid.firstKeyOf(Node.class);
        return nodeKey.getNodeId();
    }

    public OvsdbNodeAugmentation extractOvsdbNode(Node node) {
        return node.augmentation(OvsdbNodeAugmentation.class);
    }

    public static IpAddress createIpAddress(InetAddress address) {
        IpAddress ip = null;
        if (address instanceof Inet4Address) {
            ip = createIpAddress((Inet4Address)address);
        } else if (address instanceof Inet6Address) {
            ip = createIpAddress((Inet6Address)address);
        }
        return ip;
    }

    public static IpAddress createIpAddress(Inet4Address address) {
        return IetfInetUtil.INSTANCE.ipAddressFor(address);
    }

    public static IpAddress createIpAddress(Inet6Address address) {
        Ipv6Address ipv6 = new Ipv6Address(address.getHostAddress());
        return new IpAddress(ipv6);
    }

    public ConnectionInfo getConnectionInfo(Node ovsdbNode) {
        ConnectionInfo connectionInfo = null;
        OvsdbNodeAugmentation ovsdbNodeAugmentation = extractOvsdbNode(ovsdbNode);
        if (ovsdbNodeAugmentation != null) {
            connectionInfo = ovsdbNodeAugmentation.getConnectionInfo();
        }
        return connectionInfo;
    }

    public static ConnectionInfo getConnectionInfo(final String addressStr, final String portStr) {
        InetAddress inetAddress = null;
        try {
            inetAddress = InetAddress.getByName(addressStr);
        } catch (UnknownHostException e) {
            LOG.warn("Could not allocate InetAddress", e);
        }

        IpAddress address = createIpAddress(inetAddress);
        PortNumber port = new PortNumber(Integer.parseInt(portStr));

        LOG.info("connectionInfo: {}", new ConnectionInfoBuilder()
                .setRemoteIp(address)
                .setRemotePort(port)
                .build());
        return new ConnectionInfoBuilder()
                .setRemoteIp(address)
                .setRemotePort(port)
                .build();
    }

    public static String connectionInfoToString(final ConnectionInfo connectionInfo) {
        return connectionInfo.getRemoteIp().stringValue() + ":" + connectionInfo.getRemotePort().getValue();
    }

    public boolean addOvsdbNode(final ConnectionInfo connectionInfo, long timeout) {
        boolean result = provider.put(LogicalDatastoreType.CONFIGURATION,
                createInstanceIdentifier(connectionInfo),
                createNode(connectionInfo));
        if (timeout != 0) {
            try {
                Thread.sleep(timeout);
            } catch (InterruptedException e) {
                LOG.warn("Interrupted while waiting after adding OVSDB node {}",
                        connectionInfoToString(connectionInfo), e);
            }
        }
        return result;
    }

    public Node getOvsdbNode(final ConnectionInfo connectionInfo) {
        return provider.read(LogicalDatastoreType.OPERATIONAL,
                createInstanceIdentifier(connectionInfo));
    }

    public boolean deleteOvsdbNode(final ConnectionInfo connectionInfo, long timeout) {
        boolean result = provider.delete(LogicalDatastoreType.CONFIGURATION,
                createInstanceIdentifier(connectionInfo));
        if (timeout != 0) {
            try {
                Thread.sleep(timeout);
            } catch (InterruptedException e) {
                LOG.warn("Interrupted while waiting after deleting OVSDB node {}",
                        connectionInfoToString(connectionInfo), e);
            }
        }
        return result;
    }

    public boolean disconnectOvsdbNode(final ConnectionInfo connectionInfo, long timeout) {
        deleteOvsdbNode(connectionInfo, timeout);
        LOG.info("Disconnected from {}", connectionInfoToString(connectionInfo));
        return true;
    }

    /**
     * Extract the <code>store</code> type data store contents for the particular bridge identified by
     * <code>bridgeName</code>.
     *
     * @param connectionInfo address for the node
     * @param bridgeName name of the bridge
     * @param store defined by the <code>LogicalDatastoreType</code> enumeration
     * @return <code>store</code> type data store contents
     */
    @Deprecated
    public OvsdbBridgeAugmentation getBridge(ConnectionInfo connectionInfo, String bridgeName,
            org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType store) {
        return getBridge(connectionInfo, bridgeName, store.toMdsal());
    }

    /**
     * Extract the <code>store</code> type data store contents for the particular bridge identified by
     * <code>bridgeName</code>.
     *
     * @param connectionInfo address for the node
     * @param bridgeName name of the bridge
     * @param store defined by the <code>LogicalDatastoreType</code> enumeration
     * @return <code>store</code> type data store contents
     */
    public OvsdbBridgeAugmentation getBridge(ConnectionInfo connectionInfo, String bridgeName,
                                              LogicalDatastoreType store) {
        OvsdbBridgeAugmentation ovsdbBridgeAugmentation = null;
        Node bridgeNode = getBridgeNode(connectionInfo, bridgeName, store);
        if (bridgeNode != null) {
            ovsdbBridgeAugmentation = bridgeNode.augmentation(OvsdbBridgeAugmentation.class);
        }
        return ovsdbBridgeAugmentation;
    }

    /**
     * Extract the <code>LogicalDataStoreType.OPERATIONAL</code> type data store contents for the particular bridge
     * identified by <code>bridgeName</code>.
     *
     * @param connectionInfo address for the node
     * @param bridgeName name of the bridge
     * @see <code>NetvirtIT.getBridge(ConnectionInfo, String, LogicalDatastoreType)</code>
     * @return <code>LogicalDatastoreType.OPERATIONAL</code> type data store contents
     */
    public OvsdbBridgeAugmentation getBridge(ConnectionInfo connectionInfo, String bridgeName) {
        return getBridge(connectionInfo, bridgeName, LogicalDatastoreType.OPERATIONAL);
    }

    /**
     * Extract the node contents from <code>store</code> type data store for the
     * bridge identified by <code>bridgeName</code>.
     *
     * @param connectionInfo address for the node
     * @param bridgeName name of the bridge
     * @param store defined by the <code>LogicalDatastoreType</code> enumeration
     * @return <code>store</code> type data store contents
     */
    @Deprecated
    public Node getBridgeNode(ConnectionInfo connectionInfo, String bridgeName,
            org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType store) {
        return getBridgeNode(connectionInfo, bridgeName, store.toMdsal());
    }

    /**
     * Extract the node contents from <code>store</code> type data store for the
     * bridge identified by <code>bridgeName</code>.
     *
     * @param connectionInfo address for the node
     * @param bridgeName name of the bridge
     * @param store defined by the <code>LogicalDatastoreType</code> enumeration
     * @return <code>store</code> type data store contents
     */
    public Node getBridgeNode(ConnectionInfo connectionInfo, String bridgeName, LogicalDatastoreType store) {
        InstanceIdentifier<Node> bridgeIid = createInstanceIdentifier(connectionInfo, new OvsdbBridgeName(bridgeName));
        return provider.read(store, bridgeIid);
    }

    public Node getBridgeNode(Node node, String bridgeName) {
        OvsdbBridgeAugmentation bridge = extractBridgeAugmentation(node);
        if (bridge != null && bridge.getBridgeName().getValue().equals(bridgeName)) {
            return node;
        } else {
            return readBridgeNode(node, bridgeName);
        }
    }

    public Node readBridgeNode(Node node, String name) {
        Node ovsdbNode = node;
        if (extractNodeAugmentation(ovsdbNode) == null) {
            ovsdbNode = readOvsdbNode(node);
            if (ovsdbNode == null) {
                return null;
            }
        }
        Node bridgeNode = null;
        ConnectionInfo connectionInfo = getConnectionInfo(ovsdbNode);
        if (connectionInfo != null) {
            InstanceIdentifier<Node> bridgeIid =
                    createInstanceIdentifier(node.key(), name);
            bridgeNode = provider.read(LogicalDatastoreType.OPERATIONAL, bridgeIid);
        }
        return bridgeNode;
    }

    public OvsdbNodeAugmentation extractNodeAugmentation(Node node) {
        return node.augmentation(OvsdbNodeAugmentation.class);
    }

    public OvsdbBridgeAugmentation extractBridgeAugmentation(Node node) {
        if (node == null) {
            return null;
        }
        return node.augmentation(OvsdbBridgeAugmentation.class);
    }

    public Node readOvsdbNode(Node bridgeNode) {
        Node ovsdbNode = null;
        OvsdbBridgeAugmentation bridgeAugmentation = extractBridgeAugmentation(bridgeNode);
        if (bridgeAugmentation != null) {
            InstanceIdentifier<Node> ovsdbNodeIid =
                    (InstanceIdentifier<Node>) bridgeAugmentation.getManagedBy().getValue();
            ovsdbNode = provider.read(LogicalDatastoreType.OPERATIONAL, ovsdbNodeIid);
        } else {
            LOG.debug("readOvsdbNode: Provided node is not a bridge node : {}",bridgeNode);
        }
        return ovsdbNode;
    }

    public boolean deleteBridge(final ConnectionInfo connectionInfo, final String bridgeName, long timeout) {
        boolean result = provider.delete(LogicalDatastoreType.CONFIGURATION,
                createInstanceIdentifier(connectionInfo, new OvsdbBridgeName(bridgeName)));
        if (timeout != 0) {
            try {
                Thread.sleep(timeout);
            } catch (InterruptedException e) {
                LOG.warn("Interrupted while waiting after deleting bridge {}", bridgeName, e);
            }
        }
        return result;
    }

    private String getControllerIPAddress() {
        String addressString = ConfigProperties.getProperty(this.getClass(), "ovsdb.controller.address");
        if (addressString != null) {
            try {
                if (InetAddress.getByName(addressString) != null) {
                    return addressString;
                }
            } catch (UnknownHostException e) {
                LOG.error("Host {} is invalid", addressString, e);
            }
        }

        addressString = ConfigProperties.getProperty(this.getClass(), "of.address");
        if (addressString != null) {
            try {
                if (InetAddress.getByName(addressString) != null) {
                    return addressString;
                }
            } catch (UnknownHostException e) {
                LOG.error("Host {} is invalid", addressString, e);
            }
        }

        return null;
    }

    private short getControllerOFPort() {
        short openFlowPort = OPENFLOW_PORT;
        String portString = ConfigProperties.getProperty(this.getClass(), "of.listenPort");
        if (portString != null) {
            try {
                openFlowPort = Short.parseShort(portString);
            } catch (NumberFormatException e) {
                LOG.warn("Invalid port:{}, use default({})", portString,
                        openFlowPort, e);
            }
        }
        return openFlowPort;
    }

    public List<String> getControllersFromOvsdbNode(Node node) {
        List<String> controllersStr = new ArrayList<>();

        String controllerIpStr = getControllerIPAddress();
        if (controllerIpStr != null) {
            // If codepath makes it here, the ip address to be used was explicitly provided.
            // Being so, also fetch openflowPort provided via ConfigProperties.
            controllersStr.add(OPENFLOW_CONNECTION_PROTOCOL
                    + ":" + controllerIpStr + ":" + getControllerOFPort());
        } else {
            // Check if ovsdb node has manager entries
            OvsdbNodeAugmentation ovsdbNodeAugmentation = extractOvsdbNode(node);
            if (ovsdbNodeAugmentation != null) {
                List<ManagerEntry> managerEntries = ovsdbNodeAugmentation.getManagerEntry();
                if (managerEntries != null && !managerEntries.isEmpty()) {
                    for (ManagerEntry managerEntry : managerEntries) {
                        if (managerEntry == null || managerEntry.getTarget() == null) {
                            continue;
                        }
                        String managerStr = managerEntry.getTarget().getValue().toLowerCase(Locale.ROOT);
                        int firstColonIdx = managerStr.indexOf(':');
                        int lastColonIdx = managerStr.lastIndexOf(':');
                        if (lastColonIdx <= firstColonIdx) {
                            continue;
                        }
                        controllerIpStr = managerStr.substring(firstColonIdx + 1, lastColonIdx);
                        if (managerStr.startsWith("tcp")) {
                            controllersStr.add(OPENFLOW_CONNECTION_PROTOCOL + ":" + controllerIpStr + ":"
                                + getControllerOFPort());
                        } else if (managerStr.startsWith("ssl")) {
                            controllersStr.add(OPENFLOW_SECURE_PROTOCOL + ":" + controllerIpStr + ":"
                                + getControllerOFPort());
                        } else if (managerStr.startsWith("ptcp")) {
                            ConnectionInfo connectionInfo = ovsdbNodeAugmentation.getConnectionInfo();
                            /* If we're connected to switch ptcp, only then use connection info
                                to configure controller. Ptcp is configured as:
                                Manager "ptcp:<port>:<ip>"
                                ip is optional
                             */
                            String managerPortStr =  managerStr.split(":", 3)[1];
                            if (connectionInfo != null && connectionInfo.getLocalIp() != null
                                    && connectionInfo.getRemotePort() != null
                                    && managerPortStr.equals(connectionInfo.getRemotePort().toString())) {
                                IpAddress controllerIp = connectionInfo.getLocalIp();
                                if (controllerIp.getIpv6Address() != null) {
                                    controllerIpStr = "[" + connectionInfo.getLocalIp().stringValue() + "]";
                                } else {
                                    controllerIpStr = connectionInfo.getLocalIp().stringValue();
                                }
                                controllersStr.add(OPENFLOW_CONNECTION_PROTOCOL
                                    + ":" + controllerIpStr + ":" + OPENFLOW_PORT);
                            } else {
                                LOG.warn("Ovsdb Node does not contain connection info: {}", node);
                            }
                        } else if (managerStr.startsWith("pssl")) {
                            ConnectionInfo connectionInfo = ovsdbNodeAugmentation.getConnectionInfo();
                            if (connectionInfo != null && connectionInfo.getLocalIp() != null) {
                                controllerIpStr = connectionInfo.getLocalIp().stringValue();
                                controllersStr.add(OPENFLOW_SECURE_PROTOCOL
                                    + ":" + controllerIpStr + ":" + OPENFLOW_PORT);
                            } else {
                                LOG.warn("Ovsdb Node does not contain connection info: {}", node);
                            }
                        } else {
                            LOG.trace("Skipping manager entry {} for node {}",
                                    managerEntry.getTarget(), node.getNodeId().getValue());
                        }
                    }
                } else {
                    LOG.warn("Ovsdb Node does not contain manager entries : {}", node);
                }
            }
        }

        if (controllersStr.isEmpty()) {
            LOG.warn("Failed to determine OpenFlow controller ip address");
        } else if (LOG.isDebugEnabled()) {
            LOG.debug("Found {} OpenFlow Controller(s) :{}", controllersStr.size(),
                    controllersStr.stream().collect(Collectors.joining(" ")));
        }
        return controllersStr;
    }

    public long getDataPathId(Node node) {
        long dpid = 0L;
        String datapathId = getDatapathId(node);
        if (datapathId != null) {
            dpid = new BigInteger(datapathId.replaceAll(":", ""), 16).longValue();
        }
        return dpid;
    }

    public String getDatapathId(Node node) {
        OvsdbBridgeAugmentation ovsdbBridgeAugmentation = node.augmentation(OvsdbBridgeAugmentation.class);
        return getDatapathId(ovsdbBridgeAugmentation);
    }

    public String getDatapathId(OvsdbBridgeAugmentation ovsdbBridgeAugmentation) {
        String datapathId = null;
        if (ovsdbBridgeAugmentation != null && ovsdbBridgeAugmentation.getDatapathId() != null) {
            datapathId = ovsdbBridgeAugmentation.getDatapathId().getValue();
        }
        return datapathId;
    }

}
