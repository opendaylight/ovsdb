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

import java.util.List;

import org.opendaylight.ovsdb.lib.schema.ColumnSchema;
import org.opendaylight.ovsdb.lib.schema.TableSchema;

public class Row<E extends TableSchema<E>> {
    List<Column<E, ?>> columns;

    private Row() { }

    public Row(List<Column<E, ?>> columns) {
        this.columns = columns;
    }

    public <D> Column<E, D> getColumn(ColumnSchema<E, D> schema) {
        for (Column<E, ?> column : columns) {
           if (column.getSchema().equals(schema)) {
               return (Column<E, D>) column;
           }
        }
        return null;
    }

    public List<Column<E, ?>> getColumns() {
        return columns;
    }
}
