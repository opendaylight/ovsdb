/*
 * Copyright (c) 2015 Inocybe inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.transactions.md;

import static org.opendaylight.ovsdb.southbound.SouthboundUtil.schemaMismatchLog;

import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.ovsdb.lib.error.SchemaVersionMismatchException;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.OpenVSwitch;
import org.opendaylight.ovsdb.southbound.InstanceIdentifierCodec;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionInstance;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.DatapathTypeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.DatapathTypeEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.InterfaceTypeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.InterfaceTypeEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchExternalIdsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchExternalIdsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchOtherConfigsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchOtherConfigsKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenVSwitchUpdateCommand extends AbstractTransactionCommand {

    private static final Logger LOG = LoggerFactory.getLogger(OpenVSwitchUpdateCommand.class);

    private final InstanceIdentifierCodec instanceIdentifierCodec;

    public OpenVSwitchUpdateCommand(InstanceIdentifierCodec instanceIdentifierCodec, OvsdbConnectionInstance key,
            TableUpdates updates, DatabaseSchema dbSchema) {
        super(key, updates, dbSchema);
        this.instanceIdentifierCodec = instanceIdentifierCodec;
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        Map<UUID, OpenVSwitch> updatedOpenVSwitchRows = TyperUtils
                .extractRowsUpdated(OpenVSwitch.class, getUpdates(),
                        getDbSchema());
        Map<UUID, OpenVSwitch> deletedOpenVSwitchRows = TyperUtils
                .extractRowsOld(OpenVSwitch.class, getUpdates(),
                        getDbSchema());

        for (Entry<UUID, OpenVSwitch> entry : updatedOpenVSwitchRows.entrySet()) {
            OpenVSwitch openVSwitch = entry.getValue();
            final InstanceIdentifier<Node> nodePath = getInstanceIdentifier(openVSwitch);

            OvsdbNodeAugmentationBuilder ovsdbNodeBuilder = new OvsdbNodeAugmentationBuilder();

            setDbVersion(ovsdbNodeBuilder, openVSwitch);
            setOvsVersion(ovsdbNodeBuilder, openVSwitch);
            setDataPathTypes(ovsdbNodeBuilder, openVSwitch);
            setInterfaceTypes(ovsdbNodeBuilder, openVSwitch);
            OpenVSwitch oldEntry = deletedOpenVSwitchRows.get(entry.getKey());
            setExternalIds(transaction, ovsdbNodeBuilder, oldEntry, openVSwitch);
            setOtherConfig(transaction, ovsdbNodeBuilder, oldEntry, openVSwitch);
            ovsdbNodeBuilder.setConnectionInfo(getConnectionInfo());

            transaction.merge(LogicalDatastoreType.OPERATIONAL, nodePath, new NodeBuilder()
                .setNodeId(getNodeId(openVSwitch))
                .addAugmentation(ovsdbNodeBuilder.build())
                .build());
        }
    }

    private void setOtherConfig(ReadWriteTransaction transaction,
            OvsdbNodeAugmentationBuilder ovsdbNodeBuilder, OpenVSwitch oldEntry, OpenVSwitch openVSwitch) {
        Map<String, String> oldOtherConfigs = null;
        Map<String, String> otherConfigs = null;

        if (openVSwitch.getOtherConfigColumn() != null) {
            otherConfigs = openVSwitch.getOtherConfigColumn().getData();
        }
        if (oldEntry != null && oldEntry.getOtherConfigColumn() != null) {
            oldOtherConfigs = oldEntry.getOtherConfigColumn().getData();
        }

        if (oldOtherConfigs != null && !oldOtherConfigs.isEmpty()) {
            removeOldConfigs(transaction, oldOtherConfigs, openVSwitch);
        }
        if (otherConfigs != null && !otherConfigs.isEmpty()) {
            setNewOtherConfigs(ovsdbNodeBuilder, otherConfigs);
        }
    }

    @VisibleForTesting
    void removeOldConfigs(ReadWriteTransaction transaction, Map<String, String> oldOtherConfigs, OpenVSwitch ovs) {
        InstanceIdentifier<OvsdbNodeAugmentation> nodeAugmentataionIid = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class, new NodeKey(getNodeId(ovs)))
                .augmentation(OvsdbNodeAugmentation.class);
        Set<String> otherConfigKeys = oldOtherConfigs.keySet();
        for (String otherConfigKey : otherConfigKeys) {
            KeyedInstanceIdentifier<OpenvswitchOtherConfigs, OpenvswitchOtherConfigsKey> externalIid =
                    nodeAugmentataionIid
                    .child(OpenvswitchOtherConfigs.class, new OpenvswitchOtherConfigsKey(otherConfigKey));
            transaction.delete(LogicalDatastoreType.OPERATIONAL, externalIid);
        }
    }

    @VisibleForTesting
    void setNewOtherConfigs(OvsdbNodeAugmentationBuilder ovsdbNodeBuilder, Map<String, String> otherConfigs) {
        List<OpenvswitchOtherConfigs> otherConfigsList = new ArrayList<>();
        for (Entry<String, String> entry : otherConfigs.entrySet()) {
            String otherConfigKey = entry.getKey();
            String otherConfigValue = entry.getValue();
            if (otherConfigKey != null && otherConfigValue != null) {
                otherConfigsList.add(new OpenvswitchOtherConfigsBuilder().setOtherConfigKey(otherConfigKey)
                        .setOtherConfigValue(otherConfigValue).build());
            }
        }
        ovsdbNodeBuilder.setOpenvswitchOtherConfigs(otherConfigsList);
    }

    private void setExternalIds(ReadWriteTransaction transaction,
            OvsdbNodeAugmentationBuilder ovsdbNodeBuilder, OpenVSwitch oldEntry, OpenVSwitch openVSwitch) {
        Map<String, String> oldExternalIds = null;
        Map<String, String> externalIds = null;

        if (openVSwitch.getExternalIdsColumn() != null) {
            externalIds = openVSwitch.getExternalIdsColumn().getData();
        }
        if (oldEntry != null && oldEntry.getExternalIdsColumn() != null) {
            oldExternalIds = oldEntry.getExternalIdsColumn().getData();
        }
        if (oldExternalIds == null || oldExternalIds.isEmpty()) {
            setNewExternalIds(ovsdbNodeBuilder, externalIds);
        } else if (externalIds != null && !externalIds.isEmpty()) {
            removeExternalIds(transaction, oldExternalIds, openVSwitch);
            setNewExternalIds(ovsdbNodeBuilder, externalIds);
        }
    }

    @VisibleForTesting
    void removeExternalIds(ReadWriteTransaction transaction, Map<String, String> oldExternalIds, OpenVSwitch ovs) {
        InstanceIdentifier<OvsdbNodeAugmentation> nodeAugmentataionIid = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class, new NodeKey(getNodeId(ovs)))
                .augmentation(OvsdbNodeAugmentation.class);
        Set<String> externalIdKeys = oldExternalIds.keySet();
        for (String externalIdKey : externalIdKeys) {
            KeyedInstanceIdentifier<OpenvswitchExternalIds, OpenvswitchExternalIdsKey> externalIid =
                    nodeAugmentataionIid
                    .child(OpenvswitchExternalIds.class, new OpenvswitchExternalIdsKey(externalIdKey));
            transaction.delete(LogicalDatastoreType.OPERATIONAL, externalIid);
        }
    }

    @VisibleForTesting
    void setNewExternalIds(OvsdbNodeAugmentationBuilder ovsdbNodeBuilder, Map<String, String> externalIds) {
        List<OpenvswitchExternalIds> externalIdsList = new ArrayList<>();
        for (Entry<String, String> entry : externalIds.entrySet()) {
            String externalIdKey = entry.getKey();
            String externalIdValue = entry.getValue();
            if (externalIdKey != null && externalIdValue != null) {
                externalIdsList.add(new OpenvswitchExternalIdsBuilder().setExternalIdKey(externalIdKey)
                        .setExternalIdValue(externalIdValue).build());
            }
        }
        ovsdbNodeBuilder.setOpenvswitchExternalIds(externalIdsList);
    }

    private static void setInterfaceTypes(
            OvsdbNodeAugmentationBuilder ovsdbNodeBuilder,
            OpenVSwitch openVSwitch) {
        try {
            Set<String> iftypes = openVSwitch.getIfaceTypesColumn().getData();
            List<InterfaceTypeEntry> ifEntryList = new ArrayList<>();
            for (String ifType : iftypes) {
                if (SouthboundMapper.createInterfaceType(ifType) != null) {
                    InterfaceTypeEntry ifEntry = new InterfaceTypeEntryBuilder()
                            .setInterfaceType(
                                    SouthboundMapper.createInterfaceType(ifType))
                            .build();
                    ifEntryList.add(ifEntry);
                } else {
                    LOG.warn("Interface type {} not present in model", ifType);
                }
            }
            ovsdbNodeBuilder.setInterfaceTypeEntry(ifEntryList);
        } catch (SchemaVersionMismatchException e) {
            schemaMismatchLog("iface_types", SouthboundConstants.OPEN_V_SWITCH, e);
        }
    }

    private static void setDataPathTypes(OvsdbNodeAugmentationBuilder ovsdbNodeBuilder, OpenVSwitch openVSwitch) {
        try {
            Set<String> dptypes = openVSwitch.getDatapathTypesColumn()
                    .getData();
            List<DatapathTypeEntry> dpEntryList = new ArrayList<>();
            for (String dpType : dptypes) {
                if (SouthboundMapper.createDatapathType(dpType) != null) {
                    DatapathTypeEntry dpEntry = new DatapathTypeEntryBuilder()
                            .setDatapathType(
                                    SouthboundMapper.createDatapathType(dpType))
                            .build();
                    dpEntryList.add(dpEntry);
                } else {
                    LOG.warn("Datapath type {} not present in model", dpType);
                }
            }
            ovsdbNodeBuilder.setDatapathTypeEntry(dpEntryList);
        } catch (SchemaVersionMismatchException e) {
            schemaMismatchLog("datapath_types", SouthboundConstants.OPEN_V_SWITCH, e);
        }
    }

    private static void setOvsVersion(OvsdbNodeAugmentationBuilder ovsdbNodeBuilder, OpenVSwitch openVSwitch) {
        try {
            ovsdbNodeBuilder.setOvsVersion(openVSwitch.getOvsVersionColumn().getData().iterator().next());
        } catch (NoSuchElementException e) {
            LOG.debug("ovs_version is not set for this switch",e);
        }
    }

    private static void setDbVersion(OvsdbNodeAugmentationBuilder ovsdbNodeBuilder, OpenVSwitch openVSwitch) {
        try {
            ovsdbNodeBuilder.setDbVersion(openVSwitch.getDbVersionColumn().getData().iterator().next());
        } catch (NoSuchElementException e) {
            LOG.debug("db_version is not set for this switch",e);
        }
    }

    private InstanceIdentifier<Node> getInstanceIdentifier(OpenVSwitch ovs) {
        if (ovs.getExternalIdsColumn() != null
                && ovs.getExternalIdsColumn().getData() != null
                && ovs.getExternalIdsColumn().getData().containsKey(SouthboundConstants.IID_EXTERNAL_ID_KEY)) {
            String iidString = ovs.getExternalIdsColumn().getData().get(SouthboundConstants.IID_EXTERNAL_ID_KEY);
            InstanceIdentifier<Node> iid =
                    (InstanceIdentifier<Node>) instanceIdentifierCodec.bindingDeserializerOrNull(iidString);
            getOvsdbConnectionInstance().setInstanceIdentifier(iid);
        } else {
            String nodeString = SouthboundConstants.OVSDB_URI_PREFIX + "://" + SouthboundConstants.UUID + "/"
                    + ovs.getUuid().toString();
            NodeId nodeId = new NodeId(new Uri(nodeString));
            NodeKey nodeKey = new NodeKey(nodeId);
            InstanceIdentifier<Node> iid = InstanceIdentifier.builder(NetworkTopology.class)
                    .child(Topology.class,new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                    .child(Node.class,nodeKey)
                    .build();
            getOvsdbConnectionInstance().setInstanceIdentifier(iid);
        }
        return getOvsdbConnectionInstance().getInstanceIdentifier();
    }

    @VisibleForTesting
    NodeId getNodeId(OpenVSwitch ovs) {
        NodeKey nodeKey = getInstanceIdentifier(ovs).firstKeyOf(Node.class);
        return nodeKey.getNodeId();
    }
}
