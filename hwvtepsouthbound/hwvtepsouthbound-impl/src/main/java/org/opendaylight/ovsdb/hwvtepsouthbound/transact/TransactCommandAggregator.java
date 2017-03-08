/*
 * Copyright (c) 2015 China Telecom Beijing Research Institute and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound.transact;

import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TransactCommandAggregator implements TransactCommand {

    private List<TransactCommand> commands = new ArrayList<>();

    public TransactCommandAggregator(HwvtepOperationalState state, Collection<DataTreeModification<Node>> changes) {
        commands.add(new PhysicalSwitchUpdateCommand(state,changes));
        commands.add(new PhysicalSwitchRemoveCommand(state,changes));
        commands.add(new LogicalSwitchUpdateCommand(state,changes));
        commands.add(new LogicalSwitchRemoveCommand(state,changes));
        commands.add(new PhysicalPortUpdateCommand(state,changes));
        commands.add(new PhysicalPortRemoveCommand(state,changes));
        commands.add(new McastMacsRemoteUpdateCommand(state,changes));
        commands.add(new McastMacsRemoteRemoveCommand(state,changes));
        commands.add(new McastMacsLocalUpdateCommand(state,changes));
        commands.add(new McastMacsLocalRemoveCommand(state,changes));
        commands.add(new UcastMacsRemoteUpdateCommand(state,changes));
        commands.add(new UcastMacsRemoteRemoveCommand(state,changes));
        commands.add(new UcastMacsLocalUpdateCommand(state,changes));
        commands.add(new UcastMacsLocalRemoveCommand(state,changes));
        commands.add(new TunnelUpdateCommand(state,changes));
        commands.add(new TunnelRemoveCommand(state,changes));
    }

    @Override
    public void execute(TransactionBuilder transaction) {
        for (TransactCommand command:commands) {
            command.execute(transaction);
        }
    }

    @Override
    public void onConfigUpdate(TransactionBuilder transaction, InstanceIdentifier nodeIid, Identifiable data,
                               InstanceIdentifier key,
                               Object... extraData) {
    }

    @Override
    public void doDeviceTransaction(TransactionBuilder transaction, InstanceIdentifier nodeIid, Identifiable data,
                                    InstanceIdentifier key,
                                    Object... extraData) {
    }
}
