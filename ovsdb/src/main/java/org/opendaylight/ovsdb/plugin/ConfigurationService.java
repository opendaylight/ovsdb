package org.opendaylight.ovsdb.plugin;

import java.math.BigInteger;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.opendaylight.controller.sal.connection.ConnectionConstants;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.networkconfig.bridgedomain.ConfigConstants;
import org.opendaylight.controller.sal.networkconfig.bridgedomain.IPluginInBridgeDomainConfigService;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.opendaylight.ovsdb.lib.database.OVSInstance;
import org.opendaylight.ovsdb.lib.database.OvsdbType;
import org.opendaylight.ovsdb.lib.message.TransactBuilder;
import org.opendaylight.ovsdb.lib.message.operations.InsertOperation;
import org.opendaylight.ovsdb.lib.message.operations.MutateOperation;
import org.opendaylight.ovsdb.lib.message.operations.Operation;
import org.opendaylight.ovsdb.lib.message.operations.OperationResult;
import org.opendaylight.ovsdb.lib.message.operations.UpdateOperation;
import org.opendaylight.ovsdb.lib.notation.Condition;
import org.opendaylight.ovsdb.lib.notation.Function;
import org.opendaylight.ovsdb.lib.notation.Mutation;
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.notation.OvsDBMap;
import org.opendaylight.ovsdb.lib.notation.OvsDBSet;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.table.Bridge;
import org.opendaylight.ovsdb.lib.table.Capability;
import org.opendaylight.ovsdb.lib.table.Controller;
import org.opendaylight.ovsdb.lib.table.Interface;
import org.opendaylight.ovsdb.lib.table.Manager;
import org.opendaylight.ovsdb.lib.table.Mirror;
import org.opendaylight.ovsdb.lib.table.NetFlow;
import org.opendaylight.ovsdb.lib.table.Open_vSwitch;
import org.opendaylight.ovsdb.lib.table.Port;
import org.opendaylight.ovsdb.lib.table.Qos;
import org.opendaylight.ovsdb.lib.table.Queue;
import org.opendaylight.ovsdb.lib.table.SFlow;
import org.opendaylight.ovsdb.lib.table.SSL;
import org.opendaylight.ovsdb.lib.table.internal.Table;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ListenableFuture;

public class ConfigurationService implements IPluginInBridgeDomainConfigService, OVSDBConfigService,
                                             CommandProvider
{
    private static final Logger logger = LoggerFactory
            .getLogger(ConfigurationService.class);

    IConnectionServiceInternal connectionService;
    InventoryServiceInternal inventoryServiceInternal;
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

            /* Update config version */
            String ovsTableUUID = (String) ovsTable.keySet().toArray()[0];
            Mutation bm = new Mutation("next_cfg", Mutator.SUM, 1);
            List<Mutation> mutations = new ArrayList<Mutation>();
            mutations.add(bm);

            UUID uuid = new UUID(ovsTableUUID);
            Condition condition = new Condition("_uuid", Function.EQUALS, uuid);
            List<Condition> where = new ArrayList<Condition>();
            where.add(condition);
            MutateOperation updateCfgVerRequest = new MutateOperation(Open_vSwitch.NAME.getName(), where, mutations);

            TransactBuilder transaction = new TransactBuilder();
            transaction.addOperations(new ArrayList<Operation>(
                                      Arrays.asList(addSwitchRequest,
                                                    addIntfRequest,
                                                    addPortRequest,
                                                    addBridgeRequest,
                                                    updateCfgVerRequest)));

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
            if (status.isSuccess()) {
                setBridgeOFController(node, bridgeIdentifier);
            }
            return status;
        } catch(Exception e){
            logger.error("Error in createBridgeDomain(): ",e);
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
            logger.error("Error in addPort()",e);
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
            logger.error("Error in setManager(): ",e);
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
            logger.error("Error in deletePort()",e);
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
            Map<String, Table<?>> ovsTable = inventoryServiceInternal.getTableCache(node, Open_vSwitch.NAME.getName());
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
            if (ovsTable != null) {
                ovsUuid = (String) ovsTable.keySet().toArray()[0];
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
                logger.error("Error deleting Bridge : {}\n Error : {}\n Details : {}",
                        bridgeIdentifier, result.getError(), result.getDetails());
                status = new Status(StatusCode.BADREQUEST, result.getError() + " : " + result.getDetails());
            }
            return status;
        } catch (Exception e) {
            logger.error("Error in deleteBridgeDomain(): ",e);
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

    Boolean setBridgeOFController(Node node, String bridgeIdentifier) {
        if (connectionService == null) {
            logger.error("Couldn't refer to the ConnectionService");
            return false;
        }

        try{
            Map<String, Table<?>> brTableCache = inventoryServiceInternal.getTableCache(node, Bridge.NAME.getName());
            for (String uuid : brTableCache.keySet()) {
                Bridge bridge = (Bridge)brTableCache.get(uuid);
                if (bridge.getName().contains(bridgeIdentifier)) {
                    return connectionService.setOFController(node, uuid);
                }
            }
        } catch(Exception e) {
            logger.error("Error in setBridgeOFController()",e);
        }
        return false;
    }

    @Override
    public StatusWithUuid insertRow(Node node, String tableName, String parent_uuid, Table<?> row) {
        logger.info("tableName : {}, parent_uuid : {} Row : {}", tableName, parent_uuid, row.toString());
        StatusWithUuid statusWithUUID = null;

        // Schema based Table handling will help fix this static Table handling.

        if (row.getTableName().getName().equalsIgnoreCase("Bridge")) {
            statusWithUUID = insertBridgeRow(node, parent_uuid, (Bridge)row);
        }
        else if (row.getTableName().getName().equalsIgnoreCase("Capbility")) {
            statusWithUUID = insertCapabilityRow(node, parent_uuid, (Capability)row);
        }
        else if (row.getTableName().getName().equalsIgnoreCase("Controller")) {
            statusWithUUID = insertControllerRow(node, parent_uuid, (Controller)row);
        }
        else if (row.getTableName().getName().equalsIgnoreCase("Interface")) {
            statusWithUUID = insertInterfaceRow(node, parent_uuid, (Interface)row);
        }
        else if (row.getTableName().getName().equalsIgnoreCase("Manager")) {
            statusWithUUID = insertManagerRow(node, parent_uuid, (Manager)row);
        }
        else if (row.getTableName().getName().equalsIgnoreCase("Mirror")) {
            statusWithUUID = insertMirrorRow(node, parent_uuid, (Mirror)row);
        }
        else if (row.getTableName().getName().equalsIgnoreCase("NetFlow")) {
            statusWithUUID = insertNetFlowRow(node, parent_uuid, (NetFlow)row);
        }
        else if (row.getTableName().getName().equalsIgnoreCase("Open_vSwitch")) {
            statusWithUUID = insertOpen_vSwitchRow(node, (Open_vSwitch)row);
        }
        else if (row.getTableName().getName().equalsIgnoreCase("Port")) {
            statusWithUUID = insertPortRow(node, parent_uuid, (Port)row);
        }
        else if (row.getTableName().getName().equalsIgnoreCase("QoS")) {
            statusWithUUID = insertQosRow(node, parent_uuid, (Qos)row);
        }
        else if (row.getTableName().getName().equalsIgnoreCase("Queue")) {
            statusWithUUID = insertQueueRow(node, parent_uuid, (Queue)row);
        }
        else if (row.getTableName().getName().equalsIgnoreCase("sFlow")) {
            statusWithUUID = insertSflowRow(node, parent_uuid, (SFlow)row);
        }
        else if (row.getTableName().getName().equalsIgnoreCase("SSL")) {
            statusWithUUID = insertSSLRow(node, parent_uuid, (SSL)row);
        }
        return statusWithUUID;
    }


    @Override
    public Status updateRow (Node node, String tableName, String parentUUID, String rowUUID, Table<?> row) {
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

            if (ovsTable == null) {
                return new Status(StatusCode.NOTFOUND, "There are no Open_vSwitch instance in the Open_vSwitch table");
            }

            UUID uuid = new UUID(rowUUID);
            Condition condition = new Condition("_uuid", Function.EQUALS, uuid);
            List<Condition> where = new ArrayList<Condition>();
            where.add(condition);
            Operation updateRequest = new UpdateOperation(tableName, where, row);

            TransactBuilder transaction = new TransactBuilder();
            transaction.addOperations(new ArrayList<Operation>(
                                      Arrays.asList(updateRequest)));

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
                logger.error("Error Updating Row : {}/{}\n Error : {}\n Details : {}", tableName, row,
                                                                                       result.getError(),
                                                                                       result.getDetails());
                status = new Status(StatusCode.BADREQUEST, result.getError() + " : " + result.getDetails());
            }
            if (status.isSuccess()) {
                status = new Status(StatusCode.SUCCESS);
            }
            return status;
        } catch(Exception e){
            logger.error("Error in updateRow(): ",e);
        }
        return new Status(StatusCode.INTERNALERROR);
    }

    @Override
    public Status deleteRow(Node node, String tableName, String uuid) {
        if (tableName.equalsIgnoreCase("Bridge")) {
            return deleteBridgeRow(node, uuid);
        }
        else if (tableName.equalsIgnoreCase("Capbility")) {
            return deleteCapabilityRow(node, uuid);
        }
        else if (tableName.equalsIgnoreCase("Controller")) {
            return deleteControllerRow(node, uuid);
        }
        else if (tableName.equalsIgnoreCase("Interface")) {
            return deleteInterfaceRow(node, uuid);
        }
        else if (tableName.equalsIgnoreCase("Manager")) {
            return deleteManagerRow(node, uuid);
        }
        else if (tableName.equalsIgnoreCase("Mirror")) {
            return deleteMirrorRow(node, uuid);
        }
        else if (tableName.equalsIgnoreCase("NetFlow")) {
            return deleteNetFlowRow(node, uuid);
        }
        else if (tableName.equalsIgnoreCase("Open_vSwitch")) {
            return deleteOpen_vSwitchRow(node, uuid);
        }
        else if (tableName.equalsIgnoreCase("Port")) {
            return deletePortRow(node, uuid);
        }
        else if (tableName.equalsIgnoreCase("QoS")) {
            return deleteQosRow(node, uuid);
        }
        else if (tableName.equalsIgnoreCase("Queue")) {
            return deleteQueueRow(node, uuid);
        }
        else if (tableName.equalsIgnoreCase("sFlow")) {
            return deleteSflowRow(node, uuid);
        }
        else if (tableName.equalsIgnoreCase("SSL")) {
            return deleteSSLRow(node, uuid);
        }
        return new Status(StatusCode.NOTFOUND, "Table "+tableName+" not supported");
    }

    @Override
    public Map<String, Table<?>> getRows(Node node, String tableName) throws Exception{
        try{
            if (inventoryServiceInternal == null) {
                throw new Exception("Inventory Service is Unavailable.");
            }
            Map<String, Table<?>> ovsTable = inventoryServiceInternal.getTableCache(node, tableName);
            return ovsTable;
        } catch(Exception e){
            throw new Exception("Unable to read table due to "+e.getMessage());
        }
    }

    @Override
    public Table<?> getRow(Node node, String tableName, String uuid) throws Exception {
        try{
            if (inventoryServiceInternal == null) {
                throw new Exception("Inventory Service is Unavailable.");
            }
            Map<String, Table<?>> ovsTable = inventoryServiceInternal.getTableCache(node, tableName);
            if (ovsTable == null) return null;
            return ovsTable.get(uuid);
        } catch(Exception e){
            throw new Exception("Unable to read table due to "+e.getMessage());
        }
    }

    @Override
    public String getSerializedRows(Node node, String tableName) throws Exception{
        try{
            Map<String, Table<?>> ovsTable = this.getRows(node, tableName);
            if (ovsTable == null) return null;
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(ovsTable);
        } catch(Exception e){
            throw new Exception("Unable to read table due to "+e.getMessage());
        }
    }

    @Override
    public String getSerializedRow(Node node, String tableName, String uuid) throws Exception {
        try{
            Table<?> row = this.getRow(node, tableName, uuid);
            if (row == null) return null;
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(row);
        } catch(Exception e){
            throw new Exception("Unable to read table due to "+e.getMessage());
        }
    }

    @Override
    public List<String> getTables(Node node) {
        // TODO Auto-generated method stub
        return null;
    }

    private StatusWithUuid insertBridgeRow(Node node, String open_VSwitch_uuid, Bridge bridgeRow) {

        String insertErrorMsg = "bridge";
        String rowName=bridgeRow.getName();

        try{
            Map<String, Table<?>> ovsTable = inventoryServiceInternal.getTableCache(node, Open_vSwitch.NAME.getName());

            if (ovsTable == null) {
                return new StatusWithUuid(StatusCode.NOTFOUND, "There are no Open_vSwitch instance in the Open_vSwitch table");
            }

            String newBridge = "new_bridge";

            Operation addSwitchRequest = null;

            String ovsTableUUID = open_VSwitch_uuid;
            if (ovsTableUUID == null) ovsTableUUID = (String) ovsTable.keySet().toArray()[0];
            UUID bridgeUuidPair = new UUID(newBridge);
            Mutation bm = new Mutation("bridges", Mutator.INSERT, bridgeUuidPair);
            List<Mutation> mutations = new ArrayList<Mutation>();
            mutations.add(bm);

            UUID uuid = new UUID(ovsTableUUID);
            Condition condition = new Condition("_uuid", Function.EQUALS, uuid);
            List<Condition> where = new ArrayList<Condition>();
            where.add(condition);
            addSwitchRequest = new MutateOperation(Open_vSwitch.NAME.getName(), where, mutations);

            InsertOperation addBridgeRequest = new InsertOperation(Bridge.NAME.getName(), newBridge, bridgeRow);

            TransactBuilder transaction = new TransactBuilder();
            transaction.addOperations(new ArrayList<Operation>(
                                      Arrays.asList(addSwitchRequest,
                                                    addBridgeRequest)));

            int bridgeInsertIndex = transaction.getRequests().indexOf(addBridgeRequest);

            return _insertTableRow(node,transaction,bridgeInsertIndex,insertErrorMsg,rowName);

        } catch(Exception e){
            logger.error("Error in insertBridgeRow(): ",e);
        }
        return new StatusWithUuid(StatusCode.INTERNALERROR);
    }


    private StatusWithUuid insertPortRow(Node node, String bridge_uuid, Port portRow) {

        String insertErrorMsg = "port";
        String rowName=portRow.getName();

        try{
            Map<String, Table<?>> brTable = inventoryServiceInternal.getTableCache(node, Bridge.NAME.getName());
            if (brTable == null ||  brTable.get(bridge_uuid) == null) {
                return new StatusWithUuid(StatusCode.NOTFOUND, "Bridge with UUID "+bridge_uuid+" Not found");
            }
            String newPort = "new_port";
            UUID portUUID = new UUID(newPort);
            Mutation bm = new Mutation("ports", Mutator.INSERT, portUUID);
            List<Mutation> mutations = new ArrayList<Mutation>();
            mutations.add(bm);

            UUID uuid = new UUID(bridge_uuid);
            Condition condition = new Condition("_uuid", Function.EQUALS, uuid);
            List<Condition> where = new ArrayList<Condition>();
            where.add(condition);
            Operation addBrMutRequest = new MutateOperation(Bridge.NAME.getName(), where, mutations);

            // Default OVS schema is to have 1 or more interface part of Bridge. Hence it is mandatory to
            // Insert an Interface in a Port add case :-(.

            String newInterface = "new_interface";
            Interface interfaceRow = new Interface();
            interfaceRow.setName(portRow.getName());
            InsertOperation addIntfRequest = new InsertOperation(Interface.NAME.getName(),
                    newInterface, interfaceRow);

            OvsDBSet<UUID> interfaces = new OvsDBSet<UUID>();
            UUID interfaceid = new UUID(newInterface);
            interfaces.add(interfaceid);
            portRow.setInterfaces(interfaces);

            InsertOperation addPortRequest = new InsertOperation(Port.NAME.getName(), newPort, portRow);

            TransactBuilder transaction = new TransactBuilder();
            transaction.addOperations(new ArrayList<Operation>
            (Arrays.asList(addBrMutRequest, addPortRequest, addIntfRequest)));
            int portInsertIndex = transaction.getRequests().indexOf(addPortRequest);

            return _insertTableRow(node,transaction,portInsertIndex,insertErrorMsg,rowName);

            } catch (Exception e) {
            logger.error("Error in insertPortRow(): ",e);
        }
        return new StatusWithUuid(StatusCode.INTERNALERROR);
    }

    private StatusWithUuid insertInterfaceRow(Node node, String port_uuid, Interface interfaceRow) {

        String insertErrorMsg = "interface";
        String rowName=interfaceRow.getName();

        try{

            // Interface table must have entry in Port table, checking port table for port
            Map<String, Table<?>> portTable = inventoryServiceInternal.getTableCache(node, Port.NAME.getName());
            if (portTable == null ||  portTable.get(port_uuid) == null) {
                return new StatusWithUuid(StatusCode.NOTFOUND, "Port with UUID "+port_uuid+" Not found");
            }
            // MUTATOR, need to insert the interface UUID to LIST of interfaces in PORT TABLE for port_uuid
            String newInterface = "new_interface";
            UUID interfaceUUID = new UUID(newInterface);
            Mutation portTableMutation = new Mutation("interfaces", Mutator.INSERT, interfaceUUID); // field name to append is "interfaces"
            List<Mutation> mutations = new ArrayList<Mutation>();
            mutations.add(portTableMutation);

            // Create the Operation which will be used in Transact to perform the PORT TABLE mutation
            UUID uuid = new UUID(port_uuid);
            Condition condition = new Condition("_uuid", Function.EQUALS, uuid);
            List<Condition> where = new ArrayList<Condition>();
            where.add(condition);
            Operation addPortMutationRequest = new MutateOperation(Port.NAME.getName(), where, mutations);

            // Create the interface row request
            InsertOperation addIntfRequest = new InsertOperation(Interface.NAME.getName(),newInterface, interfaceRow);

            // Transaction to insert/modify tables - validate using "sudo ovsdb-client dump" on host running OVSDB process
            TransactBuilder transaction = new TransactBuilder();
            transaction.addOperations(new ArrayList<Operation>(Arrays.asList(addIntfRequest,addPortMutationRequest)));

            // Check the results. Iterates over the results of the Array of transaction Operations, and reports STATUS
            int interfaceInsertIndex = transaction.getRequests().indexOf(addIntfRequest);

            return _insertTableRow(node,transaction,interfaceInsertIndex,insertErrorMsg,rowName);

        } catch (Exception e) {
            logger.error("Error in insertInterfaceRow(): ",e);
        }
        return new StatusWithUuid(StatusCode.INTERNALERROR);
    }

    private StatusWithUuid insertOpen_vSwitchRow(Node node, Open_vSwitch row) {
        return new StatusWithUuid(StatusCode.NOTIMPLEMENTED, "Insert operation for this Table is not implemented yet.");
    }

    private StatusWithUuid insertControllerRow(Node node, String bridge_uuid, Controller row) {

        String insertErrorMsg = "controller";
        String rowName=row.getTableName().toString();

        try{

            Map<String, Table<?>> brTable = inventoryServiceInternal.getTableCache(node, Bridge.NAME.getName());
            if (brTable == null ||  brTable.get(bridge_uuid) == null) {
                return new StatusWithUuid(StatusCode.NOTFOUND, "Bridge with UUID "+bridge_uuid+" Not found");
            }

            Map<String, Table<?>> controllerCache = inventoryServiceInternal.getTableCache(node, Controller.NAME.getName());

            String uuid_name = "new_controller";
            boolean controllerExists = false;
            if (controllerCache != null) {
                for (String uuid : controllerCache.keySet()) {
                    Controller controller = (Controller)controllerCache.get(uuid);
                    if (controller.getTarget().equals(row.getTarget())) {
                        uuid_name = uuid;
                        controllerExists = true;
                        break;
                    }
                }
            }

            UUID controllerUUID = new UUID(uuid_name);
            Mutation bm = new Mutation("controller", Mutator.INSERT, controllerUUID);
            List<Mutation> mutations = new ArrayList<Mutation>();
            mutations.add(bm);

            UUID uuid = new UUID(bridge_uuid);
            Condition condition = new Condition("_uuid", Function.EQUALS, uuid);
            List<Condition> where = new ArrayList<Condition>();
            where.add(condition);
            Operation addBrMutRequest = new MutateOperation(Bridge.NAME.getName(), where, mutations);
            InsertOperation addControllerRequest = null;

            TransactBuilder transaction = new TransactBuilder();
            transaction.addOperation(addBrMutRequest);
            int portInsertIndex = -1;
            if (!controllerExists) {
                addControllerRequest = new InsertOperation(Controller.NAME.getName(), uuid_name, row);
                transaction.addOperation(addControllerRequest);
                portInsertIndex = transaction.getRequests().indexOf(addControllerRequest);
            }

            return _insertTableRow(node,transaction,portInsertIndex,insertErrorMsg,rowName);

        } catch (Exception e) {
            logger.error("Error in insertControllerRow(): ",e);
        }
        return new StatusWithUuid(StatusCode.INTERNALERROR);
    }

    private StatusWithUuid insertSSLRow(Node node, String parent_uuid, SSL row) {
        String insertErrorMsg = "SSL";
        String rowName=row.NAME.getName();

        try{
            Map<String, Table<?>> ovsTable = inventoryServiceInternal.getTableCache(node, Open_vSwitch.NAME.getName());

            if (ovsTable == null) {
                return new StatusWithUuid(StatusCode.NOTFOUND, "There are no Open_vSwitch instance in the Open_vSwitch table");
            }

            // Check SSL exists in parent (Open_vSwitch) as there can be 0|1
            for (int i=0 ; i < ovsTable.values().size(); i++){
                Open_vSwitch openVswitch = (Open_vSwitch)ovsTable.values().toArray()[i];
                if (openVswitch.getSsl().size() == 1) {
                    return new StatusWithUuid(StatusCode.BADREQUEST, "Cannot insert more than one SSL entry per Open_vSwitch.");
                }
            }
            String newSSL = "new_SSL";

            Operation addSSLRequest = null;

            String ovsTableUUID = parent_uuid;
            if (ovsTableUUID == null) ovsTableUUID = (String) ovsTable.keySet().toArray()[0];
            UUID sslUuid = new UUID(newSSL);
            Mutation sslMutation = new Mutation("ssl", Mutator.INSERT, sslUuid);
            List<Mutation> mutations = new ArrayList<Mutation>();
            mutations.add(sslMutation);

            UUID uuid = new UUID(ovsTableUUID);
            Condition condition = new Condition("_uuid", Function.EQUALS, uuid);
            List<Condition> where = new ArrayList<Condition>();
            where.add(condition);
            addSSLRequest = new MutateOperation(Open_vSwitch.NAME.getName(), where, mutations);

            InsertOperation addOpen_vSwitchRequest = new InsertOperation(SSL.NAME.getName(), newSSL, row);

            TransactBuilder transaction = new TransactBuilder();
            transaction.addOperations(new ArrayList<Operation>(
                                      Arrays.asList(addSSLRequest,
                                                    addOpen_vSwitchRequest)));

            int sslInsertIndex = transaction.getRequests().indexOf(addSSLRequest);

            return _insertTableRow(node,transaction,sslInsertIndex,insertErrorMsg,rowName);

        } catch(Exception e){
            logger.error("Error in insertSSLRow(): ",e);
        }
        return new StatusWithUuid(StatusCode.INTERNALERROR);
    }

    private StatusWithUuid insertSflowRow(Node node, String parent_uuid, SFlow row) {

        String insertErrorMsg = "sFlow";
        String rowName=row.NAME.getName();

        try{
            Map<String, Table<?>> brTable = inventoryServiceInternal.getTableCache(node, Bridge.NAME.getName());
            if (brTable == null ||  brTable.get(parent_uuid) == null) {
                return new StatusWithUuid(StatusCode.NOTFOUND, "Bridge with UUID "+parent_uuid+" Not found");
            }

            // Check SSL exists in parent (Open_vSwitch) as there can be 0|1
            for (int i=0 ; i < brTable.values().size(); i++){
                Bridge bridge = (Bridge)brTable.values().toArray()[i];
                if (bridge.getSflow().size() == 1) {
                    return new StatusWithUuid(StatusCode.BADREQUEST, "Cannot insert more than one sFlow entry per Bridge.");
                }
            }
            String newSflow = "new_sflow";

            Operation addBridgeRequest = null;

            if (parent_uuid == null) {
                return new StatusWithUuid(StatusCode.BADREQUEST, "Require parent Bridge UUID.");
            }
            UUID sflowUuid = new UUID(newSflow);
            Mutation sflowMutation = new Mutation("sflow", Mutator.INSERT, sflowUuid);
            List<Mutation> mutations = new ArrayList<Mutation>();
            mutations.add(sflowMutation);

            UUID uuid = new UUID(parent_uuid);
            Condition condition = new Condition("_uuid", Function.EQUALS, uuid);
            List<Condition> where = new ArrayList<Condition>();
            where.add(condition);
            addBridgeRequest = new MutateOperation(Bridge.NAME.getName(), where, mutations);

            InsertOperation addSflowRequest = new InsertOperation(SFlow.NAME.getName(), newSflow, row);

            TransactBuilder transaction = new TransactBuilder();
            transaction.addOperations(new ArrayList<Operation>(
                                      Arrays.asList(addSflowRequest,
                                                    addBridgeRequest)));

            int sflowInsertIndex = transaction.getRequests().indexOf(addSflowRequest);


            return _insertTableRow(node,transaction,sflowInsertIndex,insertErrorMsg,rowName);

        } catch (Exception e) {
            logger.error("Error in insertInterfaceRow(): ",e);
        }
        return new StatusWithUuid(StatusCode.INTERNALERROR);
    }

    private StatusWithUuid insertQueueRow(Node node, String parent_uuid, Queue row) {
        return new StatusWithUuid(StatusCode.NOTIMPLEMENTED, "Insert operation for this Table is not implemented yet.");
    }

    private StatusWithUuid insertQosRow(Node node, String parent_uuid, Qos row) {
        return new StatusWithUuid(StatusCode.NOTIMPLEMENTED, "Insert operation for this Table is not implemented yet.");
    }

    private StatusWithUuid insertNetFlowRow(Node node, String parent_uuid, NetFlow row) {
        return new StatusWithUuid(StatusCode.NOTIMPLEMENTED, "Insert operation for this Table is not implemented yet.");
    }

    private StatusWithUuid insertMirrorRow(Node node, String parent_uuid, Mirror row) {
        return new StatusWithUuid(StatusCode.NOTIMPLEMENTED, "Insert operation for this Table is not implemented yet.");
    }

    private StatusWithUuid insertManagerRow(Node node, String parent_uuid, Manager row) {
        return new StatusWithUuid(StatusCode.NOTIMPLEMENTED, "Insert operation for this Table is not implemented yet.");
    }

    private StatusWithUuid insertCapabilityRow(Node node, String parent_uuid, Capability row) {
        return new StatusWithUuid(StatusCode.NOTIMPLEMENTED, "Insert operation for this Table is not implemented yet.");
    }

    private StatusWithUuid _insertTableRow(Node node, TransactBuilder transaction, Integer insertIndex, String insertErrorMsg,String rowName){

        try{
            //Check for connection before calling RPC to perform transaction
            if (connectionService == null) {
                logger.error("Couldn't refer to the ConnectionService");
                return new StatusWithUuid(StatusCode.NOSERVICE);
            }

            Connection connection = this.getConnection(node);
            if (connection == null) {
                return new StatusWithUuid(StatusCode.NOSERVICE, "Connection to ovsdb-server not available");
            }

            ListenableFuture<List<OperationResult>> transResponse = connection.getRpc().transact(transaction);
            List<OperationResult> tr = transResponse.get();
            List<Operation> requests = transaction.getRequests();
            StatusWithUuid status = new StatusWithUuid(StatusCode.SUCCESS);
            for (int i = 0; i < tr.size() ; i++) {
                if (i < requests.size()) requests.get(i).setResult(tr.get(i));
                if (tr.get(i) != null && tr.get(i).getError() != null && tr.get(i).getError().trim().length() > 0) {
                    OperationResult result = tr.get(i);
                    status = new StatusWithUuid(StatusCode.BADREQUEST, result.getError() + " : " + result.getDetails());
                }
            }

            if (tr.size() > requests.size()) {
                OperationResult result = tr.get(tr.size()-1);
                logger.error("Error creating {} : {}\n Error : {}\n Details : {}",     insertErrorMsg,
                                                                                       rowName,
                                                                                       result.getError(),
                                                                                       result.getDetails());
                status = new StatusWithUuid(StatusCode.BADREQUEST, result.getError() + " : " + result.getDetails());
            }
            if (status.isSuccess()) {
                UUID uuid = tr.get(insertIndex).getUuid();
                status = new StatusWithUuid(StatusCode.SUCCESS, uuid);
            }
            return status;
        } catch(Exception e){
            logger.error("Error in _insertTableRow(): ",e);
        }
        return new StatusWithUuid(StatusCode.INTERNALERROR);
    }


    private Status deleteBridgeRow(Node node, String uuid) {
        // Set up variables for generic _deleteTableRow()
        String parentTableName=Open_vSwitch.NAME.getName();
        String childTableName=Bridge.NAME.getName();
        String parentColumn = "bridges";

        return _deleteTableRow(node,uuid,parentTableName,childTableName,parentColumn);
    }

    private Status deletePortRow(Node node, String uuid) {
        // Set up variables for generic _deleteTableRow()
        String parentTableName=Bridge.NAME.getName();
        String childTableName=Port.NAME.getName();
        String parentColumn = "ports";

        return _deleteTableRow(node,uuid,parentTableName,childTableName,parentColumn);
    }

    private Status deleteInterfaceRow(Node node, String uuid) {
        // Since Port<-Interface tables have a 1:n relationship, need to test if this is the last interface
        // assigned to a port before attempting delete.
        Map<String, Table<?>> portTable = inventoryServiceInternal.getTableCache(node, Port.NAME.getName());
        Map<String, Table<?>> interfaceTable = inventoryServiceInternal.getTableCache(node, Interface.NAME.getName());
        // Check that the UUID exists
        if (portTable == null || interfaceTable == null || uuid == null || interfaceTable.get(uuid) == null) {
            return new Status(StatusCode.NOTFOUND, "");
        }

        // Need to check if this is the last interface for that port. Cannot delete last interface.
        UUID compareUuid = new UUID(uuid);
        for (int i=0 ; i < portTable.values().size(); i++){
            Port port = (Port)portTable.values().toArray()[i];
            if ((port.getInterfaces().size() == 1) && (port.getInterfaces().contains(compareUuid))){
                return new Status(StatusCode.BADREQUEST, "Cannot delete last interface from port");
            }
        }

        // Since the above past, it's safe to use the generic _deleteTableRow method
        // Set up variables for generic _deleteTableRow()
        String parentTableName=Port.NAME.getName();
        String childTableName=Interface.NAME.getName();
        String parentColumn = "interfaces";

        return _deleteTableRow(node,uuid,parentTableName,childTableName,parentColumn);
    }

    private Status deleteControllerRow(Node node, String uuid) {
        // Set up variables for generic _deleteTableRow()
        String parentTableName=Bridge.NAME.getName();
        String childTableName=Controller.NAME.getName();
        String parentColumn = "controller";

        return _deleteTableRow(node,uuid,parentTableName,childTableName,parentColumn);
    }

    private Status deleteOpen_vSwitchRow(Node node, String uuid) {
        return new Status(StatusCode.NOTIMPLEMENTED, "delete operation for this Table is not implemented yet.");
    }

    private Status deleteSSLRow(Node node, String uuid) {
        // Set up variables for generic _deleteTableRow()
        String parentTableName=Open_vSwitch.NAME.getName();
        String childTableName=SSL.NAME.getName();
        String parentColumn = "ssl";

        return _deleteTableRow(node,uuid,parentTableName,childTableName,parentColumn);
    }

    private Status deleteSflowRow(Node node, String uuid) {
        // Set up variables for generic _deleteTableRow()
        String parentTableName=Bridge.NAME.getName();
        String childTableName=SFlow.NAME.getName();
        String parentColumn = "sflow";

        return _deleteTableRow(node,uuid,parentTableName,childTableName,parentColumn);
    }

    private Status deleteQueueRow(Node node, String uuid) {
        return new Status(StatusCode.NOTIMPLEMENTED, "delete operation for this Table is not implemented yet.");
    }

    private Status deleteQosRow(Node node, String uuid) {
        return new Status(StatusCode.NOTIMPLEMENTED, "delete operation for this Table is not implemented yet.");
    }

    private Status deleteNetFlowRow(Node node, String uuid) {
        return new Status(StatusCode.NOTIMPLEMENTED, "delete operation for this Table is not implemented yet.");
    }

    private Status deleteMirrorRow(Node node, String uuid) {
        return new Status(StatusCode.NOTIMPLEMENTED, "delete operation for this Table is not implemented yet.");
    }

    private Status deleteManagerRow(Node node, String uuid) {
        return new Status(StatusCode.NOTIMPLEMENTED, "delete operation for this Table is not implemented yet.");
    }

    private Status deleteCapabilityRow(Node node, String uuid) {
        return new Status(StatusCode.NOTIMPLEMENTED, "delete operation for this Table is not implemented yet.");
    }
    private Status _deleteTableRow(Node node,String uuid,String parentTableName, String childTableName, String parentColumn) {
        try {
            // Check there is a connectionService
            if (connectionService == null) {
                logger.error("Couldn't refer to the ConnectionService");
                return new Status(StatusCode.NOSERVICE);
            }

            // Establish the connection
            Connection connection = this.getConnection(node);
            if (connection == null) {
                return new Status(StatusCode.NOSERVICE, "Connection to ovsdb-server not available");
            }

            // Ports have a 0:n relationship with Bridges, so need to MUTATE BRIDGE row and DELETE PORT row
            Map<String, Table<?>> parentTable = inventoryServiceInternal.getTableCache(node, parentTableName);
            Map<String, Table<?>> childTable = inventoryServiceInternal.getTableCache(node, childTableName);

            // Check that the UUID exists
            if (parentTable == null || childTable == null || uuid == null || childTable.get(uuid) == null) {
                return new Status(StatusCode.NOTFOUND, "");
            }

            // Initialise the actual request var
            Operation delRequest = null;

            // Prepare the mutator to remove the port UUID from the "ports" list in the BRIDGE TABLE
            UUID rowUuid = new UUID(uuid);
            Mutation mutator = new Mutation(parentColumn, Mutator.DELETE, rowUuid);
            List<Mutation> mutations = new ArrayList<Mutation>();
            mutations.add(mutator);

            Status status = new Status(StatusCode.SUCCESS);

            // INCLUDES condition ensures that it captures all rows in the parent table (ie duplicates) that have the child UUID
            Condition condition = new Condition(parentColumn, Function.INCLUDES, rowUuid);
            List<Condition> where = new ArrayList<Condition>();
            where.add(condition);
            delRequest = new MutateOperation(parentTableName, where, mutations);

            TransactBuilder transaction = new TransactBuilder();
            transaction.addOperations(new ArrayList<Operation>(Arrays.asList(delRequest)));

            // This executes the transaction.
            ListenableFuture<List<OperationResult>> transResponse = connection.getRpc().transact(transaction);

            // Pull the responses
            List<OperationResult> tr = transResponse.get();
            List<Operation> requests = transaction.getRequests();

            for (int i = 0; i < tr.size(); i++) {
                if (i < requests.size()) requests.get(i).setResult(tr.get(i));
                if (tr.get(i) != null && tr.get(i).getError() != null && tr.get(i).getError().trim().length() > 0) {
                    OperationResult result = tr.get(i);
                    status = new Status(StatusCode.BADREQUEST, result.getError() + " : " + result.getDetails());
                }
            }

            if (tr.size() > requests.size()) {
                OperationResult result = tr.get(tr.size() - 1);
                logger.error("Error deleting: {}\n Error : {}\n Details : {}",
                        uuid, result.getError(), result.getDetails());
                status = new Status(StatusCode.BADREQUEST, result.getError() + " : " + result.getDetails());
            }
            return status;
        } catch (Exception e) {
            logger.error("Error in _deleteTableRow",e);
        }
        return new Status(StatusCode.INTERNALERROR);
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

