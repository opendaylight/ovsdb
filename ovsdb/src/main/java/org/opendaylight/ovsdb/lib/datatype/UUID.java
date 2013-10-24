package org.opendaylight.ovsdb.lib.datatype;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;

import org.opendaylight.ovsdb.lib.datatype.json.OVSDBTypesIDResolver;
import org.opendaylight.ovsdb.lib.datatype.json.UUIDStringConverter;

@JsonTypeIdResolver(OVSDBTypesIDResolver.class)
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, include = JsonTypeInfo.As.WRAPPER_ARRAY)
@JsonDeserialize(contentConverter = UUIDStringConverter.class)
public class UUID {
    String val;

    public UUID(String value) {
        this.val = value;
    }

    public String toString() {
        return val;
    }
}
