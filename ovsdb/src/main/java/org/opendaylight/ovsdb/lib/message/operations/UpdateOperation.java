/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal
 */
package org.opendaylight.ovsdb.lib.message.operations;

import java.util.List;

import org.opendaylight.ovsdb.lib.notation.Condition;
import org.opendaylight.ovsdb.lib.table.internal.Table;
//TODO Madhu : This is not complete. Getting it in to enable other committers to make progress
public class UpdateOperation extends Operation {
    String table;
    List<Condition> where;
    Table<?> row;

    public UpdateOperation(String table, List<Condition> where, Table<?> row) {
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
    public Table<?> getRow() {
        return row;
    }
    public void setRow(Table<?> row) {
        this.row = row;
    }
    @Override
    public String toString() {
        return "UpdateOperation [table=" + table + ", where=" + where
                + ", row=" + row + ", toString()=" + super.toString() + "]";
    }
}
