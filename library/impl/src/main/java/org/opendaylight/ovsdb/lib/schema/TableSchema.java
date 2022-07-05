/*
 * Copyright (c) 2014, 2015 EBay Software Foundation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib.schema;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.opendaylight.ovsdb.lib.message.TableUpdate;
import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.Insert;
import org.opendaylight.yangtools.util.CollectionWrappers;

public abstract class TableSchema<E extends TableSchema<E>> {
    private static final AtomicColumnType UUID_COLUMN_TYPE = new AtomicColumnType(UuidBaseType.SINGLETON);
    protected static final ColumnSchema UUID_COLUMN_SCHMEMA = new ColumnSchema("_uuid", UUID_COLUMN_TYPE);
    protected static final ColumnSchema VERSION_COLUMN_SCHMEMA = new ColumnSchema("_version", UUID_COLUMN_TYPE);

    private final String name;
    private final ImmutableMap<String, ColumnSchema> columns;

    private volatile List<String> columnList;

    protected TableSchema(final String name) {
        this(name, ImmutableMap.of());
    }

    protected TableSchema(final String name, final Map<String, ColumnSchema> columns) {
        this.name = requireNonNull(name);
        this.columns = ImmutableMap.copyOf(columns);
    }

    public Set<String> getColumns() {
        return columns.keySet();
    }

    public List<String> getColumnList() {
        final List<String> local = columnList;
        return local != null ? local : populateColumnList();
    }

    private synchronized List<String> populateColumnList() {
        List<String> local = columnList;
        if (local == null) {
            columnList = local = CollectionWrappers.wrapAsList(columns.keySet());
        }
        return local;
    }

    public Map<String, ColumnSchema> getColumnSchemas() {
        return columns;
    }

    public boolean hasColumn(final String column) {
        return columns.containsKey(column);
    }

    public ColumnType getColumnType(final String column) {
        return columns.get(column).getType();
    }

    public <E extends TableSchema<E>> E as(final Class<E> clazz) {
        try {
            Constructor<E> instance = clazz.getConstructor(TableSchema.class);
            return instance.newInstance(this);
        } catch (InstantiationException | IllegalAccessException
                | InvocationTargetException | NoSuchMethodException e) {
            throw new IllegalStateException("exception constructing instance of clazz " + clazz, e);
        }
    }

    public Insert<E> insert() {
        return new Insert<>(this);
    }

    public <D> ColumnSchema<E, Set<D>> multiValuedColumn(final String column, final Class<D> type) {
        //todo exception handling

        ColumnSchema<E, Set<D>> columnSchema = columns.get(column);
        columnSchema.validateType(type);
        return columnSchema;
    }

    public <K,V> ColumnSchema<E, Map<K,V>> multiValuedColumn(final String column, final Class<K> keyType,
            final Class<V> valueType) {
        //todo exception handling

        ColumnSchema<E, Map<K, V>> columnSchema = columns.get(column);
        columnSchema.validateType(valueType);
        return columnSchema;
    }

    public <D> ColumnSchema<E, D> column(final String column, final Class<D> type) {
        //todo exception handling

        ColumnSchema<E, D> columnSchema = columns.get(column);
        if (columnSchema != null) {
            columnSchema.validateType(type);
        }
        return columnSchema;
    }

    public ColumnSchema column(final String column) {
        return columns.get(column);
    }

    public String getName() {
        return name;
    }

    public TableUpdate<E> updatesFromJson(final JsonNode value) {
        TableUpdate<E> tableUpdate = new TableUpdate<>();
        Iterator<Entry<String, JsonNode>> fields = value.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> idOldNew = fields.next();
            String uuid = idOldNew.getKey();

            ObjectNode newObjectNode = (ObjectNode) idOldNew.getValue().get("new");
            ObjectNode oldObjectNode = (ObjectNode) idOldNew.getValue().get("old");

            Row<E> newRow = newObjectNode != null ? createRow(newObjectNode) : null;
            Row<E> oldRow = oldObjectNode != null ? createRow(oldObjectNode) : null;

            tableUpdate.addRow(new UUID(uuid), oldRow, newRow);
        }
        return tableUpdate;
    }

    public Row<E> createRow(final ObjectNode rowNode) {
        List<Column<E, ?>> newColumns = new ArrayList<>();
        for (Iterator<Map.Entry<String, JsonNode>> iter = rowNode.fields(); iter.hasNext();) {
            Map.Entry<String, JsonNode> next = iter.next();
            ColumnSchema<E, Object> schema = column(next.getKey(), Object.class);
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
        return new Row<>(this, newColumns);
    }

    public List<Row<E>> createRows(final JsonNode rowsNode) {
        List<Row<E>> rows = new ArrayList<>();
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
    public abstract E withInternallyGeneratedColumns();

    protected final boolean haveInternallyGeneratedColumns() {
        return hasColumn(UUID_COLUMN_SCHMEMA.getName()) && hasColumn(VERSION_COLUMN_SCHMEMA.getName());
    }
}
