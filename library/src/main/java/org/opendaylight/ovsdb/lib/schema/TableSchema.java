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
import org.opendaylight.ovsdb.lib.operations.Insert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


public class TableSchema<E extends TableSchema<E>> {

    protected static final Logger logger = LoggerFactory.getLogger(TableSchema.class);
    private String name;
    private Map<String, ColumnSchema> columns;

    public TableSchema() {
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

    public ColumnSchema getColumn(String column) {
        return this.columns.get(column);
    }

    public ColumnType getColumnType(String column) {
        return this.columns.get(column).getType();
    }

    public static TableSchema fromJson(String tableName, JsonNode json) {

        if (!json.isObject() || !json.has("columns")) {
            //todo specific types of exception
            throw new RuntimeException("bad tableschema root, expected \"columns\" as child");
        }

        Map<String, ColumnSchema> columns = new HashMap<>();
        for (Iterator<Map.Entry<String, JsonNode>> iter = json.get("columns").fields(); iter.hasNext(); ) {
            Map.Entry<String, JsonNode> column = iter.next();
            logger.debug("%s:%s", tableName, column.getKey());
            columns.put(column.getKey(), ColumnSchema.fromJson(column.getKey(), column.getValue()));
        }

        TableSchema tableSchema = new TableSchema(tableName, columns);
        return tableSchema;
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

    public <D> ColumnSchema<E, D> column(String column, Class<D> type) {
        //todo exception handling
        return columns.get(column);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
