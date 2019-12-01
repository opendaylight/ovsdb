/*
 * Copyright © 2019 PANTHEON.tech, s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib.schema.typed;

import static com.google.common.base.Verify.verifyNotNull;
import static java.util.Objects.requireNonNull;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Range;
import java.lang.reflect.Method;
import java.util.Locale;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.ovsdb.lib.error.ColumnSchemaNotFoundException;
import org.opendaylight.ovsdb.lib.error.TableSchemaNotFoundException;
import org.opendaylight.ovsdb.lib.error.TyperException;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.Version;
import org.opendaylight.ovsdb.lib.schema.ColumnSchema;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;

/**
 * Table->Method runtime-constant support. The binding of Class methods to corresponding data operations is defined
 * by annotations, which means that such mapping is Class-invariant. This invariance is captured in this class
 *
 * <p>
 * Data operations are always invoked in the context of a runtime {@link DatabaseSchema}, i.e. for a particular device
 * or a device function. This class exposes {@link #bindToSchema(TypedDatabaseSchema)}, which will construct an immutable
 * mapping between a Method and its invocation handler.
 */
final class MethodDispatch {
    abstract static class Invoker {

        abstract Object invokeMethod(Row<GenericTableSchema> row, Object proxy, Object[] args);
    }

    abstract static class Prototype {

        abstract Invoker bindTo(TypedDatabaseSchema dbSchema);
    }

    abstract static class TableInvoker extends Invoker {
        private final GenericTableSchema tableSchema;

        TableInvoker(final GenericTableSchema tableSchema) {
            this.tableSchema = tableSchema;
        }

        @Nullable GenericTableSchema tableSchema() {
            return tableSchema;
        }
    }

    abstract static class ColumnInvoker<T> extends TableInvoker {
        private final ColumnSchema<GenericTableSchema, T> columnSchema;

        ColumnInvoker(final GenericTableSchema tableSchema, final ColumnSchema<GenericTableSchema, T> columnSchema) {
            super(requireNonNull(tableSchema));
            this.columnSchema = columnSchema;
        }

        @Override
        final @NonNull GenericTableSchema tableSchema() {
            return verifyNotNull(super.tableSchema());
        }

        @Nullable ColumnSchema<GenericTableSchema, T> columnSchema() {
            return columnSchema;
        }

        @Override
        Object invokeMethod(final Row<GenericTableSchema> row, final Object proxy, final Object[] args) {
            // When the row is null, that might indicate that the user maybe interested
            // only in the ColumnSchema and not on the Data.
            return row == null ? invokeMethod(proxy, args) : invokeRowMethod(row, proxy, args);
        }

        abstract Object invokeMethod(Object proxy, Object[] args);

        abstract Object invokeRowMethod(@NonNull Row<GenericTableSchema> row, Object proxy, Object[] args);
    }

    // As the mode of invocation for a particular method is invariant, we keep the set of dynamically-supported method
    // in a per-Class cache, thus skipping reflective operations at invocation time.
    private static final LoadingCache<Class<?>, MethodDispatch> CACHE = CacheBuilder.newBuilder()
            .weakKeys().weakValues().build(new CacheLoader<Class<?>, MethodDispatch>() {
                @Override
                public MethodDispatch load(final Class<?> key) {
                    return new MethodDispatch(key);
                }
            });

    private abstract static class VersionedPrototype extends Prototype {
        private final Range<Version> supportedVersions;

        VersionedPrototype(final Range<Version> supportedVersions) {
            this.supportedVersions = requireNonNull(supportedVersions);
        }

        @Override
        final Invoker bindTo(final TypedDatabaseSchema dbSchema) {
            // TODO: this is Class/DatabaseSchema invariant and should therefore be cached in TypedDatabaseSchema
            //       with an appropriate cache
            TyperUtils.checkVersion(dbSchema.getVersion(), supportedVersions);

            return bindToImpl(dbSchema);
        }

        abstract Invoker bindToImpl(TypedDatabaseSchema dbSchema);
    }

    abstract static class TablePrototype extends VersionedPrototype {
        final String tableName;

        TablePrototype(final Range<Version> supportedVersions, final String tableName) {
            super(supportedVersions);
            this.tableName = requireNonNull(tableName);
        }

        final @Nullable GenericTableSchema findTableSchema(final DatabaseSchema dbSchema) {
            return dbSchema.table(tableName, GenericTableSchema.class);
        }

        final GenericTableSchema getTableSchema(final DatabaseSchema dbSchema) {
            final GenericTableSchema tableSchema = findTableSchema(dbSchema);
            if (tableSchema == null) {
                throw new TableSchemaNotFoundException(TableSchemaNotFoundException.createMessage(tableName,
                    dbSchema.getName()));
            }
            return tableSchema;
        }
    }

    abstract static class ColumnPrototype<T> extends TablePrototype {
        private final @NonNull Class<T> columnType;
        private final @NonNull String columnName;

        ColumnPrototype(final Method method, final Class<T> columnType, final String tableName) {
            super(TypedReflections.getColumnVersionRange(method), tableName);
            this.columnName = getColumnName(method);
            this.columnType = requireNonNull(columnType);
        }

        final @NonNull String columnName() {
            return columnName;
        }

        @Override
        final Invoker bindToImpl(final TypedDatabaseSchema dbSchema) {
            final GenericTableSchema tableSchema = findTableSchema(dbSchema);
            if (tableSchema == null) {
                throw new TableSchemaNotFoundException(TableSchemaNotFoundException.createMessage(tableName,
                    dbSchema.getName()));
            }
            return bindToImpl(tableSchema);
        }

        abstract Invoker bindToImpl(@NonNull GenericTableSchema tableSchema);

        final @Nullable ColumnSchema<GenericTableSchema, T> findColumnSchema(
                final @NonNull GenericTableSchema tableSchema) {
            return tableSchema.column(columnName, columnType);
        }

        final @NonNull ColumnSchema<GenericTableSchema, T> getColumnSchema(
                final @NonNull GenericTableSchema tableSchema) {
            final ColumnSchema<GenericTableSchema, T> columnSchema = findColumnSchema(tableSchema);
            if (columnSchema == null) {
                String message = ColumnSchemaNotFoundException.createMessage(columnName, tableSchema.getName());
                throw new ColumnSchemaNotFoundException(message);
            }
            return columnSchema;
        }
    }

    private static final String GET_STARTS_WITH = "get";
    private static final String SET_STARTS_WITH = "set";
    private static final String GETCOLUMN_ENDS_WITH = "Column";
    private static final String GETROW_ENDS_WITH = "Row";

    private final @NonNull ImmutableMap<Method, Prototype> prototypes;
    private final @NonNull String tableName;

    private MethodDispatch(final Class<?> key) {
        tableName = TypedReflections.getTableName(key);

        final ImmutableMap.Builder<Method, Prototype> builder = ImmutableMap.builder();
        for (Method method : key.getMethods()) {
            final Prototype prototype = MethodDispatch.prototypeFor(tableName, method);
            if (prototype != null) {
                builder.put(method, prototype);
            }
        }
        this.prototypes = builder.build();
    }

    static MethodDispatch forTarget(final Class<?> target) {
        return CACHE.getUnchecked(target);
    }

    @NonNull TypedRowInvocationHandler bindToSchema(final TypedDatabaseSchema dbSchema) {
        return new TypedRowInvocationHandler(tableName,
            ImmutableMap.copyOf(Maps.transformValues(prototypes, prototype -> prototype.bindTo(dbSchema))));
    }

    @NonNull String tableName() {
        return tableName;
    }


    private static @Nullable Prototype prototypeFor(final String tableName, final Method method) {
        // FIXME: order retained for being bug-by-bug compatible. We should inline checking so that we can properly
        //        reuse whatever extraction bits are needed.
        if (isGetTableSchema(method)) {
            return new GetTable.Prototype(tableName);
        }
        if (isGetRow(method)) {
            return GetRow.PROTOTYPE;
        }
        if (isSetData(method)) {
            final Class<?>[] paramTypes = method.getParameterTypes();
            if (paramTypes.length != 1) {
                throw new TyperException("Setter method : " + method.getName() + " requires 1 argument");
            }
            return new SetData.Prototype<>(method, paramTypes[0], tableName);
        }
        if (isGetData(method)) {
            return new GetData.Prototype<>(method, tableName);
        }
        if (isGetColumn(method)) {
            return new GetColumn.Prototype<>(method, tableName);
        }

        return null;
    }

    private static @NonNull String getColumnName(final Method method) {
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

        throw new TyperException("Failed to find column name for method " + method.getName());
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
}
