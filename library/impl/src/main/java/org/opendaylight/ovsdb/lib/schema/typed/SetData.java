/*
 * Copyright © 2019 PANTHEON.tech, s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib.schema.typed;

import static java.util.Objects.requireNonNull;

import java.lang.reflect.Method;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.ovsdb.lib.error.TyperException;
import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.schema.ColumnSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;

final class SetData<T> extends MethodDispatch.ColumnPrototype<T> {
    private static final class Invoker<T> extends MethodDispatch.ColumnInvoker<T> {
        private final @NonNull String columnName;

        Invoker(final @NonNull GenericTableSchema tableSchema, final @NonNull String columnName,
                final ColumnSchema<GenericTableSchema, T> columnSchema) {
            super(tableSchema, columnSchema);
            this.columnName = requireNonNull(columnName);
        }

        @Override
        Object invokeMethod(final Object proxy, final Object [] args) {
            throw new UnsupportedOperationException("No backing row supplied");
        }

        @Override
        Object invokeRowMethod(final Row<GenericTableSchema> row, final Object proxy, final Object[] args) {
            row.addColumn(columnName, new Column<>(columnSchema(), (T) args[0]));
            return proxy;
        }
    }

    SetData(final Method method, final String tableName, final String columnName) {
        super(method, findTarget(method), tableName, columnName);
    }

    @Override
    Invoker<T> bindToImpl(final GenericTableSchema tableSchema) {
        return new Invoker<>(tableSchema, columnName(), findColumnSchema(tableSchema));
    }

    private static Class<?> findTarget(final Method method) {
        final Class<?>[] paramTypes = method.getParameterTypes();
        if (paramTypes.length != 1) {
            throw new TyperException("Setter method : " + method.getName() + " requires 1 argument");
        }
        return paramTypes[0];
    }
}
