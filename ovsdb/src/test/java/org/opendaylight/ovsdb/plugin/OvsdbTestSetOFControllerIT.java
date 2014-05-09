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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsdbTestSetOFControllerIT extends OvsdbTestBase {
    private static final Logger logger = LoggerFactory
            .getLogger(OvsdbTestSetOFControllerIT.class);

    @Test
    public void setController() throws Throwable{
        TestObjects testObjects = getTestConnection();
        ConnectionService connectionService = testObjects.connectionService;
        Node node = testObjects.node;

        Map<ConfigConstants, Object> configs = new HashMap<ConfigConstants, Object>();
        configs.put(ConfigConstants.DEST_IP, "192.168.254.1");
        configs.put(ConfigConstants.CUSTOM, "6633");
        ConfigurationService configurationService = new ConfigurationService();
        configurationService.setConnectionServiceInternal(connectionService);
        configurationService.setBridgeOFController(node, BRIDGE_NAME);

    }
}
