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
import java.util.Objects;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.ovsdb.lib.error.UnsupportedMethodException;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.MethodDispatch.Invoker;

/*
 * Theory of operation: we have a set of Invoker, which are indexed by method and point to implementations we should
 * be invoking. This mapping is data-invariant, end hence we allow rebiding to a different row (which may not be null).
 */
// FIXME: okay, 'public' and 'InvocationHandler' do not mix well
public final class TypedRowInvocationHandler implements InvocationHandler {

    private final @NonNull ImmutableMap<Method, Invoker> invokers;
    private final @NonNull String tableName;
    private final @Nullable Row<GenericTableSchema> row;

    private TypedRowInvocationHandler(final @NonNull String tableName,
            @NonNull final ImmutableMap<Method, Invoker> invokers, final Row<GenericTableSchema> row) {
        this.tableName = requireNonNull(tableName);
        this.invokers = requireNonNull(invokers);
        this.row = row;
    }

    TypedRowInvocationHandler(final @NonNull String tableName, @NonNull final ImmutableMap<Method, Invoker> invokers) {
        this(tableName, invokers, null);
    }

    TypedRowInvocationHandler bindToRow(final @Nullable Row<GenericTableSchema> newRow) {
        return row == newRow ? this : new TypedRowInvocationHandler(tableName, invokers, newRow);
    }

    String getTableName() {
        return tableName;
    }

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) {
        final Invoker invoker = invokers.get(method);
        return invoker != null ? invoker.invokeMethod(row, proxy, args) : invokeObjectMethod(proxy, method, args);
    }

    // Split out to aid inlining
    private Object invokeObjectMethod(final Object proxy, final Method method, final Object[] args) {
        switch (method.getName()) {
            case "hashCode":
                if (args == null || args.length == 0) {
                    return row == null ? 0 : row.hashCode();
                }
                break;
            case "equals":
                if (args != null && args.length == 1 && method.getParameterTypes()[0] == Object.class) {
                    // We only run equality or our proxy and only when it is proxying a TypedBaseTable
                    final Object obj = args[0];
                    return proxy == obj || proxy.getClass().isInstance(obj) && obj instanceof TypedBaseTable
                            && Objects.equals(row, ((TypedBaseTable<?>)obj).getRow());
                }
                break;
            case "toString":
                if (args == null || args.length == 0) {
                    return row == null ? tableName : tableName + " : " + row.toString();
                }
                break;
            default:
                break;
        }

        throw new UnsupportedMethodException("Method not supported " + method.toString());
    }
}
