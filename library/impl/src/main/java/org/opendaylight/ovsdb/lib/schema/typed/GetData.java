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
package org.opendaylight.ovsdb.lib.schema.typed;

import java.lang.reflect.Method;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.schema.ColumnSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;

public final class GetData {
    private static final class Invoker<T> extends MethodDispatch.ColumnInvoker<T> {
        Invoker(final @NonNull GenericTableSchema tableSchema,
                final @NonNull ColumnSchema<GenericTableSchema, T> columnSchema) {
            super(tableSchema, columnSchema);
        }

        @Override
        T invokeMethod(final Object proxy, final Object[] args) {
            return null;
        }

        @Override
        T invokeRowMethod(final Row<GenericTableSchema> row, final Object proxy, final Object[] args) {
            final Column<GenericTableSchema, T> column = row.getColumn(columnSchema());
            return column == null ? null : column.getData();
        }
    }

    static final class Prototype<T> extends MethodDispatch.ColumnPrototype<T> {
        Prototype(final Method method, final String tableName) {
            super(method, (Class<T>) method.getReturnType(), tableName);
        }

        @Override
        Invoker<T> bindToImpl(final GenericTableSchema tableSchema) {
            return new Invoker<>(tableSchema, getColumnSchema(tableSchema));
        }
    }

    private GetData() {

    }
}
