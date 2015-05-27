/*
 * Copyright (c) 2015 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.openstack.netvirt;

import java.math.BigInteger;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.ovsdb.openstack.netvirt.api.OvsdbTables;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeProtocolBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.BridgeExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.BridgeOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ProtocolEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ProtocolEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagedNodeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.Options;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.OptionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.OptionsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.PortExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.PortOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.util.concurrent.CheckedFuture;

/**
 * Utility class to wrap mdsal transactions.
 *
 * @author Sam Hague (shague@redhat.com)
 */
public class MdsalUtils {
    private static final Logger LOG = LoggerFactory.getLogger(MdsalUtils.class);

    public static DataBroker getDatabroker() {
        return databroker;
    }

    private static DataBroker databroker = null;
    private static final int OVSDB_UPDATE_TIMEOUT = 500;
    private static final String PATCH_PORT_TYPE = "patch";

    /**
     * Class constructor setting the data broker.
     *
     * @param dataBroker the {@link org.opendaylight.controller.md.sal.binding.api.DataBroker}
     */
    public MdsalUtils(DataBroker dataBroker) {
        this.databroker = dataBroker;
    }

    /**
     * Executes delete as a blocking transaction.
     *
     * @param store {@link LogicalDatastoreType} which should be modified
     * @param path {@link InstanceIdentifier} to read from
     * @param <D> the data object type
     * @return the result of the request
     */
    public static <D extends org.opendaylight.yangtools.yang.binding.DataObject> boolean delete(
            final LogicalDatastoreType store, final InstanceIdentifier<D> path)  {
        boolean result = false;
        final WriteTransaction transaction = databroker.newWriteOnlyTransaction();
        transaction.delete(store, path);
        CheckedFuture<Void, TransactionCommitFailedException> future = transaction.submit();
        try {
            future.checkedGet();
            result = true;
        } catch (TransactionCommitFailedException e) {
            LOG.warn("Failed to delete {} ", path, e);
        }
        return result;
    }

    /**
     * Executes merge as a blocking transaction.
     *
     * @param logicalDatastoreType {@link LogicalDatastoreType} which should be modified
     * @param path {@link InstanceIdentifier} for path to read
     * @param <D> the data object type
     * @return the result of the request
     */
    public static <D extends org.opendaylight.yangtools.yang.binding.DataObject> boolean merge(
            final LogicalDatastoreType logicalDatastoreType, final InstanceIdentifier<D> path, D data)  {
        boolean result = false;
        final WriteTransaction transaction = databroker.newWriteOnlyTransaction();
        transaction.merge(logicalDatastoreType, path, data, true);
        CheckedFuture<Void, TransactionCommitFailedException> future = transaction.submit();
        try {
            future.checkedGet();
            result = true;
        } catch (TransactionCommitFailedException e) {
            LOG.warn("Failed to merge {} ", path, e);
        }
        return result;
    }

    /**
     * Executes put as a blocking transaction.
     *
     * @param logicalDatastoreType {@link LogicalDatastoreType} which should be modified
     * @param path {@link InstanceIdentifier} for path to read
     * @param <D> the data object type
     * @return the result of the request
     */
    public static <D extends org.opendaylight.yangtools.yang.binding.DataObject> boolean put(
            final LogicalDatastoreType logicalDatastoreType, final InstanceIdentifier<D> path, D data)  {
        boolean result = false;
        final WriteTransaction transaction = databroker.newWriteOnlyTransaction();
        transaction.put(logicalDatastoreType, path, data, true);
        CheckedFuture<Void, TransactionCommitFailedException> future = transaction.submit();
        try {
            future.checkedGet();
            result = true;
        } catch (TransactionCommitFailedException e) {
            LOG.warn("Failed to put {} ", path, e);
        }
        return result;
    }

    /**
     * Executes read as a blocking transaction.
     *
     * @param store {@link LogicalDatastoreType} to read
     * @param path {@link InstanceIdentifier} for path to read
     * @param <D> the data object type
     * @return the result as the data object requested
     */
    public static <D extends org.opendaylight.yangtools.yang.binding.DataObject> D read(
            final LogicalDatastoreType store, final InstanceIdentifier<D> path)  {
        D result = null;
        final ReadOnlyTransaction transaction = databroker.newReadOnlyTransaction();
        Optional<D> optionalDataObject;
        CheckedFuture<Optional<D>, ReadFailedException> future = transaction.read(store, path);
        try {
            optionalDataObject = future.checkedGet();
            if (optionalDataObject.isPresent()) {
                result = optionalDataObject.get();
            } else {
                LOG.debug("{}: Failed to read {}",
                        Thread.currentThread().getStackTrace()[1], path);
            }
        } catch (ReadFailedException e) {
            LOG.warn("Failed to read {} ", path, e);
        }
        transaction.close();
        return result;
    }

    public static ConnectionInfo getConnectionInfo(Node node) {
        ConnectionInfo connectionInfo = null;
        OvsdbNodeAugmentation ovsdbNodeAugmentation = node.getAugmentation(OvsdbNodeAugmentation.class);
        if (ovsdbNodeAugmentation != null) {
            connectionInfo = ovsdbNodeAugmentation.getConnectionInfo();
        }
        return connectionInfo;
    }

    public static OvsdbNodeAugmentation extractOvsdbNode(Node node) {
        return node.getAugmentation(OvsdbNodeAugmentation.class);
    }

    public static NodeId extractBridgeOvsdbNodeId(Node bridgeNode) {
        NodeId ovsdbNodeId = null;
        OvsdbBridgeAugmentation bridgeAugmentation = extractBridgeAugmentation(bridgeNode);
        if (bridgeAugmentation != null) {
            @SuppressWarnings("unchecked")
            InstanceIdentifier<Node> ovsdbNodeIid =
                    (InstanceIdentifier<Node>) (bridgeAugmentation.getManagedBy().getValue());
            ovsdbNodeId = InstanceIdentifier.keyOf(ovsdbNodeIid).getNodeId();
        }
        return ovsdbNodeId;
    }

    public static List<Node> readOvsdbTopologyNodes() {
        List<Node> ovsdbNodes = new ArrayList<>();
        InstanceIdentifier<Topology> topologyInstanceIdentifier = MdsalHelper.createInstanceIdentifier();
        Topology topology = read(LogicalDatastoreType.OPERATIONAL, topologyInstanceIdentifier);
        if (topology != null && topology.getNode() != null) {
            for (Node node : topology.getNode()) {
                OvsdbNodeAugmentation ovsdbNodeAugmentation = node.getAugmentation(OvsdbNodeAugmentation.class);
                if (ovsdbNodeAugmentation != null) {
                    ovsdbNodes.add(node);
                }
            }
        }
        return ovsdbNodes;
    }

    public static OvsdbNodeAugmentation readOvsdbNode(Node bridgeNode) {
        OvsdbNodeAugmentation nodeAugmentation = null;
        OvsdbBridgeAugmentation bridgeAugmentation = extractBridgeAugmentation(bridgeNode);
        if(bridgeAugmentation != null){
            InstanceIdentifier<Node> ovsdbNodeIid =
                    (InstanceIdentifier<Node>) bridgeAugmentation.getManagedBy().getValue();
            Node node = read(LogicalDatastoreType.OPERATIONAL, ovsdbNodeIid);
            if (node != null){
                nodeAugmentation = node.getAugmentation(OvsdbNodeAugmentation.class);
                LOG.debug("readOvsdbNode: Ovsdb node {} found that manages bridge {}",
                        nodeAugmentation != null?nodeAugmentation:"not",bridgeAugmentation);
            }else {
                LOG.debug ("readOvsdbNode: Ovsdb node that manages bridge {} not found. ",bridgeAugmentation);
            }

        }else{
            LOG.debug("readOvsdbNode: Provided node is not a bridge node : {}",bridgeNode);
        }
        return nodeAugmentation;
    }

    public static String getOvsdbNodeUUID(Node node) {
        String nodeUUID = null;
        OvsdbNodeAugmentation ovsdbNodeAugmentation = node.getAugmentation(OvsdbNodeAugmentation.class);
        if (ovsdbNodeAugmentation != null) {
            // TODO replace with proper uuid and not the system-id
            nodeUUID = getOsdbNodeExternalIdsValue(ovsdbNodeAugmentation, "system-id");
        }
        return nodeUUID;
    }

    public static String getOsdbNodeExternalIdsValue(OvsdbNodeAugmentation ovsdbNodeAugmentation, String key) {
        String value = null;
        List<OpenvswitchExternalIds> pairs = ovsdbNodeAugmentation.getOpenvswitchExternalIds();
        if (pairs != null && !pairs.isEmpty()) {
            for (OpenvswitchExternalIds pair : pairs) {
                if (pair.getExternalIdKey().equals(key)) {
                    value = pair.getExternalIdValue();
                    break;
                }
            }
        }
        return value;
    }

    public static boolean addBridge(Node ovsdbNode, String bridgeName, String target)
            throws InterruptedException, InvalidParameterException {
        boolean result = false;

        LOG.info("addBridge: node: {}, bridgeName: {}, target: {}", ovsdbNode, bridgeName, target);
        ConnectionInfo connectionInfo = getConnectionInfo(ovsdbNode);
        if (connectionInfo != null) {
            NodeBuilder bridgeNodeBuilder = new NodeBuilder();
            InstanceIdentifier<Node> bridgeIid =
                    MdsalHelper.createInstanceIdentifier(ovsdbNode.getKey(), bridgeName);
            NodeId bridgeNodeId = MdsalHelper.createManagedNodeId(bridgeIid);
            bridgeNodeBuilder.setNodeId(bridgeNodeId);
            OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();
            ovsdbBridgeAugmentationBuilder.setControllerEntry(createControllerEntries(target));
            ovsdbBridgeAugmentationBuilder.setBridgeName(new OvsdbBridgeName(bridgeName));
            ovsdbBridgeAugmentationBuilder.setProtocolEntry(createMdsalProtocols());
            ovsdbBridgeAugmentationBuilder.setFailMode(
                    MdsalHelper.OVSDB_FAIL_MODE_MAP.inverse().get("secure"));
            setManagedByForBridge(ovsdbBridgeAugmentationBuilder, ovsdbNode.getKey());
            bridgeNodeBuilder.addAugmentation(OvsdbBridgeAugmentation.class, ovsdbBridgeAugmentationBuilder.build());

            result = put(LogicalDatastoreType.CONFIGURATION, bridgeIid, bridgeNodeBuilder.build());
            LOG.info("addBridge: result: {}", result);
        } else {
            throw new InvalidParameterException("Could not find ConnectionInfo");
        }
        return result;
    }

    public static boolean deleteBridge(Node ovsdbNode) {
        boolean result = false;
        InstanceIdentifier<Node> bridgeIid =
                MdsalHelper.createInstanceIdentifier(ovsdbNode.getNodeId());

        result = delete(LogicalDatastoreType.CONFIGURATION, bridgeIid);
        LOG.info("deleteBridge node: {}, bridgeName: {} result : {}", ovsdbNode, ovsdbNode.getNodeId(),result);
        return result;
    }

    public static OvsdbBridgeAugmentation readBridge(Node node, String name) {
        OvsdbBridgeAugmentation ovsdbBridgeAugmentation = null;
        ConnectionInfo connectionInfo = getConnectionInfo(node);
        if (connectionInfo != null) {
            InstanceIdentifier<Node> bridgeIid =
                    MdsalHelper.createInstanceIdentifier(node.getKey(), name);
            Node bridgeNode = read(LogicalDatastoreType.OPERATIONAL, bridgeIid);
            if (bridgeNode != null) {
                ovsdbBridgeAugmentation = bridgeNode.getAugmentation(OvsdbBridgeAugmentation.class);
            }
        }
        return ovsdbBridgeAugmentation;
    }

    public static Node readBridgeNode(Node node, String name) {
        Node bridgeNode = null;
        ConnectionInfo connectionInfo = getConnectionInfo(node);
        if (connectionInfo != null) {
            InstanceIdentifier<Node> bridgeIid =
                    MdsalHelper.createInstanceIdentifier(node.getKey(), name);
            bridgeNode = read(LogicalDatastoreType.OPERATIONAL, bridgeIid);
        }
        return bridgeNode;
    }

    public static Node getBridgeNode(Node node, String bridgeName) {
        Node bridgeNode = null;
        OvsdbBridgeAugmentation bridge = extractBridgeAugmentation(node);
        if (bridge != null) {
            if (bridge.getBridgeName().getValue().equals(bridgeName)) {
                bridgeNode = node;
            }
        } else {
            bridgeNode = readBridgeNode(node, bridgeName);
        }

        return bridgeNode;
    }

    public static String getBridgeUuid(Node node, String name) {
        String uuid = null;
        OvsdbBridgeAugmentation ovsdbBridgeAugmentation = readBridge(node, name);
        if (ovsdbBridgeAugmentation != null) {
            uuid = ovsdbBridgeAugmentation.getBridgeUuid().getValue();
        }
        return uuid;
    }

    private static void setManagedByForBridge(OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder,
                                     NodeKey ovsdbNodeKey) {
        InstanceIdentifier<Node> connectionNodePath = MdsalHelper.createInstanceIdentifier(ovsdbNodeKey.getNodeId());
        ovsdbBridgeAugmentationBuilder.setManagedBy(new OvsdbNodeRef(connectionNodePath));
    }

    private static void setControllerForBridge(Node ovsdbNode, String bridgeName, String targetString) {
        ConnectionInfo connectionInfo = getConnectionInfo(ovsdbNode);
        if (connectionInfo != null) {
            for (ControllerEntry controllerEntry: createControllerEntries(targetString)) {
                InstanceIdentifier<ControllerEntry> iid =
                        MdsalHelper.createInstanceIdentifier(ovsdbNode.getKey(), bridgeName)
                                .augmentation(OvsdbBridgeAugmentation.class)
                                .child(ControllerEntry.class, controllerEntry.getKey());

                boolean result = put(LogicalDatastoreType.CONFIGURATION, iid, controllerEntry);
                LOG.info("addController: result: {}", result);
            }
        }
    }

    private static List<ControllerEntry> createControllerEntries(String targetString) {
        List<ControllerEntry> controllerEntries = new ArrayList<ControllerEntry>();
        ControllerEntryBuilder controllerEntryBuilder = new ControllerEntryBuilder();
        controllerEntryBuilder.setTarget(new Uri(targetString));
        controllerEntries.add(controllerEntryBuilder.build());
        return controllerEntries;
    }

    private static List<ProtocolEntry> createMdsalProtocols() {
        List<ProtocolEntry> protocolList = new ArrayList<ProtocolEntry>();
        ImmutableBiMap<String, Class<? extends OvsdbBridgeProtocolBase>> mapper =
                MdsalHelper.OVSDB_PROTOCOL_MAP.inverse();
        protocolList.add(new ProtocolEntryBuilder().
                setProtocol((Class<? extends OvsdbBridgeProtocolBase>) mapper.get("OpenFlow13")).build());
        return protocolList;
    }

    public static long getDataPathId(Node node) {
        long dpid = 0L;
        String datapathId = getDatapathId(node);
        if (datapathId != null) {
            dpid = new BigInteger(datapathId.replaceAll(":", ""), 16).longValue();
        }
        return dpid;
    }

    public static String getDatapathId(Node node) {
        String datapathId = null;
        OvsdbBridgeAugmentation ovsdbBridgeAugmentation = node.getAugmentation(OvsdbBridgeAugmentation.class);
        if (ovsdbBridgeAugmentation != null && ovsdbBridgeAugmentation.getDatapathId() != null) {
            datapathId = node.getAugmentation(OvsdbBridgeAugmentation.class).getDatapathId().getValue();
        }
        return datapathId;
    }

    public static String getDatapathId(OvsdbBridgeAugmentation ovsdbBridgeAugmentation) {
        String datapathId = null;
        if (ovsdbBridgeAugmentation != null && ovsdbBridgeAugmentation.getDatapathId() != null) {
            datapathId = ovsdbBridgeAugmentation.getDatapathId().getValue();
        }
        return datapathId;
    }

    public static OvsdbBridgeAugmentation getBridge(Node node, String name) {
        OvsdbBridgeAugmentation bridge = node.getAugmentation(OvsdbBridgeAugmentation.class);
        if (bridge != null) {
            if (!bridge.getBridgeName().getValue().equals(name)) {
                bridge = null;
            }
        }
        return bridge;
    }

    public static OvsdbBridgeAugmentation getBridge(Node node) {
        OvsdbBridgeAugmentation bridge = node.getAugmentation(OvsdbBridgeAugmentation.class);
        return bridge;
    }

    public static String getBridgeName(Node node) {
        String bridgeName = null;
        OvsdbBridgeAugmentation bridge = getBridge(node);
        if (bridge != null) {
            bridgeName = bridge.getBridgeName().getValue();
        }
        return bridgeName;
    }

    public static String extractBridgeName(Node node) {
        return (node.getAugmentation(OvsdbBridgeAugmentation.class).getBridgeName().getValue());
    }

    public static OvsdbBridgeAugmentation extractBridgeAugmentation(Node node) {
        return node.getAugmentation(OvsdbBridgeAugmentation.class);
    }

    public static List<Node> getAllBridgesOnOvsdbNode(Node node) {
        List<Node> nodes = new ArrayList<Node>();
        List<ManagedNodeEntry> managedNodes = node.getAugmentation(OvsdbNodeAugmentation.class).getManagedNodeEntry();
        for (ManagedNodeEntry managedNode : managedNodes) {
            InstanceIdentifier<?> bridgeIid = managedNode.getBridgeRef().getValue();
            Node bridgeNode = (Node) read(LogicalDatastoreType.OPERATIONAL, bridgeIid);
            if (bridgeNode != null) {
                nodes.add(bridgeNode);
            }
        }
        return nodes;
    }

    public static OvsdbNodeAugmentation extractNodeAugmentation(Node node) {
        return node.getAugmentation(OvsdbNodeAugmentation.class);
    }

    /**
     * Method read ports from bridge node. Method will check if the provided node
     * has the ports details, if not, it will read from Operational data store.
     * @param node
     * @return
     */
    public static List<OvsdbTerminationPointAugmentation> getTerminationPointsOfBridge(Node node) {
        List<OvsdbTerminationPointAugmentation> tpAugmentations = extractTerminationPointAugmentations(node);
        if(tpAugmentations.isEmpty()){
            tpAugmentations = readTerminationPointAugmentations(node);
        }
        return tpAugmentations;
    }

    public static OvsdbTerminationPointAugmentation extractTerminationPointAugmentation(Node bridgeNode, String portName) {
        OvsdbBridgeAugmentation ovsdbBridgeAugmentation = bridgeNode.getAugmentation(OvsdbBridgeAugmentation.class);
        if (ovsdbBridgeAugmentation != null) {
            List<TerminationPoint> terminationPoints = bridgeNode.getTerminationPoint();
            for(TerminationPoint terminationPoint : terminationPoints) {
                OvsdbTerminationPointAugmentation ovsdbTerminationPointAugmentation =
                        terminationPoint.getAugmentation( OvsdbTerminationPointAugmentation.class);
                if (ovsdbTerminationPointAugmentation != null
                        && ovsdbTerminationPointAugmentation.getName().equals(portName)) {
                    return ovsdbTerminationPointAugmentation;
                }
            }
        }
        return null;
    }

    public static List<TerminationPoint> extractTerminationPoints(Node node) {
        List<TerminationPoint> terminationPoints = new ArrayList<TerminationPoint>();
        OvsdbBridgeAugmentation ovsdbBridgeAugmentation = node.getAugmentation(OvsdbBridgeAugmentation.class);
        if (ovsdbBridgeAugmentation != null) {
            terminationPoints.addAll(node.getTerminationPoint());
        }
        return terminationPoints;
    }

    public static List<OvsdbTerminationPointAugmentation> extractTerminationPointAugmentations( Node node ) {
        List<OvsdbTerminationPointAugmentation> tpAugmentations = new ArrayList<OvsdbTerminationPointAugmentation>();
        List<TerminationPoint> terminationPoints = node.getTerminationPoint();
        if(terminationPoints != null && !terminationPoints.isEmpty()){
            for(TerminationPoint tp : terminationPoints){
                tpAugmentations.add(tp.getAugmentation(OvsdbTerminationPointAugmentation.class));
            }
        }
        return tpAugmentations;
    }

    public static List<OvsdbTerminationPointAugmentation> readTerminationPointAugmentations(Node node) {
        InstanceIdentifier<Node> bridgeNodeIid = MdsalHelper.createInstanceIdentifier(node.getNodeId());
        Node operNode = read(LogicalDatastoreType.OPERATIONAL, bridgeNodeIid);
        if(operNode != null){
            return extractTerminationPointAugmentations(operNode);
        }
        return new ArrayList<OvsdbTerminationPointAugmentation>();
    }

    public static String getInterfaceExternalIdsValue(
            OvsdbTerminationPointAugmentation terminationPointAugmentation, String key) {
        String value = null;
        List<InterfaceExternalIds> pairs = terminationPointAugmentation.getInterfaceExternalIds();
        if (pairs != null && !pairs.isEmpty()) {
            for (InterfaceExternalIds pair : pairs) {
                if (pair.getExternalIdKey().equals(key)) {
                    value = pair.getExternalIdValue();
                    break;
                }
            }
        }
        return value;
    }

    public static Boolean addTerminationPoint(Node bridgeNode, String bridgeName, String portName, String type) {
        InstanceIdentifier<TerminationPoint> tpIid =
                MdsalHelper.createTerminationPointInstanceIdentifier(bridgeNode, portName);
        OvsdbTerminationPointAugmentationBuilder tpAugmentationBuilder =
                new OvsdbTerminationPointAugmentationBuilder();

        tpAugmentationBuilder.setName(portName);
        if (type != null) {
            tpAugmentationBuilder.setInterfaceType(MdsalHelper.OVSDB_INTERFACE_TYPE_MAP.get(type));
        }
        TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
        tpBuilder.setKey(InstanceIdentifier.keyOf(tpIid));
        tpBuilder.addAugmentation(OvsdbTerminationPointAugmentation.class, tpAugmentationBuilder.build());
        return put(LogicalDatastoreType.CONFIGURATION, tpIid, tpBuilder.build());
    }

    public static Boolean deleteTerminationPoint(Node bridgeNode, String portName) {
        InstanceIdentifier<TerminationPoint> tpIid =
                MdsalHelper.createTerminationPointInstanceIdentifier(bridgeNode, portName);
        return delete(LogicalDatastoreType.CONFIGURATION, tpIid);
    }

    public static Boolean addTerminationPoint(Node bridgeNode, String bridgeName, String portName,
            String type, Map<String, String> options) {
        InstanceIdentifier<TerminationPoint> tpIid = MdsalHelper.createTerminationPointInstanceIdentifier(
                bridgeNode, portName);
        OvsdbTerminationPointAugmentationBuilder tpAugmentationBuilder = new OvsdbTerminationPointAugmentationBuilder();

        tpAugmentationBuilder.setName(portName);
        if (type != null) {
            tpAugmentationBuilder.setInterfaceType(MdsalHelper.OVSDB_INTERFACE_TYPE_MAP.get(type));
        }

        List<Options> optionsList = new ArrayList<Options>();
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
        /* TODO SB_MIGRATION should this be merge or put */
        return put(LogicalDatastoreType.CONFIGURATION, tpIid, tpBuilder.build());
    }

    public static TerminationPoint readTerminationPoint(Node bridgeNode, String bridgeName, String portName) {
        InstanceIdentifier<TerminationPoint> tpIid = MdsalHelper.createTerminationPointInstanceIdentifier(
                bridgeNode, portName);
        return read(LogicalDatastoreType.OPERATIONAL, tpIid);
    }

    public static Boolean addTunnelTerminationPoint(Node bridgeNode, String bridgeName, String portName, String type,
                                        Map<String, String> options) {
        return addTerminationPoint(bridgeNode, bridgeName, portName, type, options);
    }

    public static Boolean isTunnelTerminationPointExist(Node bridgeNode, String bridgeName, String portName){
        return readTerminationPoint(bridgeNode, bridgeName, portName) != null;
    }

    public static Boolean addPatchTerminationPoint(Node node, String bridgeName, String portName, String peerPortName) {
        Map<String, String> option = new HashMap<String, String>();
        option.put("peer", peerPortName);
        return addTerminationPoint(node, bridgeName, portName, PATCH_PORT_TYPE, option);
    }

    public static String getExternalId(Node node, OvsdbTables table, String key) {
        switch (table) {
        case BRIDGE:
            OvsdbBridgeAugmentation bridge = extractBridgeAugmentation(node);
            if (bridge != null && bridge.getBridgeExternalIds() != null) {
                for (BridgeExternalIds bridgeExternaIds :bridge.getBridgeExternalIds()) {
                    if (bridgeExternaIds.getBridgeExternalIdKey().equals(key)) {
                        return bridgeExternaIds.getBridgeExternalIdValue();
                    }
                }
            }
            break;
        case CONTROLLER:
            LOG.warn("getExternalId: There is no external_id for OvsdbTables: ", table);
            return null;
        case OPENVSWITCH:
            OvsdbNodeAugmentation ovsdbNode = extractNodeAugmentation(node);
            if (ovsdbNode != null && ovsdbNode.getOpenvswitchExternalIds() != null) {
                for (OpenvswitchExternalIds openvswitchExternalIds : ovsdbNode.getOpenvswitchExternalIds()) {
                    if (openvswitchExternalIds.getExternalIdKey().equals(key)) {
                        return openvswitchExternalIds.getExternalIdValue();
                    }
                }
            }
            break;
        case PORT:
            List <OvsdbTerminationPointAugmentation> ports = extractTerminationPointAugmentations(node);
            for (OvsdbTerminationPointAugmentation port : ports) {
                if (port != null && port.getPortExternalIds() != null) {
                    for (PortExternalIds portExternalIds :port.getPortExternalIds()) {
                        if (portExternalIds.getExternalIdKey().equals(key)) {
                            return portExternalIds.getExternalIdValue();
                        }
                    }
                }
            }
            break;
        default:
            LOG.debug("getExternalId: Couldn't find the specified OvsdbTable: ", table);
            return null;
        }
        return null;
    }

    public static String getOtherConfig(Node node, OvsdbTables table, String key) {
        switch (table) {
            case BRIDGE:
                OvsdbBridgeAugmentation bridge = extractBridgeAugmentation(node);
                if (bridge != null && bridge.getBridgeOtherConfigs() != null) {
                    for (BridgeOtherConfigs bridgeOtherConfigs : bridge.getBridgeOtherConfigs()) {
                        if (bridgeOtherConfigs.getBridgeOtherConfigKey().equals(key)) {
                            return bridgeOtherConfigs.getBridgeOtherConfigValue();
                        }
                    }
                }
                break;
            case CONTROLLER:
                LOG.warn("getOtherConfig: There is no other_config for OvsdbTables: ", table);
                return null;

            case OPENVSWITCH:
                OvsdbNodeAugmentation ovsdbNode = extractNodeAugmentation(node);
                if (ovsdbNode == null){
                    ovsdbNode = readOvsdbNode(node);
                }
                if (ovsdbNode != null && ovsdbNode.getOpenvswitchOtherConfigs() != null) {
                    for (OpenvswitchOtherConfigs openvswitchOtherConfigs : ovsdbNode.getOpenvswitchOtherConfigs()) {
                        if (openvswitchOtherConfigs.getOtherConfigKey().equals(key)) {
                            return openvswitchOtherConfigs.getOtherConfigValue();
                        }
                    }
                }
                break;
            case PORT:
                List <OvsdbTerminationPointAugmentation> ports = extractTerminationPointAugmentations(node);
                for (OvsdbTerminationPointAugmentation port : ports) {
                    if (port != null && port.getPortOtherConfigs() != null) {
                        for (PortOtherConfigs portOtherConfigs : port.getPortOtherConfigs()) {
                            if (portOtherConfigs.getOtherConfigKey().equals(key)) {
                                return portOtherConfigs.getOtherConfigValue();
                            }
                        }
                    }
                }
                break;
            default:
                LOG.debug("getOtherConfig: Couldn't find the specified OvsdbTable: ", table);
                return null;
        }
        return null;
    }

    public static boolean addVlanToTp(long vlan) {
        return false;
    }

    public static boolean isTunnel(OvsdbTerminationPointAugmentation port) {
        LOG.trace("MdsalUtils#isTunnel: Interface : {}", port);

        if(port.getInterfaceType() == null){
            LOG.warn("No type found for the interface : {}", port);
            return false;
        }
        return MdsalHelper.createOvsdbInterfaceType(
                port.getInterfaceType()).equals(NetworkHandler.NETWORK_TYPE_VXLAN)
                || MdsalHelper.createOvsdbInterfaceType(
                port.getInterfaceType()).equals(NetworkHandler.NETWORK_TYPE_GRE);
    }

    public static String getOptionsValue(List<Options> options, String key) {
        String value = null;
        for (Options option : options) {
            if (option.getKey().equals(key)) {
                value = option.getValue();
            }
        }
        return value;
    }

    private static Topology getOvsdbTopology() {
        InstanceIdentifier<Topology> path = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(MdsalHelper.OVSDB_TOPOLOGY_ID));

        Topology topology = read(LogicalDatastoreType.OPERATIONAL, path);
        return topology;
    }

    public static Long getOFPort(OvsdbTerminationPointAugmentation port) {
        Long ofPort = 0L;
        if (port.getOfport() != null) {
            ofPort = port.getOfport();
        }
        return ofPort;
    }

    public static Long getOFPort(Node bridgeNode, String portName) {
        Long ofPort = 0L;
        OvsdbTerminationPointAugmentation port = extractTerminationPointAugmentation(bridgeNode, portName);
        if (port != null) {
            ofPort = getOFPort(port);
        } else {
            TerminationPoint tp = readTerminationPoint(bridgeNode, null, portName);
            if (tp != null) {
                port = tp.getAugmentation(OvsdbTerminationPointAugmentation.class);
                if (port != null) {
                    ofPort = getOFPort(port);
                }
            }
        }
        return ofPort;
    }
}
