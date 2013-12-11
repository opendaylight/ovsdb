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

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.networkconfig.bridgedomain.ConfigConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.internal.Pair;

public class OvsdbTestBridgeConfigIT extends OvsdbTestBase {
    private static final Logger logger = LoggerFactory
            .getLogger(OvsdbTestSetManagerIT.class);

    @Test
    public void setBridgeConfig() throws Throwable{
        Pair<ConnectionService, Node> connection = getTestConnection();
        ConnectionService connectionService = connection.first;
        Node node = connection.second;

        Map<ConfigConstants, Object> configs = new HashMap<ConfigConstants, Object>();

        Map<String, String> exterIDPairs = new HashMap<String, String>();
        exterIDPairs.put("bridge-foo", "bri-bar");
        //Will accept multiple array pairs. Pairs must be arrays not maps.
        configs.put(ConfigConstants.CUSTOM, exterIDPairs);

        ConfigurationService configurationService = new ConfigurationService();
        configurationService.setConnectionServiceInternal(connectionService);
        configurationService.addBridgeDomainConfig(node, "br0", configs);
    }

}