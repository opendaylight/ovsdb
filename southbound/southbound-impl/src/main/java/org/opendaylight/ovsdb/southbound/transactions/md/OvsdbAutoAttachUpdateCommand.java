/*
 * Copyright (c) 2016 Inocybe Technologies and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.transactions.md;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.AutoAttach;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionInstance;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.ovsdb.southbound.SouthboundUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
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
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsdbAutoAttachUpdateCommand extends AbstractTransactionCommand {
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbAutoAttachUpdateCommand.class);

    private final Map<UUID, AutoAttach> updatedAutoAttachRows;
    private final Map<UUID, AutoAttach> oldAutoAttachRows;

    @SuppressFBWarnings(value = "MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR", justification = "Non-final for mocking")
    public OvsdbAutoAttachUpdateCommand(OvsdbConnectionInstance key, TableUpdates updates, DatabaseSchema dbSchema) {
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
            Map<UUID, AutoAttach> newUpdatedAutoAttachRows) {

        final InstanceIdentifier<Node> nodeIId = getOvsdbConnectionInstance().getInstanceIdentifier();
        final Optional<Node> ovsdbNode = SouthboundUtil.readNode(transaction, nodeIId);
        if (ovsdbNode.isPresent()) {
            for (final Entry<UUID, AutoAttach> entry : newUpdatedAutoAttachRows.entrySet()) {
                final AutoAttach autoAttach = entry.getValue();
                final AutoAttach oldAutoAttach = oldAutoAttachRows.get(entry.getKey());
                final Uri uri =
                        new Uri(SouthboundConstants.AUTOATTACH_URI_PREFIX + "://" + autoAttach.getUuid().toString());

                // FIXME: To be uncommented when Open vSwitch supports external_ids column
//                Uri uri = new Uri(getAutoAttachId(autoAttach));

                Autoattach currentAutoattach = null;
                if (oldAutoAttach.getUuidColumn() != null) {
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
                                transaction.read(LogicalDatastoreType.OPERATIONAL, currentIid).get();
                        if (optionalAutoattach.isPresent()) {
                            currentAutoattach = optionalAutoattach.orElseThrow();
                        }
                    } catch (final InterruptedException | ExecutionException e) {
                        LOG.debug("AutoAttach table entries not found in operational datastore, need to create it.", e);
                    }
                }

                final AutoattachBuilder autoAttachBuilder =
                        currentAutoattach != null ? new AutoattachBuilder(currentAutoattach)
                                : new AutoattachBuilder()
                                .setAutoattachUuid(new Uuid(entry.getKey().toString()))
                                .setAutoattachId(uri)
                                .withKey(new AutoattachKey(uri));

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
                        ovsdbNode.orElseThrow().getNodeId(), autoAttachEntry);
                final InstanceIdentifier<Autoattach> iid = nodeIId
                        .augmentation(OvsdbNodeAugmentation.class)
                        .child(Autoattach.class, autoAttachEntry.key());
                transaction.put(LogicalDatastoreType.OPERATIONAL,
                        iid, autoAttachEntry);
            }
        }
    }

    private static void setMappings(AutoattachBuilder autoAttachBuilder,
            AutoAttach autoAttach) {
        final Map<MappingsKey, Mappings> mappings = new LinkedHashMap<>();
        for (final Entry<Long, Long> entry : autoAttach.getMappingsColumn().getData().entrySet()) {
            final Long mappingsKey = entry.getKey();
            if (mappingsKey != null) {
                final MappingsKey key = new MappingsKey(Uint32.valueOf(mappingsKey));
                mappings.put(key, new MappingsBuilder()
                    .withKey(key)
                    .setMappingsValue(Uint16.valueOf(entry.getValue().intValue()))
                    .build());
            }
            autoAttachBuilder.setMappings(mappings);
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
