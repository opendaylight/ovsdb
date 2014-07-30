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
import java.util.Map;

import org.junit.Test;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.networkconfig.bridgedomain.ConfigConstants;
import org.opendaylight.ovsdb.plugin.impl.ConfigurationServiceImpl;
import org.opendaylight.ovsdb.plugin.impl.ConnectionServiceImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsdbTestBridgeConfigIT extends OvsdbTestBase {
    private static final Logger logger = LoggerFactory
            .getLogger(OvsdbTestSetManagerIT.class);

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