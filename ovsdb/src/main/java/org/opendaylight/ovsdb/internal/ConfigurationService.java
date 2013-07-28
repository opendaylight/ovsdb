package org.opendaylight.ovsdb.internal;

import java.net.InetAddress;
import java.util.*;

import org.opendaylight.ovsdb.database.OVSBridge;
import org.opendaylight.ovsdb.database.OVSInstance;
import org.opendaylight.ovsdb.sal.configuration.IPluginInNetworkConfigurationService;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.ovsdb.database.Uuid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigurationService implements IPluginInNetworkConfigurationService
{
    private static final Logger logger = LoggerFactory
            .getLogger(ConfigurationService.class);

    IConnectionServiceInternal connectionService;

    void init() {
    }

    /**
     * Function called by the dependency manager when at least one dependency
     * become unsatisfied or when the component is shutting down because for
     * example bundle is being stopped.
     *
     */
    void destroy() {
    }

    /**
     * Function called by dependency manager after "init ()" is called and after
     * the services provided by the class are registered in the service registry
     *
     */
    void start() {
    }

    /**
     * Function called by the dependency manager before the services exported by
     * the component are unregistered, this will be followed by a "destroy ()"
     * calls
     *
     */
    void stop() {
    }

    public void setConnectionServiceInternal(IConnectionServiceInternal connectionService) {
        this.connectionService = connectionService;
    }

    public void unsetConnectionServiceInternal(IConnectionServiceInternal connectionService) {
        if (this.connectionService == connectionService) {
            this.connectionService = null;
        }
    }

    /**
     * Add a new bridge
     * @param node Node serving this configuration service
     * @param bridgeConnectorIdentifier String representation of a Bridge Connector
     * @return Bridge Connector configurations
     */
    @Override
    @SuppressWarnings("unchecked")
    public boolean createBridgeDomain(Node node, String bridgeIdentifier) throws Throwable{
        try{
            if (connectionService == null) {
                logger.error("Couldn't refer to the ConnectionService");
                return false;
            }

            Connection connection = connectionService.getConnection(node);
            String identifier = "TEST";

            if (connection != null) {
                String newBridge = "new_bridge";
                String newInterface = "new_interface";
                String newPort = "new_port";
                String newSwitch = "new_switch";

                Object addSwitchRequest;

                OVSInstance instance = new OVSInstance();
                instance.monitorOVS(connection);

                if(instance.getUuid() != null){
                    List<String> bridgeUuidPair = new ArrayList<String>();
                    bridgeUuidPair.add("named-uuid");
                    bridgeUuidPair.add(newBridge);

                    List<Object> mutation = new ArrayList<Object>();
                    mutation.add("bridges");
                    mutation.add("insert");
                    mutation.add(bridgeUuidPair);

                    List<Object> mutations = new ArrayList<Object>();
                    mutations.add(mutation);

                    List<String> ovsUuidPair = new ArrayList<String>();
                    ovsUuidPair.add("uuid");
                    ovsUuidPair.add(instance.getUuid());

                    List<Object> whereInner = new ArrayList<Object>();
                    whereInner.add("_uuid");
                    whereInner.add("==");
                    whereInner.add(ovsUuidPair);

                    List<Object> where = new ArrayList<Object>();
                    where.add(whereInner);

                    addSwitchRequest = new MutateRequest("Open_vSwitch", where, mutations);
                }
                else{
                    Map<String, Object> vswitchRow = new HashMap<String, Object>();
                    ArrayList<String> bridges = new ArrayList<String>();
                    bridges.add("named-uuid");
                    bridges.add(newBridge);
                    vswitchRow.put("bridges", bridges);
                    addSwitchRequest = new InsertRequest("insert", "Open_vSwitch", newSwitch, vswitchRow);
                }

                Map<String, Object> bridgeRow = new HashMap<String, Object>();
                bridgeRow.put("name", bridgeIdentifier);
                ArrayList<String> ports = new ArrayList<String>();
                ports.add("named-uuid");
                ports.add(newPort);
                bridgeRow.put("ports", ports);
                InsertRequest addBridgeRequest = new InsertRequest("insert", "Bridge", newBridge, bridgeRow);

                Map<String, Object> portRow = new HashMap<String, Object>();
                portRow.put("name", bridgeIdentifier);
                ArrayList<String> interfaces = new ArrayList<String>();
                interfaces.add("named-uuid");
                interfaces.add(newInterface);
                portRow.put("interfaces", interfaces);
                InsertRequest addPortRequest = new InsertRequest("insert", "Port", newPort, portRow);

                Map<String, Object> interfaceRow = new HashMap<String, Object>();
                interfaceRow.put("name", bridgeIdentifier);
                interfaceRow.put("type", "internal");
                InsertRequest addIntfRequest = new InsertRequest("insert", "Interface", newInterface, interfaceRow);

                Object[] params = {"Open_vSwitch", addSwitchRequest, addIntfRequest, addPortRequest, addBridgeRequest};
                OvsdbMessage msg = new OvsdbMessage("transact", params);

                connection.sendMessage(msg);
                connection.readResponse(Uuid[].class);
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public boolean deleteBridgeDomain(Node node, String bridgeIdentifier) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<String> getBridgeDomains(Node node) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean addBridgeDomainConfig(Node node, String bridgeIdentifier, Map<String, String> config) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean removeBridgeDomainConfig(Node node, String bridgeIdentifier, Map<String, String> config) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Map<String, String> getBridgeDomainConfigs(Node node, String bridgeIdentifier) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean createBridgeConnector(Node node, String bridgeConnectorIdentifier) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean deleteBridgeConnector(Node node, String bridgeConnectorIdentifier) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean associateBridgeConnector(Node node, String bridgeIdentifier, String bridgeConnectorIdentifier) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean disassociateBridgeConnector(Node node, String bridgeIdentifier, String bridgeConnectorIdentifier) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean addBridgeConnectorConfig(Node node, String bridgeConnectorIdentifier, Map<String, String> config) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean removeBridgeConnectorConfig(Node node, String bridgeConnectorIdentifier, Map<String, String> config) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Map<String, String> getBridgeConnectorConfigs(Node node, String bridgeConnectorIdentifier) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Create a Port Attached to a Bridge
     * Ex. ovs-vsctl add-port br0 vif0
     * @param node Node serving this configuration service
     * @param bridgeDomainIdentifier String representation of a Bridge Domain
     * @param portIdentifier String representation of a user defined Port Name
     */
    @Override
    @SuppressWarnings("unchecked")
    public boolean addPort(Node node, String bridgeIdentifier, String portIdentifier) throws Throwable{
        try{
            if (connectionService == null) {
                logger.error("Couldn't refer to the ConnectionService");
                return false;
            }
            Connection connection = connectionService.getConnection(node);

            if (connection != null) {
                String newBridge = "new_bridge";
                String newInterface = "new_interface";
                String newPort = "new_port";
                String newSwitch = "new_switch";

                Map<String, OVSBridge> existingBridges = OVSBridge.monitorBridge(connection);

                OVSBridge bridge = existingBridges.get(bridgeIdentifier);

                List<String> portUuidPair = new ArrayList<String>();
                portUuidPair.add("named-uuid");
                portUuidPair.add(newPort);

                List<Object> mutation = new ArrayList<Object>();
                mutation.add("ports");
                mutation.add("insert");
                mutation.add(portUuidPair);
                List<Object> mutations = new ArrayList<Object>();
                mutations.add(mutation);

                List<String> bridgeUuidPair = new ArrayList<String>();
                bridgeUuidPair.add("uuid");
                bridgeUuidPair.add(bridge.getUuid());

                List<Object> whereInner = new ArrayList<Object>();
                whereInner.add("_uuid");
                whereInner.add("==");
                whereInner.add(bridgeUuidPair);

                List<Object> where = new ArrayList<Object>();
                where.add(whereInner);

                MutateRequest mutateBridgeRequest = new MutateRequest("Bridge", where, mutations);

                Map<String, Object> portRow = new HashMap<String, Object>();
                portRow.put("name", portIdentifier);
                ArrayList<String> interfaces = new ArrayList<String>();
                interfaces.add("named-uuid");
                interfaces.add(newInterface);
                portRow.put("interfaces", interfaces);
                InsertRequest addPortRequest = new InsertRequest("insert", "Port", newPort, portRow);

                Map<String, Object> interfaceRow = new HashMap<String, Object>();
                interfaceRow.put("name", portIdentifier);
                InsertRequest addIntfRequest = new InsertRequest("insert", "Interface", newInterface, interfaceRow);

                Object[] params = {"Open_vSwitch", mutateBridgeRequest, addIntfRequest, addPortRequest};
                OvsdbMessage msg = new OvsdbMessage("transact", params);

                connection.sendMessage(msg);
                connection.readResponse(Uuid[].class);
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return true;
    }

    /**
     * Create a Port with a VLAN Tag and it to a Bridge
     * Ex. ovs-vsctl add-port br0 vxlan1 -- set interface vxlan1 type=vxlan options:remote_ip=192.168.1.11
     * @param node Node serving this configuration service
     * @param portIdentifier String representation of a user defined Port Name
     * @param vlanid integer representing the VLAN Tag
     */
    @Override
    @SuppressWarnings("unchecked")
    public boolean addPortVlan(Node node, String bridgeIdentifier, String portIdentifier, int vlanid) throws Throwable{
        try{
            if (connectionService == null) {
                logger.error("Couldn't refer to the ConnectionService");
                return false;
            }
            Connection connection = connectionService.getConnection(node);

            if (connection != null) {
                String newBridge = "new_bridge";
                String newInterface = "new_interface";
                String newPort = "new_port";
                String newSwitch = "new_switch";

                Map<String, OVSBridge> existingBridges = OVSBridge.monitorBridge(connection);

                OVSBridge bridge = existingBridges.get(bridgeIdentifier);

                List<String> portUuidPair = new ArrayList<String>();
                portUuidPair.add("named-uuid");
                portUuidPair.add(newPort);

                List<Object> mutation = new ArrayList<Object>();
                mutation.add("ports");
                mutation.add("insert");
                mutation.add(portUuidPair);
                List<Object> mutations = new ArrayList<Object>();
                mutations.add(mutation);

                List<String> bridgeUuidPair = new ArrayList<String>();
                bridgeUuidPair.add("uuid");
                bridgeUuidPair.add(bridge.getUuid());

                List<Object> whereInner = new ArrayList<Object>();
                whereInner.add("_uuid");
                whereInner.add("==");
                whereInner.add(bridgeUuidPair);

                List<Object> where = new ArrayList<Object>();
                where.add(whereInner);

                MutateRequest mutateBridgeRequest = new MutateRequest("Bridge", where, mutations);

                Map<String, Object> portRow = new HashMap<String, Object>();
                portRow.put("name", portIdentifier);
                portRow.put("tag", vlanid);
                ArrayList<String> interfaces = new ArrayList<String>();
                interfaces.add("named-uuid");
                interfaces.add(newInterface);
                portRow.put("interfaces", interfaces);
                InsertRequest addPortRequest = new InsertRequest("insert", "Port", newPort, portRow);

                Map<String, Object> interfaceRow = new HashMap<String, Object>();
                interfaceRow.put("name", portIdentifier);
                InsertRequest addIntfRequest = new InsertRequest("insert", "Interface", newInterface, interfaceRow);

                Object[] params = {"Open_vSwitch", mutateBridgeRequest, addIntfRequest, addPortRequest};
                OvsdbMessage msg = new OvsdbMessage("transact", params);

                connection.sendMessage(msg);
                connection.readResponse(Uuid[].class);
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return true;
    }

    /**
     * Create an Encapsulated Tunnel Interface and destination Tunnel Endpoint
     * Ex. ovs-vsctl add-port br0 vxlan1 -- set interface vxlan1 type=vxlan options:remote_ip=192.168.1.11
     * @param node Node serving this configuration service
     * @param bridgeDomainIdentifier String representation of a Bridge Domain
     * @param portIdentifier String representation of a user defined Port Name
     * @param tunnelendpoint IP address of the destination Tunnel Endpoint
     * @param tunencap is the tunnel encapsulation options being CAPWAP, GRE or VXLAN
     * The Bridge must already be defined before calling addTunnel.
     */
    @Override
    @SuppressWarnings("unchecked")
    public boolean addTunnel(Node node, String bridgeIdentifier,
        String portidentifier, String tunnelendpoint, String tunencap)
                throws Throwable{
        try{
            if (connectionService == null) {
                logger.error("Couldn't refer to the ConnectionService");
                return false;
            }
            Connection connection = connectionService.getConnection(node);

            if (connection != null) {
                String newBridge = "new_bridge";
                String newInterface = "new_interface";
                String newPort = "new_port";
                String newSwitch = "new_switch";

                Map<String, OVSBridge> existingBridges = OVSBridge.monitorBridge(connection);

                OVSBridge bridge = existingBridges.get(bridgeIdentifier);

                List<String> portUuidPair = new ArrayList<String>();
                portUuidPair.add("named-uuid");
                portUuidPair.add(newPort);

                List<Object> mutation = new ArrayList<Object>();
                mutation.add("ports");
                mutation.add("insert");
                mutation.add(portUuidPair);
                List<Object> mutations = new ArrayList<Object>();
                mutations.add(mutation);

                List<String> bridgeUuidPair = new ArrayList<String>();
                bridgeUuidPair.add("uuid");
                bridgeUuidPair.add(bridge.getUuid());

                List<Object> whereInner = new ArrayList<Object>();
                whereInner.add("_uuid");
                whereInner.add("==");
                whereInner.add(bridgeUuidPair);

                List<Object> where = new ArrayList<Object>();
                where.add(whereInner);

                MutateRequest mutateBridgeRequest = new MutateRequest("Bridge", where, mutations);

                Map<String, Object> portRow = new HashMap<String, Object>();
                portRow.put("name", portidentifier);
                ArrayList<String> interfaces = new ArrayList<String>();
                interfaces.add("named-uuid");
                interfaces.add(newInterface);
                portRow.put("interfaces", interfaces);
                InsertRequest addPortRequest = new InsertRequest("insert", "Port", newPort, portRow);

                Map<String, Object> interfaceRow = new HashMap<String, Object>();
                interfaceRow.put("name", portidentifier);
                interfaceRow.put("type", tunencap);
                ArrayList<Object> intopt = new ArrayList<Object>();
                interfaceRow.put("options", intopt);
                ArrayList<Object> intoptmap = new ArrayList<Object>();
                ArrayList<String> intoptep = new ArrayList<String>();
                intopt.add("map");
                intopt.add(intoptmap);
                intoptmap.add(intoptep);
                intoptep.add("remote_ip");
                intoptep.add(tunnelendpoint);

                InsertRequest addIntfRequest = new InsertRequest("insert", "Interface",
                        newInterface, interfaceRow);

                Object[] params = {"Open_vSwitch", mutateBridgeRequest, addIntfRequest, addPortRequest};
                OvsdbMessage msg = new OvsdbMessage("transact", params);

                connection.sendMessage(msg);
                connection.readResponse(Uuid[].class);
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public Object genericConfigurationEvent(Node node, Map<String, String> config) {
        // TODO Auto-generated method stub
        return null;
    }

  }