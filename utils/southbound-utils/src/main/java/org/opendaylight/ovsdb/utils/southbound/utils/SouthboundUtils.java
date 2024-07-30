/*
 * Copyright (c) 2015, 2018 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.utils.southbound.utils;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeDpdkvhostuserclient;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.BridgeExternalIdsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.BridgeExternalIdsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.BridgeOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.BridgeOtherConfigsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.BridgeOtherConfigsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ProtocolEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ProtocolEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ProtocolEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.InterfaceTypeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.InterfaceTypeEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagedNodeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagedNodeEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagerEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagerEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchOtherConfigsKey;
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
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.binding.Key;
import org.opendaylight.yangtools.binding.KeyStep;
import org.opendaylight.yangtools.binding.NodeStep;
import org.opendaylight.yangtools.binding.PropertyIdentifier;
import org.opendaylight.yangtools.binding.util.BindingMap;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
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
    private static final int OVSDB_UPDATE_TIMEOUT = 1000;
    public static final TopologyId OVSDB_TOPOLOGY_ID = new TopologyId(new Uri("ovsdb:1"));
    public static final String OPENFLOW_CONNECTION_PROTOCOL = "tcp";
    public static final String OPENFLOW_SECURE_PROTOCOL = "ssl";
    public static final short OPENFLOW_PORT = 6653;
    public static final String OVSDB_URI_PREFIX = "ovsdb";
    public static final String BRIDGE_URI_PREFIX = "bridge";
    private static final String DISABLE_IN_BAND = "disable-in-band";
    private static final String PATCH_PORT_TYPE = "patch";
    // External ID key used for mapping between an OVSDB port and an interface name
    private static final InterfaceExternalIdsKey EXTERNAL_INTERFACE_ID_KEY = new InterfaceExternalIdsKey("iface-id");
    private static final String CREATED_BY = "created_by";
    private static final String ODL = "odl";
    private static final String FORMAT = "(\\d+)\\.(\\d+)\\.(\\d+)";
    private static final Pattern PATTERN = Pattern.compile(FORMAT);
    // DPDK interface type
    private static final InterfaceTypeEntryKey DPDK_IFACE_KEY = new InterfaceTypeEntryKey(InterfaceTypeDpdk.VALUE);

    private final UtilsProvider provider;

    public SouthboundUtils(MdsalUtils mdsalUtils) {
        provider = new MdsalUtilsProvider(mdsalUtils);
    }

    public static final ImmutableBiMap<String, InterfaceTypeBase> OVSDB_INTERFACE_TYPE_MAP =
        ImmutableBiMap.<String, InterfaceTypeBase>builder()
            .put("internal", InterfaceTypeInternal.VALUE)
            .put("vxlan", InterfaceTypeVxlan.VALUE)
            .put("vxlan-gpe", InterfaceTypeVxlanGpe.VALUE)
            .put("patch", InterfaceTypePatch.VALUE)
            .put("system", InterfaceTypeSystem.VALUE)
            .put("tap", InterfaceTypeTap.VALUE)
            .put("geneve", InterfaceTypeGeneve.VALUE)
            .put("gre", InterfaceTypeGre.VALUE)
            .put("ipsec_gre", InterfaceTypeIpsecGre.VALUE)
            .put("gre64", InterfaceTypeGre64.VALUE)
            .put("ipsec_gre64", InterfaceTypeIpsecGre64.VALUE)
            .put("lisp", InterfaceTypeLisp.VALUE)
            .put("dpdk", InterfaceTypeDpdk.VALUE)
            .put("dpdkr", InterfaceTypeDpdkr.VALUE)
            .put("dpdkvhost", InterfaceTypeDpdkvhost.VALUE)
            .put("dpdkvhostuser", InterfaceTypeDpdkvhostuser.VALUE)
            .put("dpdkvhostuserclient", InterfaceTypeDpdkvhostuserclient.VALUE)
            .build();

    public static final ImmutableBiMap<OvsdbBridgeProtocolBase, String> OVSDB_PROTOCOL_MAP =
        ImmutableBiMap.<OvsdbBridgeProtocolBase, String>builder()
            .put(OvsdbBridgeProtocolOpenflow10.VALUE, "OpenFlow10")
            .put(OvsdbBridgeProtocolOpenflow11.VALUE, "OpenFlow11")
            .put(OvsdbBridgeProtocolOpenflow12.VALUE, "OpenFlow12")
            .put(OvsdbBridgeProtocolOpenflow13.VALUE, "OpenFlow13")
            .put(OvsdbBridgeProtocolOpenflow14.VALUE, "OpenFlow14")
            .put(OvsdbBridgeProtocolOpenflow15.VALUE, "OpenFlow15")
            .build();

    private static final ImmutableBiMap<OvsdbFailModeBase, String> OVSDB_FAIL_MODE_MAP =
        ImmutableBiMap.<OvsdbFailModeBase, String>builder()
            .put(OvsdbFailModeStandalone.VALUE, "standalone")
            .put(OvsdbFailModeSecure.VALUE, "secure")
            .build();

    private static final BridgeOtherConfigs OTHER_CONFIG_DISABLE_INBAND = new BridgeOtherConfigsBuilder()
                .setBridgeOtherConfigKey(DISABLE_IN_BAND)
                .setBridgeOtherConfigValue("true")
                .build();
    private static final Map<BridgeOtherConfigsKey, BridgeOtherConfigs> DEFAULT_OTHER_CONFIGS =
        BindingMap.of(OTHER_CONFIG_DISABLE_INBAND);

    public static NodeId createNodeId(IpAddress ip, PortNumber port) {
        String uriString = OVSDB_URI_PREFIX + "://" + ip.stringValue() + ":" + port.getValue();
        Uri uri = new Uri(uriString);
        return new NodeId(uri);
    }

    public static Node createNode(ConnectionInfo key) {
        return new NodeBuilder()
                .setNodeId(createNodeId(key.getRemoteIp(), key.getRemotePort()))
                .addAugmentation(createOvsdbAugmentation(key))
                .build();
    }

    public static OvsdbNodeAugmentation createOvsdbAugmentation(ConnectionInfo key) {
        return new OvsdbNodeAugmentationBuilder().setConnectionInfo(key).build();
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

    public InstanceIdentifier<TerminationPoint> createTerminationPointInstanceIdentifier(Node node, String portName) {

        InstanceIdentifier<TerminationPoint> terminationPointPath = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(OVSDB_TOPOLOGY_ID))
                .child(Node.class,node.key())
                .child(TerminationPoint.class, new TerminationPointKey(new TpId(portName)));

        LOG.debug("Termination point InstanceIdentifier generated : {}",terminationPointPath);
        return terminationPointPath;
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
        return IetfInetUtil.ipAddressFor(address);
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
        PortNumber port = new PortNumber(Uint16.valueOf(portStr));

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

    public boolean addOvsdbNode(final ConnectionInfo connectionInfo) {
        return addOvsdbNode(connectionInfo, OVSDB_UPDATE_TIMEOUT);
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

    public boolean deleteOvsdbNode(final ConnectionInfo connectionInfo) {
        return deleteOvsdbNode(connectionInfo, OVSDB_UPDATE_TIMEOUT);
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
                    ((DataObjectIdentifier<Node>) bridgeAugmentation.getManagedBy().getValue()).toLegacy();
            ovsdbNode = provider.read(LogicalDatastoreType.OPERATIONAL, ovsdbNodeIid);
        } else {
            LOG.debug("readOvsdbNode: Provided node is not a bridge node : {}",bridgeNode);
        }
        return ovsdbNode;
    }

    public boolean deleteBridge(final ConnectionInfo connectionInfo, final String bridgeName) {
        return deleteBridge(connectionInfo, bridgeName, OVSDB_UPDATE_TIMEOUT);
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

    public Map<ProtocolEntryKey, ProtocolEntry> createMdsalProtocols() {
        final ProtocolEntry entry = new ProtocolEntryBuilder()
                .setProtocol(OVSDB_PROTOCOL_MAP.inverse().get("OpenFlow13"))
                .build();
        return Map.of(entry.key(), entry);
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
                             final OvsdbFailModeBase failMode, final boolean setManagedBy,
                             final DatapathTypeBase dpType,
                             final Map<BridgeExternalIdsKey, BridgeExternalIds> externalIds,
                             final Map<ControllerEntryKey, ControllerEntry> controllerEntries,
                             final Map<BridgeOtherConfigsKey, BridgeOtherConfigs> otherConfigs,
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
        bridgeNodeBuilder.addAugmentation(ovsdbBridgeAugmentationBuilder.build());
        LOG.debug("Built with the intent to store bridge data {}",
                ovsdbBridgeAugmentationBuilder.toString());
        boolean result = provider.merge(LogicalDatastoreType.CONFIGURATION,
                bridgeIid, bridgeNodeBuilder.build());
        if (timeout != 0) {
            Thread.sleep(OVSDB_UPDATE_TIMEOUT);
        }
        return result;
    }

    public boolean addBridge(Node ovsdbNode, String bridgeName, List<String> controllersStr, DatapathTypeBase dpType,
            String mac) {
        return addBridge(ovsdbNode, bridgeName, controllersStr, dpType, mac, null, null);
    }

    public boolean addBridge(Node ovsdbNode, String bridgeName, List<String> controllersStr, DatapathTypeBase dpType,
                             String mac, Long maxBackoff, Long inactivityProbe) {
        List<BridgeOtherConfigs> otherConfigs = new ArrayList<>();
        if (mac != null) {
            otherConfigs.add(new BridgeOtherConfigsBuilder()
                .setBridgeOtherConfigKey("hwaddr")
                .setBridgeOtherConfigValue(mac).build());
        }

        return addBridge(ovsdbNode, bridgeName, controllersStr, dpType, otherConfigs, null, null);
    }

    public boolean addBridge(Node ovsdbNode, String bridgeName, List<String> controllersStr, DatapathTypeBase dpType,
            List<BridgeOtherConfigs> otherConfigs, Uint32 maxBackoff, Uint32 inactivityProbe) {
        LOG.info("addBridge: node: {}, bridgeName: {}, controller(s): {}", ovsdbNode, bridgeName, controllersStr);
        ConnectionInfo connectionInfo = getConnectionInfo(ovsdbNode);
        if (connectionInfo == null) {
            throw new InvalidParameterException("Could not find ConnectionInfo");
        }

        OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder()
            .setControllerEntry(createControllerEntries(controllersStr, maxBackoff, inactivityProbe))
            .setBridgeName(new OvsdbBridgeName(bridgeName))
            .setProtocolEntry(createMdsalProtocols())
            .setFailMode(OVSDB_FAIL_MODE_MAP.inverse().get("secure"))
            .setBridgeExternalIds(setBridgeExternalIds())
            .setManagedBy(new OvsdbNodeRef(createInstanceIdentifier(ovsdbNode.key().getNodeId()).toIdentifier()))
            // TODO: Currently netvirt relies on this function to set disabled-in-band=true. However,
            // TODO (cont): a better design would be to have netvirt pass that in. That way this function
            // TODO (cont): can take a null otherConfigs to erase other_configs.
            .setBridgeOtherConfigs(disableInBand(otherConfigs));


        if (dpType != null) {
            ovsdbBridgeAugmentationBuilder.setDatapathType(dpType);
        }
        if (isOvsdbNodeDpdk(ovsdbNode)) {
            ovsdbBridgeAugmentationBuilder.setDatapathType(DatapathTypeNetdev.VALUE);
        }

        InstanceIdentifier<Node> bridgeIid = createInstanceIdentifier(ovsdbNode.key(), bridgeName);
        Node node = new NodeBuilder()
            .addAugmentation(ovsdbBridgeAugmentationBuilder.build())
            .setNodeId(createManagedNodeId(bridgeIid))
            .build();
        boolean result = provider.put(LogicalDatastoreType.CONFIGURATION, bridgeIid, node);
        LOG.info("addBridge: result: {}", result);
        return result;
    }

    private static Map<BridgeOtherConfigsKey, BridgeOtherConfigs> disableInBand(List<BridgeOtherConfigs> otherConfigs) {
        return otherConfigs == null || otherConfigs.isEmpty() ? DEFAULT_OTHER_CONFIGS
            : BindingMap.<BridgeOtherConfigsKey, BridgeOtherConfigs>orderedBuilder(otherConfigs.size() + 1)
                .addAll(otherConfigs)
                .add(OTHER_CONFIG_DISABLE_INBAND)
                .build();
    }

    /**
     * Set the controllers of an existing bridge node.
     *
     * @param ovsdbNode where the bridge is
     * @param bridgeName Name of the bridge
     * @param controllers controller strings
     * @return success if the write to md-sal was successful
     */
    public boolean setBridgeController(Node ovsdbNode, String bridgeName, List<String> controllers) {
        return setBridgeController(ovsdbNode, bridgeName, controllers, null, null);
    }

    /**
     * Set the controllers of an existing bridge node.
     *
     * @param ovsdbNode where the bridge is
     * @param bridgeName Name of the bridge
     * @param controllers controller strings
     * @param maxBackoff Max backoff in milliseconds
     * @param inactivityProbe inactivity probe in milliseconds
     * @return success if the write to md-sal was successful
     */
    public boolean setBridgeController(Node ovsdbNode, String bridgeName, List<String> controllers,
            Uint32 maxBackoff, Uint32 inactivityProbe) {
        LOG.debug("setBridgeController: ovsdbNode: {}, bridgeNode: {}, controller(s): {}",
                ovsdbNode, bridgeName, controllers);

        InstanceIdentifier<Node> bridgeNodeIid = createInstanceIdentifier(ovsdbNode.key(), bridgeName);
        Node bridgeNode = provider.read(LogicalDatastoreType.CONFIGURATION, bridgeNodeIid);
        if (bridgeNode == null) {
            LOG.info("setBridgeController could not find bridge in configuration {}", bridgeNodeIid);
            return false;
        }

        OvsdbBridgeAugmentation bridgeAug = extractBridgeAugmentation(bridgeNode);

        Map<ControllerEntryKey, ControllerEntry> currentControllerEntries =
            createControllerEntries(controllers, maxBackoff, inactivityProbe);

        final Map<ControllerEntryKey, ControllerEntry> newControllerEntries;
        // Only add controller entries that do not already exist on this bridge
        Map<ControllerEntryKey, ControllerEntry> existingControllerEntries = bridgeAug.getControllerEntry();
        if (existingControllerEntries != null) {
            newControllerEntries = currentControllerEntries.values().stream()
                .filter(entry -> !existingControllerEntries.containsKey(new ControllerEntryKey(entry.getTarget())))
                .collect(BindingMap.toOrderedMap());
        } else {
            newControllerEntries = currentControllerEntries;
        }

        if (newControllerEntries.isEmpty()) {
            return true;
        }

        InstanceIdentifier<Node> bridgeIid = createInstanceIdentifier(ovsdbNode.key(), bridgeName);
        return provider.merge(LogicalDatastoreType.CONFIGURATION, bridgeIid, new NodeBuilder(bridgeNode)
            .addAugmentation(new OvsdbBridgeAugmentationBuilder(bridgeAug)
                .setControllerEntry(newControllerEntries)
                .build())
            .build());
    }

    private static void setManagedBy(final OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder,
                              final ConnectionInfo connectionInfo) {
        InstanceIdentifier<Node> connectionNodePath = createInstanceIdentifier(connectionInfo);
        ovsdbBridgeAugmentationBuilder.setManagedBy(new OvsdbNodeRef(connectionNodePath.toIdentifier()));
    }

    public boolean addTerminationPoint(
            Node bridgeNode, String portName, String type, Map<String, String> options,
            Map<String, String> externalIds) {
        return addTerminationPoint(bridgeNode, portName, type, options, externalIds, null);
    }

    public boolean addTerminationPoint(
            Node bridgeNode, String portName, String type, Map<String, String> options, Map<String, String> externalIds,
            Uint32 ofPort) {
        OvsdbTerminationPointAugmentationBuilder tpAugmentationBuilder = new OvsdbTerminationPointAugmentationBuilder();

        tpAugmentationBuilder.setName(portName);
        tpAugmentationBuilder.setOfport(ofPort);
        if (type != null) {
            tpAugmentationBuilder.setInterfaceType(OVSDB_INTERFACE_TYPE_MAP.get(type));
        }

        if (options != null && options.size() > 0) {
            tpAugmentationBuilder.setOptions(buildOptions(options));
        }

        if (externalIds != null && externalIds.size() > 0) {
            final ImmutableMap.Builder<InterfaceExternalIdsKey, InterfaceExternalIds> builder =
                    ImmutableMap.builderWithExpectedSize(externalIds.size());
            for (Map.Entry<String, String> entry : externalIds.entrySet()) {
                final InterfaceExternalIdsKey key = new InterfaceExternalIdsKey(entry.getKey());
                builder.put(key, new InterfaceExternalIdsBuilder()
                    .withKey(key)
                    .setExternalIdValue(entry.getValue())
                    .build());
            }
            tpAugmentationBuilder.setInterfaceExternalIds(builder.build());
        }

        InstanceIdentifier<TerminationPoint> tpIid = createTerminationPointInstanceIdentifier(bridgeNode, portName);
        return provider.merge(LogicalDatastoreType.CONFIGURATION, tpIid, new TerminationPointBuilder()
            .withKey(InstanceIdentifier.keyOf(tpIid))
            .addAugmentation(tpAugmentationBuilder.build())
            .build());
    }

    public Boolean addTerminationPoint(Node bridgeNode, String portName, String type) {
        return addTerminationPoint(bridgeNode, portName, type, Collections.emptyMap(), null);
    }

    public Boolean addTerminationPoint(Node bridgeNode, String bridgeName, String portName,
                                       String type, Map<String, String> options) {
        OvsdbTerminationPointAugmentationBuilder tpAugmentationBuilder = new OvsdbTerminationPointAugmentationBuilder();

        tpAugmentationBuilder.setName(portName);
        if (type != null) {
            tpAugmentationBuilder.setInterfaceType(OVSDB_INTERFACE_TYPE_MAP.get(type));
        }

        tpAugmentationBuilder.setOptions(buildOptions(options));

        InstanceIdentifier<TerminationPoint> tpIid = createTerminationPointInstanceIdentifier(bridgeNode, portName);
        return provider.merge(LogicalDatastoreType.CONFIGURATION, tpIid, new TerminationPointBuilder()
            .withKey(InstanceIdentifier.keyOf(tpIid))
            .addAugmentation(tpAugmentationBuilder.build())
            .build());
    }

    public Boolean addTerminationPoint(Node bridgeNode, String bridgeName, String portName, String type) {
        InstanceIdentifier<TerminationPoint> tpIid = createTerminationPointInstanceIdentifier(bridgeNode, portName);
        OvsdbTerminationPointAugmentationBuilder tpAugmentationBuilder =
                new OvsdbTerminationPointAugmentationBuilder();

        tpAugmentationBuilder.setName(portName);
        if (type != null) {
            tpAugmentationBuilder.setInterfaceType(OVSDB_INTERFACE_TYPE_MAP.get(type));
        }
        return provider.merge(LogicalDatastoreType.CONFIGURATION, tpIid, new TerminationPointBuilder()
            .withKey(InstanceIdentifier.keyOf(tpIid))
            .addAugmentation(tpAugmentationBuilder.build())
            .build());
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
                LOG.warn("Invalid port:{}, use default({})", portString, openFlowPort, e);
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
                Map<ManagerEntryKey, ManagerEntry> managerEntries = ovsdbNodeAugmentation.getManagerEntry();
                if (managerEntries != null && !managerEntries.isEmpty()) {
                    for (ManagerEntry managerEntry : managerEntries.values()) {
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
            dpid = new BigInteger(datapathId.replace(":", ""), 16).longValue();
        }
        return dpid;
    }

    public String getDataPathIdStr(final Node node) {
        if (node != null) {
            long dpId = getDataPathId(node);
            if (dpId != 0) {
                return String.valueOf(dpId);
            }
        }

        return null;
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

    public String extractBridgeName(Node node) {
        return node.augmentation(OvsdbBridgeAugmentation.class).getBridgeName().getValue();
    }

    public boolean isBridgeOnOvsdbNode(Node ovsdbNode, String bridgeName) {
        OvsdbNodeAugmentation ovsdbNodeAugmentation = extractNodeAugmentation(ovsdbNode);
        if (ovsdbNodeAugmentation != null) {
            Map<ManagedNodeEntryKey, ManagedNodeEntry> managedNodes = ovsdbNodeAugmentation.getManagedNodeEntry();
            if (managedNodes != null) {
                for (ManagedNodeEntry managedNode : managedNodes.values()) {
                    if (matchesBridgeName(managedNode, bridgeName)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // see OVSDB-470 for background
    private static boolean matchesBridgeName(ManagedNodeEntry managedNode, String bridgeName) {
        final var iid = switch (managedNode.getBridgeRef().getValue()) {
            case DataObjectIdentifier<?> doi -> doi;
            case PropertyIdentifier<?, ?> pi -> pi.container();
        };

        InstanceIdentifier<?> bridgeIid = iid.toLegacy();
        for (var bridgeIidPathArg : bridgeIid.getPathArguments()) {
            if (bridgeIidPathArg instanceof KeyStep<?, ?> identifiableItem) {
                Key<?> key = identifiableItem.key();
                if (key instanceof NodeKey nodeKey) {
                    if (nodeKey.getNodeId().getValue().contains(bridgeName)) {
                        return true;
                    }
                } else if (key.toString().contains(bridgeName)) {
                    return true;
                }
            } else if (bridgeIidPathArg instanceof NodeStep<?> item) {
                if (item.type().getName().contains(bridgeName)) {
                    return true;
                }
            } else {
                throw new IllegalArgumentException("Unknown kind of PathArgument: " + bridgeIidPathArg);
            }
        }
        return false;
    }

    public OvsdbBridgeAugmentation getBridgeFromConfig(Node node, String bridge) {
        OvsdbBridgeAugmentation ovsdbBridgeAugmentation = null;
        InstanceIdentifier<Node> bridgeIid =
                createInstanceIdentifier(node.key(), bridge);
        Node bridgeNode = provider.read(LogicalDatastoreType.CONFIGURATION, bridgeIid);
        if (bridgeNode != null) {
            ovsdbBridgeAugmentation = bridgeNode.augmentation(OvsdbBridgeAugmentation.class);
        }
        return ovsdbBridgeAugmentation;
    }

    public boolean isOvsdbNodeDpdk(Node ovsdbNode) {
        OvsdbNodeAugmentation ovsdbNodeAugmentation = extractNodeAugmentation(ovsdbNode);
        if (ovsdbNodeAugmentation != null) {
            Map<InterfaceTypeEntryKey, InterfaceTypeEntry> ifTypes = ovsdbNodeAugmentation.getInterfaceTypeEntry();
            if (ifTypes != null) {
                if (ifTypes.containsKey(DPDK_IFACE_KEY)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Map<ControllerEntryKey, ControllerEntry> createControllerEntries(List<String> controllersStr,
            Uint32 maxBackoff, Uint32 inactivityProbe) {
        if (controllersStr == null) {
            return Map.of();
        }

        BindingMap.Builder<ControllerEntryKey, ControllerEntry> controllerEntries =
            BindingMap.orderedBuilder(controllersStr.size());
        for (String controllerStr : controllersStr) {
            controllerEntries.add(new ControllerEntryBuilder()
                .setTarget(new Uri(controllerStr))
                .setMaxBackoff(maxBackoff)
                .setInactivityProbe(inactivityProbe)
                .build());
        }
        return controllerEntries.build();
    }

    public OvsdbTerminationPointAugmentation extractTerminationPointAugmentation(Node bridgeNode, String portName) {
        if (bridgeNode.augmentation(OvsdbBridgeAugmentation.class) != null) {
            List<OvsdbTerminationPointAugmentation> tpAugmentations = extractTerminationPointAugmentations(bridgeNode);
            for (OvsdbTerminationPointAugmentation ovsdbTerminationPointAugmentation : tpAugmentations) {
                if (ovsdbTerminationPointAugmentation.getName().equals(portName)) {
                    return ovsdbTerminationPointAugmentation;
                }
            }
        }
        return null;
    }

    public List<OvsdbTerminationPointAugmentation> extractTerminationPointAugmentations(Node node) {
        List<OvsdbTerminationPointAugmentation> tpAugmentations = new ArrayList<>();
        if (node == null) {
            LOG.error("extractTerminationPointAugmentations: Node value is null");
            return Collections.emptyList();
        }
        Map<TerminationPointKey, TerminationPoint> terminationPoints = node.getTerminationPoint();
        if (terminationPoints != null && !terminationPoints.isEmpty()) {
            for (TerminationPoint tp : terminationPoints.values()) {
                OvsdbTerminationPointAugmentation ovsdbTerminationPointAugmentation =
                        tp.augmentation(OvsdbTerminationPointAugmentation.class);
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
     */
    public OvsdbTerminationPointAugmentation getTerminationPointOfBridge(Node node, String portName) {
        OvsdbTerminationPointAugmentation tpAugmentation = extractTerminationPointAugmentation(node,portName);
        if (tpAugmentation == null) {
            List<OvsdbTerminationPointAugmentation> tpAugmentations = readTerminationPointAugmentations(node);
            if (tpAugmentations != null) {
                for (OvsdbTerminationPointAugmentation ovsdbTpAugmentation : tpAugmentations) {
                    if (ovsdbTpAugmentation.getName().equals(portName)) {
                        return ovsdbTpAugmentation;
                    }
                }
            }
        }
        return tpAugmentation;
    }

    /**
     * Read the list of <code>OvsdbTerminationPointAugmentation</code> for the particular <code>node</code>.
     */
    public List<OvsdbTerminationPointAugmentation> readTerminationPointAugmentations(Node node) {
        if (node == null) {
            LOG.error("readTerminationPointAugmentations: Node value is null");
            return Collections.emptyList();
        }
        Node operNode = provider.read(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier
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
    public Map<NodeKey, Node> getOvsdbNodes() {
        InstanceIdentifier<Topology> inst = InstanceIdentifier.create(NetworkTopology.class).child(Topology.class,
                new TopologyKey(OVSDB_TOPOLOGY_ID));
        Topology topology = provider.read(LogicalDatastoreType.OPERATIONAL, inst);
        return topology != null ? topology.getNode() : null;
    }

    /**
     * Get OpenvSwitch other-config by key.
     * @param node OVSDB node
     * @param key key to extract from other-config
     * @return the value for key or null if key not found
     */
    public String getOpenvswitchOtherConfig(Node node, String key) {
        OvsdbNodeAugmentation ovsdbNode = node.augmentation(OvsdbNodeAugmentation.class);
        if (ovsdbNode == null) {
            Node nodeFromReadOvsdbNode = readOvsdbNode(node);
            if (nodeFromReadOvsdbNode != null) {
                ovsdbNode = nodeFromReadOvsdbNode.augmentation(OvsdbNodeAugmentation.class);
            }
        }

        if (ovsdbNode != null) {
            final OpenvswitchOtherConfigs found = ovsdbNode.nonnullOpenvswitchOtherConfigs()
                    .get(new OpenvswitchOtherConfigsKey(key));
            if (found != null) {
                return found.getOtherConfigValue();
            }
        }

        return null;
    }

    public static TerminationPoint getTerminationPointByExternalId(final Node bridgeNode, final String interfaceName) {
        final Map<TerminationPointKey, TerminationPoint> tps = bridgeNode.getTerminationPoint();
        if (tps != null) {
            for (TerminationPoint tp : tps.values()) {
                OvsdbTerminationPointAugmentation ovsdbTp = tp.augmentation(OvsdbTerminationPointAugmentation.class);
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

    // This utility shouldn't be called often, as it reads all OVSDB nodes each time - not good for scale
    public Node getNodeByTerminationPointExternalId(final String interfaceName) {
        Map<NodeKey, Node> nodes = getOvsdbNodes();
        if (nodes != null) {
            for (Node node : nodes.values()) {
                TerminationPoint tp = getTerminationPointByExternalId(node, interfaceName);
                if (tp != null) {
                    return node;
                }
            }
        }
        return null;
    }

    public static String getExternalInterfaceIdValue(final OvsdbTerminationPointAugmentation ovsdbTp) {
        if (ovsdbTp != null) {
            Map<InterfaceExternalIdsKey, InterfaceExternalIds> ifaceExtIds = ovsdbTp.getInterfaceExternalIds();
            if (ifaceExtIds != null) {
                final InterfaceExternalIds entry = ifaceExtIds.get(EXTERNAL_INTERFACE_ID_KEY);
                if (entry != null) {
                    return entry.getExternalIdValue();
                }
            }
        }
        return null;
    }

    public String getDatapathIdFromNodeInstanceId(InstanceIdentifier<Node> nodeInstanceId) {
        Node node = provider.read(LogicalDatastoreType.OPERATIONAL, nodeInstanceId);
        String dpId = node != null ? getDataPathIdStr(node) : null;
        if (dpId != null) {
            return dpId;
        }
        return null;
    }

    public static boolean compareDbVersionToMinVersion(final String dbVersion, final String minVersion) {
        if (dbVersion == null || minVersion == null) {
            LOG.error("Invalid DB version {} or minVersion {}", dbVersion, minVersion);
            return false;
        }
        final Matcher dbVersionMatcher = PATTERN.matcher(dbVersion);
        final Matcher minVersionMatcher = PATTERN.matcher(minVersion);
        LOG.debug("dbVersion {}, minVersion {}", dbVersion, minVersion);
        if (!dbVersionMatcher.find()) {
            LOG.error("Invalid DB version format {}", dbVersion);
            return false;
        }
        if (!minVersionMatcher.find()) {
            LOG.error("Invalid Min DB version format {}", minVersion);
            return false;
        }

        if (dbVersion != null && !dbVersion.isEmpty() && minVersion != null && !minVersion.isEmpty()) {
            final int dbVersionMatch1 = Integer.parseInt(dbVersionMatcher.group(1));
            final int dbVersionMatch2 = Integer.parseInt(dbVersionMatcher.group(2));
            final int dbVersionMatch3 = Integer.parseInt(dbVersionMatcher.group(3));
            final int minVersionMatch1 = Integer.parseInt(minVersionMatcher.group(1));
            final int minVersionMatch2 = Integer.parseInt(minVersionMatcher.group(2));
            final int minVersionMatch3 = Integer.parseInt(minVersionMatcher.group(3));
            if (dbVersionMatch1 == minVersionMatch1 && dbVersionMatch2 == minVersionMatch2
                    && dbVersionMatch3 == minVersionMatch3) {
                return true;
            }

            if (dbVersionMatch1 > minVersionMatch1) {
                return true;
            }

            if (dbVersionMatch1 < minVersionMatch1) {
                return false;
            }

            // major version is equal
            if (dbVersionMatch2 > minVersionMatch2) {
                return true;
            }

            if (dbVersionMatch2 < minVersionMatch2) {
                return false;
            }

            if (dbVersionMatch3 > minVersionMatch3) {
                return true;
            }
        }
        return false;
    }

    private static Map<BridgeExternalIdsKey, BridgeExternalIds> setBridgeExternalIds() {
        final BridgeExternalIds ids = new BridgeExternalIdsBuilder()
                .setBridgeExternalIdKey(CREATED_BY)
                .setBridgeExternalIdValue(ODL)
                .build();
        return Map.of(ids.key(), ids);
    }

    private static Map<OptionsKey, Options> buildOptions(final Map<String, String> options) {
        final ImmutableMap.Builder<OptionsKey, Options> builder = ImmutableMap.builderWithExpectedSize(options.size());
        for (Map.Entry<String, String> entry : options.entrySet()) {
            final OptionsKey key = new OptionsKey(entry.getKey());
            builder.put(key, new OptionsBuilder().withKey(key).setValue(entry.getValue()).build());
        }
        return builder.build();
    }
}
