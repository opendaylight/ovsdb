/*
 * Copyright © 2019 PANTHEON.tech, s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib.storage.mdsal;

import static java.util.Objects.requireNonNull;

import java.util.Map;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;
import org.opendaylight.ovsdb.lib.schema.typed.TypedDatabaseSchema;

/**
 * A context in which {@link TransactionComponent}s execute.
 */
public class TransactionContext {
    private final TypedDatabaseSchema dbSchema;
    private final TableUpdates updates;

    public TransactionContext(final TypedDatabaseSchema dbSchema, final TableUpdates updates) {
        this.dbSchema = requireNonNull(dbSchema);
        this.updates = requireNonNull(updates);
    }

    public final <T extends TypedBaseTable<?>> Map<UUID, T> getUpdatedRows(final Class<T> tableType) {
        // FIXME: add a cache
        return dbSchema.extractRowsUpdated(tableType, updates);
    }

    public final <T extends TypedBaseTable<?>> Map<UUID, T> getRemovedRows(final Class<T> tableType) {
        // FIXME: add a cache
        return dbSchema.extractRowsRemoved(tableType, updates);
    }

    public final <T extends TypedBaseTable<?>> Map<UUID, T> getOldRows(final Class<T> tableType) {
        // FIXME: add a cache
        return dbSchema.extractRowsOld(tableType, updates);
    }
}
