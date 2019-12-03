/*
 * Copyright © 2019 PANTHEON.tech, s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
/*
 * Copyright © 2019 PANTHEON.tech, s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib.schema;

import java.lang.reflect.Method;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.notation.Row;

final class GetData<T> extends MethodDispatch.StrictColumnPrototype<T> {
    private static final class Invoker<T> extends MethodDispatch.ColumnInvoker<T> {
        Invoker(final @NonNull GenericTableSchema tableSchema,
                final @NonNull ColumnSchema<GenericTableSchema, T> columnSchema) {
            super(tableSchema, columnSchema);
        }

        @Override
        Object invokeMethod(final Object proxy, final Object[] args) {
            return null;
        }

        @Override
        Object invokeRowMethod(final Row<GenericTableSchema> row, final Object proxy, final Object[] args) {
            final Column<GenericTableSchema, T> column = row.getColumn(columnSchema());
            return column == null ? null : column.getData();
        }
    }

    GetData(final Method method, final String tableName, final String columnName) {
        super(method, tableName, columnName);
    }

    @Override
    Invoker<T> bindToImpl(final GenericTableSchema tableSchema,
            final ColumnSchema<GenericTableSchema, T> columnSchema) {
        return new Invoker<>(tableSchema, columnSchema);
    }
}
