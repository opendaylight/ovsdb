/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal
 */

package org.opendaylight.ovsdb.lib.schema.typed;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.Version;
import org.opendaylight.ovsdb.lib.schema.ColumnSchema;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;

import com.google.common.reflect.Reflection;

public class TyperUtils {
    private static final String GET_STARTS_WITH="get";
    private static final String SET_STARTS_WITH="set";
    private static final String GETCOLUMN_ENDS_WITH="Column";

    private static <T> String getTableName (Class<T> klazz) {
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

    public static ColumnSchema<GenericTableSchema, Object> getColumnSchema(GenericTableSchema tableSchema, String columnName, Class<Object> metaClass) {
        return tableSchema.column(columnName, metaClass);
    }

    private static String getColumnName (Method method) {
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
            return method.getName().substring(index, method.getName().indexOf(GETCOLUMN_ENDS_WITH, index)).toLowerCase();
        }

        return null;
    }

    private static boolean isGetColumn (Method method) {
        TypedColumn typedColumn = method.getAnnotation(TypedColumn.class);
        if (typedColumn != null) {
            return typedColumn.method().equals(MethodType.GETCOLUMN) ? true : false;
        }

        if (method.getName().startsWith(GET_STARTS_WITH) && method.getName().endsWith(GETCOLUMN_ENDS_WITH)) {
            return true;
        }
        return false;
    }

    private static boolean isGetData (Method method) {
        TypedColumn typedColumn = method.getAnnotation(TypedColumn.class);
        if (typedColumn != null) {
            return typedColumn.method().equals(MethodType.GETDATA) ? true : false;
        }

        if (method.getName().startsWith(GET_STARTS_WITH) && !method.getName().endsWith(GETCOLUMN_ENDS_WITH)) {
            return true;
        }
        return false;
    }

    private static boolean isSetData (Method method) {
        TypedColumn typedColumn = method.getAnnotation(TypedColumn.class);
        if (typedColumn != null) {
            return typedColumn.method().equals(MethodType.SETDATA) ? true : false;
        }

        if (method.getName().startsWith(SET_STARTS_WITH)) {
            return true;
        }
        return false;
    }

    public static Version getFromVersion (Method method) {
        TypedColumn typedColumn = method.getAnnotation(TypedColumn.class);
        if (typedColumn != null) {
            return Version.fromString(typedColumn.fromVersion());
        }
        TypedTable typedTable = method.getAnnotation(TypedTable.class);
        if (typedTable != null) {
            return Version.fromString(typedTable.fromVersion());
        }
        return Version.NULL;
    }

    public static Version getUntilVersion(Method method) {
        TypedColumn typedColumn = method.getAnnotation(TypedColumn.class);
        if (typedColumn != null) {
            return Version.fromString(typedColumn.untilVersion());
        }
        TypedTable typedTable = method.getAnnotation(TypedTable.class);
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
    private static <T> boolean isValid (DatabaseSchema dbSchema, final Class<T> klazz) {
        TypedTable typedTable = klazz.getAnnotation(TypedTable.class);
        if (typedTable != null) {
            if (!dbSchema.getName().equalsIgnoreCase(typedTable.database())) {
                return false;
            }
        }

        if (!dbSchema.getTables().contains(getTableName(klazz))) {
            return false;
        }
        return true;
    }

    private static void checkSchemaVersion(DatabaseSchema dbSchema, Method method) {
        Version fromVersion = getFromVersion(method);
        Version untilVersion = getUntilVersion(method);
        if (!fromVersion.equals(Version.NULL)) {
            if (dbSchema.getVersion().compareTo(fromVersion) < 0) {
                throw new RuntimeException("This row is not supported until version "
                        + fromVersion + "of the Schema");
            }
        }
        if (!untilVersion.equals(Version.NULL)) {
            if (dbSchema.getVersion().compareTo(untilVersion) > 0) {
                throw new RuntimeException("This row was deprecated in "
                        + untilVersion + "of the Schema");
            }
        }
    }

    /**
     * This method returns a Typed Proxy implementation for the klazz passed as a parameter.
     * Per design choice, the Typed Proxy implementation is just a Wrapper on top of the actual Row which is untyped.
     * Being just a wrapper, it is state-less and more of a convenience functionality to provide a type-safe infrastructure
     * for the applications to built on top of. And this Typed infra is completely optional.
     *
     * It is the applications responsibilty to pass on the raw Row parameter & this method will return the appropriate
     * Proxy wrapper for the passed klazz Type. The raw Row parameter may be null if the caller is interested in just the
     * ColumnSchema. But that is not a very common use-case.
     *
     * @param dbSchema DatabaseSchema as learnt from a OVSDB connection
     * @param klazz Typed Class that represents a Table
     * @param row The actual Row that the wrapper is operating on. It can be null if the caller is just interested in getting ColumnSchema.
     * @return
     */
    public static <T> T getTypedRowWrapper(final DatabaseSchema dbSchema, final Class<T> klazz, final Row<GenericTableSchema> row) {
        if (!isValid(dbSchema, klazz)) {
            return null;
        }
        return Reflection.newProxy(klazz, new InvocationHandler() {
            private Object processGetData(Method method) throws Throwable {
                String columnName = getColumnName(method);
                checkSchemaVersion(dbSchema, method);
                if (columnName == null) {
                    throw new RuntimeException("Error processing Getter : "+ method.getName());
                }
                GenericTableSchema tableSchema = getTableSchema(dbSchema, klazz);
                if (tableSchema == null) {
                    throw new RuntimeException("Unable to locate TableSchema for "+getTableName(klazz)+ " in "+ dbSchema.getName());
                }
                ColumnSchema<GenericTableSchema, Object> columnSchema = getColumnSchema(tableSchema, columnName, (Class<Object>) method.getReturnType());
                if (columnSchema == null) {
                    throw new RuntimeException("Unable to locate ColumnSchema for "+columnName+ " in "+ tableSchema.getName());
                }
                if (row == null) {
                    return null;
                }
                return row.getColumn(columnSchema).getData();
            }

            private Object processGetColumn(Method method) throws Throwable {
                String columnName = getColumnName(method);
                Version fromVersion = getFromVersion(method);
                Version untilVersion = getUntilVersion(method);
                checkSchemaVersion(dbSchema, method);
                if (columnName == null) {
                    throw new RuntimeException("Error processing GetColumn : "+ method.getName());
                }
                GenericTableSchema tableSchema = getTableSchema(dbSchema, klazz);
                if (tableSchema == null) {
                    throw new RuntimeException("Unable to locate TableSchema for "+getTableName(klazz)+ " in "+ dbSchema.getName());
                }
                ColumnSchema<GenericTableSchema, Object> columnSchema = getColumnSchema(tableSchema, columnName, (Class<Object>) method.getReturnType());
                if (columnSchema == null) {
                    throw new RuntimeException("Unable to locate ColumnSchema for "+columnName+ " in "+ tableSchema.getName());
                }
                // When the row is null, that might indicate that the user maybe interested only in the ColumnSchema and not on the Data.
                if (row == null) {
                    return new Column<GenericTableSchema, Object>(columnSchema, null);
                }
                return row.getColumn(columnSchema);
            }

            private Object processSetData(Object proxy, Method method, Object[] args) throws Throwable {
                if (args == null || args.length != 1) {
                    throw new RuntimeException("Setter method : "+method.getName() + " requires 1 argument");
                }
                checkSchemaVersion(dbSchema, method);
                String columnName = getColumnName(method);
                if (columnName == null) {
                    throw new RuntimeException("Unable to locate Column Name for "+method.getName());
                }
                GenericTableSchema tableSchema = getTableSchema(dbSchema, klazz);
                ColumnSchema<GenericTableSchema, Object> columnSchema = getColumnSchema(tableSchema, columnName,
                                                                                        (Class<Object>) args[0].getClass());
                Column<GenericTableSchema, Object> column = new Column<GenericTableSchema, Object>(columnSchema, args[0]);
                row.addColumn(columnName, column);
                return proxy;
            }

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if (isSetData(method)) {
                    return processSetData(proxy, method, args);
                } else if(isGetData(method)) {
                    return processGetData(method);
                } else if(isGetColumn(method)) {
                    return processGetColumn(method);
                } else {
                    throw new RuntimeException("Unsupported method : "+method.getName());
                }
            }
        }
        );
    }
}
