/*
 * Copyright (c) 2015 - 2016 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.utils.southbound.utils;

import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.utils.config.ConfigProperties;
import org.opendaylight.ovsdb.utils.mdsal.utils.MdsalUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IetfInetUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathTypeNetdev;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeDpdk;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeDpdkr;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeDpdkvhost;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeDpdkvhostuser;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeGeneve;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeGre64;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeInternal;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeIpsecGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeIpsecGre64;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeLisp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypePatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeSystem;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeTap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeVxlanGpe;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeProtocolBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeProtocolOpenflow10;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeProtocolOpenflow11;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeProtocolOpenflow12;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeProtocolOpenflow13;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeProtocolOpenflow14;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeProtocolOpenflow15;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbFailModeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbFailModeSecure;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbFailModeStandalone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.BridgeExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.BridgeOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.BridgeOtherConfigsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ProtocolEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ProtocolEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.InterfaceTypeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagedNodeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagerEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceExternalIdsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceExternalIdsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.Options;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.OptionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.OptionsKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableBiMap;

public class SouthboundUtils {
    private static final Logger LOG = LoggerFactory.getLogger(SouthboundUtils.class);
    private static final int OVSDB_UPDATE_TIMEOUT = 1000;
    public static final TopologyId OVSDB_TOPOLOGY_ID = new TopologyId(new Uri("ovsdb:1"));
    private final MdsalUtils mdsalUtils;
    public static final String OPENFLOW_CONNECTION_PROTOCOL = "tcp";
    public static short OPENFLOW_PORT = 6653;
    public static final String OVSDB_URI_PREFIX = "ovsdb";
    public static final String BRIDGE_URI_PREFIX = "bridge";
    private static final String DISABLE_IN_BAND = "disable-in-band";
    private static final String PATCH_PORT_TYPE = "patch";
    // External ID key used for mapping between an OVSDB port and an interface name
    private static final String EXTERNAL_INTERFACE_ID_KEY = "iface-id";

    public SouthboundUtils(MdsalUtils mdsalUtils) {
        this.mdsalUtils = mdsalUtils;
    }

    public static final ImmutableBiMap<String, Class<? extends InterfaceTypeBase>> OVSDB_INTERFACE_TYPE_MAP
            = new ImmutableBiMap.Builder<String, Class<? extends InterfaceTypeBase>>()
            .put("internal", InterfaceTypeInternal.class)
            .put("vxlan", InterfaceTypeVxlan.class)
            .put("vxlan-gpe", InterfaceTypeVxlanGpe.class)
            .put("patch", InterfaceTypePatch.class)
            .put("system", InterfaceTypeSystem.class)
            .put("tap", InterfaceTypeTap.class)
            .put("geneve", InterfaceTypeGeneve.class)
            .put("gre", InterfaceTypeGre.class)
            .put("ipsec_gre", InterfaceTypeIpsecGre.class)
            .put("gre64", InterfaceTypeGre64.class)
            .put("ipsec_gre64", InterfaceTypeIpsecGre64.class)
            .put("lisp", InterfaceTypeLisp.class)
            .put("dpdk", InterfaceTypeDpdk.class)
            .put("dpdkr", InterfaceTypeDpdkr.class)
            .put("dpdkvhost", InterfaceTypeDpdkvhost.class)
            .put("dpdkvhostuser", InterfaceTypeDpdkvhostuser.class)
            .build();

    public static final ImmutableBiMap<Class<? extends OvsdbBridgeProtocolBase>,String> OVSDB_PROTOCOL_MAP
            = new ImmutableBiMap.Builder<Class<? extends OvsdbBridgeProtocolBase>,String>()
            .put(OvsdbBridgeProtocolOpenflow10.class,"OpenFlow10")
            .put(OvsdbBridgeProtocolOpenflow11.class,"OpenFlow11")
            .put(OvsdbBridgeProtocolOpenflow12.class,"OpenFlow12")
            .put(OvsdbBridgeProtocolOpenflow13.class,"OpenFlow13")
            .put(OvsdbBridgeProtocolOpenflow14.class,"OpenFlow14")
            .put(OvsdbBridgeProtocolOpenflow15.class,"OpenFlow15")
            .build();

    private static final ImmutableBiMap<Class<? extends OvsdbFailModeBase>,String> OVSDB_FAIL_MODE_MAP
            = new ImmutableBiMap.Builder<Class<? extends OvsdbFailModeBase>,String>()
            .put(OvsdbFailModeStandalone.class,"standalone")
            .put(OvsdbFailModeSecure.class,"secure")
            .build();

    public static NodeId createNodeId(IpAddress ip, PortNumber port) {
        String uriString = OVSDB_URI_PREFIX + "://"
                + String.valueOf(ip.getValue()) + ":" + port.getValue();
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

    public static NodeId createManagedNodeId(NodeId ovsdbNodeId, String bridgeName) {
        return new NodeId(ovsdbNodeId.getValue()
                + "/" + BRIDGE_URI_PREFIX + "/" + bridgeName);
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

    public static InstanceIdentifier<Node> createInstanceIdentifier(ConnectionInfo key,OvsdbBridgeName bridgeName) {
        return createInstanceIdentifier(createManagedNodeId(key, bridgeName));
    }

    public static InstanceIdentifier<Node> createInstanceIdentifier(ConnectionInfo key, String bridgeName) {
        return createInstanceIdentifier(key, new OvsdbBridgeName(bridgeName));
    }

    public InstanceIdentifier<TerminationPoint> createTerminationPointInstanceIdentifier(Node node, String portName){

        InstanceIdentifier<TerminationPoint> terminationPointPath = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(OVSDB_TOPOLOGY_ID))
                .child(Node.class,node.getKey())
                .child(TerminationPoint.class, new TerminationPointKey(new TpId(portName)));

        LOG.debug("Termination point InstanceIdentifier generated : {}",terminationPointPath);
        return terminationPointPath;
    }

    public static NodeKey createNodeKey(IpAddress ip, PortNumber port) {
        return new NodeKey(createNodeId(ip, port));
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

    public ConnectionInfo getConnectionInfo(Node ovsdbNode) {
        ConnectionInfo connectionInfo = null;
        OvsdbNodeAugmentation ovsdbNodeAugmentation = extractOvsdbNode(ovsdbNode);
        if (ovsdbNodeAugmentation != null) {
            connectionInfo = ovsdbNodeAugmentation.getConnectionInfo();
        }
        return connectionInfo;
    }

    public OvsdbNodeAugmentation extractOvsdbNode(Node node) {
        return node.getAugmentation(OvsdbNodeAugmentation.class);
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
        return String.valueOf(
                connectionInfo.getRemoteIp().getValue()) + ":" + connectionInfo.getRemotePort().getValue();
    }

    public boolean addOvsdbNode(final ConnectionInfo connectionInfo) {
        return addOvsdbNode(connectionInfo, OVSDB_UPDATE_TIMEOUT);
    }

    public boolean addOvsdbNode(final ConnectionInfo connectionInfo, long timeout) {
        boolean result = mdsalUtils.put(LogicalDatastoreType.CONFIGURATION,
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
        return mdsalUtils.read(LogicalDatastoreType.OPERATIONAL,
                createInstanceIdentifier(connectionInfo));
    }

    public boolean deleteOvsdbNode(final ConnectionInfo connectionInfo) {
        return deleteOvsdbNode(connectionInfo, OVSDB_UPDATE_TIMEOUT);
    }

    public boolean deleteOvsdbNode(final ConnectionInfo connectionInfo, long timeout) {
        boolean result = mdsalUtils.delete(LogicalDatastoreType.CONFIGURATION,
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

    public Node connectOvsdbNode(final ConnectionInfo connectionInfo) {
        return connectOvsdbNode(connectionInfo, OVSDB_UPDATE_TIMEOUT);
    }

    public Node connectOvsdbNode(final ConnectionInfo connectionInfo, long timeout) {
        addOvsdbNode(connectionInfo, timeout);
        Node node = getOvsdbNode(connectionInfo);
        LOG.info("Connected to {}", connectionInfoToString(connectionInfo));
        return node;
    }

    public boolean disconnectOvsdbNode(final ConnectionInfo connectionInfo) {
        return disconnectOvsdbNode(connectionInfo, OVSDB_UPDATE_TIMEOUT);
    }

    public boolean disconnectOvsdbNode(final ConnectionInfo connectionInfo, long timeout) {
        deleteOvsdbNode(connectionInfo, timeout);
        LOG.info("Disconnected from {}", connectionInfoToString(connectionInfo));
        return true;
    }

    public List<ControllerEntry> createControllerEntry(String controllerTarget) {
        List<ControllerEntry> controllerEntriesList = new ArrayList<>();
        controllerEntriesList.add(new ControllerEntryBuilder()
                .setTarget(new Uri(controllerTarget))
                .build());
        return controllerEntriesList;
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
            ovsdbBridgeAugmentation = bridgeNode.getAugmentation(OvsdbBridgeAugmentation.class);
        }
        return ovsdbBridgeAugmentation;
    }

    /**
     * extract the <code>LogicalDataStoreType.OPERATIONAL</code> type data store contents for the particular bridge
     * identified by <code>bridgeName</code>
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
    public Node getBridgeNode(ConnectionInfo connectionInfo, String bridgeName, LogicalDatastoreType store) {
        InstanceIdentifier<Node> bridgeIid = createInstanceIdentifier(connectionInfo, new OvsdbBridgeName(bridgeName));
        return mdsalUtils.read(store, bridgeIid);
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
                    createInstanceIdentifier(node.getKey(), name);
            bridgeNode = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, bridgeIid);
        }
        return bridgeNode;
    }

    public OvsdbNodeAugmentation extractNodeAugmentation(Node node) {
        return node.getAugmentation(OvsdbNodeAugmentation.class);
    }

    public OvsdbBridgeAugmentation extractBridgeAugmentation(Node node) {
        if (node == null) {
            return null;
        }
        return node.getAugmentation(OvsdbBridgeAugmentation.class);
    }

    public Node readOvsdbNode(Node bridgeNode) {
        Node ovsdbNode = null;
        OvsdbBridgeAugmentation bridgeAugmentation = extractBridgeAugmentation(bridgeNode);
        if (bridgeAugmentation != null) {
            InstanceIdentifier<Node> ovsdbNodeIid =
                    (InstanceIdentifier<Node>) bridgeAugmentation.getManagedBy().getValue();
            ovsdbNode = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, ovsdbNodeIid);
        }else{
            LOG.debug("readOvsdbNode: Provided node is not a bridge node : {}",bridgeNode);
        }
        return ovsdbNode;
    }

    public boolean deleteBridge(final ConnectionInfo connectionInfo, final String bridgeName) {
        return deleteBridge(connectionInfo, bridgeName, OVSDB_UPDATE_TIMEOUT);
    }

    public boolean deleteBridge(final ConnectionInfo connectionInfo, final String bridgeName, long timeout) {
        boolean result = mdsalUtils.delete(LogicalDatastoreType.CONFIGURATION,
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

    public List<ProtocolEntry> createMdsalProtocols() {
        List<ProtocolEntry> protocolList = new ArrayList<>();
        ImmutableBiMap<String, Class<? extends OvsdbBridgeProtocolBase>> mapper =
                OVSDB_PROTOCOL_MAP.inverse();
        protocolList.add(new ProtocolEntryBuilder().setProtocol(mapper.get("OpenFlow13")).build());
        return protocolList;
    }

    public boolean addBridge(final ConnectionInfo connectionInfo, InstanceIdentifier<Node> bridgeIid,
                             final String bridgeName, NodeId bridgeNodeId, final boolean setProtocolEntries,
                             final Class<? extends OvsdbFailModeBase> failMode, final boolean setManagedBy,
                             final Class<? extends DatapathTypeBase> dpType,
                             final List<BridgeExternalIds> externalIds,
                             final List<ControllerEntry> controllerEntries,
                             final List<BridgeOtherConfigs> otherConfigs,
                             final String dpid) throws InterruptedException {
        return addBridge(connectionInfo, bridgeIid, bridgeName, bridgeNodeId, setProtocolEntries, failMode,
                setManagedBy, dpType, externalIds, controllerEntries, otherConfigs, dpid);
    }

    /*
     * base method for adding test bridges.  Other helper methods used to create bridges should utilize this method.
     *
     * @param connectionInfo
     * @param bridgeIid if passed null, one is created
     * @param bridgeName cannot be null
     * @param bridgeNodeId if passed null, one is created based on <code>bridgeIid</code>
     * @param setProtocolEntries toggles whether default protocol entries are set for the bridge
     * @param failMode toggles whether default fail mode is set for the bridge
     * @param setManagedBy toggles whether to setManagedBy for the bridge
     * @param dpType if passed null, this parameter is ignored
     * @param externalIds if passed null, this parameter is ignored
     * @param otherConfig if passed null, this parameter is ignored
     * @return success of bridge addition
     * @throws InterruptedException
     */
    public boolean addBridge(final ConnectionInfo connectionInfo, InstanceIdentifier<Node> bridgeIid,
                             final String bridgeName, NodeId bridgeNodeId, final boolean setProtocolEntries,
                             final Class<? extends OvsdbFailModeBase> failMode, final boolean setManagedBy,
                             final Class<? extends DatapathTypeBase> dpType,
                             final List<BridgeExternalIds> externalIds,
                             final List<ControllerEntry> controllerEntries,
                             final List<BridgeOtherConfigs> otherConfigs,
                             final String dpid, long timeout) throws InterruptedException {

        NodeBuilder bridgeNodeBuilder = new NodeBuilder();
        if (bridgeIid == null) {
            bridgeIid = createInstanceIdentifier(connectionInfo, new OvsdbBridgeName(bridgeName));
        }
        if (bridgeNodeId == null) {
            bridgeNodeId = createManagedNodeId(bridgeIid);
        }
        bridgeNodeBuilder.setNodeId(bridgeNodeId);
        OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();
        ovsdbBridgeAugmentationBuilder.setBridgeName(new OvsdbBridgeName(bridgeName));
        if (setProtocolEntries) {
            ovsdbBridgeAugmentationBuilder.setProtocolEntry(createMdsalProtocols());
        }
        if (failMode != null) {
            ovsdbBridgeAugmentationBuilder.setFailMode(failMode);
        }
        if (setManagedBy) {
            setManagedBy(ovsdbBridgeAugmentationBuilder, connectionInfo);
        }
        if (dpType != null) {
            ovsdbBridgeAugmentationBuilder.setDatapathType(dpType);
        }
        if (externalIds != null) {
            ovsdbBridgeAugmentationBuilder.setBridgeExternalIds(externalIds);
        }
        if (controllerEntries != null) {
            ovsdbBridgeAugmentationBuilder.setControllerEntry(controllerEntries);
        }
        if (otherConfigs != null) {
            ovsdbBridgeAugmentationBuilder.setBridgeOtherConfigs(otherConfigs);
        }
        if (dpid != null && !dpid.isEmpty()) {
            DatapathId datapathId = new DatapathId(dpid);
            ovsdbBridgeAugmentationBuilder.setDatapathId(datapathId);
        }
        bridgeNodeBuilder.addAugmentation(OvsdbBridgeAugmentation.class, ovsdbBridgeAugmentationBuilder.build());
        LOG.debug("Built with the intent to store bridge data {}",
                ovsdbBridgeAugmentationBuilder.toString());
        boolean result = mdsalUtils.merge(LogicalDatastoreType.CONFIGURATION,
                bridgeIid, bridgeNodeBuilder.build());
        if (timeout != 0) {
            Thread.sleep(OVSDB_UPDATE_TIMEOUT);
        }
        return result;
    }

    /**
     * Set the controllers of an existing bridge node
     * @param ovsdbNode where the bridge is
     * @param bridgeName Name of the bridge
     * @param controllers controller strings
     * @return success if the write to md-sal was successful
     */
    public boolean setBridgeController(Node ovsdbNode, String bridgeName, List<String> controllers) {
        LOG.debug("setBridgeController: ovsdbNode: {}, bridgeNode: {}, controller(s): {}",
                ovsdbNode, bridgeName, controllers);

        InstanceIdentifier<Node> bridgeNodeIid = createInstanceIdentifier(ovsdbNode.getKey(), bridgeName);
        Node bridgeNode = mdsalUtils.read(LogicalDatastoreType.CONFIGURATION, bridgeNodeIid);
        if (bridgeNode == null) {
            LOG.info("setBridgeController could not find bridge in configuration {}", bridgeNodeIid);
            return false;
        }

        OvsdbBridgeAugmentation bridgeAug = extractBridgeAugmentation(bridgeNode);

        //Only add controller entries that do not already exist on this bridge
        List<ControllerEntry> existingControllerEntries = bridgeAug.getControllerEntry();
        List<ControllerEntry> newControllerEntries = new ArrayList<>();
        if(existingControllerEntries != null) {
            NEW_ENTRY_LOOP:
            for (ControllerEntry newEntry : createControllerEntries(controllers)) {
                for (ControllerEntry existingEntry : existingControllerEntries) {
                    if (newEntry.getTarget().equals(existingEntry.getTarget())) {
                        continue NEW_ENTRY_LOOP;
                    }
                }
                newControllerEntries.add(newEntry);
            }
        } else {
            newControllerEntries = createControllerEntries(controllers);
        }

        if(newControllerEntries.isEmpty()) {
            return true;
        }

        NodeBuilder nodeBuilder = new NodeBuilder(bridgeNode);
        OvsdbBridgeAugmentationBuilder augBuilder = new OvsdbBridgeAugmentationBuilder(bridgeAug);

        augBuilder.setControllerEntry(newControllerEntries);
        nodeBuilder.addAugmentation(OvsdbBridgeAugmentation.class, augBuilder.build());
        InstanceIdentifier<Node> bridgeIid = createInstanceIdentifier(ovsdbNode.getKey(), bridgeName);
        return mdsalUtils.merge(LogicalDatastoreType.CONFIGURATION, bridgeIid, nodeBuilder.build());
    }

    public boolean addBridge(Node ovsdbNode, String bridgeName, List<String> controllersStr,
                             final Class<? extends DatapathTypeBase> dpType, String mac) {
        boolean result;

        LOG.info("addBridge: node: {}, bridgeName: {}, controller(s): {}", ovsdbNode, bridgeName, controllersStr);
        ConnectionInfo connectionInfo = getConnectionInfo(ovsdbNode);
        if (connectionInfo != null) {
            NodeBuilder bridgeNodeBuilder = new NodeBuilder();
            InstanceIdentifier<Node> bridgeIid = createInstanceIdentifier(ovsdbNode.getKey(), bridgeName);
            NodeId bridgeNodeId = createManagedNodeId(bridgeIid);
            bridgeNodeBuilder.setNodeId(bridgeNodeId);
            OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();
            ovsdbBridgeAugmentationBuilder.setControllerEntry(createControllerEntries(controllersStr));
            ovsdbBridgeAugmentationBuilder.setBridgeName(new OvsdbBridgeName(bridgeName));
            ovsdbBridgeAugmentationBuilder.setProtocolEntry(createMdsalProtocols());
            ovsdbBridgeAugmentationBuilder.setFailMode( OVSDB_FAIL_MODE_MAP.inverse().get("secure"));
            BridgeOtherConfigsBuilder bridgeOtherConfigsBuilder = new BridgeOtherConfigsBuilder();
            bridgeOtherConfigsBuilder.setBridgeOtherConfigKey(DISABLE_IN_BAND);
            bridgeOtherConfigsBuilder.setBridgeOtherConfigValue("true");
            List<BridgeOtherConfigs> bridgeOtherConfigsList = new ArrayList<>();
            bridgeOtherConfigsList.add(bridgeOtherConfigsBuilder.build());
            if (mac != null) {
                BridgeOtherConfigsBuilder macOtherConfigBuilder = new BridgeOtherConfigsBuilder();
                macOtherConfigBuilder.setBridgeOtherConfigKey("hwaddr");
                macOtherConfigBuilder.setBridgeOtherConfigValue(mac);
                bridgeOtherConfigsList.add(macOtherConfigBuilder.build());
            }
            ovsdbBridgeAugmentationBuilder.setBridgeOtherConfigs(bridgeOtherConfigsList);
            setManagedByForBridge(ovsdbBridgeAugmentationBuilder, ovsdbNode.getKey());
            if (dpType != null) {
                ovsdbBridgeAugmentationBuilder.setDatapathType(dpType);
            }
            if (isOvsdbNodeDpdk(ovsdbNode)) {
                ovsdbBridgeAugmentationBuilder.setDatapathType(DatapathTypeNetdev.class);
            }
            bridgeNodeBuilder.addAugmentation(OvsdbBridgeAugmentation.class, ovsdbBridgeAugmentationBuilder.build());

            Node node = bridgeNodeBuilder.build();
            result = mdsalUtils.put(LogicalDatastoreType.CONFIGURATION, bridgeIid, node);
            LOG.info("addBridge: result: {}", result);
        } else {
            throw new InvalidParameterException("Could not find ConnectionInfo");
        }
        return result;
    }


    private void setManagedBy(final OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder,
                              final ConnectionInfo connectionInfo) {
        InstanceIdentifier<Node> connectionNodePath = createInstanceIdentifier(connectionInfo);
        ovsdbBridgeAugmentationBuilder.setManagedBy(new OvsdbNodeRef(connectionNodePath));
    }

    public boolean addTerminationPoint(
            Node bridgeNode, String portName, String type, Map<String, String> options,
            Map<String, String> externalIds) {
        return addTerminationPoint(bridgeNode, portName, type, options, externalIds, null);
    }

    public boolean addTerminationPoint(
            Node bridgeNode, String portName, String type, Map<String, String> options, Map<String, String> externalIds,
            Long ofPort) {
        InstanceIdentifier<TerminationPoint> tpIid = createTerminationPointInstanceIdentifier(bridgeNode, portName);
        OvsdbTerminationPointAugmentationBuilder tpAugmentationBuilder = new OvsdbTerminationPointAugmentationBuilder();

        tpAugmentationBuilder.setName(portName);
        tpAugmentationBuilder.setOfport(ofPort);
        if (type != null) {
            tpAugmentationBuilder.setInterfaceType(OVSDB_INTERFACE_TYPE_MAP.get(type));
        }

        if (options != null && options.size() > 0) {
            List<Options> optionsList = new ArrayList<>();
            for (Map.Entry<String, String> entry : options.entrySet()) {
                OptionsBuilder optionsBuilder = new OptionsBuilder();
                optionsBuilder.setKey(new OptionsKey(entry.getKey()));
                optionsBuilder.setOption(entry.getKey());
                optionsBuilder.setValue(entry.getValue());
                optionsList.add(optionsBuilder.build());
            }
            tpAugmentationBuilder.setOptions(optionsList);
        }

        if (externalIds != null && externalIds.size() > 0) {
            List<InterfaceExternalIds> externalIdsList = new ArrayList<>();
            for (Map.Entry<String, String> entry : externalIds.entrySet()) {
                InterfaceExternalIdsBuilder interfaceExternalIdsBuilder = new InterfaceExternalIdsBuilder();
                interfaceExternalIdsBuilder.setKey(new InterfaceExternalIdsKey(entry.getKey()));
                interfaceExternalIdsBuilder.setExternalIdKey(entry.getKey());
                interfaceExternalIdsBuilder.setExternalIdValue(entry.getValue());
                externalIdsList.add(interfaceExternalIdsBuilder.build());
            }
            tpAugmentationBuilder.setInterfaceExternalIds(externalIdsList);
        }

        TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
        tpBuilder.setKey(InstanceIdentifier.keyOf(tpIid));
        tpBuilder.addAugmentation(OvsdbTerminationPointAugmentation.class, tpAugmentationBuilder.build());
        /* TODO SB_MIGRATION should this be merge or mdsalUtils.put */
        return mdsalUtils.put(LogicalDatastoreType.CONFIGURATION, tpIid, tpBuilder.build());
    }

    public Boolean addTerminationPoint(Node bridgeNode, String portName, String type) {
        return addTerminationPoint(bridgeNode, portName, type, Collections.EMPTY_MAP, null);
    }

    public Boolean addTerminationPoint(Node bridgeNode, String bridgeName, String portName,
                                       String type, Map<String, String> options) {
        InstanceIdentifier<TerminationPoint> tpIid = createTerminationPointInstanceIdentifier(
                bridgeNode, portName);
        OvsdbTerminationPointAugmentationBuilder tpAugmentationBuilder = new OvsdbTerminationPointAugmentationBuilder();

        tpAugmentationBuilder.setName(portName);
        if (type != null) {
            tpAugmentationBuilder.setInterfaceType(OVSDB_INTERFACE_TYPE_MAP.get(type));
        }

        List<Options> optionsList = new ArrayList<>();
        for (Map.Entry<String, String> entry : options.entrySet()) {
            OptionsBuilder optionsBuilder = new OptionsBuilder();
            optionsBuilder.setKey(new OptionsKey(entry.getKey()));
            optionsBuilder.setOption(entry.getKey());
            optionsBuilder.setValue(entry.getValue());
            optionsList.add(optionsBuilder.build());
        }
        tpAugmentationBuilder.setOptions(optionsList);

        TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
        tpBuilder.setKey(InstanceIdentifier.keyOf(tpIid));
        tpBuilder.addAugmentation(OvsdbTerminationPointAugmentation.class, tpAugmentationBuilder.build());
        /* TODO SB_MIGRATION should this be merge or mdsalUtils.put */
        return mdsalUtils.put(LogicalDatastoreType.CONFIGURATION, tpIid, tpBuilder.build());
    }

    public Boolean addTerminationPoint(Node bridgeNode, String bridgeName, String portName, String type) {
        InstanceIdentifier<TerminationPoint> tpIid = createTerminationPointInstanceIdentifier(bridgeNode, portName);
        OvsdbTerminationPointAugmentationBuilder tpAugmentationBuilder =
                new OvsdbTerminationPointAugmentationBuilder();

        tpAugmentationBuilder.setName(portName);
        if (type != null) {
            tpAugmentationBuilder.setInterfaceType(OVSDB_INTERFACE_TYPE_MAP.get(type));
        }
        TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
        tpBuilder.setKey(InstanceIdentifier.keyOf(tpIid));
        tpBuilder.addAugmentation(OvsdbTerminationPointAugmentation.class, tpAugmentationBuilder.build());
        return mdsalUtils.put(LogicalDatastoreType.CONFIGURATION, tpIid, tpBuilder.build());
    }

    public Boolean addPatchTerminationPoint(Node node, String bridgeName, String portName, String peerPortName) {
        Map<String, String> option = new HashMap<>();
        option.put("peer", peerPortName);
        return addTerminationPoint(node, bridgeName, portName, PATCH_PORT_TYPE, option);
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
                        String[] tokens = managerEntry.getTarget().getValue().split(":");
                        if (tokens.length == 3 && tokens[0].equalsIgnoreCase("tcp")) {
                            controllersStr.add(OPENFLOW_CONNECTION_PROTOCOL
                                    + ":" + tokens[1] + ":" + getControllerOFPort());
                        } else if (tokens[0].equalsIgnoreCase("ptcp")) {
                            ConnectionInfo connectionInfo = ovsdbNodeAugmentation.getConnectionInfo();
                            if (connectionInfo != null && connectionInfo.getLocalIp() != null) {
                                controllerIpStr = String.valueOf(connectionInfo.getLocalIp().getValue());
                                controllersStr.add(OPENFLOW_CONNECTION_PROTOCOL
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
            // Neither user provided ip nor ovsdb node has manager entries. Lets use local machine ip address.
            LOG.debug("Use local machine ip address as a OpenFlow Controller ip address");
            controllerIpStr = getLocalControllerHostIpAddress();
            if (controllerIpStr != null) {
                controllersStr.add(OPENFLOW_CONNECTION_PROTOCOL
                        + ":" + controllerIpStr + ":" + OPENFLOW_PORT);
            }
        }

        if (controllersStr.isEmpty()) {
            LOG.warn("Failed to determine OpenFlow controller ip address");
        } else if (LOG.isDebugEnabled()) {
            controllerIpStr = "";
            for (String currControllerIpStr : controllersStr) {
                controllerIpStr += " " + currControllerIpStr;
            }
            LOG.debug("Found {} OpenFlow Controller(s) :{}", controllersStr.size(), controllerIpStr);
        }

        return controllersStr;
    }

    private String getLocalControllerHostIpAddress() {
        String ipaddress = null;
        try{
            for (Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces(); ifaces.hasMoreElements();){
                NetworkInterface iface = ifaces.nextElement();

                for (Enumeration<InetAddress> inetAddrs = iface.getInetAddresses(); inetAddrs.hasMoreElements();) {
                    InetAddress inetAddr = inetAddrs.nextElement();
                    if (!inetAddr.isLoopbackAddress() && inetAddr.isSiteLocalAddress()) {
                        ipaddress = inetAddr.getHostAddress();
                        break;
                    }
                }
            }
        }catch (Exception e){
            LOG.warn("Exception while fetching local host ip address ", e);
        }
        return ipaddress;
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
        OvsdbBridgeAugmentation ovsdbBridgeAugmentation = node.getAugmentation(OvsdbBridgeAugmentation.class);
        return getDatapathId(ovsdbBridgeAugmentation);
    }

    public String getDatapathId(OvsdbBridgeAugmentation ovsdbBridgeAugmentation) {
        String datapathId = null;
        if (ovsdbBridgeAugmentation != null && ovsdbBridgeAugmentation.getDatapathId() != null) {
            datapathId = ovsdbBridgeAugmentation.getDatapathId().getValue();
        }
        return datapathId;
    }

    public String extractBridgeName(Node node) {
        return node.getAugmentation(OvsdbBridgeAugmentation.class).getBridgeName().getValue();
    }

    public boolean isBridgeOnOvsdbNode(Node ovsdbNode, String bridgeName) {
        boolean found = false;
        OvsdbNodeAugmentation ovsdbNodeAugmentation = extractNodeAugmentation(ovsdbNode);
        if (ovsdbNodeAugmentation != null) {
            List<ManagedNodeEntry> managedNodes = ovsdbNodeAugmentation.getManagedNodeEntry();
            if (managedNodes != null) {
                for (ManagedNodeEntry managedNode : managedNodes) {
                    InstanceIdentifier<?> bridgeIid = managedNode.getBridgeRef().getValue();
                    if (bridgeIid.toString().contains(bridgeName)) {
                        found = true;
                        break;
                    }
                }
            }
        }
        return found;
    }

    public OvsdbBridgeAugmentation getBridgeFromConfig(Node node, String bridge) {
        OvsdbBridgeAugmentation ovsdbBridgeAugmentation = null;
        InstanceIdentifier<Node> bridgeIid =
                createInstanceIdentifier(node.getKey(), bridge);
        Node bridgeNode = mdsalUtils.read(LogicalDatastoreType.CONFIGURATION, bridgeIid);
        if (bridgeNode != null) {
            ovsdbBridgeAugmentation = bridgeNode.getAugmentation(OvsdbBridgeAugmentation.class);
        }
        return ovsdbBridgeAugmentation;
    }

    private void setManagedByForBridge(OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder,
                                       NodeKey ovsdbNodeKey) {
        InstanceIdentifier<Node> connectionNodePath = createInstanceIdentifier(ovsdbNodeKey.getNodeId());
        ovsdbBridgeAugmentationBuilder.setManagedBy(new OvsdbNodeRef(connectionNodePath));
    }

    public boolean isOvsdbNodeDpdk(Node ovsdbNode) {
        boolean found = false;
        OvsdbNodeAugmentation ovsdbNodeAugmentation = extractNodeAugmentation(ovsdbNode);
        if (ovsdbNodeAugmentation != null) {
            List<InterfaceTypeEntry> ifTypes = ovsdbNodeAugmentation.getInterfaceTypeEntry();
            if (ifTypes != null) {
                for (InterfaceTypeEntry ifType : ifTypes) {
                    if (ifType.getInterfaceType().equals(InterfaceTypeDpdk.class)) {
                        found = true;
                        break;
                    }
                }
            }
        }
        return found;
    }

    private List<ControllerEntry> createControllerEntries(List<String> controllersStr) {
        List<ControllerEntry> controllerEntries = new ArrayList<>();
        if (controllersStr != null) {
            for (String controllerStr : controllersStr) {
                ControllerEntryBuilder controllerEntryBuilder = new ControllerEntryBuilder();
                controllerEntryBuilder.setTarget(new Uri(controllerStr));
                controllerEntries.add(controllerEntryBuilder.build());
            }
        }
        return controllerEntries;
    }

    public OvsdbTerminationPointAugmentation extractTerminationPointAugmentation(Node bridgeNode, String portName) {
        if (bridgeNode.getAugmentation(OvsdbBridgeAugmentation.class) != null) {
            List<OvsdbTerminationPointAugmentation> tpAugmentations = extractTerminationPointAugmentations(bridgeNode);
            for (OvsdbTerminationPointAugmentation ovsdbTerminationPointAugmentation : tpAugmentations) {
                if (ovsdbTerminationPointAugmentation.getName().equals(portName)) {
                    return ovsdbTerminationPointAugmentation;
                }
            }
        }
        return null;
    }

    public List<OvsdbTerminationPointAugmentation> extractTerminationPointAugmentations( Node node ) {
        List<OvsdbTerminationPointAugmentation> tpAugmentations = new ArrayList<>();
        if (node == null) {
            LOG.error("extractTerminationPointAugmentations: Node value is null");
            return Collections.<OvsdbTerminationPointAugmentation>emptyList();
        }
        List<TerminationPoint> terminationPoints = node.getTerminationPoint();
        if(terminationPoints != null && !terminationPoints.isEmpty()){
            for(TerminationPoint tp : terminationPoints){
                OvsdbTerminationPointAugmentation ovsdbTerminationPointAugmentation =
                        tp.getAugmentation(OvsdbTerminationPointAugmentation.class);
                if (ovsdbTerminationPointAugmentation != null) {
                    tpAugmentations.add(ovsdbTerminationPointAugmentation);
                }
            }
        }
        return tpAugmentations;
    }

    /**
     * Extract the <code>OvsdbTerminationPointAugmentation</code> for the particular <code>node</code> identified by
     * <code>portName</code>.
     *
     * @param node
     * @param portName
     * @return
     */
    public OvsdbTerminationPointAugmentation getTerminationPointOfBridge(Node node, String portName) {
        OvsdbTerminationPointAugmentation tpAugmentation = extractTerminationPointAugmentation(node,portName);
        if(tpAugmentation == null){
            List<OvsdbTerminationPointAugmentation> tpAugmentations = readTerminationPointAugmentations(node);
            if(tpAugmentations != null){
                for(OvsdbTerminationPointAugmentation ovsdbTpAugmentation : tpAugmentations){
                    if(ovsdbTpAugmentation.getName().equals(portName)){
                        return ovsdbTpAugmentation;
                    }
                }
            }
        }
        return tpAugmentation;
    }

    /**
     * read the list of <code>OvsdbTerminationPointAugmentation</code> for the particular <code>node</code>.
     *
     * @param node
     * @return
     */
    public List<OvsdbTerminationPointAugmentation> readTerminationPointAugmentations(Node node) {
        if (node == null) {
            LOG.error("readTerminationPointAugmentations: Node value is null");
            return Collections.<OvsdbTerminationPointAugmentation>emptyList();
        }
        Node operNode = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(OVSDB_TOPOLOGY_ID))
                .child(Node.class, new NodeKey(node.getNodeId())));
        if (operNode != null) {
            return extractTerminationPointAugmentations(operNode);
        }
        return new ArrayList<>();
    }

    /**
     * Get all OVSDB nodes from topology.
     * @return a list of nodes or null if the topology could not found
     */
    public List<Node> getOvsdbNodes() {
        InstanceIdentifier<Topology> inst = InstanceIdentifier.create(NetworkTopology.class).child(Topology.class,
                new TopologyKey(OVSDB_TOPOLOGY_ID));
        Topology topology = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, inst);
        return topology != null ? topology.getNode() : null;
    }

    /**
     * Get OpenvSwitch other-config by key.
     * @param node OVSDB node
     * @param key key to extract from other-config
     * @return the value for key or null if key not found
     */
    public String getOpenvswitchOtherConfig(Node node, String key) {
        OvsdbNodeAugmentation ovsdbNode = node.getAugmentation(OvsdbNodeAugmentation.class);
        if (ovsdbNode == null) {
            Node nodeFromReadOvsdbNode = readOvsdbNode(node);
            if (nodeFromReadOvsdbNode != null) {
                ovsdbNode = nodeFromReadOvsdbNode.getAugmentation(OvsdbNodeAugmentation.class);
            }
        }

        if (ovsdbNode != null && ovsdbNode.getOpenvswitchOtherConfigs() != null) {
            for (OpenvswitchOtherConfigs openvswitchOtherConfigs : ovsdbNode.getOpenvswitchOtherConfigs()) {
                if (openvswitchOtherConfigs.getOtherConfigKey().equals(key)) {
                    return openvswitchOtherConfigs.getOtherConfigValue();
                }
            }
        }

        return null;
    }

    public TerminationPoint getTerminationPointByExternalId(final String interfaceName) {
        List<Node> nodes = getOvsdbNodes();
        if (nodes != null) {
            for (Node node : nodes) {
                TerminationPoint tp = getTerminationPointByExternalId(node, interfaceName);
                if (tp != null) {
                    return tp;
                }
            }
        }
        return null;
    }

    public static TerminationPoint getTerminationPointByExternalId(final Node bridgeNode, final String interfaceName) {
        if (bridgeNode.getTerminationPoint() != null) {
            for (TerminationPoint tp : bridgeNode.getTerminationPoint()) {
                OvsdbTerminationPointAugmentation ovsdbTp = tp.getAugmentation(OvsdbTerminationPointAugmentation.class);
                String externalIdValue = getExternalInterfaceIdValue(ovsdbTp);
                if (externalIdValue != null && externalIdValue.equals(interfaceName)) {
                    LOG.debug("Found matching termination point with iface-id {} on bridgeNode {}, returning tp {}",
                            interfaceName, bridgeNode, tp);
                    return tp;
                }
            }
        }
        return null;
    }

    public static String getExternalInterfaceIdValue(final OvsdbTerminationPointAugmentation ovsdbTp) {
        if (ovsdbTp != null) {
            List<InterfaceExternalIds> ifaceExtIds = ovsdbTp.getInterfaceExternalIds();
            if (ifaceExtIds != null) {
                for (InterfaceExternalIds entry : ifaceExtIds) {
                    if (entry.getExternalIdKey().equals(EXTERNAL_INTERFACE_ID_KEY)) {
                        return entry.getExternalIdValue();
                    }
                }
            }
        }
        return null;
    }

    public String getDatapathIdFromTerminationPoint(final OvsdbTerminationPointAugmentation ovsdbTp) {
        if (ovsdbTp == null) {
            return null;
        }

        List<Node> nodes = getOvsdbNodes();
        if (nodes == null) {
            return null;
        }

        for (Node node : nodes) {
            if (node.getTerminationPoint() == null) {
                continue;
            }
            for (TerminationPoint curTp : node.getTerminationPoint()) {
                OvsdbTerminationPointAugmentation curOvsdbTp = curTp.getAugmentation(OvsdbTerminationPointAugmentation.class);
                if (curOvsdbTp == null) {
                    continue;
                }
                if (curOvsdbTp.getPortUuid() != null && curOvsdbTp.getPortUuid().equals(ovsdbTp.getPortUuid())) {
                    long dpId = getDataPathId(node);
                    if (dpId == 0) {
                        return null;
                    }
                    return String.valueOf(dpId);
                }
            }
        }
        return null;
    }
}
