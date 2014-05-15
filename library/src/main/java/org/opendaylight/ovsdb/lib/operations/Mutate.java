/*
 * Copyright (C) 2014 Matt Oswalt
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Matt Oswalt
 *
 */
package org.opendaylight.ovsdb.lib.operations;

import com.google.common.collect.Lists;
import org.opendaylight.ovsdb.lib.notation.Condition;
import org.opendaylight.ovsdb.lib.notation.Mutation;
import org.opendaylight.ovsdb.lib.schema.ColumnSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.TableSchema;

import java.util.List;

public class Mutate<E extends TableSchema<E>> extends Operation<E> implements ConditionalOperation {

    public static final String MUTATE = "mutate";
    List<Condition> where = Lists.newArrayList();
    private List<Mutation> mutations = Lists.newArrayList();
    private int count;

    public Mutate on(TableSchema schema){
        this.setTableSchema(schema);
        return this;
    }

    public Mutate(TableSchema<E> schema) {
        super(schema, MUTATE);
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public List<Mutation> getMutations() {
        return mutations;
    }

    public void setMutations(List<Mutation> mutations) {
        this.mutations = mutations;
    }

    @Override
    public void addCondition(Condition condition) {
        this.where.add(condition);
    }

    public Where where(Condition condition) {
        return new Where(this);
    }

    public List<Condition> getWhere() {
        return where;
    }

    public void setWhere(List<Condition> where) {
        this.where = where;
    }
}