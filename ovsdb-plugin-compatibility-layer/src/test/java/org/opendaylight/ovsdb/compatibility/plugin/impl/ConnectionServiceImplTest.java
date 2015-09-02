/*
 *  Copyright (C) 2015 Red Hat, Inc. and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 *  Authors : Sam Hague
 */
package org.opendaylight.ovsdb.compatibility.plugin.impl;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.ovsdb.plugin.api.Connection;

public class ConnectionServiceImplTest {
    private static final String OVS = "OVS";
    private static final String IDENTIFIER = "192.168.120.31:45001";
    private static final String OVS_IDENTIFIER = OVS + "|" + IDENTIFIER;
    private static final String BAD_IDENTIFIER = "BAD" + "|" + IDENTIFIER;
    private static org.opendaylight.ovsdb.plugin.impl.ConnectionServiceImpl pluginConnectionService;
    private static ConnectionServiceImpl connectionService;

    @BeforeClass
    public static void setUp () {
        Node.NodeIDType.registerIDType(OVS, String.class);
        NodeConnector.NodeConnectorIDType.registerIDType(OVS, String.class, OVS);
        pluginConnectionService = new org.opendaylight.ovsdb.plugin.impl.ConnectionServiceImpl();
        Connection connection = new Connection(IDENTIFIER, null);
        pluginConnectionService.putOvsdbConnection(IDENTIFIER, connection);

        connectionService = new ConnectionServiceImpl();
        connectionService.setOvsdbConnectionService(pluginConnectionService);
    }

    @Test
    public void testGetNode () {
        Node node = connectionService.getNode(IDENTIFIER);
        assertNotNull("Node " + IDENTIFIER + " is null", node);

        node = connectionService.getNode(OVS_IDENTIFIER);
        assertNotNull("Node " + OVS_IDENTIFIER + " is null", node);

        try {
            node = connectionService.getNode(BAD_IDENTIFIER);
            fail("Expected a NullPointerException to be thrown");
        } catch (NullPointerException e) {
            assertSame(NullPointerException.class, e.getClass());
        }
    }

    @Test
    public void testGetConnection () {
        Node node = connectionService.getNode(IDENTIFIER);
        assertNotNull("Node " + IDENTIFIER + " is null", node);

        Connection connection = connectionService.getConnection(node);
        assertNotNull("Connection " + IDENTIFIER + " is null", connection);

        try {
            connection = connectionService.getConnection(null);
            fail("Expected a NullPointerException to be thrown");
        } catch (NullPointerException e) {
            assertSame(NullPointerException.class, e.getClass());
        }

        try {
            node = new Node("OVS", BAD_IDENTIFIER);
        } catch (ConstructionException e) {
            fail("Exception should not have occurred" + e);
        }
        connection = connectionService.getConnection(node);
        assertNull("Connection " + BAD_IDENTIFIER + " is not null", connection);
    }
}
