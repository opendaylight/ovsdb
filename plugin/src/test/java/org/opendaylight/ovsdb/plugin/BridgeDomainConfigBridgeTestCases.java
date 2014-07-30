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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.networkconfig.bridgedomain.ConfigConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BridgeDomainConfigBridgeTestCases extends PluginTestBase {
    private static final Logger logger = LoggerFactory
            .getLogger(BridgeDomainConfigBridgeTestCases.class);

    @Test
    public void addBridge() throws Throwable{

        TestObjects testObjects = getTestConnection();
        ConnectionService connectionService = testObjects.connectionService;
        Node node = testObjects.node;

        /**
         * Create a Bridge Domain
         *
         * @param node Node serving this configuration service
         * @param bridgeDomainIdentifier String representation of a Bridge Domain
         */
        ConfigurationService configurationService = testObjects.configurationService;
        configurationService.createBridgeDomain(node, BRIDGE_NAME, null);
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

    @Test
    public void setController() throws Throwable{
        TestObjects testObjects = getTestConnection();
        Node node = testObjects.node;

        ConfigurationService configurationService = testObjects.configurationService;
        configurationService.setBridgeOFController(node, BRIDGE_NAME);

    }

    @Test
    public void setBridgeConfig() throws Throwable{
        TestObjects testObjects = getTestConnection();
        ConnectionService connectionService = testObjects.connectionService;
        Node node = testObjects.node;

        Map<ConfigConstants, Object> configs = new HashMap<ConfigConstants, Object>();

        Map<String, String> exterIDPairs = new HashMap<String, String>();
        exterIDPairs.put("br-foo", "br-bar");
        //Will accept multiple array pairs. Pairs must be arrays not maps.
        configs.put(ConfigConstants.CUSTOM, exterIDPairs);

        ConfigurationService configurationService = testObjects.configurationService;
        configurationService.addBridgeDomainConfig(node, BRIDGE_NAME, configs);
    }

}
