/*
 * Copyright © 2014, 2017 EBay Software Foundation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.opendaylight.ovsdb.lib.error.TyperException;
import org.slf4j.Logger;

public abstract class BaseType<E extends BaseType<E>> {
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(BaseType.class);

    BaseType() {
        // Prevent external instantiation
    }

    public static BaseType fromJson(final JsonNode json, final String keyorval) {
        if (json.isValueNode()) {
            return singletonFor(json.asText().trim());
        }

        final JsonNode type = json.get(keyorval);
        if (type == null) {
            throw new TyperException("Not a type");
        }
        if (type.isTextual()) {
            //json like  "string"
            return singletonFor(type.asText());
        }
        if (type.isObject()) {
            //json like  {"type" : "string", "enum": ["set", ["access", "native-tagged"]]}" for key or value
            final JsonNode typeName = type.get("type");
            if (typeName != null) {
                final BaseTypeFactory<?> factory = factoryFor(typeName.asText());
                if (factory != null) {
                    return factory.create(type);
                }
            }
        }

        return null;
    }

    public abstract Object toValue(JsonNode value);

    public abstract void validate(Object value);

    // Find a simple singleton instance
    private static BaseType singletonFor(final String type) {
        switch (type) {
            case "boolean":
                return BooleanBaseType.SINGLETON;
            case "integer":
                return IntegerBaseType.SINGLETON;
            case "real":
                return RealBaseType.SINGLETON;
            case "string":
                return StringBaseType.SINGLETON;
            case "uuid":
                return UuidBaseType.SINGLETON;
            default:
                LOG.debug("Unknown base type {}", type);
                return null;
        }
    }

    // Find a factory for custom instantiation
    private static BaseTypeFactory<?> factoryFor(final String type) {
        switch (type) {
            case "boolean":
                return BooleanBaseType.FACTORY;
            case "integer":
                return IntegerBaseType.FACTORY;
            case "real":
                return RealBaseType.FACTORY;
            case "string":
                return StringBaseType.FACTORY;
            case "uuid":
                return UuidBaseType.FACTORY;
            default:
                LOG.debug("Unknown base type {}", type);
                return null;
        }
    }
}
