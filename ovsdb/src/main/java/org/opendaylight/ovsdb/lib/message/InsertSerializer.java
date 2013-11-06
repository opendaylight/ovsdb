package org.opendaylight.ovsdb.lib.message;

import java.io.IOException;

import org.opendaylight.ovsdb.lib.message.operations.InsertOperation;
import org.opendaylight.ovsdb.lib.notation.Condition;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class InsertSerializer extends JsonSerializer<InsertOperation> {
    @Override
    public void serialize(InsertOperation condition, JsonGenerator generator,
        SerializerProvider provider) throws IOException,
            JsonProcessingException {
        generator.writeStartArray();

        generator.writeEndArray();
    }
}