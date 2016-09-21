/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound;

import static org.opendaylight.ovsdb.southbound.SouthboundUtil.schemaMismatchLog;

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
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.Controller;
import org.opendaylight.ovsdb.schema.openvswitch.Manager;
import org.opendaylight.ovsdb.schema.openvswitch.OpenVSwitch;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IetfInetUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathTypeSystem;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeProtocolBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.QosTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ProtocolEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ProtocolEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagerEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagerEntryBuilder;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SouthboundMapper {

    private SouthboundMapper() { }

    private static final Logger LOG = LoggerFactory.getLogger(SouthboundMapper.class);
    private static final String N_CONNECTIONS_STR = "n_connections";

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

    public static InstanceIdentifier<Topology> createTopologyInstanceIdentifier() {
        return InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID));
    }

    public static InstanceIdentifier<Node> createInstanceIdentifier(NodeId nodeId) {
        return createTopologyInstanceIdentifier()
                .child(Node.class,new NodeKey(nodeId));
    }

    @SuppressWarnings("unchecked")
    public static InstanceIdentifier<Node> createInstanceIdentifier(OvsdbConnectionInstance client,Bridge bridge) {
        InstanceIdentifier<Node> iid;
        if (bridge.getExternalIdsColumn() != null
                && bridge.getExternalIdsColumn().getData() != null
                && bridge.getExternalIdsColumn().getData().containsKey(SouthboundConstants.IID_EXTERNAL_ID_KEY)) {
            String iidString = bridge.getExternalIdsColumn().getData().get(SouthboundConstants.IID_EXTERNAL_ID_KEY);
            iid = (InstanceIdentifier<Node>) SouthboundUtil.deserializeInstanceIdentifier(iidString);
        } else {
            iid = createInstanceIdentifier(client, bridge.getName());
        }
        return iid;
    }

    @SuppressWarnings("unchecked")
    public static InstanceIdentifier<Node> createInstanceIdentifier(
            OvsdbConnectionInstance client, Controller controller, String bridgeName) {
        InstanceIdentifier<Node> iid;
        if (controller.getExternalIdsColumn() != null
                && controller.getExternalIdsColumn().getData() != null
                && controller.getExternalIdsColumn().getData().containsKey(SouthboundConstants.IID_EXTERNAL_ID_KEY)) {
            String iidString = controller.getExternalIdsColumn().getData().get(SouthboundConstants.IID_EXTERNAL_ID_KEY);
            iid = (InstanceIdentifier<Node>) SouthboundUtil.deserializeInstanceIdentifier(iidString);
        } else {
            iid = createInstanceIdentifier(client, bridgeName);
        }
        return iid;
    }

    public static InstanceIdentifier<Node> createInstanceIdentifier(
            OvsdbConnectionInstance client, String bridgeName) {
        String nodeString = client.getNodeKey().getNodeId().getValue()
                + "/bridge/" + bridgeName;
        NodeId nodeId = new NodeId(new Uri(nodeString));
        return createInstanceIdentifier(nodeId);

    }

    public static NodeId createManagedNodeId(InstanceIdentifier<Node> iid) {
        NodeKey nodeKey = iid.firstKeyOf(Node.class);
        return nodeKey.getNodeId();
    }

    public static InetAddress createInetAddress(IpAddress ip) throws UnknownHostException {
        if (ip.getIpv4Address() != null) {
            return InetAddresses.forString(ip.getIpv4Address().getValue());
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

    public static String createDatapathType(OvsdbBridgeAugmentation mdsalbridge) {
        String datapathtype = SouthboundConstants.DATAPATH_TYPE_MAP.get(DatapathTypeSystem.class);

        if (mdsalbridge.getDatapathType() != null && !mdsalbridge.getDatapathType().equals(DatapathTypeBase.class)) {
            datapathtype = SouthboundConstants.DATAPATH_TYPE_MAP.get(mdsalbridge.getDatapathType());
            if (datapathtype == null) {
                throw new IllegalArgumentException("Unknown datapath type " + mdsalbridge.getDatapathType().getName());
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
            schemaMismatchLog("protocols", "Bridge", e);
        }
        List<ProtocolEntry> protocolList = new ArrayList<>();
        if (protocols != null && protocols.size() > 0) {
            ImmutableBiMap<String, Class<? extends OvsdbBridgeProtocolBase>> mapper =
                    SouthboundConstants.OVSDB_PROTOCOL_MAP.inverse();
            for (String protocol : protocols) {
                if (protocol != null && mapper.get(protocol) != null) {
                    protocolList.add(new ProtocolEntryBuilder().setProtocol(mapper.get(protocol)).build());
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
        final Set<UUID> controllerUuids = bridge.getControllerColumn().getData();
        final List<ControllerEntry> controllerEntries = new ArrayList<>();
        for (UUID controllerUuid : controllerUuids ) {
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
    public static List<ControllerEntry> createControllerEntries(Node bridgeNode,
                                                                Map<UUID, Controller> updatedControllerRows) {

        LOG.debug("createControllerEntries Bridge 2: {}\n, updatedControllerRows: {}",
                bridgeNode, updatedControllerRows);
        final List<ControllerEntry> controllerEntriesCreated = new ArrayList<>();
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
                String controllerNamedUuid = "Controller_" + getRandomUuid();
                Controller controller = TyperUtils.getTypedRowWrapper(dbSchema, Controller.class);
                controller.setTarget(controllerEntry.getTarget().getValue());
                controllerMap.put(new UUID(controllerNamedUuid), controller);
            }
        }
        return controllerMap;
    }

    public static String getRandomUuid() {
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

    /**
     * Create the {@link ManagerEntry} list given an OVSDB {@link OpenVSwitch}
     * and {@link Manager} rows.
     *
     * @param ovsdbNode the {@link OpenVSwitch} to update
     * @param updatedManagerRows the list of {@link Manager} managers with updates
     * @return list of {@link ManagerEntry} entries
     */
    public static List<ManagerEntry> createManagerEntries(OpenVSwitch ovsdbNode,
                                                                Map<UUID, Manager> updatedManagerRows) {

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
    public static List<ManagerEntry> createManagerEntries(Node ovsdbNode,
                                                                Map<Uri, Manager> updatedManagerRows) {

        LOG.debug("createManagerEntries based on OVSDB Node: {}\n, updatedManagerRows: {}",
                ovsdbNode, updatedManagerRows);
        final List<ManagerEntry> managerEntriesCreated = new ArrayList<>();
        final OvsdbNodeAugmentation ovsdbNodeAugmentation =
                ovsdbNode.getAugmentation(OvsdbNodeAugmentation.class);
        if (ovsdbNodeAugmentation == null) {
            return managerEntriesCreated;
        }

        final List<ManagerEntry> managerEntries = ovsdbNodeAugmentation.getManagerEntry();
        if (managerEntries != null) {
            for (ManagerEntry managerEntry : managerEntries) {
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
    public static void addManagerEntries(List<ManagerEntry> managerEntries,
                                            final Manager manager) {

        if (manager != null && manager.getTargetColumn() != null) {
            long numberOfConnections = 0;
            final String targetString = manager.getTargetColumn().getData();

            final Map<String, String> statusAttributeMap =
                            (manager.getStatusColumn() == null) ? null : manager.getStatusColumn().getData();
            if ((statusAttributeMap != null) && statusAttributeMap.containsKey(N_CONNECTIONS_STR)) {
                String numberOfConnectionValueStr = statusAttributeMap.get(N_CONNECTIONS_STR);
                numberOfConnections = Integer.parseInt(numberOfConnectionValueStr);
            } else {
                final boolean isConnected = manager.getIsConnectedColumn().getData();
                if (isConnected) {
                    numberOfConnections = 1;
                }
            }
            managerEntries.add(new ManagerEntryBuilder()
                    .setTarget(new Uri(targetString))
                    .setNumberOfConnections(numberOfConnections)
                    .setConnected(manager.getIsConnectedColumn().getData()).build());
        }
    }

    /**
     * Return the MD-SAL QoS type class corresponding to the QoS type.
     *
     * @param type the QoS type to match {@link String}
     * @return class matching the input QoS type {@link QosTypeBase}
     */
    public static  Class<? extends QosTypeBase> createQosType(String type) {
        Preconditions.checkNotNull(type);
        if (type.isEmpty()) {
            LOG.info("QoS type not supplied");
            return QosTypeBase.class;
        } else {
            ImmutableBiMap<String, Class<? extends QosTypeBase>> mapper =
                    SouthboundConstants.QOS_TYPE_MAP.inverse();
            if (mapper.get(type) == null) {
                LOG.info("QoS type not found in model: {}", type);
                return QosTypeBase.class;
            } else {
                return mapper.get(type);
            }
        }
    }

    public static String createQosType(Class<? extends QosTypeBase> qosTypeClass) {
        String qosType = SouthboundConstants.QOS_TYPE_MAP.get(QosTypeBase.class);

        if (qosTypeClass != null && !qosTypeClass.equals(QosTypeBase.class)) {
            qosType = SouthboundConstants.QOS_TYPE_MAP.get(qosTypeClass);
            if (qosType == null) {
                throw new IllegalArgumentException("Unknown QoS type" + qosTypeClass.getName());
            }
        }
        return qosType;
    }


    public static InstanceIdentifier<Node> getInstanceIdentifier(OpenVSwitch ovs) {
        if (ovs.getExternalIdsColumn() != null
                && ovs.getExternalIdsColumn().getData() != null
                && ovs.getExternalIdsColumn().getData().containsKey(SouthboundConstants.IID_EXTERNAL_ID_KEY)) {
            String iidString = ovs.getExternalIdsColumn().getData().get(SouthboundConstants.IID_EXTERNAL_ID_KEY);
            return (InstanceIdentifier<Node>) SouthboundUtil.deserializeInstanceIdentifier(iidString);
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

        List<TerminationPoint> terminationPoints = bridgeNode.getTerminationPoint();
        if (terminationPoints != null && !terminationPoints.isEmpty()) {
            for (TerminationPoint tp : terminationPoints) {
                OvsdbTerminationPointAugmentation ovsdbTerminationPointAugmentation =
                        tp.getAugmentation(OvsdbTerminationPointAugmentation.class);
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
