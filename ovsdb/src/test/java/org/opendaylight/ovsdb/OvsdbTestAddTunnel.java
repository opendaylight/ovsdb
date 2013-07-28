package org.opendaylight.ovsdb;


import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.ovsdb.internal.*;
import org.opendaylight.ovsdb.sal.connection.ConnectionConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.ovsdb.internal.Encapsulation;

public class OvsdbTestAddTunnel {
    private static final Logger logger = LoggerFactory
            .getLogger(OvsdbTestAddTunnel.class);

    @Test
    public void addTunnel() throws Throwable{
        Node.NodeIDType.registerIDType("OVS", String.class);
        NodeConnector.NodeConnectorIDType.registerIDType("OVS", String.class, "OVS");

        ConnectionService connectionService = new ConnectionService();
        connectionService.init();

        String identifier = "TEST";
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

        Map<ConnectionConstants, String> params = new HashMap<ConnectionConstants, String>();
        params.put(ConnectionConstants.ADDRESS, "192.168.56.101");

        Node node = connectionService.connect(identifier, params);
        if(node == null){
            logger.error("Could not connect to ovsdb server");
            return;
        }
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
        configurationService.addTunnel(node, "JUNIT_BRIDGE_TEST",
                "Jtunnel0", tunnelendpoint, tunencap);

    }
}