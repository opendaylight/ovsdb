/*
 * Copyright © 2014, 2017 EBay Software Foundation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib.notation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opendaylight.ovsdb.lib.notation.json.RowSerializer;
import org.opendaylight.ovsdb.lib.schema.ColumnSchema;
import org.opendaylight.ovsdb.lib.schema.TableSchema;

@JsonSerialize(using = RowSerializer.class)
public class Row<E extends TableSchema<E>> {
    @JsonIgnore
    private TableSchema<E> tableSchema;
    protected Map<String, Column<E, ?>> columns = new HashMap<>();

    public Row() {

    }

    public Row(TableSchema<E> tableSchema) {
        this.tableSchema = tableSchema;
    }

    public Row(TableSchema<E> tableSchema, List<Column<E, ?>> columns) {
        this.tableSchema = tableSchema;
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

    public TableSchema<E> getTableSchema() {
        return tableSchema;
    }

    public void setTableSchema(TableSchema<E> tableSchema) {
        this.tableSchema = tableSchema;
    }

    @Override
    public String toString() {
        return "Row [columns=" + columns + "]";
    }

    /**
     * The hashCode method for Row object should be used with caution.
     * This method will use all the columns in the row to calculate the hashKey.
     * Hence using this method on a partial Row will return a different hashKey
     * and will not work in most of the use-cases this method might be used.
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((columns == null) ? 0 : columns.hashCode());
        return result;
    }

    /**
     * The equals method for Row object should be used with caution.
     * This method will compare all the columns in the row being compared.
     * Hence using this method to compare a partial Row will return false
     * and will not work in most of the use-cases this method might be used.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Row other = (Row) obj;
        if (columns == null) {
            if (other.columns != null) {
                return false;
            }
        } else if (!columns.equals(other.columns)) {
            return false;
        }
        return true;
    }
}
