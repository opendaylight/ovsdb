/*
 *
 *  * Copyright (C) 2014 Red Hat Inc,
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  * and is available at http://www.eclipse.org/legal/epl-v10.html
 *  *
 *  * Authors : Dave Tucker
 *
 */

package org.opendaylight.ovsdb.lib.operations;

import com.google.common.collect.Lists;
import org.opendaylight.ovsdb.lib.notation.Condition;
import org.opendaylight.ovsdb.lib.schema.TableSchema;

import java.util.List;

public class Delete<E extends TableSchema<E>> extends Operation<E> implements ConditionalOperation {

    public static final String DELETE = "delete";

    String uuid;
    //Where where;
    List<Condition> where = Lists.newArrayList();

    private String uuidName;

    public Delete(TableSchema<E> schema) {
        super(schema, DELETE);
    }

    public Delete<E> on(TableSchema schema){
        return this;
    }

    public Where where(Condition condition) {
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
