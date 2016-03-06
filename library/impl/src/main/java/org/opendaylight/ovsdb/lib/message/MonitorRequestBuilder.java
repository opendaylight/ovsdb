/*
 * Copyright (c) 2013, 2015 EBay Software Foundation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.lib.message;

import java.util.List;
import java.util.Set;

import org.opendaylight.ovsdb.lib.schema.ColumnSchema;
import org.opendaylight.ovsdb.lib.schema.TableSchema;

public class MonitorRequestBuilder<E extends TableSchema<E>> {

    E tableSchema;
    MonitorRequest monitorRequest;

    MonitorRequestBuilder(E tableSchema) {
        this.tableSchema = tableSchema;
    }

    public static <T extends TableSchema<T>> MonitorRequestBuilder<T> builder(T tableSchema) {
        return new MonitorRequestBuilder<>(tableSchema);
    }

    MonitorRequest getMonitorRequest() {
        if (monitorRequest == null) {
            monitorRequest = new MonitorRequest();
        }
        return monitorRequest;
    }

    public MonitorRequestBuilder<E> addColumn(String column) {
        getMonitorRequest().addColumn(column);
        return this;
    }

    public MonitorRequestBuilder<E> addColumn(ColumnSchema<?, ?> column) {
        this.addColumn(column.getName());
        return this;
    }

    public MonitorRequestBuilder<E> addColumns(List<ColumnSchema<E, ?>> columns) {
        for (ColumnSchema<E, ?> schema : columns) {
            this.addColumn(schema);
        }
        return this;
    }

    public Set<String> getColumns() {
        return getMonitorRequest().getColumns();
    }

    public MonitorRequestBuilder<E> with(MonitorSelect select) {
        getMonitorRequest().setSelect(select);
        return this;
    }

    public MonitorRequest build() {
        MonitorRequest monitorRequest = getMonitorRequest();
        if (monitorRequest.getSelect() == null) {
            monitorRequest.setSelect(new MonitorSelect());
        }
        monitorRequest.setTableName(tableSchema.getName());
        return monitorRequest;
    }
}
