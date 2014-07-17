/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Brent Salisbury, Keith Burns
 */
package org.opendaylight.ovsdb.plugin;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.opendaylight.controller.clustering.services.IClusterGlobalServices;
import org.opendaylight.controller.sal.connection.ConnectionConstants;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.networkconfig.bridgedomain.ConfigConstants;
import org.opendaylight.controller.sal.networkconfig.bridgedomain.IPluginInBridgeDomainConfigService;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.error.SchemaVersionMismatchException;
import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.notation.OvsdbSet;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.Insert;
import org.opendaylight.ovsdb.lib.operations.Operation;
import org.opendaylight.ovsdb.lib.operations.OperationResult;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.ColumnSchema;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.TableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.Controller;
import org.opendaylight.ovsdb.schema.openvswitch.Interface;
import org.opendaylight.ovsdb.schema.openvswitch.Manager;
import org.opendaylight.ovsdb.schema.openvswitch.OpenVSwitch;
import org.opendaylight.ovsdb.schema.openvswitch.Port;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ListenableFuture;

public class ConfigurationService implements IPluginInBridgeDomainConfigService, OvsdbConfigService,
                                             CommandProvider
{
    private static final Logger logger = LoggerFactory
            .getLogger(ConfigurationService.class);

    IConnectionServiceInternal connectionService;
    InventoryServiceInternal inventoryServiceInternal;
    boolean forceConnect = false;
    protected static final String OPENFLOW_10 = "1.0";
    protected static final String OPENFLOW_13 = "1.3";

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

    private IClusterGlobalServices clusterServices;

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
        if (connection == null || !connection.getClient().isActive()) {
            return null;
        }

        return connection;
    }
    /*
     * There are a few Open_vSwitch schema specific special case handling to be done for
     * the older API (such as by inserting a mandatory Interface row automatically upon inserting
     * a Port row.
     */
    private void handleSpecialInsertCase(OvsdbClient client, String databaseName,
            String tableName, String uuid, Row<GenericTableSchema> row, TransactionBuilder transactionBuilder) {
        Port port = client.getTypedRowWrapper(Port.class, null);
        if (databaseName.equals(OvsVswitchdSchemaConstants.DATABASE_NAME) && tableName.equals(port.getSchema().getName())) {
            port = client.getTypedRowWrapper(Port.class, row);
            DatabaseSchema dbSchema = client.getDatabaseSchema(databaseName);
            TableSchema<GenericTableSchema> tableSchema = dbSchema.table(tableName, GenericTableSchema.class);
            ColumnSchema<GenericTableSchema, Set<UUID>> columnSchema = tableSchema.multiValuedColumn("interfaces", UUID.class);
            String namedUuid = "Special_"+tableName;
            List<Operation> priorOperations = transactionBuilder.getOperations();
            Insert portOperation = (Insert)priorOperations.get(0);
            portOperation.value(columnSchema, new UUID(namedUuid));

            Column<GenericTableSchema, ?> nameColumn = port.getNameColumn();
            List<Column<GenericTableSchema, ?>> columns = new ArrayList<Column<GenericTableSchema, ?>>();
            columns.add(nameColumn);
            Row<GenericTableSchema> intfRow = new Row<GenericTableSchema>(tableSchema, columns);
            this.processInsertTransaction(client, databaseName, "Interface", null, null, null, namedUuid, intfRow, transactionBuilder);
        }
    }

    /*
     * A common Transaction that takes in old API style Parent_uuid and inserts a mutation on
     * the parent table for the newly inserted Child.
     * Due to some additional special case(s), the Transaction is further amended by handleSpecialInsertCase
     */
    private void processInsertTransaction(OvsdbClient client, String databaseName, String childTable,
                                    String parentTable, String parentUuid, String parentColumn, String namedUuid,
                                    Row<GenericTableSchema> row, TransactionBuilder transactionBuilder) {
        DatabaseSchema dbSchema = client.getDatabaseSchema(databaseName);
        TableSchema<GenericTableSchema> childTableSchema = dbSchema.table(childTable, GenericTableSchema.class);
        transactionBuilder.add(op.insert(childTableSchema, row)
                        .withId(namedUuid));

        if (parentColumn != null) {
            TableSchema<GenericTableSchema> parentTableSchema = dbSchema.table(parentTable, GenericTableSchema.class);
            ColumnSchema<GenericTableSchema, UUID> parentColumnSchema = parentTableSchema.column(parentColumn, UUID.class);
            ColumnSchema<GenericTableSchema, UUID> _uuid = parentTableSchema.column("_uuid", UUID.class);

            transactionBuilder
                .add(op.mutate(parentTableSchema)
                        .addMutation(parentColumnSchema, Mutator.INSERT, new UUID(namedUuid))
                        .where(_uuid.opEqual(new UUID(parentUuid)))
                        .build());
        }
        /*
         * There are a few Open_vSwitch schema specific special case handling to be done for
         * the older API (such as by inserting a mandatory Interface row automatically upon inserting
         * a Port row.
         */
        handleSpecialInsertCase(client, databaseName, childTable, namedUuid, row, transactionBuilder);
    }

    /*
     * TODO : Move all the Special Cases out of ConfigurationService and into the Schema specific bundles.
     * But that makes plugin more reliant on the Typed Bundles more than just API wrapper.
     * Keeping these Special Handling locally till we introduce the full schema independent APIs in the
     * plugin layer.
     */
    public String getSpecialCaseParentUUID(Node node, String databaseName, String childTableName) {
        if (!databaseName.equals(OvsVswitchdSchemaConstants.DATABASE_NAME)) return null;
        String[] parentColumn = OvsVswitchdSchemaConstants.getParentColumnToMutate(childTableName);
        if (parentColumn != null && parentColumn[0].equals(OvsVswitchdSchemaConstants.DATABASE_NAME)) {
            Connection connection = connectionService.getConnection(node);
            OpenVSwitch openVSwitch = connection.getClient().getTypedRowWrapper(OpenVSwitch.class, null);
            ConcurrentMap<String, Row> row = this.getRows(node, openVSwitch.getSchema().getName());
            if (row == null || row.size() == 0) return null;
            return (String)row.keySet().toArray()[0];
        }
        return null;
    }

    /*
     * Though this is a New API that takes in Row object, this still is considered a
     * Deprecated call because of the assumption with a Single Row insertion.
     * An ideal insertRow must be able to take in multiple Rows, which includes the
     * Row being inserted in one Table and other Rows that needs mutate in other Tables.
     */
    @Override
    public StatusWithUuid insertRow(Node node, String tableName, String parentUuid, Row<GenericTableSchema> row) {
        String[] parentColumn = OvsVswitchdSchemaConstants.getParentColumnToMutate(tableName);
        if (parentColumn == null) {
            parentColumn = new String[]{null, null};
        }

        Connection connection = connectionService.getConnection(node);
        OvsdbClient client = connection.getClient();

        if (parentUuid == null) {
            parentUuid = this.getSpecialCaseParentUUID(node, OvsVswitchdSchemaConstants.DATABASE_NAME, tableName);
        }
        logger.debug("insertRow Connection : {} Table : {} ParentTable : {} Parent Column: {} Parent UUID : {} Row : {}",
                     client.getConnectionInfo(), tableName, parentColumn[0], parentColumn[1], parentUuid, row);

        DatabaseSchema dbSchema = client.getDatabaseSchema(OvsVswitchdSchemaConstants.DATABASE_NAME);
        TransactionBuilder transactionBuilder = client.transactBuilder(dbSchema);

        String namedUuid = "Transaction_"+ tableName;
        this.processInsertTransaction(client, OvsVswitchdSchemaConstants.DATABASE_NAME, tableName,
                                parentColumn[0], parentUuid, parentColumn[1], namedUuid,
                                row, transactionBuilder);

        ListenableFuture<List<OperationResult>> results = transactionBuilder.execute();
        List<OperationResult> operationResults;
        try {
            operationResults = results.get();
            if (operationResults.isEmpty() || (transactionBuilder.getOperations().size() != operationResults.size())) {
                return new StatusWithUuid(StatusCode.INTERNALERROR);
            }
            for (OperationResult result : operationResults) {
                if (result.getError() != null) {
                    return new StatusWithUuid(StatusCode.BADREQUEST, result.getError());
                }
            }
            UUID uuid = operationResults.get(0).getUuid();
            return new StatusWithUuid(StatusCode.SUCCESS, uuid);
        } catch (InterruptedException | ExecutionException e) {
            // TODO Auto-generated catch block
            return new StatusWithUuid(StatusCode.INTERNALERROR, e.getLocalizedMessage());
        }

    }

    @Override
    public Status updateRow (Node node, String tableName, String parentUUID, String rowUUID, Row row) {
        String databaseName = OvsVswitchdSchemaConstants.DATABASE_NAME;
        Connection connection = connectionService.getConnection(node);
        OvsdbClient client = connection.getClient();

        logger.debug("updateRow : Connection : {} databaseName : {} tableName : {} rowUUID : {} row : {}",
                      client.getConnectionInfo(), databaseName, tableName, rowUUID, row.toString());
        try{
            DatabaseSchema dbSchema = client.getDatabaseSchema(databaseName);
            TransactionBuilder transactionBuilder = client.transactBuilder(dbSchema);
            TableSchema<GenericTableSchema> tableSchema = dbSchema.table(tableName, GenericTableSchema.class);
            ColumnSchema<GenericTableSchema, UUID> _uuid = tableSchema.column("_uuid", UUID.class);
            transactionBuilder.add(op.update(tableSchema, row)
                                     .where(_uuid.opEqual(new UUID(rowUUID)))
                                     .build());

            ListenableFuture<List<OperationResult>> results = transactionBuilder.execute();
            List<OperationResult> operationResults = results.get();
            if (operationResults.isEmpty() || (transactionBuilder.getOperations().size() != operationResults.size())) {
                return new StatusWithUuid(StatusCode.INTERNALERROR);
            }
            for (OperationResult result : operationResults) {
                if (result.getError() != null) {
                    return new StatusWithUuid(StatusCode.BADREQUEST, result.getError());
                }
            }
            return new StatusWithUuid(StatusCode.SUCCESS);
        } catch(Exception e){
            logger.error("Error in updateRow(): ",e);
        }
        return new Status(StatusCode.INTERNALERROR);
    }

    private void processDeleteTransaction(OvsdbClient client, String databaseName, String childTable,
                                    String parentTable, String parentColumn, String uuid, TransactionBuilder transactionBuilder) {
        DatabaseSchema dbSchema = client.getDatabaseSchema(databaseName);
        TableSchema<GenericTableSchema> childTableSchema = dbSchema.table(childTable, GenericTableSchema.class);

        if (parentColumn != null) {
            TableSchema<GenericTableSchema> parentTableSchema = dbSchema.table(parentTable, GenericTableSchema.class);
            ColumnSchema<GenericTableSchema, UUID> parentColumnSchema = parentTableSchema.column(parentColumn, UUID.class);
            transactionBuilder
                .add(op.mutate(parentTableSchema)
                        .addMutation(parentColumnSchema, Mutator.DELETE, new UUID(uuid))
                        .where(parentColumnSchema.opIncludes(new UUID(uuid)))
                        .build());
        }

        ColumnSchema<GenericTableSchema, UUID> _uuid = childTableSchema.column("_uuid", UUID.class);
        transactionBuilder.add(op.delete(childTableSchema)
                .where(_uuid.opEqual(new UUID(uuid)))
                .build());
    }

    @Override
    public Status deleteRow(Node node, String tableName, String uuid) {
        String databaseName = OvsVswitchdSchemaConstants.DATABASE_NAME;
        Connection connection = connectionService.getConnection(node);
        OvsdbClient client = connection.getClient();

        String[] parentColumn = OvsVswitchdSchemaConstants.getParentColumnToMutate(tableName);
        if (parentColumn == null) {
            parentColumn = new String[]{null, null};
        }

        logger.debug("deleteRow : Connection : {} databaseName : {} tableName : {} Uuid : {} ParentTable : {} ParentColumn : {}",
                client.getConnectionInfo(), databaseName, tableName, uuid, parentColumn[0], parentColumn[1]);

        DatabaseSchema dbSchema = client.getDatabaseSchema(databaseName);
        TransactionBuilder transactionBuilder = client.transactBuilder(dbSchema);
        this.processDeleteTransaction(client, OvsVswitchdSchemaConstants.DATABASE_NAME, tableName,
                                      parentColumn[0], parentColumn[1], uuid, transactionBuilder);

        ListenableFuture<List<OperationResult>> results = transactionBuilder.execute();
        List<OperationResult> operationResults;
        try {
            operationResults = results.get();
            if (operationResults.isEmpty() || (transactionBuilder.getOperations().size() != operationResults.size())) {
                return new StatusWithUuid(StatusCode.INTERNALERROR);
            }
            for (OperationResult result : operationResults) {
                if (result.getError() != null) {
                    return new StatusWithUuid(StatusCode.BADREQUEST, result.getError());
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return new Status(StatusCode.SUCCESS);
    }

    @Override
    public ConcurrentMap<String, Row> getRows(Node node, String tableName) {
        ConcurrentMap<String, Row> ovsTable = inventoryServiceInternal.getTableCache(node, OvsVswitchdSchemaConstants.DATABASE_NAME,  tableName);
        return ovsTable;
    }

    @Override
    public Row getRow(Node node, String tableName, String uuid) {
        Map<String, Row> ovsTable = inventoryServiceInternal.getTableCache(node, OvsVswitchdSchemaConstants.DATABASE_NAME,  tableName);
        if (ovsTable == null) return null;
        return ovsTable.get(uuid);
    }

    @Override
    public List<String> getTables(Node node) {
        ConcurrentMap<String, ConcurrentMap<String, Row>> cache  = inventoryServiceInternal.getCache(node, OvsVswitchdSchemaConstants.DATABASE_NAME);
        if (cache == null) return null;
        return new ArrayList<String>(cache.keySet());
    }

    private List<InetAddress> getControllerIPAddresses(Connection connection) {
        List<InetAddress> controllers = null;
        InetAddress controllerIP = null;

        controllers = new ArrayList<InetAddress>();
        String addressString = System.getProperty("ovsdb.controller.address");

        if (addressString != null) {
            try {
                controllerIP = InetAddress.getByName(addressString);
                if (controllerIP != null) {
                    controllers.add(controllerIP);
                    return controllers;
                }
            } catch (UnknownHostException e) {
                logger.error("Host {} is invalid", addressString);
            }
        }

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

        addressString = System.getProperty("of.address");

        if (addressString != null) {
            try {
                controllerIP = InetAddress.getByName(addressString);
                if (controllerIP != null) {
                    controllers.add(controllerIP);
                    return controllers;
                }
            } catch (UnknownHostException e) {
                logger.error("Host {} is invalid", addressString);
            }
        }

        try {
            controllerIP = connection.getClient().getConnectionInfo().getLocalAddress();
            controllers.add(controllerIP);
            return controllers;
        } catch (Exception e) {
            logger.debug("Invalid connection provided to getControllerIPAddresses", e);
        }
        return controllers;
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

    @Override
    public Boolean setOFController(Node node, String bridgeUUID) throws InterruptedException, ExecutionException {
        Connection connection = this.getConnection(node);
        if (connection == null) {
            return false;
        }

        Bridge bridge = connection.getClient().createTypedRowWrapper(Bridge.class);

        Status updateOperationStatus = null;
        try {
            OvsdbSet<String> protocols = new OvsdbSet<String>();

            String ofVersion = System.getProperty("ovsdb.of.version", OPENFLOW_10);
            switch (ofVersion) {
                case OPENFLOW_13:
                    protocols.add("OpenFlow13");
                    break;
                case OPENFLOW_10:
                    //fall through
                default:
                    protocols.add("OpenFlow10");
                    break;
            }
            bridge.setProtocols(protocols);
            updateOperationStatus = this.updateRow(node, bridge.getSchema().getName(),
                                                   null, bridgeUUID, bridge.getRow());
            logger.debug("Bridge {} updated to {} with Status {}", bridgeUUID,
                         protocols.toArray()[0],updateOperationStatus);

        } catch (SchemaVersionMismatchException e){
            logger.debug(e.toString());
        }

        // If we fail to update the protocols
        if (updateOperationStatus != null && !updateOperationStatus.isSuccess()) {
            return updateOperationStatus.isSuccess();
        }

        Status status = null;
        List<InetAddress> ofControllerAddrs = this.getControllerIPAddresses(connection);
        short ofControllerPort = getControllerOFPort();
        for (InetAddress ofControllerAddress : ofControllerAddrs) {
            String newController = "tcp:"+ofControllerAddress.getHostAddress()+":"+ofControllerPort;
            Controller controllerRow = connection.getClient().createTypedRowWrapper(Controller.class);
            controllerRow.setTarget(ImmutableSet.of(newController));
            //ToDo: Status gets overwritten on each iteration. If any operation other than the last fails it's ignored.
            status = this.insertRow(node, controllerRow.getSchema().getName(), bridgeUUID, controllerRow.getRow());
        }

        if (status != null) {
            return status.isSuccess();
        }

        return false;
    }


    Boolean setBridgeOFController(Node node, String bridgeIdentifier) {
        if (connectionService == null) {
            logger.error("Couldn't refer to the ConnectionService");
            return false;
        }

        try{
            Connection connection = connectionService.getConnection(node);
            Bridge bridge = connection.getClient().getTypedRowWrapper(Bridge.class, null);

            Map<String, Row> brTableCache = inventoryServiceInternal.getTableCache(node, OvsVswitchdSchemaConstants.DATABASE_NAME, bridge.getSchema().getName());
            for (String uuid : brTableCache.keySet()) {
                bridge = connection.getClient().getTypedRowWrapper(Bridge.class, brTableCache.get(uuid));
                if (bridge.getName().contains(bridgeIdentifier)) {
                    return setOFController(node, uuid);
                }
            }
        } catch(Exception e) {
            logger.error("Error in setBridgeOFController()",e);
        }
        return false;
    }

    @Override
    public <T extends TypedBaseTable<?>> String getTableName(Node node, Class<T> typedClass) {
        Connection connection = connectionService.getConnection(node);
        if (connection == null) return null;
        OvsdbClient client = connection.getClient();
        TypedBaseTable<?> typedTable = client.getTypedRowWrapper(typedClass, null);
        if (typedTable == null) return null;
        return typedTable.getSchema().getName();
    }

    @Override
    public <T extends TypedBaseTable<?>> T getTypedRow(Node node, Class<T> typedClass, Row row) {
        Connection connection = connectionService.getConnection(node);
        if (connection == null) return null;
        OvsdbClient client = connection.getClient();
        return (T)client.getTypedRowWrapper(typedClass, row);
    }

    @Override
    public <T extends TypedBaseTable<?>> T createTypedRow(Node node, Class<T> typedClass) {
        Connection connection = connectionService.getConnection(node);
        if (connection == null) return null;
        OvsdbClient client = connection.getClient();
        return client.createTypedRowWrapper(typedClass);
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
        }  catch (UnknownHostException e) {
            logger.error("Unable to resolve " + ovsdbserver, e);
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

        Node node = Node.fromString(nodeName);
        if (node == null) {
            ci.println("Invalid Node");
            return;
        }
        status = this.createBridgeDomain(node, bridgeName, null);
        ci.println("Bridge creation status : "+status.toString());
    }

    public void _getBridgeDomains (CommandInterpreter ci) {
        String nodeName = ci.nextArgument();
        if (nodeName == null) {
            ci.println("Please enter Node Name");
            return;
        }

        List<String> brlist = new ArrayList<String>();
        Node node = Node.fromString(nodeName);
        brlist = this.getBridgeDomains(node);
        if (node == null) {
            ci.println("Invalid Node");
            return;
        }
        ci.println("Existing Bridges: "+brlist.toString());
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
        Node node = Node.fromString(nodeName);
        if (node == null) {
            ci.println("Invalid Node");
            return;
        }
        status = this.deleteBridgeDomain(node, bridgeName);
        ci.println("Bridge deletion status : "+status.toString());
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
        Node node = Node.fromString(nodeName);
        if (node == null) {
            ci.println("Invalid Node");
            return;
        }
        status = this.addPort(node, bridgeName, portName, customConfigs);
        ci.println("Port creation status : "+status.toString());
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
        Node node = Node.fromString(nodeName);
        if (node == null) {
            ci.println("Invalid Node");
            return;
        }
        status = this.deletePort(node, bridgeName, portName);
        ci.println("Port deletion status : "+status.toString());
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
            } catch (NumberFormatException e) {
                ci.println("Please enter Valid Vlan");
                return;
            }
        }

        Map<ConfigConstants, Object> configs = new HashMap<ConfigConstants, Object>();
        configs.put(ConfigConstants.TYPE, "VLAN");
        configs.put(ConfigConstants.VLAN, vlan);

        Status status;
        Node node = Node.fromString(nodeName);
        if (node == null) {
            ci.println("Invalid Node");
            return;
        }
        status = this.addPort(node, bridgeName, portName, configs);
        ci.println("Port creation status : "+status.toString());
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
            logger.error("Unable to resolve " + remoteIp, e);
            ci.println("Please enter valid Remote IP Address");
            return;
        }

        Map<ConfigConstants, Object> configs = new HashMap<ConfigConstants, Object>();
        configs.put(ConfigConstants.TYPE, "TUNNEL");
        configs.put(ConfigConstants.TUNNEL_TYPE, tunnelType);
        configs.put(ConfigConstants.DEST_IP, remoteIp);

        Status status;
        Node node = Node.fromString(nodeName);
        if (node == null) {
            ci.println("Invalid Node");
            return;
        }
        status = this.addPort(node, bridgeName, portName, configs);
        ci.println("Port creation status : "+status.toString());
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
        if (force.equalsIgnoreCase("YES")) {
            forceConnect = true;
        }
        else if (force.equalsIgnoreCase("NO")) {
            forceConnect = false;
        }
        else {
            ci.println("Please enter YES or NO.");
        }
        ci.println("Current ForceConnect State : "+forceConnect);
    }

    @Override
    public String getHelp() {
        StringBuilder help = new StringBuilder();
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


    /**
     * Add a new bridge
     * @param node Node serving this configuration service
     * @param bridgeIdentifier String representation of a Bridge Connector
     * @return Bridge Connector configurations
     */
    @Override
    @Deprecated
    public Status createBridgeDomain(Node node, String bridgeIdentifier, Map<ConfigConstants, Object> configs) {
        Connection connection = connectionService.getConnection(node);
        OvsdbClient client = connection.getClient();
        Bridge bridge = client.createTypedRowWrapper(Bridge.class);
        bridge.setName(bridgeIdentifier);

        String ovsTableUuid = this.getSpecialCaseParentUUID(node, OvsVswitchdSchemaConstants.DATABASE_NAME, bridge.getSchema().getName());
        return this.insertRow(node, bridge.getSchema().getName(), ovsTableUuid, bridge.getRow());
    }

    /**
     * Create a Port Attached to a Bridge
     * Ex. ovs-vsctl add-port br0 vif0
     * @param node Node serving this configuration service
     * @param bridgeIdentifier String representation of a Bridge Domain
     * @param portIdentifier String representation of a user defined Port Name
     */
    @Override
    @Deprecated
    public Status addPort(Node node, String bridgeIdentifier, String portIdentifier,
                          Map<ConfigConstants, Object> configs) {
        Connection connection = connectionService.getConnection(node);
        OvsdbClient client = connection.getClient();

        Bridge bridge = client.getTypedRowWrapper(Bridge.class, null);
        ConcurrentMap<String, Row> rows = this.getRows(node, bridge.getSchema().getName());
        if (rows == null || rows.size() == 0) {
            return new Status(StatusCode.NOTFOUND);
        }
        for (String bridgeUuid : rows.keySet()) {
            Row bridgeRow = rows.get(bridgeUuid);
            bridge = client.getTypedRowWrapper(Bridge.class, bridgeRow);
            if (bridge.getName().equals(bridgeIdentifier)) break;
        }
        if (bridge.getName() == null || !bridge.getName().equals(bridgeIdentifier)) {
            return new Status(StatusCode.NOTFOUND);
        }

        Map<String, String> options = null;
        String type = null;
        Set<Long> tags = null;
        if (configs != null) {
            type = (String) configs.get(ConfigConstants.TYPE);
            Map<String, String> customConfigs = (Map<String, String>) configs.get(ConfigConstants.CUSTOM);
            if (customConfigs != null) {
                options = new HashMap<String, String>();
                for (String customConfig : customConfigs.keySet()) {
                    options.put(customConfig, customConfigs.get(customConfig));
                }
            }
        }

        if (type != null) {
            logger.debug("Port type : " + type);
            if (type.equalsIgnoreCase(OvsVswitchdSchemaConstants.PortType.VLAN.name())) {
                tags = new HashSet<Long>();
                tags.add(Long.parseLong((String)configs.get(ConfigConstants.VLAN)));
            }
        }

        Port port = client.createTypedRowWrapper(Port.class);
        port.setName(portIdentifier);
        if (tags != null) port.setTag(tags);
        StatusWithUuid portStatus = this.insertRow(node, port.getSchema().getName(), bridge.getUuid().toString(), port.getRow());

        if (!portStatus.isSuccess()) return portStatus;
        // Ugly hack by adding a sleep for the Monitor Update to catch up.
        // TODO : Remove this once the Select operation is in place.
        // We are currently relying on the local Cache for any GET operation and that might fail if we try to
        // fetch the last installed entry. Hence we need the Select operation to work.

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        Interface interfaceRow = client.createTypedRowWrapper(Interface.class);
        ConcurrentMap<String, Row> intfRows = this.getRows(node, interfaceRow.getSchema().getName());
        if (intfRows == null || intfRows.size() == 0) {
            return new Status(StatusCode.NOTFOUND);
        }
        for (String intfUuid : intfRows.keySet()) {
            Row intfRow = rows.get(intfUuid);
            interfaceRow = client.getTypedRowWrapper(Interface.class, intfRow);
            if (interfaceRow == null || interfaceRow.getName() == null) continue;
            if (interfaceRow.getName().equals(portIdentifier)) break;
        }
        if (interfaceRow.getName() == null || !interfaceRow.getName().equals(portIdentifier)) {
            return new Status(StatusCode.NOTFOUND);
        }

        if (type != null) {
            logger.debug("Interface type : " + type);
            if (type.equalsIgnoreCase(OvsVswitchdSchemaConstants.PortType.TUNNEL.name())) {
                interfaceRow.setType((String)configs.get(ConfigConstants.TUNNEL_TYPE));
                if (options == null) options = new HashMap<String, String>();
                options.put("remote_ip", (String)configs.get(ConfigConstants.DEST_IP));
            } else if (type.equalsIgnoreCase(OvsVswitchdSchemaConstants.PortType.PATCH.name()) ||
                       type.equalsIgnoreCase(OvsVswitchdSchemaConstants.PortType.INTERNAL.name())) {
                interfaceRow.setType(type.toLowerCase());
            }
        }
        if (options != null) {
            interfaceRow.setOptions(options);
        }

        Status intfStatus = null;
        intfStatus = this.updateRow(node, interfaceRow.getSchema().getName(), portStatus.getUuid().toString(),
                                    interfaceRow.getUuid().toString(), interfaceRow.getRow());

        if (intfStatus.isSuccess()) return portStatus;
        return intfStatus;
    }

    /**
     * Implements the OVS Connection for Managers
     *
     * @param node Node serving this configuration service
     * @param managerip String Representing IP and connection types
     */
    @SuppressWarnings("unchecked")
    @Deprecated
    public boolean setManager(Node node, String managerip) {
        Connection connection = connectionService.getConnection(node);
        OvsdbClient client = connection.getClient();
        Manager manager = client.createTypedRowWrapper(Manager.class);
        manager.setTarget(ImmutableSet.of(managerip));

        OpenVSwitch openVSwitch = connection.getClient().getTypedRowWrapper(OpenVSwitch.class, null);
        ConcurrentMap<String, Row> row = this.getRows(node, openVSwitch.getSchema().getName());
        if (row == null || row.size() == 0) {
            return false;
        }
        String ovsTableUuid = (String)row.keySet().toArray()[0];

        Status status = this.insertRow(node, manager.getSchema().getName(), ovsTableUuid, manager.getRow());
        return status.isSuccess();
    }

    @Override
    @Deprecated
    public Status addBridgeDomainConfig(Node node, String bridgeIdentfier,
            Map<ConfigConstants, Object> configs) {
        String mgmt = (String)configs.get(ConfigConstants.MGMT);
        if (mgmt != null) {
            if (setManager(node, mgmt)) return new Status(StatusCode.SUCCESS);
        }
        return new Status(StatusCode.BADREQUEST);
    }

    @Override
    @Deprecated
    public Status deletePort(Node node, String bridgeIdentifier, String portIdentifier) {
        Connection connection = connectionService.getConnection(node);
        OvsdbClient client = connection.getClient();

        Port port = client.getTypedRowWrapper(Port.class, null);
        ConcurrentMap<String, Row> rows = this.getRows(node, port.getSchema().getName());
        if (rows == null || rows.size() == 0) {
            return new Status(StatusCode.NOTFOUND);
        }
        for (String portUuid : rows.keySet()) {
            Row bridgeRow = rows.get(portUuid);
            port = client.getTypedRowWrapper(Port.class, bridgeRow);
            if (port.getName().equals(portIdentifier)) break;
        }
        if (port.getName() == null || !port.getName().equals(portIdentifier)) {
            return new Status(StatusCode.NOTFOUND);
        }
        return this.deleteRow(node, port.getSchema().getName(), port.getUuid().toString());
    }

    @Override
    @Deprecated
    public Status deleteBridgeDomain(Node node, String bridgeIdentifier) {
        Connection connection = connectionService.getConnection(node);
        OvsdbClient client = connection.getClient();

        Bridge bridge = client.getTypedRowWrapper(Bridge.class, null);
        ConcurrentMap<String, Row> rows = this.getRows(node, bridge.getSchema().getName());
        if (rows == null || rows.size() == 0) {
            return new Status(StatusCode.NOTFOUND);
        }
        for (String bridgeUuid : rows.keySet()) {
            Row bridgeRow = rows.get(bridgeUuid);
            bridge = client.getTypedRowWrapper(Bridge.class, bridgeRow);
            if (bridge.getName().equals(bridgeIdentifier)) break;
        }
        if (bridge.getName() == null || !bridge.getName().equals(bridgeIdentifier)) {
            return new Status(StatusCode.NOTFOUND);
        }
        return this.deleteRow(node, bridge.getSchema().getName(), bridge.getUuid().toString());
    }

    @Override
    public List<String> getBridgeDomains(Node node) {
        if (connectionService == null) {
            logger.error("Couldn't refer to the ConnectionService");
            return null;
        }

        Connection connection = connectionService.getConnection(node);
        Bridge bridge = connection.getClient().getTypedRowWrapper(Bridge.class, null);
        List<String> brlist = new ArrayList<String>();
        Map<String, Row> brTableCache = inventoryServiceInternal.getTableCache(node, OvsVswitchdSchemaConstants.DATABASE_NAME, bridge.getSchema().getName());
        if(brTableCache != null){
            for (String uuid : brTableCache.keySet()) {
                bridge = connection.getClient().getTypedRowWrapper(Bridge.class, brTableCache.get(uuid));
                brlist.add(bridge.getName());
            }
        }
        return brlist;
    }

    @Override
    public NodeConnector getNodeConnector(Node arg0, String arg1, String arg2) {
        return null;
    }

    @Override
    @Deprecated
    public Status addPortConfig(Node node, String bridgeIdentifier, String portIdentifier,
            Map<ConfigConstants, Object> configs) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    @Deprecated
    public Node getBridgeDomainNode(Node node, String bridgeIdentifier) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    @Deprecated
    public Map<ConfigConstants, Object> getPortConfigs(Node node, String bridgeIdentifier,
            String portIdentifier) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    @Deprecated
    public Status removeBridgeDomainConfig(Node node, String bridgeIdentifier,
            Map<ConfigConstants, Object> configs) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    @Deprecated
    public Status removePortConfig(Node node, String bridgeIdentifier, String portIdentifier,
            Map<ConfigConstants, Object> configs) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    @Deprecated
    public Map<ConfigConstants, Object> getBridgeDomainConfigs(Node node, String bridgeIdentifier) {
        // TODO Auto-generated method stub
        return null;
    }
}

