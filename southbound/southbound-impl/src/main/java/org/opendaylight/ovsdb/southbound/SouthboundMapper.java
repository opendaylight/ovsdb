/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound;

import static java.util.Objects.requireNonNull;
import static org.opendaylight.ovsdb.southbound.SouthboundUtil.schemaMismatchLog;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.net.InetAddresses;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.error.SchemaVersionMismatchException;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TypedDatabaseSchema;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.Controller;
import org.opendaylight.ovsdb.schema.openvswitch.Manager;
import org.opendaylight.ovsdb.schema.openvswitch.OpenVSwitch;
import org.opendaylight.ovsdb.schema.openvswitch.Qos;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IetfInetUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathTypeNetdev;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathTypeSystem;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeStt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeSystem;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeTap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeVxlanGpe;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeProtocolBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.QosTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.QosTypeEgressPolicer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.QosTypeLinuxCodel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.QosTypeLinuxFqCodel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.QosTypeLinuxHfsc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.QosTypeLinuxHtb;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.QosTypeLinuxSfq;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ProtocolEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ProtocolEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ProtocolEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagerEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagerEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagerEntryKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SouthboundMapper {
    private static final Logger LOG = LoggerFactory.getLogger(SouthboundMapper.class);
    private static final String N_CONNECTIONS_STR = "n_connections";

    @VisibleForTesting
    public static final ImmutableBiMap<DatapathTypeBase, String> DATAPATH_TYPE_MAP = ImmutableBiMap.of(
            DatapathTypeSystem.VALUE, "system",
            DatapathTypeNetdev.VALUE, "netdev");
    @VisibleForTesting
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
            .put("stt", InterfaceTypeStt.VALUE)
            .build();
    private static final ImmutableBiMap<QosTypeBase, String> QOS_TYPE_MAP =
        ImmutableBiMap.<QosTypeBase, String>builder()
            .put(QosTypeLinuxHtb.VALUE, SouthboundConstants.QOS_LINUX_HTB)
            .put(QosTypeLinuxHfsc.VALUE, SouthboundConstants.QOS_LINUX_HFSC)
            .put(QosTypeLinuxSfq.VALUE, SouthboundConstants.QOS_LINUX_SFQ)
            .put(QosTypeLinuxCodel.VALUE, SouthboundConstants.QOS_LINUX_CODEL)
            .put(QosTypeLinuxFqCodel.VALUE, SouthboundConstants.QOS_LINUX_FQ_CODEL)
            .put(QosTypeEgressPolicer.VALUE, SouthboundConstants.QOS_EGRESS_POLICER)
            .build();

    private SouthboundMapper() {
        // Hidden on purpose
    }

    public static IpAddress createIpAddress(final InetAddress address) {
        IpAddress ip = null;
        if (address instanceof Inet4Address) {
            ip = createIpAddress((Inet4Address)address);
        } else if (address instanceof Inet6Address) {
            ip = createIpAddress((Inet6Address)address);
        }
        return ip;
    }

    public static IpAddress createIpAddress(final Inet4Address address) {
        return IetfInetUtil.ipAddressFor(address);
    }

    public static IpAddress createIpAddress(final Inet6Address address) {
        Ipv6Address ipv6 = new Ipv6Address(address.getHostAddress());
        return new IpAddress(ipv6);
    }

    public static InstanceIdentifier<Topology> createTopologyInstanceIdentifier() {
        return InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID));
    }

    public static InstanceIdentifier<Node> createInstanceIdentifier(final NodeId nodeId) {
        return createTopologyInstanceIdentifier()
                .child(Node.class,new NodeKey(nodeId));
    }

    @SuppressWarnings("unchecked")
    public static InstanceIdentifier<Node> createInstanceIdentifier(
            final InstanceIdentifierCodec instanceIdentifierCodec, final OvsdbConnectionInstance client,
            final Bridge bridge) {
        InstanceIdentifier<Node> iid;
        if (bridge.getExternalIdsColumn() != null
                && bridge.getExternalIdsColumn().getData() != null
                && bridge.getExternalIdsColumn().getData().containsKey(SouthboundConstants.IID_EXTERNAL_ID_KEY)) {
            String iidString = bridge.getExternalIdsColumn().getData().get(SouthboundConstants.IID_EXTERNAL_ID_KEY);
            iid = (InstanceIdentifier<Node>) instanceIdentifierCodec.bindingDeserializerOrNull(iidString);
        } else {
            iid = createInstanceIdentifier(client, bridge.getName());
        }
        return iid;
    }

    @SuppressWarnings("unchecked")
    public static InstanceIdentifier<Node> createInstanceIdentifier(
            final InstanceIdentifierCodec instanceIdentifierCodec, final OvsdbConnectionInstance client,
            final Controller controller, final String bridgeName) {
        InstanceIdentifier<Node> iid;
        if (controller.getExternalIdsColumn() != null
                && controller.getExternalIdsColumn().getData() != null
                && controller.getExternalIdsColumn().getData().containsKey(SouthboundConstants.IID_EXTERNAL_ID_KEY)) {
            String iidString = controller.getExternalIdsColumn().getData().get(SouthboundConstants.IID_EXTERNAL_ID_KEY);
            iid = (InstanceIdentifier<Node>) instanceIdentifierCodec.bindingDeserializerOrNull(iidString);
        } else {
            iid = createInstanceIdentifier(client, bridgeName);
        }
        return iid;
    }

    public static InstanceIdentifier<Node> createInstanceIdentifier(
            final OvsdbConnectionInstance client, final String bridgeName) {
        String nodeString = client.getNodeKey().getNodeId().getValue()
                + "/bridge/" + bridgeName;
        NodeId nodeId = new NodeId(new Uri(nodeString));
        return createInstanceIdentifier(nodeId);

    }

    public static NodeId createManagedNodeId(final InstanceIdentifier<Node> iid) {
        NodeKey nodeKey = iid.firstKeyOf(Node.class);
        return nodeKey.getNodeId();
    }

    public static InetAddress createInetAddress(final IpAddress ip) throws UnknownHostException {
        if (ip.getIpv4Address() != null) {
            return InetAddresses.forString(ip.getIpv4Address().getValue());
        } else if (ip.getIpv6Address() != null) {
            return InetAddress.getByName(ip.getIpv6Address().getValue());
        } else {
            throw new UnknownHostException("IP Address has no value");
        }
    }

    public static DatapathId createDatapathId(final Bridge bridge) {
        requireNonNull(bridge);
        if (bridge.getDatapathIdColumn() == null) {
            return null;
        } else {
            return createDatapathId(bridge.getDatapathIdColumn().getData());
        }
    }

    public static DatapathId createDatapathId(final Set<String> dpids) {
        requireNonNull(dpids);
        if (dpids.isEmpty()) {
            return null;
        } else {
            String[] dpidArray = new String[dpids.size()];
            dpids.toArray(dpidArray);
            return createDatapathId(dpidArray[0]);
        }
    }

    public static DatapathId createDatapathId(final String dpid) {
        requireNonNull(dpid);
        DatapathId datapath;
        if (dpid.matches("^[0-9a-fA-F]{16}")) {
            Splitter splitter = Splitter.fixedLength(2);
            Joiner joiner = Joiner.on(":");
            datapath = new DatapathId(joiner.join(splitter.split(dpid)));
        } else {
            datapath = new DatapathId(dpid);
        }
        return datapath;
    }

    public static String createDatapathType(final OvsdbBridgeAugmentation mdsalbridge) {
        String datapathtype = DATAPATH_TYPE_MAP.get(DatapathTypeSystem.VALUE);

        if (mdsalbridge.getDatapathType() != null && !mdsalbridge.getDatapathType().equals(DatapathTypeBase.VALUE)) {
            datapathtype = DATAPATH_TYPE_MAP.get(mdsalbridge.getDatapathType());
            if (datapathtype == null) {
                throw new IllegalArgumentException("Unknown datapath type " + mdsalbridge.getDatapathType());
            }
        }
        return datapathtype;
    }

    public static DatapathTypeBase createDatapathType(final String type) {
        if (type.isEmpty()) {
            return DatapathTypeSystem.VALUE;
        }
        return DATAPATH_TYPE_MAP.inverse().get(type);
    }

    public static Set<String> createOvsdbBridgeProtocols(final OvsdbBridgeAugmentation ovsdbBridgeNode) {
        Set<String> protocols = new HashSet<>();
        Map<ProtocolEntryKey, ProtocolEntry> entries = ovsdbBridgeNode.getProtocolEntry();
        if (entries != null) {
            for (ProtocolEntry protocol : entries.values()) {
                final OvsdbBridgeProtocolBase lookup = protocol.getProtocol();
                final String toAdd = SouthboundConstants.OVSDB_PROTOCOL_MAP.get(protocol.getProtocol());
                Preconditions.checkArgument(toAdd != null, "Unknown protocol %s", lookup);
                protocols.add(toAdd);
            }
        }
        return protocols;
    }

    public static InterfaceTypeBase createInterfaceType(final String type) {
        return OVSDB_INTERFACE_TYPE_MAP.get(requireNonNull(type));
    }

    public static String createOvsdbInterfaceType(final InterfaceTypeBase mdsaltype) {
        return OVSDB_INTERFACE_TYPE_MAP.inverse().get(requireNonNull(mdsaltype));
    }

    public static List<ProtocolEntry> createMdsalProtocols(final Bridge bridge) {
        Set<String> protocols = null;
        try {
            protocols = bridge.getProtocolsColumn().getData();
        } catch (SchemaVersionMismatchException e) {
            schemaMismatchLog("protocols", "Bridge", e);
        }
        List<ProtocolEntry> protocolList = new ArrayList<>();
        if (protocols != null && !protocols.isEmpty()) {
            ImmutableBiMap<String, OvsdbBridgeProtocolBase> mapper = SouthboundConstants.OVSDB_PROTOCOL_MAP.inverse();
            for (String protocol : protocols) {
                if (protocol != null) {
                    final OvsdbBridgeProtocolBase mapped = mapper.get(protocol);
                    if (mapped != null) {
                        protocolList.add(new ProtocolEntryBuilder().setProtocol(mapped).build());
                    }
                }
            }
        }
        return protocolList;
    }

    /**
     * Create the {@link ControllerEntry} list given an OVSDB {@link Bridge}
     * and {@link Controller} rows.
     *
     * @param bridge the {@link Bridge} to update
     * @param updatedControllerRows the list of {@link Controller} controllers with updates
     * @return list of {@link ControllerEntry} entries
     */
    public static List<ControllerEntry> createControllerEntries(final Bridge bridge,
                                                                final Map<UUID, Controller> updatedControllerRows) {

        LOG.debug("createControllerEntries Bridge: {}\n, updatedControllerRows: {}",
                bridge, updatedControllerRows);
        final Set<UUID> controllerUuids = bridge.getControllerColumn().getData();
        final List<ControllerEntry> controllerEntries = new ArrayList<>();
        for (UUID controllerUuid : controllerUuids) {
            final Controller controller = updatedControllerRows.get(controllerUuid);
            addControllerEntries(controllerEntries, controller);
        }
        LOG.debug("controllerEntries: {}", controllerEntries);
        return controllerEntries;
    }

    /**
     * Create the {@link ControllerEntry} list given an MDSAL {@link Node} bridge
     * and {@link Controller} rows.
     *
     * @param bridgeNode the {@link Node} to update
     * @param updatedControllerRows the list of {@link Controller} controllers with updates
     * @return list of {@link ControllerEntry} entries
     */
    public static List<ControllerEntry> createControllerEntries(final Node bridgeNode,
                                                                final Map<UUID, Controller> updatedControllerRows) {

        LOG.debug("createControllerEntries Bridge 2: {}\n, updatedControllerRows: {}",
                bridgeNode, updatedControllerRows);
        final List<ControllerEntry> controllerEntriesCreated = new ArrayList<>();
        final OvsdbBridgeAugmentation ovsdbBridgeAugmentation =
                bridgeNode.augmentation(OvsdbBridgeAugmentation.class);
        if (ovsdbBridgeAugmentation == null) {
            return controllerEntriesCreated;
        }

        final Map<ControllerEntryKey, ControllerEntry> controllerEntries = ovsdbBridgeAugmentation.getControllerEntry();
        if (controllerEntries != null) {
            for (ControllerEntry controllerEntry : controllerEntries.values()) {
                final Controller controller = updatedControllerRows.get(
                        new UUID(controllerEntry.getControllerUuid().getValue()));
                addControllerEntries(controllerEntriesCreated, controller);
            }
        }
        LOG.debug("controllerEntries: {}", controllerEntriesCreated);
        return controllerEntriesCreated;
    }

    /**
     * Add the OVSDB {@link Controller} updates to the MDSAL {@link ControllerEntry} list.
     *
     * @param controllerEntries the list of {@link ControllerEntry} to update
     * @param controller the updated OVSDB {@link Controller}
     */
    public static void addControllerEntries(final List<ControllerEntry> controllerEntries,
                                            final Controller controller) {

        if (controller != null && controller.getTargetColumn() != null) {
            final String targetString = controller.getTargetColumn().getData();
            final org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid uuid =
                    new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                            .ietf.yang.types.rev130715.Uuid(controller.getUuid().toString());
            ControllerEntryBuilder builder = new ControllerEntryBuilder();

            if (controller.getMaxBackoffColumn() != null && controller.getMaxBackoffColumn().getData() != null
                    && !controller.getMaxBackoffColumn().getData().isEmpty()) {
                builder.setMaxBackoff(Uint32.valueOf(controller.getMaxBackoffColumn().getData().iterator().next()));
            }
            if (controller.getInactivityProbeColumn() != null && controller.getInactivityProbeColumn().getData() != null
                    && !controller.getInactivityProbeColumn().getData().isEmpty()) {
                builder.setInactivityProbe(
                    Uint32.valueOf(controller.getInactivityProbeColumn().getData().iterator().next()));
            }
            controllerEntries.add(builder
                    .setTarget(new Uri(targetString))
                    .setIsConnected(controller.getIsConnectedColumn().getData())
                    .setControllerUuid(uuid).build());
        }
    }

    // This is not called from anywhere but test. Do we need this?
    public static Map<UUID, Controller> createOvsdbController(final OvsdbBridgeAugmentation omn,
            final DatabaseSchema dbSchema) {
        Map<ControllerEntryKey, ControllerEntry> controllerEntries = omn.getControllerEntry();
        Map<UUID,Controller> controllerMap = new HashMap<>();
        if (controllerEntries != null && !controllerEntries.isEmpty()) {
            for (ControllerEntry controllerEntry : controllerEntries.values()) {
                String controllerNamedUuid = "Controller_" + getRandomUuid();
                Controller controller = TypedDatabaseSchema.of(dbSchema).getTypedRowWrapper(Controller.class);
                controller.setTarget(controllerEntry.getTarget().getValue());
                controllerMap.put(new UUID(controllerNamedUuid), controller);
            }
        }
        return controllerMap;
    }

    public static String getRandomUuid() {
        return "Random_" + java.util.UUID.randomUUID().toString().replace("-", "");
    }

    public static ConnectionInfo createConnectionInfo(final OvsdbClient client) {
        ConnectionInfoBuilder connectionInfoBuilder = new ConnectionInfoBuilder();
        connectionInfoBuilder.setRemoteIp(createIpAddress(client.getConnectionInfo().getRemoteAddress()));
        connectionInfoBuilder.setRemotePort(new PortNumber(Uint16.valueOf(client.getConnectionInfo().getRemotePort())));
        connectionInfoBuilder.setLocalIp(createIpAddress(client.getConnectionInfo().getLocalAddress()));
        connectionInfoBuilder.setLocalPort(new PortNumber(Uint16.valueOf(client.getConnectionInfo().getLocalPort())));
        return connectionInfoBuilder.build();
    }

    public static ConnectionInfo suppressLocalIpPort(final ConnectionInfo connectionInfo) {
        ConnectionInfoBuilder connectionInfoBuilder = new ConnectionInfoBuilder();
        connectionInfoBuilder.setRemoteIp(connectionInfo.getRemoteIp());
        connectionInfoBuilder.setRemotePort(connectionInfo.getRemotePort());
        return connectionInfoBuilder.build();
    }

    /**
     * Create the {@link ManagerEntry} list given an OVSDB {@link OpenVSwitch}
     * and {@link Manager} rows.
     *
     * @param ovsdbNode the {@link OpenVSwitch} to update
     * @param updatedManagerRows the list of {@link Manager} managers with updates
     * @return list of {@link ManagerEntry} entries
     */
    public static List<ManagerEntry> createManagerEntries(final OpenVSwitch ovsdbNode,
                                                                final Map<UUID, Manager> updatedManagerRows) {

        LOG.debug("createManagerEntries OpenVSwitch: {}\n, updatedManagerRows: {}",
                ovsdbNode, updatedManagerRows);
        final Set<UUID> managerUuids = ovsdbNode.getManagerOptionsColumn().getData();
        final List<ManagerEntry> managerEntries = new ArrayList<>();
        for (UUID managerUuid : managerUuids) {
            final Manager manager = updatedManagerRows.get(managerUuid);
            addManagerEntries(managerEntries, manager);
        }
        LOG.debug("managerEntries: {}", managerEntries);
        return managerEntries;
    }

    /**
     * Create the {@link ManagerEntry} list given an MDSAL {@link Node} ovsdbNode
     * and {@link Manager} rows.
     *
     * @param ovsdbNode the {@link Node} to update
     * @param updatedManagerRows the list of {@link Manager} managers with updates
     * @return list of {@link ManagerEntry} entries
     */
    public static List<ManagerEntry> createManagerEntries(final Node ovsdbNode,
                                                                final Map<Uri, Manager> updatedManagerRows) {

        LOG.debug("createManagerEntries based on OVSDB Node: {}\n, updatedManagerRows: {}",
                ovsdbNode, updatedManagerRows);
        final List<ManagerEntry> managerEntriesCreated = new ArrayList<>();
        final OvsdbNodeAugmentation ovsdbNodeAugmentation =
                ovsdbNode.augmentation(OvsdbNodeAugmentation.class);
        if (ovsdbNodeAugmentation == null) {
            return managerEntriesCreated;
        }

        final Map<ManagerEntryKey, ManagerEntry> managerEntries = ovsdbNodeAugmentation.getManagerEntry();
        if (managerEntries != null) {
            for (ManagerEntry managerEntry : managerEntries.values()) {
                final Manager manager = updatedManagerRows.get(managerEntry.getTarget());
                addManagerEntries(managerEntriesCreated, manager);
            }
        }
        LOG.debug("managerEntries: {}", managerEntriesCreated);
        return managerEntriesCreated;
    }

    /**
     * Add the OVSDB {@link Manager} updates to the MDSAL {@link ManagerEntry} list.
     *
     * @param managerEntries the list of {@link ManagerEntry} to update
     * @param manager the updated OVSDB {@link Manager}
     */
    public static void addManagerEntries(final List<ManagerEntry> managerEntries,
                                            final Manager manager) {

        if (manager != null && manager.getTargetColumn() != null) {
            Uint32 numberOfConnections = Uint32.ZERO;
            final String targetString = manager.getTargetColumn().getData();

            final Map<String, String> statusAttributeMap =
                            manager.getStatusColumn() == null ? null : manager.getStatusColumn().getData();
            if (statusAttributeMap != null && statusAttributeMap.containsKey(N_CONNECTIONS_STR)) {
                String numberOfConnectionValueStr = statusAttributeMap.get(N_CONNECTIONS_STR);
                numberOfConnections = Uint32.valueOf(numberOfConnectionValueStr);
            } else {
                final boolean isConnected = manager.getIsConnectedColumn().getData();
                if (isConnected) {
                    numberOfConnections = Uint32.ONE;
                }
            }
            managerEntries.add(new ManagerEntryBuilder()
                    .setTarget(new Uri(targetString))
                    .setNumberOfConnections(numberOfConnections)
                    .setConnected(manager.getIsConnectedColumn().getData()).build());
        }
    }

    /**
     * Return the MD-SAL QoS type class corresponding to the QoS type {@link Qos}.
     *
     * @param type the QoS type to match {@link String}
     * @return class matching the input QoS type {@link QosTypeBase}
     */
    public static QosTypeBase createQosType(final String type) {
        requireNonNull(type);
        if (type.isEmpty()) {
            LOG.info("QoS type not supplied");
            return QosTypeBase.VALUE;
        }

        ImmutableBiMap<String, QosTypeBase> mapper = QOS_TYPE_MAP.inverse();
        if (mapper.get(type) == null) {
            LOG.info("QoS type not found in model: {}", type);
            return QosTypeBase.VALUE;
        } else {
            return mapper.get(type);
        }
    }

    public static String createQosType(final QosTypeBase qosTypeClass) {
        String qosType = QOS_TYPE_MAP.get(QosTypeBase.VALUE);

        if (qosTypeClass != null && !qosTypeClass.equals(QosTypeBase.VALUE)) {
            qosType = QOS_TYPE_MAP.get(qosTypeClass);
            if (qosType == null) {
                throw new IllegalArgumentException("Unknown QoS type" + qosTypeClass);
            }
        }
        return qosType;
    }


    public static InstanceIdentifier<Node> getInstanceIdentifier(final InstanceIdentifierCodec instanceIdentifierCodec,
            final OpenVSwitch ovs) {
        if (ovs.getExternalIdsColumn() != null
                && ovs.getExternalIdsColumn().getData() != null
                && ovs.getExternalIdsColumn().getData().containsKey(SouthboundConstants.IID_EXTERNAL_ID_KEY)) {
            String iidString = ovs.getExternalIdsColumn().getData().get(SouthboundConstants.IID_EXTERNAL_ID_KEY);
            return (InstanceIdentifier<Node>) instanceIdentifierCodec.bindingDeserializerOrNull(iidString);
        } else {
            String nodeString = SouthboundConstants.OVSDB_URI_PREFIX + "://" + SouthboundConstants.UUID + "/"
                    + ovs.getUuid().toString();
            NodeId nodeId = new NodeId(new Uri(nodeString));
            NodeKey nodeKey = new NodeKey(nodeId);
            return InstanceIdentifier.builder(NetworkTopology.class)
                    .child(Topology.class,new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                    .child(Node.class,nodeKey)
                    .build();
        }
    }

    public static Map<InstanceIdentifier<?>, DataObject> extractTerminationPointConfigurationChanges(
            final Node bridgeNode) {
        Map<InstanceIdentifier<?>, DataObject> changes = new HashMap<>();
        final InstanceIdentifier<Node> bridgeNodeIid =
                SouthboundMapper.createInstanceIdentifier(bridgeNode.getNodeId());
        changes.put(bridgeNodeIid, bridgeNode);

        Map<TerminationPointKey, TerminationPoint> terminationPoints = bridgeNode.getTerminationPoint();
        if (terminationPoints != null) {
            for (TerminationPoint tp : terminationPoints.values()) {
                OvsdbTerminationPointAugmentation ovsdbTerminationPointAugmentation =
                        tp.augmentation(OvsdbTerminationPointAugmentation.class);
                if (ovsdbTerminationPointAugmentation != null) {
                    final InstanceIdentifier<OvsdbTerminationPointAugmentation> tpIid =
                            bridgeNodeIid
                                    .child(TerminationPoint.class, new TerminationPointKey(tp.getTpId()))
                                    .builder()
                                    .augmentation(OvsdbTerminationPointAugmentation.class)
                                    .build();
                    changes.put(tpIid, ovsdbTerminationPointAugmentation);
                }
            }
        }
        return changes;
    }
}
