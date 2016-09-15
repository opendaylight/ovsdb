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

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.Version;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.southbound.InstanceIdentifierCodec;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionInstance;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsdbOperationalCommandAggregator implements TransactionCommand {

    private static final Logger LOG = LoggerFactory.getLogger(OvsdbOperationalCommandAggregator.class);
    private List<TransactionCommand> commands = new ArrayList<>();

    public OvsdbOperationalCommandAggregator(InstanceIdentifierCodec instanceIdentifierCodec,
            OvsdbConnectionInstance key, TableUpdates updates, DatabaseSchema dbSchema) {
        commands.add(new OpenVSwitchUpdateCommand(instanceIdentifierCodec, key, updates, dbSchema));
        commands.add(new OvsdbManagersUpdateCommand(key, updates,  dbSchema));
        commands.add(new OvsdbManagersRemovedCommand(key, updates,  dbSchema));
        commands.add(new OvsdbQosUpdateCommand(instanceIdentifierCodec, key, updates,  dbSchema));
        commands.add(new OvsdbQosRemovedCommand(key, updates,  dbSchema));
        commands.add(new OvsdbQueueUpdateCommand(instanceIdentifierCodec, key, updates,  dbSchema));
        commands.add(new OvsdbQueueRemovedCommand(key, updates,  dbSchema));
        commands.add(new OvsdbBridgeUpdateCommand(instanceIdentifierCodec, key, updates,  dbSchema));
        commands.add(new OvsdbBridgeRemovedCommand(instanceIdentifierCodec, key, updates,  dbSchema));
        commands.add(new OvsdbControllerUpdateCommand(key, updates,  dbSchema));
        commands.add(new OvsdbControllerRemovedCommand(instanceIdentifierCodec, key, updates,  dbSchema));
        commands.add(new OvsdbPortUpdateCommand(instanceIdentifierCodec, key, updates, dbSchema));
        commands.add(new OvsdbPortRemoveCommand(instanceIdentifierCodec, key, updates, dbSchema));

        if (dbSchema.getVersion().compareTo(
                Version.fromString(SouthboundConstants.AUTOATTACH_SUPPORTED_OVS_SCHEMA_VERSION)) >= 0) {
            commands.add(new OvsdbAutoAttachUpdateCommand(key, updates, dbSchema));
            commands.add(new OvsdbAutoAttachRemovedCommand(key, updates, dbSchema));
        } else {
            LOG.debug("UNSUPPORTED FUNCTIONALITY: AutoAttach not supported in OVS schema version {}",
                    dbSchema.getVersion().toString());
        }
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        for (TransactionCommand command: commands) {
            try {
                command.execute(transaction);
            } catch (NullPointerException e) {
                LOG.warn("Exception trying to execute {}", command, e);
            }
        }
    }
}
