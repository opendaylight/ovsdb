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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Lists;
import java.util.List;
import javax.ws.rs.core.Response;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.northbound.commons.exception.ResourceNotFoundException;
import org.opendaylight.controller.northbound.commons.exception.ServiceUnavailableException;
import org.opendaylight.ovsdb.plugin.api.Connection;
import org.opendaylight.ovsdb.plugin.api.OvsdbConnectionService;
import org.opendaylight.ovsdb.plugin.impl.ConnectionServiceImpl;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ServiceHelper.class)
public class NodeResourceTest {
    private static final String OVS = "OVS";
    private static final String IDENTIFIER = "192.168.120.31:45001";
    private static final String IDENTIFIER2 = "192.168.120.31:45002";
    private static final String OVS_IDENTIFIER = OVS + "|" + IDENTIFIER;
    private static final String OVS_IDENTIFIER2 = OVS + "|" + IDENTIFIER2;
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

        try {
            NodeResource.getOvsdbNode(IDENTIFIER, this);
            fail("Expected an ServiceUnavailableException to be thrown");
        } catch (ServiceUnavailableException e) {
            assertSame(ServiceUnavailableException.class, e.getClass());
        }

        try {
            NodeResource.getOvsdbNode(BAD_IDENTIFIER, this);
            fail("Expected an ResourceNotFoundException to be thrown");
        } catch (ResourceNotFoundException e) {
            assertSame(ResourceNotFoundException.class, e.getClass());
        }

        Node node = NodeResource.getOvsdbNode(OVS_IDENTIFIER, this);
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

        try {
            NodeResource.getOvsdbConnection(IDENTIFIER, this);
            fail("Expected an ServiceUnavailableException to be thrown");
        } catch (ServiceUnavailableException e) {
            assertSame(ServiceUnavailableException.class, e.getClass());
        }

        try {
            NodeResource.getOvsdbConnection(BAD_IDENTIFIER, this);
            fail("Expected an ResourceNotFoundException to be thrown");
        } catch (ResourceNotFoundException e) {
            assertSame(ResourceNotFoundException.class, e.getClass());
        }

        Connection testConnection = NodeResource.getOvsdbConnection(IDENTIFIER, this);
        assertNotNull("Connection " + OVS_IDENTIFIER + " is null", testConnection);
    }

    @Test
    public void testGetNodes () {
        ConnectionServiceImpl connectionService = new ConnectionServiceImpl();

        PowerMockito.mockStatic(ServiceHelper.class);
        when(ServiceHelper.getGlobalInstance(eq(OvsdbConnectionService.class), anyObject()))
                .thenReturn(connectionService)
                .thenReturn(connectionService)
                .thenReturn(connectionService);

        NodeResource nodeResource = new NodeResource();

        // Check getNodes when there are no nodes
        try {
            Response response = nodeResource.getNodes();
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            assertNotNull("entity should not be null", response.getEntity());
            String id = "";
            List<String> ids = Lists.newArrayList();
            ids.add(id);
            assertEquals("there should be no nodes", ids.toString(), response.getEntity());
        } catch (JsonProcessingException ex) {
            fail("Exception should not have been caught");
        }

        // Check getNodes when there is a node
        Connection connection = new Connection(IDENTIFIER, null);
        connectionService.putOvsdbConnection(IDENTIFIER, connection);

        try {
            Response response = nodeResource.getNodes();
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            assertNotNull("entity should not be null", response.getEntity());
            String id = "\"" + OVS_IDENTIFIER + "\"";
            List<String> ids = Lists.newArrayList();
            ids.add(id);
            assertEquals(OVS_IDENTIFIER + " should be found", ids.toString(), response.getEntity());
        } catch (JsonProcessingException ex) {
            fail("Exception should not have been caught");
        }

        // Check getNodes when there are multiple nodes
        connection = new Connection(IDENTIFIER2, null);
        connectionService.putOvsdbConnection(IDENTIFIER2, connection);

        try {
            Response response = nodeResource.getNodes();
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            assertNotNull("entity should not be null", response.getEntity());
            String id = "\"" + OVS_IDENTIFIER + "\"";
            String id2 = "\"" + OVS_IDENTIFIER2 + "\"";
            List<String> ids = Lists.newArrayList();
            ids.add(id);
            ids.add(id2);
            assertEquals(OVS_IDENTIFIER + " and " + OVS_IDENTIFIER2 + " should be found",
                    ids.toString().replaceAll("\\s",""), response.getEntity());
        } catch (JsonProcessingException ex) {
            fail("Exception should not have been caught");
        }
    }
}
