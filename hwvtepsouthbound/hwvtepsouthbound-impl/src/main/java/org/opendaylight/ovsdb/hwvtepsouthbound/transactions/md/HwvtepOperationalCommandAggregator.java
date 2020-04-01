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
import java.util.concurrent.atomic.AtomicInteger;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepConnectionInstance;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundConstants;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HwvtepOperationalCommandAggregator implements TransactionCommand {

    private static final Logger LOG = LoggerFactory.getLogger(HwvtepOperationalCommandAggregator.class);
    private List<TransactionCommand> commands = new ArrayList<>();
    private final HwvtepConnectionInstance connectionInstance;
    private final TableUpdates tableUpdates;
    private final AtomicInteger retryCount = new AtomicInteger(HwvtepSouthboundConstants.CHAIN_RETRY_COUNT);

    public HwvtepOperationalCommandAggregator(HwvtepConnectionInstance key,TableUpdates updates,
            DatabaseSchema dbSchema) {
        this.connectionInstance = key;
        this.tableUpdates = updates;
        commands.add(new GlobalUpdateCommand(key, updates, dbSchema));
        commands.add(new HwvtepPhysicalSwitchUpdateCommand(key, updates, dbSchema));
        commands.add(new HwvtepPhysicalSwitchRemoveCommand(key, updates, dbSchema));
        commands.add(new HwvtepManagerUpdateCommand(key, updates, dbSchema));
        commands.add(new HwvtepManagerRemoveCommand(key, updates, dbSchema));
        commands.add(new HwvtepLogicalSwitchUpdateCommand(key, updates, dbSchema));
        commands.add(new HwvtepPhysicalPortUpdateCommand(key, updates, dbSchema));
        commands.add(new HwvtepPhysicalPortRemoveCommand(key, updates, dbSchema));
        commands.add(new HwvtepPhysicalLocatorUpdateCommand(key, updates, dbSchema));
        commands.add(new HwvtepMacEntriesRemoveCommand(key, updates, dbSchema));
        commands.add(new HwvtepTunnelUpdateCommand(key, updates, dbSchema));
        commands.add(new HwvtepTunnelRemoveCommand(key, updates, dbSchema));
        commands.add(new HwvtepUcastMacsLocalUpdateCommand(key, updates, dbSchema));
        commands.add(new HwvtepUcastMacsRemoteUpdateCommand(key, updates, dbSchema));
        commands.add(new HwvtepMcastMacsLocalUpdateCommand(key, updates, dbSchema));
        commands.add(new HwvtepMcastMacsRemoteUpdateCommand(key, updates, dbSchema));
        commands.add(new HwvtepLogicalSwitchRemoveCommand(key, updates, dbSchema));
        commands.add(new HwvtepLogicalRouterUpdateCommand(key, updates, dbSchema));
        commands.add(new HwvtepLogicalRouterRemoveCommand(key, updates, dbSchema));
        commands.add(new HwvtepPhysicalLocatorRemoveCommand(key, updates, dbSchema));
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void execute(ReadWriteTransaction transaction) {
        if (!connectionInstance.isActive()) {
            return;
        }
        try {
            for (TransactionCommand command : commands) {
                command.execute(transaction);
            }
        } catch (Exception e) {
            LOG.error("Failed to handle request from device ", e);
            throw e;
        }
        connectionInstance.getDeviceInfo().onOperDataAvailable();
    }

    @Override
    public String toString() {
        return tableUpdates.toString();
    }

    public void onSuccess() {
        for (TransactionCommand command : commands) {
            command.onSuccess();
        }
    }

    public void onFailure() {
        for (TransactionCommand command : commands) {
            command.onFailure();
        }
    }

    public int getTransactionChainRetryCount() {
        return retryCount.decrementAndGet();
    }
}
