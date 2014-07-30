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
import org.opendaylight.ovsdb.plugin.impl.ConnectionServiceImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsdbTestDeletePortIT extends OvsdbTestBase {
    private static final Logger logger = LoggerFactory
            .getLogger(OvsdbTestAddPortIT.class);

    @Test
    public void deletePort() throws Throwable{
        TestObjects testObjects = getTestConnection();
        ConnectionServiceImpl connectionService = testObjects.connectionService;
        Node node = testObjects.node;

        /**
         * Deletes an existing port from an existing bridge
         * Ex. ovs-vsctl del-port ovsbr0 tap0
         * @param node Node serving this configuration service
         * @param bridgeDomainIdentifier String representation of a Bridge Domain
         * @param portIdentifier String representation of a user defined Port Name
         */
        ConfigurationServiceImpl configurationService = testObjects.configurationService;
        configurationService.deletePort(node, BRIDGE_NAME, PORT_NAME);
    }
}
