package org.opendaylight.ovsdb.plugin;

import org.junit.Test;
import org.opendaylight.controller.sal.core.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.internal.Pair;

public class OvsdbTestSetManagerIT extends OvsdbTestBase {
    private static final Logger logger = LoggerFactory
            .getLogger(OvsdbTestSetManagerIT.class);

    @Test
    public void setManager() throws Throwable{
        Pair<ConnectionService, Node> connection = getTestConnection();
        ConnectionService connectionService = connection.first;
        Node node = connection.second;

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
