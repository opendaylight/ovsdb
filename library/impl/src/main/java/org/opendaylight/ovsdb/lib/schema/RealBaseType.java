/*
 * Copyright © 2014, 2017 EBay Software Foundation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib.schema;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

final class RealBaseType extends BaseType<RealBaseType> {
    static final RealBaseType SINGLETON = new RealBaseType();

    private double min = Double.MIN_VALUE;
    private double max = Double.MAX_VALUE;
    private Set<Double> enums;

    @Override
    void fillConstraints(final JsonNode type) {
        JsonNode typeMaxNode = type.get("maxReal");
        if (typeMaxNode != null) {
            max = typeMaxNode.asLong();
        }
        JsonNode typeMinNode = type.get("minReal");
        if (typeMinNode != null) {
            min = typeMinNode.asLong();
        }
        Optional<Set<Double>> typeEnumsOpt = populateEnum(type);
        if (typeEnumsOpt.isPresent()) {
            enums = typeEnumsOpt.get();
        }
    }

    @Override
    public Object toValue(final JsonNode value) {
        return value.asDouble();
    }

    @Override
    public void validate(final Object value) {

    }

    private static Optional<Set<Double>> populateEnum(final JsonNode node) {
        if (node.has("enum")) {
            Set<Double> nodesEnums = new HashSet<>();
            JsonNode anEnum = node.get("enum").get(1);
            for (JsonNode enm : anEnum) {
                nodesEnums.add(enm.asDouble());
            }
            return Optional.of(nodesEnums);
        } else {
            return Optional.empty();
        }
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
}