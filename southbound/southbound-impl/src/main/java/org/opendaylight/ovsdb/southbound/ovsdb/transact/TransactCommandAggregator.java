/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class TransactCommandAggregator implements TransactCommand {

    private List<TransactCommand> commands = new ArrayList<>();
    private AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes;
    private BridgeOperationalState operationalState;

    public TransactCommandAggregator(BridgeOperationalState state,AsyncDataChangeEvent<InstanceIdentifier<?>,
            DataObject> changes) {
        this.operationalState = state;
        this.changes = changes;
        commands.add(new BridgeUpdateCommand(state,changes));
        commands.add(new OpenVSwitchBridgeAddCommand());
        commands.add(new ControllerUpdateCommand(state,changes));
        commands.add(new ControllerRemovedCommand(state,changes));
        commands.add(new ProtocolUpdateCommand(state,changes));
        commands.add(new ProtocolRemovedCommand(state,changes));
        commands.add(new BridgeRemovedCommand(state,changes));
        commands.add(new TerminationPointCreateCommand(state,changes));
        commands.add(new TerminationPointDeleteCommand(state, changes));
        commands.add(new OvsdbNodeUpdateCommand(changes));
        commands.add(new TerminationPointUpdateCommand(state, changes));
    }

    @Override
    public void execute(TransactionBuilder transaction) {
        for (TransactCommand command:commands) {
            command.execute(transaction);
        }
    }
}
