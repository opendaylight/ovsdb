package org.opendaylight.ovsdb.lib.notation.json;

import java.io.IOException;

import org.opendaylight.ovsdb.lib.notation.Condition;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class ConditionSerializer extends JsonSerializer<Condition> {
    @Override
    public void serialize(Condition condition, JsonGenerator generator,
        SerializerProvider provider) throws IOException,
            JsonProcessingException {
        generator.writeStartArray();
        generator.writeString(condition.getColumn());
        generator.writeString(condition.getFunction().toString());
        generator.writeObject(condition.getValue());
        generator.writeEndArray();
    }
}