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
import org.opendaylight.ovsdb.plugin.impl.ConfigurationServiceImpl;
import org.opendaylight.ovsdb.plugin.internal.Encapsulation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsdbTestAddTunnelIT extends OvsdbTestBase {
    private static final Logger logger = LoggerFactory
            .getLogger(OvsdbTestAddTunnelIT.class);

    @Test
    public void addTunnel() throws Throwable{
        TestObjects testObjects = getTestConnection();
        Node node = testObjects.node;

        /**
         * tunnelendpoint IP address of the
         * destination Tunnel Endpoint.
         * tunencap is the tunnel encapsulation
         * options being (CAPWAP, GRE, VXLAN).
         * Use the following lines to test GRE and CAPWAP
         * Encapsulation encap = Encapsulation.GRE;
         * Encapsulation encap = Encapsulation.CAPWAP;
         */

        Encapsulation encap = Encapsulation.VXLAN;
        String tunencap = encap.toString();
        String tunnelendpoint = FAKE_IP;

        /**
         * Create an Encapsulated Tunnel Interface and destination Tunnel Endpoint
         *
         * Ex. ovs-vsctl add-port br0 vxlan1 (cont)
         * -- set interface vxlan1 type=vxlan options:remote_ip=192.168.1.11
         * @param node Node serving this configuration service
         * @param bridgeDomainIdentifier String representation of a Bridge Domain
         * @param portIdentifier String representation of a user defined Port Name
         * @param tunnelendpoint IP address of the destination Tunnel Endpoint
         * @param tunencap is the tunnel encapsulation options being CAPWAP, GRE or VXLAN
         * The Bridge must already be defined before calling addTunnel.
         */
        ConfigurationServiceImpl configurationService = testObjects.configurationService;
        Map<ConfigConstants, Object> configs = new HashMap<ConfigConstants, Object>();
        configs.put(ConfigConstants.TYPE, "TUNNEL");
        configs.put(ConfigConstants.TUNNEL_TYPE, tunencap);
        configs.put(ConfigConstants.DEST_IP, tunnelendpoint);

        configurationService.addPort(node, BRIDGE_NAME, TUNNEL_PORT_NAME, configs);

    }
}