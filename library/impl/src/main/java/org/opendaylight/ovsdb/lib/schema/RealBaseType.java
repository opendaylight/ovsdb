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

final class RealBaseType extends BaseType<RealBaseType> {
    static final RealBaseType SINGLETON = new RealBaseType(Double.MIN_VALUE, Double.MAX_VALUE, null);
    static final BaseTypeFactory<RealBaseType> FACTORY = new Factory();

    private final double min;
    private final double max;
    private final ImmutableSet<Double> enums;

    RealBaseType(final double min, final double max, final ImmutableSet<Double> enums) {
        this.min = min;
        this.max = max;
        this.enums = enums;
    }

    @Override
    public Object toValue(final JsonNode value) {
        return value.asDouble();
    }

    @Override
    public void validate(final Object value) {

    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public Set<Double> getEnums() {
        return enums;
    }

    @Override
    public String toString() {
        return "RealBaseType";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (enums == null ? 0 : enums.hashCode());
        long temp;
        temp = Double.doubleToLongBits(max);
        result = prime * result + (int) (temp ^ temp >>> 32);
        temp = Double.doubleToLongBits(min);
        result = prime * result + (int) (temp ^ temp >>> 32);
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
        RealBaseType other = (RealBaseType) obj;
        if (enums == null) {
            if (other.enums != null) {
                return false;
            }
        } else if (!enums.equals(other.enums)) {
            return false;
        }
        if (Double.doubleToLongBits(max) != Double.doubleToLongBits(other.max)) {
            return false;
        }
        if (Double.doubleToLongBits(min) != Double.doubleToLongBits(other.min)) {
            return false;
        }
        return true;
    }

    private static final class Factory extends BaseTypeFactory.WithEnum<RealBaseType, Double> {
        @Override
        RealBaseType create(final JsonNode typeDefinition) {
            // FIXME: is asLong() appropriate here?
            final JsonNode typeMaxNode = typeDefinition.get("maxReal");
            final double max = typeMaxNode != null ? typeMaxNode.asLong() : Double.MAX_VALUE;

            final JsonNode typeMinNode = typeDefinition.get("minReal");
            final double min = typeMinNode != null ? typeMinNode.asLong() : Double.MIN_VALUE;

            final JsonNode typeEnumNode = typeDefinition.get("enum");
            final ImmutableSet<Double> enums = typeEnumNode != null ? parseEnums(typeEnumNode) : null;

            // TODO: improve accuracy here -- requires understanding the FIXME above
            return typeMinNode == null && typeMaxNode == null && enums == null ? SINGLETON
                    : new RealBaseType(min, max, enums);
        }

        @Override
        Double getEnumValue(final JsonNode jsonEnum) {
            return jsonEnum.asDouble();
        }
    }
}
