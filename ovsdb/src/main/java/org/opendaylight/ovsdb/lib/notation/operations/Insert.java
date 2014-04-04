/*
 * Copyright (C) 2014 EBay Software Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Ashwin Raveendran
 */
package org.opendaylight.ovsdb.lib.notation.operations;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Maps;
import org.opendaylight.ovsdb.lib.schema.ColumnSchema;
import org.opendaylight.ovsdb.lib.schema.TableSchema;

import java.util.Map;


public class Insert<E extends TableSchema<E>> extends Operation<E> {

    public static final String INSERT = "insert";

    String uuid;

    @JsonProperty("uuid-name")
    private String uuidName;

    private Map<String, Object> row = Maps.newHashMap();

    public Insert on(TableSchema schema){
        this.setTableSchema(schema);
        return this;
    }

    public Insert withId(String name) {
        this.uuidName = name;
        this.setOp(INSERT);
        return this;
    }


    public Insert(TableSchema<E> schema) {
        super(schema, INSERT);
    }

    public <D, C extends TableSchema<C>> Insert<E> value(ColumnSchema<C, D> columnSchema, D value) {
        row.put(columnSchema.getName(), value);
        return this;
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
