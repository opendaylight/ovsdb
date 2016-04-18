/*
 * Copyright (c) 2016 Inocybe Technologies and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.AutoAttach;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.ovsdb.southbound.SouthboundProvider;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.Autoattach;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.autoattach.AutoattachExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.autoattach.Mappings;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;

public class AutoAttachUpdateCommand implements TransactCommand {
    private static final Logger LOG = LoggerFactory.getLogger(AutoAttachUpdateCommand.class);

    @Override
    public void execute(TransactionBuilder transaction, BridgeOperationalState state,
                        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> events) {
        execute(transaction, state, TransactUtils.extractCreatedOrUpdated(events, OvsdbNodeAugmentation.class));
    }

    @Override
    public void execute(TransactionBuilder transaction, BridgeOperationalState state,
                        Collection<DataTreeModification<Node>> modifications) {
        execute(transaction, state, TransactUtils.extractCreatedOrUpdated(modifications, OvsdbNodeAugmentation.class));
    }

    private void execute(TransactionBuilder transaction, BridgeOperationalState state,
                         Map<InstanceIdentifier<OvsdbNodeAugmentation>, OvsdbNodeAugmentation> createdOrUpdated) {

        // FIXME: Fix if loop after ovs community supports external_ids column in AutoAttach Table
        if (true) {
            try {
                throw new UnsupportedOperationException("CRUD operations not supported from ODL for auto_attach column for"
                        + " this version of ovsdb schema due to missing external_ids column");
            } catch (UnsupportedOperationException e) {
                LOG.debug(e.getMessage());
            }
            return;
        }

        for (Entry<InstanceIdentifier<OvsdbNodeAugmentation>, OvsdbNodeAugmentation> ovsdbNodeEntry
                : createdOrUpdated.entrySet()) {
            updateAutoAttach(transaction, state, ovsdbNodeEntry.getKey(), ovsdbNodeEntry.getValue());
        }
    }

    private void updateAutoAttach(TransactionBuilder transaction, BridgeOperationalState state,
            InstanceIdentifier<OvsdbNodeAugmentation> iid,
            OvsdbNodeAugmentation ovsdbNode) {

        List<Autoattach> autoAttachList = ovsdbNode.getAutoattach();
        if (state.getBridgeNode(iid).isPresent()) {
            return;
        }
        OvsdbNodeAugmentation currentOvsdbNode = state.getBridgeNode(iid).get().getAugmentation(OvsdbNodeAugmentation.class);
        List<Autoattach> currentAutoAttach = currentOvsdbNode.getAutoattach();

        if (autoAttachList != null) {
            for (Autoattach autoAttach : autoAttachList) {
                AutoAttach autoAttachWrapper = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), AutoAttach.class);
                if (autoAttach.getSystemName() != null) {
                    autoAttachWrapper.setSystemName(autoAttach.getSystemName());
                }
                if (autoAttach.getSystemDescription() != null) {
                    autoAttachWrapper.setSystemDescription(autoAttach.getSystemDescription());
                }

                List<Mappings> mappingsList = autoAttach.getMappings();
                if (mappingsList != null && !mappingsList.isEmpty()) {
                    Map<Long, Long> newMappings = new HashMap<>();
                    for (Mappings mappings : mappingsList) {
                        Long mappingsValue = new Long(mappings.getMappingsValue().toString());
                        newMappings.put(mappings.getMappingsKey(), mappingsValue);
                    }
                    autoAttachWrapper.setMappings(newMappings);
                }

                List<AutoattachExternalIds> externalIds = autoAttach.getAutoattachExternalIds();
                Map<String, String> externalIdsMap = new HashMap<>();
                if (externalIds != null) {
                    for (AutoattachExternalIds externalId : externalIds) {
                        externalIdsMap.put(externalId.getAutoattachExternalIdKey(), externalId.getAutoattachExternalIdValue());
                    }
                }
                externalIdsMap.put(SouthboundConstants.AUTOATTACH_ID_EXTERNAL_ID_KEY, autoAttach.getAutoattachId().getValue());
                // FIXME: To be uncommented when Open vSwitch supports external_ids column
//                try {
//                    autoAttachWrapper.setExternalIds(ImmutableMap.copyOf(externalIdsMap));
//                } catch (NullPointerException e) {
//                    LOG.warn("Incomplete AutoAttach external IDs");
//                }

                Uuid aaUuid = getAutoAttachUuid(currentAutoAttach, autoAttach.getAutoattachId());
                if (aaUuid != null) {
                    UUID uuid = new UUID(aaUuid.getValue());
                    AutoAttach newAutoAttach = TyperUtils.getTypedRowWrapper(
                            transaction.getDatabaseSchema(), AutoAttach.class, null);
                    newAutoAttach.getUuidColumn().setData(uuid);
                    LOG.trace("Updating autoattach table entries {}", uuid);
                    transaction.add(op.update(autoAttachWrapper)
                            .where(newAutoAttach.getUuidColumn().getSchema().opEqual(uuid)).build());
                    transaction.add(op.comment("Updating AutoAttach table: " + uuid));
                } else {
                    Uri bridgeUri = autoAttach.getBridgeId();
                    String namedUuid = SouthboundMapper.getRandomUUID();
                    Bridge bridge = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), Bridge.class);
                    transaction.add(op.insert(autoAttachWrapper).withId(namedUuid));
                    OvsdbBridgeAugmentation ovsdbBridgeAugmentation = getBridge(iid, bridgeUri);
                    if (ovsdbBridgeAugmentation != null) {
                        bridge.setName(ovsdbBridgeAugmentation.getBridgeName().getValue());
                        bridge.setAutoAttach(Sets.newHashSet(new UUID(namedUuid)));
                        LOG.trace("Create Autoattach table {}, "
                                + "and mutate the bridge {}", autoAttach.getAutoattachId(), getBridge(iid, bridgeUri).getBridgeName().getValue());
                        transaction.add(op.mutate(bridge)
                                .addMutation(bridge.getAutoAttachColumn().getSchema(),
                                        Mutator.INSERT,bridge.getAutoAttachColumn().getData())
                                .where(bridge.getNameColumn().getSchema()
                                        .opEqual(bridge.getNameColumn().getData())).build());
                        transaction.add(op.comment("Bridge: Mutating " + ovsdbBridgeAugmentation.getBridgeName().getValue()
                                + " to add autoattach column " + namedUuid));
                    }
                }
            }
        }
    }

    private OvsdbBridgeAugmentation getBridge(InstanceIdentifier<OvsdbNodeAugmentation> key,
            Uri bridgeUri) {
        InstanceIdentifier<OvsdbBridgeAugmentation> bridgeIid = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class, new NodeKey(new NodeId(bridgeUri)))
                .augmentation(OvsdbBridgeAugmentation.class);

        OvsdbBridgeAugmentation bridge = null;
        try (ReadOnlyTransaction transaction = SouthboundProvider.getDb().newReadOnlyTransaction()) {
            Optional<OvsdbBridgeAugmentation> bridgeOptional = transaction.read(LogicalDatastoreType.OPERATIONAL, bridgeIid).get();
            if (bridgeOptional.isPresent()) {
                bridge = bridgeOptional.get();
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Error reading from datastore", e);
        }
        return bridge;
    }

    private Uuid getAutoAttachUuid(List<Autoattach> currentAutoAttach, Uri autoattachId) {
        if (currentAutoAttach != null && !currentAutoAttach.isEmpty()) {
            for (Autoattach autoAttach : currentAutoAttach) {
                if (autoAttach.getAutoattachId().equals(autoattachId)) {
                    return autoAttach.getAutoattachUuid();
                }
            }
        }
        return null;
    }
}
