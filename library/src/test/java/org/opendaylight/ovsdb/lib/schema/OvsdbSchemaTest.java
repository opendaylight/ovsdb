/*
 * Copyright (C) 2014 EBay Software Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Ashwin Raveendran
 */
package org.opendaylight.ovsdb.lib.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.opendaylight.ovsdb.lib.notation.Version;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.io.IOException;
import java.io.InputStream;


public class OvsdbSchemaTest {

     /**
      * Test OVSDB schema construction from JSON text in
      * test_schema.json. Following tables are used: "Port", "Manager",
      * "Bridge", "Interface", "SSL", "Open_vSwitch", "Queue",
      * "NetFlow", "Mirror", "QoS", "Controller", "Flow_Table", "sFlow"
      * tables.
      */
    @Test
    public void testSchema() throws IOException {
        InputStream resourceAsStream = OvsdbSchemaTest.class.getResourceAsStream("test_schema.json");
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(resourceAsStream);
        System.out.println("jsonNode = " + jsonNode.get("id"));

        DatabaseSchema schema = DatabaseSchema.fromJson("some", jsonNode.get("result"));
        assertNotNull(schema);
        assertEquals(Version.fromString("6.12.0"), schema.getVersion());
    }
}
