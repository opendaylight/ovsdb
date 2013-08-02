package org.opendaylight.ovsdb;

import org.junit.Test;
import org.opendaylight.controller.sal.connection.ConnectionConstants;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.ovsdb.internal.ConfigurationService;
import org.opendaylight.ovsdb.internal.ConnectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OVSDBTestGetBridgeDomains {
    private static final Logger logger = LoggerFactory
            .getLogger(OVSDBTestGetBridgeDomains.class);

    @Test
    public void getBridgeDomains() throws Throwable{
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
         * List a Bridge Domain
         *
         * @param node Node serving this configuration service
         *
         */
        ConfigurationService configurationService = new ConfigurationService();
        configurationService.setConnectionServiceInternal(connectionService);
        List<String> ls = configurationService.getBridgeDomains(node);
        System.out.println(ls);
    }
}
