/*
 *  Copyright (C) 2015 Red Hat, Inc. and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 *  Authors : Sam Hague
 */
package org.opendaylight.ovsdb.plugin.impl;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.ovsdb.plugin.api.Connection;

public class ConnectionServiceImplTest {
    private static final String OVS = "OVS";
    private static final String IDENTIFIER = "192.168.120.31:45001";
    private static final String OVS_IDENTIFIER = OVS + "|" + IDENTIFIER;
    private static final String BAD_IDENTIFIER = "BAD" + "|" + IDENTIFIER;
    private static ConnectionServiceImpl connectionService;

    @BeforeClass
    public static void setUp () {
        connectionService = new ConnectionServiceImpl();
        Connection connection = new Connection(IDENTIFIER, null);
        connectionService.putOvsdbConnection(IDENTIFIER, connection);
    }

    @Test
    public void testGetNode () {
        Node node = connectionService.getNode(IDENTIFIER);
        assertNotNull("Node " + IDENTIFIER + " is null", node);

        node = connectionService.getNode(OVS_IDENTIFIER);
        assertNotNull("Node " + OVS_IDENTIFIER + " is null", node);

        node = connectionService.getNode(IDENTIFIER + "extra");
        assertNull("Node " + BAD_IDENTIFIER + " is not null", node);
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
    }
}
