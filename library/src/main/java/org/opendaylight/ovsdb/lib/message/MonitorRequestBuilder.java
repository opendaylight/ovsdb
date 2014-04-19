/*
 * Copyright (C) 2013 EBay Software Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Ashwin Raveendran, Madhu Venugopal
 */
package org.opendaylight.ovsdb.lib.message;

import com.google.common.collect.Sets;
import org.opendaylight.ovsdb.lib.schema.ColumnSchema;
import org.opendaylight.ovsdb.lib.schema.TableSchema;

import java.util.List;

public class MonitorRequestBuilder<E extends TableSchema<E>> {

    E tableSchema;
    MonitorRequest<E> monitorRequest;

    MonitorRequestBuilder(E tableSchema) {
        this.tableSchema = tableSchema;
    }

    public static <T extends TableSchema<T>> MonitorRequestBuilder<T> builder(T tableSchema) {
        return new MonitorRequestBuilder<>(tableSchema);
    }

    MonitorRequest<E> getMonitorRequest() {
        if (monitorRequest == null) {
            monitorRequest = new MonitorRequest<>();
            monitorRequest.setColumns(Sets.<String>newHashSet());
        }
        return monitorRequest;
    }

    public MonitorRequestBuilder<E> addColumn(String column) {
        getMonitorRequest().getColumns().add(column);
        return this;
    }

    public MonitorRequestBuilder<E> addColumn(ColumnSchema<?, ?> column) {
        this.addColumn(column.getName());
        return this;
    }

    public MonitorRequestBuilder<E> addColumns(List<ColumnSchema<E, ?>> columns) {
        for(ColumnSchema<E, ?> schema : columns) {
            this.addColumn(schema);
        }
        return this;
    }

    public MonitorRequestBuilder<E> with(MonitorSelect select) {
        getMonitorRequest().setSelect(select);
        return this;
    }

    public MonitorRequest<E> build() {
        MonitorRequest<E> monitorRequest = getMonitorRequest();
        if (monitorRequest.getSelect() == null) {
            monitorRequest.setSelect(new MonitorSelect());
        }
        monitorRequest.setTableName(tableSchema.getName());
        return monitorRequest;
    }
}
