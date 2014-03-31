package org.opendaylight.ovsdb.lib.meta;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;

import java.util.Set;

/**
 * @author araveendrann
 */
public abstract class BaseType<E extends BaseType<E>> {

    private static BaseType[] types = new BaseType[]{
            new StringBaseType(),
            new IntegerBaseType(),
            new RealBaseType(),
            new BooleanBaseType(),
            new UuidBaseType(),
    };

    public static BaseType fromJson(JsonNode json, String keyorval) {
        BaseType baseType = null;
        if (json.isValueNode()) {
            for (BaseType baseTypeFactory : types) {
                String type = json.asText().trim();
                baseType = baseTypeFactory.fromString(type);
                if (baseType != null) {
                    break;
                }
            }
        } else {
            if (!json.has(keyorval)) {
                throw new RuntimeException("Not a type");
            }

            for (BaseType baseTypeFactory : types) {
                baseType = baseTypeFactory.fromJsonNode(json.get(keyorval), keyorval);
                if (baseType != null) {
                    break;
                }
            }
        }
        return baseType;
    }

    protected abstract E fromString(String type);

    protected abstract void getConstraints(E baseType, JsonNode type);

    protected E fromJsonNode(JsonNode type, String keyorval) {

        E baseType = null;

        //json like  "string"
        if (type.isTextual()) {
            baseType = fromString(type.asText());
            if (baseType != null) {
                return baseType;
            }
        }

        //json like  {"type" : "string", "enum": ["set", ["access", "native-tagged"]]}" for key or value
        if (type.isObject() && type.has("type")) {
            baseType = fromString(type.get("type").asText());
            if (baseType != null) {
                getConstraints(baseType, type);
            }
        }

        return baseType;
    }

    public static class IntegerBaseType extends BaseType<IntegerBaseType> {
        long min = Long.MIN_VALUE;
        long max = Long.MAX_VALUE;
        Set<Integer> enums;

        public IntegerBaseType fromString(String typeString) {
            return "integer".equals(typeString) ? new IntegerBaseType() : null;
        }

        @Override
        protected void getConstraints(IntegerBaseType baseType, JsonNode type) {

            JsonNode node = null;

            if ((node = type.get("maxInteger")) != null) {
                baseType.setMax(node.asLong());
            }

            if ((node = type.get("minInteger")) != null) {
                baseType.setMin(node.asLong());
            }

            populateEnum(type);
        }

        private void populateEnum(JsonNode node) {
            if (node.has("enum")) {
                Set<Long> s = Sets.newHashSet();
                JsonNode anEnum = node.get("enum").get(1);
                for (JsonNode n : anEnum) {
                    s.add(n.asLong());
                }
            }
        }


        public long getMin() {
            return min;
        }

        public void setMin(long min) {
            this.min = min;
        }

        public long getMax() {
            return max;
        }

        public void setMax(long max) {
            this.max = max;
        }

        public Set<Integer> getEnums() {
            return enums;
        }

        public void setEnums(Set<Integer> enums) {
            this.enums = enums;
        }
    }

    public static class RealBaseType extends BaseType<RealBaseType> {
        double min = Double.MIN_VALUE;
        double max = Double.MAX_VALUE;
        Set<Double> enums;

        public RealBaseType fromString(String typeString) {
            return "real".equals(typeString) ? new RealBaseType() : null;
        }

        @Override
        protected void getConstraints(RealBaseType baseType, JsonNode type) {

            JsonNode node = null;

            if ((node = type.get("maxReal")) != null) {
                baseType.setMax(node.asLong());
            }

            if ((node = type.get("minReal")) != null) {
                baseType.setMin(node.asLong());
            }

            populateEnum(type);
        }

        private void populateEnum(JsonNode node) {
            if (node.has("enum")) {
                Set<Double> s = Sets.newHashSet();
                JsonNode anEnum = node.get("enum").get(1);
                for (JsonNode n : anEnum) {
                    s.add(n.asDouble());
                }
            }
        }

        public double getMin() {
            return min;
        }

        public void setMin(double min) {
            this.min = min;
        }

        public double getMax() {
            return max;
        }

        public void setMax(double max) {
            this.max = max;
        }

        public Set<Double> getEnums() {
            return enums;
        }

        public void setEnums(Set<Double> enums) {
            this.enums = enums;
        }


    }


    public static class BooleanBaseType extends BaseType {

        public BooleanBaseType fromString(String typeString) {
            return "boolean".equals(typeString) ? new BooleanBaseType() : null;
        }

        @Override
        protected void getConstraints(BaseType baseType, JsonNode node) {
            //no op
        }
    }

    public static class StringBaseType extends BaseType<StringBaseType> {
        int minLength = Integer.MIN_VALUE;
        int maxLength = Integer.MAX_VALUE;
        Set<String> enums;

        public StringBaseType fromString(String typeString) {
            return "string".equals(typeString) ? new StringBaseType() : null;
        }

        @Override
        protected void getConstraints(StringBaseType baseType, JsonNode type) {

            JsonNode node = null;

            if ((node = type.get("maxLength")) != null) {
                baseType.setMaxLength(node.asInt());
            }

            if ((node = type.get("minLength")) != null) {
                baseType.setMinLength(node.asInt());
            }

            populateEnum(baseType, type);
        }

        private void populateEnum(StringBaseType baseType, JsonNode node) {
            if (node.has("enum")) {
                Set<String> s = Sets.newHashSet();
                JsonNode anEnum = node.get("enum").get(1);
                for (JsonNode n : anEnum) {
                    s.add(n.asText());
                }
                baseType.setEnums(s);
            }
        }

        public int getMinLength() {
            return minLength;
        }

        public void setMinLength(int minLength) {
            this.minLength = minLength;
        }

        public int getMaxLength() {
            return maxLength;
        }

        public void setMaxLength(int maxLength) {
            this.maxLength = maxLength;
        }

        public Set<String> getEnums() {
            return enums;
        }

        public void setEnums(Set<String> enums) {
            this.enums = enums;
        }
    }


    public static class UuidBaseType extends BaseType<UuidBaseType> {
        public static enum RefType {strong, weak}

        String refTable;
        RefType refType;


        public UuidBaseType fromString(String typeString) {
            return "uuid".equals(typeString) ? new UuidBaseType() : null;
        }

        @Override
        protected void getConstraints(UuidBaseType baseType, JsonNode node) {

            JsonNode refTable = node.get("refTable");
            baseType.setRefTable(refTable != null ? refTable.asText() : null);

            JsonNode refTypeJson = node.get("refType");
            baseType.setRefType(refTypeJson != null ? RefType.valueOf(refTypeJson.asText()) : RefType.strong);

        }

        public String getRefTable() {
            return refTable;
        }

        public void setRefTable(String refTable) {
            this.refTable = refTable;
        }

        public RefType getRefType() {
            return refType;
        }

        public void setRefType(RefType refType) {
            this.refType = refType;
        }
    }
}
