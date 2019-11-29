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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

final class StringBaseType extends BaseType<StringBaseType> {
    // FIXME: negative minimum leng
    static final StringBaseType SINGLETON = new StringBaseType(Integer.MIN_VALUE, Integer.MAX_VALUE, null);
    static final BaseTypeFactory<StringBaseType> FACTORY = new Factory();

    private final int minLength;
    private final int maxLength;
    private final ImmutableSet<String> enums;

    StringBaseType(final int min, final int max, final ImmutableSet<String> enums) {
        this.minLength = min;
        this.maxLength = max;
        this.enums = enums;
    }

    @Override
    public Object toValue(final JsonNode value) {
        return value.asText();
    }

    @Override
    public void validate(final Object value) {

    }

    public int getMinLength() {
        return minLength;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public Set<String> getEnums() {
        return enums;
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

    private static final class Factory extends BaseTypeFactory<StringBaseType> {
        @Override
        StringBaseType create(final JsonNode typeDefinition) {
            final JsonNode typeMaxNode = typeDefinition.get("maxLength");
            final int max = typeMaxNode != null ? typeMaxNode.asInt() : Integer.MAX_VALUE;

            final JsonNode typeMinNode = typeDefinition.get("minLength");
            final int min = typeMinNode != null ? typeMinNode.asInt() : Integer.MIN_VALUE;

            final JsonNode typeEnumNode = typeDefinition.get("enum");
            final ImmutableSet<String> enums = typeEnumNode != null ? parseEnums(typeEnumNode) : null;

            return min == Integer.MIN_VALUE && max == Integer.MAX_VALUE && enums == null ? SINGLETON
                    : new StringBaseType(min, max, enums);
        }


        private static ImmutableSet<String> parseEnums(final JsonNode enumVal) {
            if (enumVal.isTextual()) {
                return ImmutableSet.of(enumVal.asText());
            }
            if (enumVal.isArray()) {
                final List<String> tmp = new ArrayList<>();
                JsonNode anEnum = enumVal.get(1);
                for (JsonNode enm : anEnum) {
                    tmp.add(enm.asText());
                }
                return ImmutableSet.copyOf(tmp);
            }
            return ImmutableSet.of();
        }
    }
}
