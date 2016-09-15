/*
 * Copyright (c) 2016 Intel  Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.Queue;
import org.opendaylight.ovsdb.southbound.InstanceIdentifierCodec;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.Queues;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueueRemovedCommand implements TransactCommand {
    private static final Logger LOG = LoggerFactory.getLogger(QueueRemovedCommand.class);

    @Override
    public void execute(TransactionBuilder transaction, BridgeOperationalState state,
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> events,
            InstanceIdentifierCodec instanceIdentifierCodec) {
        execute(transaction, state, TransactUtils.extractOriginal(events, OvsdbNodeAugmentation.class),
                TransactUtils.extractUpdated(events, OvsdbNodeAugmentation.class));
    }

    @Override
    public void execute(TransactionBuilder transaction, BridgeOperationalState state,
            Collection<DataTreeModification<Node>> modifications, InstanceIdentifierCodec instanceIdentifierCodec) {
        execute(transaction, state, TransactUtils.extractOriginal(modifications, OvsdbNodeAugmentation.class),
                TransactUtils.extractUpdated(modifications, OvsdbNodeAugmentation.class));
    }

    private void execute(TransactionBuilder transaction, BridgeOperationalState state,
                         Map<InstanceIdentifier<OvsdbNodeAugmentation>, OvsdbNodeAugmentation> originals,
                         Map<InstanceIdentifier<OvsdbNodeAugmentation>, OvsdbNodeAugmentation> updated) {
        for (Map.Entry<InstanceIdentifier<OvsdbNodeAugmentation>, OvsdbNodeAugmentation> originalEntry : originals
                .entrySet()) {
            InstanceIdentifier<OvsdbNodeAugmentation> ovsdbNodeIid = originalEntry.getKey();
            OvsdbNodeAugmentation original = originalEntry.getValue();
            OvsdbNodeAugmentation update = updated.get(ovsdbNodeIid);

            if (original != null && update != null) {
                List<Queues> origQueues = original.getQueues();
                List<Queues> updatedQueues = update.getQueues();
                if (origQueues != null && !origQueues.isEmpty()) {
                    for (Queues origQueue : origQueues) {
                        OvsdbNodeAugmentation operNode =
                                state.getBridgeNode(ovsdbNodeIid).get().getAugmentation(
                                        OvsdbNodeAugmentation.class);
                        List<Queues> operQueues = operNode.getQueues();

                        boolean found = false;
                        if (updatedQueues != null && !updatedQueues.isEmpty()) {
                            for (Queues updatedQueue : updatedQueues) {
                                if (origQueue.getQueueId().equals(updatedQueue.getQueueId())) {
                                    found = true;
                                    break;
                                }
                            }
                        }
                        if (!found) {
                            LOG.debug("Received request to delete Queue entry {}", origQueue.getQueueId());
                            Uuid queueUuid = getQueueUuid(operQueues, origQueue.getQueueId());
                            if (queueUuid != null) {
                                Queue queue =
                                        TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), Queue.class,
                                                null);
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

    private Uuid getQueueUuid(List<Queues> operQueues, Uri queueId) {
        if (operQueues != null && !operQueues.isEmpty()) {
            for (Queues queueEntry : operQueues) {
                if (queueEntry.getQueueId().equals(queueId)) {
                    return queueEntry.getQueueUuid();
                }
            }
        }
        return null;
    }

}
