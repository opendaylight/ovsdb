/*
 * Copyright (C) 2014 EBay Software Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Ashwin Raveendran
 */
package org.opendaylight.ovsdb.lib.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.opendaylight.ovsdb.lib.notation.Condition;
import org.opendaylight.ovsdb.lib.notation.Function;


public class ColumnSchema<E extends TableSchema<E>, D> {
    String name;
    ColumnType type;
    boolean ephemeral;
    boolean mutable;

    public ColumnSchema(String name, ColumnType columnType) {
        this.name = name;
        this.type = columnType;
    }

    public static ColumnSchema fromJson(String name, JsonNode json) {
        if (!json.isObject() || !json.has("type")) {
            //todo specific types of exception
            throw new RuntimeException("bad column schema root, expected \"type\" as child");
        }

        return new ColumnSchema(name, ColumnType.fromJson(json.get("type")));
    }

    public String getName() {
        return name;
    }

    public ColumnType getType() { return type; }

    // --- Operations on the column ----------//

    public Condition opEqual(D data) {
        return new Condition(this.getName(), Function.EQUALS, data);
    }

    public Condition opGreaterThan(D data) {
        return new Condition(this.getName(), Function.GREATER_THAN, data);
    }

    public Condition opLesserThan(D data) {
        return new Condition(this.getName(), Function.GREATER_THAN, data);
    }

    public Condition opLesserThanOrEquals(D data) {
        return new Condition(this.getName(), Function.LESS_THAN_OR_EQUALS, data);
    }

    // --- Operations on the column ----------//:w


    @Override
    public String toString() {
        return "ColumnSchema{" +
                "name='" + name + '\'' +
                ", type=" + type +
                '}';
    }

    /**
     * Validates the passed in value against the constraints set for this ColumnSchema
     * @param value
     * @throws java.lang.RuntimeException (validation exception)
     */
    public D validate(Object value)throws RuntimeException {
        //todo(type check and validate based on constraints set)
        this.type.validate(value);
        return (D) value;
    }

    /**
     * Verifies if this Column if of the specified type
     * @param type the type to check for
     */
    public void validateType(Class<?> type) {

    }

    public D valueFromJson(JsonNode value) {
      return (D) this.getType().valueFromJson(value);
    }

}
