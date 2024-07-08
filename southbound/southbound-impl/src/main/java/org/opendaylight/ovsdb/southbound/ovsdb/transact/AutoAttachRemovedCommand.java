/*
 * Copyright © 2016, 2017 Inocybe Technologies and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.Operations;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.schema.openvswitch.AutoAttach;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.southbound.InstanceIdentifierCodec;
import org.opendaylight.ovsdb.southbound.SouthboundUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.Autoattach;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.AutoattachKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagedNodeEntry;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressFBWarnings("UPM_UNCALLED_PRIVATE_METHOD")
public class AutoAttachRemovedCommand extends AbstractTransactCommand {
    private static final Logger LOG = LoggerFactory.getLogger(AutoAttachRemovedCommand.class);

    public AutoAttachRemovedCommand(final Operations op) {
        super(op);
    }

    @Override
    public void execute(final TransactionBuilder transaction, final BridgeOperationalState state,
            final Collection<DataTreeModification<Node>> modifications,
            final InstanceIdentifierCodec instanceIdentifierCodec) {
        execute(transaction, state, TransactUtils.extractOriginal(modifications, OvsdbNodeAugmentation.class),
                TransactUtils.extractUpdated(modifications, OvsdbNodeAugmentation.class));
    }

    @Override
    public void execute(final TransactionBuilder transaction, final BridgeOperationalState state,
            final DataChangeEvent events, final InstanceIdentifierCodec instanceIdentifierCodec) {
        execute(transaction, state, TransactUtils.extractOriginal(events, OvsdbNodeAugmentation.class),
                TransactUtils.extractUpdated(events, OvsdbNodeAugmentation.class));
    }

    private void execute(final TransactionBuilder transaction, final BridgeOperationalState state,
            final Map<InstanceIdentifier<OvsdbNodeAugmentation>, OvsdbNodeAugmentation> original,
            final Map<InstanceIdentifier<OvsdbNodeAugmentation>, OvsdbNodeAugmentation> updated) {
        for (var originalEntry : original.entrySet()) {
            final InstanceIdentifier<OvsdbNodeAugmentation> ovsdbNodeIid = originalEntry.getKey();
            final OvsdbNodeAugmentation ovsdbNodeAugmentation = originalEntry.getValue();
            final OvsdbNodeAugmentation deletedOvsdbNodeAugmentation = updated.get(ovsdbNodeIid);

            if (ovsdbNodeAugmentation != null && deletedOvsdbNodeAugmentation != null) {
                final Map<AutoattachKey, Autoattach> origAutoattachList = ovsdbNodeAugmentation.getAutoattach();
                final Map<AutoattachKey, Autoattach> deletedAutoattachList =
                        deletedOvsdbNodeAugmentation.getAutoattach();
                if (origAutoattachList != null && !origAutoattachList.isEmpty()
                        && (deletedAutoattachList == null || deletedAutoattachList.isEmpty())) {

                    if (true) {
                        // FIXME: Remove if loop after ovs community supports external_ids column in AutoAttach Table
                        LOG.info("UNSUPPORTED FUNCTIONALITY: Auto Attach related CRUD operations are not supported for"
                                + " this version of OVSDB schema due to missing external_ids column.");
                        return;
                    }
                    final OvsdbNodeAugmentation currentOvsdbNode =
                            state.getBridgeNode(ovsdbNodeIid).orElseThrow().augmentation(OvsdbNodeAugmentation.class);
                    final Map<AutoattachKey, Autoattach> currentAutoAttach = currentOvsdbNode.getAutoattach();
                    for (final Autoattach origAutoattach : origAutoattachList.values()) {
                        deleteAutoAttach(state, transaction, ovsdbNodeIid,
                            getAutoAttachUuid(currentAutoAttach, origAutoattach.key()));
                    }
                }
            }
        }
    }

    private void deleteAutoAttach(final BridgeOperationalState state, final TransactionBuilder transaction,
            final InstanceIdentifier<OvsdbNodeAugmentation> ovsdbNodeIid, final Uuid autoattachUuid) {

        LOG.debug("Received request to delete Autoattach entry {}", autoattachUuid);
        final OvsdbBridgeAugmentation bridgeAugmentation = getBridge(state, ovsdbNodeIid, autoattachUuid);
        if (autoattachUuid != null && bridgeAugmentation != null) {
            final UUID uuid = new UUID(autoattachUuid.getValue());
            final AutoAttach autoattach = transaction.getTypedRowSchema(AutoAttach.class);
            transaction.add(op.delete(autoattach.getSchema())
                    .where(autoattach.getUuidColumn().getSchema().opEqual(uuid))
                    .build());
            transaction.add(op.comment("AutoAttach: Deleting {} " + uuid
                    + " attached to " + bridgeAugmentation.getBridgeName().getValue()));

            final Bridge bridge = transaction.getTypedRowSchema(Bridge.class);
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

    private static Uuid getAutoAttachUuid(final Map<AutoattachKey, Autoattach> currentAutoAttach,
            final AutoattachKey autoAttachId) {
        if (currentAutoAttach != null) {
            final Autoattach autoAttach = currentAutoAttach.get(autoAttachId);
            if (autoAttach != null) {
                return autoAttach.getAutoattachUuid();
            }
        }
        return null;
    }

    private static OvsdbBridgeAugmentation getBridge(final BridgeOperationalState state,
            final InstanceIdentifier<OvsdbNodeAugmentation> key, final Uuid aaUuid) {
        if (aaUuid == null) {
            return null;
        }
        try (ReadTransaction transaction = state.dataBroker().newReadOnlyTransaction()) {
            final var nodeOptional = SouthboundUtil.readNode(transaction, key.firstIdentifierOf(Node.class));
            if (nodeOptional.isPresent()) {
                final var managedNodes = nodeOptional.orElseThrow()
                    .augmentation(OvsdbNodeAugmentation.class).getManagedNodeEntry();
                for (final ManagedNodeEntry managedNode : managedNodes.values()) {
                    final OvsdbBridgeRef ovsdbBridgeRef = managedNode.getBridgeRef();
                    final InstanceIdentifier<OvsdbBridgeAugmentation> brIid = ovsdbBridgeRef.getValue()
                            .firstIdentifierOf(Node.class).augmentation(OvsdbBridgeAugmentation.class);
                    final Optional<OvsdbBridgeAugmentation> optionalBridge =
                            transaction.read(LogicalDatastoreType.OPERATIONAL, brIid).get();
                    OvsdbBridgeAugmentation bridge = optionalBridge.orElseThrow();
                    if (bridge != null && bridge.getAutoAttach() != null
                            && bridge.getAutoAttach().equals(aaUuid)) {
                        return bridge;
                    }
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Error reading from datastore", e);
        }
        return null;
    }
}
