package org.opendaylight.overlay.transactions.md.operational;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.overlay.OverlayMapper;
import org.opendaylight.overlay.transactions.md.utils.AbstractTransactionCommand;
import org.opendaylight.ovsdb.southbound.ovsdb.transact.TransactUtils;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class OverlayNodeCreateCommand extends AbstractTransactionCommand {
    private static final Logger LOG = LoggerFactory.getLogger(OverlayNodeCreateCommand.class);

    public OverlayNodeCreateCommand(AsyncDataChangeEvent<InstanceIdentifier<?>,
            DataObject> changes) {
        super(changes);
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        Map<InstanceIdentifier<Node>, Node> created =
                TransactUtils.extractCreated(getChanges(), Node.class);
        for (Map.Entry<InstanceIdentifier<Node>, Node> nodeEntry :
                created.entrySet()) {
            Node node = nodeEntry.getValue();
            LOG.debug("Received request to create node id: {} node key: {}",
                    node.getNodeId(),
                    node.getKey());
            transaction.put(LogicalDatastoreType.OPERATIONAL,
                    nodeEntry.getKey(),
                    OverlayMapper.createNode(node.getKey()));
        }
    }
}