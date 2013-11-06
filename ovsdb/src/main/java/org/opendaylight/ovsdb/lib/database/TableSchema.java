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

    public Map<String, ColumnSchema> getColumns(){
        return this.columns;
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
