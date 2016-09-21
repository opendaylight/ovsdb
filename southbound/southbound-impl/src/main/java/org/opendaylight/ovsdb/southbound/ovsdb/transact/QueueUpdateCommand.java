/*
 * Copyright (c) 2016 Intel Corporation and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.Queue;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.ovsdb.southbound.SouthboundUtil;
import org.opendaylight.ovsdb.utils.yang.YangUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.Queues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.QueuesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.queues.QueuesExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.queues.QueuesOtherConfig;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueueUpdateCommand implements TransactCommand {
    private static final Logger LOG = LoggerFactory.getLogger(QueueUpdateCommand.class);

    @Override
    public void execute(TransactionBuilder transaction, BridgeOperationalState state,
                        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> events) {
        execute(transaction, state, TransactUtils.extractCreatedOrUpdated(events, Queues.class));
    }

    @Override
    public void execute(TransactionBuilder transaction, BridgeOperationalState state,
                        Collection<DataTreeModification<Node>> modifications) {
        execute(transaction, state, TransactUtils.extractCreatedOrUpdated(modifications, Queues.class));
    }

    private void execute(TransactionBuilder transaction, BridgeOperationalState state,
                         Map<InstanceIdentifier<Queues>, Queues> createdOrUpdated) {
        for (Entry<InstanceIdentifier<Queues>, Queues> queueMapEntry: createdOrUpdated.entrySet()) {
            InstanceIdentifier<OvsdbNodeAugmentation> iid =
                    queueMapEntry.getKey().firstIdentifierOf(OvsdbNodeAugmentation.class);
            if (!state.getBridgeNode(iid).isPresent()) {
                return;
            }

            Queues queueEntry = queueMapEntry.getValue();
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

            Map<String, String> externalIdsMap = new HashMap<>();
            try {
                YangUtils.copyYangKeyValueListToMap(externalIdsMap, queueEntry.getQueuesExternalIds(),
                        QueuesExternalIds::getQueuesExternalIdKey, QueuesExternalIds::getQueuesExternalIdValue);
            } catch (NullPointerException e) {
                LOG.warn("Incomplete Queue external IDs", e);
            }
            externalIdsMap.put(SouthboundConstants.IID_EXTERNAL_ID_KEY,
                    SouthboundUtil.serializeInstanceIdentifier(
                    SouthboundMapper.createInstanceIdentifier(iid.firstKeyOf(Node.class).getNodeId())
                    .augmentation(OvsdbNodeAugmentation.class)
                    .child(Queues.class, new QueuesKey(queueEntry.getQueueId()))));
            queue.setExternalIds(externalIdsMap);

            try {
                queue.setOtherConfig(YangUtils.convertYangKeyValueListToMap(queueEntry.getQueuesOtherConfig(),
                        QueuesOtherConfig::getQueueOtherConfigKey, QueuesOtherConfig::getQueueOtherConfigValue));
            } catch (NullPointerException e) {
                LOG.warn("Incomplete Queue other_config", e);
            }

            OvsdbNodeAugmentation operNode =
                state.getBridgeNode(iid).get().getAugmentation(OvsdbNodeAugmentation.class);
            Uuid operQueueUuid = getQueueEntryUuid(operNode.getQueues(), queueEntry.getQueueId());
            if (operQueueUuid == null) {
                UUID namedUuid = new UUID(SouthboundConstants.QUEUE_NAMED_UUID_PREFIX
                        + TransactUtils.bytesToHexString(queueEntry.getQueueId().getValue().getBytes()));
                transaction.add(op.insert(queue).withId(namedUuid.toString())).build();
                LOG.info("Added queue Uuid : {} for Ovsdb Node : {}",
                        namedUuid, operNode);
            } else {
                UUID uuid = new UUID(operQueueUuid.getValue());
                Queue extraQueue = TyperUtils.getTypedRowWrapper(
                        transaction.getDatabaseSchema(), Queue.class, null);
                extraQueue.getUuidColumn().setData(uuid);
                transaction.add(op.update(queue)
                        .where(extraQueue.getUuidColumn().getSchema().opEqual(uuid)).build());
                LOG.info("Updated queue entries: {} for Ovsdb Node : {}",
                        queue, operNode);
            }
            transaction.build();
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
