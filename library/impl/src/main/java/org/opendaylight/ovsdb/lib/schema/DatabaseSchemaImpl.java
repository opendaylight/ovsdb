/*
 * Copyright (c) 2014, 2015 EBay Software Foundation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib.schema;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.reflect.Invokable;
import com.google.common.reflect.Reflection;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.opendaylight.ovsdb.lib.error.ParsingException;
import org.opendaylight.ovsdb.lib.message.TableUpdate;
import org.opendaylight.ovsdb.lib.message.TableUpdate.RowUpdate;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.notation.Version;
import org.opendaylight.ovsdb.lib.schema.typed.TypedReflections;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@VisibleForTesting
public class DatabaseSchemaImpl implements DatabaseSchema {
    private static final Logger LOG = LoggerFactory.getLogger(DatabaseSchemaImpl.class);

    private final LoadingCache<Class<?>, TypedRowInvocationHandler> handlers = CacheBuilder.newBuilder()
            .weakKeys().weakValues().build(new CacheLoader<Class<?>, TypedRowInvocationHandler>() {
                @Override
                public TypedRowInvocationHandler load(final Class<?> key) {
                    return MethodDispatch.forTarget(key).bindToSchema(DatabaseSchemaImpl.this);
                }
            });
    private final String name;
    private final Version version;
    private final ImmutableMap<String, TableSchema> tables;

    public DatabaseSchemaImpl(final String name, final Version version, final Map<String, TableSchema> tables) {
        this.name = requireNonNull(name);
        this.version = requireNonNull(version);
        this.tables = ImmutableMap.copyOf(tables);
    }

    //todo : this needs to move to a custom factory
    public static DatabaseSchema fromJson(final String dbName, final JsonNode json) {
        if (!json.isObject() || !json.has("tables")) {
            throw new ParsingException("bad DatabaseSchema root, expected \"tables\" as child but was not found");
        }
        if (!json.isObject() || !json.has("version")) {
            throw new ParsingException("bad DatabaseSchema root, expected \"version\" as child but was not found");
        }

        Version dbVersion = Version.fromString(json.get("version").asText());

        Map<String, TableSchema> tables = new HashMap<>();
        for (Iterator<Map.Entry<String, JsonNode>> iter = json.get("tables").fields(); iter.hasNext(); ) {
            Map.Entry<String, JsonNode> table = iter.next();
            LOG.trace("Read schema for table[{}]:{}", table.getKey(), table.getValue());

            //todo : this needs to done by a factory
            tables.put(table.getKey(), GenericTableSchema.fromJson(table.getKey(), table.getValue()));
        }

        return new DatabaseSchemaImpl(dbName, dbVersion, tables);
    }

    @Override
    public Set<String> getTables() {
        return tables.keySet();
    }

    @Override
    public boolean hasTable(final String table) {
        return tables.containsKey(table);
    }

    @Override
    public <E extends TableSchema<E>> E table(final String tableName, final Class<E> clazz) {
        TableSchema<E> table = tables.get(tableName);

        if (clazz.isInstance(table)) {
            return clazz.cast(table);
        }

        return createTableSchema(clazz, table);
    }

    protected <E extends TableSchema<E>> E createTableSchema(final Class<E> clazz, final TableSchema<E> table) {
        Constructor<E> declaredConstructor;
        try {
            declaredConstructor = clazz.getDeclaredConstructor(TableSchema.class);
        } catch (NoSuchMethodException e) {
            String message = String.format("Class %s does not have public constructor that accepts TableSchema object",
                    clazz);
            throw new IllegalArgumentException(message, e);
        }
        Invokable<E, E> invokable = Invokable.from(declaredConstructor);
        try {
            return invokable.invoke(null, table);
        } catch (InvocationTargetException | IllegalAccessException e) {
            String message = String.format("Not able to create instance of class %s using public constructor "
                    + "that accepts TableSchema object", clazz);
            throw new IllegalArgumentException(message, e);
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Version getVersion() {
        return version;
    }

    @Override
    public DatabaseSchema withInternallyGeneratedColumns() {
        return haveInternallyGeneratedColumns() ? this : new DatabaseSchemaImpl(name, version,
            Maps.transformValues(tables, TableSchema::withInternallyGeneratedColumns));
    }

    @Override
    public GenericTableSchema getTableSchema(final Class<?> klazz) {
        return getTableSchema(TypedReflections.getTableName(klazz));
    }

    private GenericTableSchema getTableSchema(final String tableName) {
        return table(tableName, GenericTableSchema.class);
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

        TypedRowInvocationHandler handler = handlers.getUnchecked(klazz);
        if (row != null) {
            row.setTableSchema(getTableSchema(handler.getTableName()));
            handler = handler.bindToRow(row);
        }
        return Reflection.newProxy(klazz, handler);
    }

    @Override
    public <T> Map<UUID, T> extractRowsOld(final Class<T> klazz, final TableUpdates updates) {
        Map<UUID,T> result = new HashMap<>();
        for (RowUpdate<GenericTableSchema> rowUpdate : extractRowUpdates(klazz, updates).values()) {
            if (rowUpdate != null && rowUpdate.getOld() != null) {
                Row<GenericTableSchema> row = rowUpdate.getOld();
                result.put(rowUpdate.getUuid(), getTypedRowWrapper(klazz, row));
            }
        }
        return result;
    }

    @Override
    public <T> Map<UUID, T> extractRowsUpdated(final Class<T> klazz, final TableUpdates updates) {
        final Map<UUID, T> result = new HashMap<>();
        for (RowUpdate<GenericTableSchema> rowUpdate : extractRowUpdates(klazz, updates).values()) {
            if (rowUpdate != null && rowUpdate.getNew() != null) {
                result.put(rowUpdate.getUuid(), getTypedRowWrapper(klazz, rowUpdate.getNew()));
            }
        }
        return result;
    }

    @Override
    public <T> Map<UUID, T> extractRowsRemoved(final Class<T> klazz, final TableUpdates updates) {
        final Map<UUID, T> result = new HashMap<>();
        for (RowUpdate<GenericTableSchema> rowUpdate : extractRowUpdates(klazz, updates).values()) {
            if (rowUpdate != null && rowUpdate.getNew() == null && rowUpdate.getOld() != null) {
                result.put(rowUpdate.getUuid(), getTypedRowWrapper(klazz, rowUpdate.getOld()));
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
    private Map<UUID, RowUpdate<GenericTableSchema>> extractRowUpdates(final Class<?> klazz,
            final TableUpdates updates) {
        TableUpdate<GenericTableSchema> update = updates.getUpdate(table(TypedReflections.getTableName(klazz),
            GenericTableSchema.class));
        Map<UUID, RowUpdate<GenericTableSchema>> result = new HashMap<>();
        if (update != null) {
            Map<UUID, RowUpdate<GenericTableSchema>> rows = update.getRows();
            if (rows != null) {
                result = rows;
            }
        }
        return result;
    }

    private boolean haveInternallyGeneratedColumns() {
        for (TableSchema tableSchema : tables.values()) {
            if (!tableSchema.haveInternallyGeneratedColumns()) {
                return false;
            }
        }
        return true;
    }
}
