/*
 * Copyright © 2014, 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib.schema.typed;

import com.google.common.collect.Range;
import java.util.Map;
import org.opendaylight.ovsdb.lib.error.SchemaVersionMismatchException;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.notation.Version;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;

/**
 * Utility methods for typed OVSDB schema data.
 */
public final class TyperUtils {
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
        return dbSchema.getTableSchema(klazz);
    }

    static void checkVersion(final Version schemaVersion, final Range<Version> range) {
        if (!range.contains(schemaVersion)) {
            throw new SchemaVersionMismatchException(schemaVersion, range);
        }
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
    @Deprecated
    public static <T> Map<UUID,T> extractRowsUpdated(final Class<T> klazz, final TableUpdates updates,
            final DatabaseSchema dbSchema) {
        return dbSchema.extractRowsUpdated(klazz, updates);
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
    @Deprecated
    public static <T> Map<UUID, T> extractRowsOld(final Class<T> klazz, final TableUpdates updates,
            final DatabaseSchema dbSchema) {
        return dbSchema.extractRowsOld(klazz, updates);
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
    public static <T> Map<UUID,T> extractRowsRemoved(final Class<T> klazz, final TableUpdates updates,
            final DatabaseSchema dbSchema) {
        return dbSchema.extractRowsRemoved(klazz, updates);
    }
}
