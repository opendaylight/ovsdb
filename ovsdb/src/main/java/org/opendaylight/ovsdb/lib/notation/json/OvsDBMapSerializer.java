package org.opendaylight.ovsdb.lib.notation.json;

import java.io.IOException;
import java.util.Map;
import org.opendaylight.ovsdb.lib.notation.OvsDBMap;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class OvsDBMapSerializer extends JsonSerializer<OvsDBMap<?,?>> {
    @Override
    public void serialize(OvsDBMap<?,?> map, JsonGenerator generator,
        SerializerProvider provider) throws IOException,
            JsonProcessingException {
        generator.writeStartArray();
        generator.writeString("map");
        generator.writeStartArray();
        Map<?,?> javaMap = map.delegate();
        for (Object t : javaMap.keySet()) {
            generator.writeStartArray();
            generator.writeObject(t);
            generator.writeObject(javaMap.get(t));
            generator.writeEndArray();
        }
        generator.writeEndArray();
        generator.writeEndArray();
    }
}