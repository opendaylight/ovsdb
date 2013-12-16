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

public class SelectOperation extends Operation {
    String table;
    List<Condition> where;
    List<String> columns;

    public SelectOperation(String table, List<Condition> where, List<String> columns) {
        super();
        super.setOp("select");
        this.table = table;
        this.where = where;
        this.columns = columns;
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
    public List<String> getColumns() {
        return columns;
    }
    public void setColumns(List<String> columns) {
        this.columns = columns;
    }
    @Override
    public String toString() {
        return "SelectOperation [table=" + table + ", where=" + where
                + ", columns=" + columns + "]";
    }
}
