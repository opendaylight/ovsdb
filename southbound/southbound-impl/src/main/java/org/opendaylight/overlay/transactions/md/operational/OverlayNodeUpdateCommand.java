/*
 * Copyright (c) 2014 Intel Corp. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.overlay.transactions.md.operational;

import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.overlay.OverlayMapper;
import org.opendaylight.overlay.transactions.md.utils.AbstractTransactionCommand;
import org.opendaylight.ovsdb.southbound.ovsdb.transact.TransactUtils;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class OverlayNodeUpdateCommand extends AbstractTransactionCommand {
    private static final Logger LOG = LoggerFactory.getLogger(OverlayNodeUpdateCommand.class);

    public OverlayNodeUpdateCommand(AsyncDataChangeEvent<InstanceIdentifier<?>,
            DataObject> changes) {
        super(changes);
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        Map<InstanceIdentifier<Node>, Node> changed =
                TransactUtils.extractUpdated(getChanges(), Node.class);
        for (Map.Entry<InstanceIdentifier<Node>, Node> nodeEntry :
                changed.entrySet()) {
            final InstanceIdentifier<Node> nodePath = OverlayMapper
                    .createInstanceIdentifier(nodeEntry.getValue().getNodeId());
            LOG.debug("Received request to update node id: {}",
                    nodeEntry.getValue().getNodeId().toString());
            Optional<Node> node = Optional.absent();
            try {
                node = transaction.read(
                        LogicalDatastoreType.OPERATIONAL, nodePath)
                        .checkedGet();
            } catch (final ReadFailedException e) {
                LOG.warn("Read Operational/DS for Node failed {}",
                        nodePath, e);
            }
            if (node.isPresent()) {
                transaction.merge(LogicalDatastoreType.OPERATIONAL,
                        nodePath, OverlayMapper.createNode(new NodeKey(nodeEntry.getValue().getNodeId())));
            }
        }
    }
}
