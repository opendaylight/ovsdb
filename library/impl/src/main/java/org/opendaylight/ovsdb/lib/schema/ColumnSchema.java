/*
 * Copyright (c) 2014, 2015 EBay Software Foundation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib.schema;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.opendaylight.ovsdb.lib.error.BadSchemaException;
import org.opendaylight.ovsdb.lib.notation.Condition;
import org.opendaylight.ovsdb.lib.notation.Function;
import org.opendaylight.ovsdb.lib.notation.OvsdbMap;
import org.opendaylight.ovsdb.lib.notation.OvsdbSet;

public class ColumnSchema<E extends TableSchema<E>, D> {
    private final String name;
    private final ColumnType type;

    public ColumnSchema(final String name, final ColumnType columnType) {
        this.name = name;
        this.type = columnType;
    }

    public static <E extends TableSchema<E>, D> ColumnSchema<E, D> fromJson(final String name, final JsonNode json) {
        if (!json.isObject()) {
            throw new BadSchemaException("bad column schema root, expected an object");
        }
        final JsonNode type = json.get("type");
        if (type == null) {
            throw new BadSchemaException("bad column schema root, expected \"type\" as child");
        }
        return new ColumnSchema<>(name, ColumnType.fromJson(type));
    }

    public String getName() {
        return name;
    }

    public ColumnType getType() {
        return type;
    }

    // --- Operations on the column ----------//

    public Condition opEqual(final D data) {
        return new Condition(this.getName(), Function.EQUALS, data);
    }

    public Condition opGreaterThan(final D data) {
        return new Condition(this.getName(), Function.GREATER_THAN, data);
    }

    public Condition opLesserThan(final D data) {
        return new Condition(this.getName(), Function.GREATER_THAN, data);
    }

    public Condition opLesserThanOrEquals(final D data) {
        return new Condition(this.getName(), Function.LESS_THAN_OR_EQUALS, data);
    }

    public Condition opIncludes(final D data) {
        return new Condition(this.getName(), Function.INCLUDES, data);
    }

    public Condition opExcludes(final D data) {
        return new Condition(this.getName(), Function.EXCLUDES, data);
    }

    // --- Operations on the column ----------//:w


    @Override
    public String toString() {
        return "ColumnSchema{"
                + "name='" + name + '\''
                + ", type=" + type
                + '}';
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (name == null ? 0 : name.hashCode());
        result = prime * result + (type == null ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final ColumnSchema<?, ?> other = (ColumnSchema<?, ?>) obj;
        return Objects.equals(name, other.name) && Objects.equals(type, other.type);
    }

    /**
     * Validates the passed in value against the constraints set for this ColumnSchema.
     */
    public D validate(final Object value) {
        //todo(type check and validate based on constraints set)
        this.type.validate(value);
        return (D) value;
    }

    /**
     * Verifies if this Column if of the specified type.
     * @param typeClass the type to check for
     */
    public void validateType(final Class<?> typeClass) {

    }

    public D valueFromJson(final JsonNode value) {
        return (D) this.getType().valueFromJson(value);
    }

    public Object getNormalizeData(final D value) {
        Object untypedValue;
        if (value instanceof Set) {
            untypedValue = OvsdbSet.fromSet((Set) value);
        } else if (value instanceof Map) {
            untypedValue = OvsdbMap.fromMap((Map) value);
        } else {
            untypedValue = value;
        }
        return untypedValue;
    }
}
