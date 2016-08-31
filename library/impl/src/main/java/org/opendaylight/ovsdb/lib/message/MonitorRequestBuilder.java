/*
 * Copyright (c) 2013, 2015 EBay Software Foundation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.lib.message;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import org.opendaylight.ovsdb.lib.schema.ColumnSchema;
import org.opendaylight.ovsdb.lib.schema.TableSchema;

public class MonitorRequestBuilder<E extends TableSchema<E>> {

    private final E tableSchema;
    private final Collection<String> columns = new HashSet<>();
    private MonitorSelect select;

    public MonitorRequestBuilder(E tableSchema) {
        this.tableSchema = tableSchema;
    }

    public MonitorRequestBuilder<E> addColumn(String column) {
        this.columns.add(column);
        return this;
    }

    public MonitorRequestBuilder<E> addColumn(ColumnSchema<?, ?> column) {
        this.addColumn(column.getName());
        return this;
    }

    public MonitorRequestBuilder<E> addColumns(Collection<String> columns) {
        this.columns.addAll(columns);
        return this;
    }

    public MonitorRequestBuilder<E> addColumns(List<ColumnSchema<E, ?>> columns) {
        for (ColumnSchema<E, ?> schema : columns) {
            this.addColumn(schema);
        }
        return this;
    }

    public Collection<String> getColumns() {
        return this.columns;
    }

    public MonitorRequestBuilder<E> with(MonitorSelect select) {
        this.select = select;
        return this;
    }

    public MonitorRequest build() {
        MonitorRequest request = new MonitorRequest(tableSchema.getName(), new HashSet<>(this.columns));
        request.setSelect(select == null ? new MonitorSelect() : select);
        return request;
    }
}
