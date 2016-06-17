/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.southbound.transactions.md;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.Version;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionInstance;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsdbOperationalCommandAggregator implements TransactionCommand {

    private static final Logger LOG = LoggerFactory.getLogger(OvsdbOperationalCommandAggregator.class);
    private List<TransactionCommand> commands = new ArrayList<>();
    private CountDownLatch commandCompletionLatch = null;

    public OvsdbOperationalCommandAggregator(OvsdbConnectionInstance key,TableUpdates updates,
            DatabaseSchema dbSchema) {
        commands.add(new OpenVSwitchUpdateCommand(key, updates, dbSchema));
        commands.add(new OvsdbManagersUpdateCommand(key, updates,  dbSchema));
        commands.add(new OvsdbManagersRemovedCommand(key, updates,  dbSchema));
        commands.add(new OvsdbQosUpdateCommand(key, updates,  dbSchema));
        commands.add(new OvsdbQosRemovedCommand(key, updates,  dbSchema));
        commands.add(new OvsdbQueueUpdateCommand(key, updates,  dbSchema));
        commands.add(new OvsdbQueueRemovedCommand(key, updates,  dbSchema));
        commands.add(new OvsdbBridgeUpdateCommand(key, updates,  dbSchema));
        commands.add(new OvsdbBridgeRemovedCommand(key, updates,  dbSchema));
        commands.add(new OvsdbControllerUpdateCommand(key, updates,  dbSchema));
        commands.add(new OvsdbControllerRemovedCommand(key, updates,  dbSchema));
        commands.add(new OvsdbPortUpdateCommand(key, updates, dbSchema));
        commands.add(new OvsdbPortRemoveCommand(key, updates, dbSchema));

        if(dbSchema.getVersion().compareTo(Version.fromString(SouthboundConstants.AUTOATTACH_SUPPORTED_OVS_SCHEMA_VERSION)) >= 0) {
            commands.add(new OvsdbAutoAttachUpdateCommand(key, updates, dbSchema));
            commands.add(new OvsdbAutoAttachRemovedCommand(key, updates, dbSchema));
        } else {
            LOG.debug("UNSUPPORTED FUNCTIONALITY: AutoAttach not supported in OVS schema version {}", dbSchema.getVersion().toString());
        }
    }

    public OvsdbOperationalCommandAggregator(OvsdbConnectionInstance key,TableUpdates updates,
                                             DatabaseSchema dbSchema, CountDownLatch commandCompletionLatch) {
        this(key, updates, dbSchema);
        this.commandCompletionLatch = commandCompletionLatch;
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        for (TransactionCommand command: commands) {
            command.execute(transaction);
        }

        // notify command completion
        if (commandCompletionLatch != null) {
            commandCompletionLatch.countDown();
        }
    }
}
