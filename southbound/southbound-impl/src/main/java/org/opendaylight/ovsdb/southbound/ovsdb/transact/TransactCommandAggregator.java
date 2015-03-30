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

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class TransactCommandAggregator implements TransactCommand {

    private List<TransactCommand> commands = new ArrayList<TransactCommand>();
    private AsyncDataChangeEvent<InstanceIdentifier<?>, OvsdbBridgeAugmentation> changes;
    private DataBroker db;

    public TransactCommandAggregator(DataBroker db,AsyncDataChangeEvent<InstanceIdentifier<?>,
            OvsdbBridgeAugmentation> changes) {
        this.db = db;
        this.changes = changes;
        commands.add(new BridgeCreateCommand(changes));
        commands.add(new BridgeRemovedCommand(db,changes));
    }

    @Override
    public void execute(TransactionBuilder transaction) {
        for (TransactCommand command:commands) {
            command.execute(transaction);
        }
    }
}
