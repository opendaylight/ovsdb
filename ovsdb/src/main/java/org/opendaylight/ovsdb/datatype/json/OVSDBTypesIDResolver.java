package org.opendaylight.ovsdb.datatype.json;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.opendaylight.ovsdb.datatype.OvsDBSet;
import org.opendaylight.ovsdb.datatype.UUID;

public  class OVSDBTypesIDResolver implements TypeIdResolver {

        private JavaType baseType;

        @Override
        public void init(JavaType bt) {
            this.baseType = bt;
        }

        @Override
        public String idFromValue(Object value) {
            throw new UnsupportedOperationException("not yet done");
        }

        @Override
        public String idFromValueAndType(Object value, Class<?> suggestedType) {
            throw new UnsupportedOperationException("not yet done");
        }

        @Override
        public String idFromBaseType() {
            throw new UnsupportedOperationException("not yet done");
        }

        @Override
        public JavaType typeFromId(String id) {
            if ("set".equals(id)) {
                return TypeFactory.defaultInstance().constructCollectionType(OvsDBSet.class, Object.class);
            } else if ("uuid".equals(id)) {
                return TypeFactory.defaultInstance().constructType(UUID.class);
            }
            return null;
        }

        @Override
        public JsonTypeInfo.Id getMechanism() {
            throw new UnsupportedOperationException("not yet done");
        }
    }