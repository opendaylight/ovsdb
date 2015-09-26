/*
 * Copyright (c) 2014, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.lib.schema.typed;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
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
import org.opendaylight.ovsdb.lib.schema.TableSchema;

import com.google.common.base.Preconditions;
import com.google.common.reflect.Reflection;

public class TyperUtils {
    private static final String GET_STARTS_WITH = "get";
    private static final String SET_STARTS_WITH = "set";
    private static final String GETCOLUMN_ENDS_WITH = "Column";
    private static final String GETROW_ENDS_WITH = "Row";

    private TyperUtils() {
        // Prevent instantiating a utility class
    }

    private static <T> String getTableName(Class<T> klazz) {
        TypedTable typedTable = klazz.getAnnotation(TypedTable.class);
        if (typedTable != null) {
            return typedTable.name();
        }
        return klazz.getSimpleName();
    }

    public static <T> GenericTableSchema getTableSchema(DatabaseSchema dbSchema, Class<T> klazz) {
        String tableName = getTableName(klazz);
        return dbSchema.table(tableName, GenericTableSchema.class);
    }

    public static ColumnSchema<GenericTableSchema, Object>
        getColumnSchema(GenericTableSchema tableSchema, String columnName, Class<Object> metaClass) {
        return tableSchema.column(columnName, metaClass);
    }

    private static String getColumnName(Method method) {
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
            return method.getName().substring(index, method.getName().length()).toLowerCase();
        } else if (isGetColumn(method)) {
            return method.getName().substring(index, method.getName().indexOf(GETCOLUMN_ENDS_WITH,
                    index)).toLowerCase();
        }

        return null;
    }

    private static boolean isGetTableSchema(Method method) {
        TypedColumn typedColumn = method.getAnnotation(TypedColumn.class);
        return typedColumn != null && typedColumn.method().equals(MethodType.GETTABLESCHEMA);
    }

    private static boolean isGetRow(Method method) {
        TypedColumn typedColumn = method.getAnnotation(TypedColumn.class);
        if (typedColumn != null) {
            return typedColumn.method().equals(MethodType.GETROW);
        }

        return method.getName().startsWith(GET_STARTS_WITH) && method.getName().endsWith(GETROW_ENDS_WITH);
    }

    private static boolean isGetColumn(Method method) {
        TypedColumn typedColumn = method.getAnnotation(TypedColumn.class);
        if (typedColumn != null) {
            return typedColumn.method().equals(MethodType.GETCOLUMN);
        }

        return method.getName().startsWith(GET_STARTS_WITH) && method.getName().endsWith(GETCOLUMN_ENDS_WITH);
    }

    private static boolean isGetData(Method method) {
        TypedColumn typedColumn = method.getAnnotation(TypedColumn.class);
        if (typedColumn != null) {
            return typedColumn.method().equals(MethodType.GETDATA);
        }

        return method.getName().startsWith(GET_STARTS_WITH) && !method.getName().endsWith(GETCOLUMN_ENDS_WITH);
    }

    private static boolean isSetData(Method method) {
        TypedColumn typedColumn = method.getAnnotation(TypedColumn.class);
        if (typedColumn != null) {
            return typedColumn.method().equals(MethodType.SETDATA);
        }

        return method.getName().startsWith(SET_STARTS_WITH);
    }

    public static Version getColumnFromVersion(Method method) {
        TypedColumn typedColumn = method.getAnnotation(TypedColumn.class);
        if (typedColumn != null) {
            return Version.fromString(typedColumn.fromVersion());
        }
        return Version.NULL;
    }

    public static <T> Version getTableFromVersion(final Class<T> klazz) {
        TypedTable typedTable = klazz.getAnnotation(TypedTable.class);
        if (typedTable != null) {
            return Version.fromString(typedTable.fromVersion());
        }
        return Version.NULL;
    }

    public static Version getColumnUntilVersion(Method method) {
        TypedColumn typedColumn = method.getAnnotation(TypedColumn.class);
        if (typedColumn != null) {
            return Version.fromString(typedColumn.untilVersion());
        }
        return Version.NULL;
    }

    public static <T> Version getTableUntilVersion(final Class<T> klazz) {
        TypedTable typedTable = klazz.getAnnotation(TypedTable.class);
        if (typedTable != null) {
            return Version.fromString(typedTable.untilVersion());
        }
        return Version.NULL;
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
    private static <T> boolean isValid(DatabaseSchema dbSchema, final Class<T> klazz) {
        if (dbSchema == null) {
            return false;
        }

        TypedTable typedTable = klazz.getAnnotation(TypedTable.class);
        if (typedTable != null && !dbSchema.getName().equalsIgnoreCase(typedTable.database())) {
            return false;
        }

        checkTableSchemaVersion(dbSchema, klazz);

        return true;
    }

    private static void checkColumnSchemaVersion(DatabaseSchema dbSchema, Method method) {
        Version fromVersion = getColumnFromVersion(method);
        Version untilVersion = getColumnUntilVersion(method);
        Version schemaVersion = dbSchema.getVersion();
        checkVersion(schemaVersion, fromVersion, untilVersion);
    }

    private static <T> void checkTableSchemaVersion(DatabaseSchema dbSchema, Class<T> klazz) {
        Version fromVersion = getTableFromVersion(klazz);
        Version untilVersion = getTableUntilVersion(klazz);
        Version schemaVersion = dbSchema.getVersion();
        checkVersion(schemaVersion, fromVersion, untilVersion);
    }

    private static void checkVersion(Version schemaVersion, Version fromVersion, Version untilVersion) {
        if (!fromVersion.equals(Version.NULL) && schemaVersion.compareTo(fromVersion) < 0) {
            String message = SchemaVersionMismatchException.createMessage(schemaVersion, fromVersion);
            throw new SchemaVersionMismatchException(message);
        }
        if (!untilVersion.equals(Version.NULL) && schemaVersion.compareTo(untilVersion) > 0) {
            String message = SchemaVersionMismatchException.createMessage(schemaVersion, untilVersion);
            throw new SchemaVersionMismatchException(message);
        }
    }

    /**
     * This method returns a Typed Proxy implementation for the klazz passed as a parameter.
     * Per design choice, the Typed Proxy implementation is just a Wrapper on top of the actual
     * Row which is untyped.
     * Being just a wrapper, it is state-less and more of a convenience functionality to
     * provide a type-safe infrastructure for the applications to built on top of.
     * And this Typed infra is completely optional.
     *
     * It is the applications responsibility to pass on the raw Row parameter and this method will
     * return the appropriate Proxy wrapper for the passed klazz Type.
     * The raw Row parameter may be null if the caller is interested in just the ColumnSchema.
     * But that is not a very common use-case.
     *
     * @param dbSchema DatabaseSchema as learnt from a OVSDB connection
     * @param klazz Typed Class that represents a Table
     * @return
     */
    public static <T> T getTypedRowWrapper(final DatabaseSchema dbSchema, final Class<T> klazz) {
        return getTypedRowWrapper(dbSchema, klazz,new Row<GenericTableSchema>());
    }

    /**
     * This method returns a Typed Proxy implementation for the klazz passed as a parameter.
     * Per design choice, the Typed Proxy implementation is just a Wrapper on top of the actual
     * Row which is untyped.
     * Being just a wrapper, it is state-less and more of a convenience functionality
     * to provide a type-safe infrastructure for the applications to built on top of.
     * And this Typed infra is completely optional.
     *
     * It is the applications responsibility to pass on the raw Row parameter and this method
     * will return the appropriate Proxy wrapper for the passed klazz Type.
     * The raw Row parameter may be null if the caller is interested in just the
     * ColumnSchema. But that is not a very common use-case.
     *
     * @param dbSchema DatabaseSchema as learnt from a OVSDB connection
     * @param klazz Typed Class that represents a Table
     * @param row The actual Row that the wrapper is operating on. It can be null if the caller
     *            is just interested in getting ColumnSchema.
     * @return
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
            private Object processGetData(Method method) {
                String columnName = getColumnName(method);
                checkColumnSchemaVersion(dbSchema, method);
                if (columnName == null) {
                    throw new TyperException("Error processing Getter : " + method.getName());
                }
                GenericTableSchema tableSchema = getTableSchema(dbSchema, klazz);
                if (tableSchema == null) {
                    String message =
                            TableSchemaNotFoundException.createMessage(getTableName(klazz), dbSchema.getName());
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

            private Object processGetColumn(Method method) {
                String columnName = getColumnName(method);
                checkColumnSchemaVersion(dbSchema, method);
                if (columnName == null) {
                    throw new TyperException("Error processing GetColumn : " + method.getName());
                }
                GenericTableSchema tableSchema = getTableSchema(dbSchema, klazz);
                if (tableSchema == null) {
                    String message =
                            TableSchemaNotFoundException.createMessage(getTableName(klazz), dbSchema.getName());
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
                    return new Column<GenericTableSchema, Object>(columnSchema, null);
                }
                return row.getColumn(columnSchema);
            }

            private Object processSetData(Object proxy, Method method, Object[] args) {
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

            private Object processGetTableSchema() {
                if (dbSchema == null) {
                    return null;
                }
                return getTableSchema(dbSchema, klazz);
            }

            private Boolean isHashCodeMethod(Method method, Object[] args) {
                return (args == null || args.length == 0) && method.getName().equals("hashCode");
            }
            private Boolean isEqualsMethod(Method method, Object[] args) {
                return (args != null
                        && args.length == 1
                        && method.getName().equals("equals")
                        && Object.class.equals(method.getParameterTypes()[0]));
            }
            private Boolean isToStringMethod(Method method, Object[] args) {
                return (args == null || args.length == 0) && method.getName().equals("toString");
            }
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
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
                    return hashCode();
                } else if (isEqualsMethod(method, args)) {
                    return proxy.getClass().isInstance(args[0]) && this.equals(args[0]);
                } else if (isToStringMethod(method, args)) {
                    return this.toString();
                }
                throw new UnsupportedMethodException("Method not supported " + method.toString());
            }

            @Override
            public boolean equals(Object obj) {
                if (obj == null) {
                    return false;
                }
                TypedBaseTable<?> typedRowObj = (TypedBaseTable<?>)obj;
                if (row == null && typedRowObj.getRow() == null) {
                    return true;
                }
                if (row.equals(typedRowObj.getRow())) {
                    return true;
                }
                return false;
            }

            @Override public int hashCode() {
                if (row == null) {
                    return 0;
                }
                return row.hashCode();
            }

            @Override public String toString() {
                String tableName;
                try {
                    TableSchema<?> schema = (TableSchema<?>)processGetTableSchema();
                    tableName = schema.getName();
                } catch (Exception e) {
                    tableName = "";
                }
                if (row == null) {
                    return tableName;
                }
                return tableName + " : " + row.toString();
            }
        }
        );
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
    public static <T> Map<UUID,T> extractRowsUpdated(Class<T> klazz,TableUpdates updates,DatabaseSchema dbSchema) {
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
     * that correspond to old version of rows of type klazz that have been updated
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
    public static <T> Map<UUID,T> extractRowsOld(Class<T> klazz,TableUpdates updates,DatabaseSchema dbSchema) {
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
    public static <T> Map<UUID,T> extractRowsRemoved(Class<T> klazz,TableUpdates updates,DatabaseSchema dbSchema) {
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
    public static Map<UUID,TableUpdate<GenericTableSchema>.RowUpdate<GenericTableSchema>>
        extractRowUpdates(Class<?> klazz,TableUpdates updates,DatabaseSchema dbSchema) {
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
