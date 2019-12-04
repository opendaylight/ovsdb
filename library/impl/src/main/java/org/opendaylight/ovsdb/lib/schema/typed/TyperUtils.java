/*
 * Copyright © 2014, 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib.schema.typed;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Range;
import java.util.Map;
import org.opendaylight.ovsdb.lib.error.SchemaVersionMismatchException;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.notation.Version;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;

/**
 * Utility methods for typed OVSDB schema data.
 */
public final class TyperUtils {
    private static final LoadingCache<DatabaseSchema, TypedDatabaseSchema> TYPED_CACHE = CacheBuilder.newBuilder()
            .weakKeys().weakValues().build(new CacheLoader<DatabaseSchema, TypedDatabaseSchema>() {
                @Override
                public TypedDatabaseSchema load(final DatabaseSchema key) {
                    return new TypedDatabaseSchemaImpl(key);
                }
            });

    private TyperUtils() {
        // Prevent instantiating a utility class
    }

    /**
     * Retrieve the table schema for the given table in the given database schema.
     *
     * @param dbSchema The database schema.
     * @param klazz The class whose table schema should be retrieved. Classes are matched in the database schema either
     *     using their {@link TypedTable} annotation, if they have one, or by name.
     * @return the table schema.
     */
    @Deprecated
    public static GenericTableSchema getTableSchema(final DatabaseSchema dbSchema, final Class<?> klazz) {
        return getTyped(dbSchema).getTableSchema(klazz);
    }

    static void checkVersion(final Version schemaVersion, final Range<Version> range) {
        if (!range.contains(schemaVersion)) {
            throw new SchemaVersionMismatchException(schemaVersion, range);
        }
    }

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
     * @param dbSchema DatabaseSchema as learnt from a OVSDB connection
     * @param klazz Typed Class that represents a Table
     */
    @Deprecated
    public static <T extends TypedBaseTable<?>> T getTypedRowWrapper(final DatabaseSchema dbSchema,
            final Class<T> klazz) {
        return getTypedRowWrapper(dbSchema, klazz, new Row<>());
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
     * @param dbSchema DatabaseSchema as learnt from a OVSDB connection
     * @param klazz Typed Class that represents a Table
     * @param row The actual Row that the wrapper is operating on. It can be null if the caller
     *            is just interested in getting ColumnSchema.
     */
    @Deprecated
    public static <T extends TypedBaseTable<?>> T getTypedRowWrapper(final DatabaseSchema dbSchema,
            final Class<T> klazz, final Row<GenericTableSchema> row) {
        return dbSchema == null ? null : getTyped(dbSchema).getTypedRowWrapper(klazz, row);
    }

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
     * @param dbSchema Dbschema for the TableUpdates
     * @return Map&lt;UUID,T&gt; for the type of things being sought
     */
    public static <T extends TypedBaseTable<?>> Map<UUID,T> extractRowsUpdated(final Class<T> klazz,
            final TableUpdates updates, final DatabaseSchema dbSchema) {
        return getTyped(dbSchema).extractRowsUpdated(klazz, updates);
    }

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
     * @param dbSchema Dbschema for the TableUpdates
     * @return Map&lt;UUID,T&gt; for the type of things being sought
     */
    public static <T extends TypedBaseTable<?>> Map<UUID, T> extractRowsOld(final Class<T> klazz,
            final TableUpdates updates, final DatabaseSchema dbSchema) {
        return getTyped(dbSchema).extractRowsOld(klazz, updates);
    }

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
     * @param dbSchema Dbschema for the TableUpdates
     * @return Map&lt;UUID,T&gt; for the type of things being sought
     */
    public static <T extends TypedBaseTable<?>> Map<UUID,T> extractRowsRemoved(final Class<T> klazz,
            final TableUpdates updates, final DatabaseSchema dbSchema) {
        return getTyped(dbSchema).extractRowsRemoved(klazz, updates);
    }

    private static TypedDatabaseSchema getTyped(final DatabaseSchema dbSchema) {
        return dbSchema instanceof TypedDatabaseSchema ? (TypedDatabaseSchema) dbSchema
                : TYPED_CACHE.getUnchecked(dbSchema);
    }
}
