/*
 * Copyright (c) 2014, 2015 EBay Software Foundation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.opendaylight.ovsdb.lib.notation.OvsdbSet;

final class AtomicColumnType extends ColumnType {
    AtomicColumnType(final BaseType baseType) {
        super(baseType);
    }

    /**
     * Creates a ColumnType from the JsonNode if the implementation knows how to, returns null otherwise.
     *
     * @param json the JSONNode object that needs to converted
     * @return a valid SubType or Null (if the JsonNode does not represent the subtype)
     */
    static AtomicColumnType fromJsonNode(final JsonNode json) {
        if (json.isObject() && json.has("value")) {
            return null;
        }
        BaseType jsonBaseType = BaseType.fromJson(json, "key");

        if (jsonBaseType != null) {

            AtomicColumnType atomicColumnType = new AtomicColumnType(jsonBaseType);

            JsonNode minNode = json.get("min");
            if (minNode != null) {
                atomicColumnType.setMin(minNode.asLong());
            }

            JsonNode maxNode = json.get("max");
            if (maxNode != null) {
                if (maxNode.isNumber()) {
                    atomicColumnType.setMax(maxNode.asLong());
                } else if ("unlimited".equals(maxNode.asText())) {
                    atomicColumnType.setMax(Long.MAX_VALUE);
                }
            }
            return atomicColumnType;
        }

        return null;
    }

    @Override
    public Object valueFromJson(final JsonNode value) {
        if (isMultiValued()) {
            OvsdbSet<Object> result = new OvsdbSet<>();
            if (value.isArray()) {
                if (value.size() == 2) {
                    if (value.get(0).isTextual() && "set".equals(value.get(0).asText())) {
                        for (JsonNode node: value.get(1)) {
                            result.add(getBaseType().toValue(node));
                        }
                    } else {
                        result.add(getBaseType().toValue(value));
                    }
                }
            } else {
                result.add(getBaseType().toValue(value));
            }
            return result;
        } else {
            return getBaseType().toValue(value);
        }
    }
}
