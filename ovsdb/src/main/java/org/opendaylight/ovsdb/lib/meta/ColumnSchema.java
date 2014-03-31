package org.opendaylight.ovsdb.lib.meta;

import com.fasterxml.jackson.databind.JsonNode;
import org.opendaylight.ovsdb.lib.notation.Condition;
import org.opendaylight.ovsdb.lib.notation.Function;

/**
 * @author ashw7n
 */

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

    @Override
    public String toString() {
        return "ColumnType [type=" + type + ", ephemeral=" + ephemeral
                + ", mutable=" + mutable + "]";
    }

    /**
     * Validates the passed in value against the constraints set for this ColumnSchema
     * @param value
     * @throws java.lang.RuntimeException (validation exception)
     */
    public void validate(Object value)throws RuntimeException {
        //todo(type check and validate based on constraints set)
    }
}
