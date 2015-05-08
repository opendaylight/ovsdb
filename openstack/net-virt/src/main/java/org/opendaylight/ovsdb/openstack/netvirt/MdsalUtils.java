/*
 * Copyright (c) 2015 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.openstack.netvirt;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.util.concurrent.CheckedFuture;

import java.math.BigInteger;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.ovsdb.openstack.netvirt.api.OvsdbTables;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeInternal;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeProtocolBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ProtocolEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ProtocolEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.Options;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.OptionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.OptionsKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to wrap mdsal transactions.
 *
 * @author Sam Hague (shague@redhat.com)
 */
public class MdsalUtils {
    private static final Logger LOG = LoggerFactory.getLogger(MdsalUtils.class);
    private static DataBroker databroker = null;
    private static final int OVSDB_UPDATE_TIMEOUT = 500;

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
        for (OpenvswitchExternalIds pair : pairs) {
            if (pair.getKey().equals(key)) {
                value = pair.getExternalIdValue();
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
                    SouthboundMapper.createInstanceIdentifier(connectionInfo, new OvsdbBridgeName(bridgeName));
            NodeId bridgeNodeId = SouthboundMapper.createManagedNodeId(bridgeIid);
            bridgeNodeBuilder.setNodeId(bridgeNodeId);
            OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();
            //ovsdbBridgeAugmentationBuilder.setControllerEntry(setControllerEntries(target));
            ovsdbBridgeAugmentationBuilder.setBridgeName(new OvsdbBridgeName(bridgeName));
            ovsdbBridgeAugmentationBuilder.setProtocolEntry(createMdsalProtocols());
            ovsdbBridgeAugmentationBuilder.setFailMode(
                    SouthboundConstants.OVSDB_FAIL_MODE_MAP.inverse().get("secure"));
            setManagedByForBridge(ovsdbBridgeAugmentationBuilder, connectionInfo);
            bridgeNodeBuilder.addAugmentation(OvsdbBridgeAugmentation.class, ovsdbBridgeAugmentationBuilder.build());

            result = put(LogicalDatastoreType.CONFIGURATION, bridgeIid, bridgeNodeBuilder.build());
            LOG.info("addBridge: result: {}", result);
            Thread.sleep(OVSDB_UPDATE_TIMEOUT);
            setControllerForBridge(ovsdbNode, bridgeName, target);
            Thread.sleep(OVSDB_UPDATE_TIMEOUT);
        } else {
            throw new InvalidParameterException("Could not find ConnectionInfo");
        }
        return result;
    }

    public static boolean deleteBridge(Node ovsdbNode) {
        boolean result = false;
        LOG.info("deleteBridge: node: {}, bridgeName: {}", ovsdbNode, ovsdbNode.getNodeId());

        InstanceIdentifier<Node> bridgeIid =
                SouthboundMapper.createInstanceIdentifier(ovsdbNode.getNodeId());

        result = delete(LogicalDatastoreType.CONFIGURATION, bridgeIid);
        LOG.info("deleteBridge: result: {}", result);
        return result;
    }

    public static OvsdbBridgeAugmentation readBridge(Node node, String name) {
        OvsdbBridgeAugmentation ovsdbBridgeAugmentation = null;
        ConnectionInfo connectionInfo = getConnectionInfo(node);
        if (connectionInfo != null) {
            InstanceIdentifier<Node> bridgeIid =
                    SouthboundMapper.createInstanceIdentifier(connectionInfo,
                            new OvsdbBridgeName(name));
            Node bridgeNode = read(LogicalDatastoreType.OPERATIONAL, bridgeIid);
            if (bridgeNode != null) {
                ovsdbBridgeAugmentation = bridgeNode.getAugmentation(OvsdbBridgeAugmentation.class);
            }
        }
        return ovsdbBridgeAugmentation;
    }

    public static Uuid getBridgeUuid(Node node, String name) {
        Uuid uuid = null;
        OvsdbBridgeAugmentation ovsdbBridgeAugmentation = readBridge(node, name);
        if (ovsdbBridgeAugmentation != null) {
            uuid = ovsdbBridgeAugmentation.getBridgeUuid();
        }
        return uuid;
    }

    private static void setManagedByForBridge(OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder,
                                     ConnectionInfo connectionInfo) {
        InstanceIdentifier<Node> connectionNodePath = SouthboundMapper.createInstanceIdentifier(connectionInfo);
        ovsdbBridgeAugmentationBuilder.setManagedBy(new OvsdbNodeRef(connectionNodePath));
    }

    private static void setControllerForBridge(Node ovsdbNode, String bridgeName, String targetString) {
        ConnectionInfo connectionInfo = getConnectionInfo(ovsdbNode);
        if (connectionInfo != null) {
            for (ControllerEntry controllerEntry: createControllerEntries(targetString)) {
                InstanceIdentifier<ControllerEntry> iid =
                        SouthboundMapper.createInstanceIdentifier(connectionInfo, new OvsdbBridgeName(bridgeName))
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
                SouthboundConstants.OVSDB_PROTOCOL_MAP.inverse();
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
            if (!bridge.getBridgeName().equals(name)) {
                bridge = null;
            }
        }
        return bridge;
    }

    public static String extractBridgeName(Node node) {
        return (node.getAugmentation(OvsdbBridgeAugmentation.class).getBridgeName().getValue());
    }

    public static OvsdbBridgeAugmentation extractBridgeAugmentation(Node node) {
        return node.getAugmentation(OvsdbBridgeAugmentation.class);
    }

    public static List<Node> getAllBridgesOnOvsdbNode(Node node) {
        return null;
    }

    /**
     * Method read ports from bridge node. Method will check if the provided node
     * has the ports details, if not, it will read from Operational data store.
     * @param node
     * @return
     */
    public static List<OvsdbTerminationPointAugmentation> getTerminationPointsOfBridge(Node node) {
        List<OvsdbTerminationPointAugmentation> tpAugmentations = extractTerminationPointAugmentations( node);
        if(tpAugmentations.isEmpty()){
            tpAugmentations = readTerminationPointAugmentationFromDataStore(node);
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

    public static List<OvsdbTerminationPointAugmentation> readTerminationPointAugmentationFromDataStore( Node node ) {
        InstanceIdentifier<Node> bridgeNodeIid = SouthboundMapper.createInstanceIdentifier(node.getNodeId());
        Node operNode = read(LogicalDatastoreType.OPERATIONAL, bridgeNodeIid);
        if(operNode != null){
            return extractTerminationPointAugmentations(operNode);
        }
        return new ArrayList<OvsdbTerminationPointAugmentation>();
    }

    public static String getInterfaceExternalIdsValue(
            OvsdbTerminationPointAugmentation terminationPointAugmentation,String key) {
        String value = null;
        List<InterfaceExternalIds> pairs = terminationPointAugmentation.getInterfaceExternalIds();
        if (pairs != null) {
            for (InterfaceExternalIds pair : pairs) {
                if (pair.getKey().equals(key)) {
                    value = pair.getExternalIdValue();
                }
            }
        }
        return value;
    }

    public static Boolean addTerminationPoint(Node bridgeNode, String bridgeName, String portName) {
        InstanceIdentifier<TerminationPoint> tpIid =
                MdsalHelper.createTerminationPointInstanceIdentifier(bridgeNode, portName);
        OvsdbTerminationPointAugmentationBuilder tpAugmentationBuilder =
                new OvsdbTerminationPointAugmentationBuilder();

        tpAugmentationBuilder.setName(portName);
        tpAugmentationBuilder.setInterfaceType(InterfaceTypeInternal.class);
        TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
        tpBuilder.addAugmentation(OvsdbTerminationPointAugmentation.class, tpAugmentationBuilder.build());
        return put(LogicalDatastoreType.CONFIGURATION,tpIid,tpBuilder.build());
    }

    public static Boolean deleteTerminationPoint(Node bridgeNode, String portName) {
        InstanceIdentifier<TerminationPoint> tpIid =
                MdsalHelper.createTerminationPointInstanceIdentifier(bridgeNode, portName);
        return delete(LogicalDatastoreType.CONFIGURATION,tpIid);
    }

    public static Boolean addTunnelTerminationPoint(Node bridgeNode, String bridgeName, String portName, String type,
                                        Map<String, String> options) {
        InstanceIdentifier<TerminationPoint> tpIid =
                MdsalHelper.createTerminationPointInstanceIdentifier(bridgeNode, portName);
        OvsdbTerminationPointAugmentationBuilder tpAugmentationBuilder =
                new OvsdbTerminationPointAugmentationBuilder();

        tpAugmentationBuilder.setName(portName);
        tpAugmentationBuilder.setInterfaceType(MdsalHelper.OVSDB_INTERFACE_TYPE_MAP.get(type));

        List<Options> optionsList = new ArrayList<Options>();
        for(Map.Entry<String,String> entry : options.entrySet()){
            OptionsBuilder optionsBuilder = new OptionsBuilder();
            optionsBuilder.setKey(new OptionsKey(entry.getKey()));
            optionsBuilder.setOption(entry.getKey());
            optionsBuilder.setValue(entry.getValue());
            optionsList.add(optionsBuilder.build());
        }
        tpAugmentationBuilder.setOptions(optionsList);

        TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
        tpBuilder.addAugmentation(OvsdbTerminationPointAugmentation.class, tpAugmentationBuilder.build());
        return put(LogicalDatastoreType.CONFIGURATION,tpIid,tpBuilder.build());
    }

    public static Boolean addPatchTerminationPoint(Node node, String bridgeName, String portName, String peerPortName) {
        return false;
    }

    public static String getExternalId(Node node, OvsdbTables table, String key) {
        return null;
    }

    public static String getOtherConfig(Node node, OvsdbTables table, String key) {
        return null;
    }

    public static boolean addVlanToTp(long vlan) {
        return false;
    }

    public static boolean isTunnel(OvsdbTerminationPointAugmentation port) {
        return SouthboundMapper.createOvsdbInterfaceType(
                port.getInterfaceType()).equals(NetworkHandler.NETWORK_TYPE_VXLAN)
                || SouthboundMapper.createOvsdbInterfaceType(
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
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID));

        Topology topology = read(LogicalDatastoreType.OPERATIONAL, path);
        return topology;
    }

}
