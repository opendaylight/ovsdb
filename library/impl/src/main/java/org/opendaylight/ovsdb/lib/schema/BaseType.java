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
            final JsonNode nestedType = type.get("type");
            if (nestedType != null) {
                final BaseType ret = builderFor(nestedType.asText());
                if (ret != null) {
                    ret.fillConstraints(type);
                    return ret;
                }
            }
        }

        return null;
    }

    abstract void fillConstraints(JsonNode type);

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

    // Create a new instance for customization
    private static BaseType builderFor(final String type) {
        switch (type) {
            case "boolean":
                return new BooleanBaseType();
            case "integer":
                return new IntegerBaseType();
            case "real":
                return new RealBaseType();
            case "string":
                return new StringBaseType();
            case "uuid":
                return new UuidBaseType();
            default:
                LOG.debug("Unknown base type {}", type);
                return null;
        }
    }

}
