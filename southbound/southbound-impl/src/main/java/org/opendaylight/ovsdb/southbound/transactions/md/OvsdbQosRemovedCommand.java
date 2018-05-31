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
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.Qos;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionInstance;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.ovsdb.southbound.SouthboundUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.QosEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.QosEntriesKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsdbQosRemovedCommand extends AbstractTransactionCommand {
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbQueueUpdateCommand.class);

    private Map<UUID, Qos> removedQosRows;

    public OvsdbQosRemovedCommand(OvsdbConnectionInstance key,
            TableUpdates updates, DatabaseSchema dbSchema) {
        super(key, updates, dbSchema);
        removedQosRows = TyperUtils.extractRowsRemoved(Qos.class, getUpdates(), getDbSchema());
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        if (removedQosRows == null || removedQosRows.isEmpty()) {
            return;
        }

        final InstanceIdentifier<Node> nodeIId = getOvsdbConnectionInstance().getInstanceIdentifier();
        final Optional<Node> ovsdbNode = SouthboundUtil.readNode(transaction, nodeIId);
        if (ovsdbNode.isPresent()) {
            List<InstanceIdentifier<QosEntries>> result = new ArrayList<>();
            InstanceIdentifier<Node> ovsdbNodeIid =
                    SouthboundMapper.createInstanceIdentifier(getOvsdbConnectionInstance().getNodeId());
            for (UUID qosUuid : removedQosRows.keySet()) {
                QosEntriesKey qosKey = getQosEntriesKey(ovsdbNode.get(), qosUuid);
                if (qosKey != null) {
                    InstanceIdentifier<QosEntries> iid = ovsdbNodeIid
                        .augmentation(OvsdbNodeAugmentation.class)
                        .child(QosEntries.class, qosKey);
                    result.add(iid);
                }
            }
            deleteQos(transaction, result);
        }
    }

    private QosEntriesKey getQosEntriesKey(Node node, UUID qosUuid) {
        List<QosEntries> qosList = node.augmentation(OvsdbNodeAugmentation.class).getQosEntries();
        if (qosList == null || qosList.isEmpty()) {
            LOG.debug("Deleting Qos {}, Ovsdb Node {} does not have a Qos list.", qosUuid.toString(), node);
            return null;
        }
        Iterator<QosEntries> itr = qosList.iterator();
        Uuid quUuid = new Uuid(qosUuid.toString());
        while (itr.hasNext()) {
            QosEntries qos = itr.next();
            if (qos.getQosUuid().equals(quUuid)) {
                return qos.key();
            }
        }
        LOG.debug("Deleted Queue {} not found in Ovsdb Node {}", qosUuid.toString(), node);
        return null;
    }

    private void deleteQos(ReadWriteTransaction transaction,
            List<InstanceIdentifier<QosEntries>> qosEntryIids) {
        for (InstanceIdentifier<QosEntries> qosEntryIid: qosEntryIids) {
            transaction.delete(LogicalDatastoreType.OPERATIONAL, qosEntryIid);
        }
    }
}
