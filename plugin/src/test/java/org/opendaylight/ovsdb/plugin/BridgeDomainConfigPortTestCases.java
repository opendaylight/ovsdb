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
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.networkconfig.bridgedomain.ConfigConstants;
import org.opendaylight.ovsdb.plugin.impl.ConfigurationServiceImpl;
import org.opendaylight.ovsdb.plugin.impl.ConnectionServiceImpl;
import org.opendaylight.ovsdb.plugin.internal.Encapsulation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BridgeDomainConfigPortTestCases extends PluginTestBase {
    private static final Logger logger = LoggerFactory
            .getLogger(BridgeDomainConfigPortTestCases.class);
    private Properties props;

    @Before
    public void loadProps() throws IOException {
        props = loadProperties();
    }

    /**
     * Create a Port and attach it to a Bridge
     * Ex. ovs-vsctl add-port br0 vif0
     *
     * @throws Throwable
     */
    @Test
    public void addPort() throws Throwable{
        TestObjects testObjects = getTestConnection();
        ConnectionServiceImpl connectionService = testObjects.connectionService;
        Node node = testObjects.node;
        ConfigurationServiceImpl configurationService = testObjects.configurationService;
        configurationService.addPort(node, BRIDGE_NAME, PORT_NAME, null);
    }

    /**
     * Create a Port with a user defined VLAN, and attach it to the specified bridge.
     * Ex. ovs-vsctl add-port JUNIT_BRIDGE_TEST Jvlanvif0 tag=100
     *
     * @throws Throwable
     */
    @Test
    public void addPortVlan() throws Throwable{
        TestObjects testObjects = getTestConnection();
        ConnectionServiceImpl connectionService = testObjects.connectionService;
        Node node = testObjects.node;

        int vlanid = 100;

        ConfigurationServiceImpl configurationService = testObjects.configurationService;
        Map<ConfigConstants, Object> configs = new HashMap<ConfigConstants, Object>();
        configs.put(ConfigConstants.TYPE, "VLAN");
        configs.put(ConfigConstants.VLAN, vlanid+"");
        configurationService.addPort(node, BRIDGE_NAME, TAGGED_PORT_NAME, configs);
    }

    /**
     * Create an Encapsulated Tunnel Interface and destination Tunnel Endpoint
     * tunnelendpoint IP address of the destination Tunnel Endpoint.
     * tunencap is the tunnel encapsulation options being (CAPWAP, GRE, VXLAN).
     *
     * Ex. ovs-vsctl add-port br0 vxlan1 (cont)
     * -- set interface vxlan1 type=vxlan options:remote_ip=192.168.1.11
     *
     * @throws Throwable
     */

    @Test
    public void addTunnel() throws Throwable{
        TestObjects testObjects = getTestConnection();
        Node node = testObjects.node;

        Encapsulation encap = Encapsulation.VXLAN;
        String tunencap = encap.toString();
        String tunnelendpoint = FAKE_IP;

        ConfigurationServiceImpl configurationService = testObjects.configurationService;
        Map<ConfigConstants, Object> configs = new HashMap<ConfigConstants, Object>();
        configs.put(ConfigConstants.TYPE, "TUNNEL");
        configs.put(ConfigConstants.TUNNEL_TYPE, tunencap);
        configs.put(ConfigConstants.DEST_IP, tunnelendpoint);

        configurationService.addPort(node, BRIDGE_NAME, TUNNEL_PORT_NAME, configs);
    }

    /**
     * Deletes an existing port from an existing bridge
     * Ex. ovs-vsctl del-port ovsbr0 tap0
     *
     * @throws Throwable
     */
    @Test
    public void deletePort() throws Throwable{
        TestObjects testObjects = getTestConnection();
        ConnectionServiceImpl connectionService = testObjects.connectionService;
        Node node = testObjects.node;

        ConfigurationServiceImpl configurationService = testObjects.configurationService;
        configurationService.deletePort(node, BRIDGE_NAME, PORT_NAME);
    }

}
