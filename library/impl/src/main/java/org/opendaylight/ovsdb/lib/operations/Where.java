/*
 * Copyright (c) 2014, 2015 EBay Software Foundation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.lib.operations;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.opendaylight.ovsdb.lib.notation.Condition;
import org.opendaylight.ovsdb.lib.notation.Function;
import org.opendaylight.ovsdb.lib.schema.ColumnSchema;

public class Where {

    @JsonIgnore
    ConditionalOperation operation;

    public Where() {
    }

    public Where(ConditionalOperation operation) {
        this.operation = operation;
    }

    public Where condition(Condition condition) {
        operation.addCondition(condition);
        return this;
    }

    public Where condition(ColumnSchema column, Function function, Object value) {
        this.condition(new Condition(column.getName(), function, value));
        return this;
    }

    public Where and(ColumnSchema column, Function function, Object value) {
        condition(column, function, value);
        return this;
    }

    public Where and(Condition condition) {
        condition(condition);
        return this;
    }

    public Operation build() {
        return (Operation) this.operation;
    }

}
