/*
 * Copyright (c) 2014, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.lib.operations;

import com.google.common.collect.Lists;
import java.util.List;
import org.opendaylight.ovsdb.lib.notation.Condition;
import org.opendaylight.ovsdb.lib.schema.ColumnSchema;
import org.opendaylight.ovsdb.lib.schema.TableSchema;

public class Select extends Operation implements ConditionalOperation {

    private static final String SELECT = "select";
    private List<Condition> where = Lists.newArrayList();
    private List<String> columns = Lists.newArrayList();

    public Select on(TableSchema schema) {
        this.setTableSchema(schema);
        return this;
    }

    public Select(TableSchema schema) {
        super(schema, SELECT);
    }

    public <D> Select column(ColumnSchema<D> columnSchema) {
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