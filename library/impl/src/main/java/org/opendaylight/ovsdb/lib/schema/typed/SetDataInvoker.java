package org.opendaylight.ovsdb.lib.schema.typed;

import static java.util.Objects.requireNonNull;

import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.schema.ColumnSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;

final class SetDataInvoker<T> extends MethodDispatch.ColumnInvoker<T> {
    private final @NonNull String columnName;

    SetDataInvoker(final @NonNull GenericTableSchema tableSchema, final @NonNull String columnName,
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