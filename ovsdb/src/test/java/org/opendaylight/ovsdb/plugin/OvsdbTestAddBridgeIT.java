/*
 * Copyright (C) 2013 of individual owners listed as Authors
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

import com.google.gson.internal.Pair;

public class OvsdbTestAddBridgeIT extends OvsdbTestBase {
    private static final Logger logger = LoggerFactory
            .getLogger(OvsdbTestAddBridgeIT.class);

    @Test
    public void addBridge() throws Throwable{

        Pair<ConnectionService, Node> connection = getTestConnection();
        ConnectionService connectionService = connection.first;
        Node node = connection.second;

        /**
         * Create a Bridge Domain
         *
         * @param node Node serving this configuration service
         * @param bridgeDomainIdentifier String representation of a Bridge Domain
         */
        ConfigurationService configurationService = new ConfigurationService();
        configurationService.setConnectionServiceInternal(connectionService);
        configurationService.createBridgeDomain(node, "JUNIT_BRIDGE_TEST", null);
    }

}
