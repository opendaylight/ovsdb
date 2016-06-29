/*
 * Copyright (c) 2016 Intel Corporation and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import com.google.common.collect.ImmutableMap;
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
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.ovsdb.southbound.SouthboundUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
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
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QosUpdateCommand implements TransactCommand {
    private static final Logger LOG = LoggerFactory.getLogger(QosUpdateCommand.class);

    @Override
    public void execute(TransactionBuilder transaction, BridgeOperationalState state,
                        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> events) {
        execute(transaction, state, TransactUtils.extractCreatedOrUpdated(events, OvsdbNodeAugmentation.class));
    }

    @Override
    public void execute(TransactionBuilder transaction, BridgeOperationalState state,
                        Collection<DataTreeModification<Node>> modifications) {
        execute(transaction, state, TransactUtils.extractCreatedOrUpdated(modifications, OvsdbNodeAugmentation.class));
    }

    private void execute(TransactionBuilder transaction, BridgeOperationalState state,
                         Map<InstanceIdentifier<OvsdbNodeAugmentation>, OvsdbNodeAugmentation> createdOrUpdated) {
        for (Entry<InstanceIdentifier<OvsdbNodeAugmentation>, OvsdbNodeAugmentation> ovsdbNodeEntry:
            createdOrUpdated.entrySet()) {
            updateQos(transaction, state, ovsdbNodeEntry.getKey(), ovsdbNodeEntry.getValue());
        }
    }

    private void updateQos(
            TransactionBuilder transaction, BridgeOperationalState state,
            InstanceIdentifier<OvsdbNodeAugmentation> iid, OvsdbNodeAugmentation ovsdbNode) {

        List<QosEntries> qosEntries = ovsdbNode.getQosEntries();

        if (!state.getBridgeNode(iid).isPresent()) {
            return;
        }
        OvsdbNodeAugmentation operNode = state.getBridgeNode(iid).get().getAugmentation(OvsdbNodeAugmentation.class);
        List<QosEntries> operQosEntries = operNode.getQosEntries();
        List<Queues> operQueues = operNode.getQueues();

        if (qosEntries != null) {
            for (QosEntries qosEntry : qosEntries) {
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

                List<QosExternalIds> externalIds = qosEntry.getQosExternalIds();
                Map<String, String> externalIdsMap = new HashMap<>();
                if (externalIds != null) {
                    for (QosExternalIds externalId : externalIds) {
                        externalIdsMap.put(externalId.getQosExternalIdKey(), externalId.getQosExternalIdValue());
                    }
                }
                externalIdsMap.put(SouthboundConstants.QOS_ID_EXTERNAL_ID_KEY, qosEntry.getQosId().getValue());
                try {
                    qos.setExternalIds(ImmutableMap.copyOf(externalIdsMap));
                } catch (NullPointerException e) {
                    LOG.warn("Incomplete Qos external IDs", e);
                }
                externalIdsMap.put(SouthboundConstants.IID_EXTERNAL_ID_KEY,
                        SouthboundUtil.serializeInstanceIdentifier(
                        SouthboundMapper.createInstanceIdentifier(iid.firstKeyOf(Node.class, NodeKey.class).getNodeId())
                        .augmentation(OvsdbNodeAugmentation.class)
                        .child(QosEntries.class, new QosEntriesKey(qosEntry.getQosId()))));
                qos.setExternalIds(externalIdsMap);

                List<QosOtherConfig> otherConfigs = qosEntry.getQosOtherConfig();
                if (otherConfigs != null) {
                    Map<String, String> otherConfigsMap = new HashMap<>();
                    for (QosOtherConfig otherConfig : otherConfigs) {
                        otherConfigsMap.put(otherConfig.getOtherConfigKey(), otherConfig.getOtherConfigValue());
                    }
                    try {
                        qos.setOtherConfig(ImmutableMap.copyOf(otherConfigsMap));
                    } catch (NullPointerException e) {
                        LOG.warn("Incomplete Qos other_config", e);
                    }
                }

                Uuid operQosUuid = getQosEntryUuid(operQosEntries, qosEntry.getQosId());
                if (operQosUuid == null) {
                    UUID namedUuid = new UUID(SouthboundConstants.QOS_NAMED_UUID_PREFIX
                            + TransactUtils.bytesToHexString(qosEntry.getQosId().getValue().getBytes()));
                    transaction.add(op.insert(qos).withId(namedUuid.toString())).build();
                    LOG.info("Added QoS Uuid: {} for node : {} ",
                            namedUuid, ovsdbNode);
                } else {
                    UUID uuid = new UUID(operQosUuid.getValue());
                    Qos extraQos = TyperUtils.getTypedRowWrapper(
                            transaction.getDatabaseSchema(), Qos.class, null);
                    extraQos.getUuidColumn().setData(uuid);
                    transaction.add(op.update(qos)
                            .where(extraQos.getUuidColumn().getSchema().opEqual(uuid)).build());
                    LOG.info("Updated  QoS Uuid : {} for node : {} ",
                            operQosUuid, ovsdbNode);
                }
                transaction.build();
            }
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
