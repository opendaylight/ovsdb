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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TearDown extends PluginTestBase {
    private static final Logger logger = LoggerFactory
            .getLogger(TearDown.class);

    /**
     * Final Cleanup. Delete the test Bridge.
     */

    @Test
    public void deleteBridge() throws Throwable{

        TestObjects testObjects = getTestConnection();
        ConnectionService connectionService = testObjects.connectionService;
        Node node = testObjects.node;
        ConfigurationService configurationService = testObjects.configurationService;
        configurationService.deleteBridgeDomain(node, BRIDGE_NAME);
    }

}
