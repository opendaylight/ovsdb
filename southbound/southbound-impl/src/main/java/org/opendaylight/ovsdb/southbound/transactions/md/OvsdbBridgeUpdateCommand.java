/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.transactions.md;

import static org.opendaylight.ovsdb.southbound.SouthboundUtil.schemaMismatchLog;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.net.InetAddresses;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.math.NumberUtils;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.ovsdb.lib.error.SchemaVersionMismatchException;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.Controller;
import org.opendaylight.ovsdb.southbound.InstanceIdentifierCodec;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionInstance;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.ovsdb.southbound.SouthboundUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeProtocolBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.BridgeExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.BridgeExternalIdsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.BridgeExternalIdsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.BridgeOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.BridgeOtherConfigsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.BridgeOtherConfigsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ProtocolEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ProtocolEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagedNodeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagedNodeEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsdbBridgeUpdateCommand extends AbstractTransactionCommand {
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbBridgeUpdateCommand.class);
    private final InstanceIdentifierCodec instanceIdentifierCodec;
    private final Map<UUID,Bridge> updatedBridgeRows;
    private final Map<UUID, Bridge> oldBridgeRows;
    private final List<InstanceIdentifier<Node>> updatedBridges = new ArrayList<>();
    private final Map<NodeId, Node> updatedBridgeNodes;

    public OvsdbBridgeUpdateCommand(InstanceIdentifierCodec instanceIdentifierCodec, OvsdbConnectionInstance key,
            TableUpdates updates, DatabaseSchema dbSchema,
                                    Map<NodeId, Node> updatedBridgeNodes) {
        super(key,updates,dbSchema);
        this.instanceIdentifierCodec = instanceIdentifierCodec;
        updatedBridgeRows = TyperUtils.extractRowsUpdated(Bridge.class, getUpdates(), getDbSchema());
        oldBridgeRows = TyperUtils.extractRowsOld(Bridge.class, getUpdates(), getDbSchema());
        this.updatedBridgeNodes = updatedBridgeNodes;
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        if (updatedBridgeRows == null || updatedBridgeRows.isEmpty()) {
            return;
        }

        final InstanceIdentifier<Node> connectionIId = getOvsdbConnectionInstance().getInstanceIdentifier();
        Optional<Node> connection = SouthboundUtil.readNode(transaction, connectionIId);
        if (!connection.isPresent()) {
            return;
        }

        for (Entry<UUID, Bridge> entry : updatedBridgeRows.entrySet()) {
            updateBridge(transaction, entry.getValue(), connectionIId);
        }
    }

    @VisibleForTesting
    void updateBridge(ReadWriteTransaction transaction,
            Bridge bridge, InstanceIdentifier<Node> connectionIId) {
        // Update the connection node to let it know it manages this bridge
        Node connectionNode = buildConnectionNode(bridge);
        transaction.merge(LogicalDatastoreType.OPERATIONAL, connectionIId, connectionNode);

        // Update the bridge node with whatever data we are getting
        InstanceIdentifier<Node> bridgeIid = getInstanceIdentifier(bridge);
        Node bridgeNode = buildBridgeNode(bridge);
        transaction.merge(LogicalDatastoreType.OPERATIONAL, bridgeIid, bridgeNode);
        updatedBridges.add(bridgeIid);
        updatedBridgeNodes.put(getNodeId(bridge), bridgeNode);
        deleteEntries(transaction, protocolEntriesToRemove(bridgeIid, bridge));
        deleteEntries(transaction, externalIdsToRemove(bridgeIid,bridge));
        deleteEntries(transaction, bridgeOtherConfigsToRemove(bridgeIid,bridge));
    }

    @VisibleForTesting
    <T extends DataObject> void deleteEntries(ReadWriteTransaction transaction,
            List<InstanceIdentifier<T>> entryIids) {
        for (InstanceIdentifier<T> entryIid : entryIids) {
            transaction.delete(LogicalDatastoreType.OPERATIONAL, entryIid);
        }
    }

    private List<InstanceIdentifier<BridgeOtherConfigs>> bridgeOtherConfigsToRemove(
            InstanceIdentifier<Node> bridgeIid, Bridge bridge) {
        Preconditions.checkNotNull(bridgeIid);
        Preconditions.checkNotNull(bridge);
        List<InstanceIdentifier<BridgeOtherConfigs>> result = new ArrayList<>();

        Bridge oldBridge = oldBridgeRows.get(bridge.getUuid());

        if (oldBridge != null && oldBridge.getOtherConfigColumn() != null) {
            for (Entry<String, String> otherConfig:
                oldBridge.getOtherConfigColumn().getData().entrySet()) {
                if (bridge.getOtherConfigColumn() == null
                        || !bridge.getOtherConfigColumn().getData().containsKey(otherConfig.getKey())) {
                    InstanceIdentifier<BridgeOtherConfigs> iid = bridgeIid
                            .augmentation(OvsdbBridgeAugmentation.class)
                            .child(BridgeOtherConfigs.class,
                                    new BridgeOtherConfigsKey(otherConfig.getKey()));
                    result.add(iid);
                }
            }
        }
        return result;
    }

    private List<InstanceIdentifier<BridgeExternalIds>> externalIdsToRemove(
            InstanceIdentifier<Node> bridgeIid, Bridge bridge) {
        Preconditions.checkNotNull(bridgeIid);
        Preconditions.checkNotNull(bridge);
        List<InstanceIdentifier<BridgeExternalIds>> result = new ArrayList<>();

        Bridge oldBridge = oldBridgeRows.get(bridge.getUuid());

        if (oldBridge != null && oldBridge.getExternalIdsColumn() != null) {
            for (Entry<String, String> externalId:
                oldBridge.getExternalIdsColumn().getData().entrySet()) {
                if (bridge.getExternalIdsColumn() == null
                        || !bridge.getExternalIdsColumn().getData().containsKey(externalId.getKey())) {
                    InstanceIdentifier<BridgeExternalIds> iid = bridgeIid
                            .augmentation(OvsdbBridgeAugmentation.class)
                            .child(BridgeExternalIds.class,
                                    new BridgeExternalIdsKey(externalId.getKey()));
                    result.add(iid);
                }
            }
        }
        return result;
    }

    private List<InstanceIdentifier<ProtocolEntry>> protocolEntriesToRemove(
            InstanceIdentifier<Node> bridgeIid, Bridge bridge) {
        Preconditions.checkNotNull(bridgeIid);
        Preconditions.checkNotNull(bridge);
        List<InstanceIdentifier<ProtocolEntry>> result = new ArrayList<>();
        Bridge oldBridge = oldBridgeRows.get(bridge.getUuid());

        try {
            if (oldBridge != null && oldBridge.getProtocolsColumn() != null) {
                for (String protocol : oldBridge.getProtocolsColumn().getData()) {
                    if (bridge.getProtocolsColumn() == null || !bridge.getProtocolsColumn().getData()
                                .contains(protocol)) {
                        Class<? extends OvsdbBridgeProtocolBase> proto = SouthboundConstants.OVSDB_PROTOCOL_MAP
                                .inverse().get(protocol);
                        InstanceIdentifier<ProtocolEntry> iid = bridgeIid
                                .augmentation(OvsdbBridgeAugmentation.class)
                                .child(ProtocolEntry.class,
                                        new ProtocolEntryKey(proto));
                        result.add(iid);
                    }
                }
            }
        } catch (SchemaVersionMismatchException e) {
            schemaMismatchLog("protocols", "Bridge", e);
        }
        return result;
    }

    private Node buildConnectionNode(
            Bridge bridge) {
        //Update node with managed node reference
        NodeBuilder connectionNode = new NodeBuilder();
        connectionNode.setNodeId(getOvsdbConnectionInstance().getNodeId());

        OvsdbNodeAugmentationBuilder ovsdbConnectionAugmentationBuilder = new OvsdbNodeAugmentationBuilder();
        List<ManagedNodeEntry> managedBridges = new ArrayList<>();
        InstanceIdentifier<Node> bridgeIid =
                SouthboundMapper.createInstanceIdentifier(instanceIdentifierCodec, getOvsdbConnectionInstance(),
                        bridge);
        ManagedNodeEntry managedBridge = new ManagedNodeEntryBuilder().setBridgeRef(
                new OvsdbBridgeRef(bridgeIid)).build();
        managedBridges.add(managedBridge);
        ovsdbConnectionAugmentationBuilder.setManagedNodeEntry(managedBridges);

        connectionNode.addAugmentation(ovsdbConnectionAugmentationBuilder.build());

        LOG.debug("Update node with bridge node ref {}",
                ovsdbConnectionAugmentationBuilder.getManagedNodeEntry().values().iterator().next());
        return connectionNode.build();
    }

    private Node buildBridgeNode(Bridge bridge) {
        NodeBuilder bridgeNodeBuilder = new NodeBuilder();
        NodeId bridgeNodeId = getNodeId(bridge);
        bridgeNodeBuilder.setNodeId(bridgeNodeId);
        OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();
        ovsdbBridgeAugmentationBuilder.setBridgeName(new OvsdbBridgeName(bridge.getName()));
        ovsdbBridgeAugmentationBuilder.setBridgeUuid(new Uuid(bridge.getUuid().toString()));
        setDataPath(ovsdbBridgeAugmentationBuilder, bridge);
        setDataPathType(ovsdbBridgeAugmentationBuilder, bridge);
        setProtocol(ovsdbBridgeAugmentationBuilder, bridge);
        setExternalIds(ovsdbBridgeAugmentationBuilder, bridge);
        setOtherConfig(ovsdbBridgeAugmentationBuilder, bridge);
        setFailMode(ovsdbBridgeAugmentationBuilder, bridge);
        setOpenFlowNodeRef(ovsdbBridgeAugmentationBuilder, bridge);
        setManagedBy(ovsdbBridgeAugmentationBuilder);
        setAutoAttach(ovsdbBridgeAugmentationBuilder, bridge);
        setStpEnalbe(ovsdbBridgeAugmentationBuilder,bridge);
        bridgeNodeBuilder.addAugmentation(ovsdbBridgeAugmentationBuilder.build());

        LOG.debug("Built with the intent to store bridge data {}",
                ovsdbBridgeAugmentationBuilder.build());
        return bridgeNodeBuilder.build();
    }

    private static void setAutoAttach(OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder, Bridge bridge) {
        try {
            if (bridge.getAutoAttachColumn() != null
                    && bridge.getAutoAttachColumn().getData() != null
                    && !bridge.getAutoAttachColumn().getData().isEmpty()) {
                Set<UUID> uuids = bridge.getAutoAttachColumn().getData();
                for (UUID uuid : uuids) {
                    ovsdbBridgeAugmentationBuilder.setAutoAttach(new Uuid(uuid.toString()));
                }
            }
        } catch (SchemaVersionMismatchException e) {
            schemaMismatchLog("auto_attach", "Bridge", e);
        }
    }

    private void setManagedBy(OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder) {
        InstanceIdentifier<Node> connectionNodePath = getOvsdbConnectionInstance().getInstanceIdentifier();
        ovsdbBridgeAugmentationBuilder.setManagedBy(new OvsdbNodeRef(connectionNodePath));
    }

    private static void setDataPathType(OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder, Bridge bridge) {
        ovsdbBridgeAugmentationBuilder.setDatapathType(
                SouthboundMapper.createDatapathType(bridge.getDatapathTypeColumn().getData()));
    }

    private static void setFailMode(OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder, Bridge bridge) {
        if (bridge.getFailModeColumn() != null
                && bridge.getFailModeColumn().getData() != null
                && !bridge.getFailModeColumn().getData().isEmpty()) {
            String[] failmodeArray = new String[bridge.getFailModeColumn().getData().size()];
            bridge.getFailModeColumn().getData().toArray(failmodeArray);
            ovsdbBridgeAugmentationBuilder.setFailMode(
                    SouthboundConstants.OVSDB_FAIL_MODE_MAP.inverse().get(failmodeArray[0]));
        }
    }

    private static void setOtherConfig(OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder, Bridge bridge) {
        Map<String, String> otherConfigs = bridge
                .getOtherConfigColumn().getData();
        if (otherConfigs != null && !otherConfigs.isEmpty()) {
            List<BridgeOtherConfigs> otherConfigList = new ArrayList<>();
            for (Entry<String, String> entry : otherConfigs.entrySet()) {
                String otherConfigKey = entry.getKey();
                String otherConfigValue = entry.getValue();
                if (otherConfigKey != null && otherConfigValue != null) {
                    otherConfigList.add(new BridgeOtherConfigsBuilder()
                            .setBridgeOtherConfigKey(otherConfigKey)
                            .setBridgeOtherConfigValue(otherConfigValue)
                            .build());
                }
            }
            ovsdbBridgeAugmentationBuilder.setBridgeOtherConfigs(otherConfigList);
        }
    }

    private static void setExternalIds(OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder, Bridge bridge) {
        Map<String, String> externalIds = bridge.getExternalIdsColumn()
                .getData();
        if (externalIds != null && !externalIds.isEmpty()) {
            List<BridgeExternalIds> externalIdsList = new ArrayList<>();
            for (Entry<String, String> entry : externalIds.entrySet()) {
                String externalIdKey = entry.getKey();
                String externalIdValue = entry.getValue();
                if (externalIdKey != null && externalIdValue != null) {
                    externalIdsList.add(new BridgeExternalIdsBuilder()
                            .setBridgeExternalIdKey(externalIdKey)
                            .setBridgeExternalIdValue(externalIdValue)
                            .build());
                }
            }
            ovsdbBridgeAugmentationBuilder.setBridgeExternalIds(externalIdsList);
        }
    }

    private static void setProtocol(OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder, Bridge bridge) {
        final List<ProtocolEntry> protocols = SouthboundMapper.createMdsalProtocols(bridge);
        if (!protocols.isEmpty()) {
            ovsdbBridgeAugmentationBuilder.setProtocolEntry(protocols);
        }
    }

    private static void setDataPath(OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder, Bridge bridge) {
        DatapathId dpid = SouthboundMapper.createDatapathId(bridge);
        if (dpid != null) {
            ovsdbBridgeAugmentationBuilder.setDatapathId(dpid);
        }
    }

    private static void setStpEnalbe(OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder,
                              Bridge bridge) {
        if (bridge.getStpEnableColumn() != null) {
            Boolean stpEnable = bridge.getStpEnableColumn().getData();
            if (stpEnable != null) {
                ovsdbBridgeAugmentationBuilder.setStpEnable(stpEnable);
            }
        }
    }

    private void setOpenFlowNodeRef(OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder, Bridge bridge) {
        Map<UUID, Controller> updatedControllerRows =
                TyperUtils.extractRowsUpdated(Controller.class, getUpdates(), getDbSchema());
        LOG.debug("setOpenFlowNodeRef: updatedControllerRows: {}", updatedControllerRows);
        for (ControllerEntry controllerEntry: SouthboundMapper.createControllerEntries(bridge, updatedControllerRows)) {
            if (controllerEntry != null
                && controllerEntry.isIsConnected() != null && controllerEntry.isIsConnected()) {
                String [] controllerTarget = controllerEntry.getTarget().getValue().split(":");
                IpAddress bridgeControllerIpAddress = null;
                for (String targetElement : controllerTarget) {
                    if (InetAddresses.isInetAddress(targetElement)) {
                        bridgeControllerIpAddress = IpAddressBuilder.getDefaultInstance(targetElement);
                        continue;
                    }
                    if (NumberUtils.isCreatable(targetElement)) {
                        continue;
                    }
                }
                try {
                    Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
                networkInterfacesLoop:
                    while (networkInterfaces.hasMoreElements()) {
                        NetworkInterface networkInterface = networkInterfaces.nextElement();
                        Enumeration<InetAddress> networkInterfaceAddresses = networkInterface.getInetAddresses();
                        while (networkInterfaceAddresses.hasMoreElements()) {
                            InetAddress networkInterfaceAddress = networkInterfaceAddresses.nextElement();
                            if (bridgeControllerIpAddress.getIpv4Address().getValue()
                                    .equals(networkInterfaceAddress.getHostAddress())) {
                                ovsdbBridgeAugmentationBuilder.setBridgeOpenflowNodeRef(
                                        getOvsdbConnectionInstance().getInstanceIdentifier());
                                break networkInterfacesLoop;
                            }
                        }
                    }
                } catch (SocketException e) {
                    LOG.warn("Error getting local ip address", e);
                }
            }
        }
    }

    private InstanceIdentifier<Node> getInstanceIdentifier(Bridge bridge) {
        return SouthboundMapper.createInstanceIdentifier(instanceIdentifierCodec, getOvsdbConnectionInstance(),
                bridge);
    }

    private NodeId getNodeId(Bridge bridge) {
        NodeKey nodeKey = getInstanceIdentifier(bridge).firstKeyOf(Node.class);
        return nodeKey.getNodeId();
    }

    @Override
    public void onSuccess() {
        for (InstanceIdentifier<Node> updatedBridge : updatedBridges) {
            LOG.debug("Updated bridge {} in operational datastore", updatedBridge);
        }
    }

    @Override
    public void onFailure(Throwable throwable) {
        for (InstanceIdentifier<Node> updatedBridge : updatedBridges) {
            LOG.error("Failed to update bridge {} in operational datastore", updatedBridge);
        }
    }
}
