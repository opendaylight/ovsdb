/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.southbound.transactions.md;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.math.NumberUtils;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.lib.error.SchemaVersionMismatchException;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.Controller;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionInstance;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.ovsdb.southbound.SouthboundUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeProtocolBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
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

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.net.InetAddresses;

public class OvsdbBridgeUpdateCommand extends AbstractTransactionCommand {
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbBridgeUpdateCommand.class);
    private Map<UUID,Bridge> updatedBridgeRows;
    private Map<UUID, Bridge> oldBridgeRows;

    public OvsdbBridgeUpdateCommand(OvsdbConnectionInstance key, TableUpdates updates,
            DatabaseSchema dbSchema) {
        super(key,updates,dbSchema);
        updatedBridgeRows = TyperUtils.extractRowsUpdated(Bridge.class, getUpdates(), getDbSchema());
        oldBridgeRows = TyperUtils.extractRowsOld(Bridge.class, getUpdates(), getDbSchema());
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        for (Entry<UUID, Bridge> entry : updatedBridgeRows.entrySet()) {
            updateBridge(transaction, entry.getValue());
        }
    }

    private void updateBridge(ReadWriteTransaction transaction,
            Bridge bridge) {
        final InstanceIdentifier<Node> connectionIId = getOvsdbConnectionInstance().getInstanceIdentifier();
        Optional<Node> connection = SouthboundUtil.readNode(transaction, connectionIId);
        if (connection.isPresent()) {
            LOG.debug("Connection {} is present",connection);

            // Update the connection node to let it know it manages this bridge
            Node connectionNode = buildConnectionNode(bridge);
            transaction.merge(LogicalDatastoreType.OPERATIONAL, connectionIId, connectionNode);

            // Update the bridge node with whatever data we are getting
            InstanceIdentifier<Node> bridgeIid = getInstanceIdentifier(bridge);
            Node bridgeNode = buildBridgeNode(bridge);
            transaction.merge(LogicalDatastoreType.OPERATIONAL, bridgeIid, bridgeNode);
            deleteEntries(transaction, protocolEntriesToRemove(bridgeIid,bridge));
            deleteEntries(transaction, externalIdsToRemove(bridgeIid,bridge));
            deleteEntries(transaction, bridgeOtherConfigsToRemove(bridgeIid,bridge));
        }
    }

    private <T extends DataObject> void deleteEntries(ReadWriteTransaction transaction,
            List<InstanceIdentifier<T>> entryIids) {
        for (InstanceIdentifier<T> entryIid: entryIids) {
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
            LOG.warn("protocol not supported by this version of ovsdb", e);
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
        InstanceIdentifier<Node> bridgeIid = SouthboundMapper.createInstanceIdentifier(getOvsdbConnectionInstance(),
                bridge);
        ManagedNodeEntry managedBridge = new ManagedNodeEntryBuilder().setBridgeRef(
                new OvsdbBridgeRef(bridgeIid)).build();
        managedBridges.add(managedBridge);
        ovsdbConnectionAugmentationBuilder.setManagedNodeEntry(managedBridges);

        connectionNode.addAugmentation(OvsdbNodeAugmentation.class, ovsdbConnectionAugmentationBuilder.build());

        LOG.debug("Update node with bridge node ref {}",
                ovsdbConnectionAugmentationBuilder.getManagedNodeEntry().iterator().next());
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
        bridgeNodeBuilder.addAugmentation(OvsdbBridgeAugmentation.class, ovsdbBridgeAugmentationBuilder.build());

        LOG.debug("Built with the intent to store bridge data {}",
                ovsdbBridgeAugmentationBuilder.build());
        return bridgeNodeBuilder.build();
    }

    private void setManagedBy(OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder) {
        InstanceIdentifier<Node> connectionNodePath = getOvsdbConnectionInstance().getInstanceIdentifier();
        ovsdbBridgeAugmentationBuilder.setManagedBy(new OvsdbNodeRef(connectionNodePath));
    }

    private void setDataPathType(OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder,
            Bridge bridge) {
        ovsdbBridgeAugmentationBuilder.setDatapathType(
                SouthboundMapper.createDatapathType(bridge.getDatapathTypeColumn().getData()));
    }

    private void setFailMode(OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder,
            Bridge bridge) {
        if (bridge.getFailModeColumn() != null
                && bridge.getFailModeColumn().getData() != null
                && !bridge.getFailModeColumn().getData().isEmpty()) {
            String[] failmodeArray = new String[bridge.getFailModeColumn().getData().size()];
            bridge.getFailModeColumn().getData().toArray(failmodeArray);
            ovsdbBridgeAugmentationBuilder.setFailMode(
                    SouthboundConstants.OVSDB_FAIL_MODE_MAP.inverse().get(failmodeArray[0]));
        }
    }

    private void setOtherConfig(OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder,
            Bridge bridge) {
        Map<String, String> otherConfigs = bridge
                .getOtherConfigColumn().getData();
        if (otherConfigs != null && !otherConfigs.isEmpty()) {
            Set<String> otherConfigKeys = otherConfigs.keySet();
            List<BridgeOtherConfigs> otherConfigList = new ArrayList<>();
            String otherConfigValue;
            for (String otherConfigKey : otherConfigKeys) {
                otherConfigValue = otherConfigs.get(otherConfigKey);
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

    private void setExternalIds(OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder,
            Bridge bridge) {
        Map<String, String> externalIds = bridge.getExternalIdsColumn()
                .getData();
        if (externalIds != null && !externalIds.isEmpty()) {
            Set<String> externalIdKeys = externalIds.keySet();
            List<BridgeExternalIds> externalIdsList = new ArrayList<>();
            String externalIdValue;
            for (String externalIdKey : externalIdKeys) {
                externalIdValue = externalIds.get(externalIdKey);
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

    private void setProtocol(OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder,
            Bridge bridge) {
        if (SouthboundMapper.createMdsalProtocols(bridge) != null
                && SouthboundMapper.createMdsalProtocols(bridge).size() > 0) {
            ovsdbBridgeAugmentationBuilder.setProtocolEntry(SouthboundMapper.createMdsalProtocols(bridge));
        }
    }

    private void setDataPath(OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder,
            Bridge bridge) {
        DatapathId dpid = SouthboundMapper.createDatapathId(bridge);
        if (dpid != null) {
            ovsdbBridgeAugmentationBuilder.setDatapathId(dpid);
        }
    }

    private void setOpenFlowNodeRef(OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder,
            Bridge bridge) {
        Map<UUID, Controller> updatedControllerRows =
                TyperUtils.extractRowsUpdated(Controller.class, getUpdates(), getDbSchema());
        LOG.debug("setOpenFlowNodeRef: updatedControllerRows: {}", updatedControllerRows);
        for (ControllerEntry controllerEntry: SouthboundMapper.createControllerEntries(bridge, updatedControllerRows)) {
            if (controllerEntry != null
                && controllerEntry.isIsConnected() != null && controllerEntry.isIsConnected()) {
                String [] controllerTarget = controllerEntry.getTarget().getValue().split(":");
                IpAddress bridgeControllerIpAddress = null;
                PortNumber bridgeControllerPortNumber = null;
                for (String targetElement : controllerTarget) {
                    if (InetAddresses.isInetAddress(targetElement)) {
                        bridgeControllerIpAddress = new IpAddress(targetElement.toCharArray());
                        continue;
                    }
                    if (NumberUtils.isNumber(targetElement)) {
                        bridgeControllerPortNumber = new PortNumber(
                                Integer.valueOf(String.valueOf(targetElement)));
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
                } catch (Exception e) {
                    LOG.warn("Error getting local ip address {}", e);
                }
            }
        }
    }
    private InstanceIdentifier<Node> getInstanceIdentifier(Bridge bridge) {
        return SouthboundMapper.createInstanceIdentifier(getOvsdbConnectionInstance(),
                bridge);
    }

    private NodeId getNodeId(Bridge bridge) {
        NodeKey nodeKey = getInstanceIdentifier(bridge).firstKeyOf(Node.class, NodeKey.class);
        return nodeKey.getNodeId();
    }
}
