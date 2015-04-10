package org.opendaylight.overlay.transactions.md.operational;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.overlay.OverlayMapper;
import org.opendaylight.overlay.transactions.md.utils.AbstractTransactionCommand;
import org.opendaylight.ovsdb.southbound.ovsdb.transact.TransactUtils;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class OverlayLinkCreateCommand extends AbstractTransactionCommand {
    private static final Logger LOG = LoggerFactory.getLogger(OverlayLinkCreateCommand.class);

    public OverlayLinkCreateCommand(AsyncDataChangeEvent<InstanceIdentifier<?>,
            DataObject> changes) {
        super(changes);
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        Map<InstanceIdentifier<Link>, Link> created =
                TransactUtils.extractCreated(getChanges(), Link.class);
        for (Map.Entry<InstanceIdentifier<Link>, Link> linkEntry :
                created.entrySet()) {
            Link link = linkEntry.getValue();
            LOG.debug("Received request to create link id: {} link key: {}",
                    link.getLinkId(),
                    link.getKey());
            transaction.put(LogicalDatastoreType.OPERATIONAL,
                    linkEntry.getKey(),
                    OverlayMapper.createLink(link.getKey(), link.getSource().getSourceNode(),
                            link.getDestination().getDestNode()));
        }
    }
}