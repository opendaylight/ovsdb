/*
 * Copyright (c) 2014, 2015 EBay Software Foundation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.lib.operations;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.notation.Condition;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.schema.ColumnSchema;
import org.opendaylight.ovsdb.lib.schema.TableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;

public class Update extends Operation implements ConditionalOperation {

    private static final String UPDATE = "update";

    private Map<String, Object> row = Maps.newHashMap();
    private String uuid;

    private List<Condition> where = Lists.newArrayList();

    private String uuidName;

    public Update(TableSchema schema) {
        super(schema, UPDATE);
    }

    public Update on(TableSchema schema) {
        return this;
    }

    public Update(TableSchema schema, Row row) {
        super(schema, UPDATE);
        Collection<Column<?>> columns = row.getColumns();
        for (Column<?> column : columns) {
            this.set(column);
        }
    }

    public Update(TypedBaseTable typedTable) {
        this(typedTable.getSchema(), typedTable.getRow());
    }

    public <D> Update set(ColumnSchema<D> columnSchema, D value) {
        columnSchema.validate(value);
        Object untypedValue = columnSchema.getNormalizeData(value);
        this.row.put(columnSchema.getName(), untypedValue);
        return this;
    }

    public <D> Update set(Column<D> column) {
        ColumnSchema<D> columnSchema = column.getSchema();
        D value = column.getData();
        return this.set(columnSchema, value);
    }

    public Where where(Condition condition) {
        where.add(condition);
        return new Where(this);
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUuidName() {
        return uuidName;
    }

    public void setUuidName(String uuidName) {
        this.uuidName = uuidName;
    }

    public Map<String, Object> getRow() {
        return row;
    }

    public void setRow(Map<String, Object> row) {
        this.row = row;
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
