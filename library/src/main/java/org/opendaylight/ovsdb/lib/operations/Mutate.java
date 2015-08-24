/*
 * Copyright (c) 2014, 2015 Matt Oswalt and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib.operations;

import java.util.List;

import org.opendaylight.ovsdb.lib.notation.Condition;
import org.opendaylight.ovsdb.lib.notation.Mutation;
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.schema.ColumnSchema;
import org.opendaylight.ovsdb.lib.schema.TableSchema;

import com.google.common.collect.Lists;

public class Mutate<E extends TableSchema<E>> extends Operation<E> implements ConditionalOperation {

    public static final String MUTATE = "mutate";
    List<Condition> where = Lists.newArrayList();
    private List<Mutation> mutations = Lists.newArrayList();

    public Mutate on(TableSchema schema) {
        this.setTableSchema(schema);
        return this;
    }

    public Mutate(TableSchema<E> schema) {
        super(schema, MUTATE);
    }

    public <T extends TableSchema<T>, D> Mutate<E> addMutation(ColumnSchema<T, D> columnSchema,
                                                               Mutator mutator, D value) {
        columnSchema.validate(value);
        Object untypedValue = columnSchema.getNormalizeData(value);
        mutations.add(new Mutation(columnSchema.getName(), mutator, untypedValue));
        return this;
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
        this.where.add(condition);
        return new Where(this);
    }

    public List<Condition> getWhere() {
        return where;
    }

    public void setWhere(List<Condition> where) {
        this.where = where;
    }
}