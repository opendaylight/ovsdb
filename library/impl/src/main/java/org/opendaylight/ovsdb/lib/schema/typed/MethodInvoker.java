/*
 * Copyright © 2019 PANTHEON.tech, s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib.schema.typed;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.Range;
import java.lang.reflect.Method;
import java.util.Locale;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.ovsdb.lib.error.ColumnSchemaNotFoundException;
import org.opendaylight.ovsdb.lib.error.TableSchemaNotFoundException;
import org.opendaylight.ovsdb.lib.error.TyperException;
import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.Version;
import org.opendaylight.ovsdb.lib.schema.ColumnSchema;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;

@NonNullByDefault
abstract class MethodInvoker {
    private abstract static class VersionedMethodInvoker extends MethodInvoker {
        private final Range<Version> supportedVersions;

        VersionedMethodInvoker(final Range<Version> supportedVersions) {
            this.supportedVersions = requireNonNull(supportedVersions);
        }

        @Override
        final @Nullable Object invokeMethod(final DatabaseSchema dbSchema,
                final Row<GenericTableSchema> row, final Object proxy, final Object[] args) {
            // TODO: this is Class/DatabaseSchema invariant and should therefore be cached in TypedDatabaseSchema
            //       with an appropriate cache
            TyperUtils.checkVersion(dbSchema.getVersion(), supportedVersions);

            return invokeMethodImpl(dbSchema, row, proxy, args);
        }

        abstract @Nullable Object invokeMethodImpl(DatabaseSchema dbSchema, Row<GenericTableSchema> row, Object proxy,
                Object[] args);
    }

    private abstract static class TableMethodInvoker extends VersionedMethodInvoker {
        final String tableName;

        TableMethodInvoker(final Range<Version> supportedVersions, final String tableName) {
            super(supportedVersions);
            this.tableName = requireNonNull(tableName);
        }

        final @Nullable GenericTableSchema findTableSchema(final DatabaseSchema dbSchema) {
            return dbSchema.table(tableName, GenericTableSchema.class);
        }
    }

    private abstract static class ColumnMethodInvoker<T> extends TableMethodInvoker {
        final Class<T> columnType;
        final String columnName;

        ColumnMethodInvoker(final Method method, final Class<T> columnType, final String tableName) {
            super(TypedReflections.getColumnVersionRange(method), tableName);
            this.columnName = getColumnName(method);
            if (columnName == null) {
                throw new TyperException("Failed to find column name for method " + method.getName());
            }

            this.columnType = requireNonNull(columnType);
        }

        @Override
        final @Nullable Object invokeMethodImpl(final DatabaseSchema dbSchema,
                final Row<GenericTableSchema> row, final Object proxy,
                final Object[] args) {
            // TODO: this is Method/DatabaseSchema invariant and should therefore be cached in TypedDatabaseSchema
            final GenericTableSchema tableSchema = findTableSchema(dbSchema);
            if (tableSchema == null) {
                throw new TableSchemaNotFoundException(TableSchemaNotFoundException.createMessage(tableName,
                    dbSchema.getName()));
            }

            // When the row is null, that might indicate that the user maybe interested
            // only in the ColumnSchema and not on the Data.
            return row == null ? invokeMethod(tableSchema, proxy, args)
                    : invokeRowMethod(tableSchema, row, proxy, args);
        }

        abstract @Nullable Object invokeMethod(GenericTableSchema tableSchema, Object proxy, Object[] args);

        abstract @Nullable Object invokeRowMethod(GenericTableSchema tableSchema, Row<GenericTableSchema> row,
                Object proxy, Object[] args);

        // TODO: this is Method/DatabaseSchema invariant and should therefore be cached in TypedDatabaseSchema
        final ColumnSchema<GenericTableSchema, T> getColumnSchema(final GenericTableSchema tableSchema) {
            final ColumnSchema<GenericTableSchema, T> columnSchema = findColumnSchema(tableSchema);
            if (columnSchema == null) {
                String message = ColumnSchemaNotFoundException.createMessage(columnName, tableSchema.getName());
                throw new ColumnSchemaNotFoundException(message);
            }
            return columnSchema;
        }

        // TODO: this is Method/DatabaseSchema invariant and should therefore be cached in TypedDatabaseSchema
        final @Nullable ColumnSchema<GenericTableSchema, T> findColumnSchema(final GenericTableSchema tableSchema) {
            return tableSchema.column(columnName, columnType);
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

    private static final class GetTableSchema extends TableMethodInvoker {
        GetTableSchema(final String tableName) {
            super(Range.all(), tableName);
        }

        @Override
        @Nullable GenericTableSchema invokeMethodImpl(final DatabaseSchema dbSchema,
                final Row<GenericTableSchema> row, final Object proxy, final Object[] args) {
            return findTableSchema(dbSchema);
        }
    }

    private static final MethodInvoker GET_ROW = new MethodInvoker() {
        @Override
        @Nullable Object invokeMethod(final DatabaseSchema dbSchema, final Row<GenericTableSchema> row,
                final Object proxy, final Object[] args) {
            return row;
        }
    };

    private static final class GetColumn<T> extends ColumnMethodInvoker<T> {
        GetColumn(final Method method, final String tableName) {
            super(method, (Class<T>) method.getReturnType(), tableName);
        }

        @Override
        @Nullable Column<?, T> invokeMethod(final GenericTableSchema tableSchema, final Object proxy,
                final Object[] args) {
            return new Column<>(getColumnSchema(tableSchema), null);
        }

        @Override
        @Nullable Column<?, T> invokeRowMethod(final GenericTableSchema tableSchema, final Row<GenericTableSchema> row,
                final Object proxy, final Object[] args) {
            return row.getColumn(getColumnSchema(tableSchema));
        }
    }

    private static final class GetData<T> extends ColumnMethodInvoker<T> {
        GetData(final Method method, final String tableName) {
            super(method, (Class<T>) method.getReturnType(), tableName);
        }

        @Override
        @Nullable T invokeMethod(final GenericTableSchema tableSchema, final Object proxy, final Object[] args) {
            getColumnSchema(tableSchema);
            return null;
        }

        @Override
        @Nullable
        T invokeRowMethod(final GenericTableSchema tableSchema, final Row<GenericTableSchema> row, final Object proxy,
                final Object[] args) {
            final Column<GenericTableSchema, T> column = row.getColumn(getColumnSchema(tableSchema));
            return column == null ? null : column.getData();
        }
    }

    private static final class SetData<T> extends ColumnMethodInvoker<T> {
        SetData(final Method method, final Class<T> target, final String tableName) {
            super(method, target, tableName);
        }

        @Override
        @Nullable Object invokeMethod(final GenericTableSchema tableSchema, final Object proxy, final Object[] args) {
            throw new UnsupportedOperationException("No backing row supplied");
        }

        @Override
        @Nullable Object invokeRowMethod(final GenericTableSchema tableSchema, final Row<GenericTableSchema> row,
                final Object proxy, final Object[] args) {
            row.addColumn(columnName, new Column(findColumnSchema(tableSchema), args[0]));
            return proxy;
        }
    }

    private static final String GET_STARTS_WITH = "get";
    private static final String SET_STARTS_WITH = "set";
    private static final String GETCOLUMN_ENDS_WITH = "Column";
    private static final String GETROW_ENDS_WITH = "Row";

    static @Nullable MethodInvoker of(final String tableName, final Method method) {
        // FIXME: order retained for being bug-by-bug compatible. We should inline checking so that we can properly
        //        reuse whatever extraction bits are needed.
        if (isGetTableSchema(method)) {
            return new GetTableSchema(tableName);
        }
        if (isGetRow(method)) {
            return GET_ROW;
        }
        if (isSetData(method)) {
            final Class<?>[] paramTypes = method.getParameterTypes();
            if (paramTypes.length != 1) {
                throw new TyperException("Setter method : " + method.getName() + " requires 1 argument");
            }
            return new SetData<>(method, paramTypes[0], tableName);
        }
        if (isGetData(method)) {
            return new GetData<>(method, tableName);
        }
        if (isGetColumn(method)) {
            return new GetColumn<>(method, tableName);
        }

        return null;
    }

    abstract @Nullable Object invokeMethod(DatabaseSchema dbSchema, Row<GenericTableSchema> row, Object proxy,
            Object[] args);


    static boolean isGetColumn(final Method method) {
        TypedColumn typedColumn = method.getAnnotation(TypedColumn.class);
        if (typedColumn != null) {
            return typedColumn.method().equals(MethodType.GETCOLUMN);
        }

        return method.getName().startsWith(GET_STARTS_WITH) && method.getName().endsWith(GETCOLUMN_ENDS_WITH);
    }

    static boolean isGetData(final Method method) {
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
}
