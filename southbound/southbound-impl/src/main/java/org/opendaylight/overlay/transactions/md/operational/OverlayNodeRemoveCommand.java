/*
 * Copyright (c) 2015 Intel Corp. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.overlay.transactions.md.operational;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.overlay.transactions.md.utils.AbstractTransactionCommand;
import org.opendaylight.ovsdb.southbound.ovsdb.transact.TransactUtils;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class OverlayNodeRemoveCommand extends
        AbstractTransactionCommand {
    private static final Logger LOG = LoggerFactory.getLogger(OverlayNodeCreateCommand.class);

    public OverlayNodeRemoveCommand(AsyncDataChangeEvent<InstanceIdentifier<?>,
            DataObject> changes) {
        super(changes);
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        Set<InstanceIdentifier<Node>> removed =
                TransactUtils.extractRemoved(getChanges(), Node.class);
        for (InstanceIdentifier<Node> nodeRemoved :
                removed) {
            InstanceIdentifier<Node> nodeId = nodeRemoved;
            LOG.debug("Received request to delete node id: {}",
                    nodeId.toString());
            transaction.delete(LogicalDatastoreType.OPERATIONAL,
                    nodeId.firstIdentifierOf(Node.class));
        }
    }
}
