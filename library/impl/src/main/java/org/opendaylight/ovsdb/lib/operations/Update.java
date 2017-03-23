/*
 * Copyright © 2014, 2017 EBay Software Foundation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.lib.operations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.notation.Condition;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.schema.ColumnSchema;
import org.opendaylight.ovsdb.lib.schema.TableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;

public class Update<E extends TableSchema<E>> extends Operation<E> implements ConditionalOperation {

    public static final String UPDATE = "update";

    private final Map<String, Object> row = new HashMap<>();
    private String uuid;

    private final List<Condition> where = new ArrayList<>();

    private String uuidName;

    public Update(TableSchema<E> schema) {
        super(schema, UPDATE);
    }

    public Update<E> on(TableSchema schema) {
        return this;
    }

    public Update(TableSchema<E> schema, Row<E> row) {
        super(schema, UPDATE);
        Collection<Column<E,?>> columns = row.getColumns();
        for (Column<E,?> column : columns) {
            this.set(column);
        }
    }

    public Update(TypedBaseTable<E> typedTable) {
        this(typedTable.getSchema(), typedTable.getRow());
    }

    public <T extends TableSchema<T>, D> Update<E> set(ColumnSchema<T, D> columnSchema, D value) {
        columnSchema.validate(value);
        Object untypedValue = columnSchema.getNormalizeData(value);
        this.row.put(columnSchema.getName(), untypedValue);
        return this;
    }

    public <T extends TableSchema<T>, D> Update<E> set(Column<T, D> column) {
        ColumnSchema<T, D> columnSchema = column.getSchema();
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
        this.row.clear();
        this.row.putAll(row);
    }

    @Override
    public void addCondition(Condition condition) {
        this.where.add(condition);
    }

    public List<Condition> getWhere() {
        return where;
    }

    public void setWhere(List<Condition> where) {
        this.where.clear();
        this.where.addAll(where);
    }
}
