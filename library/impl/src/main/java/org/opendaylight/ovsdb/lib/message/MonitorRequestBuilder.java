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

public class MonitorRequestBuilder {

    private final TableSchema tableSchema;
    private final Collection<String> columns = new HashSet<>();
    private MonitorSelect select;

    public MonitorRequestBuilder(TableSchema tableSchema) {
        this.tableSchema = tableSchema;
    }

    public MonitorRequestBuilder addColumn(String column) {
        this.columns.add(column);
        return this;
    }

    public MonitorRequestBuilder addColumn(ColumnSchema<?> column) {
        this.addColumn(column.getName());
        return this;
    }

    public MonitorRequestBuilder addColumns(Collection<String> columns) {
        this.columns.addAll(columns);
        return this;
    }

    public MonitorRequestBuilder addColumns(List<ColumnSchema<?>> columns) {
        for (ColumnSchema<?> schema : columns) {
            this.addColumn(schema);
        }
        return this;
    }

    public Collection<String> getColumns() {
        return this.columns;
    }

    public MonitorRequestBuilder with(MonitorSelect select) {
        this.select = select;
        return this;
    }

    public MonitorRequest build() {
        MonitorRequest request = new MonitorRequest(tableSchema.getName(), new HashSet<>(this.columns));
        request.setSelect(select == null ? new MonitorSelect() : select);
        return request;
    }
}
