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

public class MonitorRequestBuilder {

    TableSchema tableSchema;
    MonitorRequest monitorRequest;

    MonitorRequestBuilder(TableSchema tableSchema) {
        this.tableSchema = tableSchema;
    }

    public static MonitorRequestBuilder builder(TableSchema tableSchema) {
        return new MonitorRequestBuilder(tableSchema);
    }

    MonitorRequest getMonitorRequest() {
        if (monitorRequest == null) {
            monitorRequest = new MonitorRequest();
        }
        return monitorRequest;
    }

    public MonitorRequestBuilder addColumn(String column) {
        getMonitorRequest().addColumn(column);
        return this;
    }

    public MonitorRequestBuilder addColumn(ColumnSchema<?> column) {
        this.addColumn(column.getName());
        return this;
    }

    public MonitorRequestBuilder addColumns(List<ColumnSchema<?>> columns) {
        for (ColumnSchema<?> schema : columns) {
            this.addColumn(schema);
        }
        return this;
    }

    public Set<String> getColumns() {
        return getMonitorRequest().getColumns();
    }

    public MonitorRequestBuilder with(MonitorSelect select) {
        getMonitorRequest().setSelect(select);
        return this;
    }

    public MonitorRequest build() {
        MonitorRequest newBuiltMonitorRequest = getMonitorRequest();
        if (newBuiltMonitorRequest.getSelect() == null) {
            newBuiltMonitorRequest.setSelect(new MonitorSelect());
        }
        newBuiltMonitorRequest.setTableName(tableSchema.getName());
        return newBuiltMonitorRequest;
    }
}
