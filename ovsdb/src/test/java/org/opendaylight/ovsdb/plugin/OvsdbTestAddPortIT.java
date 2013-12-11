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
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.sal.core.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.internal.Pair;

public class OvsdbTestAddPortIT extends OvsdbTestBase {
    private static final Logger logger = LoggerFactory
            .getLogger(OvsdbTestAddPortIT.class);
    private Properties props;

    @Before
    public void loadProps() throws IOException {
        props = loadProperties();
    }

    @Test
    public void addPort() throws Throwable{
        Pair<ConnectionService, Node> connection = getTestConnection();
        ConnectionService connectionService = connection.first;
        Node node = connection.second;

        /**
         * Create a Port and attach it to a Bridge
         * Ex. ovs-vsctl add-port br0 vif0
         * @param node Node serving this configuration service
         * @param bridgeDomainIdentifier String representation of a Bridge Domain
         * @param portIdentifier String representation of a user defined Port Name
         */
        ConfigurationService configurationService = new ConfigurationService();
        configurationService.setConnectionServiceInternal(connectionService);
        configurationService.addPort(node, "JUNIT_BRIDGE_TEST", "Jvif0", null);
    }
}