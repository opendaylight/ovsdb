package org.opendaylight.ovsdb.lib.message.operations;

import java.util.List;

import org.opendaylight.ovsdb.lib.notation.Condition;
import org.opendaylight.ovsdb.lib.notation.Mutation;
//TODO Madhu : This is not complete. Getting it in to enable other committers to make progress
public class UpdateOperation extends Operation {
    String table;
    List<Condition> where;
    Object row;

    public UpdateOperation(String table, List<Condition> where, Object row) {
        super();
        super.setOp("update");
        this.table = table;
        this.where = where;
        this.row = row;
    }
    public String getTable() {
        return table;
    }
    public void setTable(String table) {
        this.table = table;
    }
    public List<Condition> getWhere() {
        return where;
    }
    public void setWhere(List<Condition> where) {
        this.where = where;
    }
    public Object getRow() {
        return row;
    }
    public void setRow(Object row) {
        this.row = row;
    }
    @Override
    public String toString() {
        return "UpdateOperation [table=" + table + ", where=" + where
                + ", row=" + row + ", toString()=" + super.toString() + "]";
    }
}
