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
import org.opendaylight.ovsdb.plugin.impl.ConfigurationServiceImpl;
import org.opendaylight.ovsdb.plugin.impl.ConnectionServiceImpl;
import org.opendaylight.ovsdb.plugin.impl.InventoryServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BridgeDomainConfigBridgeTestCases extends PluginTestBase {
    private static final Logger logger = LoggerFactory
            .getLogger(BridgeDomainConfigBridgeTestCases.class);

    /**
     * Add OVS bridge "JUNIT_TEST_BRIDGE" for all other test
     * Ex. ovs-vsctl add-br br0
     *
     * @throws Throwable
     */
    @Test
    public void addBridge() throws Throwable{

        TestObjects testObjects = getTestConnection();
        ConnectionServiceImpl connectionService = testObjects.connectionService;
        Node node = testObjects.node;

        /**
         * Create a Bridge Domain
         *
         * @param node Node serving this configuration service
         * @param bridgeDomainIdentifier String representation of a Bridge Domain
         */
        ConfigurationServiceImpl configurationService = testObjects.configurationService;
        configurationService.createBridgeDomain(node, BRIDGE_NAME, null);
    }

    /**
     * List all bridge domains on this OVS bridge
     * Ex. ovs-vsctl show
     *
     * @throws Throwable
     */
    @Test
    public void getBridgeDomains() throws Throwable{

        TestObjects testObjects = getTestConnection();
        ConnectionServiceImpl connectionService = testObjects.connectionService;
        InventoryServiceImpl inventoryService = testObjects.inventoryService;
        Node node = testObjects.node;

        /**
         * List a Bridge Domain
         *
         * @param node Node serving this configuration service
         *
         */
        ConfigurationServiceImpl configurationService = testObjects.configurationService;
        List<String> ls = configurationService.getBridgeDomains(node);
    }

    /**
     * Register self node as the bridge's OpenFlow controller
     * e.g., ovs-vsctl set-controller br0 <node>
     *
     * @throws Throwable
     */
    @Test
    public void setController() throws Throwable{
        TestObjects testObjects = getTestConnection();
        Node node = testObjects.node;

        ConfigurationServiceImpl configurationService = testObjects.configurationService;
        configurationService.setBridgeOFController(node, BRIDGE_NAME);
    }

    /**
     * Test the assignment of "external ID" key/value pairs on bridge
     * e.g., ovs-vsctl br−set−external−id br0 <key> <value>
     *
     * @throws Throwable
     */
    @Test
    public void setBridgeConfig() throws Throwable{
        TestObjects testObjects = getTestConnection();
        ConnectionServiceImpl connectionService = testObjects.connectionService;
        Node node = testObjects.node;

        Map<ConfigConstants, Object> configs = new HashMap<ConfigConstants, Object>();

        Map<String, String> exterIDPairs = new HashMap<String, String>();
        exterIDPairs.put("br-foo", "br-bar");
        //Will accept multiple array pairs. Pairs must be arrays not maps.
        configs.put(ConfigConstants.CUSTOM, exterIDPairs);

        ConfigurationServiceImpl configurationService = testObjects.configurationService;
        configurationService.addBridgeDomainConfig(node, BRIDGE_NAME, configs);
    }

}
