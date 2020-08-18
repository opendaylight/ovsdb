/*
 * Copyright (c) 2015, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.transactions.md;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepConnectionInstance;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HwvtepOperationalCommandAggregator implements TransactionCommand {

    private static final Logger LOG = LoggerFactory.getLogger(HwvtepOperationalCommandAggregator.class);
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
            try {
                // This may be noisy, can be silenced if needed.
                LOG.trace("Executing command {}", command);
                command.execute(transaction);
            } catch (NullPointerException | NoSuchElementException | ClassCastException e) {
                LOG.error("Execution of command {} failed with the following exception."
                        + " Continuing the execution of remaining commands", command, e);
            }

        }
        connectionInstance.getDeviceInfo().onOperDataAvailable();
    }

    @Override
    public void onSuccess() {

        for (TransactionCommand command : commands) {
            command.onSuccess();
        }
    }

    @Override
    public void onFailure() {
        for (TransactionCommand command : commands) {
            command.onFailure();
        }
    }

}
