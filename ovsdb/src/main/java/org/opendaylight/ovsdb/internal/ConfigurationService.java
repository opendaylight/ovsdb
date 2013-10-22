package org.opendaylight.ovsdb.internal;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.opendaylight.controller.sal.utils.NetUtils;
import org.opendaylight.ovsdb.database.OVSBridge;
import org.opendaylight.ovsdb.database.OVSPort;
import org.opendaylight.ovsdb.database.OVSInstance;
import org.opendaylight.ovsdb.database.OvsdbType;
import org.opendaylight.controller.sal.connection.ConnectionConstants;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.networkconfig.bridgedomain.ConfigConstants;
import org.opendaylight.controller.sal.networkconfig.bridgedomain.IPluginInBridgeDomainConfigService;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigurationService implements IPluginInBridgeDomainConfigService, CommandProvider
{
    private static final Logger logger = LoggerFactory
            .getLogger(ConfigurationService.class);

    IConnectionServiceInternal connectionService;
    boolean forceConnect = false;

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
        registerWithOSGIConsole();
    }

    private void registerWithOSGIConsole() {
        BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass())
                .getBundleContext();
        bundleContext.registerService(CommandProvider.class.getName(), this,
                null);
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

    private Connection getConnection (Node node) {

        Connection connection = connectionService.getConnection(node);
        if (connection == null || connection.getSocket() == null) {
            return null;
        }

        /*
         * This is possible only when the connection is disconnected due to any reason.
         * But, we have to implement ECHO handling else, it results in timeout and the
         * connection being partially closed from the server side and the client Socket
         * seems to be up. Hence forcing the issue for now till we implement the ECHO.
         */
        if (connection.getSocket().isClosed() || forceConnect) {
            String address = connection.getSocket().getInetAddress().getHostAddress();
            Map<ConnectionConstants, String> params = new HashMap<ConnectionConstants, String>();
            params.put(ConnectionConstants.ADDRESS, address);
            params.put(ConnectionConstants.PORT, connection.getSocket().getPort()+"");
            node = connectionService.connect(connection.getIdentifier(), params);
            connection = connectionService.getConnection(node);
        }
        if (connection == null || connection.getSocket() == null || connection.getSocket().isClosed()) {
            return null;
        }
        return connection;
    }
    /**
     * Add a new bridge
     * @param node Node serving this configuration service
     * @param bridgeConnectorIdentifier String representation of a Bridge Connector
     * @return Bridge Connector configurations
     */
    @Override
    public Status createBridgeDomain(Node node, String bridgeIdentifier,
            Map<ConfigConstants, Object> configs) throws Throwable {
        try{
            if (connectionService == null) {
                logger.error("Couldn't refer to the ConnectionService");
                return new Status(StatusCode.NOSERVICE);
            }

            Connection connection = this.getConnection(node);
            if (connection == null || connection.getSocket() == null) {
                return new Status(StatusCode.NOSERVICE, "Connection to ovsdb-server not available");
            }

            if (connection != null) {
                String newBridge = "new_bridge";
                String newInterface = "new_interface";
                String newPort = "new_port";
                String newSwitch = "new_switch";

                Object addSwitchRequest;

                OVSInstance instance = OVSInstance.monitorOVS(connection);

                if(instance != null){
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
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return new Status(StatusCode.SUCCESS);
    }

    /**
     * Create a Port Attached to a Bridge
     * Ex. ovs-vsctl add-port br0 vif0
     * @param node Node serving this configuration service
     * @param bridgeDomainIdentifier String representation of a Bridge Domain
     * @param portIdentifier String representation of a user defined Port Name
     */
    @Override
    public Status addPort(Node node, String bridgeIdentifier, String portIdentifier, Map<ConfigConstants, Object> configs) {
        try{
            if (connectionService == null) {
                logger.error("Couldn't refer to the ConnectionService");
                return new Status(StatusCode.NOSERVICE);
            }
            Connection connection = this.getConnection(node);
            if (connection == null || connection.getSocket() == null) {
                return new Status(StatusCode.NOSERVICE, "Connection to ovsdb-server not available");
            }

            if (connection != null) {
                String newInterface = "new_interface";
                String newPort = "new_port";

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
                String portType = null;
                if (configs != null) {
                    portType = (String)configs.get(ConfigConstants.TYPE);
                    if (portType != null && portType.equalsIgnoreCase(OvsdbType.PortType.VLAN.name())) {
                        try {
                        portRow.put("tag", Integer.parseInt((String)configs.get(ConfigConstants.VLAN)));
                        } catch (Exception e) {
                        }
                    }
                }
                ArrayList<String> interfaces = new ArrayList<String>();
                interfaces.add("named-uuid");
                interfaces.add(newInterface);
                portRow.put("interfaces", interfaces);
                InsertRequest addPortRequest = new InsertRequest("insert", "Port", newPort, portRow);

                Map<String, Object> interfaceRow = new HashMap<String, Object>();
                interfaceRow.put("name", portIdentifier);
                //Tunnel specific

                if (portType != null && portType.equalsIgnoreCase(OvsdbType.PortType.TUNNEL.name())) {
                    interfaceRow.put("type", configs.get(ConfigConstants.TUNNEL_TYPE));
                    ArrayList<Object> intopt = new ArrayList<Object>();
                    interfaceRow.put("options", intopt);
                    ArrayList<Object> intoptmap = new ArrayList<Object>();
                    ArrayList<String> intoptep = new ArrayList<String>();
                    intopt.add("map");
                    intopt.add(intoptmap);
                    intoptmap.add(intoptep);
                    intoptep.add("remote_ip");
                    intoptep.add((String)configs.get(ConfigConstants.DEST_IP));
                }
                InsertRequest addIntfRequest = new InsertRequest("insert", "Interface", newInterface, interfaceRow);

                Object[] params = {"Open_vSwitch", mutateBridgeRequest, addIntfRequest, addPortRequest};
                OvsdbMessage msg = new OvsdbMessage("transact", params);

                connection.sendMessage(msg);
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return new Status(StatusCode.SUCCESS);
    }
    /**
     * Implements the OVS Connection for Managers
     *
     * @param node Node serving this configuration service
     * @param String with IP and connection types
     */
    @SuppressWarnings("unchecked")
    public boolean setManager(Node node, String managerip) throws Throwable{
        try{
            if (connectionService == null) {
                logger.error("Couldn't refer to the ConnectionService");
                return false;
            }
            Connection connection = this.getConnection(node);
            if (connection == null || connection.getSocket() == null) {
                return false;
            }

            if (connection != null) {
                String newmanager = "new_manager";

                OVSInstance instance = OVSInstance.monitorOVS(connection);

                Map ovsoutter = new LinkedHashMap();
                Map ovsinner = new LinkedHashMap();
                ArrayList ovsalist1 = new ArrayList();
                ArrayList ovsalist2 = new ArrayList();
                ArrayList ovsalist3 = new ArrayList();
                ArrayList ovsalist4 = new ArrayList();

                //OVS Table Update
                ovsoutter.put("where", ovsalist1);
                ovsalist1.add(ovsalist2);
                ovsalist2.add("_uuid");
                ovsalist2.add("==");
                ovsalist2.add(ovsalist3);
                ovsalist3.add("uuid");
                ovsalist3.add(instance.getUuid());
                ovsoutter.put("op", "update");
                ovsoutter.put("table", "Open_vSwitch");
                ovsoutter.put("row", ovsinner);
                ovsinner.put("manager_options", ovsalist4);
                ovsalist4.add("named-uuid");
                ovsalist4.add(newmanager);

                Map mgroutside = new LinkedHashMap();
                Map mgrinside = new LinkedHashMap();

                //Manager Table Insert
                mgroutside.put("uuid-name", newmanager);
                mgroutside.put("op", "insert");
                mgroutside.put("table","Manager");
                mgroutside.put("row", mgrinside);
                mgrinside.put("target", managerip);

                Object[] params = {"Open_vSwitch", ovsoutter, mgroutside};
                OvsdbMessage msg = new OvsdbMessage("transact", params);

                connection.sendMessage(msg);

            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public Status addBridgeDomainConfig(Node node, String bridgeIdentifier,
                                        Map<ConfigConstants, Object> configs) {
        try{
            if (connectionService == null) {
                logger.error("Couldn't refer to the ConnectionService");
                return new Status(StatusCode.NOSERVICE);
            }
            Connection connection = this.getConnection(node);
            if (connection == null || connection.getSocket() == null) {
                return new Status(StatusCode.NOSERVICE);
            }
            if (connection != null) {

                Map<String, OVSBridge> existingBridges = OVSBridge.monitorBridge(connection);
                OVSBridge bridge = existingBridges.get(bridgeIdentifier);

                ArrayList totalColumn = new ArrayList();
                Map<String, String> columnPairs = (Map<String, String>) configs.get(ConfigConstants.CUSTOM);
                if (columnPairs == null) {
                    return new Status(StatusCode.NOTACCEPTABLE);
                }
                int num = columnPairs.size();
                ArrayList columnArray = new ArrayList();
                for(String key : columnPairs.keySet())
                {
                    ArrayList arg = new ArrayList<String>();
                    arg.add(key);
                    arg.add(columnPairs.get(key));
                    columnArray.add(arg);
                }

                Map outterbr = new LinkedHashMap();
                Map brcolumn = new LinkedHashMap();
                ArrayList brlist1 = new ArrayList();
                ArrayList brlist2 = new ArrayList();
                ArrayList brlist3 = new ArrayList();
                ArrayList brlist4 = new ArrayList();
                ArrayList brlist5 = new ArrayList();
                //Bridge Table set w/the extIdValue
                outterbr.put("table", "Bridge");
                brlist1.add(brlist2);
                brlist2.add(brlist3);
                brlist3.add("_uuid");
                brlist3.add("==");
                brlist3.add(brlist4);
                brlist4.add("uuid");
                brlist4.add(bridge.getUuid());
                outterbr.put("where", brlist2);
                outterbr.put("op", "update");
                outterbr.put("row", brcolumn);
                brcolumn.put("external_ids", brlist5);
                brlist5.add("map");
                brlist5.add(columnArray);

                Object[] params = {"Open_vSwitch",outterbr};
                OvsdbMessage msg = new OvsdbMessage("transact", params);
                connection.sendMessage(msg);

            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return new Status(StatusCode.SUCCESS);
    }


    @Override
    public Status addPortConfig(Node node, String bridgeIdentifier, String portIdentifier,
            Map<ConfigConstants, Object> configs) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Status deletePort(Node node, String bridgeIdentifier, String portIdentifier) {
        try{
            if (connectionService == null) {
                logger.error("Couldn't refer to the ConnectionService");
                return new Status(StatusCode.NOSERVICE);
            }
            Connection connection = this.getConnection(node);
            if (connection == null || connection.getSocket() == null) {
                return new Status(StatusCode.NOSERVICE, "Connection to ovsdb-server not available");
            }

            if (connection != null) {
                Map<String, OVSPort> existingPorts = OVSPort.monitorPort(connection);
                OVSPort port = existingPorts.get(portIdentifier);
                String retrievedBridgeUuid = port.getBridgeUuid();

                OVSPort bridgeUuid = existingPorts.get(bridgeIdentifier);

                List<String> portUuidPair = new ArrayList<String>();
                portUuidPair.add("uuid");
                portUuidPair.add(port.getUuid());

                List<Object> mutation = new ArrayList<Object>();
                mutation.add("ports");
                mutation.add("delete");
                mutation.add(portUuidPair);
                List<Object> mutations = new ArrayList<Object>();
                mutations.add(mutation);

                List<String> bridgeUuidPair = new ArrayList<String>();
                bridgeUuidPair.add("uuid");
                bridgeUuidPair.add(retrievedBridgeUuid);

                List<Object> whereInner = new ArrayList<Object>();
                whereInner.add("_uuid");
                whereInner.add("==");
                whereInner.add(bridgeUuidPair);

                List<Object> where = new ArrayList<Object>();
                where.add(whereInner);

                MutateRequest mutateBridgeRequest = new MutateRequest("Bridge", where, mutations);

                Object[] params = {"Open_vSwitch", mutateBridgeRequest};
                OvsdbMessage msg = new OvsdbMessage("transact", params);

                connection.sendMessage(msg);

            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return new Status(StatusCode.SUCCESS);
    }

    @Override
    public Node getBridgeDomainNode(Node node, String bridgeIdentifier) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<ConfigConstants, Object> getPortConfigs(Node node, String bridgeIdentifier,
            String portIdentifier) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Status removeBridgeDomainConfig(Node node, String bridgeIdentifier,
            Map<ConfigConstants, Object> configs) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Status removePortConfig(Node node, String bridgeIdentifier, String portIdentifier,
            Map<ConfigConstants, Object> configs) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Status deleteBridgeDomain(Node node, String bridgeIdentifier) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<ConfigConstants, Object> getBridgeDomainConfigs(Node node, String bridgeIdentifier) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<String> getBridgeDomains(Node node) {

        Connection connection = connectionService.getConnection(node);
        Map<String, OVSBridge> existingBridges = OVSBridge.monitorBridge(connection);
        List<String> bridgeDomains = new ArrayList<String>(existingBridges.keySet());
        return bridgeDomains;
    }

    public Boolean setBridgeOFController(Node node, String bridgeIdentifier, Map<ConfigConstants, Object> configs) {

        try{
            if (connectionService == null) {
                logger.error("Couldn't refer to the ConnectionService");
                return false;
            }
            Connection connection = this.getConnection(node);
            if (connection == null || connection.getSocket() == null) {
                return false;
            }
            if (connection != null) {
                String controllerAddr = (String)configs.get(ConfigConstants.DEST_IP);
                String ofPort = (String)configs.get(ConfigConstants.CUSTOM);

                try {
                    controllerAddr = InetAddress.getByName(controllerAddr).getHostAddress();
                }  catch (Exception e) {
                    e.printStackTrace();
                    logger.error("Invalid Address <" + controllerAddr +
                            "> Please enter valid IP-Address");
                    return false;
                }
                if ((controllerAddr != null) && (controllerAddr.trim().length() > 0) &&
                        !NetUtils.isIPv4AddressValid(controllerAddr)) {
                    logger.debug("Invalid ip address " + controllerAddr);
                }

                Map<String, OVSBridge> existingBridges = OVSBridge.monitorBridge(connection);
                OVSBridge bridge = existingBridges.get(bridgeIdentifier);

                if (bridge != null) {
                    Map ctrlouter = new LinkedHashMap();
                    Map ctrlinner = new LinkedHashMap();
                    Map brouter = new LinkedHashMap();
                    Map brinner = new LinkedHashMap();
                    ArrayList bralist1 = new ArrayList();
                    ArrayList bralist2 = new ArrayList();
                    ArrayList bralist3 = new ArrayList();
                    ArrayList bralist4 = new ArrayList();

                    //Controller Table Insert
                    ctrlouter.put("table", "Controller");
                    ctrlouter.put("uuid-name", "row" + bridgeIdentifier);
                    ctrlouter.put("op", "insert");
                    ctrlinner.put("target", "tcp:" + controllerAddr + ":" + ofPort);
                    ctrlouter.put("row", ctrlinner);

                    //Bridge Table Update
                    brouter.put("table","Bridge");
                    bralist1.add(bralist2);
                    bralist2.add("_uuid");
                    bralist2.add("==");
                    bralist2.add(bralist3);
                    bralist3.add("uuid");
                    bralist3.add(bridge.getUuid());
                    brouter.put("where",bralist1);
                    brouter.put("op","update");
                    brouter.put("row",brinner);
                    bralist4.add("named-uuid");
                    bralist4.add("row" + bridgeIdentifier);
                    brinner.put("controller",bralist4);

                    Object[] params = {"Open_vSwitch", ctrlouter, brouter};
                    OvsdbMessage msg = new OvsdbMessage("transact", params);

                    //Send the Method and Param
                    connection.sendMessage(msg);
                }
                else {
                    logger.error(bridgeIdentifier +
                            " Does not exist on " + connection.getIdentifier());
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public NodeConnector getNodeConnector(Node arg0, String arg1, String arg2) {
        return null;
    }

    public void _ovsconnect (CommandInterpreter ci) {
        String bridgeName = ci.nextArgument();
        if (bridgeName == null) {
            ci.println("Please enter Bridge Name");
            return;
        }

        String ovsdbserver = ci.nextArgument();
        if (ovsdbserver == null) {
            ci.println("Please enter valid IP-Address");
            return;
        }
        try {
            InetAddress.getByName(ovsdbserver);
        }  catch (Exception e) {
            e.printStackTrace();
            ci.println("Please enter valid IP-Address");
            return;
        }
        String port = ci.nextArgument();
        if (port == null) {
            port = "6634";
        }

        ci.println("connecting to ovsdb server : "+ovsdbserver+":"+port+" ... ");
        Map<ConnectionConstants, String> params = new HashMap<ConnectionConstants, String>();
        params.put(ConnectionConstants.ADDRESS, ovsdbserver);
        params.put(ConnectionConstants.PORT, port);
        Node node = connectionService.connect(bridgeName, params);
        if (node != null) ci.println("Node Name: "+node.toString());
        else ci.println("Could not connect to Node");
    }

    public void _addBridge (CommandInterpreter ci) {
        String nodeName = ci.nextArgument();
        if (nodeName == null) {
            ci.println("Please enter Node Name");
            return;
        }

        String bridgeName = ci.nextArgument();
        if (bridgeName == null) {
            ci.println("Please enter Bridge Name");
            return;
        }
        Status status;
        try {
            status = this.createBridgeDomain(Node.fromString(nodeName), bridgeName, null);
            ci.println("Bridge creation status : "+status.toString());
        } catch (Throwable e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ci.println("Failed to create Bridge "+bridgeName);
        }
    }

    public void _addPort (CommandInterpreter ci) {
        String nodeName = ci.nextArgument();
        if (nodeName == null) {
            ci.println("Please enter Node Name");
            return;
        }

        String bridgeName = ci.nextArgument();
        if (bridgeName == null) {
            ci.println("Please enter Bridge Name");
            return;
        }

        String portName = ci.nextArgument();
        if (portName == null) {
            ci.println("Please enter Port Name");
            return;
        }

        Status status;
        try {
            status = this.addPort(Node.fromString(nodeName), bridgeName, portName, null);
            ci.println("Port creation status : "+status.toString());
        } catch (Throwable e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ci.println("Failed to create Port "+portName+" in Bridge "+bridgeName);
        }
    }

    public void _addPortVlan (CommandInterpreter ci) {
        String nodeName = ci.nextArgument();
        if (nodeName == null) {
            ci.println("Please enter Node Name");
            return;
        }

        String bridgeName = ci.nextArgument();
        if (bridgeName == null) {
            ci.println("Please enter Bridge Name");
            return;
        }

        String portName = ci.nextArgument();
        if (portName == null) {
            ci.println("Please enter Port Name");
            return;
        }

        String vlan = ci.nextArgument();
        if (vlan == null) {
            ci.println("Please enter Valid Vlan");
            return;
        } else {
            try {
            Integer.parseInt(vlan);
            } catch (Exception e) {
                ci.println("Please enter Valid Vlan");
                return;
            }
        }

        Map<ConfigConstants, Object> configs = new HashMap<ConfigConstants, Object>();
        configs.put(ConfigConstants.TYPE, "VLAN");
        configs.put(ConfigConstants.VLAN, vlan);

        Status status;
        try {
            status = this.addPort(Node.fromString(nodeName), bridgeName, portName, configs);
            ci.println("Port creation status : "+status.toString());
        } catch (Throwable e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ci.println("Failed to create Port "+portName+" in Bridge "+bridgeName);
        }
    }

    public void _addTunnel (CommandInterpreter ci) {
        String nodeName = ci.nextArgument();
        if (nodeName == null) {
            ci.println("Please enter Node Name");
            return;
        }

        String bridgeName = ci.nextArgument();
        if (bridgeName == null) {
            ci.println("Please enter Bridge Name");
            return;
        }

        String portName = ci.nextArgument();
        if (portName == null) {
            ci.println("Please enter Port Name");
            return;
        }

        String tunnelType = ci.nextArgument();
        if (tunnelType == null) {
            ci.println("Please enter Tunnel Type");
            return;
        }

        String remoteIp = ci.nextArgument();
        if (remoteIp == null) {
            ci.println("Please enter valid Remote IP Address");
            return;
        }

        try {
            InetAddress.getByName(remoteIp);
        }  catch (Exception e) {
            e.printStackTrace();
            ci.println("Please enter valid Remote IP Address");
            return;
        }

        Map<ConfigConstants, Object> configs = new HashMap<ConfigConstants, Object>();
        configs.put(ConfigConstants.TYPE, "TUNNEL");
        configs.put(ConfigConstants.TUNNEL_TYPE, tunnelType);
        configs.put(ConfigConstants.DEST_IP, remoteIp);

        Status status;
        try {
            status = this.addPort(Node.fromString(nodeName), bridgeName, portName, configs);
            ci.println("Port creation status : "+status.toString());
        } catch (Throwable e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ci.println("Failed to create Port "+portName+" in Bridge "+bridgeName);
        }
    }

    public void _forceConnect (CommandInterpreter ci) {
        String force = ci.nextArgument();
        if (force.equalsIgnoreCase("YES")) forceConnect = true;
        else if (force.equalsIgnoreCase("NO")) forceConnect = false;
        else ci.println("Please enter YES or NO.");
        ci.println("Current ForceConnect State : "+forceConnect);
        return;
    }

    @Override
    public String getHelp() {
        StringBuffer help = new StringBuffer();
        help.append("---OVSDB CLI---\n");
        help.append("\t ovsconnect <ConnectionName> <ip-address>                        - Connect to OVSDB\n");
        help.append("\t addBridge <Node> <BridgeName>                                   - Add Bridge\n");
        help.append("\t addPort <Node> <BridgeName> <PortName>                          - Add Port\n");
        help.append("\t addPortVlan <Node> <BridgeName> <PortName> <vlan>               - Add Port, Vlan\n");
        help.append("\t addTunnel <Node> <Bridge> <Port> <tunnel-type> <remote-ip>      - Add Tunnel\n");
        help.append("\t forceConnect <yes|no>   - Force a new OVSDB Connection for every command (Workaround)");
        return help.toString();
    }
}
