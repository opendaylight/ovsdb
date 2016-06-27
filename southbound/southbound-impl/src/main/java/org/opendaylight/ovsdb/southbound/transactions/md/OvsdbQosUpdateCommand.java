/*
 * Copyright (c) 2016 Intel Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.southbound.transactions.md;

import com.google.common.base.Optional;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.Qos;
import org.opendaylight.ovsdb.schema.openvswitch.Queue;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionInstance;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.ovsdb.southbound.SouthboundUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbQueueRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.QosEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.QosEntriesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.QosEntriesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.Queues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.QueuesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.qos.entries.QosExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.qos.entries.QosExternalIdsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.qos.entries.QosExternalIdsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.qos.entries.QosOtherConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.qos.entries.QosOtherConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.qos.entries.QosOtherConfigKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.qos.entries.QueueList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.qos.entries.QueueListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.qos.entries.QueueListKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsdbQosUpdateCommand extends AbstractTransactionCommand {
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbQosUpdateCommand.class);

    private Map<UUID, Qos> updatedQosRows;
    private Map<UUID, Qos> oldQosRows;
    private Map<UUID, Queue> updatedQueueRows;

    public OvsdbQosUpdateCommand(OvsdbConnectionInstance key,
            TableUpdates updates, DatabaseSchema dbSchema) {
        super(key, updates, dbSchema);
        updatedQosRows = TyperUtils.extractRowsUpdated(Qos.class,getUpdates(), getDbSchema());
        oldQosRows = TyperUtils.extractRowsOld(Qos.class, getUpdates(), getDbSchema());
        updatedQueueRows = TyperUtils.extractRowsUpdated(Queue.class, getUpdates(), getDbSchema());
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        if (updatedQosRows != null && !updatedQosRows.isEmpty()) {
            updateQos(transaction, updatedQosRows);
        }
    }

    /**
     * Update the QosEntries values after finding the related {@link OpenVSwitch} list.
     * <p>
     * Qos and OpenVSwitch are independent tables in the Open_vSwitch schema
     * but the OVSDB yang model includes the Qos fields in the
     * OvsdbNode data. In some cases the OVSDB will send OpenVSwitch and Qos
     * updates together and in other cases independently. This method here
     * assumes the latter.
     * </p>
     *
     * @param transaction the {@link ReadWriteTransaction}
     * @param updatedQosRows updated {@link Qos} rows

     */
    private void updateQos(ReadWriteTransaction transaction,
                                  Map<UUID, Qos> updatedQosRows) {

        final InstanceIdentifier<Node> nodeIId = getOvsdbConnectionInstance().getInstanceIdentifier();
        final Optional<Node> ovsdbNode = SouthboundUtil.readNode(transaction, nodeIId);
        if (ovsdbNode.isPresent()) {
            for (Entry<UUID, Qos> entry : updatedQosRows.entrySet()) {
                Qos qos = entry.getValue();
                QosEntriesBuilder qosEntryBuilder = new QosEntriesBuilder();
                qosEntryBuilder.setQosId(new Uri(getQosId(qos)));
                qosEntryBuilder.setQosUuid(new Uuid(entry.getKey().toString()));
                qosEntryBuilder.setQosType(
                        SouthboundMapper.createQosType(qos.getTypeColumn().getData().toString()));
                Qos oldQos = oldQosRows.get(entry.getKey());
                setOtherConfig(transaction, qosEntryBuilder, oldQos, qos, nodeIId);
                setExternalIds(transaction, qosEntryBuilder, oldQos, qos, nodeIId);
                setQueueList(transaction, qosEntryBuilder, oldQos, qos, nodeIId, ovsdbNode.get());

                QosEntries qosEntry = qosEntryBuilder.build();
                LOG.debug("Update Ovsdb Node {} with qos entries {}",ovsdbNode.get(), qosEntry);
                InstanceIdentifier<QosEntries> iid = nodeIId
                        .augmentation(OvsdbNodeAugmentation.class)
                        .child(QosEntries.class, qosEntry.getKey());
                transaction.merge(LogicalDatastoreType.OPERATIONAL,
                        iid, qosEntry);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private String getQosId(Qos qos) {
        if (qos.getExternalIdsColumn() != null
                && qos.getExternalIdsColumn().getData() != null) {
            if (qos.getExternalIdsColumn().getData().containsKey(SouthboundConstants.IID_EXTERNAL_ID_KEY)) {
                InstanceIdentifier<QosEntries> qosIid =
                        (InstanceIdentifier<QosEntries>) SouthboundUtil.deserializeInstanceIdentifier(
                                qos.getExternalIdsColumn().getData().get(SouthboundConstants.IID_EXTERNAL_ID_KEY));
                if (qosIid != null) {
                    QosEntriesKey qosEntriesKey = qosIid.firstKeyOf(QosEntries.class);
                    if (qosEntriesKey != null) {
                        return qosEntriesKey.getQosId().getValue();
                    }
                }
            } else if (qos.getExternalIdsColumn().getData().containsKey(SouthboundConstants.QOS_ID_EXTERNAL_ID_KEY)) {
                return qos.getExternalIdsColumn().getData().get(SouthboundConstants.QOS_ID_EXTERNAL_ID_KEY);
            }
        }
        return SouthboundConstants.QOS_URI_PREFIX + "://" + qos.getUuid().toString();
    }

    private Queue getQueue(UUID queueUuid) {
        for (Entry<UUID, Queue> entry : updatedQueueRows.entrySet()) {
            if (entry.getKey().equals(queueUuid)) {
                return entry.getValue();
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private InstanceIdentifier<Queues> getQueueIid(UUID queueUuid, Node ovsdbNode) {
        Queue queue = getQueue(queueUuid);
        if (queue != null && queue.getExternalIdsColumn() != null
                && queue.getExternalIdsColumn().getData() != null
                && queue.getExternalIdsColumn().getData().containsKey(SouthboundConstants.IID_EXTERNAL_ID_KEY)) {
            return (InstanceIdentifier<Queues>) SouthboundUtil.deserializeInstanceIdentifier(
                    queue.getExternalIdsColumn().getData().get(SouthboundConstants.IID_EXTERNAL_ID_KEY));
        } else {
            OvsdbNodeAugmentation node = ovsdbNode.getAugmentation(OvsdbNodeAugmentation.class);
            if (node.getQueues() != null && !node.getQueues().isEmpty()) {
                for (Queues q : node.getQueues()) {
                    if (q.getQueueUuid().equals(new Uuid(queueUuid.toString()))) {
                        return SouthboundMapper.createInstanceIdentifier(ovsdbNode.getNodeId())
                                .augmentation(OvsdbNodeAugmentation.class)
                                .child(Queues.class, new QueuesKey(q.getQueueId()));
                    }
                }
            }
            LOG.debug("A Queue with UUID {} was not found in Ovsdb Node {}", queueUuid, node);
            return SouthboundMapper.createInstanceIdentifier(ovsdbNode.getNodeId())
                    .augmentation(OvsdbNodeAugmentation.class)
                    .child(Queues.class, new QueuesKey(
                            new Uri(SouthboundConstants.QUEUE_URI_PREFIX + "://" + queueUuid.toString())));
        }
    }

    private void setOtherConfig(ReadWriteTransaction transaction,
            QosEntriesBuilder qosEntryBuilder, Qos oldQos, Qos qos,
            InstanceIdentifier<Node> nodeIId) {
        Map<String, String> oldOtherConfigs = null;
        Map<String, String> otherConfigs = null;

        if (qos.getOtherConfigColumn() != null) {
            otherConfigs = qos.getOtherConfigColumn().getData();
        }
        if (oldQos != null && oldQos.getOtherConfigColumn() != null) {
            oldOtherConfigs = oldQos.getOtherConfigColumn().getData();
        }
        if ((oldOtherConfigs != null) && !oldOtherConfigs.isEmpty()) {
            removeOldConfigs(transaction, qosEntryBuilder, oldOtherConfigs, qos, nodeIId);
        }
        if (otherConfigs != null && !otherConfigs.isEmpty()) {
            setNewOtherConfigs(qosEntryBuilder, otherConfigs);
        }
    }

    private void removeOldConfigs(ReadWriteTransaction transaction,
            QosEntriesBuilder qosEntryBuilder, Map<String, String> oldOtherConfigs,
            Qos qos, InstanceIdentifier<Node> nodeIId) {
        InstanceIdentifier<QosEntries> qosIId = nodeIId
                .augmentation(OvsdbNodeAugmentation.class)
                .child(QosEntries.class, qosEntryBuilder.build().getKey());
        Set<String> otherConfigKeys = oldOtherConfigs.keySet();
        for (String otherConfigKey : otherConfigKeys) {
            KeyedInstanceIdentifier<QosOtherConfig, QosOtherConfigKey> otherIId =
                    qosIId
                    .child(QosOtherConfig.class, new QosOtherConfigKey(otherConfigKey));
            transaction.delete(LogicalDatastoreType.OPERATIONAL, otherIId);
        }
    }

    private void setNewOtherConfigs(QosEntriesBuilder qosEntryBuilder,
            Map<String, String> otherConfig) {
        Set<String> otherConfigKeys = otherConfig.keySet();
        List<QosOtherConfig> otherConfigList = new ArrayList<>();
        String otherConfigValue;
        for (String otherConfigKey : otherConfigKeys) {
            otherConfigValue = otherConfig.get(otherConfigKey);
            if (otherConfigKey != null && otherConfigValue != null) {
                otherConfigList.add(new QosOtherConfigBuilder().setOtherConfigKey(otherConfigKey)
                        .setOtherConfigValue(otherConfigValue).build());
            }
        }
        qosEntryBuilder.setQosOtherConfig(otherConfigList);
    }

    private void setExternalIds(ReadWriteTransaction transaction,
            QosEntriesBuilder qosEntryBuilder, Qos oldQos, Qos qos,
            InstanceIdentifier<Node> nodeIId) {
        Map<String, String> oldExternalIds = null;
        Map<String, String> externalIds = null;

        if (qos.getExternalIdsColumn() != null) {
            externalIds = qos.getExternalIdsColumn().getData();
        }
        if (oldQos != null && oldQos.getExternalIdsColumn() != null) {
            oldExternalIds = oldQos.getExternalIdsColumn().getData();
        }
        if ((oldExternalIds != null) && !oldExternalIds.isEmpty()) {
            removeOldExternalIds(transaction, qosEntryBuilder, oldExternalIds, qos, nodeIId);
        }
        if (externalIds != null && !externalIds.isEmpty()) {
            setNewExternalIds(qosEntryBuilder, externalIds);
        }
    }

    private void removeOldExternalIds(ReadWriteTransaction transaction,
            QosEntriesBuilder qosEntryBuilder, Map<String, String> oldExternalIds,
            Qos qos, InstanceIdentifier<Node> nodeIId) {
        InstanceIdentifier<QosEntries> qosIId = nodeIId
                .augmentation(OvsdbNodeAugmentation.class)
                .child(QosEntries.class, qosEntryBuilder.build().getKey());
        Set<String> externalIdsKeys = oldExternalIds.keySet();
        for (String extIdKey : externalIdsKeys) {
            KeyedInstanceIdentifier<QosExternalIds, QosExternalIdsKey> externalIId =
                    qosIId
                    .child(QosExternalIds.class, new QosExternalIdsKey(extIdKey));
            transaction.delete(LogicalDatastoreType.OPERATIONAL, externalIId);
        }
    }

    private void setNewExternalIds(QosEntriesBuilder qosEntryBuilder,
            Map<String, String> externalIds) {
        Set<String> externalIdsKeys = externalIds.keySet();
        List<QosExternalIds> externalIdsList = new ArrayList<>();
        String extIdValue;
        for (String extIdKey : externalIdsKeys) {
            extIdValue = externalIds.get(extIdKey);
            if (extIdKey != null && extIdValue != null) {
                externalIdsList.add(new QosExternalIdsBuilder().setQosExternalIdKey(extIdKey)
                        .setQosExternalIdValue(extIdValue).build());
            }
        }
        qosEntryBuilder.setQosExternalIds(externalIdsList);
    }

    private void setQueueList(ReadWriteTransaction transaction,
            QosEntriesBuilder qosEntryBuilder, Qos oldQos, Qos qos,
            InstanceIdentifier<Node> nodeIId, Node ovsdbNode) {
        Map<Long,UUID> oldQueueList = null;
        Map<Long,UUID> queueList = null;

        if (qos.getQueuesColumn() != null) {
            queueList = qos.getQueuesColumn().getData();
        }
        if (oldQos != null && oldQos.getQueuesColumn() != null) {
            oldQueueList = oldQos.getQueuesColumn().getData();
        }
        if ((oldQueueList != null) && !oldQueueList.isEmpty()) {
            removeOldQueues(transaction, qosEntryBuilder, oldQueueList, qos, nodeIId);
        }
        if (queueList != null && !queueList.isEmpty()) {
            setNewQueues(qosEntryBuilder, queueList, ovsdbNode);
        }
    }

    private void removeOldQueues(ReadWriteTransaction transaction,
            QosEntriesBuilder qosEntryBuilder, Map<Long, UUID> oldQueueList,
            Qos qos, InstanceIdentifier<Node> nodeIId) {
        InstanceIdentifier<QosEntries> qosIId = nodeIId
                .augmentation(OvsdbNodeAugmentation.class)
                .child(QosEntries.class, qosEntryBuilder.build().getKey());
        Collection<Long> queueListKeys = oldQueueList.keySet();
        for (Long queueListKey : queueListKeys) {
            KeyedInstanceIdentifier<QueueList, QueueListKey> otherIId =
                    qosIId
                    .child(QueueList.class, new QueueListKey(new Long(queueListKey.toString())));
            transaction.delete(LogicalDatastoreType.OPERATIONAL, otherIId);
        }
    }

    private void setNewQueues(QosEntriesBuilder qosEntryBuilder,
            Map<Long, UUID> queueList, Node ovsdbNode) {
        Set<Entry<Long, UUID>> queueEntries = queueList.entrySet();
        List<QueueList> newQueueList = new ArrayList<>();
        for (Entry<Long, UUID> queueEntry : queueEntries) {
            InstanceIdentifier<Queues> queueIid = getQueueIid(queueEntry.getValue(), ovsdbNode);
            if (queueIid != null) {
                newQueueList.add(
                    new QueueListBuilder()
                    .setQueueNumber(queueEntry.getKey())
                    .setQueueRef(new OvsdbQueueRef(queueIid))
                    .setQueueUuid(new Uuid(queueEntry.getValue().toString())).build());
            }
        }
        qosEntryBuilder.setQueueList(newQueueList);
    }
}
