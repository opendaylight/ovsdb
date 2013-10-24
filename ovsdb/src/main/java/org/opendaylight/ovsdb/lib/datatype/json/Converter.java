package org.opendaylight.ovsdb.lib.datatype.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.util.StdConverter;

import org.opendaylight.ovsdb.lib.datatype.OvsDBMap;
import org.opendaylight.ovsdb.lib.datatype.OvsDBSet;
import org.opendaylight.ovsdb.lib.datatype.UUID;

public class Converter {

    static AtomDeser atomDeser = new AtomDeser();
    static MapDeser mapDeser = new MapDeser();
    static SetDeser setDeser = new SetDeser();

    public static class MapConverter extends StdConverter<JsonNode, OvsDBMap<Object, Object>> {
        @Override
        public OvsDBMap<Object, Object> convert(JsonNode value) {
            return mapDeser.deserialize(value);
        }
    }

    public static class SetConverter extends StdConverter<JsonNode, OvsDBSet<Object>> {
        @Override
        public OvsDBSet<Object> convert(JsonNode value) {
            return setDeser.deserialize(value);
        }
    }

    static class MapDeser {
        public OvsDBMap<Object, Object> deserialize(JsonNode node) {
            if (node.isArray()) {
                if (node.size() == 2) {
                    if (node.get(0).isTextual() && "map".equals(node.get(0).asText())) {
                        OvsDBMap<Object, Object> map = new OvsDBMap<Object, Object>();
                        for (JsonNode pairNode : node.get(1)) {
                            if (pairNode.isArray() && node.size() == 2) {
                                Object key = atomDeser.deserialize(pairNode.get(0));
                                Object value = atomDeser.deserialize(pairNode.get(1));
                                map.put(key, value);
                            }
                        }
                        return map.isEmpty() ? null : map;
                    } else if (node.size() == 0) {
                        return null;
                    }
                }
            }
            throw new RuntimeException("not a map type");
        }
    }

    static class SetDeser {
        public OvsDBSet<Object> deserialize(JsonNode node) {
            OvsDBSet<Object> set = new OvsDBSet<Object>();
            if (node.isArray()) {
                if (node.size() == 2) {
                    if (node.get(0).isTextual() && "set".equals(node.get(0).asText())) {
                        for (JsonNode atomNode : node.get(1)) {
                            set.add(atomDeser.deserialize(atomNode));
                        }
                        return set;
                    }
                } else if (node.size() == 0) {
                    return null;
                }
            }
            //treat the whole thing as a single Atom
            Object atom = atomDeser.deserialize(node);
            if (null != atom) {
                set.add(atom);
            }
            return set;
        }
    }


    static class AtomDeser {

        public Object deserialize(JsonNode node) {
            if (!node.isArray()) {
                switch (node.getNodeType()) {
                    case BOOLEAN:
                        return node.asBoolean();
                    case NUMBER:
                        if (node.isFloatingPointNumber()) {
                            return node.decimalValue();
                        } else {
                            return node.bigIntegerValue();
                        }
                    case STRING:
                        return node.asText();
                }
            }

            if (node.isArray() && node.get(0).isTextual()) {
                if ("uuid".equals(node.get(0).asText())) {
                    return new UUID(node.get(1).asText());
                }
            }

            throw new RuntimeException("not an atom node");
        }
    }
}
