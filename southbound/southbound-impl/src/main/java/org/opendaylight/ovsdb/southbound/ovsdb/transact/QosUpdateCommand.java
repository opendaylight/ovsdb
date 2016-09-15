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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.Qos;
import org.opendaylight.ovsdb.southbound.InstanceIdentifierCodec;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.ovsdb.utils.yang.YangUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
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
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QosUpdateCommand implements TransactCommand {
    private static final Logger LOG = LoggerFactory.getLogger(QosUpdateCommand.class);

    @Override
    public void execute(TransactionBuilder transaction, BridgeOperationalState state,
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> events,
            InstanceIdentifierCodec instanceIdentifierCodec) {
        execute(transaction, state, TransactUtils.extractCreatedOrUpdated(events, QosEntries.class),
                instanceIdentifierCodec);
    }

    @Override
    public void execute(TransactionBuilder transaction, BridgeOperationalState state,
            Collection<DataTreeModification<Node>> modifications, InstanceIdentifierCodec instanceIdentifierCodec) {
        execute(transaction, state, TransactUtils.extractCreatedOrUpdated(modifications, QosEntries.class),
                instanceIdentifierCodec);
    }

    private void execute(TransactionBuilder transaction, BridgeOperationalState state,
            Map<InstanceIdentifier<QosEntries>, QosEntries> createdOrUpdated,
            InstanceIdentifierCodec instanceIdentifierCodec) {
        for (Entry<InstanceIdentifier<QosEntries>, QosEntries> qosMapEntry: createdOrUpdated.entrySet()) {
            InstanceIdentifier<OvsdbNodeAugmentation> iid =
                    qosMapEntry.getKey().firstIdentifierOf(OvsdbNodeAugmentation.class);
            if (!state.getBridgeNode(iid).isPresent()) {
                return;
            }
            OvsdbNodeAugmentation operNode =
                state.getBridgeNode(iid).get().getAugmentation(OvsdbNodeAugmentation.class);

            QosEntries qosEntry = qosMapEntry.getValue();
            Qos qos = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), Qos.class);

            if (qosEntry.getQosType() != null) {
                qos.setType(SouthboundMapper.createQosType(qosEntry.getQosType()));
            }

            List<QueueList> queueList = qosEntry.getQueueList();
            Map<Long, UUID> newQueueList = new HashMap<>();
            if (queueList != null && !queueList.isEmpty()) {
                for (QueueList queue : queueList) {
                    if (queue.getQueueRef() != null) {
                        newQueueList.put(queue.getQueueNumber(),
                                new UUID(getQueueUuid(queue.getQueueRef(), operNode)));
                    } else if (queue.getQueueUuid() != null) {
                        newQueueList.put(queue.getQueueNumber(), new UUID(queue.getQueueUuid().getValue()));
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

            Uuid operQosUuid = getQosEntryUuid(operNode.getQosEntries(), qosEntry.getQosId());
            if (operQosUuid == null) {
                UUID namedUuid = new UUID(SouthboundConstants.QOS_NAMED_UUID_PREFIX
                        + TransactUtils.bytesToHexString(qosEntry.getQosId().getValue().getBytes()));
                transaction.add(op.insert(qos).withId(namedUuid.toString())).build();
                LOG.info("Added QoS Uuid: {} for node : {} ", namedUuid, operNode.getConnectionInfo());
            } else {
                UUID uuid = new UUID(operQosUuid.getValue());
                Qos extraQos = TyperUtils.getTypedRowWrapper(
                        transaction.getDatabaseSchema(), Qos.class, null);
                extraQos.getUuidColumn().setData(uuid);
                transaction.add(op.update(qos)
                        .where(extraQos.getUuidColumn().getSchema().opEqual(uuid)).build());
                LOG.info("Updated  QoS Uuid : {} for node : {} ", operQosUuid, operNode.getConnectionInfo());
            }
            transaction.build();
        }
    }

    private String getQueueUuid(OvsdbQueueRef queueRef, OvsdbNodeAugmentation operNode) {
        QueuesKey queueKey = queueRef.getValue().firstKeyOf(Queues.class);
        if (operNode.getQueues() != null && !operNode.getQueues().isEmpty()) {
            for (Queues queue : operNode.getQueues()) {
                if (queue.getQueueId().equals(queueKey.getQueueId())) {
                    return queue.getQueueUuid().getValue();
                }
            }
        }
        return SouthboundConstants.QUEUE_NAMED_UUID_PREFIX
                + TransactUtils.bytesToHexString(queueKey.getQueueId().getValue().getBytes());
    }

    private Uuid getQosEntryUuid(List<QosEntries> operQosEntries, Uri qosId) {
        if (operQosEntries != null && !operQosEntries.isEmpty()) {
            for (QosEntries qosEntry : operQosEntries) {
                if (qosEntry.getQosId().equals(qosId)) {
                    return qosEntry.getQosUuid();
                }
            }
        }
        return null;
    }
}
