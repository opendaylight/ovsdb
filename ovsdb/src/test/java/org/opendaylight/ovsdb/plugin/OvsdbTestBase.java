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
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.opendaylight.controller.sal.connection.ConnectionConstants;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;

public abstract class OvsdbTestBase {
    private final static String identifier = "TEST";
    protected final static String BRIDGE_NAME = "JUNIT_TEST_BRIDGE";
    protected final static String PORT_NAME = "eth0";
    protected final static String TAGGED_PORT_NAME = "eth1";
    protected final static String TUNNEL_PORT_NAME = "vxlan0";
    protected final static String FAKE_IP = "192.168.254.254";

    public Properties loadProperties() throws IOException {
        InputStream is = this
                .getClass()
                .getClassLoader()
                .getResourceAsStream(
                        "org/opendaylight/ovsdb/lib/message/integration-test.properties");
        if (is == null) {
            throw new IOException("Unable to load integration-test.properties");
        }
        Properties props = new Properties();
        props.load(is);

        return props;
    }

    public class TestObjects {
        public final ConnectionService connectionService;
        public final InventoryService inventoryService;
        public final Node node;

        public TestObjects(ConnectionService connectionService, Node node, InventoryService inventoryService) {
            this.connectionService = connectionService;
            this.inventoryService = inventoryService;
            this.node = node;
        }
    }

    public TestObjects getTestConnection() throws IOException {
        Node.NodeIDType.registerIDType("OVS", String.class);
        NodeConnector.NodeConnectorIDType.registerIDType("OVS", String.class,
                "OVS");

        ConnectionService connectionService = new ConnectionService();
        connectionService.init();
        InventoryService inventory = new InventoryService();
        inventory.init();
        connectionService.setInventoryServiceInternal(inventory);
        Map<ConnectionConstants, String> params = new HashMap<ConnectionConstants, String>();
        Properties props = loadProperties();
        params.put(ConnectionConstants.ADDRESS,
                props.getProperty("ovsdbserver.ipaddress"));
        params.put(ConnectionConstants.PORT,
                props.getProperty("ovsdbserver.port", "6640"));

        Node node = connectionService.connect(identifier, params);
        if (node == null) {
            throw new IOException("Failed to connect to the ovsdb server");
        }
        return new TestObjects(connectionService, node, inventory);
    }

}
