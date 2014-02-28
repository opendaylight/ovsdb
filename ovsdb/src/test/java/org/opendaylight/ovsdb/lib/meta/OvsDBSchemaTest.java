package org.opendaylight.ovsdb.lib.meta;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static junit.framework.Assert.*;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author araveendrann
 */
public class OvsDBSchemaTest {

    @Test
    public void testSchema() throws IOException {
        InputStream resourceAsStream = OvsDBSchemaTest.class.getResourceAsStream("test_schema.json");
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(resourceAsStream);
        System.out.println("jsonNode = " + jsonNode.get("id"));

        DatabaseSchema schema = DatabaseSchema.fromJson(jsonNode.get("result"));
        assertNotNull(schema);

    }
}
