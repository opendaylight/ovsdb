/*
 *
 *  * Copyright (C) 2014 EBay Software Foundation
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  * and is available at http://www.eclipse.org/legal/epl-v10.html
 *  *
 *  * Authors : Ashwin Raveendran
 *
 */

package org.opendaylight.ovsdb.lib.notation;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.opendaylight.ovsdb.lib.schema.ColumnSchema;
import org.opendaylight.ovsdb.lib.schema.TableSchema;

import com.google.common.collect.Maps;

public class Row<E extends TableSchema<E>> {
    protected Map<String, Column<E, ?>> columns;

    public Row() {
        this.columns = Maps.newHashMap();
    }

    public Row(List<Column<E, ?>> columns) {
        this.columns = Maps.newHashMap();
        for (Column<E, ?> column : columns) {
            this.columns.put(column.getSchema().getName(), column);
        }
    }

    public <D> Column<E, D> getColumn(ColumnSchema<E, D> schema) {
        return (Column<E, D>) columns.get(schema.getName());
    }

    public Collection<Column<E, ?>> getColumns() {
        return columns.values();
    }

    public void addColumn(String columnName, Column<E, ?> data) {
        this.columns.put(columnName, data);
    }
}
