/*
 * Copyright (c) 2016 Intel Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.transactions.md;

import com.google.common.base.Optional;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.schema.openvswitch.Qos;
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

public class OvsdbQosRemovedCommand extends AbstractTransactionComponent {
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbQosRemovedCommand.class);

    public OvsdbQosRemovedCommand() {
        super(Qos.class);
    }

    @Override
    public void execute(final OvsdbTransactionContext context) {
        final Map<UUID, Qos> removedQosRows = context.getRemovedRows(Qos.class);
        if (removedQosRows == null || removedQosRows.isEmpty()) {
            return;
        }

        final Optional<Node> ovsdbNode = SouthboundUtil.readNode(transaction, context.getNode());
        if (ovsdbNode.isPresent()) {
            InstanceIdentifier<Node> ovsdbNodeIid =
                    SouthboundMapper.createInstanceIdentifier(getOvsdbConnectionInstance().getNodeId());
            for (UUID qosUuid : removedQosRows.keySet()) {
                QosEntriesKey qosKey = getQosEntriesKey(ovsdbNode.get(), qosUuid);
                if (qosKey != null) {
                    transaction.delete(LogicalDatastoreType.OPERATIONAL, ovsdbNodeIid
                        .augmentation(OvsdbNodeAugmentation.class)
                        .child(QosEntries.class, qosKey));
                }
            }
        }
    }

    private static QosEntriesKey getQosEntriesKey(final Node node, final UUID qosUuid) {
        List<QosEntries> qosList = node.augmentation(OvsdbNodeAugmentation.class).getQosEntries();
        if (qosList == null || qosList.isEmpty()) {
            LOG.debug("Deleting Qos {}, Ovsdb Node {} does not have a Qos list.", qosUuid, node);
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
        LOG.debug("Deleted Queue {} not found in Ovsdb Node {}", qosUuid, node);
        return null;
    }
}
