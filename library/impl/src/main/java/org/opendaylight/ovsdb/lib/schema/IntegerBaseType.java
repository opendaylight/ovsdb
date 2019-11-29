/*
 * Copyright © 2014, 2017 EBay Software Foundation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableSet;
import java.util.Set;

final class IntegerBaseType extends BaseType<IntegerBaseType> {
    static final IntegerBaseType SINGLETON = new IntegerBaseType(Long.MIN_VALUE, Long.MAX_VALUE, null);
    static final BaseTypeFactory<IntegerBaseType> FACTORY = new Factory();

    private final long min;
    private final long max;
    private final ImmutableSet<Integer> enums;

    IntegerBaseType(final long min, final long max, final ImmutableSet<Integer> enums) {
        this.min = min;
        this.max = max;
        this.enums = enums;
    }

    @Override
    public Object toValue(final JsonNode value) {
        return value.asLong();
    }

    @Override
    public void validate(final Object value) {

    }

    public long getMin() {
        return min;
    }

    public long getMax() {
        return max;
    }

    public Set<Integer> getEnums() {
        return enums;
    }

    @Override
    public String toString() {
        return "IntegerBaseType";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (enums == null ? 0 : enums.hashCode());
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
        IntegerBaseType other = (IntegerBaseType) obj;
        if (enums == null) {
            if (other.enums != null) {
                return false;
            }
        } else if (!enums.equals(other.enums)) {
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

    private static final class Factory extends BaseTypeFactory.WithEnum<IntegerBaseType, Integer> {
        @Override
        IntegerBaseType create(final JsonNode typeDefinition) {
            final JsonNode typeMaxNode = typeDefinition.get("maxInteger");
            final long max = typeMaxNode != null ? typeMaxNode.asLong() : Long.MAX_VALUE;

            final JsonNode typeMinNode = typeDefinition.get("minInteger");
            final long min = typeMinNode != null ? typeMinNode.asLong() : Long.MIN_VALUE;

            final JsonNode typeEnumNode = typeDefinition.get("enum");
            final ImmutableSet<Integer> enums = typeEnumNode != null ? parseEnums(typeEnumNode) : null;

            return min == Long.MIN_VALUE && max == Long.MAX_VALUE && enums == null ? SINGLETON
                    : new IntegerBaseType(min, max, enums);
        }

        @Override
        Integer getEnumValue(final JsonNode jsonEnum) {
            return jsonEnum.asInt();
        }
    }
}
