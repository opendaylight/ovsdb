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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.reflect.Invokable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.opendaylight.ovsdb.lib.error.ParsingException;
import org.opendaylight.ovsdb.lib.notation.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@VisibleForTesting
public class DatabaseSchemaImpl implements DatabaseSchema {
    private static final Logger LOG = LoggerFactory.getLogger(DatabaseSchemaImpl.class);

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

    protected final boolean haveInternallyGeneratedColumns() {
        for (TableSchema tableSchema : tables.values()) {
            if (!tableSchema.haveInternallyGeneratedColumns()) {
                return false;
            }
        }
        return true;
    }
}
