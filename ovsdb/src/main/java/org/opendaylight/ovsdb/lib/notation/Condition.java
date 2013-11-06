package org.opendaylight.ovsdb.lib.notation;

import org.opendaylight.ovsdb.lib.notation.json.ConditionSerializer;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
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
