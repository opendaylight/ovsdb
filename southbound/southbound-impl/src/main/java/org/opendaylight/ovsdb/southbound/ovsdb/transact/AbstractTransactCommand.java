package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public abstract class AbstractTransactCommand implements TransactCommand {

    private BridgeOperationalState operationalState;
    private AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes;

    protected AbstractTransactCommand() {
        // NO OP
    }

    public AbstractTransactCommand(BridgeOperationalState state, AsyncDataChangeEvent<InstanceIdentifier<?>,
            DataObject> changes) {
        this.operationalState = state;
        this.changes = changes;
    }

    public BridgeOperationalState getOperationalState() {
        return operationalState;
    }

    public AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> getChanges() {
        return changes;
    }
}
