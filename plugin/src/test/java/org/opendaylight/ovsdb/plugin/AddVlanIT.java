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

import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.networkconfig.bridgedomain.ConfigConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Ignore("Deprecated")
public class AddVlanIT extends PluginITBase {
    private static final Logger logger = LoggerFactory
            .getLogger(AddVlanIT.class);

    @Test
    public void addPortVlan() throws Throwable{
        TestObjects testObjects = getTestConnection();
        ConnectionService connectionService = testObjects.connectionService;
        Node node = testObjects.node;

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
        ConfigurationService configurationService = testObjects.configurationService;
        Map<ConfigConstants, Object> configs = new HashMap<ConfigConstants, Object>();
        configs.put(ConfigConstants.TYPE, "VLAN");
        configs.put(ConfigConstants.VLAN, vlanid+"");
        configurationService.addPort(node, BRIDGE_NAME, TAGGED_PORT_NAME, configs);
    }
}