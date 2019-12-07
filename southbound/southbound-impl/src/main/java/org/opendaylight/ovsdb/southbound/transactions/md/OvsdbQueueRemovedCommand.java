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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.lib.storage.mdsal.TransactionComponent;
import org.opendaylight.ovsdb.schema.openvswitch.Queue;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.ovsdb.southbound.SouthboundUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.Queues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.QueuesKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsdbQueueRemovedCommand extends TransactionComponent {
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbQueueRemovedCommand.class);

    public OvsdbQueueRemovedCommand() {
        super(Queue.class);
    }

    @Override
    public void execute(final ReadWriteTransaction transaction) {
        Map<UUID, Queue> removedQueueRows = TyperUtils.extractRowsRemoved(Queue.class, getUpdates(), getDbSchema());
        if (removedQueueRows == null || removedQueueRows.isEmpty()) {
            return;
        }

        final InstanceIdentifier<Node> nodeIId = getOvsdbConnectionInstance().getInstanceIdentifier();
        final Optional<Node> ovsdbNode = SouthboundUtil.readNode(transaction, nodeIId);
        if (ovsdbNode.isPresent()) {
            List<InstanceIdentifier<Queues>> result = new ArrayList<>();
            InstanceIdentifier<Node> ovsdbNodeIid =
                    SouthboundMapper.createInstanceIdentifier(getOvsdbConnectionInstance().getNodeId());
            for (UUID queueUuid : removedQueueRows.keySet()) {
                QueuesKey queueKey = getQueueKey(ovsdbNode.get(), queueUuid);
                if (queueKey != null) {
                    InstanceIdentifier<Queues> iid = ovsdbNodeIid
                        .augmentation(OvsdbNodeAugmentation.class)
                        .child(Queues.class, queueKey);
                    result.add(iid);
                }
            }
            deleteQueue(transaction, result);
        }
    }

    private QueuesKey getQueueKey(final Node node, final UUID queueUuid) {
        List<Queues> queueList = node.augmentation(OvsdbNodeAugmentation.class).getQueues();
        if (queueList == null || queueList.isEmpty()) {
            LOG.debug("Deleting Queue {}, Ovsdb Node {} does not have a Queue list.", queueUuid, node);
            return null;
        }
        Iterator<Queues> itr = queueList.iterator();
        Uuid quUuid = new Uuid(queueUuid.toString());
        while (itr.hasNext()) {
            Queues queue = itr.next();
            if (queue.getQueueUuid().equals(quUuid)) {
                return queue.key();
            }
        }
        LOG.debug("Deleted Queue {} not found in Ovsdb Node {}", queueUuid, node);
        return null;
    }

    private void deleteQueue(final ReadWriteTransaction transaction,
            final List<InstanceIdentifier<Queues>> queueIids) {
        for (InstanceIdentifier<Queues> queueIid: queueIids) {
            transaction.delete(LogicalDatastoreType.OPERATIONAL, queueIid);
        }
    }
}
