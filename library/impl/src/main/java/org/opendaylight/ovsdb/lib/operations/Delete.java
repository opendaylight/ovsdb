/*
 * Copyright (c) 2014, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.lib.operations;

import com.google.common.collect.Lists;
import java.util.List;
import org.opendaylight.ovsdb.lib.notation.Condition;
import org.opendaylight.ovsdb.lib.schema.TableSchema;

public class Delete extends Operation implements ConditionalOperation {

    public static final String DELETE = "delete";
    private List<Condition> where = Lists.newArrayList();
    private Integer count;

    public Delete(TableSchema schema) {
        super(schema, DELETE);
    }

    public Delete on(TableSchema schema) {
        return this;
    }

    public Where where(Condition condition) {
        where.add(condition);
        return new Where(this);
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

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }
}
