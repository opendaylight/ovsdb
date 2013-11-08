package org.opendaylight.ovsdb.plugin;

import java.util.List;

import org.junit.Test;
import org.opendaylight.controller.sal.core.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.internal.Pair;

public class OVSDBTestGetBridgeDomainsIT extends OvsdbTestBase {
    private static final Logger logger = LoggerFactory
            .getLogger(OVSDBTestGetBridgeDomainsIT.class);

    @Test
    public void getBridgeDomains() throws Throwable{

        Pair<ConnectionService, Node> connection = getTestConnection();
        ConnectionService connectionService = connection.first;
        Node node = connection.second;

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
