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
import java.util.List;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.sal.core.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetBridgeDomainsIT extends PluginITBase {
    private static final Logger logger = LoggerFactory
            .getLogger(GetBridgeDomainsIT.class);

    private Properties props;

    @Before
    public void loadProps() throws IOException {
        props = loadProperties();
    }

    @Test
    public void getBridgeDomains() throws Throwable{

        TestObjects testObjects = getTestConnection();
        ConnectionService connectionService = testObjects.connectionService;
        InventoryService inventoryService = testObjects.inventoryService;
        Node node = testObjects.node;

        /**
         * List a Bridge Domain
         *
         * @param node Node serving this configuration service
         *
         */
        ConfigurationService configurationService = testObjects.configurationService;
        List<String> ls = configurationService.getBridgeDomains(node);
    }
}
