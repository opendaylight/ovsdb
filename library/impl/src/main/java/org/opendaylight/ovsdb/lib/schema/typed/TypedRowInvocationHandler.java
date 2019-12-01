/*
 * Copyright © 2014, 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib.schema.typed;

import static java.util.Objects.requireNonNull;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Objects;
import org.opendaylight.ovsdb.lib.error.ColumnSchemaNotFoundException;
import org.opendaylight.ovsdb.lib.error.TableSchemaNotFoundException;
import org.opendaylight.ovsdb.lib.error.TyperException;
import org.opendaylight.ovsdb.lib.error.UnsupportedMethodException;
import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.schema.ColumnSchema;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;

final class TypedRowInvocationHandler implements InvocationHandler {
    private static final String GET_STARTS_WITH = "get";
    private static final String SET_STARTS_WITH = "set";
    private static final String GETCOLUMN_ENDS_WITH = "Column";
    private static final String GETROW_ENDS_WITH = "Row";

    private final Class<?> target;
    private final DatabaseSchema dbSchema;
    private final Row<GenericTableSchema> row;

    TypedRowInvocationHandler(final Class<?> target, final DatabaseSchema dbSchema,
            final Row<GenericTableSchema> row) {
        this.target = requireNonNull(target);
        this.dbSchema = requireNonNull(dbSchema);
        this.row = row;
    }

    private Object processGetData(final Method method) {
        String columnName = getColumnName(method);
        checkColumnSchemaVersion(dbSchema, method);
        if (columnName == null) {
            throw new TyperException("Error processing Getter : " + method.getName());
        }
        GenericTableSchema tableSchema = TyperUtils.getTableSchema(dbSchema, target);
        if (tableSchema == null) {
            String message = TableSchemaNotFoundException.createMessage(TypedReflections.getTableName(target),
                        dbSchema.getName());
            throw new TableSchemaNotFoundException(message);
        }
        ColumnSchema<GenericTableSchema, Object> columnSchema =
                TyperUtils.getColumnSchema(tableSchema, columnName, (Class<Object>) method.getReturnType());
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
        GenericTableSchema tableSchema = TyperUtils.getTableSchema(dbSchema, target);
        if (tableSchema == null) {
            String message = TableSchemaNotFoundException.createMessage(TypedReflections.getTableName(target),
                dbSchema.getName());
            throw new TableSchemaNotFoundException(message);
        }
        ColumnSchema<GenericTableSchema, Object> columnSchema =
                TyperUtils.getColumnSchema(tableSchema, columnName, (Class<Object>) method.getReturnType());
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
        GenericTableSchema tableSchema = TyperUtils.getTableSchema(dbSchema, target);
        ColumnSchema<GenericTableSchema, Object> columnSchema =
                TyperUtils.getColumnSchema(tableSchema, columnName, (Class<Object>) args[0].getClass());
        Column<GenericTableSchema, Object> column =
                new Column<>(columnSchema, args[0]);
        row.addColumn(columnName, column);
        return proxy;
    }

    private GenericTableSchema processGetTableSchema() {
        return TyperUtils.getTableSchema(dbSchema, target);
    }

    private static Boolean isHashCodeMethod(final Method method, final Object[] args) {
        return (args == null || args.length == 0) && method.getName().equals("hashCode");
    }

    private static Boolean isEqualsMethod(final Method method, final Object[] args) {
        return args != null
                && args.length == 1
                && method.getName().equals("equals")
                && Object.class.equals(method.getParameterTypes()[0]);
    }

    private static Boolean isToStringMethod(final Method method, final Object[] args) {
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
        return obj instanceof TypedBaseTable && Objects.equals(row, ((TypedBaseTable<?>)obj).getRow());
    }

    private int processHashCode() {
        return row == null ? 0 : row.hashCode();
    }

    private String processToString() {
        final GenericTableSchema schema = processGetTableSchema();
        final String tableName = schema != null ? schema.getName() : "";
        return row == null ? tableName : tableName + " : " + row.toString();
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

    private static boolean isGetRow(final Method method) {
        TypedColumn typedColumn = method.getAnnotation(TypedColumn.class);
        if (typedColumn != null) {
            return typedColumn.method().equals(MethodType.GETROW);
        }

        return method.getName().startsWith(GET_STARTS_WITH) && method.getName().endsWith(GETROW_ENDS_WITH);
    }

    private static boolean isGetTableSchema(final Method method) {
        TypedColumn typedColumn = method.getAnnotation(TypedColumn.class);
        return typedColumn != null && typedColumn.method().equals(MethodType.GETTABLESCHEMA);
    }

    private static boolean isSetData(final Method method) {
        TypedColumn typedColumn = method.getAnnotation(TypedColumn.class);
        if (typedColumn != null) {
            return typedColumn.method().equals(MethodType.SETDATA);
        }

        return method.getName().startsWith(SET_STARTS_WITH);
    }

    private static void checkColumnSchemaVersion(final DatabaseSchema dbSchema, final Method method) {
        TyperUtils.checkVersion(dbSchema.getVersion(), TypedReflections.getColumnVersionRange(method));
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
}
