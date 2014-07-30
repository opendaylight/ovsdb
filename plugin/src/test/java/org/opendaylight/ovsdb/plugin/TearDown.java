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

public class TearDown extends OvsdbPluginTestBase {
    private static final Logger logger = LoggerFactory
            .getLogger(TearDown.class);

    @Test
    public void deleteBridge() throws Throwable{

        TestObjects testObjects = getTestConnection();
        ConnectionService connectionService = testObjects.connectionService;
        Node node = testObjects.node;

        /**
         * Delete a Bridge Domain
         *
         * @param node Node serving this configuration service
         * @param bridgeDomainIdentifier String representation of a Bridge Domain
         */
        ConfigurationService configurationService = testObjects.configurationService;
        configurationService.deleteBridgeDomain(node, BRIDGE_NAME);
    }

}
