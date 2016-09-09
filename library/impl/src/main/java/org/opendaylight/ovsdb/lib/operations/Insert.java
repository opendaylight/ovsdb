/*
 * Copyright (c) 2014 EBay Software Foundation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.lib.operations;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.Map;
import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.schema.ColumnSchema;
import org.opendaylight.ovsdb.lib.schema.TableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;


public class Insert extends Operation {

    public static final String INSERT = "insert";

    private String uuid;

    @JsonProperty("uuid-name")
    private String uuidName;

    private Map<String, Object> row = Maps.newHashMap();

    public Insert on(TableSchema schema) {
        this.setTableSchema(schema);
        return this;
    }

    public Insert withId(String name) {
        this.uuidName = name;
        this.setOp(INSERT);
        return this;
    }


    public Insert(TableSchema schema) {
        super(schema, INSERT);
    }

    public Insert(TableSchema schema, Row row) {
        super(schema, INSERT);
        Collection<Column<?>> columns = row.getColumns();
        for (Column<?> column : columns) {
            this.value(column);
        }
    }

    public Insert(TypedBaseTable typedTable) {
        this(typedTable.getSchema(), typedTable.getRow());
    }

    public <D, C extends TableSchema> Insert value(ColumnSchema<D> columnSchema, D value) {
        Object untypedValue = columnSchema.getNormalizeData(value);
        row.put(columnSchema.getName(), untypedValue);
        return this;
    }

    public <D, C extends TableSchema> Insert value(Column<D> column) {
        ColumnSchema<D> columnSchema = column.getSchema();
        D value = column.getData();
        return this.value(columnSchema, value);
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

}
