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
import static org.junit.Assert.assertNotNull;
import java.io.IOException;
import java.io.InputStream;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class OvsDBSchemaTest {

    @Test
    public void testSchema() throws IOException {
        InputStream resourceAsStream = OvsDBSchemaTest.class.getResourceAsStream("test_schema.json");
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(resourceAsStream);
        System.out.println("jsonNode = " + jsonNode.get("id"));

        DatabaseSchema schema = DatabaseSchema.fromJson("some", jsonNode.get("result"));
        assertNotNull(schema);

    }
}
