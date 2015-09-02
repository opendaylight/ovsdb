/*
 *  Copyright (C) 2014 Red Hat, Inc. and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 *  Authors : Sam Hague
 */
package org.opendaylight.ovsdb.plugin.shell;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.Set;
import org.junit.Test;
import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.notation.Version;
import org.opendaylight.ovsdb.lib.schema.ColumnSchema;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.plugin.api.OvsVswitchdSchemaConstants;
import org.opendaylight.ovsdb.plugin.impl.InventoryServiceImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;

public class PrintCacheTest {
    private static final String NODESTRING = "OVS|10.10.10.10:65342";
    private static final String BRIDGES = "bridges";
    private static final String OVS = "OVS";
    private static final String BRIDGE = "bridge1";
    private static final String OPENVSWITCH = "Open_vSwitch";
    private static final String CACHE =
            "Database Open_vSwitch" + System.getProperty("line.separator") +
            "\tTable Open_vSwitch" + System.getProperty("line.separator") +
            "\t\t1==bridges : bridge1 " + System.getProperty("line.separator") +
            "-----------------------------------------------------------" + System.getProperty("line.separator");

    @Test
    public void testDoExecute () throws Exception {
        // Read in schema and create the DatabaseSchema
        InputStream resourceAsStream = PrintCacheTest.class.getResourceAsStream("test_schema.json");
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(resourceAsStream);
        DatabaseSchema schema = DatabaseSchema.fromJson("some", jsonNode.get("result"));
        assertNotNull(schema);
        assertEquals(Version.fromString("6.12.0"), schema.getVersion());

        // Add params to PrintCache
        PrintCache printCacheTest = new PrintCache();
        Field cNField = printCacheTest.getClass().getDeclaredField("nodeName");
        cNField.setAccessible(true);
        cNField.set(printCacheTest, NODESTRING);
        InventoryServiceImpl inventoryService = new InventoryServiceImpl();
        inventoryService.init();
        printCacheTest.setOvsdbInventory(inventoryService);

        // Test that an empty cache prints nothing
        // Capture the output from PrintCache and compare it to what is expected
        PrintStream originalBaos = System.out;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        printCacheTest.doExecute();
        assertEquals("PrintCache output does not match expected output:", "", baos.toString());

        // Add some data to the bridges row in the Open_vSwitch table
        GenericTableSchema ovsTable = schema.table(OPENVSWITCH, GenericTableSchema.class);
        ColumnSchema<GenericTableSchema, Set<UUID>> bridges = ovsTable.multiValuedColumn(BRIDGES, UUID.class);
        Column column = new Column(bridges, new UUID(BRIDGE).toString());
        Row row = new Row(ovsTable);
        row.addColumn(BRIDGES, column);

        NodeId nodeId = new NodeId(NODESTRING);
        NodeKey nodeKey = new NodeKey(nodeId);
        Node node = new NodeBuilder()
                .setId(nodeId)
                .setKey(nodeKey)
                .build();
        inventoryService.updateRow(node, OvsVswitchdSchemaConstants.DATABASE_NAME, OPENVSWITCH, new UUID("1").toString(), row);

        // Test that a populated cache is printed correctly
        // Capture the output from PrintCache and compare it to what is expected
        printCacheTest.doExecute();
        System.setOut(originalBaos);
        assertEquals("PrintCache output does not match expected output:", CACHE, baos.toString());
    }
}
