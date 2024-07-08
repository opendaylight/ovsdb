/*
 * Copyright (c) 2016 Intel  Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import java.util.Collection;
import java.util.Map;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.Operations;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.schema.openvswitch.Queue;
import org.opendaylight.ovsdb.southbound.InstanceIdentifierCodec;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.Queues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.QueuesKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueueRemovedCommand extends AbstractTransactCommand {
    private static final Logger LOG = LoggerFactory.getLogger(QueueRemovedCommand.class);

    public QueueRemovedCommand(final Operations op) {
        super(op);
    }

    @Override
    public void execute(final TransactionBuilder transaction, final BridgeOperationalState state,
            final DataChangeEvent events, final InstanceIdentifierCodec instanceIdentifierCodec) {
        execute(transaction, state, TransactUtils.extractOriginal(events, OvsdbNodeAugmentation.class),
                TransactUtils.extractUpdated(events, OvsdbNodeAugmentation.class));
    }

    @Override
    public void execute(final TransactionBuilder transaction, final BridgeOperationalState state,
            final Collection<DataTreeModification<Node>> modifications,
            final InstanceIdentifierCodec instanceIdentifierCodec) {
        execute(transaction, state, TransactUtils.extractOriginal(modifications, OvsdbNodeAugmentation.class),
                TransactUtils.extractUpdated(modifications, OvsdbNodeAugmentation.class));
    }

    private void execute(final TransactionBuilder transaction, final BridgeOperationalState state,
            final Map<InstanceIdentifier<OvsdbNodeAugmentation>, OvsdbNodeAugmentation> originals,
            final Map<InstanceIdentifier<OvsdbNodeAugmentation>, OvsdbNodeAugmentation> updated) {
        for (Map.Entry<InstanceIdentifier<OvsdbNodeAugmentation>, OvsdbNodeAugmentation> originalEntry : originals
                .entrySet()) {
            InstanceIdentifier<OvsdbNodeAugmentation> ovsdbNodeIid = originalEntry.getKey();
            OvsdbNodeAugmentation original = originalEntry.getValue();
            OvsdbNodeAugmentation update = updated.get(ovsdbNodeIid);

            if (original != null && update != null) {
                Map<QueuesKey, Queues> origQueues = original.getQueues();
                Map<QueuesKey, Queues> updatedQueues = update.getQueues();
                if (origQueues != null && !origQueues.isEmpty()) {
                    for (Queues origQueue : origQueues.values()) {
                        OvsdbNodeAugmentation operNode =
                                state.getBridgeNode(ovsdbNodeIid).orElseThrow().augmentation(
                                        OvsdbNodeAugmentation.class);
                        if (updatedQueues == null || !updatedQueues.containsKey(origQueue.key())) {
                            LOG.debug("Received request to delete Queue entry {}", origQueue.getQueueId());
                            Uuid queueUuid = getQueueUuid(operNode.getQueues(), origQueue.key());
                            if (queueUuid != null) {
                                Queue queue = transaction.getTypedRowSchema(Queue.class);
                                transaction.add(op.delete(queue.getSchema())
                                        .where(queue.getUuidColumn().getSchema().opEqual(
                                                new UUID(queueUuid.getValue())))
                                        .build());
                                LOG.info("Deleted queue Uuid : {}  for the  Ovsdb Node  : {}", queueUuid, operNode);
                            } else {
                                LOG.warn("Unable to delete Queue{} for node {} because it was not found in the "
                                        + "operational store, and thus we cannot retrieve its UUID",
                                        ovsdbNodeIid, origQueue.getQueueId());
                            }
                        }
                    }
                }
            }
        }
    }

    private static Uuid getQueueUuid(final Map<QueuesKey, Queues> operQueues, final QueuesKey queueId) {
        if (operQueues != null) {
            Queues queueEntry = operQueues.get(queueId);
            if (queueEntry != null) {
                return queueEntry.getQueueUuid();
            }
        }
        return null;
    }
}
