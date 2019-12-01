/*
 * Copyright © 2019 PANTHEON.tech, s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib.schema.typed;

import java.lang.reflect.Method;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.schema.ColumnSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;

final class GetColumn<T> extends MethodDispatch.StrictColumnPrototype<T> {
    private static final class Invoker<T> extends MethodDispatch.ColumnInvoker<T> {
        Invoker(final @NonNull GenericTableSchema tableSchema,
                final @NonNull ColumnSchema<GenericTableSchema, T> columnSchema) {
            super(tableSchema, columnSchema);
        }

        @Override
        Object invokeMethod(final Object proxy, final Object[] args) {
            return new Column<>(columnSchema(), null);
        }

        @Override
        Object invokeRowMethod(final Row<GenericTableSchema> row, final Object proxy, final Object[] args) {
            return row.getColumn(columnSchema());
        }
    }

    GetColumn(final Method method, final String tableName, final String columnName) {
        super(method, tableName, columnName);
    }

    @Override
    Invoker<T> bindToImpl(final GenericTableSchema tableSchema,
            final ColumnSchema<GenericTableSchema, T> columnSchema) {
        return new Invoker<>(tableSchema, columnSchema);
    }
}
