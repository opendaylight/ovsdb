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
    private final HwvtepConnectionInstance connectionInstance;

    public HwvtepOperationalCommandAggregator(HwvtepConnectionInstance key,TableUpdates updates,
            DatabaseSchema dbSchema) {
        this.connectionInstance = key;
        commands.add(new GlobalUpdateCommand(key, updates, dbSchema));
        commands.add(new HwvtepPhysicalSwitchUpdateCommand(key, updates, dbSchema));
        commands.add(new HwvtepPhysicalSwitchRemoveCommand(key, updates, dbSchema));
        commands.add(new HwvtepManagerUpdateCommand(key, updates, dbSchema));
        commands.add(new HwvtepManagerRemoveCommand(key, updates, dbSchema));
        commands.add(new HwvtepLogicalSwitchUpdateCommand(key, updates, dbSchema));
        commands.add(new HwvtepPhysicalPortUpdateCommand(key, updates, dbSchema));
        commands.add(new HwvtepPhysicalPortRemoveCommand(key, updates, dbSchema));
        commands.add(new HwvtepPhysicalLocatorUpdateCommand(key, updates, dbSchema));
        commands.add(new HwvtepTunnelUpdateCommand(key, updates, dbSchema));
        commands.add(new HwvtepTunnelRemoveCommand(key, updates, dbSchema));
        commands.add(new HwvtepPhysicalLocatorRemoveCommand(key, updates, dbSchema));
        commands.add(new HwvtepUcastMacsLocalUpdateCommand(key, updates, dbSchema));
        commands.add(new HwvtepUcastMacsRemoteUpdateCommand(key, updates, dbSchema));
        commands.add(new HwvtepMcastMacsLocalUpdateCommand(key, updates, dbSchema));
        commands.add(new HwvtepMcastMacsRemoteUpdateCommand(key, updates, dbSchema));
        commands.add(new HwvtepMacEntriesRemoveCommand(key, updates, dbSchema));
        commands.add(new HwvtepLogicalSwitchRemoveCommand(key, updates, dbSchema));
        commands.add(new HwvtepLogicalRouterUpdateCommand(key, updates, dbSchema));
        commands.add(new HwvtepLogicalRouterRemoveCommand(key, updates, dbSchema));
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        for (TransactionCommand command: commands) {
            command.execute(transaction);
        }
        connectionInstance.getDeviceInfo().onOpDataAvailable();
    }
}
