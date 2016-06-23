/*
 * Copyright (c) 2016 Inocybe Technologies and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.southbound.transactions.md;

import com.google.common.base.Optional;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsdbAutoAttachUpdateCommand extends AbstractTransactionCommand {
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbAutoAttachUpdateCommand.class);

    private Map<UUID, AutoAttach> updatedAutoAttachRows;
    private Map<UUID, AutoAttach> oldAutoAttachRows;

    public OvsdbAutoAttachUpdateCommand(OvsdbConnectionInstance key,
            TableUpdates updates, DatabaseSchema dbSchema) {
        super(key, updates, dbSchema);
        updatedAutoAttachRows = TyperUtils.extractRowsUpdated(AutoAttach.class, getUpdates(), getDbSchema());
        oldAutoAttachRows = TyperUtils.extractRowsOld(AutoAttach.class, getUpdates(), getDbSchema());
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        if (updatedAutoAttachRows != null && !updatedAutoAttachRows.isEmpty()) {
            updateAutoAttach(transaction, updatedAutoAttachRows);
        }
    }

    private void updateAutoAttach(ReadWriteTransaction transaction,
            Map<UUID, AutoAttach> updatedAutoAttachRows) {

        final InstanceIdentifier<Node> nodeIId = getOvsdbConnectionInstance().getInstanceIdentifier();
        final Optional<Node> ovsdbNode = SouthboundUtil.readNode(transaction, nodeIId);
        if (ovsdbNode.isPresent()) {
            for (final Entry<UUID, AutoAttach> entry : updatedAutoAttachRows.entrySet()) {
                final AutoAttach autoAttach = entry.getValue();
                final AutoAttach oldAutoAttach = oldAutoAttachRows.get(entry.getKey());
                final Uri uri =
                        new Uri(SouthboundConstants.AUTOATTACH_URI_PREFIX + "://" + autoAttach.getUuid().toString());

                // FIXME: To be uncommented when Open vSwitch supports external_ids column
//                Uri uri = new Uri(getAutoAttachId(autoAttach));

                Autoattach currentAutoattach = null;
                try {
                    final InstanceIdentifier<Autoattach> currentIid = nodeIId
                            .augmentation(OvsdbNodeAugmentation.class)
                            .child(Autoattach.class, new AutoattachKey(new Uri(oldAutoAttach
                                    .getUuidColumn().getData().toString())));
                    // FIXME: To be uncommented and replaced to currentIid when
                    // Open vSwitch supports external_ids column
//                    InstanceIdentifier<Autoattach> currentIid = nodeIId
//                            .augmentation(OvsdbNodeAugmentation.class)
//                            .child(Autoattach.class, new AutoattachKey(new Uri(oldAutoAttach
//                                    .getExternalIdsColumn().getData()
//                                    .get(SouthboundConstants.AUTOATTACH_ID_EXTERNAL_ID_KEY))));
                    final Optional<Autoattach> optionalAutoattach =
                            transaction.read(LogicalDatastoreType.OPERATIONAL, currentIid).checkedGet();
                    if (optionalAutoattach.isPresent()) {
                        currentAutoattach = optionalAutoattach.get();
                    }
                } catch (final Exception e) {
                    LOG.debug("AutoAttach table entries not found in operational datastore, need to create it.", e);
                }

                final AutoattachBuilder autoAttachBuilder =
                        (currentAutoattach != null) ? new AutoattachBuilder(currentAutoattach)
                                : new AutoattachBuilder()
                                .setAutoattachUuid(new Uuid(entry.getKey().toString()))
                                .setAutoattachId(uri)
                                .setKey(new AutoattachKey(uri));

                if (autoAttach.getSystemNameColumn() != null
                        && autoAttach.getSystemNameColumn().getData() != null
                        && !autoAttach.getSystemNameColumn().getData().isEmpty()) {
                    autoAttachBuilder.setSystemName(autoAttach.getSystemNameColumn().getData());
                }
                if (autoAttach.getSystemDescriptionColumn() != null
                        && autoAttach.getSystemDescriptionColumn().getData() != null
                        && !autoAttach.getSystemDescriptionColumn().getData().isEmpty()) {
                    autoAttachBuilder.setSystemDescription(autoAttach.getSystemDescriptionColumn().getData());
                }
                if (autoAttach.getMappingsColumn() != null
                        && autoAttach.getMappingsColumn().getData() != null
                        && !autoAttach.getMappingsColumn().getData().isEmpty()) {
                    setMappings(autoAttachBuilder, autoAttach);
                }
                // FIXME: To be uncommented when Open vSwitch supports external_ids column
//                setExternalIds(autoAttachBuilder, autoAttach);

                final Autoattach autoAttachEntry = autoAttachBuilder.build();
                LOG.trace("Update Ovsdb Node {} with AutoAttach table entries {}",
                        ovsdbNode.get().getNodeId(), autoAttachEntry);
                final InstanceIdentifier<Autoattach> iid = nodeIId
                        .augmentation(OvsdbNodeAugmentation.class)
                        .child(Autoattach.class, autoAttachEntry.getKey());
                transaction.put(LogicalDatastoreType.OPERATIONAL,
                        iid, autoAttachEntry);
            }
        }
    }

    private void setMappings(AutoattachBuilder autoAttachBuilder,
            AutoAttach autoAttach) {
        final Map<Long, Long> mappings = autoAttach.getMappingsColumn().getData();
        final Set<Long> mappingsKeys = mappings.keySet();
        final List<Mappings> mappingsList = new ArrayList<>();
        for (final Long mappingsKey : mappingsKeys) {
            final Integer mappingsValue = new Integer(mappings.get(mappingsKey).toString());
            if (mappingsKey != null) {
                mappingsList.add(new MappingsBuilder()
                        .setKey(new MappingsKey(mappingsKey))
                        .setMappingsKey(mappingsKey)
                        .setMappingsValue(mappingsValue)
                        .build());
            }
            autoAttachBuilder.setMappings(mappingsList);
        }
    }

    // FIXME: To be uncommented when Open vSwitch supports external_ids column
//    private String getAutoAttachId(AutoAttach autoAttach) {
//        if (autoAttach.getExternalIdsColumn() != null
//                && autoAttach.getExternalIdsColumn().getData() != null
//                && autoAttach.getExternalIdsColumn().getData()
//                        .containsKey(SouthboundConstants.AUTOATTACH_ID_EXTERNAL_ID_KEY)) {
//            return autoAttach.getExternalIdsColumn().getData().get(SouthboundConstants.AUTOATTACH_ID_EXTERNAL_ID_KEY);
//        } else {
//            return SouthboundConstants.AUTOATTACH_URI_PREFIX + "://" + autoAttach.getUuid().toString();
//        }
//    }
//
//    private void setExternalIds(AutoattachBuilder autoAttachBuilder,
//            AutoAttach autoAttach) {
//        List<AutoattachExternalIds> externalIdsList = new ArrayList<>();
//        if (autoAttach.getExternalIdsColumn() != null
//                && autoAttach.getExternalIdsColumn().getData() != null
//                && !autoAttach.getExternalIdsColumn().getData().isEmpty()) {
//            Map<String, String> externalIds = autoAttach.getExternalIdsColumn().getData();
//            Set<String> externalIdsKeys = externalIds.keySet();
//
//            String extIdValue;
//            for (String extIdKey : externalIdsKeys) {
//                extIdValue = externalIds.get(extIdKey);
//                if (extIdKey != null && extIdValue != null) {
//                    externalIdsList.add(new AutoattachExternalIdsBuilder().setAutoattachExternalIdKey(extIdKey)
//                            .setAutoattachExternalIdValue(extIdValue).build());
//                }
//            }
//        } else {
//            externalIdsList.add(new AutoattachExternalIdsBuilder()
//                    .setAutoattachExternalIdKey(SouthboundConstants.AUTOATTACH_ID_EXTERNAL_ID_KEY)
//                    .setAutoattachExternalIdValue(autoAttach.getUuid().toString()).build());
//        }
//        autoAttachBuilder.setAutoattachExternalIds(externalIdsList);
//    }
}
