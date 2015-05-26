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
import java.util.NoSuchElementException;
import java.util.Set;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.lib.error.SchemaVersionMismatchException;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.OpenVSwitch;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionInstance;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.DatapathTypeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.DatapathTypeEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.InterfaceTypeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.InterfaceTypeEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchExternalIdsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchOtherConfigsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenVSwitchUpdateCommand extends AbstractTransactionCommand {

    private static final Logger LOG = LoggerFactory
            .getLogger(OpenVSwitchUpdateCommand.class);

    public OpenVSwitchUpdateCommand(OvsdbConnectionInstance key, TableUpdates updates,
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
            final InstanceIdentifier<Node> nodePath = SouthboundMapper.createInstanceIdentifier(getConnectionInfo());

            OvsdbNodeAugmentationBuilder ovsdbNodeBuilder = new OvsdbNodeAugmentationBuilder();

            setVersion(ovsdbNodeBuilder, openVSwitch);
            setDataPathTypes(ovsdbNodeBuilder, openVSwitch);
            setInterfaceTypes(ovsdbNodeBuilder, openVSwitch);
            setExternalIds(ovsdbNodeBuilder, openVSwitch);
            setOtherConfig(ovsdbNodeBuilder, openVSwitch);
            ovsdbNodeBuilder.setConnectionInfo(getConnectionInfo());

            NodeBuilder nodeBuilder = new NodeBuilder();
            ConnectionInfo connectionInfo = getConnectionInfo();
            nodeBuilder.setNodeId(SouthboundMapper.createNodeId(
                    connectionInfo.getRemoteIp(), connectionInfo.getRemotePort()));
            nodeBuilder.addAugmentation(OvsdbNodeAugmentation.class,
                    ovsdbNodeBuilder.build());
            transaction.merge(LogicalDatastoreType.OPERATIONAL, nodePath,
                    nodeBuilder.build());
        }
    }

    private void setOtherConfig(OvsdbNodeAugmentationBuilder ovsdbNodeBuilder,
            OpenVSwitch openVSwitch) {
        Map<String, String> otherConfigs = openVSwitch.getOtherConfigColumn().getData();
        if (otherConfigs != null && !otherConfigs.isEmpty()) {
            Set<String> otherConfigKeys = otherConfigs.keySet();
            List<OpenvswitchOtherConfigs> otherConfigsList = new ArrayList<OpenvswitchOtherConfigs>();
            String otherConfigValue;
            for (String otherConfigKey : otherConfigKeys) {
                otherConfigValue = otherConfigs.get(otherConfigKey);
                if (otherConfigKey != null && otherConfigValue != null) {
                    otherConfigsList.add(new OpenvswitchOtherConfigsBuilder()
                            .setOtherConfigKey(otherConfigKey)
                            .setOtherConfigValue(otherConfigValue)
                            .build());
                }
            }
            ovsdbNodeBuilder.setOpenvswitchOtherConfigs(otherConfigsList);
        }
    }

    private void setExternalIds(OvsdbNodeAugmentationBuilder ovsdbNodeBuilder,
            OpenVSwitch openVSwitch) {
        Map<String, String> externalIds = openVSwitch.getExternalIdsColumn().getData();
        if (externalIds != null && !externalIds.isEmpty()) {
            Set<String> externalIdKeys = externalIds.keySet();
            List<OpenvswitchExternalIds> externalIdsList = new ArrayList<OpenvswitchExternalIds>();
            String externalIdValue;
            for (String externalIdKey : externalIdKeys) {
                externalIdValue = externalIds.get(externalIdKey);
                if (externalIdKey != null && externalIdValue != null) {
                    externalIdsList.add(new OpenvswitchExternalIdsBuilder()
                            .setExternalIdKey(externalIdKey)
                            .setExternalIdValue(externalIdValue)
                            .build());
                }
            }
            ovsdbNodeBuilder.setOpenvswitchExternalIds(externalIdsList);
        }
    }

    private void setInterfaceTypes(
            OvsdbNodeAugmentationBuilder ovsdbNodeBuilder,
            OpenVSwitch openVSwitch) {
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
    }

    private void setDataPathTypes(
            OvsdbNodeAugmentationBuilder ovsdbNodeBuilder,
            OpenVSwitch openVSwitch) {
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
            LOG.debug("Datapath types not supported by this version of ovsdb",e);
        }
    }

    private void setVersion(OvsdbNodeAugmentationBuilder ovsdbNodeBuilder,
            OpenVSwitch openVSwitch) {
        try {
            ovsdbNodeBuilder.setOvsVersion(openVSwitch.getOvsVersionColumn().getData().iterator().next());
        } catch (NoSuchElementException e) {
            LOG.debug("ovs_version is not set for this switch",e);
        }
    }
}
