/*
 * Copyright (c) 2014, 2015 EBay Software Foundation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.opendaylight.ovsdb.lib.notation.OvsdbMap;

final class KeyValuedColumnType extends ColumnType {
    private final BaseType keyType;

    private KeyValuedColumnType(final BaseType keyType, final BaseType valueType, final long min, final long max) {
        super(valueType, min, max);
        this.keyType = keyType;
    }

    /**
     * Creates a ColumnType from the JsonNode if the implementation knows how to, returns null otherwise.
     *
     * @param json the JSONNode object that needs to converted
     * @return a valid SubType or Null (if the JsonNode does not represent the subtype)
     */
    static KeyValuedColumnType fromJsonNode(final JsonNode json) {
        if (json.isValueNode() || !json.has("value")) {
            return null;
        }
        BaseType jsonKeyType = BaseType.fromJson(json, "key");
        BaseType valueType = BaseType.fromJson(json, "value");

        return new KeyValuedColumnType(jsonKeyType, valueType, minFromJson(json), maxFromJson(json));
    }

    @Override
    public Object valueFromJson(final JsonNode node) {
        if (node.isArray() && node.size() == 2) {
            if (node.get(0).isTextual() && "map".equals(node.get(0).asText())) {
                OvsdbMap<Object, Object> map = new OvsdbMap<>();
                for (JsonNode pairNode : node.get(1)) {
                    if (pairNode.isArray() && node.size() == 2) {
                        Object key = keyType.toValue(pairNode.get(0));
                        Object value = getBaseType().toValue(pairNode.get(1));
                        map.put(key, value);
                    }
                }
                return map;
            } else if (node.size() == 0) {
                return null;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "KeyValuedColumnType [keyType=" + keyType + " " + super.toString() + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result
                + (keyType == null ? 0 : keyType.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        KeyValuedColumnType other = (KeyValuedColumnType) obj;
        if (keyType == null) {
            if (other.keyType != null) {
                return false;
            }
        } else if (!keyType.equals(other.keyType)) {
            return false;
        }
        return true;
    }
}