/*
 *  Copyright (C) 2015 Red Hat, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 *  Authors : Sam Hague
 */
package org.opendaylight.ovsdb.northbound;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.northbound.commons.exception.ResourceNotFoundException;
import org.opendaylight.controller.northbound.commons.exception.ServiceUnavailableException;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.ovsdb.plugin.api.Connection;
import org.opendaylight.ovsdb.plugin.api.OvsdbConnectionService;
import org.opendaylight.ovsdb.plugin.impl.ConnectionServiceImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ServiceHelper.class)
public class NodeResourceTest {
    private static final String OVS = "OVS";
    private static final String IDENTIFIER = "192.168.120.31:45001";
    private static final String OVS_IDENTIFIER = OVS + "|" + IDENTIFIER;
    private static final String BAD_IDENTIFIER = "BAD" + "|" + IDENTIFIER;

    @Test
    public void testGetOvsdbNode () {
        ConnectionServiceImpl connectionService = new ConnectionServiceImpl();
        Connection connection = new Connection(IDENTIFIER, null);
        connectionService.putOvsdbConnection(IDENTIFIER, connection);

        PowerMockito.mockStatic(ServiceHelper.class);
        when(ServiceHelper.getGlobalInstance(eq(OvsdbConnectionService.class), anyObject()))
                .thenReturn(null)
                .thenReturn(connectionService)
                .thenReturn(connectionService);

        Node node = null;
        try {
            node = NodeResource.getOvsdbNode(IDENTIFIER, this);
            fail("Expected an ServiceUnavailableException to be thrown");
        } catch (ServiceUnavailableException e) {
            assertSame(ServiceUnavailableException.class, e.getClass());
        }

        try {
            node = NodeResource.getOvsdbNode(BAD_IDENTIFIER, this);
            fail("Expected an ResourceNotFoundException to be thrown");
        } catch (ResourceNotFoundException e) {
            assertSame(ResourceNotFoundException.class, e.getClass());
        }

        node = NodeResource.getOvsdbNode(OVS_IDENTIFIER, this);
        assertNotNull("Node " + OVS_IDENTIFIER + " is null", node);
    }

    @Test
    public void testGetOvsdbConnection () {
        ConnectionServiceImpl connectionService = new ConnectionServiceImpl();
        Connection connection = new Connection(IDENTIFIER, null);
        connectionService.putOvsdbConnection(IDENTIFIER, connection);

        PowerMockito.mockStatic(ServiceHelper.class);
        when(ServiceHelper.getGlobalInstance(eq(OvsdbConnectionService.class), anyObject()))
                .thenReturn(null)
                .thenReturn(connectionService)
                .thenReturn(connectionService);

        Connection testConnection = null;
        try {
            testConnection = NodeResource.getOvsdbConnection(IDENTIFIER, this);
            fail("Expected an ServiceUnavailableException to be thrown");
        } catch (ServiceUnavailableException e) {
            assertSame(ServiceUnavailableException.class, e.getClass());
        }

        try {
            testConnection = NodeResource.getOvsdbConnection(BAD_IDENTIFIER, this);
            fail("Expected an ResourceNotFoundException to be thrown");
        } catch (ResourceNotFoundException e) {
            assertSame(ResourceNotFoundException.class, e.getClass());
        }

        testConnection = NodeResource.getOvsdbConnection(IDENTIFIER, this);
        assertNotNull("Connection " + OVS_IDENTIFIER + " is null", testConnection);
    }
}
