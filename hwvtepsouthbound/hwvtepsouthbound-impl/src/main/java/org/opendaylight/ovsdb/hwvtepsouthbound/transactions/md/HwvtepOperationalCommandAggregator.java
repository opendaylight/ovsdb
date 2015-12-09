/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound.transactions.md;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepConnectionInstance;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;

public class HwvtepOperationalCommandAggregator implements TransactionCommand {

    private List<TransactionCommand> commands = new ArrayList<>();

    public HwvtepOperationalCommandAggregator(HwvtepConnectionInstance key,TableUpdates updates,
            DatabaseSchema dbSchema) {
        commands.add(new GlobalUpdateCommand(key, updates, dbSchema));
        commands.add(new PhysicalSwitchUpdateCommand(key, updates, dbSchema));
        commands.add(new PhysicalSwitchRemoveCommand(key, updates, dbSchema));
        commands.add(new HwvtepManagerUpdateCommand(key, updates, dbSchema));
        commands.add(new LogicalSwitchUpdateCommand(key, updates, dbSchema));
        commands.add(new LogicalSwitchRemoveCommand(key, updates, dbSchema));
        commands.add(new PhysicalPortUpdateCommand(key, updates, dbSchema));
        commands.add(new PhysicalPortRemoveCommand(key, updates, dbSchema));
        commands.add(new HwvtepTunnelUpdateCommand(key, updates, dbSchema));
        commands.add(new PhysicalLocatorUpdateCommand(key, updates, dbSchema));
        commands.add(new PhysicalLocatorRemoveCommand(key, updates, dbSchema));
        commands.add(new UcastMacsLocalUpdateCommand(key, updates, dbSchema));
        commands.add(new UcastMacsRemoteUpdateCommand(key, updates, dbSchema));
        commands.add(new McastMacsLocalUpdateCommand(key, updates, dbSchema));
        commands.add(new McastMacsRemoteUpdateCommand(key, updates, dbSchema));
        commands.add(new MacEntriesRemoveCommand(key, updates, dbSchema));
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        for (TransactionCommand command: commands) {
            command.execute(transaction);
        }
    }
}
