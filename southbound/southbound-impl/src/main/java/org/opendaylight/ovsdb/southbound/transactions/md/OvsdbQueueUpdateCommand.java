/*
 * Copyright (c) 2016 Intel Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.southbound.transactions.md;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.Queue;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionInstance;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.ovsdb.southbound.SouthboundUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.Queues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.QueuesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.QueuesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.queues.QueuesExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.queues.QueuesExternalIdsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.queues.QueuesExternalIdsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.queues.QueuesOtherConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.queues.QueuesOtherConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.queues.QueuesOtherConfigKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class OvsdbQueueUpdateCommand extends AbstractTransactionCommand {
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbQueueUpdateCommand.class);

    private Map<UUID, Queue> updatedQueueRows;
    private Map<UUID, Queue> oldQueueRows;

    public OvsdbQueueUpdateCommand(OvsdbConnectionInstance key,
            TableUpdates updates, DatabaseSchema dbSchema) {
        super(key, updates, dbSchema);
        updatedQueueRows = TyperUtils.extractRowsUpdated(Queue.class,getUpdates(), getDbSchema());
        oldQueueRows = TyperUtils.extractRowsOld(Queue.class, getUpdates(), getDbSchema());
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        if (updatedQueueRows != null && !updatedQueueRows.isEmpty()) {
            updateQueue(transaction);
        }
    }

    /**
     * Update the Queues values after finding the related {@OpenVSwitch} list.
     * <p>
     * Queue and OpenVSwitch are independent tables in the Open_vSwitch schema
     * but the OVSDB yang model includes the Queue fields in the
     * OvsdbNode data. In some cases the OVSDB will send OpenVSwitch and Queue
     * updates together and in other cases independently. This method here
     * assumes the latter.
     * </p>
     *
     * @param transaction the {@link ReadWriteTransaction}
     * @param updatedQueueRows updated {@link Queue} rows

     */
    private void updateQueue(ReadWriteTransaction transaction) {

        final InstanceIdentifier<Node> nodeIId = getOvsdbConnectionInstance().getInstanceIdentifier();
        final Optional<Node> ovsdbNode = SouthboundUtil.readNode(transaction, nodeIId);
        if (ovsdbNode.isPresent()) {
            for (Entry<UUID, Queue> entry : updatedQueueRows.entrySet()) {
                Queue queue = entry.getValue();
                Queue oldQueue = oldQueueRows.get(entry.getKey());
                QueuesBuilder queuesBuilder = new QueuesBuilder();
                queuesBuilder.setQueueId(new Uri(getQueueId(queue)));
                queuesBuilder.setQueueUuid(new Uuid(entry.getKey().toString()));
                Collection<Long> dscp = queue.getDscpColumn().getData();
                if (!dscp.isEmpty()) {
                    queuesBuilder.setDscp(dscp.iterator().next().shortValue());
                }
                setOtherConfig(transaction, queuesBuilder, oldQueue, queue, nodeIId);
                setExternalIds(transaction, queuesBuilder, oldQueue, queue, nodeIId);

                Queues queues = queuesBuilder.build();
                LOG.debug("Update Ovsdb Node {} with queue entries {}",ovsdbNode.get(), queues);
                InstanceIdentifier<Queues> iid = nodeIId
                        .augmentation(OvsdbNodeAugmentation.class)
                        .child(Queues.class, queues.getKey());
                transaction.merge(LogicalDatastoreType.OPERATIONAL,
                        iid, queues);
            }
        }
    }

    private String getQueueId(Queue queue) {
        if (queue.getExternalIdsColumn() != null
                && queue.getExternalIdsColumn().getData() != null
                && queue.getExternalIdsColumn().getData().containsKey(SouthboundConstants.IID_EXTERNAL_ID_KEY)) {
            InstanceIdentifier<Queues> queueIid = (InstanceIdentifier<Queues>) SouthboundUtil.deserializeInstanceIdentifier(
                    queue.getExternalIdsColumn().getData().get(SouthboundConstants.IID_EXTERNAL_ID_KEY));
            if (queueIid != null) {
                QueuesKey queuesKey = queueIid.firstKeyOf(Queues.class);
                if (queuesKey != null) {
                    return queuesKey.getQueueId().getValue();
                }
            }
        }
        return SouthboundConstants.QUEUE_URI_PREFIX + "://" + queue.getUuid().toString();
    }

    private void setOtherConfig(ReadWriteTransaction transaction,
            QueuesBuilder queuesBuilder, Queue oldQueue, Queue queue,
            InstanceIdentifier<Node> nodeIId) {
        Map<String, String> oldOtherConfigs = null;
        Map<String, String> otherConfigs = null;

        if (queue.getOtherConfigColumn() != null) {
            otherConfigs = queue.getOtherConfigColumn().getData();
        }
        if (oldQueue != null && oldQueue.getOtherConfigColumn() != null) {
            oldOtherConfigs = oldQueue.getOtherConfigColumn().getData();
        }
        if ((oldOtherConfigs != null) && !oldOtherConfigs.isEmpty()) {
            removeOldConfigs(transaction, queuesBuilder, oldOtherConfigs, queue, nodeIId);
        }
        if (otherConfigs != null && !otherConfigs.isEmpty()) {
            setNewOtherConfigs(queuesBuilder, otherConfigs);
        }
    }

    private void removeOldConfigs(ReadWriteTransaction transaction,
            QueuesBuilder queuesBuilder, Map<String, String> oldOtherConfigs,
            Queue queue, InstanceIdentifier<Node> nodeIId) {
        InstanceIdentifier<Queues> queueIId = nodeIId
                .augmentation(OvsdbNodeAugmentation.class)
                .child(Queues.class, queuesBuilder.build().getKey());
        Set<String> otherConfigKeys = oldOtherConfigs.keySet();
        for (String otherConfigKey : otherConfigKeys) {
            KeyedInstanceIdentifier<QueuesOtherConfig, QueuesOtherConfigKey> otherIId =
                    queueIId
                    .child(QueuesOtherConfig.class, new QueuesOtherConfigKey(otherConfigKey));
            transaction.delete(LogicalDatastoreType.OPERATIONAL, otherIId);
        }
    }

    private void setNewOtherConfigs(QueuesBuilder queuesBuilder,
            Map<String, String> otherConfig) {
        Set<String> otherConfigKeys = otherConfig.keySet();
        List<QueuesOtherConfig> otherConfigList = new ArrayList<>();
        String otherConfigValue;
        for (String otherConfigKey : otherConfigKeys) {
            otherConfigValue = otherConfig.get(otherConfigKey);
            if (otherConfigKey != null && otherConfigValue != null) {
                otherConfigList.add(new QueuesOtherConfigBuilder().setQueueOtherConfigKey(otherConfigKey)
                        .setQueueOtherConfigValue(otherConfigValue).build());
            }
        }
        queuesBuilder.setQueuesOtherConfig(otherConfigList);
    }

    private void setExternalIds(ReadWriteTransaction transaction,
            QueuesBuilder queuesBuilder, Queue oldQueue, Queue queue,
            InstanceIdentifier<Node> nodeIId) {
        Map<String, String> oldExternalIds = null;
        Map<String, String> externalIds = null;

        if (queue.getExternalIdsColumn() != null) {
            externalIds = queue.getExternalIdsColumn().getData();
        }
        if (oldQueue != null && oldQueue.getExternalIdsColumn() != null) {
            oldExternalIds = oldQueue.getExternalIdsColumn().getData();
        }
        if ((oldExternalIds != null) && !oldExternalIds.isEmpty()) {
            removeOldExternalIds(transaction, queuesBuilder, oldExternalIds, queue, nodeIId);
        }
        if (externalIds != null && !externalIds.isEmpty()) {
            setNewExternalIds(queuesBuilder, externalIds);
        }
    }

    private void removeOldExternalIds(ReadWriteTransaction transaction,
            QueuesBuilder queuesBuilder, Map<String, String> oldExternalIds,
            Queue queue, InstanceIdentifier<Node> nodeIId) {
        InstanceIdentifier<Queues> queueIId = nodeIId
                .augmentation(OvsdbNodeAugmentation.class)
                .child(Queues.class, queuesBuilder.build().getKey());
        Set<String> externalIdsKeys = oldExternalIds.keySet();
        for (String extIdKey : externalIdsKeys) {
            KeyedInstanceIdentifier<QueuesExternalIds, QueuesExternalIdsKey> externalIId =
                    queueIId
                    .child(QueuesExternalIds.class, new QueuesExternalIdsKey(extIdKey));
            transaction.delete(LogicalDatastoreType.OPERATIONAL, externalIId);
        }
    }

    private void setNewExternalIds(QueuesBuilder queuesBuilder,
            Map<String, String> externalIds) {
        Set<String> externalIdsKeys = externalIds.keySet();
        List<QueuesExternalIds> externalIdsList = new ArrayList<>();
        String externalIdValue;
        for (String extIdKey : externalIdsKeys) {
            externalIdValue = externalIds.get(extIdKey);
            if (extIdKey != null && externalIdValue != null) {
                externalIdsList.add(new QueuesExternalIdsBuilder().setQueuesExternalIdKey(extIdKey)
                        .setQueuesExternalIdValue(externalIdValue).build());
            }
        }
        queuesBuilder.setQueuesExternalIds(externalIdsList);
    }

}
