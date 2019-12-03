/*
 * Copyright (c) 2014, 2015 EBay Software Foundation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib.schema;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.Set;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.notation.Version;
import org.opendaylight.ovsdb.lib.schema.typed.TypedTable;

/**
 * Represents an ovsdb database schema, which is comprised of a set of tables.
 */
public interface DatabaseSchema {
    Set<String> getTables();

    boolean hasTable(String table);

    <E extends TableSchema<E>> E table(String tableName, Class<E> clazz);

    String getName();

    Version getVersion();

    DatabaseSchema withInternallyGeneratedColumns();

    /**
     * Retrieve the table schema for the given table in the given database schema.
     *
     * @param klazz The class whose table schema should be retrieved. Classes are matched in the database schema either
     *              using their {@link TypedTable} annotation, if they have one, or by name.
     * @return the table schema.
     */
    GenericTableSchema getTableSchema(Class<?> klazz);

    /**
     * Returns a Typed Proxy implementation for the klazz passed as a parameter.
     * Per design choice, the Typed Proxy implementation is just a Wrapper on top of the actual
     * Row which is untyped.
     *
     * <p>Being just a wrapper, it is state-less and more of a convenience functionality to
     * provide a type-safe infrastructure for the applications to built on top of.
     * And this Typed infra is completely optional.
     *
     * <p>It is the applications responsibility to pass on the raw Row parameter and this method will
     * return the appropriate Proxy wrapper for the passed klazz Type.
     * The raw Row parameter may be null if the caller is interested in just the ColumnSchema.
     * But that is not a very common use-case.
     *
     * @param klazz Typed Class that represents a Table
     */
    default <T> T getTypedRowWrapper(final Class<T> klazz) {
        return getTypedRowWrapper(klazz, new Row<>());
    }

    /**
     * Returns a Typed Proxy implementation for the klazz passed as a parameter.
     * Per design choice, the Typed Proxy implementation is just a Wrapper on top of the actual
     * Row which is untyped.
     *
     * <p>Being just a wrapper, it is state-less and more of a convenience functionality
     * to provide a type-safe infrastructure for the applications to built on top of.
     * And this Typed infra is completely optional.
     *
     * <p>It is the applications responsibility to pass on the raw Row parameter and this method
     * will return the appropriate Proxy wrapper for the passed klazz Type.
     * The raw Row parameter may be null if the caller is interested in just the
     * ColumnSchema. But that is not a very common use-case.
     *
     * @param klazz Typed Class that represents a Table
     * @param row The actual Row that the wrapper is operating on. It can be null if the caller
     *            is just interested in getting ColumnSchema.
     */
    <T> T getTypedRowWrapper(Class<T> klazz, Row<GenericTableSchema> row);

    /**
     * This method extracts all row updates of Class&lt;T&gt; klazz from a TableUpdates
     * that correspond to old version of rows of type klazz that have been updated.
     * Example:
     * <code>
     * Map&lt;UUID,Bridge&gt; oldBridges = extractRowsOld(Bridge.class,updates,dbSchema)
     * </code>
     *
     * @param klazz Class for row type to be extracted
     * @param updates TableUpdates from which to extract rowUpdates
     * @return Map&lt;UUID,T&gt; for the type of things being sought
     */
    <T> Map<UUID, T> extractRowsOld(Class<T> klazz, TableUpdates updates);

    /**
     * This method extracts all row updates of Class&lt;T&gt; klazz from a TableUpdates
     * that correspond to removal of rows of type klazz.
     * Example:
     * <code>
     * Map&lt;UUID,Bridge&gt; updatedBridges = extractRowsRemoved(Bridge.class,updates,dbSchema)
     * </code>
     *
     * @param klazz Class for row type to be extracted
     * @param updates TableUpdates from which to extract rowUpdates
     * @return Map&lt;UUID,T&gt; for the type of things being sought
     */
    <T> Map<UUID,T> extractRowsRemoved(Class<T> klazz, TableUpdates updates);

    /**
     * This method extracts all row updates of Class&lt;T&gt; klazz from a TableUpdates
     * that correspond to insertion or updates of rows of type klazz.
     * Example:
     * <code>
     * Map&lt;UUID,Bridge&gt; updatedBridges = extractRowsUpdated(Bridge.class,updates,dbSchema)
     * </code>
     *
     * @param klazz Class for row type to be extracted
     * @param updates TableUpdates from which to extract rowUpdates
     * @return Map&lt;UUID,T&gt; for the type of things being sought
     */
    <T> Map<UUID, T> extractRowsUpdated(Class<T> klazz, TableUpdates updates);

    static DatabaseSchema fromJson(final String dbName, final JsonNode json) {
        return DatabaseSchemaImpl.fromJson(dbName, json);
    }
}
