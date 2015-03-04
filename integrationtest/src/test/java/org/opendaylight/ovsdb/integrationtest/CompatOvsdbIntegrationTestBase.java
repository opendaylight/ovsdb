package org.opendaylight.ovsdb.integrationtest;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.ovsdb.plugin.api.ConnectionConstants;
import org.opendaylight.ovsdb.compatibility.plugin.api.OvsdbConnectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CompatOvsdbIntegrationTestBase extends OvsdbIntegrationTestBase {
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbIntegrationTestBase.class);

    public Node getCompatPluginTestConnection() throws IOException,
            InterruptedException, ExecutionException, TimeoutException {
        Properties props = loadProperties();
        String addressStr = props.getProperty(SERVER_IPADDRESS);
        String portStr = props.getProperty(SERVER_PORT, DEFAULT_SERVER_PORT);
        String connectionType = props.getProperty(CONNECTION_TYPE, "active");
        org.opendaylight.controller.sal.core.Node node = null;

        OvsdbConnectionService connection = (OvsdbConnectionService)
                ServiceHelper.getGlobalInstance(OvsdbConnectionService.class, this);
        // If the connection type is active, controller connects to the ovsdb-server
        if (connectionType.equalsIgnoreCase(CONNECTION_TYPE_ACTIVE)) {
            if (addressStr == null) {
                fail(usage());
            }

            Map<ConnectionConstants, String> params = new HashMap<ConnectionConstants, String>();
            params.put(ConnectionConstants.ADDRESS, addressStr);
            params.put(ConnectionConstants.PORT, portStr);
            node = connection.connect(IDENTIFIER, params);
        }  else if (connectionType.equalsIgnoreCase(CONNECTION_TYPE_PASSIVE)) {
            // Wait for CONNECTION_INIT_TIMEOUT for the Passive connection to be initiated by the ovsdb-server.
            Thread.sleep(CONNECTION_INIT_TIMEOUT);
            List<Node> nodes = connection.getNodes();
            assertNotNull(nodes);
            assertTrue(nodes.size() > 0);
            node = nodes.get(0);
        }

        if (node != null) {
            LOG.info("getPluginTestConnection: Successfully connected to {}", node);
        } else {
            fail("Connection parameter (" + CONNECTION_TYPE + ") must be active or passive");
        }
        return node;
    }
}
