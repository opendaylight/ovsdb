package org.opendaylight.ovsdb.lib.message.operations;

import java.util.Map;

import org.opendaylight.ovsdb.lib.table.internal.Table;

import com.fasterxml.jackson.annotation.JsonProperty;
// TODO Madhu : This is not complete. Getting it in to enable other committers to make progress
public class InsertOperation extends Operation {
    String table;
    @JsonProperty("uuid-name")
    public String uuidName;
    public Table<?> row;

    public InsertOperation() {
        super();
        super.setOp("insert");
    }

    public InsertOperation(String table, String uuidName,
            Table<?> row) {
        this();
        this.table = table;
        this.uuidName = uuidName;
        this.row = row;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public String getUuidName() {
        return uuidName;
    }

    public void setUuidName(String uuidName) {
        this.uuidName = uuidName;
    }

    public Table<?> getRow() {
        return row;
    }

    public void setRow(Table<?> row) {
        this.row = row;
    }

    @Override
    public String toString() {
        return "InsertOperation [table=" + table + ", uuidName=" + uuidName
                + ", row=" + row + ", toString()=" + super.toString() + "]";
    }
}
