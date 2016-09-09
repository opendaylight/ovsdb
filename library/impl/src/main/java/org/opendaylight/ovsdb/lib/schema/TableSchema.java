/*
 * Copyright (c) 2014, 2015 EBay Software Foundation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.lib.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.opendaylight.ovsdb.lib.error.BadSchemaException;
import org.opendaylight.ovsdb.lib.message.TableUpdate;
import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.Insert;
import org.opendaylight.ovsdb.lib.schema.BaseType.UuidBaseType;
import org.opendaylight.ovsdb.lib.schema.ColumnType.AtomicColumnType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TableSchema {
    private static final Logger LOG = LoggerFactory.getLogger(TableSchema.class);

    private String name;
    private Map<String, ColumnSchema> columns;

    public TableSchema() {
    }

    public TableSchema(String name) {
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

    public <E extends TableSchema> E as(Class<E> clazz) {
        try {
            Constructor<E> instance = clazz.getConstructor(TableSchema.class);
            return instance.newInstance(this);
        } catch (InstantiationException | IllegalAccessException
                | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException("exception constructing instance of clazz " + clazz, e);
        }
    }

    public Insert insert() {
        return new Insert(this);
    }

    public <D> ColumnSchema<Set<D>> multiValuedColumn(String column, Class<D> type) {
        //todo exception handling

        ColumnSchema<Set<D>> columnSchema = columns.get(column);
        columnSchema.validateType(type);
        return columnSchema;
    }

    public <K,V> ColumnSchema<Map<K,V>> multiValuedColumn(String column, Class<K> keyType, Class<V> valueType) {
        //todo exception handling

        ColumnSchema<Map<K, V>> columnSchema = columns.get(column);
        columnSchema.validateType(valueType);
        return columnSchema;
    }

    public <D> ColumnSchema<D> column(String column, Class<D> type) {
        //todo exception handling

        ColumnSchema<D> columnSchema = columns.get(column);
        if (columnSchema != null) {
            columnSchema.validateType(type);
        }
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

    public TableUpdate updatesFromJson(JsonNode value) {
        TableUpdate tableUpdate = new TableUpdate();
        Iterator<Entry<String, JsonNode>> fields = value.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> idOldNew = fields.next();
            String uuid = idOldNew.getKey();

            ObjectNode newObjectNode = (ObjectNode) idOldNew.getValue().get("new");
            ObjectNode oldObjectNode = (ObjectNode) idOldNew.getValue().get("old");

            Row newRow = newObjectNode != null ? createRow(newObjectNode) : null;
            Row oldRow = oldObjectNode != null ? createRow(oldObjectNode) : null;

            tableUpdate.addRow(new UUID(uuid), oldRow, newRow);
        }
        return tableUpdate;
    }

    public Row createRow(ObjectNode rowNode) {
        List<Column<?>> newColumns = new ArrayList<>();
        for (Iterator<Map.Entry<String, JsonNode>> iter = rowNode.fields(); iter.hasNext();) {
            Map.Entry<String, JsonNode> next = iter.next();
            ColumnSchema<Object> schema = column(next.getKey(), Object.class);
            /*
             * Ideally the ColumnSchema shouldn't be null at this stage. But there can be cases in which
             * the OVSDB manager Schema implementation might decide to include some "hidden" columns that
             * are NOT reported in getSchema, but decide to report it in unfiltered monitor.
             * Hence adding some safety checks around that.
             */
            if (schema != null) {
                Object value = schema.valueFromJson(next.getValue());
                newColumns.add(new Column<>(schema, value));
            }
        }
        return new Row(this, newColumns);
    }

    public List<Row> createRows(JsonNode rowsNode) {
        List<Row> rows = new ArrayList<>();
        for (JsonNode rowNode : rowsNode.get("rows")) {
            rows.add(createRow((ObjectNode)rowNode));
        }

        return rows;
    }

    /*
     * RFC 7047 Section 3.2 specifies 2 internally generated columns in each table
     * namely _uuid and _version which are not exposed in get_schema call.
     * Since these 2 columns are extremely useful for Mutate, update and select operations,
     * the ColumnSchema for these 2 columns are manually populated.
     *
     * It is to be noted that these 2 columns are specified as part of the RFC7047 and not
     * a specific Schema implementation detail & hence adding it by default in the Library
     * for better application experience using the library.
     */
    public void populateInternallyGeneratedColumns() {
        columns.put("_uuid", new ColumnSchema("_uuid", new AtomicColumnType(new UuidBaseType())));
        columns.put("_version", new ColumnSchema("_version", new AtomicColumnType(new UuidBaseType())));
    }

    public static TableSchema fromJson(String tableName, JsonNode json) {

        if (!json.isObject() || !json.has("columns")) {
            throw new BadSchemaException("bad tableschema root, expected \"columns\" as child");
        }

        Map<String, ColumnSchema> columns = new HashMap<>();
        for (Iterator<Map.Entry<String, JsonNode>> iter = json.get("columns").fields(); iter.hasNext(); ) {
            Map.Entry<String, JsonNode> column = iter.next();
            LOG.trace("{}:{}", tableName, column.getKey());
            columns.put(column.getKey(), ColumnSchema.fromJson(column.getKey(), column.getValue()));
        }

        return new TableSchema(tableName, columns);
    }
}
