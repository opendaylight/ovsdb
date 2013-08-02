package org.opendaylight.ovsdb;

import org.junit.Test;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.ovsdb.internal.ConfigurationService;
import org.opendaylight.ovsdb.internal.ConnectionService;
import org.opendaylight.controller.sal.connection.ConnectionConstants;
import org.opendaylight.controller.sal.networkconfig.bridgedomain.ConfigConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.Map;

public class OvsdbTestAddVlan {
    private static final Logger logger = LoggerFactory
            .getLogger(OvsdbTestAddVlan.class);

    @Test
    public void addPortVlan() throws Throwable{
        Node.NodeIDType.registerIDType("OVS", String.class);
        NodeConnector.NodeConnectorIDType.registerIDType("OVS", String.class, "OVS");

        ConnectionService connectionService = new ConnectionService();
        connectionService.init();
        String identifier = "TEST";
        Map<ConnectionConstants, String> params = new HashMap<ConnectionConstants, String>();
        params.put(ConnectionConstants.ADDRESS, "172.28.30.51");
        params.put(ConnectionConstants.PORT, "6634");
        int vlanid = 100;

        Node node = connectionService.connect(identifier, params);
        if(node == null){
            logger.error("Could not connect to ovsdb server");
            return;
        }
        /**
         * Create a Port with a user defined VLAN, and attach it to the specified bridge.
         *
         * Ex. ovs-vsctl add-port JUNIT_BRIDGE_TEST Jvlanvif0 tag=100
         * @param node Node serving this configuration service
         * @param bridgeDomainIdentifier String representation of a Bridge Domain
         * @param portIdentifier String representation of a user defined Port Name
         * @param vlanid Integer note: only one VID is accepted with tag=x method
         */
        ConfigurationService configurationService = new ConfigurationService();
        configurationService.setConnectionServiceInternal(connectionService);
        Map<ConfigConstants, Object> configs = new HashMap<ConfigConstants, Object>();
        configs.put(ConfigConstants.TYPE, "VLAN");
        configs.put(ConfigConstants.VLAN, vlanid+"");
        configurationService.addPort(node, "JUNIT_BRIDGE_TEST", "Jtagvif0", configs);
    }
}