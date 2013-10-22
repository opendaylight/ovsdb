package org.opendaylight.ovsdb;

import org.junit.Test;
import org.opendaylight.controller.sal.connection.ConnectionConstants;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.networkconfig.bridgedomain.ConfigConstants;
import org.opendaylight.ovsdb.internal.ConfigurationService;
import org.opendaylight.ovsdb.internal.ConnectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class OvsdbTestBridgeConfig {
    private static final Logger logger = LoggerFactory
            .getLogger(OvsdbTestSetManager.class);

    @Test
    public void setBridgeConfig() throws Throwable{
        Node.NodeIDType.registerIDType("OVS", String.class);
        NodeConnector.NodeConnectorIDType.registerIDType("OVS", String.class, "OVS");

        ConnectionService connectionService = new ConnectionService();
        connectionService.init();
        String identifier = "TEST";
        Map<ConnectionConstants, String> params = new HashMap<ConnectionConstants, String>();
        params.put(ConnectionConstants.ADDRESS, "192.168.254.128");
        params.put(ConnectionConstants.PORT, "6640");

        Map<ConfigConstants, Object> configs = new HashMap<ConfigConstants, Object>();

        Map<String, String> exterIDPairs = new HashMap<String, String>();
        exterIDPairs.put("bridge-foo", "bri-bar");
        //Will accept multiple array pairs. Pairs must be arrays not maps.
        configs.put(ConfigConstants.CUSTOM, exterIDPairs);

        Node node = connectionService.connect(identifier, params);
        if(node == null){
            logger.error("Could not connect to ovsdb server");
            return;
        }

        ConfigurationService configurationService = new ConfigurationService();
        configurationService.setConnectionServiceInternal(connectionService);
        configurationService.addBridgeDomainConfig(node, "br0", configs);
    }

}