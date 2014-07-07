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

import java.util.Set;

import org.opendaylight.ovsdb.lib.error.TyperException;
import org.opendaylight.ovsdb.lib.jsonrpc.JsonUtils;
import org.opendaylight.ovsdb.lib.notation.OvsDBMap;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;


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
        throw new TyperException(String.format("could not find the right column type %s",
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

    /*
     * Per RFC 7047, Section 3.2 <type> :
     * If "min" or "max" is not specified, each defaults to 1.  If "max" is specified as "unlimited", then there is no specified maximum
     * number of elements, although the implementation will enforce some limit.  After considering defaults, "min" must be exactly 0 or
     * exactly 1, "max" must be at least 1, and "max" must be greater than or equal to "min".
     *
     * If "min" and "max" are both 1 and "value" is not specified, the
     * type is the scalar type specified by "key".
     */
    public boolean isMultiValued() {
        return this.min != this.max;
    }

    public abstract Object valueFromJson(JsonNode value);

    public abstract void validate(Object value);

    @Override
    public String toString() {
        return "ColumnType{" +
                "baseType=" + baseType +
                ", min=" + min +
                ", max=" + max +
                '}';
    }

    public static class AtomicColumnType extends ColumnType {
        static final org.slf4j.Logger logger = LoggerFactory.getLogger(AtomicColumnType.class);
        public AtomicColumnType() {
        }

        public AtomicColumnType(BaseType baseType1) {
            super(baseType1);
        }

        @Override
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
                    if (node.isNumber()){
                        atomicColumnType.setMax(node.asLong());
                    } else if ("unlimited".equals(node.asText())) {
                        atomicColumnType.setMax(Long.MAX_VALUE);
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
                if(value.isArray()) {
                    if (value.size() == 2) {
                        if (value.get(0).isTextual() && "set".equals(value.get(0).asText())) {
                            for(JsonNode node: value.get(1)) {
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

        @Override
        public void validate(Object value) {
            this.baseType.validate(value);
        }

    }

    public static class KeyValuedColumnType extends ColumnType {
        BaseType keyType;

        public BaseType getKeyType() {
            return keyType;
        }

        public KeyValuedColumnType() {
        }

        public KeyValuedColumnType(BaseType keyType, BaseType valueType) {
            super(valueType);
            this.keyType = keyType;
        }

        @Override
        public KeyValuedColumnType fromJsonNode(JsonNode json) {
            if (json.isValueNode() || !json.has("value")) {
                return null;
            }
            BaseType keyType = BaseType.fromJson(json, "key");
            BaseType valueType = BaseType.fromJson(json, "value");

            KeyValuedColumnType keyValueColumnType = new KeyValuedColumnType(keyType, valueType);
            JsonNode node = null;
            if ((node = json.get("min")) != null) {
                keyValueColumnType.setMin(node.asLong());
            }

            if ((node = json.get("max")) != null) {
                if (node.isLong()){
                    keyValueColumnType.setMax(node.asLong());
                } else if (node.isTextual() && "unlimited".equals(node.asText())) {
                    keyValueColumnType.setMax(Long.MAX_VALUE);
                }
            }

            return keyValueColumnType;
        }

        @Override
        public Object valueFromJson(JsonNode node) {
            if (node.isArray()) {
                if (node.size() == 2) {
                    if (node.get(0).isTextual() && "map".equals(node.get(0).asText())) {
                        OvsDBMap<Object, Object> map = new OvsDBMap<Object, Object>();
                        for (JsonNode pairNode : node.get(1)) {
                            if (pairNode.isArray() && node.size() == 2) {
                                Object key = getKeyType().toValue(pairNode.get(0));
                                Object value = getBaseType().toValue(pairNode.get(1));
                                map.put(key, value);
                            }
                        }
                        return map;
                    } else if (node.size() == 0) {
                        return null;
                    }
                }
            }
            return null;
        }

        @Override
        public void validate(Object value) {
            this.baseType.validate(value);
        }
    }
}
