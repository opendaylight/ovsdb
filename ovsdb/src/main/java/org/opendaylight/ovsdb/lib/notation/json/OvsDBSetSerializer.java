package org.opendaylight.ovsdb.lib.notation.json;

import java.io.IOException;
import java.util.Set;

import org.opendaylight.ovsdb.lib.notation.Condition;
import org.opendaylight.ovsdb.lib.notation.OvsDBSet;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class OvsDBSetSerializer extends JsonSerializer<OvsDBSet<?>> {
    @Override
    public void serialize(OvsDBSet<?> set, JsonGenerator generator,
        SerializerProvider provider) throws IOException,
            JsonProcessingException {
        generator.writeStartArray();
        generator.writeString("set");
        generator.writeStartArray();
        Set<?> javaSet = set.delegate();
        for (Object t : javaSet) {
            generator.writeObject(t);
        }
        generator.writeEndArray();
        generator.writeEndArray();
    }
}