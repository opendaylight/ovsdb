package org.opendaylight.ovsdb.database;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TableSchema {
    @JsonProperty("columns")
    public Map<String, ColumnSchema> columns;
    @JsonProperty("maxRows")
    public Integer maxRows;
    @JsonProperty("isRoot")
    public Boolean isRoot;
    @JsonProperty("indexes")
    public Object indexes;

    public Map<String, ColumnSchema> getColumns(){
        return this.columns;
    }
}
