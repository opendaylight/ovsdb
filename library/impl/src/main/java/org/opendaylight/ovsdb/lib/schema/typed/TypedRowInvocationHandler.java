/*
 * Copyright © 2014, 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib.schema.typed;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableMap;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Objects;
import org.opendaylight.ovsdb.lib.error.ColumnSchemaNotFoundException;
import org.opendaylight.ovsdb.lib.error.TableSchemaNotFoundException;
import org.opendaylight.ovsdb.lib.error.TyperException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Objects;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import com.google.common.base.Objects;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import org.opendaylight.ovsdb.lib.error.UnsupportedMethodException;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;

final class TypedRowInvocationHandler implements InvocationHandler {
    // As the mode of invocation for a particular method is invariant, we keep the set of dynamically-supported method
    // in a per-Class cache, thus skipping reflective operations at invocation time.
    private static final LoadingCache<Class<?>, ImmutableMap<Method, MethodInvoker>> METHOD_INVOKERS =
            CacheBuilder.newBuilder().weakKeys().weakValues()
                .build(new CacheLoader<Class<?>, ImmutableMap<Method, MethodInvoker>>() {
                    @Override
                    public ImmutableMap<Method, MethodInvoker> load(final Class<?> key) {
                        final String tableName = TypedReflections.getTableName(key);
                        final ImmutableMap.Builder<Method, MethodInvoker> builder = ImmutableMap.builder();
                        for (Method method : key.getMethods()) {
                            final MethodInvoker invoker = MethodInvoker.of(tableName, method);
                            if (invoker != null) {
                                builder.put(method, invoker);
                            }
                        }
                        return builder.build();
                    }
                });

    private final Class<?> target;
    private final DatabaseSchema dbSchema;
    private final Row<GenericTableSchema> row;
    private final ImmutableMap<Method, MethodInvoker> invokers;

    TypedRowInvocationHandler(final Class<?> target, final DatabaseSchema dbSchema,
            final Row<GenericTableSchema> row) {
        this.target = requireNonNull(target);
        this.dbSchema = requireNonNull(dbSchema);
        this.row = row;
        this.invokers = METHOD_INVOKERS.getUnchecked(target);
    }

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Exception {
        final MethodInvoker invoker = invokers.get(method);
        return invoker != null ? invoker.invokeMethod(dbSchema, row, proxy, args)
                : invokeObjectMethod(proxy, method, args);
    }

    private Object invokeObjectMethod(final Object proxy, final Method method, final Object[] args) {
        switch (method.getName()) {
            case "hashCode":
                if (args == null || args.length == 0) {
                    return row == null ? 0 : row.hashCode();
                }
                break;
            case "equals":
                if (args != null && args.length == 1 && method.getParameterTypes()[0] == Object.class) {
                    // TODO: this equality looks weird...
                    final Object obj = args[0];
                    return proxy.getClass().isInstance(obj) && obj instanceof TypedBaseTable
                            && Objects.equal(row, ((TypedBaseTable<?>)obj).getRow());
                }
                break;
            case "toString":
                if (args == null || args.length == 0) {
                    final GenericTableSchema schema = TyperUtils.getTableSchema(dbSchema, target);
                    final String tableName = schema != null ? schema.getName() : "";
                    return row == null ? tableName : tableName + " : " + row.toString();
                }
                break;
            default:
                break;
        }

        throw new UnsupportedMethodException("Method not supported " + method.toString());
    }
}
