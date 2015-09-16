/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound;

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
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.Controller;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathTypeSystem;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeProtocolBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ProtocolEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ProtocolEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableBiMap;

public class SouthboundMapper {
    private static final Logger LOG = LoggerFactory.getLogger(SouthboundMapper.class);

    private static NodeId createNodeId(OvsdbConnectionInstance client) {
        NodeKey key = client.getInstanceIdentifier().firstKeyOf(Node.class, NodeKey.class);
        return key.getNodeId();

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
        Ipv4Address ipv4 = new Ipv4Address(address.getHostAddress());
        return new IpAddress(ipv4);
    }

    public static IpAddress createIpAddress(Inet6Address address) {
        Ipv6Address ipv6 = new Ipv6Address(address.getHostAddress());
        return new IpAddress(ipv6);
    }

    public static InstanceIdentifier<Node> createInstanceIdentifier(NodeId nodeId) {
        InstanceIdentifier<Node> nodePath = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class,new NodeKey(nodeId));
        return nodePath;
    }

    public static InstanceIdentifier<Node> createInstanceIdentifier(OvsdbConnectionInstance client,Bridge bridge) {
        InstanceIdentifier<Node> iid;
        if (bridge.getExternalIdsColumn() != null
                && bridge.getExternalIdsColumn().getData() != null
                && bridge.getExternalIdsColumn().getData().containsKey(SouthboundConstants.IID_EXTERNAL_ID_KEY)) {
            String iidString = bridge.getExternalIdsColumn().getData().get(SouthboundConstants.IID_EXTERNAL_ID_KEY);
            iid = (InstanceIdentifier<Node>) SouthboundUtil.deserializeInstanceIdentifier(iidString);
        } else {
            String nodeString = client.getNodeKey().getNodeId().getValue()
                    + "/bridge/" + bridge.getName();
            NodeId nodeId = new NodeId(new Uri(nodeString));
            NodeKey nodeKey = new NodeKey(nodeId);
            iid = InstanceIdentifier.builder(NetworkTopology.class)
                    .child(Topology.class,new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                    .child(Node.class,nodeKey)
                    .build();
        }
        return iid;
    }

    public static InstanceIdentifier<Node> createInstanceIdentifier(
            OvsdbConnectionInstance client, Controller controller, String bridgeName) {
        InstanceIdentifier<Node> iid;
        if (controller.getExternalIdsColumn() != null
                && controller.getExternalIdsColumn().getData() != null
                && controller.getExternalIdsColumn().getData().containsKey(SouthboundConstants.IID_EXTERNAL_ID_KEY)) {
            String iidString = controller.getExternalIdsColumn().getData().get(SouthboundConstants.IID_EXTERNAL_ID_KEY);
            iid = (InstanceIdentifier<Node>) SouthboundUtil.deserializeInstanceIdentifier(iidString);
        } else {
            // TODO retrieve bridge name
            String nodeString = client.getNodeKey().getNodeId().getValue()
                    + "/bridge/" + bridgeName;
            NodeId nodeId = new NodeId(new Uri(nodeString));
            NodeKey nodeKey = new NodeKey(nodeId);
            iid = InstanceIdentifier.builder(NetworkTopology.class)
                    .child(Topology.class,new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                    .child(Node.class,nodeKey)
                    .build();
        }
        return iid;
    }

    public static NodeId createManagedNodeId(InstanceIdentifier<Node> iid) {
        NodeKey nodeKey = iid.firstKeyOf(Node.class, NodeKey.class);
        return nodeKey.getNodeId();
    }

    public static InetAddress createInetAddress(IpAddress ip) throws UnknownHostException {
        if (ip.getIpv4Address() != null) {
            return InetAddress.getByName(ip.getIpv4Address().getValue());
        } else if (ip.getIpv6Address() != null) {
            return InetAddress.getByName(ip.getIpv6Address().getValue());
        } else {
            throw new UnknownHostException("IP Address has no value");
        }
    }

    public static DatapathId createDatapathId(Bridge bridge) {
        Preconditions.checkNotNull(bridge);
        if (bridge.getDatapathIdColumn() == null) {
            return null;
        } else {
            return createDatapathId(bridge.getDatapathIdColumn().getData());
        }
    }

    public static DatapathId createDatapathId(Set<String> dpids) {
        Preconditions.checkNotNull(dpids);
        if (dpids.isEmpty()) {
            return null;
        } else {
            String[] dpidArray = new String[dpids.size()];
            dpids.toArray(dpidArray);
            return createDatapathId(dpidArray[0]);
        }
    }

    public static String createDatapathType(OvsdbBridgeAugmentation mdsalbridge) {
        String datapathtype = new String(SouthboundConstants.DATAPATH_TYPE_MAP.get(DatapathTypeSystem.class));

        if (mdsalbridge.getDatapathType() != null) {
            if (SouthboundConstants.DATAPATH_TYPE_MAP.get(mdsalbridge.getDatapathType()) != null) {
                datapathtype = SouthboundConstants.DATAPATH_TYPE_MAP.get(mdsalbridge.getDatapathType());
            } else {
                throw new IllegalArgumentException("Unknown datapath type "
                        + SouthboundConstants.DATAPATH_TYPE_MAP.get(mdsalbridge.getDatapathType()));
            }
        }
        return datapathtype;
    }

    public static  Class<? extends DatapathTypeBase> createDatapathType(String type) {
        Preconditions.checkNotNull(type);
        if (type.isEmpty()) {
            return DatapathTypeSystem.class;
        } else {
            ImmutableBiMap<String, Class<? extends DatapathTypeBase>> mapper =
                    SouthboundConstants.DATAPATH_TYPE_MAP.inverse();
            return mapper.get(type);
        }
    }

    public static DatapathId createDatapathId(String dpid) {
        Preconditions.checkNotNull(dpid);
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

    public static Set<String> createOvsdbBridgeProtocols(OvsdbBridgeAugmentation ovsdbBridgeNode) {
        Set<String> protocols = new HashSet<>();
        if (ovsdbBridgeNode.getProtocolEntry() != null && ovsdbBridgeNode.getProtocolEntry().size() > 0) {
            for (ProtocolEntry protocol : ovsdbBridgeNode.getProtocolEntry()) {
                if (SouthboundConstants.OVSDB_PROTOCOL_MAP.get(protocol.getProtocol()) != null) {
                    protocols.add(SouthboundConstants.OVSDB_PROTOCOL_MAP.get(protocol.getProtocol()));
                } else {
                    throw new IllegalArgumentException("Unknown protocol " + protocol.getProtocol());
                }
            }
        }
        return protocols;
    }

    public static  Class<? extends InterfaceTypeBase> createInterfaceType(String type) {
        Preconditions.checkNotNull(type);
        return SouthboundConstants.OVSDB_INTERFACE_TYPE_MAP.get(type);
    }

    public static String createOvsdbInterfaceType(Class<? extends InterfaceTypeBase> mdsaltype) {
        Preconditions.checkNotNull(mdsaltype);
        ImmutableBiMap<Class<? extends InterfaceTypeBase>, String> mapper =
                SouthboundConstants.OVSDB_INTERFACE_TYPE_MAP.inverse();
        return mapper.get(mdsaltype);
    }

    public static List<ProtocolEntry> createMdsalProtocols(Bridge bridge) {
        Set<String> protocols = null;
        try {
            protocols = bridge.getProtocolsColumn().getData();
        } catch (SchemaVersionMismatchException e) {
            LOG.warn("protocols not supported by this version of ovsdb", e);
        }
        List<ProtocolEntry> protocolList = new ArrayList<ProtocolEntry>();
        if (protocols != null && protocols.size() > 0) {
            ImmutableBiMap<String, Class<? extends OvsdbBridgeProtocolBase>> mapper =
                    SouthboundConstants.OVSDB_PROTOCOL_MAP.inverse();
            for (String protocol : protocols) {
                if (protocol != null && mapper.get(protocol) != null) {
                    protocolList.add(new ProtocolEntryBuilder().
                            setProtocol((Class<? extends OvsdbBridgeProtocolBase>) mapper.get(protocol)).build());
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
    public static List<ControllerEntry> createControllerEntries(Bridge bridge,
                                                                Map<UUID, Controller> updatedControllerRows) {

        LOG.debug("createControllerEntries Bridge: {}\n, updatedControllerRows: {}",
                bridge, updatedControllerRows);
        final Set<UUID> controllerUUIDs = bridge.getControllerColumn().getData();
        final List<ControllerEntry> controllerEntries = new ArrayList<ControllerEntry>();
        for (UUID controllerUUID : controllerUUIDs ) {
            final Controller controller = updatedControllerRows.get(controllerUUID);
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
    public static List<ControllerEntry> createControllerEntries(Node bridgeNode,
                                                                Map<UUID, Controller> updatedControllerRows) {

        LOG.debug("createControllerEntries Bridge 2: {}\n, updatedControllerRows: {}",
                bridgeNode, updatedControllerRows);
        final List<ControllerEntry> controllerEntriesCreated = new ArrayList<ControllerEntry>();
        final OvsdbBridgeAugmentation ovsdbBridgeAugmentation =
                bridgeNode.getAugmentation(OvsdbBridgeAugmentation.class);
        if (ovsdbBridgeAugmentation == null) {
            return controllerEntriesCreated;
        }

        final List<ControllerEntry> controllerEntries = ovsdbBridgeAugmentation.getControllerEntry();
        if (controllerEntries != null) {
            for (ControllerEntry controllerEntry : controllerEntries) {
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
    public static void addControllerEntries(List<ControllerEntry> controllerEntries,
                                            final Controller controller) {

        if (controller != null && controller.getTargetColumn() != null) {
            final String targetString = controller.getTargetColumn().getData();
            final org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid uuid =
                    new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                            .ietf.yang.types.rev130715.Uuid(controller.getUuid().toString());

            controllerEntries.add(new ControllerEntryBuilder()
                    .setTarget(new Uri(targetString))
                    .setIsConnected(controller.getIsConnectedColumn().getData())
                    .setControllerUuid(uuid).build());
        }
    }

    public static Map<UUID, Controller> createOvsdbController(OvsdbBridgeAugmentation omn,DatabaseSchema dbSchema) {
        List<ControllerEntry> controllerEntries = omn.getControllerEntry();
        Map<UUID,Controller> controllerMap = new HashMap<>();
        if (controllerEntries != null && !controllerEntries.isEmpty()) {
            for (ControllerEntry controllerEntry : controllerEntries) {
                String controllerNamedUUID = "Controller_" + getRandomUUID();
                Controller controller = TyperUtils.getTypedRowWrapper(dbSchema, Controller.class);
                controller.setTarget(controllerEntry.getTarget().getValue());
                controllerMap.put(new UUID(controllerNamedUUID), controller);
            }
        }
        return controllerMap;
    }

    public static String getRandomUUID() {
        return "Random_" + java.util.UUID.randomUUID().toString().replace("-", "");
    }
    public static ConnectionInfo createConnectionInfo(OvsdbClient client) {
        ConnectionInfoBuilder connectionInfoBuilder = new ConnectionInfoBuilder();
        connectionInfoBuilder.setRemoteIp(createIpAddress(client.getConnectionInfo().getRemoteAddress()));
        connectionInfoBuilder.setRemotePort(new PortNumber(client.getConnectionInfo().getRemotePort()));
        connectionInfoBuilder.setLocalIp(createIpAddress(client.getConnectionInfo().getLocalAddress()));
        connectionInfoBuilder.setLocalPort(new PortNumber(client.getConnectionInfo().getLocalPort()));
        return connectionInfoBuilder.build();
    }

    public static ConnectionInfo suppressLocalIpPort(ConnectionInfo connectionInfo) {
        ConnectionInfoBuilder connectionInfoBuilder = new ConnectionInfoBuilder();
        connectionInfoBuilder.setRemoteIp(connectionInfo.getRemoteIp());
        connectionInfoBuilder.setRemotePort(connectionInfo.getRemotePort());
        return connectionInfoBuilder.build();
    }
}
