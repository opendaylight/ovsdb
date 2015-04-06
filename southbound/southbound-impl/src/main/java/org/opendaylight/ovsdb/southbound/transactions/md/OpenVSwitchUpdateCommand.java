/*
 * Copyright (c) 2015 Inocybe inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.transactions.md;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.ovsdb.lib.error.SchemaVersionMismatchException;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.OpenVSwitch;
import org.opendaylight.ovsdb.southbound.OvsdbClientKey;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.external.ids.attributes.ExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.external.ids.attributes.ExternalIdsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.DatapathTypeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.DatapathTypeEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.InterfaceTypeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.InterfaceTypeEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.other.config.attributes.OtherConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.other.config.attributes.OtherConfigsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class OpenVSwitchUpdateCommand extends AbstractTransactionCommand {

    private static final Logger LOG = LoggerFactory
            .getLogger(OpenVSwitchUpdateCommand.class);

    public OpenVSwitchUpdateCommand(OvsdbClientKey key, TableUpdates updates,
            DatabaseSchema dbSchema) {
        super(key, updates, dbSchema);
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        Map<UUID, OpenVSwitch> updatedOpenVSwitchRows = TyperUtils
                .extractRowsUpdated(OpenVSwitch.class, getUpdates(),
                        getDbSchema());

        for (Entry<UUID, OpenVSwitch> entry : updatedOpenVSwitchRows.entrySet()) {
            OpenVSwitch openVSwitch = entry.getValue();
            final InstanceIdentifier<Node> nodePath = getKey()
                    .toInstanceIndentifier();
            Optional<Node> node = Optional.absent();
            try {
                node = transaction.read(LogicalDatastoreType.OPERATIONAL,
                        nodePath).checkedGet();
            } catch (final ReadFailedException e) {
                LOG.debug("Read Operational/DS for Node fail! {}", nodePath, e);
            }
            if (node.isPresent()) {
                LOG.debug("Node {} is present", node);
                OvsdbNodeAugmentation ovsdbNode = SouthboundMapper
                        .createOvsdbAugmentation(getKey());
                OvsdbNodeAugmentationBuilder ovsdbNodeBuilder = new OvsdbNodeAugmentationBuilder();
                ovsdbNodeBuilder.setOvsVersion(openVSwitch.getVersion()
                        .toString());
                try {
                    Set<String> dptypes = openVSwitch.getDatapathTypesColumn()
                            .getData();
                    List<DatapathTypeEntry> dpEntryList = new ArrayList<DatapathTypeEntry>();
                    for (String dpType : dptypes) {
                        DatapathTypeEntry dpEntry = new DatapathTypeEntryBuilder()
                                .setDatapathType(
                                        SouthboundMapper.createDatapathType(dpType))
                                .build();
                        dpEntryList.add(dpEntry);
                    }
                    ovsdbNodeBuilder.setDatapathTypeEntry(dpEntryList);
                } catch (SchemaVersionMismatchException e) {
                    LOG.debug("Datapath types not supported by this version of ovsdb",e);;
                }
                try {
                    Set<String> iftypes = openVSwitch.getIfaceTypesColumn().getData();
                    List<InterfaceTypeEntry> ifEntryList = new ArrayList<InterfaceTypeEntry>();
                    for (String ifType : iftypes) {
                        InterfaceTypeEntry ifEntry = new InterfaceTypeEntryBuilder()
                                .setInterfaceType(
                                        SouthboundMapper.createInterfaceType(ifType))
                                .build();
                        ifEntryList.add(ifEntry);
                    }
                    ovsdbNodeBuilder.setInterfaceTypeEntry(ifEntryList);
                } catch (SchemaVersionMismatchException e) {
                    LOG.debug("Iface types  not supported by this version of ovsdb",e);;
                }

                Map<String, String> externalIds = openVSwitch.getExternalIdsColumn().getData();
                if (externalIds != null && !externalIds.isEmpty()) {
                    Set<String> externalIdKeys = externalIds.keySet();
                    List<ExternalIds> externalIdsList = new ArrayList<ExternalIds>();
                    String externalIdValue;
                    for (String externalIdKey : externalIdKeys) {
                        externalIdValue = externalIds.get(externalIdKey);
                        if (externalIdKey != null && externalIdValue != null) {
                            externalIdsList.add(new ExternalIdsBuilder()
                                    .setExternalIdKey(externalIdKey)
                                    .setExternalIdValue(externalIdValue)
                                    .build());
                        }
                    }
                    ovsdbNodeBuilder.setExternalIds(externalIdsList);
                }

                Map<String, String> otherConfigs = openVSwitch.getOtherConfigColumn().getData();
                if (otherConfigs != null && !otherConfigs.isEmpty()) {
                    Set<String> otherConfigKeys = otherConfigs.keySet();
                    List<OtherConfigs> otherConfigsList = new ArrayList<OtherConfigs>();
                    String otherConfigValue;
                    for (String otherConfigKey : otherConfigKeys) {
                        otherConfigValue = otherConfigs.get(otherConfigKey);
                        if (otherConfigKey != null && otherConfigValue != null) {
                            otherConfigsList.add(new OtherConfigsBuilder()
                                    .setOtherConfigKey(otherConfigKey)
                                    .setOtherConfigValue(otherConfigValue)
                                    .build());
                        }
                    }
                    ovsdbNodeBuilder.setOtherConfigs(otherConfigsList);
                }

                NodeBuilder nodeBuilder = new NodeBuilder();
                nodeBuilder.setNodeId(SouthboundMapper.createNodeId(
                        ovsdbNode.getIp(), ovsdbNode.getPort()));
                nodeBuilder.addAugmentation(OvsdbNodeAugmentation.class,
                        ovsdbNodeBuilder.build());
                transaction.merge(LogicalDatastoreType.OPERATIONAL, nodePath,
                        nodeBuilder.build());
            }
        }
    }
}
