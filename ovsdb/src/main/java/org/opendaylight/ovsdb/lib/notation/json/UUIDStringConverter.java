package org.opendaylight.ovsdb.lib.notation.json;

import com.fasterxml.jackson.databind.util.StdConverter;

import org.opendaylight.ovsdb.lib.notation.UUID;

public class UUIDStringConverter extends StdConverter<String, UUID> {

    @Override
    public UUID convert(String value) {
        return new UUID(value);
    }

}
