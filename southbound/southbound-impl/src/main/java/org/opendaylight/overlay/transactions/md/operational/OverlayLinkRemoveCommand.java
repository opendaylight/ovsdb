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
import org.opendaylight.ovsdb.southbound.ovsdb.transact.TransactUtils;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class OverlayLinkRemoveCommand extends
        org.opendaylight.overlay.transactions.md.utils.AbstractTransactionCommand {
    private static final Logger LOG = LoggerFactory.getLogger(OverlayLinkCreateCommand.class);

    public OverlayLinkRemoveCommand(AsyncDataChangeEvent<InstanceIdentifier<?>,
            DataObject> changes) {
        super(changes);
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        Set<InstanceIdentifier<Link>> removed =
                TransactUtils.extractRemoved(getChanges(), Link.class);
        for (InstanceIdentifier<Link> linkRemoved :
                removed) {
            InstanceIdentifier<Link> linkId = linkRemoved;
            LOG.debug("Received request to delete link id: {}",
                    linkId.toString());
            transaction.delete(LogicalDatastoreType.OPERATIONAL,
                    linkId.firstIdentifierOf(Link.class));
        }
    }

}
