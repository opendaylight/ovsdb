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
import org.opendaylight.ovsdb.lib.notation.Mutation;

public class MutateOperation extends Operation {
    String table;
    List<Condition> where;
    List<Mutation> mutations;

    public MutateOperation(String table, List<Condition> where,
                           List<Mutation> mutations) {
        super();
        super.setOp("mutate");
        this.table = table;
        this.where = where;
        this.mutations = mutations;
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

    public List<Mutation> getMutations() {
        return mutations;
    }

    public void setMutations(List<Mutation> mutations) {
        this.mutations = mutations;
    }

    @Override
    public String toString() {
        return "MutateOperation [table=" + table + ", where=" + where
                + ", mutations=" + mutations + ", toString()="
                + super.toString() + "]";
    }
}
