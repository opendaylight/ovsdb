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

final class IntegerBaseType extends BaseType<IntegerBaseType> {
    private long min = Long.MIN_VALUE;
    private long max = Long.MAX_VALUE;
    private Set<Integer> enums;

    @Override
    void fillConstraints(final JsonNode type) {
        JsonNode typeMaxNode = type.get("maxInteger");
        if (typeMaxNode != null) {
            setMax(typeMaxNode.asLong());
        }
        JsonNode typeMinNode = type.get("minInteger");
        if (typeMinNode != null) {
            setMin(typeMinNode.asLong());
        }
        Optional<Set<Integer>> typeEnumsOpt = populateEnum(type);
        if (typeEnumsOpt.isPresent()) {
            setEnums(typeEnumsOpt.get());
        }
    }

    @Override
    public Object toValue(final JsonNode value) {
        return value.asLong();
    }

    @Override
    public void validate(final Object value) {

    }

    private static Optional<Set<Integer>> populateEnum(final JsonNode node) {
        if (node.has("enum")) {
            Set<Integer> nodesEnums = new HashSet<>();
            JsonNode anEnum = node.get("enum").get(1);
            for (JsonNode enm : anEnum) {
                nodesEnums.add(enm.asInt());
            }
            return Optional.of(nodesEnums);
        } else {
            return Optional.empty();
        }
    }

    public long getMin() {
        return min;
    }

    public void setMin(final long min) {
        this.min = min;
    }

    public long getMax() {
        return max;
    }

    public void setMax(final long max) {
        this.max = max;
    }

    public Set<Integer> getEnums() {
        return enums;
    }

    public void setEnums(final Set<Integer> enums) {
        this.enums = enums;
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
}