/*
 * Copyright (c) 2015 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.utils.southbound.utils;

import com.google.common.collect.ImmutableBiMap;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

import java.util.List;
import java.util.Map;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.ovsdb.utils.mdsal.utils.MdsalUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.BridgeExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.BridgeOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ProtocolEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ProtocolEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfoBuilder;
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

public class SouthboundUtils {
    private static final Logger LOG = LoggerFactory.getLogger(SouthboundUtils.class);
    private static final int OVSDB_UPDATE_TIMEOUT = 1000;
    private static final String DEFAULT_OPENFLOW_PORT = "6653";
    private static final String OPENFLOW_CONNECTION_PROTOCOL = "tcp";
    private MdsalUtils mdsalUtils;
    public static final TopologyId OVSDB_TOPOLOGY_ID = new TopologyId(new Uri("ovsdb:1"));

    public SouthboundUtils(MdsalUtils mdsalUtils) {
        this.mdsalUtils = mdsalUtils;
    }

    public static final ImmutableBiMap<String, Class<? extends InterfaceTypeBase>> OVSDB_INTERFACE_TYPE_MAP
            = new ImmutableBiMap.Builder<String, Class<? extends InterfaceTypeBase>>()
            .put("internal", InterfaceTypeInternal.class)
            .put("vxlan", InterfaceTypeVxlan.class)
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

    public static NodeId createNodeId(IpAddress ip, PortNumber port) {
        String uriString = SouthboundConstants.OVSDB_URI_PREFIX + "://"
                + String.valueOf(ip.getValue()) + ":" + port.getValue();
        Uri uri = new Uri(uriString);
        return new NodeId(uri);
    }

    public Node createNode(ConnectionInfo key) {
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setNodeId(createNodeId(key.getRemoteIp(), key.getRemotePort()));
        nodeBuilder.addAugmentation(OvsdbNodeAugmentation.class, createOvsdbAugmentation(key));
        return nodeBuilder.build();
    }

    public OvsdbNodeAugmentation createOvsdbAugmentation(ConnectionInfo key) {
        OvsdbNodeAugmentationBuilder ovsdbNodeBuilder = new OvsdbNodeAugmentationBuilder();
        ovsdbNodeBuilder.setConnectionInfo(key);
        return ovsdbNodeBuilder.build();
    }

    public InstanceIdentifier<Node> createInstanceIdentifier(ConnectionInfo key) {
        return createInstanceIdentifier(key.getRemoteIp(), key.getRemotePort());
    }

    public InstanceIdentifier<Node> createInstanceIdentifier(IpAddress ip, PortNumber port) {
        InstanceIdentifier<Node> path = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class,createNodeKey(ip,port));
        LOG.debug("Created ovsdb path: {}",path);
        return path;
    }

    public InstanceIdentifier<Node> createInstanceIdentifier(ConnectionInfo key,OvsdbBridgeName bridgeName) {
        return SouthboundMapper.createInstanceIdentifier(createManagedNodeId(key, bridgeName));
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

    public NodeKey createNodeKey(IpAddress ip, PortNumber port) {
        return new NodeKey(createNodeId(ip, port));
    }

    public NodeId createManagedNodeId(ConnectionInfo key, OvsdbBridgeName bridgeName) {
        return createManagedNodeId(key.getRemoteIp(), key.getRemotePort(), bridgeName);
    }

    public NodeId createManagedNodeId(IpAddress ip, PortNumber port, OvsdbBridgeName bridgeName) {
        return new NodeId(createNodeId(ip,port).getValue()
                + "/" + SouthboundConstants.BRIDGE_URI_PREFIX + "/" + bridgeName.getValue());
    }

    public ConnectionInfo getConnectionInfo(final String addressStr, final String portStr) {
        InetAddress inetAddress = null;
        try {
            inetAddress = InetAddress.getByName(addressStr);
        } catch (UnknownHostException e) {
            LOG.warn("Could not allocate InetAddress");
        }

        IpAddress address = SouthboundMapper.createIpAddress(inetAddress);
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

    public String connectionInfoToString(final ConnectionInfo connectionInfo) {
        return String.valueOf(
                connectionInfo.getRemoteIp().getValue()) + ":" + connectionInfo.getRemotePort().getValue();
    }

    public boolean addOvsdbNode(final ConnectionInfo connectionInfo) {
        boolean result = mdsalUtils.put(LogicalDatastoreType.CONFIGURATION,
                createInstanceIdentifier(connectionInfo),
                createNode(connectionInfo));
        try {
            Thread.sleep(OVSDB_UPDATE_TIMEOUT);
        } catch (InterruptedException e) {
            LOG.warn("Interrupted while waiting after adding OVSDB node {}", connectionInfoToString(connectionInfo), e);
        }
        return result;
    }

    public Node getOvsdbNode(final ConnectionInfo connectionInfo) {
        return mdsalUtils.read(LogicalDatastoreType.OPERATIONAL,
                createInstanceIdentifier(connectionInfo));
    }

    public boolean deleteOvsdbNode(final ConnectionInfo connectionInfo) {
        boolean result = mdsalUtils.delete(LogicalDatastoreType.CONFIGURATION,
                createInstanceIdentifier(connectionInfo));
        try {
            Thread.sleep(OVSDB_UPDATE_TIMEOUT);
        } catch (InterruptedException e) {
            LOG.warn("Interrupted while waiting after deleting OVSDB node {}", connectionInfoToString(connectionInfo),
                    e);
        }
        return result;
    }

    public Node connectOvsdbNode(final ConnectionInfo connectionInfo) {
        addOvsdbNode(connectionInfo);
        Node node = getOvsdbNode(connectionInfo);
        LOG.info("Connected to {}", connectionInfoToString(connectionInfo));
        return node;
    }

    public boolean disconnectOvsdbNode(final ConnectionInfo connectionInfo) {
        deleteOvsdbNode(connectionInfo);
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

    public boolean deleteBridge(final ConnectionInfo connectionInfo, final String bridgeName) {

        boolean result = mdsalUtils.delete(LogicalDatastoreType.CONFIGURATION,
                createInstanceIdentifier(connectionInfo, new OvsdbBridgeName(bridgeName)));
        try {
            Thread.sleep(OVSDB_UPDATE_TIMEOUT);
        } catch (InterruptedException e) {
            LOG.warn("Interrupted while waiting after deleting bridge {}", bridgeName, e);
        }
        return result;
    }

    public List<ProtocolEntry> createMdsalProtocols() {
        List<ProtocolEntry> protocolList = new ArrayList<>();
        ImmutableBiMap<String, Class<? extends OvsdbBridgeProtocolBase>> mapper =
                SouthboundConstants.OVSDB_PROTOCOL_MAP.inverse();
        protocolList.add(new ProtocolEntryBuilder().setProtocol(mapper.get("OpenFlow13")).build());
        return protocolList;
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
                             final String dpid) throws InterruptedException {

        NodeBuilder bridgeNodeBuilder = new NodeBuilder();
        if (bridgeIid == null) {
            bridgeIid = createInstanceIdentifier(connectionInfo, new OvsdbBridgeName(bridgeName));
        }
        if (bridgeNodeId == null) {
            bridgeNodeId = SouthboundMapper.createManagedNodeId(bridgeIid);
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
        Thread.sleep(OVSDB_UPDATE_TIMEOUT);
        return result;
    }

    public boolean addBridge(final ConnectionInfo connectionInfo, InstanceIdentifier<Node> bridgeIid,
                             final String bridgeName, NodeId bridgeNodeId, final boolean setProtocolEntries,
                             final Class<? extends OvsdbFailModeBase> failMode, final boolean setManagedBy,
                             final Class<? extends DatapathTypeBase> dpType,
                             final List<BridgeExternalIds> externalIds,
                             final List<ControllerEntry> controllerEntries,
                             final List<BridgeOtherConfigs> otherConfigs) throws InterruptedException {
        return addBridge(connectionInfo, bridgeIid, bridgeName, bridgeNodeId, setProtocolEntries,
                failMode, setManagedBy, dpType, externalIds, controllerEntries,
                otherConfigs, null);
    }

    public boolean addBridge(final ConnectionInfo connectionInfo, final String bridgeName)
            throws InterruptedException {

        return addBridge(connectionInfo, null, bridgeName, null, true,
                SouthboundConstants.OVSDB_FAIL_MODE_MAP.inverse().get("secure"), true, null, null, null, null);
    }

    private void setManagedBy(final OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder,
                              final ConnectionInfo connectionInfo) {
        InstanceIdentifier<Node> connectionNodePath = createInstanceIdentifier(connectionInfo);
        ovsdbBridgeAugmentationBuilder.setManagedBy(new OvsdbNodeRef(connectionNodePath));
    }

    public Boolean addTerminationPoint(Node bridgeNode, String bridgeName, String portName,
                                       String type, Map<String, String> options,
                                       Map<String, String> externalIds) {
        InstanceIdentifier<TerminationPoint> tpIid = createTerminationPointInstanceIdentifier(bridgeNode, portName);
        OvsdbTerminationPointAugmentationBuilder tpAugmentationBuilder = new OvsdbTerminationPointAugmentationBuilder();

        tpAugmentationBuilder.setName(portName);
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

    public TerminationPoint readTerminationPoint(Node bridgeNode, String bridgeName, String portName) {
        InstanceIdentifier<TerminationPoint> tpIid = createTerminationPointInstanceIdentifier(
                bridgeNode, portName);
        return mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, tpIid);
    }

    public Boolean addTerminationPoint(Node bridgeNode, String bridgeName, String portName, String type) {
        return addTerminationPoint(bridgeNode, bridgeName, portName, type, null, null);
    }

    public Boolean addTunnelTerminationPoint(Node bridgeNode, String bridgeName, String portName, String type,
                                             Map<String, String> options) {
        return addTerminationPoint(bridgeNode, bridgeName, portName, type, options, null);
    }
}
