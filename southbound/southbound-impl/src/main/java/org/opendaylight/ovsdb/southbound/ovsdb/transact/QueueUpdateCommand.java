/*
 * Copyright (c) 2016 Intel Corporation and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.Mutate;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.Qos;
import org.opendaylight.ovsdb.schema.openvswitch.Queue;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.QosEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.Queues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.queues.QueuesExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.queues.QueuesOtherConfig;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

public class QueueUpdateCommand extends AbstractTransactCommand {
    private static final Logger LOG = LoggerFactory.getLogger(QueueUpdateCommand.class);


    public QueueUpdateCommand(BridgeOperationalState state, AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        super(state, changes);
    }

    @Override
    public void execute(TransactionBuilder transaction) {
        Map<InstanceIdentifier<OvsdbNodeAugmentation>, OvsdbNodeAugmentation> created =
                TransactUtils.extractCreated(getChanges(),OvsdbNodeAugmentation.class);
        for (Entry<InstanceIdentifier<OvsdbNodeAugmentation>, OvsdbNodeAugmentation> ovsdbNodeEntry:
            created.entrySet()) {
            updateQueue(transaction,  ovsdbNodeEntry.getKey(), ovsdbNodeEntry.getValue());
        }
        Map<InstanceIdentifier<OvsdbNodeAugmentation>, OvsdbNodeAugmentation> updated =
                TransactUtils.extractUpdated(getChanges(),OvsdbNodeAugmentation.class);
        for (Entry<InstanceIdentifier<OvsdbNodeAugmentation>, OvsdbNodeAugmentation> ovsdbNodeEntry:
            updated.entrySet()) {
            updateQueue(transaction,  ovsdbNodeEntry.getKey(), ovsdbNodeEntry.getValue());
        }
    }

    private void updateQueue(
            TransactionBuilder transaction,
            InstanceIdentifier<OvsdbNodeAugmentation> iid, OvsdbNodeAugmentation ovsdbNode) {

        List<Queues> queueList = ovsdbNode.getQueues();

        if (!getOperationalState().getBridgeNode(iid).isPresent()) {
            return;
        }
        OvsdbNodeAugmentation operNode = getOperationalState().getBridgeNode(iid).get().getAugmentation(OvsdbNodeAugmentation.class);
        List<Queues> operQueues = operNode.getQueues();

        if (queueList != null) {
            for (Queues queueEntry : queueList) {
                Queue queue = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), Queue.class);

                if (queueEntry.getDscp() != null) {
                    try {
                        Set<Long> dscpSet = new HashSet<>();
                            if (dscpSet.add(new Long(queueEntry.getDscp().toString()))) {
                            queue.setDscp(dscpSet);
                        }
                    } catch (NumberFormatException e) {
                        LOG.warn("Invalid DSCP {} setting for Queue {}", queueEntry.getDscp(), queueEntry, e);
                    }
                }

                Uuid queueUuid = getQueueEntryUuid(operQueues, queueEntry.getQueueId());
                UUID uuid = null;
                if (queueUuid != null) {
                    uuid = new UUID(queueUuid.getValue());
                }

                List<QueuesExternalIds> externalIds = queueEntry.getQueuesExternalIds();
                Map<String, String> externalIdsMap = new HashMap<>();
                if (externalIds != null) {
                    for (QueuesExternalIds externalId : externalIds) {
                        externalIdsMap.put(externalId.getQueuesExternalIdKey(), externalId.getQueuesExternalIdValue());
                    }
                }
                externalIdsMap.put(SouthboundConstants.QUEUE_ID_EXTERNAL_ID_KEY, queueEntry.getQueueId().getValue());
                try {
                    queue.setExternalIds(ImmutableMap.copyOf(externalIdsMap));
                } catch (NullPointerException e) {
                    LOG.warn("Incomplete Queue external IDs");
                }

                List<QueuesOtherConfig> otherConfigs = queueEntry.getQueuesOtherConfig();
                if (otherConfigs != null) {
                    Map<String, String> otherConfigsMap = new HashMap<>();
                    for (QueuesOtherConfig otherConfig : otherConfigs) {
                        otherConfigsMap.put(otherConfig.getQueueOtherConfigKey(), otherConfig.getQueueOtherConfigValue());
                    }
                    try {
                        queue.setOtherConfig(ImmutableMap.copyOf(otherConfigsMap));
                    } catch (NullPointerException e) {
                        LOG.warn("Incomplete Queue other_config", e);
                    }
                }
                if (uuid == null) {
                    transaction.add(op.insert(queue)).build();
                } else {
                    transaction.add(op.update(queue)).build();
                    Queue extraQueue = TyperUtils.getTypedRowWrapper(
                            transaction.getDatabaseSchema(), Queue.class, null);
                    extraQueue.getUuidColumn().setData(uuid);
                    transaction.add(op.update(queue.getSchema())
                            .where(extraQueue.getUuidColumn().getSchema().opEqual(uuid)).build());
                }
                transaction.build();
            }
        }
    }

    private Uuid getQueueEntryUuid(List<Queues> operQueues, Uri queueId) {
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
