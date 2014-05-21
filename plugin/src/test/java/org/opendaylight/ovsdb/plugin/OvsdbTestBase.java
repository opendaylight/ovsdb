/*
 * [[ Authors will Fill in the Copyright header ]]
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Brent Salisbury, Hugo Trippaers
 */
package org.opendaylight.ovsdb.plugin;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import junit.framework.Assert;

import org.opendaylight.controller.sal.connection.ConnectionConstants;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;

public abstract class OvsdbTestBase {
    private final static String identifier = "TEST";
    private final static String SERVER_IPADDRESS = "ovsdbserver.ipaddress";
    private final static String SERVER_PORT = "ovsdbserver.port";
    private final static String DEFAULT_SERVER_PORT = "6640";

    public Properties loadProperties() throws IOException {
        Properties props = new Properties(System.getProperties());
        return props;
    }

    public class TestObjects {
        public final ConnectionService connectionService;
        public final Node node;

        public TestObjects(ConnectionService connectionService, Node node) {
            this.connectionService = connectionService;
            this.node = node;
        }
    }

    public TestObjects getTestConnection() throws IOException {
        Properties props = loadProperties();
        String address = props.getProperty(SERVER_IPADDRESS);
        String port = props.getProperty(SERVER_PORT, DEFAULT_SERVER_PORT);

        if (address == null) {
            Assert.fail("Usage : mvn -Pintegrationtest -Dovsdbserver.ipaddress=x.x.x.x -Dovsdbserver.port=yyyy verify");
        }

        Node.NodeIDType.registerIDType("OVS", String.class);
        NodeConnector.NodeConnectorIDType.registerIDType("OVS", String.class,
                "OVS");
        InventoryService inventoryService = new InventoryService();
        inventoryService.init();

        ConnectionService connectionService = new ConnectionService();
        connectionService.init();

        connectionService.setInventoryServiceInternal(inventoryService);
        Map<ConnectionConstants, String> params = new HashMap<ConnectionConstants, String>();

        params.put(ConnectionConstants.ADDRESS, address);
        params.put(ConnectionConstants.PORT, port);

        Node node = connectionService.connect(identifier, params);
        if (node == null) {
            throw new IOException("Failed to connecto to ovsdb server");
        }
        return new TestObjects(connectionService, node);
    }

}
