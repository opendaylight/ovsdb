package org.opendaylight.overlay.transactions.md.utils;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.overlay.transactions.md.utils.TransactionCommand;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public abstract class AbstractTransactionCommand implements TransactionCommand {

    private AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes;

    public AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> getChanges() {
        return changes;
    }

    protected AbstractTransactionCommand() {
        // Not Used
    }

    public AbstractTransactionCommand(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        this.changes = changes;
    }

}