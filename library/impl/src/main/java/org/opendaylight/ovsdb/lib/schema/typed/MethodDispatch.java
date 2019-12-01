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

    abstract static class FailedInvoker extends Invoker {
        @Override
        final Object invokeMethod(final Row<GenericTableSchema> row, final Object proxy, final Object[] args) {
            throw newException();
        }

        abstract @NonNull RuntimeException newException();
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
        private final String tableName;

        TablePrototype(final Range<Version> supportedVersions, final String tableName) {
            super(supportedVersions);
            this.tableName = requireNonNull(tableName);
        }

        final @Nullable GenericTableSchema findTableSchema(final DatabaseSchema dbSchema) {
            return dbSchema.table(tableName, GenericTableSchema.class);
        }

        final @NonNull FailedInvoker tableSchemaNotFound(final DatabaseSchema dbSchema) {
            return new FailedInvoker() {
                @Override
                RuntimeException newException() {
                    return new TableSchemaNotFoundException(tableName, dbSchema.getName());
                }
            };
        }
    }

    abstract static class ColumnPrototype<T> extends TablePrototype {
        private final @NonNull Class<T> columnType;
        private final @NonNull String columnName;

        ColumnPrototype(final Method method, final Class<?> columnType, final String tableName,
                final String columnName) {
            super(TypedReflections.getColumnVersionRange(method), tableName);
            this.columnName = requireNonNull(columnName);
            this.columnType = requireNonNull((Class<T>) columnType);
        }

        final @NonNull String columnName() {
            return columnName;
        }

        @Override
        final Invoker bindToImpl(final TypedDatabaseSchema dbSchema) {
            final GenericTableSchema tableSchema = findTableSchema(dbSchema);
            return tableSchema == null ? tableSchemaNotFound(dbSchema) : bindToImpl(tableSchema);
        }

        final @Nullable ColumnSchema<GenericTableSchema, T> findColumnSchema(
                final @NonNull GenericTableSchema tableSchema) {
            return tableSchema.column(columnName, columnType);
        }

        abstract Invoker bindToImpl(@NonNull GenericTableSchema tableSchema);
    }

    abstract static class StrictColumnPrototype<T> extends ColumnPrototype<T> {
        StrictColumnPrototype(final Method method, final String tableName, final String columnName) {
            super(method, method.getReturnType(), tableName, columnName);
        }

        @Override
        final Invoker bindToImpl(@NonNull final GenericTableSchema tableSchema) {
            final ColumnSchema<GenericTableSchema, T> columnSchema = findColumnSchema(tableSchema);
            return columnSchema != null ? bindToImpl(tableSchema, columnSchema) : new FailedInvoker() {
                @Override
                RuntimeException newException() {
                    return new ColumnSchemaNotFoundException(columnName(), tableSchema.getName());
                }
            };
        }

        abstract Invoker bindToImpl(@NonNull GenericTableSchema tableSchema,
                @NonNull ColumnSchema<GenericTableSchema, T> columnSchema);
    }

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

    private static @Nullable Prototype prototypeFor(final String tableName, final Method method) {
        final TypedColumn typedColumn = method.getAnnotation(TypedColumn.class);
        if (typedColumn != null) {
            final MethodType methodType = typedColumn.method();
            switch (methodType) {
                case GETCOLUMN:
                    return new GetColumn<>(method, typedColumn.name(), tableName);
                case GETDATA:
                    return new GetData<>(method, typedColumn.name(), tableName);
                case GETROW:
                    return GetRow.INSTANCE;
                case GETTABLESCHEMA:
                    return new GetTable(tableName);
                case SETDATA:
                    return new SetData<>(method, typedColumn.name(), tableName);
                default:
                    throw new TyperException("Unhandled method type " + methodType);
            }
        }

        /*
         * Attempting to get the column name by parsing the method name with a following convention :
         * 1. GETDATA : get<ColumnName>
         * 2. SETDATA : set<ColumnName>
         * 3. GETCOLUMN : get<ColumnName>Column
         * where <ColumnName> is the name of the column that we are interested in.
         */
        final String name = method.getName();
        if (name.startsWith("set")) {
            return new SetData<>(method, accessorName(name.substring(3)), tableName);
        }
        if (name.startsWith("get")) {
            if (name.endsWith("Row")) {
                return GetRow.INSTANCE;
            }
            final String tail = name.substring(3);
            if (tail.endsWith("Column")) {
                return new GetColumn<>(method, tableName, accessorName(tail.substring(0, tail.length() - 6)));
            }
            return new GetData<>(method, accessorName(tail), tableName);
        }

        return null;
    }

    private static String accessorName(final String columnName) {
        return columnName.toLowerCase(Locale.ROOT);
    }
}
