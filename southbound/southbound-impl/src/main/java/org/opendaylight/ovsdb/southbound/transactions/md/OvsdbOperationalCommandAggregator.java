/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.transactions.md;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.ovsdb.lib.message.TableUpdate.RowUpdate;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.notation.Version;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;
import org.opendaylight.ovsdb.lib.storage.mdsal.TransactionComponent;
import org.opendaylight.ovsdb.southbound.InstanceIdentifierCodec;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionInstance;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsdbOperationalCommandAggregator implements TransactionCommand {
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbOperationalCommandAggregator.class);

    private static final ImmutableList<TransactionComponent> BASE = ImmutableList.of(
        new OpenVSwitchUpdateCommand(),
        new OvsdbManagersUpdateCommand(),
        new OvsdbManagersRemovedCommand(),
        new OvsdbQosUpdateCommand(),
        new OvsdbQosRemovedCommand(),
        new OvsdbQueueUpdateCommand(),
        new OvsdbQueueRemovedCommand(),
        new OvsdbBridgeUpdateCommand(),
        new OvsdbBridgeRemovedCommand(),
        new OvsdbControllerUpdateCommand(),
        new OvsdbControllerRemovedCommand(),
        new OvsdbPortUpdateCommand(),
        new OvsdbPortRemoveCommand());
    private static final ImmutableList<TransactionComponent> AUTOATTACH =
            ImmutableList.<TransactionComponent>builderWithExpectedSize(BASE.size() + 2)
                .addAll(BASE)
                .add(new OvsdbAutoAttachUpdateCommand())
                .add(new OvsdbAutoAttachRemovedCommand())
                .build();
    private static final ImmutableSet<Class<? extends TypedBaseTable<?>>> BASE_TABLES = BASE.stream()
            .flatMap(component -> component.getInputTableTypes().stream())
            .collect(ImmutableSet.toImmutableSet());
    private static final ImmutableSet<Class<? extends TypedBaseTable<?>>> AUTOATTACH_TABLES = AUTOATTACH.stream()
            .flatMap(component -> component.getInputTableTypes().stream())
            .collect(ImmutableSet.toImmutableSet());

    private final ImmutableSet<Class<? extends TypedBaseTable<?>>> inputTableTypes;
    private final ImmutableList<TransactionComponent> components;

    public OvsdbOperationalCommandAggregator(final InstanceIdentifierCodec instanceIdentifierCodec,
            final OvsdbConnectionInstance key, final TableUpdates updates, final DatabaseSchema dbSchema) {
        if (dbSchema.getVersion().compareTo(
            Version.fromString(SouthboundConstants.AUTOATTACH_SUPPORTED_OVS_SCHEMA_VERSION)) >= 0) {
            inputTableTypes = AUTOATTACH_TABLES;
            components = AUTOATTACH;
        } else {
            LOG.debug("UNSUPPORTED FUNCTIONALITY: AutoAttach not supported in OVS schema version {}",
                dbSchema.getVersion());
            inputTableTypes = BASE_TABLES;
            components = BASE;
        }
    }

    @VisibleForTesting
    OvsdbOperationalCommandAggregator(final InstanceIdentifierCodec instanceIdentifierCodec,
            final OvsdbConnectionInstance key, final TableUpdates updates, final DatabaseSchema dbSchema,
            final TransactionComponent component) {
        inputTableTypes = component.getInputTableTypes();
        components = ImmutableList.of(component);
    }

    @Override
    public void execute(final ReadWriteTransaction transaction) {
        final Map<Class<? extends TypedBaseTable<?>>, Map<UUID, RowUpdate<GenericTableSchema>>> rowUpdates
                = new HashMap<>();

        for (TransactionComponent component : components) {
            try {
                component.execute(transaction);
            } catch (NullPointerException | NoSuchElementException | ClassCastException e) {
                LOG.warn("Exception trying to execute {}", command, e);
            }
        }
    }

    @Override
    public void onSuccess() {
        // FIXME: log something meaningful
    }

    @Override
    public void onFailure(final Throwable throwable) {
        // FIXME: log the failure
    }
}
