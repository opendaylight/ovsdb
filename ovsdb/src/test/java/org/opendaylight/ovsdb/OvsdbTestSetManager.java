package org.opendaylight.ovsdb;

import org.junit.Test;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.ovsdb.internal.ConfigurationService;
import org.opendaylight.ovsdb.internal.ConnectionService;
import org.opendaylight.controller.sal.connection.ConnectionConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class OvsdbTestSetManager {
    private static final Logger logger = LoggerFactory
            .getLogger(OvsdbTestSetManager.class);

    @Test
    public void setManager() throws Throwable{
        Node.NodeIDType.registerIDType("OVS", String.class);
        NodeConnector.NodeConnectorIDType.registerIDType("OVS", String.class, "OVS");

        ConnectionService connectionService = new ConnectionService();
        connectionService.init();
        String identifier = "TEST";
        Map<ConnectionConstants, String> params = new HashMap<ConnectionConstants, String>();
        params.put(ConnectionConstants.ADDRESS, "172.28.30.51");

        Node node = connectionService.connect(identifier, params);
        if(node == null){
            logger.error("Could not connect to ovsdb server");
            return;
        }
        /**
         * Implements the OVS Connection for Managers
         *
         * @param node Node serving this configuration service
         * @param String with IP and connection type ex. type:ip:port
         *
         */
        ConfigurationService configurationService = new ConfigurationService();
        configurationService.setConnectionServiceInternal(connectionService);
        configurationService.setManager(node, "ptcp:6634:172.16.58.128");
    }

}
