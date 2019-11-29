/*
 * Copyright (c) 2014, 2015 EBay Software Foundation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import java.util.function.Function;
import org.opendaylight.ovsdb.lib.error.TyperException;
import org.opendaylight.ovsdb.lib.jsonrpc.JsonUtils;

public abstract class ColumnType {
    private static final ImmutableList<Function<JsonNode, ColumnType>> FACTORIES = ImmutableList.of(
        AtomicColumnType::fromJsonNode, KeyValuedColumnType::fromJsonNode);

    private final BaseType baseType;

    private long min = 1;
    private long max = 1;

    ColumnType(final BaseType baseType) {
        this.baseType = baseType;
    }

    /**
     * JSON.
     * <pre>
            "type": {
                "key": {
                     "maxInteger": 4294967295,
                     "minInteger": 0,
                     "type": "integer"
                },
                "min": 0,
                "value": {
                    "type": "uuid",
                    "refTable": "Queue"
                 },
                 "max": "unlimited"
            }</pre>
     */
    public static ColumnType fromJson(final JsonNode json) {
        for (Function<JsonNode, ColumnType> factory : FACTORIES) {
            ColumnType columnType = factory.apply(json);
            if (null != columnType) {
                return columnType;
            }
        }
        //todo move to speicfic typed exception
        throw new TyperException(String.format("could not find the right column type %s",
                JsonUtils.prettyString(json)));
    }

    public BaseType getBaseType() {
        return baseType;
    }

    public long getMin() {
        return min;
    }

    void setMin(final long min) {
        this.min = min;
    }

    public long getMax() {
        return max;
    }

    void setMax(final long max) {
        this.max = max;
    }

    /*
     * Per RFC 7047, Section 3.2 <type> :
     * If "min" or "max" is not specified, each defaults to 1.  If "max" is specified as "unlimited",
     * then there is no specified maximum number of elements, although the implementation will
     * enforce some limit.  After considering defaults, "min" must be exactly 0 or
     * exactly 1, "max" must be at least 1, and "max" must be greater than or equal to "min".
     *
     * If "min" and "max" are both 1 and "value" is not specified, the
     * type is the scalar type specified by "key".
     */
    public boolean isMultiValued() {
        return this.min != this.max;
    }

    public abstract Object valueFromJson(JsonNode value);

    public void validate(final Object value) {
        baseType.validate(value);
    }

    @Override
    public String toString() {
        return "ColumnType{"
                + "baseType=" + baseType
                + ", min=" + min
                + ", max=" + max
                + '}';
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + (baseType == null ? 0 : baseType.hashCode());
        result = prime * result + (int) (max ^ max >>> 32);
        result = prime * result + (int) (min ^ min >>> 32);
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ColumnType other = (ColumnType) obj;
        if (baseType == null) {
            if (other.baseType != null) {
                return false;
            }
        } else if (!baseType.equals(other.baseType)) {
            return false;
        }
        if (max != other.max) {
            return false;
        }
        if (min != other.min) {
            return false;
        }
        return true;
    }
}
