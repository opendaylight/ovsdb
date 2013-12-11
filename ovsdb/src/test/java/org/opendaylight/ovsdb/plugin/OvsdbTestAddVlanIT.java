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

public class OvsdbTestAddVlanIT extends OvsdbTestBase {
    private static final Logger logger = LoggerFactory
            .getLogger(OvsdbTestAddVlanIT.class);

    @Test
    public void addPortVlan() throws Throwable{
        Pair<ConnectionService, Node> connection = getTestConnection();
        ConnectionService connectionService = connection.first;
        Node node = connection.second;

        int vlanid = 100;

        /**
         * Create a Port with a user defined VLAN, and attach it to the specified bridge.
         *
         * Ex. ovs-vsctl add-port JUNIT_BRIDGE_TEST Jvlanvif0 tag=100
         * @param node Node serving this configuration service
         * @param bridgeDomainIdentifier String representation of a Bridge Domain
         * @param portIdentifier String representation of a user defined Port Name
         * @param vlanid Integer note: only one VID is accepted with tag=x method
         */
        ConfigurationService configurationService = new ConfigurationService();
        configurationService.setConnectionServiceInternal(connectionService);
        Map<ConfigConstants, Object> configs = new HashMap<ConfigConstants, Object>();
        configs.put(ConfigConstants.TYPE, "VLAN");
        configs.put(ConfigConstants.VLAN, vlanid+"");
        configurationService.addPort(node, "JUNIT_BRIDGE_TEST", "Jtagvif0", configs);
    }
}