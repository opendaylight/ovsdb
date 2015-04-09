package org.opendaylight.ovsdb.southbound.transactions.md;

import java.util.ArrayList;
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

    public OvsdbBridgeUpdateCommand(OvsdbClientKey key, TableUpdates updates,
            DatabaseSchema dbSchema) {
        super(key,updates,dbSchema);
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        Map<UUID,Bridge> updatedBridgeRows = TyperUtils.extractRowsUpdated(Bridge.class, getUpdates(), getDbSchema());
        Map<UUID,Controller> updatedControllerRows = TyperUtils.extractRowsUpdated(Controller.class,
                getUpdates(), getDbSchema());
        for (Entry<UUID, Bridge> entry : updatedBridgeRows.entrySet()) {
            Bridge bridge = entry.getValue();
            final InstanceIdentifier<Node> nodePath = getKey().toInstanceIndentifier();
            Optional<Node> node = Optional.absent();
            try {
                node = transaction.read(LogicalDatastoreType.OPERATIONAL, nodePath).checkedGet();
            } catch (final ReadFailedException e) {
                LOG.debug("Read Operational/DS for Node fail! {}", nodePath, e);
            }
            if (node.isPresent()) {
                LOG.debug("Node {} is present",node);
                NodeBuilder managedNodeBuilder = new NodeBuilder();
                InstanceIdentifier<Node> managedNodePath = SouthboundMapper.createInstanceIdentifier(getKey(),bridge);
                NodeId manageNodeId = SouthboundMapper.createManagedNodeId(managedNodePath);
                managedNodeBuilder.setNodeId(manageNodeId);
                OvsdbBridgeAugmentationBuilder ovsdbManagedNodeBuilder = new OvsdbBridgeAugmentationBuilder();
                ovsdbManagedNodeBuilder.setBridgeName(new OvsdbBridgeName(bridge.getName()));
                ovsdbManagedNodeBuilder.setBridgeUuid(new Uuid(bridge.getUuid().toString()));
                DatapathId dpid = SouthboundMapper.createDatapathId(bridge);
                if (dpid != null) {
                    ovsdbManagedNodeBuilder.setDatapathId(dpid);
                }
                ovsdbManagedNodeBuilder.setDatapathType(
                        SouthboundMapper.createDatapathType(bridge.getDatapathTypeColumn().getData()));
                if (SouthboundMapper.createMdsalProtocols(bridge) != null
                        && SouthboundMapper.createMdsalProtocols(bridge).size() > 0) {
                    ovsdbManagedNodeBuilder.setProtocolEntry(SouthboundMapper.createMdsalProtocols(bridge));
                }

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
                    ovsdbManagedNodeBuilder.setBridgeExternalIds(externalIdsList);
                }

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
                    ovsdbManagedNodeBuilder.setBridgeOtherConfigs(otherConfigList);
                }

                if (!SouthboundMapper.createControllerEntries(bridge, updatedControllerRows).isEmpty()) {
                    ovsdbManagedNodeBuilder.setControllerEntry(
                            SouthboundMapper.createControllerEntries(bridge, updatedControllerRows));
                }

                if (bridge.getFailModeColumn() != null
                        && bridge.getFailModeColumn().getData() != null
                        && !bridge.getFailModeColumn().getData().isEmpty()) {
                    String[] failmodeArray = new String[bridge.getFailModeColumn().getData().size()];
                    bridge.getFailModeColumn().getData().toArray(failmodeArray);
                    ovsdbManagedNodeBuilder.setFailMode(
                            SouthboundConstants.OVSDB_FAIL_MODE_MAP.inverse().get(failmodeArray[0]));
                }
                ovsdbManagedNodeBuilder.setManagedBy(new OvsdbNodeRef(nodePath));
                managedNodeBuilder.addAugmentation(OvsdbBridgeAugmentation.class, ovsdbManagedNodeBuilder.build());

                LOG.debug("Store managed node augmentation data {}",ovsdbManagedNodeBuilder.toString());
                transaction.merge(LogicalDatastoreType.OPERATIONAL, managedNodePath, managedNodeBuilder.build());

                //Update node with managed node reference
                NodeBuilder nodeBuilder = new NodeBuilder();
                nodeBuilder.setNodeId(SouthboundMapper.createNodeId(getKey().getIp(),getKey().getPort()));

                OvsdbNodeAugmentationBuilder ovsdbNodeBuilder = new OvsdbNodeAugmentationBuilder();
                List<ManagedNodeEntry> managedNodes = new ArrayList<ManagedNodeEntry>();
                ManagedNodeEntry managedNodeEntry = new ManagedNodeEntryBuilder().setBridgeRef(
                        new OvsdbBridgeRef(managedNodePath)).build();
                managedNodes.add(managedNodeEntry);
                ovsdbNodeBuilder.setManagedNodeEntry(managedNodes);

                nodeBuilder.addAugmentation(OvsdbNodeAugmentation.class, ovsdbNodeBuilder.build());

                LOG.debug("Update node with managed node ref {}",ovsdbNodeBuilder.toString());
                transaction.merge(LogicalDatastoreType.OPERATIONAL, nodePath, nodeBuilder.build());

            }
        }
    }
}
