/*
 * Copyright (c) 2013, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.lib.notation;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.opendaylight.ovsdb.lib.notation.json.ConditionSerializer;

@JsonSerialize(using = ConditionSerializer.class)
public class Condition {

    String column;
    Function function;
    Object value;

    public Condition(String column, Function function, Object value) {
        super();
        this.column = column;
        this.function = function;
        this.value = value;
    }

    public String getColumn() {
        return column;
    }

    public void setColumn(String column) {
        this.column = column;
    }

    public Function getFunction() {
        return function;
    }

    public void setFunction(Function function) {
        this.function = function;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
