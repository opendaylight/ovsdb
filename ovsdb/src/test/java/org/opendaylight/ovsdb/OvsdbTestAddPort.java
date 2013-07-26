package org.opendaylight.ovsdb;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.ovsdb.database.Uuid;
import org.opendaylight.ovsdb.internal.*;
import org.opendaylight.ovsdb.sal.connection.ConnectionConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsdbTestAddPort {
    private static final Logger logger = LoggerFactory
            .getLogger(OvsdbTestAddPort.class);

    @Test
    public void addPort() throws Throwable{
        Node.NodeIDType.registerIDType("OVS", String.class);
        NodeConnector.NodeConnectorIDType.registerIDType("OVS", String.class, "OVS");

        ConnectionService connectionService = new ConnectionService();
        connectionService.init();
        String identifier = "TEST";
        Map<ConnectionConstants, String> params = new HashMap<ConnectionConstants, String>();
        params.put(ConnectionConstants.ADDRESS, "192.168.56.101");

        Node node = connectionService.connect(identifier, params);
        if(node == null){
            logger.error("Could not connect to ovsdb server");
            return;
        }
        /**
         * Create a Bridge Domain
         * @param node Node serving this configuration service
         * @param bridgeDomainIdentifier String representation of a Bridge Domain
         * @param portIdentifier String representation of a user defined Port Name
         * Temporarily add the bridge table _uuid to bridge_uuid in ConfigurationService
         */
        ConfigurationService configurationService = new ConfigurationService();
        configurationService.setConnectionServiceInternal(connectionService);
        configurationService.addPort(node, "JUNIT_BRIDGE_TEST", "Jnic1");
    }
}