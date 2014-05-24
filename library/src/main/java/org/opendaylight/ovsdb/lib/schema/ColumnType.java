/*
 * Copyright (C) 2014 EBay Software Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Ashwin Raveendran
 */
package org.opendaylight.ovsdb.lib.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import org.opendaylight.ovsdb.lib.jsonrpc.JsonUtils;

import java.util.Set;


public abstract class ColumnType {
    BaseType baseType;
    long min = 1;
    long max = 1;

    public long getMin() {
        return min;
    }

    void setMin(long min) {
        this.min = min;
    }

    public long getMax() {
        return max;
    }

    void setMax(long max) {
        this.max = max;
    }

    private static ColumnType columns[] = new ColumnType[]{
            new AtomicColumnType(),
            new KeyValuedColumnType()
    };


    public ColumnType() {

    }

    public ColumnType(BaseType baseType) {
        this.baseType = baseType;
    }

    public BaseType getBaseType() {
        return baseType;
    }

    /**
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
            }
     * @param json
     * @return
     */
    public static ColumnType fromJson(JsonNode json) {
        for (ColumnType colType : columns) {
            ColumnType columnType = colType.fromJsonNode(json);
            if (null != columnType) {
                return columnType;
            }
        }
        //todo move to speicfic typed exception
        throw new RuntimeException(String.format("could not find the right column type %s",
                JsonUtils.prettyString(json)));
    }


    /**
     * Creates a ColumnType from the JsonNode if the implementation  knows how to, returns null otherwise
     *
     * @param json the JSONNode object that needs to converted
     * @return a valid SubType or Null (if the JsonNode does not represent
     * the subtype)
     */
    protected abstract ColumnType fromJsonNode(JsonNode json);

    public boolean isMultiValued() {
        //todo check if this is the right logic
        return this.min != this.max && this.min != 1;
    }

    public abstract Object valueFromJson(JsonNode value);

    public abstract void validate(Object value);

    public static class AtomicColumnType extends ColumnType {

        public AtomicColumnType() {
        }

        public AtomicColumnType(BaseType baseType1) {
            super(baseType1);
        }

        public AtomicColumnType fromJsonNode(JsonNode json) {
            if (json.isObject() && json.has("value")) {
                return null;
            }
            BaseType baseType = BaseType.fromJson(json, "key");

            if (baseType != null) {

                AtomicColumnType atomicColumnType = new AtomicColumnType(baseType);

                JsonNode node = null;
                if ((node = json.get("min")) != null) {
                    atomicColumnType.setMin(node.asLong());
                }

                if ((node = json.get("max")) != null) {
                    if (node.isLong()){
                        atomicColumnType.setMax(node.asLong());
                    } else if (node.isTextual() && "unlimited".equals(node.asText())) {
                        max = Long.MAX_VALUE;
                    }
                }
                return atomicColumnType;
            }

            return null;
        }

        @Override
        public Object valueFromJson(JsonNode value) {
            if (isMultiValued()) {
                Set<Object> result = Sets.newHashSet();
               if(value.isContainerNode()) {
                  for(JsonNode node: value) {
                     result.add(getBaseType().toValue(node));
                  }
               } else {
                   result.add(getBaseType().toValue(value));
               }
                return result;
            } else {
                return getBaseType().toValue(value);
            }
        }

        @Override
        public void validate(Object value) {
            this.baseType.validate(value);
        }

    }

    public static class KeyValuedColumnType extends ColumnType {

        BaseType valueType;

        public KeyValuedColumnType() {
        }

        public KeyValuedColumnType(BaseType baseType, BaseType valueType) {
            super(baseType);
        }

        public KeyValuedColumnType fromJsonNode(JsonNode json) {
            if (json.isValueNode() || !json.has("value")) {
                return null;
            }
            BaseType keyType = BaseType.fromJson(json, "key");
            BaseType valueType = BaseType.fromJson(json, "value");

            return new KeyValuedColumnType(keyType, valueType);
        }

        @Override
        public Object valueFromJson(JsonNode value) {
            throw new UnsupportedOperationException("needs to be implemented");
        }

        @Override
        public void validate(Object value) {
            throw new UnsupportedOperationException("not implemented yet");
        }
    }
}
