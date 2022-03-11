/*
 * Copyright (c) 2016 Intel Corporation and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.schema.openvswitch.Queue;
import org.opendaylight.ovsdb.southbound.InstanceIdentifierCodec;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.ovsdb.utils.yang.YangUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.Queues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.QueuesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.queues.QueuesExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.queues.QueuesOtherConfig;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueueUpdateCommand implements TransactCommand {
    private static final Logger LOG = LoggerFactory.getLogger(QueueUpdateCommand.class);

    @Override
    public void execute(final TransactionBuilder transaction, final BridgeOperationalState state,
            final DataChangeEvent events, final InstanceIdentifierCodec instanceIdentifierCodec) {
        execute(transaction, state, TransactUtils.extractCreatedOrUpdated(events, Queues.class),
                instanceIdentifierCodec);
    }

    @Override
    public void execute(final TransactionBuilder transaction, final BridgeOperationalState state,
            final Collection<DataTreeModification<Node>> modifications,
            final InstanceIdentifierCodec instanceIdentifierCodec) {
        execute(transaction, state, TransactUtils.extractCreatedOrUpdated(modifications, Queues.class),
                instanceIdentifierCodec);
    }

    @SuppressFBWarnings("DCN_NULLPOINTER_EXCEPTION")
    private static void execute(final TransactionBuilder transaction, final BridgeOperationalState state,
            final Map<InstanceIdentifier<Queues>, Queues> createdOrUpdated,
            final InstanceIdentifierCodec instanceIdentifierCodec) {
        for (Entry<InstanceIdentifier<Queues>, Queues> queueMapEntry: createdOrUpdated.entrySet()) {
            InstanceIdentifier<OvsdbNodeAugmentation> iid =
                    queueMapEntry.getKey().firstIdentifierOf(OvsdbNodeAugmentation.class);
            if (!state.getBridgeNode(iid).isPresent()) {
                return;
            }

            Queues queueEntry = queueMapEntry.getValue();
            Queue queue = transaction.getTypedRowWrapper(Queue.class);

            if (queueEntry.getDscp() != null) {
                try {
                    Set<Long> dscpSet = new HashSet<>();
                    if (dscpSet.add(Long.valueOf(queueEntry.getDscp().toString()))) {
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
                    instanceIdentifierCodec.serialize(
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
                state.getBridgeNode(iid).get().augmentation(OvsdbNodeAugmentation.class);
            Uuid operQueueUuid = getQueueEntryUuid(operNode.getQueues(), queueEntry.key());
            if (operQueueUuid == null) {
                UUID namedUuid = new UUID(SouthboundConstants.QUEUE_NAMED_UUID_PREFIX
                        + TransactUtils.bytesToHexString(queueEntry.getQueueId().getValue().getBytes(UTF_8)));
                transaction.add(op.insert(queue).withId(namedUuid.toString()));
                LOG.info("Added queue Uuid : {} for Ovsdb Node : {}",
                        namedUuid, operNode);
            } else {
                UUID uuid = new UUID(operQueueUuid.getValue());
                Queue extraQueue = transaction.getTypedRowSchema(Queue.class);
                extraQueue.getUuidColumn().setData(uuid);
                transaction.add(op.update(queue)
                        .where(extraQueue.getUuidColumn().getSchema().opEqual(uuid)).build());
                LOG.info("Updated queue entries: {} for Ovsdb Node : {}",
                        queue, operNode);
            }
        }
    }

    private static Uuid getQueueEntryUuid(final Map<QueuesKey, Queues> operQueues, final QueuesKey queueId) {
        if (operQueues != null) {
            Queues queueEntry = operQueues.get(queueId);
            if (queueEntry != null) {
                return queueEntry.getQueueUuid();
            }
        }
        return null;
    }
}
