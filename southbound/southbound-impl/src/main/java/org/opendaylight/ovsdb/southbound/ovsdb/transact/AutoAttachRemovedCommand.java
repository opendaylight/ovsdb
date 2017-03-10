/*
 * Copyright © 2016, 2017 Inocybe Technologies and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import com.google.common.base.Optional;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
import org.opendaylight.ovsdb.southbound.InstanceIdentifierCodec;
import org.opendaylight.ovsdb.southbound.SouthboundProvider;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.Autoattach;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagedNodeEntry;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoAttachRemovedCommand implements TransactCommand {
    private static final Logger LOG = LoggerFactory.getLogger(AutoAttachRemovedCommand.class);

    @Override
    public void execute(TransactionBuilder transaction, BridgeOperationalState state,
            Collection<DataTreeModification<Node>> modifications, InstanceIdentifierCodec instanceIdentifierCodec) {
        execute(transaction, state, TransactUtils.extractOriginal(modifications, OvsdbNodeAugmentation.class),
                TransactUtils.extractUpdated(modifications, OvsdbNodeAugmentation.class));
    }

    @Override
    public void execute(TransactionBuilder transaction, BridgeOperationalState state,
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> events,
            InstanceIdentifierCodec instanceIdentifierCodec) {
        execute(transaction, state, TransactUtils.extractOriginal(events, OvsdbNodeAugmentation.class),
                TransactUtils.extractUpdated(events, OvsdbNodeAugmentation.class));
    }

    private void execute(TransactionBuilder transaction, BridgeOperationalState state,
                         Map<InstanceIdentifier<OvsdbNodeAugmentation>, OvsdbNodeAugmentation> original,
                         Map<InstanceIdentifier<OvsdbNodeAugmentation>, OvsdbNodeAugmentation> updated) {

        for (final Map.Entry<InstanceIdentifier<OvsdbNodeAugmentation>, OvsdbNodeAugmentation> originalEntry :
                original.entrySet()) {
            final InstanceIdentifier<OvsdbNodeAugmentation> ovsdbNodeIid = originalEntry.getKey();
            final OvsdbNodeAugmentation ovsdbNodeAugmentation = originalEntry.getValue();
            final OvsdbNodeAugmentation deletedOvsdbNodeAugmentation = updated.get(ovsdbNodeIid);

            if (ovsdbNodeAugmentation != null && deletedOvsdbNodeAugmentation != null) {
                final List<Autoattach> origAutoattachList = ovsdbNodeAugmentation.getAutoattach();
                final List<Autoattach> deletedAutoattachList = deletedOvsdbNodeAugmentation.getAutoattach();
                if (origAutoattachList != null && !origAutoattachList.isEmpty()
                        && (deletedAutoattachList == null || deletedAutoattachList.isEmpty())) {

                    if (true) {
                        // FIXME: Remove if loop after ovs community supports external_ids column in AutoAttach Table
                        LOG.info("UNSUPPORTED FUNCTIONALITY: Auto Attach related CRUD operations are not supported for"
                                + " this version of OVSDB schema due to missing external_ids column.");
                        return;
                    }
                    final OvsdbNodeAugmentation currentOvsdbNode =
                            state.getBridgeNode(ovsdbNodeIid).get().getAugmentation(OvsdbNodeAugmentation.class);
                    final List<Autoattach> currentAutoAttach = currentOvsdbNode.getAutoattach();
                    for (final Autoattach origAutoattach : origAutoattachList) {
                        final Uri autoAttachId = origAutoattach.getAutoattachId();
                        deleteAutoAttach(transaction, ovsdbNodeIid, getAutoAttachUuid(currentAutoAttach, autoAttachId));
                    }
                }
            }
        }
    }

    private void deleteAutoAttach(TransactionBuilder transaction,
            InstanceIdentifier<OvsdbNodeAugmentation> ovsdbNodeIid,
            Uuid autoattachUuid) {

        LOG.debug("Received request to delete Autoattach entry {}", autoattachUuid);
        final OvsdbBridgeAugmentation bridgeAugmentation = getBridge(ovsdbNodeIid, autoattachUuid);
        if (autoattachUuid != null && bridgeAugmentation != null) {
            final UUID uuid = new UUID(autoattachUuid.getValue());
            final AutoAttach autoattach =
                    TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), AutoAttach.class, null);
            transaction.add(op.delete(autoattach.getSchema())
                    .where(autoattach.getUuidColumn().getSchema().opEqual(uuid))
                    .build());
            transaction.add(op.comment("AutoAttach: Deleting {} " + uuid
                    + " attached to " + bridgeAugmentation.getBridgeName().getValue()));

            final Bridge bridge = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(),
                    Bridge.class,null);

            transaction.add(op.mutate(bridge.getSchema())
                    .addMutation(bridge.getAutoAttachColumn().getSchema(),
                            Mutator.DELETE, Collections.singleton(uuid))
                    .where(bridge.getNameColumn().getSchema()
                            .opEqual(bridgeAugmentation.getBridgeName().getValue())).build());

            transaction.add(op.comment("Bridge: Mutating " + bridgeAugmentation.getBridgeName().getValue()
                    + " to remove autoattach column " + uuid));
        } else {
            LOG.debug("Unable to delete AutoAttach {} for node {} because it was not found in the operational store, "
                    + "and thus we cannot retrieve its UUID", autoattachUuid, ovsdbNodeIid);
        }
    }

    private Uuid getAutoAttachUuid(List<Autoattach> currentAutoAttach, Uri autoAttachId) {
        if (currentAutoAttach != null && !currentAutoAttach.isEmpty()) {
            for (final Autoattach autoAttach : currentAutoAttach) {
                if (autoAttach.getAutoattachId().equals(autoAttachId)) {
                    return autoAttach.getAutoattachUuid();
                }
            }
        }
        return null;
    }

    private OvsdbBridgeAugmentation getBridge(InstanceIdentifier<OvsdbNodeAugmentation> key, Uuid aaUuid) {
        if (aaUuid == null) {
            return null;
        }
        OvsdbBridgeAugmentation bridge = null;
        final InstanceIdentifier<Node> nodeIid = key.firstIdentifierOf(Node.class);
        try (ReadOnlyTransaction transaction = SouthboundProvider.getDb().newReadOnlyTransaction()) {
            final Optional<Node> nodeOptional = transaction.read(LogicalDatastoreType.OPERATIONAL, nodeIid).get();
            if (nodeOptional.isPresent()) {
                final List<ManagedNodeEntry> managedNodes =
                        nodeOptional.get().getAugmentation(OvsdbNodeAugmentation.class).getManagedNodeEntry();
                for (final ManagedNodeEntry managedNode : managedNodes) {
                    final OvsdbBridgeRef ovsdbBridgeRef = managedNode.getBridgeRef();
                    final InstanceIdentifier<OvsdbBridgeAugmentation> brIid = ovsdbBridgeRef.getValue()
                            .firstIdentifierOf(Node.class).augmentation(OvsdbBridgeAugmentation.class);
                    final Optional<OvsdbBridgeAugmentation> optionalBridge =
                            transaction.read(LogicalDatastoreType.OPERATIONAL, brIid).get();
                    bridge = optionalBridge.orNull();
                    if (bridge != null && bridge.getAutoAttach() != null
                            && bridge.getAutoAttach().equals(aaUuid)) {
                        return bridge;
                    }
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Error reading from datastore",e);
        }
        return null;
    }
}
