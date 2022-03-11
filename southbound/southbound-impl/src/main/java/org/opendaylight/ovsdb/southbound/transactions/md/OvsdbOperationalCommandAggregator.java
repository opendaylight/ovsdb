/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.southbound.transactions.md;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.Version;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.southbound.InstanceIdentifierCodec;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionInstance;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsdbOperationalCommandAggregator implements TransactionCommand {

    private static final Logger LOG = LoggerFactory.getLogger(OvsdbOperationalCommandAggregator.class);
    private final List<TransactionCommand> commands = new ArrayList<>();
    private final Map<NodeId, Node> updatedBridgeNodes = new HashMap<>();
    private boolean initialUpdate;

    public OvsdbOperationalCommandAggregator(InstanceIdentifierCodec instanceIdentifierCodec,
            OvsdbConnectionInstance key, TableUpdates updates, DatabaseSchema dbSchema, boolean initialUpdate) {
        this.initialUpdate = initialUpdate;
        commands.add(new OpenVSwitchUpdateCommand(instanceIdentifierCodec, key, updates, dbSchema));
        commands.add(new OvsdbManagersUpdateCommand(key, updates,  dbSchema));
        commands.add(new OvsdbManagersRemovedCommand(key, updates,  dbSchema));
        commands.add(new OvsdbQosUpdateCommand(instanceIdentifierCodec, key, updates,  dbSchema));
        commands.add(new OvsdbQosRemovedCommand(key, updates,  dbSchema));
        commands.add(new OvsdbQueueUpdateCommand(instanceIdentifierCodec, key, updates,  dbSchema));
        commands.add(new OvsdbQueueRemovedCommand(key, updates,  dbSchema));
        commands.add(new OvsdbBridgeUpdateCommand(instanceIdentifierCodec, key, updates,  dbSchema,
                updatedBridgeNodes));
        commands.add(new OvsdbBridgeRemovedCommand(instanceIdentifierCodec, key, updates,  dbSchema));
        commands.add(new OvsdbControllerUpdateCommand(key, updates,  dbSchema));
        commands.add(new OvsdbControllerRemovedCommand(instanceIdentifierCodec, key, updates,  dbSchema));
        if (initialUpdate) {
            commands.add(new OvsdbInitialPortUpdateCommand(instanceIdentifierCodec, key, updates, dbSchema,
                    updatedBridgeNodes));
        } else {
            commands.add(new OvsdbPortUpdateCommand(instanceIdentifierCodec, key, updates, dbSchema));
        }
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

    @SuppressFBWarnings("DCN_NULLPOINTER_EXCEPTION")
    @Override
    public void execute(ReadWriteTransaction transaction) {
        for (TransactionCommand command : commands) {
            try {
                command.execute(transaction);
            } catch (NullPointerException | NoSuchElementException | ClassCastException e) {
                LOG.warn("Exception trying to execute {}", command, e);
            }
        }
    }

    @Override
    public void onSuccess() {
        for (TransactionCommand command : commands) {
            command.onSuccess();
        }
    }

    @Override
    public void onFailure(Throwable throwable) {
        for (TransactionCommand command: commands) {
            command.onFailure(throwable);
        }
    }
}
