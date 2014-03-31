/*
 * [[ Authors will Fill in the Copyright header ]]
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Brent Salisbury, Evan Zeller
 */
package org.opendaylight.ovsdb.lib.database;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TableSchema {

    @JsonProperty("columns")
    private Map<String, ColumnSchema> columns;

    @JsonProperty("maxRows")
    private Integer maxRows;

    @JsonProperty("isRoot")
    private Boolean isRoot;

    @JsonProperty("indexes")
    private Object indexes;

    public Map<String, ColumnSchema> getColumns() {
        return this.columns;
    }

    public ColumnSchema getColumn(String columnName) {
        return this.columns.get(columnName);
    }

    public Integer getMaxRows() {
        return maxRows;
    }

    public Boolean getIsRoot() {
        return isRoot;
    }

    public Object getIndexes() {
        return indexes;
    }

    @Override
    public String toString() {
        return "TableSchema [columns=" + columns + ", maxRows=" + maxRows
                + ", isRoot=" + isRoot + ", indexes=" + indexes + "]";
    }
}
