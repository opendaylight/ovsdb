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
        public final Node node;

        public TestObjects(ConnectionService connectionService, Node node) {
            this.connectionService = connectionService;
            this.node = node;
        }
    }

    public TestObjects getTestConnection() throws IOException {
        Node.NodeIDType.registerIDType("OVS", String.class);
        NodeConnector.NodeConnectorIDType.registerIDType("OVS", String.class,
                "OVS");
        InventoryService inventoryService = new InventoryService();
        inventoryService.init();

        ConnectionService connectionService = new ConnectionService();
        connectionService.init();

        connectionService.setInventoryServiceInternal(inventoryService);
        Map<ConnectionConstants, String> params = new HashMap<ConnectionConstants, String>();
        Properties props = loadProperties();
        params.put(ConnectionConstants.ADDRESS,
                props.getProperty("ovsdbserver.ipaddress"));
        params.put(ConnectionConstants.PORT,
                props.getProperty("ovsdbserver.port", "6640"));

        Node node = connectionService.connect(identifier, params);
        if (node == null) {
            throw new IOException("Failed to connecto to ovsdb server");
        }
        return new TestObjects(connectionService, node);
    }

}
