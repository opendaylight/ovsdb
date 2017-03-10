/*
 * Copyright © 2014, 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.lib.operations;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.ovsdb.lib.notation.Condition;
import org.opendaylight.ovsdb.lib.schema.ColumnSchema;
import org.opendaylight.ovsdb.lib.schema.TableSchema;

public class Select<E extends TableSchema<E>> extends Operation<E> implements ConditionalOperation {

    public static final String SELECT = "select";
    private List<Condition> where = new ArrayList<>();
    private List<String> columns = new ArrayList<>();

    public Select on(TableSchema schema) {
        this.setTableSchema(schema);
        return this;
    }

    public Select(TableSchema<E> schema) {
        super(schema, SELECT);
    }

    public <D, C extends TableSchema<C>> Select<E> column(ColumnSchema<C, D> columnSchema) {
        columns.add(columnSchema.getName());
        return this;
    }

    public Where where(Condition condition) {
        where.add(condition);
        return new Where(this);
    }

    public List<String> getColumns() {
        return columns;
    }

    public void setColumns(List<String> columns) {
        this.columns = columns;
    }

    @Override
    public void addCondition(Condition condition) {
        this.where.add(condition);
    }

    public List<Condition> getWhere() {
        return where;
    }

    public void setWhere(List<Condition> where) {
        this.where = where;
    }
}