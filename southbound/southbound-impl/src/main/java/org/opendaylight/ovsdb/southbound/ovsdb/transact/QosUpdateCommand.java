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
import java.util.Map;
import java.util.Map.Entry;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.schema.openvswitch.Qos;
import org.opendaylight.ovsdb.southbound.InstanceIdentifierCodec;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.ovsdb.utils.yang.YangUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbQueueRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.QosEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.QosEntriesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.Queues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.QueuesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.qos.entries.QosExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.qos.entries.QosOtherConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.qos.entries.QueueList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.qos.entries.QueueListKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QosUpdateCommand implements TransactCommand {
    private static final Logger LOG = LoggerFactory.getLogger(QosUpdateCommand.class);

    @Override
    public void execute(final TransactionBuilder transaction, final BridgeOperationalState state,
            final DataChangeEvent events, final InstanceIdentifierCodec instanceIdentifierCodec) {
        execute(transaction, state, TransactUtils.extractCreatedOrUpdated(events, QosEntries.class),
                instanceIdentifierCodec);
    }

    @Override
    public void execute(final TransactionBuilder transaction, final BridgeOperationalState state,
            final Collection<DataTreeModification<Node>> modifications,
            final InstanceIdentifierCodec instanceIdentifierCodec) {
        execute(transaction, state, TransactUtils.extractCreatedOrUpdated(modifications, QosEntries.class),
                instanceIdentifierCodec);
    }

    @SuppressFBWarnings("DCN_NULLPOINTER_EXCEPTION")
    private static void execute(final TransactionBuilder transaction, final BridgeOperationalState state,
            final Map<InstanceIdentifier<QosEntries>, QosEntries> createdOrUpdated,
            final InstanceIdentifierCodec instanceIdentifierCodec) {
        for (Entry<InstanceIdentifier<QosEntries>, QosEntries> qosMapEntry: createdOrUpdated.entrySet()) {
            InstanceIdentifier<OvsdbNodeAugmentation> iid =
                    qosMapEntry.getKey().firstIdentifierOf(OvsdbNodeAugmentation.class);
            if (!state.getBridgeNode(iid).isPresent()) {
                return;
            }
            OvsdbNodeAugmentation operNode =
                state.getBridgeNode(iid).orElseThrow().augmentation(OvsdbNodeAugmentation.class);

            QosEntries qosEntry = qosMapEntry.getValue();
            Qos qos = transaction.getTypedRowWrapper(Qos.class);

            if (qosEntry.getQosType() != null) {
                qos.setType(SouthboundMapper.createQosType(qosEntry.getQosType()));
            }

            Map<QueueListKey, QueueList> queueList = qosEntry.getQueueList();
            Map<Long, UUID> newQueueList = new HashMap<>();
            if (queueList != null && !queueList.isEmpty()) {
                for (QueueList queue : queueList.values()) {
                    if (queue.getQueueRef() != null) {
                        newQueueList.put(queue.getQueueNumber().toJava(),
                                new UUID(getQueueUuid(queue.getQueueRef(), operNode)));
                    }
                }
            }
            qos.setQueues(newQueueList);

            Map<String, String> externalIdsMap = new HashMap<>();
            try {
                YangUtils.copyYangKeyValueListToMap(externalIdsMap, qosEntry.getQosExternalIds(),
                        QosExternalIds::getQosExternalIdKey, QosExternalIds::getQosExternalIdValue);
            } catch (NullPointerException e) {
                LOG.warn("Incomplete Qos external IDs", e);
            }
            externalIdsMap.put(SouthboundConstants.IID_EXTERNAL_ID_KEY,

                    instanceIdentifierCodec.serialize(
                    SouthboundMapper.createInstanceIdentifier(iid.firstKeyOf(Node.class).getNodeId())
                    .augmentation(OvsdbNodeAugmentation.class)
                    .child(QosEntries.class, new QosEntriesKey(qosEntry.getQosId()))));
            qos.setExternalIds(externalIdsMap);

            try {
                qos.setOtherConfig(YangUtils.convertYangKeyValueListToMap(qosEntry.getQosOtherConfig(),
                        QosOtherConfig::getOtherConfigKey, QosOtherConfig::getOtherConfigValue));
            } catch (NullPointerException e) {
                LOG.warn("Incomplete Qos other_config", e);
            }

            Uuid operQosUuid = getQosEntryUuid(operNode.getQosEntries(), qosEntry.key());
            if (operQosUuid == null) {
                UUID namedUuid = new UUID(SouthboundConstants.QOS_NAMED_UUID_PREFIX
                        + TransactUtils.bytesToHexString(qosEntry.getQosId().getValue().getBytes(UTF_8)));
                transaction.add(op.insert(qos).withId(namedUuid.toString()));
                LOG.info("Added QoS Uuid: {} for node : {} ", namedUuid, operNode.getConnectionInfo());
            } else {
                UUID uuid = new UUID(operQosUuid.getValue());
                Qos extraQos = transaction.getTypedRowSchema(Qos.class);
                extraQos.getUuidColumn().setData(uuid);
                transaction.add(op.update(qos)
                        .where(extraQos.getUuidColumn().getSchema().opEqual(uuid)).build());
                LOG.info("Updated  QoS Uuid : {} for node : {} ", operQosUuid, operNode.getConnectionInfo());
            }
        }
    }

    private static String getQueueUuid(final OvsdbQueueRef queueRef, final OvsdbNodeAugmentation operNode) {
        QueuesKey queueKey = queueRef.getValue().firstKeyOf(Queues.class);
        Map<QueuesKey, Queues> queues = operNode.getQueues();
        if (queues != null) {
            Queues queue = queues.get(queueKey);
            if (queue != null) {
                return queue.getQueueUuid().getValue();
            }
        }
        return SouthboundConstants.QUEUE_NAMED_UUID_PREFIX
                + TransactUtils.bytesToHexString(queueKey.getQueueId().getValue().getBytes(UTF_8));
    }

    private static Uuid getQosEntryUuid(final Map<QosEntriesKey, QosEntries> operQosEntries,
            final QosEntriesKey qosId) {
        if (operQosEntries != null) {
            QosEntries qosEntry = operQosEntries.get(qosId);
            if (qosEntry != null) {
                return qosEntry.getQosUuid();
            }
        }
        return null;
    }
}
