/*
 *
 *  * Copyright (C) 2014 EBay Software Foundation
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  * and is available at http://www.eclipse.org/legal/epl-v10.html
 *  *
 *  * Authors : Ashwin Raveendran
 *
 */

package org.opendaylight.ovsdb.lib.operations;

import java.util.List;
import java.util.Map;

import org.opendaylight.ovsdb.lib.notation.Condition;
import org.opendaylight.ovsdb.lib.schema.ColumnSchema;
import org.opendaylight.ovsdb.lib.schema.TableSchema;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class Update<E extends TableSchema<E>> extends Operation<E> implements ConditionalOperation {

    public static final String UPDATE = "update";

    Map<String, Object> row = Maps.newHashMap();
    String uuid;
    //Where where;
    List<Condition> where = Lists.newArrayList();

    private String uuidName;

    public Update(TableSchema<E> schema) {
        super(schema, UPDATE);
    }

    public Update<E> on(TableSchema schema){
        return this;
    }

    public <T extends TableSchema<T>, D> Update<E> set(ColumnSchema<T, D> columnSchema, D value) {
        columnSchema.validate(value);
        Object untypedValue = columnSchema.getNormalizeData(value);
        this.row.put(columnSchema.getName(), untypedValue);
        return this;
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
