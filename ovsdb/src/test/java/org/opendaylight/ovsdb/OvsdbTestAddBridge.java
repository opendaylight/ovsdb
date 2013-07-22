package org.opendaylight.ovsdb;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.ovsdb.database.Uuid;
import org.opendaylight.ovsdb.internal.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsdbTestAddBridge {
    private static final Logger logger = LoggerFactory
            .getLogger(OvsdbTestAddBridge.class);
    @Test
    public void addBridge() throws Throwable{
        Node.NodeIDType.registerIDType("OVS", String.class);
        NodeConnector.NodeConnectorIDType.registerIDType("OVS", String.class, "OVS");
        String identifier = "TEST";
        InetAddress address = InetAddress.getByName("172.16.3.196");
        Connection connection = OvsdbIO.connect(identifier, address);
        if (connection != null) {
            String newBridge = "new_bridge";
            String newInterface = "new_interface";
            String newPort = "new_port";
            String newSwitch = "new_switch";

            Map<String, Object> bridgeRow = new HashMap<String, Object>();
            bridgeRow.put("name", "br1");
            ArrayList<String> ports = new ArrayList<String>();
            ports.add("named-uuid");
            ports.add(newPort);
            bridgeRow.put("ports", ports);
            InsertRequest addBridgeRequest = new InsertRequest("insert", "Bridge", newBridge, bridgeRow);

            Map<String, Object> portRow = new HashMap<String, Object>();
            portRow.put("name", "br1");
            ArrayList<String> interfaces = new ArrayList<String>();
            interfaces.add("named-uuid");
            interfaces.add(newInterface);
            portRow.put("interfaces", interfaces);
            InsertRequest addPortRequest = new InsertRequest("insert", "Port", newPort, portRow);

            Map<String, Object> interfaceRow = new HashMap<String, Object>();
            interfaceRow.put("name", "br1");
            interfaceRow.put("type", "internal");
            InsertRequest addIntfRequest = new InsertRequest("insert", "Interface", newInterface, interfaceRow);

            Map<String, Object> vswitchRow = new HashMap<String, Object>();
            ArrayList<String> bridges = new ArrayList<String>();
            bridges.add("named-uuid");
            bridges.add(newBridge);
            vswitchRow.put("bridges", bridges);
            InsertRequest addSwitchRequest = new InsertRequest("insert", "Open_vSwitch", newSwitch, vswitchRow);

            Object[] params = {"Open_vSwitch", addSwitchRequest, addIntfRequest, addPortRequest, addBridgeRequest};
            OvsdbMessage msg = new OvsdbMessage("transact", params);
            connection.sendMessage(msg);
            connection.readResponse(Uuid[].class);

        }
    }

    public void addPort() throws Throwable{
        InetAddress address = InetAddress.getByName("172.16.3.169");
        Connection connection = OvsdbIO.connect("add_port", address);
        if(connection != null){
            String newPort = "new_port";
            String newIntf = "new_interface";

            Map<String, Object> portRow = new HashMap<String, Object>();
            portRow.put("name", "vnic0");
            ArrayList<String> interfaces = new ArrayList<String>();
            interfaces.add("named-uuid");
            interfaces.add(newIntf);
            portRow.put("interfaces", interfaces);
            InsertRequest addPortRequest = new InsertRequest("insert", "Port", newPort, portRow);

            Map<String,Object> intfRow = new HashMap<String, Object>();
            intfRow.put("name", "vnic0");
            InsertRequest addIntfRequest = new InsertRequest("insert", "Interface", newIntf, intfRow);


        }
    }
}
