/*
 * Copyright (c) 2016 Inocybe Technologies and others. All rights reserved.
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
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.AutoAttach;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionInstance;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.ovsdb.southbound.SouthboundUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.Autoattach;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.AutoattachBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.AutoattachKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.autoattach.Mappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.autoattach.MappingsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.autoattach.MappingsKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class OvsdbAutoAttachUpdateCommand extends AbstractTransactionCommand {
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbAutoAttachUpdateCommand.class);

    private Map<UUID, AutoAttach> updatedAutoAttachRows;
    private Map<UUID, AutoAttach> oldAutoAttachRows;

    public OvsdbAutoAttachUpdateCommand(OvsdbConnectionInstance key,
            TableUpdates updates, DatabaseSchema dbSchema) {
        super(key, updates, dbSchema);
        updatedAutoAttachRows = TyperUtils.extractRowsUpdated(AutoAttach.class,getUpdates(), getDbSchema());
        oldAutoAttachRows = TyperUtils.extractRowsOld(AutoAttach.class, getUpdates(), getDbSchema());
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        if (updatedAutoAttachRows != null && !updatedAutoAttachRows.isEmpty()) {
            LOG.debug("In OvsdbAutoAttachUpdateCommand");
            updateAutoAttach(transaction, updatedAutoAttachRows);
        }
    }

    private void updateAutoAttach(ReadWriteTransaction transaction,
            Map<UUID, AutoAttach> updatedAutoAttachRows) {

        final InstanceIdentifier<Node> nodeIId = getOvsdbConnectionInstance().getInstanceIdentifier();
        final Optional<Node> ovsdbNode = SouthboundUtil.readNode(transaction, nodeIId);
        if (ovsdbNode.isPresent()) {
            for (Entry<UUID, AutoAttach> entry : updatedAutoAttachRows.entrySet()) {
                AutoAttach autoAttach = entry.getValue();
                AutoAttach oldAutoAttach = oldAutoAttachRows.get(entry.getKey());
                Uri uri = new Uri(SouthboundConstants.AUTOATTACH_URI_PREFIX + "://" + autoAttach.getUuid().toString());

                AutoattachBuilder autoAttachBuilder = new AutoattachBuilder()
                        .setAutoattachUuid(new Uuid(entry.getKey().toString()))
                        .setAutoattachId(uri)
                        .setKey(new AutoattachKey(uri));
                if (autoAttach.getSystemNameColumn() != null) {
                    String sysName = autoAttach.getSystemNameColumn().getData();
                    if (sysName != null) {
                        autoAttachBuilder.setSystemName(sysName);
                    }
                }
                if (autoAttach.getSystemDescriptionColumn() != null) {
                    String sysDescription = autoAttach.getSystemDescriptionColumn().getData();
                    if (sysDescription != null) {
                        autoAttachBuilder.setSystemDescription(sysDescription);
                    }
                }
                setMappings(transaction, autoAttachBuilder, oldAutoAttach, autoAttach, nodeIId);

                Autoattach autoAttachEntry = autoAttachBuilder.build();
                LOG.debug("Update Ovsdb Node {} with AutoAttach table entries {}",
                        ovsdbNode.get().getNodeId(), autoAttachEntry);
                InstanceIdentifier<Autoattach> iid = nodeIId
                        .augmentation(OvsdbNodeAugmentation.class)
                        .child(Autoattach.class, autoAttachEntry.getKey());
                transaction.merge(LogicalDatastoreType.OPERATIONAL,
                        iid, autoAttachEntry);
            }
        }
    }

    private void setMappings(ReadWriteTransaction transaction,
            AutoattachBuilder autoAttachBuilder,
            AutoAttach oldAutoAttach,
            AutoAttach autoAttach,
            InstanceIdentifier<Node> nodeIId) {

        Map<Long, Long> mappings = null;
        Map<Long, Long> oldMappings = null;
        if (oldAutoAttach != null && oldAutoAttach.getMappingsColumn() != null) {
            oldMappings = oldAutoAttach.getMappingsColumn().getData();
            if (oldMappings != null && !oldMappings.isEmpty()) {
                removeOldMappings(transaction, autoAttachBuilder, oldMappings, autoAttach, nodeIId);
            }
        }
        if (autoAttach.getMappingsColumn() != null) {
            mappings = autoAttach.getMappingsColumn().getData();
            if (mappings != null && !mappings.isEmpty()) {
                setNewMappings(autoAttachBuilder, mappings);
            }
        }
    }

    private void removeOldMappings(ReadWriteTransaction transaction,
            AutoattachBuilder autoAttachBuilder,
            Map<Long, Long> oldMappings,
            AutoAttach autoAttach,
            InstanceIdentifier<Node> nodeIId) {
        InstanceIdentifier<Autoattach> autoAttachIId = nodeIId
                .augmentation(OvsdbNodeAugmentation.class)
                .child(Autoattach.class, autoAttachBuilder.build().getKey());
        Set<Long> mappingsKeys = oldMappings.keySet();
        for (Long mappingsKey : mappingsKeys) {
            KeyedInstanceIdentifier<Mappings, MappingsKey> mappingIId =
                    autoAttachIId
                    .child(Mappings.class, new MappingsKey(mappingsKey));
            transaction.delete(LogicalDatastoreType.OPERATIONAL, mappingIId);
        }
    }

    private void setNewMappings(AutoattachBuilder autoAttachBuilder,
            Map<Long, Long> mappings) {
        Set<Long> mappingsKeys = mappings.keySet();
        List<Mappings> mappingsList = new ArrayList<>();
        for (Long mappingsKey : mappingsKeys) {
            Integer mappingsValue = new Integer(mappings.get(mappingsKey).toString());
            if (mappingsKey != null && mappingsValue != null) {
                mappingsList.add(new MappingsBuilder()
                        .setKey(new MappingsKey(mappingsKey))
                        .setMappingsKey(mappingsKey)
                        .setMappingsValue(mappingsValue)
                        .build());
            }
        }
        autoAttachBuilder.setMappings(mappingsList);
    }
}
