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
import org.opendaylight.ovsdb.lib.impl.OvsdbConnectionService;
import org.opendaylight.ovsdb.plugin.impl.ConfigurationServiceImpl;
import org.opendaylight.ovsdb.plugin.impl.ConnectionServiceImpl;
import org.opendaylight.ovsdb.plugin.impl.InventoryServiceImpl;

public abstract class PluginTestBase {
    private final static String identifier = "TEST";
    protected final static String BRIDGE_NAME = "JUNIT_TEST_BRIDGE";
    protected final static String PORT_NAME = "test0";
    protected final static String TAGGED_PORT_NAME = "test1";
    protected final static String TUNNEL_PORT_NAME = "vxlan0";
    protected final static String FAKE_IP = "192.168.254.254";
    private final static String SERVER_IPADDRESS = "ovsdbserver.ipaddress";
    private final static String SERVER_PORT = "ovsdbserver.port";
    private final static String DEFAULT_SERVER_PORT = "6640";

    public Properties loadProperties() throws IOException {
        Properties props = new Properties(System.getProperties());
        return props;
    }

    public class TestObjects {
        public final ConnectionServiceImpl connectionService;
        public final InventoryServiceImpl inventoryService;
        public final ConfigurationServiceImpl configurationService;
        public final Node node;

        public TestObjects(ConnectionServiceImpl connectionService, Node node, InventoryServiceImpl inventoryService, ConfigurationServiceImpl configurationService) {
            this.connectionService = connectionService;
            this.inventoryService = inventoryService;
            this.configurationService = configurationService;
            this.node = node;
        }
    }

    public TestObjects getTestConnection() throws IOException {
        if (OvsdbPluginTestSuiteIT.getTestObjects() != null) {
            return OvsdbPluginTestSuiteIT.getTestObjects();
        }
        Properties props = loadProperties();
        String address = props.getProperty(SERVER_IPADDRESS);
        String port = props.getProperty(SERVER_PORT, DEFAULT_SERVER_PORT);

        if (address == null) {
            Assert.fail("Usage : mvn -Pintegrationtest -Dovsdbserver.ipaddress=x.x.x.x -Dovsdbserver.port=yyyy verify");
        }

        Node.NodeIDType.registerIDType("OVS", String.class);
        NodeConnector.NodeConnectorIDType.registerIDType("OVS", String.class,
                "OVS");
        InventoryServiceImpl inventoryService = new InventoryServiceImpl();
        inventoryService.init();

        ConnectionServiceImpl connectionService = new ConnectionServiceImpl();
        connectionService.init();
        InventoryServiceImpl inventory = new InventoryServiceImpl();
        inventory.init();
        connectionService.setOvsdbInventoryService(inventory);
        connectionService.setOvsdbConnection(OvsdbConnectionService.getService());
        ConfigurationServiceImpl configurationService = new ConfigurationServiceImpl();
        configurationService.setConnectionServiceInternal(connectionService);
        configurationService.setOvsdbInventoryService(inventory);
        inventory.setOvsdbConfigurationService(configurationService);

        Map<ConnectionConstants, String> params = new HashMap<ConnectionConstants, String>();

        params.put(ConnectionConstants.ADDRESS, address);
        params.put(ConnectionConstants.PORT, port);

        Node node = connectionService.connect(identifier, params);
        if (node == null) {
            throw new IOException("Failed to connect to the ovsdb server");
        }

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        TestObjects testObject = new TestObjects(connectionService, node, inventory, configurationService);
        OvsdbPluginTestSuiteIT.setTestObjects(testObject);
        return testObject;
    }

}
