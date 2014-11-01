/*
 *  Copyright (C) 2014 Red Hat, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 *  Authors : Sam Hague
 */
package org.opendaylight.ovsdb.plugin.shell;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import com.google.common.collect.Sets;
import java.util.HashMap;
import java.util.Map;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.plugin.api.OvsVswitchdSchemaConstants;
import org.opendaylight.ovsdb.plugin.impl.NodeFactory;
import org.opendaylight.ovsdb.schema.openvswitch.OpenVSwitch;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.plugin.impl.InventoryServiceImpl;
import org.opendaylight.ovsdb.plugin.internal.IPAddressProperty;
import org.opendaylight.ovsdb.plugin.internal.L4PortProperty;

/*
 * This test needs some work to populate the db so that the printCache
 * is properly exercised.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(Node.class)
public class PrintCacheTest {
    private static final String NODESTRING = "OVS:10.10.10.10:65342";
    private NodeFactory nodeFactory;

    @Test
    public void testDoExecute () throws Exception {

        Node.NodeIDType.registerIDType("OVS", String.class);
        NodeConnector.NodeConnectorIDType.registerIDType("OVS", String.class, "OVS");
        Node node = new Node("OVS", "1");
        PowerMockito.mockStatic(Node.class);
        when(Node.fromString(NODESTRING)).thenReturn(node);

        /* Trying to find a way to populate the db */
        InetAddress address = InetAddress.getByName("10.10.10.10");
        int port = 65342;
        IPAddressProperty addressProp = new IPAddressProperty(address);
        L4PortProperty l4Port = new L4PortProperty(port);
        Set<Property> props = new HashSet<Property>();
        props.add(addressProp);
        props.add(l4Port);
        InventoryServiceImpl inventoryService = new InventoryServiceImpl();
        inventoryService.init();

        ConcurrentMap<String, Row> ovsMap = new ConcurrentHashMap<>();
        OpenVSwitch ovsTable = PowerMockito.mock(OpenVSwitch.class);
        Map localIp = new HashMap();
        localIp.put("local_ip", "10.10.10.10");
        ovsTable.setOtherConfig(localIp);

        TableUpdates tableUpdates = new TableUpdates();
        ovsTable.setBridges(Sets.newHashSet(new UUID("testbridge")));
        inventoryService.processTableUpdates(node, OvsVswitchdSchemaConstants.DATABASE_NAME, tableUpdates);

        /* Add the printCache params */
        PrintCache printCacheTest = new PrintCache();
        Field cNField = printCacheTest.getClass().getDeclaredField("nodeName");
        cNField.setAccessible(true);
        cNField.set(printCacheTest, NODESTRING);
        printCacheTest.setOvsdbInventory(inventoryService);

        /* Capture the printCache output and verify it */
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        printCacheTest.doExecute();
        //assertEquals("status\n", baos.toString());
        assertEquals("", baos.toString());
    }
}
