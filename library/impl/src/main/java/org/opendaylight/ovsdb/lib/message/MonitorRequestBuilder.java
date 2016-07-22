/*
 * Copyright (c) 2013, 2015 EBay Software Foundation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.lib.message;

import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import org.opendaylight.ovsdb.lib.schema.ColumnSchema;
import org.opendaylight.ovsdb.lib.schema.TableSchema;

public class MonitorRequestBuilder<E extends TableSchema<E>> {

    private E tableSchema;
    private Set<String> columns;
    private MonitorSelect select;

    MonitorRequestBuilder(E tableSchema) {
        this.tableSchema = tableSchema;
    }

    public static <T extends TableSchema<T>> MonitorRequestBuilder<T> builder(T tableSchema) {
        return new MonitorRequestBuilder<>(tableSchema);
    }

    public MonitorRequestBuilder<E> addColumn(String column) {
        if (columns == null) {
            columns = Sets.newHashSet();
        }
        columns.add(column);
        return this;
    }

    public MonitorRequestBuilder<E> addColumn(ColumnSchema<?, ?> column) {
        this.addColumn(column.getName());
        return this;
    }

    public MonitorRequestBuilder<E> addColumns(List<ColumnSchema<E, ?>> newColumns) {
        for (ColumnSchema<E, ?> schema : newColumns) {
            this.addColumn(schema);
        }
        return this;
    }

    public Set<String> getColumns() {
        return columns;
    }

    public MonitorRequestBuilder<E> with(MonitorSelect theSelect) {
        this.select = theSelect;
        return this;
    }

    public MonitorRequest build() {
        MonitorSelect nonNullSelect;
        if (select == null) {
            nonNullSelect = new MonitorSelectBuilder().build();
        } else {
            nonNullSelect = select;
        }
        return new MonitorRequest(tableSchema.getName(), columns, nonNullSelect);
    }
}
