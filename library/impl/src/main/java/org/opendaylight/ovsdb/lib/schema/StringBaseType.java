package org.opendaylight.ovsdb.lib.schema;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/*
 * Copyright © 2014, 2017 EBay Software Foundation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
final class StringBaseType extends BaseType<StringBaseType> {
    private int minLength = Integer.MIN_VALUE;
    private int maxLength = Integer.MAX_VALUE;
    private Set<String> enums;

    @Override
    void fillConstraints(final JsonNode type) {
        JsonNode typeMaxNode = type.get("maxLength");
        if (typeMaxNode != null) {
            setMaxLength(typeMaxNode.asInt());
        }
        JsonNode typeMinNode = type.get("minLength");
        if (typeMinNode != null) {
            setMinLength(typeMinNode.asInt());
        }
        Optional<Set<String>> typeEnumsOpt = populateEnum(type);
        if (typeEnumsOpt.isPresent()) {
            setEnums(typeEnumsOpt.get());
        }
    }

    @Override
    public Object toValue(final JsonNode value) {
        return value.asText();
    }

    @Override
    public void validate(final Object value) {

    }

    private static Optional<Set<String>> populateEnum(final JsonNode node) {
        if (node.has("enum")) {
            Set<String> nodesEnums = new HashSet<>();
            JsonNode enumVal = node.get("enum");
            if (enumVal.isArray()) {
                JsonNode anEnum = enumVal.get(1);
                for (JsonNode enm : anEnum) {
                    nodesEnums.add(enm.asText());
                }
            } else if (enumVal.isTextual()) {
                nodesEnums.add(enumVal.asText());
            }
            return Optional.of(nodesEnums);
        } else {
            return Optional.empty();
        }
    }

    public int getMinLength() {
        return minLength;
    }

    public void setMinLength(final int minLength) {
        this.minLength = minLength;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(final int maxLength) {
        this.maxLength = maxLength;
    }

    public Set<String> getEnums() {
        return enums;
    }

    public void setEnums(final Set<String> enums) {
        this.enums = enums;
    }

    @Override
    public String toString() {
        return "StringBaseType";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (enums == null ? 0 : enums.hashCode());
        result = prime * result + maxLength;
        result = prime * result + minLength;
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
        StringBaseType other = (StringBaseType) obj;
        if (enums == null) {
            if (other.enums != null) {
                return false;
            }
        } else if (!enums.equals(other.enums)) {
            return false;
        }
        if (maxLength != other.maxLength) {
            return false;
        }
        if (minLength != other.minLength) {
            return false;
        }
        return true;
    }
}
