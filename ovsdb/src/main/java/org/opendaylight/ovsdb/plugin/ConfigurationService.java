package org.opendaylight.ovsdb.plugin;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.*;

import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.opendaylight.ovsdb.lib.database.OVSInstance;
import org.opendaylight.ovsdb.lib.database.OvsdbType;
import org.opendaylight.ovsdb.lib.message.TransactBuilder;
import org.opendaylight.ovsdb.lib.message.operations.InsertOperation;
import org.opendaylight.ovsdb.lib.message.operations.MutateOperation;
import org.opendaylight.ovsdb.lib.message.operations.Operation;
import org.opendaylight.ovsdb.lib.message.operations.OperationResult;
import org.opendaylight.ovsdb.lib.notation.Condition;
import org.opendaylight.ovsdb.lib.notation.Function;
import org.opendaylight.ovsdb.lib.notation.Mutation;
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.notation.OvsDBMap;
import org.opendaylight.ovsdb.lib.notation.OvsDBSet;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.table.Bridge;
import org.opendaylight.ovsdb.lib.table.Controller;
import org.opendaylight.ovsdb.lib.table.Interface;
import org.opendaylight.ovsdb.lib.table.Open_vSwitch;
import org.opendaylight.ovsdb.lib.table.Port;
import org.opendaylight.ovsdb.lib.table.internal.Table;
import org.opendaylight.controller.clustering.services.IClusterGlobalServices;
import org.opendaylight.controller.sal.connection.ConnectionConstants;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.networkconfig.bridgedomain.ConfigConstants;
import org.opendaylight.controller.sal.networkconfig.bridgedomain.IPluginInBridgeDomainConfigService;
import org.opendaylight.controller.sal.utils.NetUtils;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ListenableFuture;

public class ConfigurationService implements IPluginInBridgeDomainConfigService, CommandProvider
{
    private static final Logger logger = LoggerFactory
            .getLogger(ConfigurationService.class);

    IConnectionServiceInternal connectionService;
    InventoryServiceInternal inventoryServiceInternal;
    private IClusterGlobalServices clusterServices;
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

    public void setInventoryServiceInternal(InventoryServiceInternal inventoryServiceInternal) {
        this.inventoryServiceInternal = inventoryServiceInternal;
    }

    public void unsetInventoryServiceInternal(InventoryServiceInternal inventoryServiceInternal) {
        if (this.inventoryServiceInternal == inventoryServiceInternal) {
            this.inventoryServiceInternal = null;
        }
    }

    public void setClusterServices(IClusterGlobalServices i) {
        this.clusterServices = i;
    }

    public void unsetClusterServices(IClusterGlobalServices i) {
        if (this.clusterServices == i) {
            this.clusterServices = null;
        }
    }

    private Connection getConnection (Node node) {
        Connection connection = connectionService.getConnection(node);
        if (connection == null || !connection.getChannel().isActive()) {
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
            if (connection == null) {
                return new Status(StatusCode.NOSERVICE, "Connection to ovsdb-server not available");
            }

            Map<String, Table<?>> ovsTable = inventoryServiceInternal.getTableCache(node, Open_vSwitch.NAME.getName());
            String newBridge = "new_bridge";
            String newInterface = "new_interface";
            String newPort = "new_port";
            String newSwitch = "new_switch";

            Operation addSwitchRequest = null;

            if(ovsTable != null){
                String ovsTableUUID = (String) ovsTable.keySet().toArray()[0];
                UUID bridgeUuidPair = new UUID(newBridge);
                Mutation bm = new Mutation("bridges", Mutator.INSERT, bridgeUuidPair);
                List<Mutation> mutations = new ArrayList<Mutation>();
                mutations.add(bm);

                UUID uuid = new UUID(ovsTableUUID);
                Condition condition = new Condition("_uuid", Function.EQUALS, uuid);
                List<Condition> where = new ArrayList<Condition>();
                where.add(condition);
                addSwitchRequest = new MutateOperation(Open_vSwitch.NAME.getName(), where, mutations);
            }
            else{
                Open_vSwitch ovsTableRow = new Open_vSwitch();
                OvsDBSet<UUID> bridges = new OvsDBSet<UUID>();
                UUID bridgeUuidPair = new UUID(newBridge);
                bridges.add(bridgeUuidPair);
                ovsTableRow.setBridges(bridges);
                addSwitchRequest = new InsertOperation(Open_vSwitch.NAME.getName(), newSwitch, ovsTableRow);
            }

            Bridge bridgeRow = new Bridge();
            bridgeRow.setName(bridgeIdentifier);
            OvsDBSet<UUID> ports = new OvsDBSet<UUID>();
            UUID port = new UUID(newPort);
            ports.add(port);
            bridgeRow.setPorts(ports);
            InsertOperation addBridgeRequest = new InsertOperation(Bridge.NAME.getName(), newBridge, bridgeRow);

            Port portRow = new Port();
            portRow.setName(bridgeIdentifier);
            OvsDBSet<UUID> interfaces = new OvsDBSet<UUID>();
            UUID interfaceid = new UUID(newInterface);
            interfaces.add(interfaceid);
            portRow.setInterfaces(interfaces);
            InsertOperation addPortRequest = new InsertOperation(Port.NAME.getName(), newPort, portRow);

            Interface interfaceRow = new Interface();
            interfaceRow.setName(bridgeIdentifier);
            interfaceRow.setType("internal");
            InsertOperation addIntfRequest = new InsertOperation(Interface.NAME.getName(), newInterface, interfaceRow);

            TransactBuilder transaction = new TransactBuilder();
            transaction.addOperations(new ArrayList<Operation>(
                                      Arrays.asList(addSwitchRequest, addIntfRequest, addPortRequest, addBridgeRequest)));

            ListenableFuture<List<OperationResult>> transResponse = connection.getRpc().transact(transaction);
            List<OperationResult> tr = transResponse.get();
            List<Operation> requests = transaction.getRequests();
            Status status = new Status(StatusCode.SUCCESS);
            for (int i = 0; i < tr.size() ; i++) {
                if (i < requests.size()) requests.get(i).setResult(tr.get(i));
                if (tr.get(i).getError() != null && tr.get(i).getError().trim().length() > 0) {
                    OperationResult result = tr.get(i);
                    status = new Status(StatusCode.BADREQUEST, result.getError() + " : " + result.getDetails());
                }
            }

            if (tr.size() > requests.size()) {
                OperationResult result = tr.get(tr.size()-1);
                logger.error("Error creating Bridge : {}\n Error : {}\n Details : {}", bridgeIdentifier,
                                                                                       result.getError(),
                                                                                       result.getDetails());
                status = new Status(StatusCode.BADREQUEST, result.getError() + " : " + result.getDetails());
            }
            if (status.isSuccess()) {
                setBridgeOFController(node, bridgeIdentifier);
            }
            return status;
        } catch(Exception e){
            e.printStackTrace();
        }
        return new Status(StatusCode.INTERNALERROR);
    }

    /**
     * Create a Port Attached to a Bridge
     * Ex. ovs-vsctl add-port br0 vif0
     * @param node Node serving this configuration service
     * @param bridgeDomainIdentifier String representation of a Bridge Domain
     * @param portIdentifier String representation of a user defined Port Name
     */
    @Override
    public Status addPort(Node node, String bridgeIdentifier, String portIdentifier,
                          Map<ConfigConstants, Object> configs) {
        try{
            if (connectionService == null) {
                logger.error("Couldn't refer to the ConnectionService");
                return new Status(StatusCode.NOSERVICE);
            }
            Connection connection = this.getConnection(node);
            if (connection == null) {
                return new Status(StatusCode.NOSERVICE, "Connection to ovsdb-server not available");
            }
            if (connection != null) {
                Map<String, Table<?>> brTable = inventoryServiceInternal.getTableCache(node, Bridge.NAME.getName());
                String newBridge = "new_bridge";
                String newInterface = "new_interface";
                String newPort = "new_port";

                if(brTable != null){
                    Operation addBrMutRequest = null;
                    String brUuid = null;
                    for (String uuid : brTable.keySet()) {
                        Bridge bridge = (Bridge) brTable.get(uuid);
                        if (bridge.getName().contains(bridgeIdentifier)) {
                            brUuid = uuid;
                        }
                    }

                    UUID brUuidPair = new UUID(newPort);
                    Mutation bm = new Mutation("ports", Mutator.INSERT, brUuidPair);
                    List<Mutation> mutations = new ArrayList<Mutation>();
                    mutations.add(bm);

                    UUID uuid = new UUID(brUuid);
                    Condition condition = new Condition("_uuid", Function.EQUALS, uuid);
                    List<Condition> where = new ArrayList<Condition>();
                    where.add(condition);
                    addBrMutRequest = new MutateOperation(Bridge.NAME.getName(), where, mutations);

                    OvsDBMap<String, String> options = null;
                    String type = null;
                    OvsDBSet<BigInteger> tags = null;
                    if (configs != null) {
                        type = (String) configs.get(ConfigConstants.TYPE);
                        Map<String, String> customConfigs = (Map<String, String>) configs.get(ConfigConstants.CUSTOM);
                        if (customConfigs != null) {
                            options = new OvsDBMap<String, String>();
                            for (String customConfig : customConfigs.keySet()) {
                                options.put(customConfig, customConfigs.get(customConfig));
                            }
                        }
                    }

                    Interface interfaceRow = new Interface();
                    interfaceRow.setName(portIdentifier);

                    if (type != null) {
                        if (type.equalsIgnoreCase(OvsdbType.PortType.TUNNEL.name())) {
                            interfaceRow.setType((String)configs.get(ConfigConstants.TUNNEL_TYPE));
                            if (options == null) options = new OvsDBMap<String, String>();
                            options.put("remote_ip", (String)configs.get(ConfigConstants.DEST_IP));
                        } else if (type.equalsIgnoreCase(OvsdbType.PortType.VLAN.name())) {
                            tags = new OvsDBSet<BigInteger>();
                            tags.add(BigInteger.valueOf(Integer.parseInt((String)configs.get(ConfigConstants.VLAN))));
                        } else if (type.equalsIgnoreCase(OvsdbType.PortType.PATCH.name())) {
                            interfaceRow.setType(type.toLowerCase());
                        }
                    }
                    if (options != null) {
                        interfaceRow.setOptions(options);
                    }

                    InsertOperation addIntfRequest = new InsertOperation(Interface.NAME.getName(),
                            newInterface, interfaceRow);

                    Port portRow = new Port();
                    portRow.setName(portIdentifier);
                    if (tags != null) portRow.setTag(tags);
                    OvsDBSet<UUID> interfaces = new OvsDBSet<UUID>();
                    UUID interfaceid = new UUID(newInterface);
                    interfaces.add(interfaceid);
                    portRow.setInterfaces(interfaces);
                    InsertOperation addPortRequest = new InsertOperation(Port.NAME.getName(), newPort, portRow);

                    TransactBuilder transaction = new TransactBuilder();
                    transaction.addOperations(new ArrayList<Operation>
                            (Arrays.asList(addBrMutRequest, addPortRequest, addIntfRequest)));

                    ListenableFuture<List<OperationResult>> transResponse = connection.getRpc().transact(transaction);
                    List<OperationResult> tr = transResponse.get();
                    List<Operation> requests = transaction.getRequests();
                    Status status = new Status(StatusCode.SUCCESS);
                    for (int i = 0; i < tr.size() ; i++) {
                        if (i < requests.size()) requests.get(i).setResult(tr.get(i));
                        if (tr.get(i).getError() != null && tr.get(i).getError().trim().length() > 0) {
                            OperationResult result = tr.get(i);
                            status = new Status(StatusCode.BADREQUEST, result.getError() + " : " + result.getDetails());
                        }
                    }

                    if (tr.size() > requests.size()) {
                        OperationResult result = tr.get(tr.size()-1);
                        logger.error("Error creating Bridge : {}\n Error : {}\n Details : {}", bridgeIdentifier,
                                result.getError(),
                                result.getDetails());
                        status = new Status(StatusCode.BADREQUEST, result.getError() + " : " + result.getDetails());
                    }
                    return status;
                }
                return new Status(StatusCode.INTERNALERROR);
            }
        } catch(Exception e){
            e.printStackTrace();
        }
        return new Status(StatusCode.INTERNALERROR);
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
            if (connection == null) {
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

                //connection.sendMessage(msg);

            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public Status addBridgeDomainConfig(Node node, String bridgeIdentfier,
            Map<ConfigConstants, Object> configs) {
        String mgmt = (String)configs.get(ConfigConstants.MGMT);
        if (mgmt != null) {
            try {
                if (setManager(node, mgmt)) return new Status(StatusCode.SUCCESS);
            } catch (Throwable e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return new Status(StatusCode.INTERNALERROR);
            }
        }
        return new Status(StatusCode.BADREQUEST);
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
                if (connection == null) {
                    return new Status(StatusCode.NOSERVICE, "Connection to ovsdb-server not available");
                }

                Map<String, Table<?>> brTable = inventoryServiceInternal.getTableCache(node, Bridge.NAME.getName());
                Map<String, Table<?>> portTable = inventoryServiceInternal.getTableCache(node, Port.NAME.getName());
                Operation delPortRequest = null;
                String brUuid = null;
                String portUuid = null;
                if(brTable != null){
                    for (String uuid : brTable.keySet()) {
                        Bridge bridge = (Bridge) brTable.get(uuid);
                        if (bridge.getName().contains(bridgeIdentifier)) {
                            brUuid = uuid;
                        }
                    }
                }
            if(portTable != null){
                for (String uuid : portTable.keySet()) {
                    Port port = (Port) portTable.get(uuid);
                    if (port.getName().contains(portIdentifier)) {
                        portUuid = uuid;
                    }
                }
            }

            UUID portUuidPair = new UUID(portUuid);
            Mutation bm = new Mutation("ports", Mutator.DELETE, portUuidPair);
            List<Mutation> mutations = new ArrayList<Mutation>();
            mutations.add(bm);

            UUID uuid = new UUID(brUuid);
            Condition condition = new Condition("_uuid", Function.EQUALS, uuid);
            List<Condition> where = new ArrayList<Condition>();
            where.add(condition);
            delPortRequest = new MutateOperation(Bridge.NAME.getName(), where, mutations);

            TransactBuilder transaction = new TransactBuilder();
            transaction.addOperations(new ArrayList<Operation>(Arrays.asList(delPortRequest)));

            ListenableFuture<List<OperationResult>> transResponse = connection.getRpc().transact(transaction);
            List<OperationResult> tr = transResponse.get();
            List<Operation> requests = transaction.getRequests();
            Status status = new Status(StatusCode.SUCCESS);
            for (int i = 0; i < tr.size() ; i++) {
                if (i < requests.size()) requests.get(i).setResult(tr.get(i));
                if (tr.get(i).getError() != null && tr.get(i).getError().trim().length() > 0) {
                    OperationResult result = tr.get(i);
                    status = new Status(StatusCode.BADREQUEST, result.getError() + " : " + result.getDetails());
                }
            }

            if (tr.size() > requests.size()) {
                OperationResult result = tr.get(tr.size()-1);
                logger.error("Error creating Bridge : {}\n Error : {}\n Details : {}", bridgeIdentifier,
                        result.getError(),
                        result.getDetails());
                status = new Status(StatusCode.BADREQUEST, result.getError() + " : " + result.getDetails());
            }
            return status;
        } catch(Exception e){
            e.printStackTrace();
        }
        return new Status(StatusCode.INTERNALERROR);
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

        try {
            if (connectionService == null) {
                logger.error("Couldn't refer to the ConnectionService");
                return new Status(StatusCode.NOSERVICE);
            }
            Connection connection = this.getConnection(node);
            if (connection == null) {
                return new Status(StatusCode.NOSERVICE, "Connection to ovsdb-server not available");
            }
            Map<String, Table<?>> OvsTable = inventoryServiceInternal.getTableCache(node, Open_vSwitch.NAME.getName());
            Map<String, Table<?>> brTable = inventoryServiceInternal.getTableCache(node, Bridge.NAME.getName());
            Operation delBrRequest = null;
            String ovsUuid = null;
            String brUuid = null;

            if (brTable != null) {
                for (String uuid : brTable.keySet()) {
                    Bridge bridge = (Bridge) brTable.get(uuid);
                    if (bridge.getName().contains(bridgeIdentifier)) {
                        brUuid = uuid;
                    }
                }
            }
            if (OvsTable != null) {
                for (String uuid : OvsTable.keySet()) {
                    Open_vSwitch open_vSwitch = (Open_vSwitch) OvsTable.get(uuid);
                    ovsUuid = uuid;
                }
            }
            UUID bridgeUuidPair = new UUID(brUuid);
            Mutation bm = new Mutation("bridges", Mutator.DELETE, bridgeUuidPair);
            List<Mutation> mutations = new ArrayList<Mutation>();
            mutations.add(bm);

            UUID uuid = new UUID(ovsUuid);
            Condition condition = new Condition("_uuid", Function.EQUALS, uuid);
            List<Condition> where = new ArrayList<Condition>();
            where.add(condition);
            delBrRequest = new MutateOperation(Open_vSwitch.NAME.getName(), where, mutations);

            TransactBuilder transaction = new TransactBuilder();
            transaction.addOperations(new ArrayList<Operation>(Arrays.asList(delBrRequest)));

            ListenableFuture<List<OperationResult>> transResponse = connection.getRpc().transact(transaction);
            List<OperationResult> tr = transResponse.get();
            List<Operation> requests = transaction.getRequests();
            Status status = new Status(StatusCode.SUCCESS);
            for (int i = 0; i < tr.size(); i++) {
                if (i < requests.size()) requests.get(i).setResult(tr.get(i));
                if (tr.get(i) != null && tr.get(i).getError() != null && tr.get(i).getError().trim().length() > 0) {
                    OperationResult result = tr.get(i);
                    status = new Status(StatusCode.BADREQUEST, result.getError() + " : " + result.getDetails());
                }
            }

            if (tr.size() > requests.size()) {
                OperationResult result = tr.get(tr.size() - 1);
                logger.error("Error creating Bridge : {}\n Error : {}\n Details : {}",
                        bridgeIdentifier, result.getError(), result.getDetails());
                status = new Status(StatusCode.BADREQUEST, result.getError() + " : " + result.getDetails());
            }
            return status;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new Status(StatusCode.INTERNALERROR);
    }

    @Override
    public Map<ConfigConstants, Object> getBridgeDomainConfigs(Node node, String bridgeIdentifier) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<String> getBridgeDomains(Node node) {
        List<String> brlist = new ArrayList<String>();
        Map<String, Table<?>> brTableCache = inventoryServiceInternal.getTableCache(node, Bridge.NAME.getName());
        if(brTableCache != null){
            for (String uuid : brTableCache.keySet()) {
                Bridge bridge = (Bridge) brTableCache.get(uuid);
                brlist.add(bridge.getName());
            }
        }
        return brlist;
    }

    @Override
    public NodeConnector getNodeConnector(Node arg0, String arg1, String arg2) {
        return null;
    }

    private short getControllerOFPort() {
        Short defaultOpenFlowPort = 6633;
        Short openFlowPort = defaultOpenFlowPort;
        String portString = System.getProperty("of.listenPort");
        if (portString != null) {
            try {
                openFlowPort = Short.decode(portString).shortValue();
            } catch (NumberFormatException e) {
                logger.warn("Invalid port:{}, use default({})", portString,
                        openFlowPort);
            }
        }
        return openFlowPort;
    }

    private List<InetAddress> getControllerIPAddresses() {
        List<InetAddress> controllers = null;
        if (clusterServices != null) {
            controllers = clusterServices.getClusteredControllers();
            if (controllers != null && controllers.size() > 0) {
                if (controllers.size() == 1) {
                    InetAddress controller = controllers.get(0);
                    if (!controller.equals(InetAddress.getLoopbackAddress())) {
                        return controllers;
                    }
                } else {
                    return controllers;
                }
            }
        }

        controllers = new ArrayList<InetAddress>();
        InetAddress controllerIP;
        Enumeration<NetworkInterface> nets;
        try {
            nets = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface netint : Collections.list(nets)) {
                Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
                for (InetAddress inetAddress : Collections.list(inetAddresses)) {
                    if (!inetAddress.isLoopbackAddress() &&
                            NetUtils.isIPv4AddressValid(inetAddress.getHostAddress())) {
                        controllers.add(inetAddress);
                    }
                }
            }
        } catch (SocketException e) {
            controllers.add(InetAddress.getLoopbackAddress());
        }
        return controllers;
    }

    public Boolean setBridgeOFController(Node node, String bridgeIdentifier) {
        try{
            if (connectionService == null) {
                logger.error("Couldn't refer to the ConnectionService");
                return false;
            }
            Connection connection = this.getConnection(node);
            if (connection == null) {
                return false;
            }

            if (connection != null) {
                List<InetAddress> ofControllerAddrs = getControllerIPAddresses();
                short ofControllerPort = getControllerOFPort();
                OvsDBSet<UUID> controllerUUIDs = new OvsDBSet<UUID>();
                List<Operation> controllerInsertOperations = new ArrayList<Operation>();
                Map<String, Table<?>> controllerCache = inventoryServiceInternal.getTableCache(node, Controller.NAME.getName());

                int count = 0;
                for (InetAddress ofControllerAddress : ofControllerAddrs) {
                    String cntrlUuid = null;
                    String newController = "tcp:"+ofControllerAddress.getHostAddress()+":"+ofControllerPort;
                    if (controllerCache != null) {
                        for (String uuid : controllerCache.keySet()) {
                            Controller controller = (Controller)controllerCache.get(uuid);
                            if (controller.getTarget().equals(newController)) {
                                cntrlUuid = uuid;
                                controllerUUIDs.add(new UUID(uuid));
                                break;
                            }
                        }
                    }
                    if (cntrlUuid == null) {
                        count++;
                        String uuid_name = "new_controller_"+count;
                        controllerUUIDs.add(new UUID(uuid_name));
                        Controller controllerRow = new Controller();
                        controllerRow.setTarget(newController);
                        InsertOperation addCtlRequest = new InsertOperation(Controller.NAME.getName(), uuid_name, controllerRow);
                        controllerInsertOperations.add(addCtlRequest);
                    }
                }
                String brCntrlUuid = null;
                Map<String, Table<?>> brTableCache = inventoryServiceInternal.getTableCache(node, Bridge.NAME.getName());
                for (String uuid : brTableCache.keySet()) {
                    Bridge bridge = (Bridge)brTableCache.get(uuid);
                    if (bridge.getName().contains(bridgeIdentifier)) {
                        brCntrlUuid = uuid;
                    }
                }
                Operation addControlRequest = null;
                Mutation bm = new Mutation("controller", Mutator.INSERT, controllerUUIDs);
                List<Mutation> mutations = new ArrayList<Mutation>();
                mutations.add(bm);

                UUID uuid = new UUID(brCntrlUuid);
                Condition condition = new Condition("_uuid", Function.EQUALS, uuid);
                List<Condition> where = new ArrayList<Condition>();
                where.add(condition);
                addControlRequest = new MutateOperation(Bridge.NAME.getName(), where, mutations);

                TransactBuilder transaction = new TransactBuilder();
                transaction.addOperations(controllerInsertOperations);
                transaction.addOperation(addControlRequest);

                ListenableFuture<List<OperationResult>> transResponse = connection.getRpc().transact(transaction);
                List<OperationResult> tr = transResponse.get();
                List<Operation> requests = transaction.getRequests();
                Status status = new Status(StatusCode.SUCCESS);
                for (int i = 0; i < tr.size() ; i++) {
                    if (i < requests.size()) requests.get(i).setResult(tr.get(i));
                    if (tr.get(i) != null && tr.get(i).getError() != null && tr.get(i).getError().trim().length() > 0) {
                        OperationResult result = tr.get(i);
                        status = new Status(StatusCode.BADREQUEST, result.getError() + " : " + result.getDetails());
                    }
                }

                if (tr.size() > requests.size()) {
                    OperationResult result = tr.get(tr.size()-1);
                    logger.error("Error creating Bridge : {}\n Error : {}\n Details : {}", bridgeIdentifier,
                            result.getError(),
                            result.getDetails());
                    status = new Status(StatusCode.BADREQUEST, result.getError() + " : " + result.getDetails());

                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return true;
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
            Node node = Node.fromString(nodeName);
            if (node == null) {
                ci.println("Invalid Node");
                return;
            }
            status = this.createBridgeDomain(node, bridgeName, null);
            ci.println("Bridge creation status : "+status.toString());
        } catch (Throwable e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ci.println("Failed to create Bridge "+bridgeName);
        }
    }

    public void _getBridgeDomains (CommandInterpreter ci) {
        String nodeName = ci.nextArgument();
        if (nodeName == null) {
            ci.println("Please enter Node Name");
            return;
        }
        Status status;

        List<String> brlist = new ArrayList<String>();
        try {
            Node node = Node.fromString(nodeName);
            brlist = this.getBridgeDomains(node);
            if (node == null) {
                ci.println("Invalid Node");
                return;
            }
            ci.println("Existing Bridges: "+brlist.toString());
        } catch (Throwable e) {
            e.printStackTrace();
            ci.println("Failed to list Bridges");
        }
    }

    public void _deleteBridgeDomain (CommandInterpreter ci) {
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
            Node node = Node.fromString(nodeName);
            if (node == null) {
                ci.println("Invalid Node");
                return;
            }
            status = this.deleteBridgeDomain(node, bridgeName);
            ci.println("Bridge deletion status : "+status.toString());
        } catch (Throwable e) {
            e.printStackTrace();
            ci.println("Failed to delete Bridge "+bridgeName);
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

        String type = ci.nextArgument();

        Map<String, String> configs = new HashMap<String, String>();
        while(true) {
            String configKey = ci.nextArgument();
            if (configKey == null) break;
            String configValue = ci.nextArgument();
            if (configValue == null) break;
            configs.put(configKey, configValue);
        }

        Map<ConfigConstants, Object> customConfigs = null;
        if (type != null) {
            customConfigs = new HashMap<ConfigConstants, Object>();
            customConfigs.put(ConfigConstants.TYPE, type);
        }

        if (configs.size() > 0) {
            if (customConfigs == null) customConfigs = new HashMap<ConfigConstants, Object>();
            customConfigs.put(ConfigConstants.CUSTOM, configs);
            ci.println(customConfigs.toString());
        }
        Status status;
        try {
            Node node = Node.fromString(nodeName);
            if (node == null) {
                ci.println("Invalid Node");
                return;
            }
            status = this.addPort(node, bridgeName, portName, customConfigs);
            ci.println("Port creation status : "+status.toString());
        } catch (Throwable e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ci.println("Failed to create Port "+portName+" in Bridge "+bridgeName);
        }
    }

    public void _deletePort (CommandInterpreter ci) {
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
            Node node = Node.fromString(nodeName);
            if (node == null) {
                ci.println("Invalid Node");
                return;
            }
            status = this.deletePort(node, bridgeName, portName);
            ci.println("Port deletion status : "+status.toString());
        } catch (Throwable e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ci.println("Failed to delete Port "+portName+" in Bridge "+bridgeName);
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
            Node node = Node.fromString(nodeName);
            if (node == null) {
                ci.println("Invalid Node");
                return;
            }
            status = this.addPort(node, bridgeName, portName, configs);
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
            Node node = Node.fromString(nodeName);
            if (node == null) {
                ci.println("Invalid Node");
                return;
            }
            status = this.addPort(node, bridgeName, portName, configs);
            ci.println("Port creation status : "+status.toString());
        } catch (Throwable e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ci.println("Failed to create Port "+portName+" in Bridge "+bridgeName);
        }
    }

    public void _printCache (CommandInterpreter ci) {
        String nodeName = ci.nextArgument();
        if (nodeName == null) {
            ci.println("Please enter Node Name");
            return;
        }
        Node node = Node.fromString(nodeName);
        if (node == null) {
            ci.println("Invalid Node");
            return;
        }
        inventoryServiceInternal.printCache(node);
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
        help.append("\t getBridgeDomains <Node>                                         - Get Bridges\n");
        help.append("\t deleteBridgeDomain <Node> <BridgeName>                          - Delete a Bridge\n");
        help.append("\t addPort <Node> <BridgeName> <PortName> <type> <options pairs>   - Add Port\n");
        help.append("\t deletePort <Node> <BridgeName> <PortName>                       - Delete Port\n");
        help.append("\t addPortVlan <Node> <BridgeName> <PortName> <vlan>               - Add Port, Vlan\n");
        help.append("\t addTunnel <Node> <Bridge> <Port> <tunnel-type> <remote-ip>      - Add Tunnel\n");
        help.append("\t printCache <Node>                                               - Prints Table Cache");
        return help.toString();
    }
}
