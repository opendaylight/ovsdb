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

import org.junit.Test;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.ovsdb.plugin.impl.ConfigurationServiceImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BridgeDomainConfigManagerTestCases extends PluginTestBase {
    private static final Logger logger = LoggerFactory
            .getLogger(BridgeDomainConfigManagerTestCases.class);

    @Test
    public void setManager() throws Throwable{
        TestObjects testObjects = getTestConnection();
        Node node = testObjects.node;


        String port = "6634";
        String host = FAKE_IP;
        String connectionType = "ptcp";

        String manager = connectionType + ":" + host + ":" + port;

        /**
         * Implements the OVS Connection for Managers
         *
         * @param node Node serving this configuration service
         * @param String with IP and connection type ex. type:ip:port
         *
         */
        ConfigurationServiceImpl configurationService = testObjects.configurationService;
        configurationService.setManager(node, manager);
    }

}