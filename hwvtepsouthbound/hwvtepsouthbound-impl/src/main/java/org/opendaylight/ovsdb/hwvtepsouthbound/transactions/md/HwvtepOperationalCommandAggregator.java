/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
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


    private List<TransactionCommand> commands = new ArrayList<TransactionCommand>();

    public HwvtepOperationalCommandAggregator(HwvtepConnectionInstance key,TableUpdates updates,
            DatabaseSchema dbSchema) {
        //TODO: Add commands in here
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        for (TransactionCommand command: commands) {
            command.execute(transaction);
        }
    }
}
