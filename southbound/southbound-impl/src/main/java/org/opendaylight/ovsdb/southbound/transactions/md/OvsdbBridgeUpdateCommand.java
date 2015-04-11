package org.opendaylight.ovsdb.southbound.transactions.md;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.Controller;
import org.opendaylight.ovsdb.southbound.OvsdbClientKey;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.BridgeExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.BridgeExternalIdsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.BridgeOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.BridgeOtherConfigsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagedNodeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagedNodeEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class OvsdbBridgeUpdateCommand extends AbstractTransactionCommand {
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbBridgeUpdateCommand.class);
    private Map<UUID,Controller> updatedControllerRows;
    private Map<UUID,Bridge> updatedBridgeRows;

    public OvsdbBridgeUpdateCommand(OvsdbClientKey key, TableUpdates updates,
            DatabaseSchema dbSchema) {
        super(key,updates,dbSchema);
        updatedBridgeRows = TyperUtils.extractRowsUpdated(Bridge.class, getUpdates(), getDbSchema());
        updatedControllerRows = TyperUtils.extractRowsUpdated(Controller.class,
                getUpdates(), getDbSchema());
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        for (Entry<UUID, Bridge> entry : updatedBridgeRows.entrySet()) {
            updateBridge(transaction, entry.getValue());
        }
    }

    private void updateBridge(ReadWriteTransaction transaction,
            Bridge bridge) {
        final InstanceIdentifier<Node> connectionIId = getKey().toInstanceIndentifier();
        Optional<Node> connection = readNode(transaction, getKey().toInstanceIndentifier());
        if (connection.isPresent()) {
            LOG.debug("Connection {} is present",connection);

            // Update the connection node to let it know it manages this bridge
            Node connectionNode = buildConnectionNode(bridge);
            transaction.merge(LogicalDatastoreType.OPERATIONAL, connectionIId, connectionNode);

            // Update the bridge node with whatever data we are getting
            InstanceIdentifier<Node> bridgeIid = SouthboundMapper.createInstanceIdentifier(getKey(),bridge);
            Optional<Node> oldBridgeNode = readNode(transaction,bridgeIid);
            Node updatedBridgeNode = buildBridgeNode(bridge);
            transaction.merge(LogicalDatastoreType.OPERATIONAL, bridgeIid, updatedBridgeNode);
            deleteControllers(transaction, controllerEntriesToRemove(bridgeIid,bridge,oldBridgeNode));
        }
    }

    private void deleteControllers(ReadWriteTransaction transaction,
            List<InstanceIdentifier<ControllerEntry>> controllerEntryIids) {
        for (InstanceIdentifier<ControllerEntry> controllerEntryIid: controllerEntryIids) {
            transaction.delete(LogicalDatastoreType.OPERATIONAL, controllerEntryIid);
        }
    }

    private List<InstanceIdentifier<ControllerEntry>> controllerEntriesToRemove(
            InstanceIdentifier<Node> bridgeIid, Bridge bridge, Optional<Node> oldBridgeNode) {
        List<InstanceIdentifier<ControllerEntry>> result =
                new ArrayList<InstanceIdentifier<ControllerEntry>>();
        if (oldBridgeNode.isPresent()) {
            Map<UUID,ControllerEntryKey> oldControllerEntryKeys = extractControllerKeys(oldBridgeNode.get());
            for (Entry<UUID,ControllerEntryKey> entry: oldControllerEntryKeys.entrySet()) {
                if (bridge != null
                        && bridge.getControllerColumn() != null
                        && bridge.getControllerColumn().getData() != null
                        && !bridge.getControllerColumn().getData().contains(entry.getKey())) {
                    result.add(bridgeIid
                            .augmentation(OvsdbBridgeAugmentation.class)
                            .child(ControllerEntry.class,entry.getValue()));
                }
            }
        }
        return result;
    }

    private Map<UUID,ControllerEntryKey> extractControllerKeys(Node bridgeNode) {
        Map<UUID,ControllerEntryKey> result = new HashMap<UUID,ControllerEntryKey>();
        if (bridgeNode != null && bridgeNode.getAugmentation(OvsdbBridgeAugmentation.class) != null) {
            OvsdbBridgeAugmentation bridgeAugmentation = bridgeNode.getAugmentation(OvsdbBridgeAugmentation.class);
            if (bridgeAugmentation.getControllerEntry() != null) {
                for (ControllerEntry controller: bridgeAugmentation.getControllerEntry()) {
                    result.put(new UUID(controller.getControllerUuid().getValue()),controller.getKey());
                }
            }
        }
        return result;
    }

    private Optional<Node> readNode(ReadWriteTransaction transaction,
            final InstanceIdentifier<Node> connectionIid) {
        Optional<Node> node = Optional.absent();
        try {
            node = transaction.read(LogicalDatastoreType.OPERATIONAL, connectionIid).checkedGet();
        } catch (final ReadFailedException e) {
            LOG.debug("Read Operational/DS for Node fail! {}", connectionIid, e);
        }
        return node;
    }

    private Node buildConnectionNode(
            Bridge bridge) {
        //Update node with managed node reference
        NodeBuilder connectionNode = new NodeBuilder();
        connectionNode.setNodeId(SouthboundMapper.createNodeId(getKey().getIp(),getKey().getPort()));

        OvsdbNodeAugmentationBuilder ovsdbConnectionAugmentationBuilder = new OvsdbNodeAugmentationBuilder();
        List<ManagedNodeEntry> managedBridges = new ArrayList<ManagedNodeEntry>();
        InstanceIdentifier<Node> bridgeIid = SouthboundMapper.createInstanceIdentifier(getKey(),bridge);
        ManagedNodeEntry managedBridge = new ManagedNodeEntryBuilder().setBridgeRef(
                new OvsdbBridgeRef(bridgeIid)).build();
        managedBridges.add(managedBridge);
        ovsdbConnectionAugmentationBuilder.setManagedNodeEntry(managedBridges);

        connectionNode.addAugmentation(OvsdbNodeAugmentation.class, ovsdbConnectionAugmentationBuilder.build());

        LOG.debug("Update node with bridge node ref {}",ovsdbConnectionAugmentationBuilder.toString());
        return connectionNode.build();
    }

    private Node buildBridgeNode(Bridge bridge) {
        NodeBuilder bridgeNodeBuilder = new NodeBuilder();
        InstanceIdentifier<Node> bridgeIid = SouthboundMapper.createInstanceIdentifier(getKey(),bridge);
        NodeId bridgeNodeId = SouthboundMapper.createManagedNodeId(bridgeIid);
        bridgeNodeBuilder.setNodeId(bridgeNodeId);
        OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder = new OvsdbBridgeAugmentationBuilder();
        ovsdbBridgeAugmentationBuilder.setBridgeName(new OvsdbBridgeName(bridge.getName()));
        ovsdbBridgeAugmentationBuilder.setBridgeUuid(new Uuid(bridge.getUuid().toString()));
        setDataPath(ovsdbBridgeAugmentationBuilder, bridge);
        setDataPathType(ovsdbBridgeAugmentationBuilder, bridge);
        setProtocol(ovsdbBridgeAugmentationBuilder, bridge);
        setExternalIds(ovsdbBridgeAugmentationBuilder, bridge);
        setOtherConfig(ovsdbBridgeAugmentationBuilder, bridge);
        setController(ovsdbBridgeAugmentationBuilder, bridge);
        setFailMode(ovsdbBridgeAugmentationBuilder, bridge);
        setManagedBy(ovsdbBridgeAugmentationBuilder);
        bridgeNodeBuilder.addAugmentation(OvsdbBridgeAugmentation.class, ovsdbBridgeAugmentationBuilder.build());

        LOG.debug("Built with the intent to store bridge data {}",
                ovsdbBridgeAugmentationBuilder.toString());
        return bridgeNodeBuilder.build();
    }

    private void setManagedBy(OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder) {
        InstanceIdentifier<Node> connectionNodePath = getKey().toInstanceIndentifier();
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

    private void setController(OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder,Bridge bridge) {
        if (!SouthboundMapper.createControllerEntries(bridge, updatedControllerRows).isEmpty()) {
            ovsdbBridgeAugmentationBuilder.setControllerEntry(
                    SouthboundMapper.createControllerEntries(bridge, updatedControllerRows));
        }
    }

    private void setOtherConfig(OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder,
            Bridge bridge) {
        Map<String, String> otherConfigs = bridge
                .getOtherConfigColumn().getData();
        if (otherConfigs != null && !otherConfigs.isEmpty()) {
            Set<String> otherConfigKeys = otherConfigs.keySet();
            List<BridgeOtherConfigs> otherConfigList = new ArrayList<BridgeOtherConfigs>();
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
            List<BridgeExternalIds> externalIdsList = new ArrayList<BridgeExternalIds>();
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
}
