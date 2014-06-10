/*
 * Copyright (C) 2014 EBay Software Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Ashwin Raveendran
 */
package org.opendaylight.ovsdb.lib.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import org.opendaylight.ovsdb.lib.message.TableUpdate;
import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.operations.Insert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


public abstract class TableSchema<E extends TableSchema<E>> {


    protected static final Logger logger = LoggerFactory.getLogger(TableSchema.class);
    private String name;
    private Map<String, ColumnSchema> columns;

    public TableSchema() {
    }

    protected TableSchema(String name) {
        this.name = name;
    }

    public TableSchema(String name, Map<String, ColumnSchema> columns) {
        this.name = name;
        this.columns = columns;
    }

    public Set<String> getColumns() {
        return this.columns.keySet();
    }

    public Map<String, ColumnSchema> getColumnSchemas() {
        return columns;
    }

    public boolean hasColumn(String column) {
        return this.getColumns().contains(column);
    }


    public ColumnType getColumnType(String column) {
        return this.columns.get(column).getType();
    }

    public <E extends TableSchema<E>> E as(Class<E> clazz) {
        try {
            Constructor<E> e = clazz.getConstructor(TableSchema.class);
            return e.newInstance(this);
        } catch (Exception e) {
            throw new RuntimeException("exception constructing instance of clazz " + clazz, e);
        }
    }

    public Insert<E> insert() {
        return new Insert<>(this);
    }

    public <D> ColumnSchema<E, Set<D>> multiValuedColumn(String column, Class<D> type) {
        //todo exception handling

        ColumnSchema columnSchema = columns.get(column);
        columnSchema.validateType(type);
        return columnSchema;
    }

    public <D> ColumnSchema<E, D> column(String column, Class<D> type) {
        //todo exception handling

        ColumnSchema columnSchema = columns.get(column);
        columnSchema.validateType(type);
        return columnSchema;
    }

    public ColumnSchema column(String column) {
        return this.columns.get(column);
    }


    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    protected void setColumns(Map<String, ColumnSchema> columns) {
        this.columns = columns;
    }

    public TableUpdate<E> updatesFromJson(JsonNode value) {

        Map.Entry<String, JsonNode> idOldNew = value.fields().next();

        ObjectNode new_ = (ObjectNode) idOldNew.getValue().get("new");
        ObjectNode old = (ObjectNode) idOldNew.getValue().get("old");

        Row<E> newRow = new_ != null ? createRow(new_) : null;
        Row<E> oldRow = old != null ? createRow(old) : null;

        TableUpdate<E> tableUpdate = new TableUpdate<>();
        tableUpdate.setNew(newRow);
        tableUpdate.setOld(oldRow);


        return tableUpdate;
    }

    protected Row<E> createRow(ObjectNode rowNode) {
        List<Column<E, ?>> columns = Lists.newArrayList();
        for (Iterator<Map.Entry<String, JsonNode>> iter = rowNode.fields(); iter.hasNext();) {
            Map.Entry<String, JsonNode> next = iter.next();
            ColumnSchema<E, Object> schema = column(next.getKey(), Object.class);
            Object o = schema.valueFromJson(next.getValue());
            columns.add(new Column<>(schema, o));
        }
        return new Row<>(columns);
    }
}
