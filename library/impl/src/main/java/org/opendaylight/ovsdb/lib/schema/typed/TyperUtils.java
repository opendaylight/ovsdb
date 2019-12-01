/*
 * Copyright © 2014, 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.lib.schema.typed;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Range;
import com.google.common.reflect.Reflection;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.opendaylight.ovsdb.lib.error.ColumnSchemaNotFoundException;
import org.opendaylight.ovsdb.lib.error.SchemaVersionMismatchException;
import org.opendaylight.ovsdb.lib.error.TableSchemaNotFoundException;
import org.opendaylight.ovsdb.lib.error.TyperException;
import org.opendaylight.ovsdb.lib.error.UnsupportedMethodException;
import org.opendaylight.ovsdb.lib.message.TableUpdate;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.notation.Version;
import org.opendaylight.ovsdb.lib.schema.ColumnSchema;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;

/**
 * Utility methods for typed OVSDB schema data.
 */
public final class TyperUtils {

    private static final String GET_STARTS_WITH = "get";
    private static final String SET_STARTS_WITH = "set";
    private static final String GETCOLUMN_ENDS_WITH = "Column";
    private static final String GETROW_ENDS_WITH = "Row";

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
    public static GenericTableSchema getTableSchema(final DatabaseSchema dbSchema, final Class<?> klazz) {
        return dbSchema.table(TypedReflections.getTableName(klazz), GenericTableSchema.class);
    }

    public static ColumnSchema<GenericTableSchema, Object> getColumnSchema(final GenericTableSchema tableSchema,
            final String columnName, final Class<Object> metaClass) {
        return tableSchema.column(columnName, metaClass);
    }

    private static String getColumnName(final Method method) {
        TypedColumn typedColumn = method.getAnnotation(TypedColumn.class);
        if (typedColumn != null) {
            return typedColumn.name();
        }

        /*
         * Attempting to get the column name by parsing the method name with a following convention :
         * 1. GETDATA : get<ColumnName>
         * 2. SETDATA : set<ColumnName>
         * 3. GETCOLUMN : get<ColumnName>Column
         * where <ColumnName> is the name of the column that we are interested in.
         */
        int index = GET_STARTS_WITH.length();
        if (isGetData(method) || isSetData(method)) {
            return method.getName().substring(index, method.getName().length()).toLowerCase(Locale.ROOT);
        } else if (isGetColumn(method)) {
            return method.getName().substring(index, method.getName().indexOf(GETCOLUMN_ENDS_WITH,
                    index)).toLowerCase(Locale.ROOT);
        }

        return null;
    }

    private static boolean isGetTableSchema(final Method method) {
        TypedColumn typedColumn = method.getAnnotation(TypedColumn.class);
        return typedColumn != null && typedColumn.method().equals(MethodType.GETTABLESCHEMA);
    }

    private static boolean isGetRow(final Method method) {
        TypedColumn typedColumn = method.getAnnotation(TypedColumn.class);
        if (typedColumn != null) {
            return typedColumn.method().equals(MethodType.GETROW);
        }

        return method.getName().startsWith(GET_STARTS_WITH) && method.getName().endsWith(GETROW_ENDS_WITH);
    }

    private static boolean isGetColumn(final Method method) {
        TypedColumn typedColumn = method.getAnnotation(TypedColumn.class);
        if (typedColumn != null) {
            return typedColumn.method().equals(MethodType.GETCOLUMN);
        }

        return method.getName().startsWith(GET_STARTS_WITH) && method.getName().endsWith(GETCOLUMN_ENDS_WITH);
    }

    private static boolean isGetData(final Method method) {
        TypedColumn typedColumn = method.getAnnotation(TypedColumn.class);
        if (typedColumn != null) {
            return typedColumn.method().equals(MethodType.GETDATA);
        }

        return method.getName().startsWith(GET_STARTS_WITH) && !method.getName().endsWith(GETCOLUMN_ENDS_WITH);
    }

    private static boolean isSetData(final Method method) {
        TypedColumn typedColumn = method.getAnnotation(TypedColumn.class);
        if (typedColumn != null) {
            return typedColumn.method().equals(MethodType.SETDATA);
        }

        return method.getName().startsWith(SET_STARTS_WITH);
    }

    /**
     * Method that checks validity of the parameter passed to getTypedRowWrapper.
     * This method checks for a valid Database Schema matching the expected Database for a given table
     * and checks for the presence of the Table in Database Schema.
     *
     * @param dbSchema DatabaseSchema as learnt from a OVSDB connection
     * @param klazz Typed Class that represents a Table
     * @return true if valid, false otherwise
     */
    private static <T> boolean isValid(final DatabaseSchema dbSchema, final Class<T> klazz) {
        if (dbSchema == null) {
            return false;
        }

        final String dbName = TypedReflections.getTableDatabase(klazz);
        if (dbName != null && !dbSchema.getName().equalsIgnoreCase(dbName)) {
            return false;
        }

        checkTableSchemaVersion(dbSchema, klazz);

        return true;
    }

    private static void checkColumnSchemaVersion(final DatabaseSchema dbSchema, final Method method) {
        checkVersion(dbSchema.getVersion(), TypedReflections.getColumnVersionRange(method));
    }

    private static <T> void checkTableSchemaVersion(final DatabaseSchema dbSchema, final Class<T> klazz) {
        checkVersion(dbSchema.getVersion(), TypedReflections.getTableVersionRange(klazz));
    }

    @VisibleForTesting
    static void checkVersion(final Version schemaVersion, final Range<Version> range) {
        if (!range.contains(schemaVersion)) {
            throw new SchemaVersionMismatchException(schemaVersion,
                range.hasLowerBound() ? range.lowerEndpoint() : Version.NULL,
                        range.hasUpperBound() ? range.upperEndpoint() : Version.NULL);
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
    public static <T> T getTypedRowWrapper(final DatabaseSchema dbSchema, final Class<T> klazz) {
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
    public static <T> T getTypedRowWrapper(final DatabaseSchema dbSchema, final Class<T> klazz,
                                           final Row<GenericTableSchema> row) {
        if (!isValid(dbSchema, klazz)) {
            return null;
        }
        if (row != null) {
            row.setTableSchema(getTableSchema(dbSchema, klazz));
        }
        return Reflection.newProxy(klazz, new InvocationHandler() {
            private Object processGetData(final Method method) {
                String columnName = getColumnName(method);
                checkColumnSchemaVersion(dbSchema, method);
                if (columnName == null) {
                    throw new TyperException("Error processing Getter : " + method.getName());
                }
                GenericTableSchema tableSchema = getTableSchema(dbSchema, klazz);
                if (tableSchema == null) {
                    String message = TableSchemaNotFoundException.createMessage(TypedReflections.getTableName(klazz),
                                dbSchema.getName());
                    throw new TableSchemaNotFoundException(message);
                }
                ColumnSchema<GenericTableSchema, Object> columnSchema =
                        getColumnSchema(tableSchema, columnName, (Class<Object>) method.getReturnType());
                if (columnSchema == null) {
                    String message = ColumnSchemaNotFoundException.createMessage(columnName, tableSchema.getName());
                    throw new ColumnSchemaNotFoundException(message);
                }
                if (row == null || row.getColumn(columnSchema) == null) {
                    return null;
                }
                return row.getColumn(columnSchema).getData();
            }

            private Object processGetRow() {
                return row;
            }

            private Object processGetColumn(final Method method) {
                String columnName = getColumnName(method);
                checkColumnSchemaVersion(dbSchema, method);
                if (columnName == null) {
                    throw new TyperException("Error processing GetColumn : " + method.getName());
                }
                GenericTableSchema tableSchema = getTableSchema(dbSchema, klazz);
                if (tableSchema == null) {
                    String message = TableSchemaNotFoundException.createMessage(TypedReflections.getTableName(klazz),
                        dbSchema.getName());
                    throw new TableSchemaNotFoundException(message);
                }
                ColumnSchema<GenericTableSchema, Object> columnSchema =
                        getColumnSchema(tableSchema, columnName, (Class<Object>) method.getReturnType());
                if (columnSchema == null) {
                    String message = ColumnSchemaNotFoundException.createMessage(columnName, tableSchema.getName());
                    throw new ColumnSchemaNotFoundException(message);
                }
                // When the row is null, that might indicate that the user maybe interested
                // only in the ColumnSchema and not on the Data.
                if (row == null) {
                    return new Column<>(columnSchema, null);
                }
                return row.getColumn(columnSchema);
            }

            private Object processSetData(final Object proxy, final Method method, final Object[] args) {
                if (args == null || args.length != 1) {
                    throw new TyperException("Setter method : " + method.getName() + " requires 1 argument");
                }
                checkColumnSchemaVersion(dbSchema, method);
                String columnName = getColumnName(method);
                if (columnName == null) {
                    throw new TyperException("Unable to locate Column Name for " + method.getName());
                }
                GenericTableSchema tableSchema = getTableSchema(dbSchema, klazz);
                ColumnSchema<GenericTableSchema, Object> columnSchema =
                        getColumnSchema(tableSchema, columnName, (Class<Object>) args[0].getClass());
                Column<GenericTableSchema, Object> column =
                        new Column<>(columnSchema, args[0]);
                row.addColumn(columnName, column);
                return proxy;
            }

            private GenericTableSchema processGetTableSchema() {
                return dbSchema == null ? null : getTableSchema(dbSchema, klazz);
            }

            private Boolean isHashCodeMethod(final Method method, final Object[] args) {
                return (args == null || args.length == 0) && method.getName().equals("hashCode");
            }

            private Boolean isEqualsMethod(final Method method, final Object[] args) {
                return args != null
                        && args.length == 1
                        && method.getName().equals("equals")
                        && Object.class.equals(method.getParameterTypes()[0]);
            }

            private Boolean isToStringMethod(final Method method, final Object[] args) {
                return (args == null || args.length == 0) && method.getName().equals("toString");
            }

            @Override
            public Object invoke(final Object proxy, final Method method, final Object[] args) throws Exception {
                if (isGetTableSchema(method)) {
                    return processGetTableSchema();
                } else if (isGetRow(method)) {
                    return processGetRow();
                } else if (isSetData(method)) {
                    return processSetData(proxy, method, args);
                } else if (isGetData(method)) {
                    return processGetData(method);
                } else if (isGetColumn(method)) {
                    return processGetColumn(method);
                } else if (isHashCodeMethod(method, args)) {
                    return processHashCode();
                } else if (isEqualsMethod(method, args)) {
                    return proxy.getClass().isInstance(args[0]) && processEquals(args[0]);
                } else if (isToStringMethod(method, args)) {
                    return processToString();
                }
                throw new UnsupportedMethodException("Method not supported " + method.toString());
            }

            private boolean processEquals(final Object obj) {
                return obj instanceof TypedBaseTable && Objects.equal(row, ((TypedBaseTable<?>)obj).getRow());
            }

            private int processHashCode() {
                return row == null ? 0 : row.hashCode();
            }

            private String processToString() {
                final GenericTableSchema schema = processGetTableSchema();
                final String tableName = schema != null ? schema.getName() : "";
                return row == null ? tableName : tableName + " : " + row.toString();
            }
        });
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
    public static <T> Map<UUID,T> extractRowsUpdated(final Class<T> klazz, final TableUpdates updates,
            final DatabaseSchema dbSchema) {
        Preconditions.checkNotNull(klazz);
        Preconditions.checkNotNull(updates);
        Preconditions.checkNotNull(dbSchema);
        Map<UUID,T> result = new HashMap<>();
        Map<UUID,TableUpdate<GenericTableSchema>.RowUpdate<GenericTableSchema>> rowUpdates =
                extractRowUpdates(klazz,updates,dbSchema);
        for (TableUpdate<GenericTableSchema>.RowUpdate<GenericTableSchema> rowUpdate : rowUpdates.values()) {
            if (rowUpdate != null && rowUpdate.getNew() != null) {
                Row<GenericTableSchema> row = rowUpdate.getNew();
                result.put(rowUpdate.getUuid(),TyperUtils.getTypedRowWrapper(dbSchema,klazz,row));
            }
        }
        return result;
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
    public static <T> Map<UUID, T> extractRowsOld(final Class<T> klazz, final TableUpdates updates,
            final DatabaseSchema dbSchema) {
        Preconditions.checkNotNull(klazz);
        Preconditions.checkNotNull(updates);
        Preconditions.checkNotNull(dbSchema);
        Map<UUID,T> result = new HashMap<>();
        Map<UUID,TableUpdate<GenericTableSchema>.RowUpdate<GenericTableSchema>> rowUpdates =
                extractRowUpdates(klazz,updates,dbSchema);
        for (TableUpdate<GenericTableSchema>.RowUpdate<GenericTableSchema> rowUpdate : rowUpdates.values()) {
            if (rowUpdate != null && rowUpdate.getOld() != null) {
                Row<GenericTableSchema> row = rowUpdate.getOld();
                result.put(rowUpdate.getUuid(),TyperUtils.getTypedRowWrapper(dbSchema,klazz,row));
            }
        }
        return result;
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
        Preconditions.checkNotNull(klazz);
        Preconditions.checkNotNull(updates);
        Preconditions.checkNotNull(dbSchema);
        Map<UUID,T> result = new HashMap<>();
        Map<UUID,TableUpdate<GenericTableSchema>.RowUpdate<GenericTableSchema>> rowUpdates =
                extractRowUpdates(klazz,updates,dbSchema);
        for (TableUpdate<GenericTableSchema>.RowUpdate<GenericTableSchema> rowUpdate : rowUpdates.values()) {
            if (rowUpdate != null && rowUpdate.getNew() == null && rowUpdate.getOld() != null) {
                Row<GenericTableSchema> row = rowUpdate.getOld();
                result.put(rowUpdate.getUuid(),TyperUtils.getTypedRowWrapper(dbSchema,klazz,row));
            }
        }
        return result;
    }

    /**
     * This method extracts all RowUpdates of Class&lt;T&gt; klazz from a TableUpdates
     * that correspond to rows of type klazz.
     * Example:
     * <code>
     * Map&lt;UUID,TableUpdate&lt;GenericTableSchema&gt;.RowUpdate&lt;GenericTableSchema&gt;&gt; updatedBridges =
     *     extractRowsUpdates(Bridge.class,updates,dbSchema)
     * </code>
     *
     * @param klazz Class for row type to be extracted
     * @param updates TableUpdates from which to extract rowUpdates
     * @param dbSchema Dbschema for the TableUpdates
     * @return Map&lt;UUID,TableUpdate&lt;GenericTableSchema&gt;.RowUpdate&lt;GenericTableSchema&gt;&gt;
     *     for the type of things being sought
     */
    static Map<UUID,TableUpdate<GenericTableSchema>.RowUpdate<GenericTableSchema>> extractRowUpdates(
            final Class<?> klazz,final TableUpdates updates,final DatabaseSchema dbSchema) {
        Preconditions.checkNotNull(klazz);
        Preconditions.checkNotNull(updates);
        Preconditions.checkNotNull(dbSchema);
        Map<UUID, TableUpdate<GenericTableSchema>.RowUpdate<GenericTableSchema>> result =
                new HashMap<>();
        TableUpdate<GenericTableSchema> update = updates.getUpdate(TyperUtils.getTableSchema(dbSchema, klazz));
        if (update != null) {
            Map<UUID, TableUpdate<GenericTableSchema>.RowUpdate<GenericTableSchema>> rows = update.getRows();
            if (rows != null) {
                result = rows;
            }
        }
        return result;
    }
}
