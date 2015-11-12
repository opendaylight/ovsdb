/*
 * Copyright (c) 2015 China Telecom Beijing Research Institute and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound.transact;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

public class TransactCommandAggregator implements TransactCommand {

    private List<TransactCommand> commands = new ArrayList<TransactCommand>();

    public TransactCommandAggregator(HwvtepOperationalState state, Collection<DataTreeModification<Node>> changes) {
        commands.add(new LogicalSwitchUpdateCommand(state,changes));
        commands.add(new LogicalSwitchRemoveCommand(state,changes));
        commands.add(new PhysicalLocatorUpdateCommand(state,changes));
        commands.add(new PhysicalLocatorRemoveCommand(state,changes));
        commands.add(new PhysicalPortUpdateCommand(state,changes));
        commands.add(new PhysicalPortRemoveCommand(state,changes));
    }

    @Override
    public void execute(TransactionBuilder transaction) {
        for (TransactCommand command:commands) {
            command.execute(transaction);
        }
    }
}
