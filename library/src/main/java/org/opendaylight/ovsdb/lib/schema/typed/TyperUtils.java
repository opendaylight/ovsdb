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
import org.opendaylight.ovsdb.lib.schema.ColumnSchema;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;

import com.google.common.reflect.Reflection;

public class TyperUtils {
    private static <T> String getTableName (Class<T> klazz) {
        TypedTable typedTable = (TypedTable) klazz.getAnnotation(TypedTable.class);
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

        if (!method.getName().startsWith("get") && !method.getName().startsWith("set")) {
            return null;
        }
        return method.getName().substring("get".length(), method.getName().length()).toLowerCase();
    }

    private static boolean isGetter (Method method) {
        TypedColumn typedColumn = method.getAnnotation(TypedColumn.class);
        if (typedColumn != null) {
            return typedColumn.method().equals(MethodType.GETTER) ? true : false;
        }

        if (method.getName().startsWith("get")) {
            return true;
        }
        return false;
    }

    private static boolean isSetter (Method method) {
        TypedColumn typedColumn = method.getAnnotation(TypedColumn.class);
        if (typedColumn != null) {
            return typedColumn.method().equals(MethodType.SETTER) ? true : false;
        }

        if (method.getName().startsWith("set")) {
            return true;
        }
        return false;
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
        TypedTable typedTable = (TypedTable) klazz.getAnnotation(TypedTable.class);
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

    public static <T> T getTypedRowWrapper(final DatabaseSchema dbSchema, final Class<T> klazz, final Row<GenericTableSchema> row) {
        if (!isValid(dbSchema, klazz)) {
            return null;
        }
        return Reflection.newProxy(klazz, new InvocationHandler() {
            private Object processGetter(Method method) throws Throwable {
                String columnName = getColumnName(method);
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
                return row.getColumn(columnSchema);
            }

            private Object processSetter(Object proxy, Method method, Object[] args) throws Throwable {
                if (args == null || args.length != 1) {
                    throw new RuntimeException("Setter method : "+method.getName() + " requires 1 argument");
                }
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
                if (isSetter(method)) {
                    return processSetter(proxy, method, args);
                } else if(isGetter(method)) {
                    return processGetter(method);
                }
                // At the moment, we support just getters and setters for the columns.
                throw new RuntimeException("Unsupported method : "+method.getName());
            }
        }
        );
    }
}
