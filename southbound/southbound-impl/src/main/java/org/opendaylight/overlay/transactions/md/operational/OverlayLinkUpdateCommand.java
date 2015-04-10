/*
* Copyright (c) 2015 Intel Corp. and others.  All rights reserved.
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
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class OverlayLinkUpdateCommand extends AbstractTransactionCommand {
    private static final Logger LOG = LoggerFactory.getLogger(OverlayLinkUpdateCommand.class);

    public OverlayLinkUpdateCommand(AsyncDataChangeEvent<InstanceIdentifier<?>,
            DataObject> changes) {
        super(changes);
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        Map<InstanceIdentifier<Link>, Link> changed =
                TransactUtils.extractUpdated(getChanges(), Link.class);
        for (Map.Entry<InstanceIdentifier<Link>, Link> linkEntry :
                changed.entrySet()) {
            final InstanceIdentifier<Link> linkPath = OverlayMapper
                    .createInstanceIdentifier(linkEntry.getValue().getLinkId());
            LOG.debug("Received request to update link id: {}",
                    linkEntry.getValue().getLinkId().toString());
            Optional<Link> link = Optional.absent();
            try {
                link = transaction.read(
                        LogicalDatastoreType.OPERATIONAL, linkPath)
                        .checkedGet();
            } catch (final ReadFailedException e) {
                LOG.warn("Read Operational/DS for Link failed {}",
                        linkPath, e);
            }
            if (link.isPresent()) {
                transaction.merge(LogicalDatastoreType.OPERATIONAL,
                        linkPath, OverlayMapper.createLink(new LinkKey(linkEntry.getValue().getLinkId()),
                                linkEntry.getValue().getSource().getSourceNode(),
                                linkEntry.getValue().getDestination().getDestNode()));
            }
        }
    }
}
