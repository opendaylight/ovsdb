package org.opendaylight.ovsdb.lib.notation.json;

import java.io.IOException;

import org.opendaylight.ovsdb.lib.notation.Mutation;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class MutationSerializer extends JsonSerializer<Mutation> {
    @Override
    public void serialize(Mutation condition, JsonGenerator generator,
        SerializerProvider provider) throws IOException,
            JsonProcessingException {
        generator.writeStartArray();
        generator.writeString(condition.getColumn());
        generator.writeString(condition.getMutator().toString());
        generator.writeObject(condition.getValue());
        generator.writeEndArray();
    }
}