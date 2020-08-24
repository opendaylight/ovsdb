/*
 * Copyright © 2016, 2017 Inocybe Technologies and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.schema.openvswitch.AutoAttach;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.southbound.InstanceIdentifierCodec;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.ovsdb.southbound.SouthboundProvider;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.Autoattach;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.AutoattachKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.autoattach.AutoattachExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.autoattach.AutoattachExternalIdsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.autoattach.Mappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.autoattach.MappingsKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressFBWarnings("UPM_UNCALLED_PRIVATE_METHOD")
public class AutoAttachUpdateCommand implements TransactCommand {
    private static final Logger LOG = LoggerFactory.getLogger(AutoAttachUpdateCommand.class);

    @Override
    public void execute(final TransactionBuilder transaction, final BridgeOperationalState state,
            final DataChangeEvent events, final InstanceIdentifierCodec instanceIdentifierCodec) {
        execute(transaction, state, TransactUtils.extractCreatedOrUpdated(events, OvsdbNodeAugmentation.class));
    }

    @Override
    public void execute(final TransactionBuilder transaction, final BridgeOperationalState state,
            final Collection<DataTreeModification<Node>> modifications,
            final InstanceIdentifierCodec instanceIdentifierCodec) {
        execute(transaction, state, TransactUtils.extractCreatedOrUpdated(modifications, OvsdbNodeAugmentation.class));
    }

    private void execute(final TransactionBuilder transaction, final BridgeOperationalState state,
                         final Map<InstanceIdentifier<OvsdbNodeAugmentation>, OvsdbNodeAugmentation> createdOrUpdated) {

        for (final Entry<InstanceIdentifier<OvsdbNodeAugmentation>, OvsdbNodeAugmentation> ovsdbNodeEntry
                : createdOrUpdated.entrySet()) {
            updateAutoAttach(transaction, state, ovsdbNodeEntry.getKey(), ovsdbNodeEntry.getValue());
        }
    }

    private static void updateAutoAttach(final TransactionBuilder transaction, final BridgeOperationalState state,
            final InstanceIdentifier<OvsdbNodeAugmentation> iid,
            final OvsdbNodeAugmentation ovsdbNode) {

        if (!state.getBridgeNode(iid).isPresent()) {
            return;
        }
        final Map<AutoattachKey, Autoattach> autoAttachList = ovsdbNode.getAutoattach();
        if (autoAttachList != null) {
            if (true) {
                // FIXME: Remove if loop after ovs community supports external_ids column in AutoAttach Table
                LOG.info("UNSUPPORTED FUNCTIONALITY: Auto Attach related CRUD operations are not supported for"
                        + " this version of OVSDB schema due to missing external_ids column.");
                return;
            }

            final OvsdbNodeAugmentation currentOvsdbNode =
                    state.getBridgeNode(iid).get().augmentation(OvsdbNodeAugmentation.class);
            final Map<AutoattachKey, Autoattach> currentAutoAttach = currentOvsdbNode.getAutoattach();
            for (final Autoattach autoAttach : autoAttachList.values()) {
                final AutoAttach autoAttachWrapper = transaction.getTypedRowWrapper(AutoAttach.class);
                if (autoAttach.getSystemName() != null) {
                    autoAttachWrapper.setSystemName(autoAttach.getSystemName());
                }
                if (autoAttach.getSystemDescription() != null) {
                    autoAttachWrapper.setSystemDescription(autoAttach.getSystemDescription());
                }

                final Map<MappingsKey, Mappings> mappingsList = autoAttach.getMappings();
                if (mappingsList != null && !mappingsList.isEmpty()) {
                    final Map<Long, Long> newMappings = new HashMap<>();
                    for (final Mappings mappings : mappingsList.values()) {
                        newMappings.put(mappings.getMappingsKey().toJava(), mappings.getMappingsValue().longValue());
                    }
                    autoAttachWrapper.setMappings(newMappings);
                }

                final Map<AutoattachExternalIdsKey, AutoattachExternalIds> externalIds =
                        autoAttach.getAutoattachExternalIds();
                final Map<String, String> externalIdsMap = new HashMap<>();
                if (externalIds != null) {
                    for (final AutoattachExternalIds externalId : externalIds.values()) {
                        externalIdsMap.put(externalId.getAutoattachExternalIdKey(),
                                externalId.getAutoattachExternalIdValue());
                    }
                }
                externalIdsMap.put(SouthboundConstants.AUTOATTACH_ID_EXTERNAL_ID_KEY,
                        autoAttach.getAutoattachId().getValue());
                // FIXME: To be uncommented when Open vSwitch supports external_ids column
//                try {
//                    autoAttachWrapper.setExternalIds(ImmutableMap.copyOf(externalIdsMap));
//                } catch (NullPointerException e) {
//                    LOG.warn("Incomplete AutoAttach external IDs");
//                }

                final Uuid aaUuid = getAutoAttachUuid(currentAutoAttach, autoAttach.key());
                if (aaUuid != null) {
                    final UUID uuid = new UUID(aaUuid.getValue());
                    final AutoAttach newAutoAttach = transaction.getTypedRowSchema(AutoAttach.class);
                    newAutoAttach.getUuidColumn().setData(uuid);
                    LOG.trace("Updating autoattach table entries {}", uuid);
                    transaction.add(op.update(autoAttachWrapper)
                            .where(newAutoAttach.getUuidColumn().getSchema().opEqual(uuid)).build());
                    transaction.add(op.comment("Updating AutoAttach table: " + uuid));
                } else {
                    final Uri bridgeUri = autoAttach.getBridgeId();
                    final String namedUuid = SouthboundMapper.getRandomUuid();
                    final Bridge bridge = transaction.getTypedRowWrapper(Bridge.class);
                    transaction.add(op.insert(autoAttachWrapper).withId(namedUuid));
                    final OvsdbBridgeAugmentation ovsdbBridgeAugmentation = getBridge(iid, bridgeUri);
                    if (ovsdbBridgeAugmentation != null) {
                        bridge.setName(ovsdbBridgeAugmentation.getBridgeName().getValue());
                        bridge.setAutoAttach(Collections.singleton(new UUID(namedUuid)));
                        LOG.trace("Create Autoattach table {}, and mutate the bridge {}",
                                autoAttach.getAutoattachId(), getBridge(iid, bridgeUri).getBridgeName().getValue());
                        transaction.add(op.mutate(bridge)
                                .addMutation(bridge.getAutoAttachColumn().getSchema(),
                                        Mutator.INSERT,bridge.getAutoAttachColumn().getData())
                                .where(bridge.getNameColumn().getSchema()
                                        .opEqual(bridge.getNameColumn().getData())).build());
                        transaction.add(
                                op.comment("Bridge: Mutating " + ovsdbBridgeAugmentation.getBridgeName().getValue()
                                + " to add autoattach column " + namedUuid));
                    }
                }
            }
        }
    }

    private static OvsdbBridgeAugmentation getBridge(final InstanceIdentifier<OvsdbNodeAugmentation> key,
            final Uri bridgeUri) {
        final InstanceIdentifier<OvsdbBridgeAugmentation> bridgeIid = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class, new NodeKey(new NodeId(bridgeUri)))
                .augmentation(OvsdbBridgeAugmentation.class);

        OvsdbBridgeAugmentation bridge = null;
        try (ReadTransaction transaction = SouthboundProvider.getDb().newReadOnlyTransaction()) {
            final Optional<OvsdbBridgeAugmentation> bridgeOptional =
                    transaction.read(LogicalDatastoreType.OPERATIONAL, bridgeIid).get();
            if (bridgeOptional.isPresent()) {
                bridge = bridgeOptional.get();
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Error reading from datastore", e);
        }
        return bridge;
    }

    private static Uuid getAutoAttachUuid(final Map<AutoattachKey, Autoattach> currentAutoAttach,
            final AutoattachKey autoattachId) {
        if (currentAutoAttach != null) {
            final Autoattach found = currentAutoAttach.get(autoattachId);
            if (found != null) {
                return found.getAutoattachUuid();
            }
        }
        return null;
    }
}
