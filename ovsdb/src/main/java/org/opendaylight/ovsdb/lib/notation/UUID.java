package org.opendaylight.ovsdb.lib.notation;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;

import org.opendaylight.ovsdb.lib.notation.json.OVSDBTypesIDResolver;
import org.opendaylight.ovsdb.lib.notation.json.UUIDSerializer;
import org.opendaylight.ovsdb.lib.notation.json.UUIDStringConverter;

@JsonTypeIdResolver(OVSDBTypesIDResolver.class)
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, include = JsonTypeInfo.As.WRAPPER_ARRAY)
@JsonDeserialize(contentConverter = UUIDStringConverter.class)
@JsonSerialize(using = UUIDSerializer.class)
/*
 * Handles both uuid and named-uuid.
 */
public class UUID {
    String val;

    public UUID(String value) {
        this.val = value;
    }

    public String toString() {
        return val;
    }
}
