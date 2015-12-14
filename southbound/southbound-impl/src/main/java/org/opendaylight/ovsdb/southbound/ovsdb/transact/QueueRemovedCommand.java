/*
 * Copyright (c) 2016 Intel  Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.Queue;
import org.opendaylight.ovsdb.schema.openvswitch.OpenVSwitch;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.Queues;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;

public class QueueRemovedCommand extends AbstractTransactCommand {
    private static final Logger LOG = LoggerFactory.getLogger(QueueRemovedCommand.class);

    public QueueRemovedCommand(BridgeOperationalState state,
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        super(state, changes);
    }

    @Override
    public void execute(TransactionBuilder transaction) {
        Set<InstanceIdentifier<Queues>> removed =
                TransactUtils.extractRemoved(getChanges(),Queues.class);

        Map<InstanceIdentifier<OvsdbNodeAugmentation>, OvsdbNodeAugmentation> originals
            = TransactUtils.extractOriginal(getChanges(),OvsdbNodeAugmentation.class);

        Map<InstanceIdentifier<OvsdbNodeAugmentation>, OvsdbNodeAugmentation> updated
        = TransactUtils.extractUpdated(getChanges(), OvsdbNodeAugmentation.class);

        Iterator<InstanceIdentifier<OvsdbNodeAugmentation>> itr = originals.keySet().iterator();
        while (itr.hasNext()) {
            InstanceIdentifier<OvsdbNodeAugmentation> ovsdbNodeIid = itr.next();
            OvsdbNodeAugmentation original = originals.get(ovsdbNodeIid);
            OvsdbNodeAugmentation update = updated.get(ovsdbNodeIid);

            if (original != null && update != null) {
                List<Queues> origQueues = original.getQueues();
                List<Queues> updatedQueues = update.getQueues();
                if (origQueues != null && !origQueues.isEmpty()) {
                    for (Queues origQueue : origQueues) {
                        OvsdbNodeAugmentation operNode = getOperationalState().getBridgeNode(ovsdbNodeIid).get().getAugmentation(OvsdbNodeAugmentation.class);
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
                            LOG.info("Received request to delete Queue entry {}",origQueue.getQueueId());
                            Uuid queueUuid = getQueueUuid(operQueues, origQueue.getQueueId());
                            if (queueUuid != null) {
                                Queue queue = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), Queue.class, null);
                                  transaction.add(op.delete(queue.getSchema())
                                          .where(queue.getUuidColumn().getSchema().opEqual(new UUID(queueUuid.getValue())))
                                          .build());
                            } else {
                                LOG.warn("Unable to delete Queue{} for node {} because it was not found in the operational store, "
                                        + "and thus we cannot retrieve its UUID", ovsdbNodeIid, origQueue.getQueueId());
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
