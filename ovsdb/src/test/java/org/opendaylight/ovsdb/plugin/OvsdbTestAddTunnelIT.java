package org.opendaylight.ovsdb.plugin;


import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.networkconfig.bridgedomain.ConfigConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.internal.Pair;

public class OvsdbTestAddTunnelIT extends OvsdbTestBase {
    private static final Logger logger = LoggerFactory
            .getLogger(OvsdbTestAddTunnelIT.class);

    @Test
    public void addTunnel() throws Throwable{
        Pair<ConnectionService, Node> connection = getTestConnection();
        ConnectionService connectionService = connection.first;
        Node node = connection.second;

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
        String tunnelendpoint = "192.168.100.100";

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
        ConfigurationService configurationService = new ConfigurationService();
        configurationService.setConnectionServiceInternal(connectionService);
        Map<ConfigConstants, Object> configs = new HashMap<ConfigConstants, Object>();
        configs.put(ConfigConstants.TYPE, "TUNNEL");
        configs.put(ConfigConstants.TUNNEL_TYPE, tunencap);
        configs.put(ConfigConstants.DEST_IP, tunnelendpoint);

        configurationService.addPort(node, "JUNIT_BRIDGE_TEST", "Jtunnel0", configs);

    }
}