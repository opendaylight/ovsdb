/*
 * Copyright © 2019 PANTHEON.tech, s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib.schema.typed;

import static java.util.Objects.requireNonNull;

import com.google.common.reflect.Reflection;
import java.util.HashMap;
import java.util.Map;
import org.opendaylight.ovsdb.lib.message.TableUpdate;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.ForwardingDatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;

final class TypedDatabaseSchemaImpl extends ForwardingDatabaseSchema implements TypedDatabaseSchema {
    private final DatabaseSchema delegate;

    TypedDatabaseSchemaImpl(final DatabaseSchema delegate) {
        this.delegate = requireNonNull(delegate);
    }

    @Override
    protected DatabaseSchema delegate() {
        return delegate;
    }

    @Override
    public TypedDatabaseSchema withInternallyGeneratedColumns() {
        final DatabaseSchema newDelegate = delegate.withInternallyGeneratedColumns();
        return newDelegate == delegate ? this : new TypedDatabaseSchemaImpl(newDelegate);
    }

    @Override
    public GenericTableSchema getTableSchema(final Class<?> klazz) {
        return TyperUtils.getTableSchema(this, klazz);
    }

    @Override
    public <T> T getTypedRowWrapper(final Class<T> klazz, final Row<GenericTableSchema> row) {
        // Check validity of  of the parameter passed to getTypedRowWrapper:
        // -  checks for a valid Database Schema matching the expected Database for a given table
        // - checks for the presence of the Table in Database Schema.
        final String dbName = TypedReflections.getTableDatabase(klazz);
        if (dbName != null && !dbName.equalsIgnoreCase(getName())) {
            return null;
        }
        TyperUtils.checkVersion(getVersion(), TypedReflections.getTableVersionRange(klazz));

        if (row != null) {
            row.setTableSchema(getTableSchema(klazz));
        }
        return Reflection.newProxy(klazz, new TypedRowInvocationHandler(klazz, this, row));
    }

    @Override
    public <T> Map<UUID, T> extractRowsOld(final Class<T> klazz, final TableUpdates updates) {
        Map<UUID,TableUpdate<GenericTableSchema>.RowUpdate<GenericTableSchema>> rowUpdates = extractRowUpdates(
            requireNonNull(klazz), requireNonNull(updates));
        Map<UUID,T> result = new HashMap<>();
        for (TableUpdate<GenericTableSchema>.RowUpdate<GenericTableSchema> rowUpdate : rowUpdates.values()) {
            if (rowUpdate != null && rowUpdate.getOld() != null) {
                Row<GenericTableSchema> row = rowUpdate.getOld();
                result.put(rowUpdate.getUuid(), getTypedRowWrapper(klazz, row));
            }
        }
        return result;
    }

    @Override
    public <T> Map<UUID, T> extractRowsUpdated(final Class<T> klazz, final TableUpdates updates) {
        final Map<UUID,TableUpdate<GenericTableSchema>.RowUpdate<GenericTableSchema>> rowUpdates =
                extractRowUpdates(klazz, updates);
        final Map<UUID,T> result = new HashMap<>();
        for (TableUpdate<GenericTableSchema>.RowUpdate<GenericTableSchema> rowUpdate : rowUpdates.values()) {
            if (rowUpdate != null && rowUpdate.getNew() != null) {
                Row<GenericTableSchema> row = rowUpdate.getNew();
                result.put(rowUpdate.getUuid(), getTypedRowWrapper(klazz, row));
            }
        }
        return result;
    }

    @Override
    public <T> Map<UUID, T> extractRowsRemoved(final Class<T> klazz, final TableUpdates updates) {
        final Map<UUID, TableUpdate<GenericTableSchema>.RowUpdate<GenericTableSchema>> rowUpdates =
                extractRowUpdates(klazz, updates);
        final Map<UUID, T> result = new HashMap<>();
        for (TableUpdate<GenericTableSchema>.RowUpdate<GenericTableSchema> rowUpdate : rowUpdates.values()) {
            if (rowUpdate != null && rowUpdate.getNew() == null && rowUpdate.getOld() != null) {
                Row<GenericTableSchema> row = rowUpdate.getOld();
                result.put(rowUpdate.getUuid(), getTypedRowWrapper(klazz, row));
            }
        }
        return result;
    }

    /**
     * This method extracts all RowUpdates of Class&lt;T&gt; klazz from a TableUpdates that correspond to rows of type
     * klazz. Example:
     * <code>
     * Map&lt;UUID,TableUpdate&lt;GenericTableSchema&gt;.RowUpdate&lt;GenericTableSchema&gt;&gt; updatedBridges =
     *     extractRowsUpdates(Bridge.class,updates,dbSchema)
     * </code>
     *
     * @param klazz Class for row type to be extracted
     * @param updates TableUpdates from which to extract rowUpdates
     * @return Map&lt;UUID,TableUpdate&lt;GenericTableSchema&gt;.RowUpdate&lt;GenericTableSchema&gt;&gt;
     *     for the type of things being sought
     */
    private Map<UUID,TableUpdate<GenericTableSchema>.RowUpdate<GenericTableSchema>> extractRowUpdates(
            final Class<?> klazz, final TableUpdates updates) {
        TableUpdate<GenericTableSchema> update = updates.getUpdate(table(TypedReflections.getTableName(klazz),
            GenericTableSchema.class));
        Map<UUID, TableUpdate<GenericTableSchema>.RowUpdate<GenericTableSchema>> result = new HashMap<>();
        if (update != null) {
            Map<UUID, TableUpdate<GenericTableSchema>.RowUpdate<GenericTableSchema>> rows = update.getRows();
            if (rows != null) {
                result = rows;
            }
        }
        return result;
    }
}
