/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
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
