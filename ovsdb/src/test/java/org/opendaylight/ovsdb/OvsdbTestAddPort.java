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
        params.put(ConnectionConstants.ADDRESS, "172.28.30.51");
        params.put(ConnectionConstants.PORT, "6634");

        Node node = connectionService.connect(identifier, params);
        if(node == null){
            logger.error("Could not connect to ovsdb server");
            return;
        }
        /**
         * Create a Port and attach it to a Bridge
         * Ex. ovs-vsctl add-port br0 vif0
         * @param node Node serving this configuration service
         * @param bridgeDomainIdentifier String representation of a Bridge Domain
         * @param portIdentifier String representation of a user defined Port Name
         */
        ConfigurationService configurationService = new ConfigurationService();
        configurationService.setConnectionServiceInternal(connectionService);
        configurationService.addPort(node, "JUNIT_BRIDGE_TEST", "Jvif0", null);
    }
}