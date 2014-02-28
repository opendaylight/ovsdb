package org.opendaylight.ovsdb.lib.jsonrpc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

/**
 * @author araveendrann
 */
public class JsonUtils {

    static ObjectMapper mapper = new ObjectMapper();

    static ObjectWriter prettyWriter = mapper.writerWithDefaultPrettyPrinter();

    public static String prettyString(Object jsonNode){
        try {
            return prettyWriter.writeValueAsString(jsonNode);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
