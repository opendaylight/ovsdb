package org.opendaylight.overlay.transactions.md.operational;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.overlay.transactions.md.utils.TransactionCommand;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.ArrayList;
import java.util.List;

public class OverlayOperationalCommandAggregator implements TransactionCommand {


    private List<TransactionCommand> commands = new ArrayList<TransactionCommand>();

    public OverlayOperationalCommandAggregator(AsyncDataChangeEvent<InstanceIdentifier<?>,
            DataObject> changes) {
        commands.add(new OverlayNodeCreateCommand(changes));
        commands.add(new OverlayNodeUpdateCommand(changes));
        commands.add(new OverlayNodeRemoveCommand(changes));
        commands.add(new OverlayLinkCreateCommand(changes));
        commands.add(new OverlayLinkUpdateCommand(changes));
        commands.add(new OverlayLinkRemoveCommand(changes));
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        for (TransactionCommand command : commands) {
            command.execute(transaction);
        }
    }
}
