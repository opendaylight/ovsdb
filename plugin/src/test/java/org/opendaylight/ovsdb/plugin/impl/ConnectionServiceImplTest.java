/*
 *  Copyright (C) 2015 Red Hat, Inc.
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

import org.junit.Test;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.ovsdb.plugin.api.Connection;

public class ConnectionServiceImplTest {
    private static final String OVS = "OVS";
    private static final String IDENTIFIER = "192.168.120.31:45001";
    private static final String OVS_IDENTIFIER = OVS + "|" + IDENTIFIER;
    private static final String BAD_IDENTIFIER = "BAD" + "|" + IDENTIFIER;

    @Test
    public void testGetNode () {
        Node.NodeIDType.registerIDType(OVS, String.class);
        NodeConnector.NodeConnectorIDType.registerIDType(OVS, String.class, OVS);
        ConnectionServiceImpl connectionService = new ConnectionServiceImpl();
        Connection connection = new Connection(IDENTIFIER, null);
        connectionService.putOvsdbConnection(IDENTIFIER, connection);

        Node node = connectionService.getNode(IDENTIFIER);
        assertNotNull("Node " + IDENTIFIER + " is null", node);

        node = connectionService.getNode(OVS_IDENTIFIER);
        assertNotNull("Node " + OVS_IDENTIFIER + " is null", node);

        node = connectionService.getNode(IDENTIFIER + "extra");
        assertNull("Node " + BAD_IDENTIFIER + " is not null", node);
    }
}
