/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Brent Salisbury, Keith Burns
 */
package org.opendaylight.ovsdb.plugin.impl;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

import org.opendaylight.ovsdb.plugin.api.Status;
import org.opendaylight.ovsdb.plugin.api.StatusCode;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.error.SchemaVersionMismatchException;
import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.notation.OvsdbSet;
import org.opendaylight.ovsdb.lib.notation.ReferencedRow;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.Insert;
import org.opendaylight.ovsdb.lib.operations.Operation;
import org.opendaylight.ovsdb.lib.operations.OperationResult;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.BaseType.UuidBaseType;
import org.opendaylight.ovsdb.lib.schema.ColumnSchema;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.TableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;
import org.opendaylight.ovsdb.plugin.api.Connection;
import org.opendaylight.ovsdb.plugin.api.OvsVswitchdSchemaConstants;
import org.opendaylight.ovsdb.plugin.api.OvsdbConfigurationService;
import org.opendaylight.ovsdb.plugin.api.OvsdbConnectionService;
import org.opendaylight.ovsdb.plugin.api.OvsdbInventoryService;
import org.opendaylight.ovsdb.plugin.api.StatusWithUuid;
import org.opendaylight.ovsdb.plugin.error.OvsdbPluginException;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.Controller;
import org.opendaylight.ovsdb.schema.openvswitch.OpenVSwitch;
import org.opendaylight.ovsdb.schema.openvswitch.Port;
import org.opendaylight.ovsdb.utils.config.ConfigProperties;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ListenableFuture;

public class ConfigurationServiceImpl implements OvsdbConfigurationService
{
    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationServiceImpl.class);

    OvsdbConnectionService connectionService;
    OvsdbInventoryService ovsdbInventoryService;
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
    }

    /**
     * Function called by the dependency manager before the services exported by
     * the component are unregistered, this will be followed by a "destroy ()"
     * calls
     *
     */
    void stop() {
    }

    public void setConnectionServiceInternal(OvsdbConnectionService connectionService) {
        this.connectionService = connectionService;
    }

    public void unsetConnectionServiceInternal(OvsdbConnectionService connectionService) {
        if (this.connectionService.equals(connectionService)) {
            this.connectionService = null;
        }
    }

    public void setOvsdbInventoryService(OvsdbInventoryService ovsdbInventoryService) {
        this.ovsdbInventoryService = ovsdbInventoryService;
    }

    public void unsetInventoryServiceInternal(OvsdbInventoryService ovsdbInventoryService) {
        if (this.ovsdbInventoryService.equals(ovsdbInventoryService)) {
            this.ovsdbInventoryService = null;
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
            String tableName, Row<GenericTableSchema> row, TransactionBuilder transactionBuilder) {
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
            this.processTypedInsertTransaction(client, databaseName, "Interface", null, null, null, namedUuid, intfRow, transactionBuilder);
        }
    }

    /*
     * A common Transaction that takes in old API style Parent_uuid and inserts a mutation on
     * the parent table for the newly inserted Child.
     * Due to some additional special case(s), the Transaction is further amended by handleSpecialInsertCase
     */
    private void processTypedInsertTransaction(OvsdbClient client, String databaseName, String childTable,
                                    String parentTable, String parentUuid, String parentColumn, String namedUuid,
                                    Row<GenericTableSchema> row, TransactionBuilder transactionBuilder) {
        this.processInsertTransaction(client, databaseName, childTable, parentTable, new UUID(parentUuid), parentColumn,
                                      namedUuid, row, transactionBuilder);
        /*
         * There are a few Open_vSwitch schema specific special case handling to be done for
         * the older API (such as by inserting a mandatory Interface row automatically upon inserting
         * a Port row.
         */
        handleSpecialInsertCase(client, databaseName, childTable, row, transactionBuilder);
    }

    /*
     * TODO : Move all the Special Cases out of ConfigurationService and into the Schema specific bundles.
     * But that makes plugin more reliant on the Typed Bundles more than just API wrapper.
     * Keeping these Special Handling locally till we introduce the full schema independent APIs in the
     * plugin layer.
     */
    public String getSpecialCaseParentUUID(Node node, String databaseName, String childTableName) {
        if (!databaseName.equals(OvsVswitchdSchemaConstants.DATABASE_NAME)) {
            return null;
        }
        String[] parentColumn = OvsVswitchdSchemaConstants.getParentColumnToMutate(childTableName);
        if (parentColumn != null && parentColumn[0].equals(OvsVswitchdSchemaConstants.DATABASE_NAME)) {
            Connection connection = connectionService.getConnection(node);
            OpenVSwitch openVSwitch = connection.getClient().getTypedRowWrapper(OpenVSwitch.class, null);
            ConcurrentMap<String, Row> row = this.getRows(node, openVSwitch.getSchema().getName());
            if (row == null || row.size() == 0) {
                return null;
            }
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
    @Deprecated
    public StatusWithUuid insertRow(Node node, String tableName, String parentUuid, Row<GenericTableSchema> row) {
        String[] parentColumn = OvsVswitchdSchemaConstants.getParentColumnToMutate(tableName);
        if (parentColumn == null) {
            parentColumn = new String[]{null, null};
        }

        Connection connection = connectionService.getConnection(node);
        OvsdbClient client = connection.getClient();

        String myParentUuid = parentUuid;
        if (myParentUuid == null) {
            myParentUuid = this.getSpecialCaseParentUUID(node, OvsVswitchdSchemaConstants.DATABASE_NAME, tableName);
        }
        LOG.debug("insertRow Connection : {} Table : {} ParentTable : {} Parent Column: {} Parent UUID : {} Row : {}",
                client.getConnectionInfo(), tableName, parentColumn[0], parentColumn[1], myParentUuid, row);

        DatabaseSchema dbSchema = client.getDatabaseSchema(OvsVswitchdSchemaConstants.DATABASE_NAME);
        TransactionBuilder transactionBuilder = client.transactBuilder(dbSchema);

        String namedUuid = "Transaction_"+ tableName;
        this.processTypedInsertTransaction(client, OvsVswitchdSchemaConstants.DATABASE_NAME, tableName,
                                parentColumn[0], myParentUuid, parentColumn[1], namedUuid,
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
    @Deprecated
    public Status updateRow (Node node, String tableName, String parentUUID, String rowUUID, Row row) {
        String databaseName = OvsVswitchdSchemaConstants.DATABASE_NAME;
        this.updateRow(node, databaseName, tableName, new UUID(rowUUID), row, true);
        return new StatusWithUuid(StatusCode.SUCCESS);
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
    @Deprecated
    public Status deleteRow(Node node, String tableName, String uuid) {
        String databaseName = OvsVswitchdSchemaConstants.DATABASE_NAME;
        Connection connection = connectionService.getConnection(node);
        OvsdbClient client = connection.getClient();

        String[] parentColumn = OvsVswitchdSchemaConstants.getParentColumnToMutate(tableName);
        if (parentColumn == null) {
            parentColumn = new String[]{null, null};
        }

        LOG.debug("deleteRow : Connection : {} databaseName : {} tableName : {} Uuid : {} ParentTable : {} ParentColumn : {}",
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
            LOG.error("Error in deleteRow() {} {}", node, tableName, e);
        }

        return new Status(StatusCode.SUCCESS);
    }

    @Override
    @Deprecated
    public ConcurrentMap<String, Row> getRows(Node node, String tableName) {
        return ovsdbInventoryService.getTableCache(node, OvsVswitchdSchemaConstants.DATABASE_NAME, tableName);
    }

    @Override
    @Deprecated
    public Row getRow(Node node, String tableName, String uuid) {
        Map<String, Row> ovsTable = ovsdbInventoryService.getTableCache(node, OvsVswitchdSchemaConstants.DATABASE_NAME,  tableName);
        if (ovsTable == null) {
            return null;
        }
        return ovsTable.get(uuid);
    }

    @Override
    @Deprecated
    public List<String> getTables(Node node) {
        return this.getTables(node, OvsVswitchdSchemaConstants.DATABASE_NAME);
    }

    private InetAddress getControllerIPAddress(Connection connection) {
        InetAddress controllerIP = null;

        String addressString = ConfigProperties.getProperty(this.getClass(), "ovsdb.controller.address");

        if (addressString != null) {
            try {
                controllerIP = InetAddress.getByName(addressString);
                if (controllerIP != null) {
                    return controllerIP;
                }
            } catch (UnknownHostException e) {
                LOG.error("Host {} is invalid", addressString);
            }
        }

        addressString = ConfigProperties.getProperty(this.getClass(), "of.address");

        if (addressString != null) {
            try {
                controllerIP = InetAddress.getByName(addressString);
                if (controllerIP != null) {
                    return controllerIP;
                }
            } catch (UnknownHostException e) {
                LOG.error("Host {} is invalid", addressString);
            }
        }

        try {
            controllerIP = connection.getClient().getConnectionInfo().getLocalAddress();
            return controllerIP;
        } catch (Exception e) {
            LOG.debug("Invalid connection provided to getControllerIPAddresses", e);
        }
        return controllerIP;
    }

    private short getControllerOFPort() {
        short openFlowPort = (short) 6633;
        String portString = ConfigProperties.getProperty(this.getClass(), "of.listenPort");
        if (portString != null) {
            try {
                openFlowPort = Short.parseShort(portString);
            } catch (NumberFormatException e) {
                LOG.warn("Invalid port:{}, use default({})", portString,
                        openFlowPort);
            }
        }
        return openFlowPort;
    }

    private UUID getCurrentControllerUuid(Node node, final String controllerTableName, final String target) {
        ConcurrentMap<String, Row> rows = this.getRows(node, controllerTableName);

        if (rows != null) {
            for (Map.Entry<String, Row> entry : rows.entrySet()) {
                Controller currController = this.getTypedRow(node, Controller.class, entry.getValue());
                Column<GenericTableSchema, String> column = currController.getTargetColumn();
                String currTarget = column.getData();
                if (currTarget != null && currTarget.equalsIgnoreCase(target)) {
                    return currController.getUuid();
                }
            }
        }
        return null;
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

            String ofVersion = System.getProperty("ovsdb.of.version", OPENFLOW_13);
            switch (ofVersion) {
                case OPENFLOW_13:
                    //fall through
                default:
                    protocols.add("OpenFlow13");
                    break;
            }
            bridge.setProtocols(protocols);
            updateOperationStatus = this.updateRow(node, bridge.getSchema().getName(),
                                                   null, bridgeUUID, bridge.getRow());
            LOG.debug("Bridge {} updated to {} with Status {}", bridgeUUID,
                    protocols.toArray()[0], updateOperationStatus);

        } catch (SchemaVersionMismatchException e){
            LOG.debug(e.toString());
        }

        // If we fail to update the protocols
        if (updateOperationStatus != null && !updateOperationStatus.isSuccess()) {
            return updateOperationStatus.isSuccess();
        }

        Status status;
        UUID currControllerUuid;
        InetAddress ofControllerAddr = this.getControllerIPAddress(connection);
        short ofControllerPort = getControllerOFPort();
        String newControllerTarget = "tcp:"+ofControllerAddr.getHostAddress()+":"+ofControllerPort;
        Controller newController = connection.getClient().createTypedRowWrapper(Controller.class);
        newController.setTarget(newControllerTarget);
        final String controllerTableName = newController.getSchema().getName();

        currControllerUuid = getCurrentControllerUuid(node, controllerTableName, newControllerTarget);

        if (currControllerUuid != null) {
            bridge = connection.getClient().createTypedRowWrapper(Bridge.class);
            bridge.setController(Sets.newHashSet(currControllerUuid));
            status = this.updateRow(node, bridge.getSchema().getName(), null, bridgeUUID, bridge.getRow());
        } else {
            status = this.insertRow(node, controllerTableName, bridgeUUID, newController.getRow());
        }

        return status != null && status.isSuccess();

    }


    public Boolean setBridgeOFController(Node node, String bridgeIdentifier) {
        if (connectionService == null) {
            LOG.error("Couldn't refer to the ConnectionService");
            return false;
        }

        try{
            Connection connection = connectionService.getConnection(node);
            Bridge bridge = connection.getClient().getTypedRowWrapper(Bridge.class, null);

            Map<String, Row> brTableCache = ovsdbInventoryService.getTableCache(node, OvsVswitchdSchemaConstants.DATABASE_NAME, bridge.getSchema().getName());
            for (String uuid : brTableCache.keySet()) {
                bridge = connection.getClient().getTypedRowWrapper(Bridge.class, brTableCache.get(uuid));
                if (bridge.getName().contains(bridgeIdentifier)) {
                    return setOFController(node, uuid);
                }
            }
        } catch(Exception e) {
            LOG.error("Error in setBridgeOFController()", e);
        }
        return false;
    }

    @Override
    public <T extends TypedBaseTable<?>> String getTableName(Node node, Class<T> typedClass) {
        Connection connection = connectionService.getConnection(node);
        if (connection == null) {
            return null;
        }
        OvsdbClient client = connection.getClient();
        TypedBaseTable<?> typedTable = client.getTypedRowWrapper(typedClass, null);
        if (typedTable == null) {
            return null;
        }
        return typedTable.getSchema().getName();
    }

    @Override
    public <T extends TypedBaseTable<?>> T getTypedRow(Node node, Class<T> typedClass, Row row) {
        Connection connection = connectionService.getConnection(node);
        if (connection == null) {
            return null;
        }
        OvsdbClient client = connection.getClient();
        return (T)client.getTypedRowWrapper(typedClass, row);
    }

    @Override
    public <T extends TypedBaseTable<?>> T createTypedRow(Node node, Class<T> typedClass) {
        Connection connection = connectionService.getConnection(node);
        if (connection == null) {
            return null;
        }
        OvsdbClient client = connection.getClient();
        return client.createTypedRowWrapper(typedClass);
    }

    // SCHEMA-INDEPENDENT Configuration Service APIs

    private String getTableNameForRowUuid(Node node, String databaseName, UUID rowUuid) {
        ConcurrentMap<String, ConcurrentMap<String, Row>> cache  = ovsdbInventoryService.getCache(node, databaseName);
        if (cache == null) {
            return null;
        }
        for (String tableName : cache.keySet()) {
            ConcurrentMap<String, Row> rows = cache.get(tableName);
            if (rows.get(rowUuid.toString()) != null) {
                return tableName;
            }
        }
        return null;
    }

    private String getReferencingColumn (TableSchema<?> parentTableSchema, String childTableName) throws OvsdbPluginException {
        Map<String, ColumnSchema> columnSchemas = parentTableSchema.getColumnSchemas();
        String refColumn = null;
        for (String columnName : columnSchemas.keySet()) {
            ColumnSchema columnSchema = columnSchemas.get(columnName);
            if (columnSchema.getType().getBaseType().getClass().equals(UuidBaseType.class)) {
                UuidBaseType refType = (UuidBaseType)columnSchema.getType().getBaseType();
                if (refType.getRefTable() != null && refType.getRefTable().equalsIgnoreCase(childTableName)) {
                    if (refColumn == null) {
                        refColumn = columnName;
                    } else {
                        throw new OvsdbPluginException("Multiple Referencing Columns for "+ childTableName +" on "+ parentTableSchema.getName());
                    }
                }
            }
        }
        if (refColumn != null) {
            return refColumn;
        }
        throw new OvsdbPluginException("No Referencing Column for "+childTableName+" on "+parentTableSchema.getName());
    }
    /*
     * A common Insert Transaction convenience method that populates the TransactionBuilder with insert operation
     * for a Child Row and also mutates the parent row with the UUID of the inserted Child.
     */
    private void processInsertTransaction(OvsdbClient client, String databaseName, String childTable,
                                    String parentTable, UUID parentUuid, String parentColumn, String namedUuid,
                                    Row<GenericTableSchema> row,
                                    TransactionBuilder transactionBuilder) {
        // Insert the row as the first transaction entry
        DatabaseSchema dbSchema = client.getDatabaseSchema(databaseName);
        TableSchema<GenericTableSchema> childTableSchema = dbSchema.table(childTable, GenericTableSchema.class);
        transactionBuilder.add(op.insert(childTableSchema, row)
                        .withId(namedUuid));

        // Followed by the Mutation
        if (parentColumn != null) {
            TableSchema<GenericTableSchema> parentTableSchema = dbSchema.table(parentTable, GenericTableSchema.class);
            ColumnSchema<GenericTableSchema, UUID> parentColumnSchema = parentTableSchema.column(parentColumn, UUID.class);
            ColumnSchema<GenericTableSchema, UUID> _uuid = parentTableSchema.column("_uuid", UUID.class);

            transactionBuilder
                .add(op.mutate(parentTableSchema)
                        .addMutation(parentColumnSchema, Mutator.INSERT, new UUID(namedUuid))
                        .where(_uuid.opEqual(parentUuid))
                        .build());
        }
    }

    /**
     * insert a Row in a Table of a specified Database Schema.
     *
     * This method can insert just a single Row specified in the row parameter.
     * But {@link #insertTree(Node, String, String, UUID, Row) insertTree}
     * can insert a hierarchy of rows with parent-child relationship.
     *
     * @param node OVSDB Node
     * @param databaseName Database Name that represents the Schema supported by the node.
     * @param tableName Table on which the row is inserted
     * @param parentTable Name of the Parent Table to which this operation will result in attaching/mutating.
     * @param parentUuid UUID of a Row in parent table to which this operation will result in attaching/mutating.
     * @param parentColumn Name of the Column in the Parent Table to be mutated with the UUID that results from the insert operation.
     * @param row Row of table Content to be inserted
     * @throws OvsdbPluginException Any failure during the insert transaction will result in a specific exception.
     * @return UUID of the inserted Row
     */
    @Override
    public UUID insertRow(Node node, String databaseName, String tableName, String parentTable, UUID parentUuid,
                          String parentColumn, Row<GenericTableSchema> row) throws OvsdbPluginException {
        Connection connection = connectionService.getConnection(node);
        OvsdbClient client = connection.getClient();
        DatabaseSchema dbSchema = client.getDatabaseSchema(databaseName);
        TableSchema<GenericTableSchema> tableSchema = dbSchema.table(tableName, GenericTableSchema.class);

        Row<GenericTableSchema> processedRow = this.insertTree(node, databaseName, tableName, parentTable, parentUuid, parentColumn, row);

        ColumnSchema<GenericTableSchema, UUID> _uuid = tableSchema.column("_uuid", UUID.class);
        Column<GenericTableSchema, UUID> uuid = processedRow.getColumn(_uuid);
        return uuid.getData();
    }

    /**
     * insert a Row in a Table of a specified Database Schema. This is a convenience method on top of
     * {@link insertRow(Node, String, String, String, UUID, String, Row) insertRow}
     * which assumes that OVSDB schema implementation that corresponds to the databaseName will provide
     * the necessary service to populate the Parent Table Name and Parent Column Name.
     *
     * This method can insert just a single Row specified in the row parameter.
     * But {@link #insertTree(Node, String, String, UUID, Row) insertTree}
     * can insert a hierarchy of rows with parent-child relationship.
     *
     * @param node OVSDB Node
     * @param databaseName Database Name that represents the Schema supported by the node.
     * @param tableName Table on which the row is inserted
     * @param parentRowUuid UUID of the parent table to which this operation will result in attaching/mutating.
     * @param row Row of table Content to be inserted
     * @throws OvsdbPluginException Any failure during the insert transaction will result in a specific exception.
     * @return UUID of the inserted Row
     */
    @Override
    public UUID insertRow(Node node, String databaseName, String tableName,
            UUID parentRowUuid, Row<GenericTableSchema> row)
            throws OvsdbPluginException {
        return this.insertRow(node, databaseName, tableName, null, parentRowUuid, null, row);
    }

    /**
     * inserts a Tree of Rows in multiple Tables that has parent-child relationships referenced through the OVSDB schema's refTable construct
     *
     * @param node OVSDB Node
     * @param databaseName Database Name that represents the Schema supported by the node.
     * @param tableName Table on which the row is inserted
     * @param parentTable Name of the Parent Table to which this operation will result in attaching/mutating.
     * @param parentUuid UUID of a Row in parent table to which this operation will result in attaching/mutating.
     * @param parentColumn Name of the Column in the Parent Table to be mutated with the UUID that results from the insert operation.
     * @param row Row Tree with parent-child relationships via column of type refTable.
     * @throws OvsdbPluginException Any failure during the insert transaction will result in a specific exception.
     * @return Returns the row tree with the UUID of every inserted Row populated in the _uuid column of every row in the tree
     */
    @Override
    public Row<GenericTableSchema> insertTree(Node node, String databaseName, String tableName, String parentTable, UUID parentUuid,
                                              String parentColumn, Row<GenericTableSchema> row) throws OvsdbPluginException {
        Connection connection = connectionService.getConnection(node);
        OvsdbClient client = connection.getClient();

        if (databaseName == null || tableName == null) {
            throw new OvsdbPluginException("databaseName, tableName and parentUuid are Mandatory Parameters");
        }

        if (parentTable == null && parentUuid != null) {
            parentTable = this.getTableNameForRowUuid(node, databaseName, parentUuid);
        }

        if (parentColumn == null && parentTable != null) {
            DatabaseSchema dbSchema = client.getDatabaseSchema(databaseName);
            TableSchema<GenericTableSchema> parentTableSchema = dbSchema.table(parentTable, GenericTableSchema.class);
            parentColumn = this.getReferencingColumn(parentTableSchema, tableName);
        }

        LOG.debug("insertTree Connection : {} Table : {} ParentTable : {} Parent Column: {} Parent UUID : {} Row : {}",
                client.getConnectionInfo(), tableName, parentTable, parentColumn, parentUuid, row);

        Map<UUID, Map.Entry<String, Row<GenericTableSchema>>> referencedRows = Maps.newConcurrentMap();
        extractReferencedRows(node, databaseName, row, referencedRows, 0);
        DatabaseSchema dbSchema = client.getDatabaseSchema(OvsVswitchdSchemaConstants.DATABASE_NAME);
        TransactionBuilder transactionBuilder = client.transactBuilder(dbSchema);

        String namedUuid = "Transaction_"+ tableName;
        this.processInsertTransaction(client, databaseName, tableName, parentTable, parentUuid,
                                      parentColumn, namedUuid, row, transactionBuilder);

        int referencedRowsInsertIndex = transactionBuilder.getOperations().size();
        // Insert Referenced Rows
        if (referencedRows != null) {
            for (UUID refUuid : referencedRows.keySet()) {
                Map.Entry<String, Row<GenericTableSchema>> referencedRow = referencedRows.get(refUuid);
                TableSchema<GenericTableSchema> refTableSchema = dbSchema.table(referencedRow.getKey(), GenericTableSchema.class);
                transactionBuilder.add(op.insert(refTableSchema, referencedRow.getValue())
                                .withId(refUuid.toString()));
            }
        }

        ListenableFuture<List<OperationResult>> results = transactionBuilder.execute();
        List<OperationResult> operationResults;
        try {
            operationResults = results.get();
            if (operationResults.isEmpty() || (transactionBuilder.getOperations().size() != operationResults.size())) {
                throw new OvsdbPluginException("Insert Operation Failed");
            }
            for (OperationResult result : operationResults) {
                if (result.getError() != null) {
                    throw new OvsdbPluginException("Insert Operation Failed with Error : " + result.getError());
                }
            }
            return getNormalizedRow(dbSchema, tableName, row, referencedRows, operationResults, referencedRowsInsertIndex);
        } catch (InterruptedException | ExecutionException e) {
            throw new OvsdbPluginException("Exception : "+e.getLocalizedMessage());
        }
    }

    /**
     * inserts a Tree of Rows in multiple Tables that has parent-child relationships referenced through the OVSDB schema's refTable construct.
     * This is a convenience method on top of {@link #insertTree(Node, String, String, String, UUID, String, Row) insertTree}
     *
     * @param node OVSDB Node
     * @param databaseName Database Name that represents the Schema supported by the node.
     * @param tableName Table on which the row is inserted
     * @param parentRowUuid UUID of a Row in parent table to which this operation will result in attaching/mutating.
     * @param row Row Tree with parent-child relationships via column of type refTable.
     * @throws OvsdbPluginException Any failure during the insert transaction will result in a specific exception.
     * @return Returns the row tree with the UUID of every inserted Row populated in the _uuid column of every row in the tree
     */
    @Override
    public Row<GenericTableSchema> insertTree(Node node, String databaseName,
            String tableName, UUID parentRowUuid, Row<GenericTableSchema> row)
            throws OvsdbPluginException {
        return this.insertTree(node, databaseName, tableName, null, parentRowUuid, null, row);
    }

    /**
     * Convenience method that helps insertTree to extract Rows that are referenced directly from within a primary row
     * to be inserted. These referenced rows are *NOT* defined in the OVSDB specification. But, we felt that from a northbound
     * application standpoint, having such an option is useful and our implementation supports it for applications to make use of.
     * In short, whichever ColumnSchema is based on an UUID (refered by RefTable in schema), applications can directly insert an
     * entire row and this method will help navigate it through and identify such cases.
     * After identifying these Referenced Rows, it will modify the primary row with Named UUIDs and fill out the referencedRows
     * Map structure so that insertTree can insert all the Rows defined in this Tree of rows in a single transaction with automatic
     * Mutation on the parent rows.
     *
     * @param node OVSDB Node
     * @param dbName Database Name that represents the Schema supported by the node.
     * @param row Row Tree with parent-child relationships via column of type refTable.
     * @param referencedRows Map of Named-UUID to the actual referenced row (with RefTable)
     * @param namedUuidSuffix Named UUID must be unique for every new Row insert within a given transaction.
     *        This index will help to retain the uniqueness.
     */
    private void extractReferencedRows(Node node, String dbName, Row<GenericTableSchema> row,
                                       Map<UUID, Map.Entry<String, Row<GenericTableSchema>>> referencedRows,
                                       int namedUuidSuffix) {
        OvsdbClient client = connectionService.getConnection(node).getClient();
        Collection<Column<GenericTableSchema, ?>> columns = row.getColumns();
        for (Column column : columns) {
            if (column.getData() != null) {
                if (column.getData() instanceof ReferencedRow) {
                    ReferencedRow refRowObject = (ReferencedRow)column.getData();
                    UUID refUuid = new UUID("NamedUuid"+namedUuidSuffix++);
                    column.setData(refUuid);
                    try {
                        DatabaseSchema dbSchema = client.getSchema(dbName).get();
                        GenericTableSchema schema = dbSchema.table(refRowObject.getRefTable(), GenericTableSchema.class);
                        Row<GenericTableSchema> refRow = schema.createRow((ObjectNode)refRowObject.getJsonNode());
                        referencedRows.put(refUuid, new AbstractMap.SimpleEntry<>(refRowObject.getRefTable(), refRow));
                        extractReferencedRows(node, dbName, refRow, referencedRows, namedUuidSuffix);
                    } catch (InterruptedException | ExecutionException e) {
                        LOG.error("Exception while extracting multi-level Row references " + e.getLocalizedMessage());
                    }
                } else if (column.getData() instanceof OvsdbSet) {
                    OvsdbSet<Object> setObject = (OvsdbSet<Object>)column.getData();
                    OvsdbSet<Object> modifiedSet = new OvsdbSet<>();
                    for (Object obj : setObject) {
                        if (obj instanceof ReferencedRow) {
                            ReferencedRow refRowObject = (ReferencedRow)obj;
                            UUID refUuid = new UUID("NamedUuid"+namedUuidSuffix++);
                            modifiedSet.add(refUuid);
                            try {
                                DatabaseSchema dbSchema = client.getSchema(dbName).get();
                                GenericTableSchema schema = dbSchema.table(refRowObject.getRefTable(), GenericTableSchema.class);
                                Row<GenericTableSchema> refRow = schema.createRow((ObjectNode)refRowObject.getJsonNode());
                                referencedRows.put(refUuid, new AbstractMap.SimpleEntry<>(refRowObject.getRefTable(), refRow));
                                extractReferencedRows(node, dbName, refRow, referencedRows, namedUuidSuffix);
                            } catch (InterruptedException | ExecutionException e) {
                                LOG.error("Exception while extracting multi-level Row references " + e.getLocalizedMessage());
                            }
                        } else {
                            modifiedSet.add(obj);
                        }
                    }
                    column.setData(modifiedSet);
                }
            }
        }
    }

    /**
     * getNormalizedRow normalizes the Row from a namedUuid Space as defined in extractReferencedRows to the actual Uuid as created
     * by the Ovsdb-server. In order to perform this normalization, it processes the operation results for a corresponding Transaction
     * where the referenced rows are inserted along with the Primary row. It changes the named-Uuid to the actual Uuid before returning
     * the Row to the application.
     *
     * @param dbSchema Database Schema supported by the node.
     * @param row Row Tree with parent-child relationships via column of type refTable.
     * @param tableName Table on which the row is inserted
     * @param referencedRows Map of Named-UUID to the actual referenced row (with RefTable)
     * @param operationResults Operation Results returned by ovsdb-server for the insertTree transaction
     * @param referencedRowsInsertIndex Starting index in OperationResults from which the ReferencedRow insert results begin.
     * @return
     */
    private Row<GenericTableSchema> getNormalizedRow(DatabaseSchema dbSchema, String tableName, Row<GenericTableSchema> row,
                                                     Map<UUID, Map.Entry<String, Row<GenericTableSchema>>> referencedRows,
                                                     List<OperationResult> operationResults, int referencedRowsInsertIndex) {
        UUID primaryRowUuid = operationResults.get(0).getUuid();
        TableSchema<GenericTableSchema> primaryRowTableSchema = dbSchema.table(tableName, GenericTableSchema.class);
        ColumnSchema<GenericTableSchema, UUID> uuid = primaryRowTableSchema.column("_uuid", UUID.class);
        if (uuid != null) {
            Column<GenericTableSchema, UUID> uuidColumn = new Column<>(uuid, primaryRowUuid);
            row.addColumn("_uuid", uuidColumn);
        }

        if (referencedRows != null) {
            Collection<Column<GenericTableSchema, ?>> columns = row.getColumns();
            Object[] rowKeys = referencedRows.keySet().toArray();
            for (int idx = 0; idx < rowKeys.length; idx++) {
                UUID refUuid = (UUID) rowKeys[idx];
                for (Column column : columns) {
                    if (column.getData() != null) {
                        if ((column.getData() instanceof UUID) && column.getData().equals(refUuid)) {
                            column.setData(operationResults.get(referencedRowsInsertIndex + idx).getUuid());
                        } else if ((column.getData() instanceof OvsdbSet) && ((OvsdbSet)column.getData()).contains(refUuid)) {
                            OvsdbSet<UUID> refSet = (OvsdbSet<UUID>)column.getData();
                            refSet.remove(refUuid);
                            refSet.add(operationResults.get(referencedRowsInsertIndex + idx).getUuid());
                        }
                    }
                }
            }
        }
        return row;
    }

    @Override
    public Row<GenericTableSchema> updateRow(Node node, String databaseName,
            String tableName, UUID rowUuid, Row<GenericTableSchema> row,
            boolean overwrite) {
        Connection connection = connectionService.getConnection(node);
        OvsdbClient client = connection.getClient();

        LOG.debug("updateRow : Connection : {} databaseName : {} tableName : {} rowUUID : {} row : {}",
                client.getConnectionInfo(), databaseName, tableName, rowUuid, row.toString());
        try{
            DatabaseSchema dbSchema = client.getDatabaseSchema(databaseName);
            TransactionBuilder transactionBuilder = client.transactBuilder(dbSchema);
            TableSchema<GenericTableSchema> tableSchema = dbSchema.table(tableName, GenericTableSchema.class);
            ColumnSchema<GenericTableSchema, UUID> uuid = tableSchema.column("_uuid", UUID.class);
            transactionBuilder.add(op.update(tableSchema, row)
                    .where(uuid.opEqual(rowUuid))
                    .build());

            ListenableFuture<List<OperationResult>> results = transactionBuilder.execute();
            List<OperationResult> operationResults = results.get();
            for (OperationResult result : operationResults) {
                if (result.getError() != null) {
                    throw new OvsdbPluginException("Error updating row : " + result.getError() +
                                                   " Details: " + result.getDetails());
                }
            }
            if (operationResults.isEmpty() || (transactionBuilder.getOperations().size() != operationResults.size())) {
                throw new OvsdbPluginException("Failed to update row. Please check OVS logs for more info.");
            }

            return this.getRow(node, databaseName, tableName, rowUuid);
        } catch(Exception e){
            throw new OvsdbPluginException("Error updating row due to an exception "+ e.getMessage());
        }
    }

    @Override
    public void deleteRow(Node node, String databaseName, String tableName, String parentTable, UUID parentRowUuid,
            String parentColumn, UUID rowUuid) {
        Connection connection = connectionService.getConnection(node);
        OvsdbClient client = connection.getClient();

        if (parentTable == null && parentRowUuid != null) {
            parentTable = this.getTableNameForRowUuid(node, databaseName, parentRowUuid);
        }

        String myParentColumn = parentColumn;
        if (myParentColumn == null && parentTable != null) {
            DatabaseSchema dbSchema = client.getDatabaseSchema(databaseName);
            TableSchema<GenericTableSchema> parentTableSchema = dbSchema.table(parentTable, GenericTableSchema.class);
            myParentColumn = this.getReferencingColumn(parentTableSchema, tableName);
        }

        LOG.debug("deleteRow : Connection : {} databaseName : {} tableName : {} Uuid : {} ParentTable : {} ParentColumn : {}",
                client.getConnectionInfo(), databaseName, tableName, rowUuid, parentTable, myParentColumn);

        DatabaseSchema dbSchema = client.getDatabaseSchema(databaseName);
        TransactionBuilder transactionBuilder = client.transactBuilder(dbSchema);
        this.processDeleteTransaction(client, databaseName, tableName,
                                      parentTable, myParentColumn, rowUuid.toString(), transactionBuilder);

        ListenableFuture<List<OperationResult>> results = transactionBuilder.execute();
        List<OperationResult> operationResults;
        try {
            operationResults = results.get();
            if (operationResults.isEmpty() || (transactionBuilder.getOperations().size() != operationResults.size())) {
                throw new OvsdbPluginException("Delete Operation Failed");
            }
            for (OperationResult result : operationResults) {
                if (result.getError() != null) {
                    throw new OvsdbPluginException("Delete Operation Failed with Error : " + result.getError());
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Error in deleteRow() {} {} {} {}", node, databaseName, tableName, parentTable, e);
        }
    }

    @Override
    public void deleteRow(Node node, String databaseName, String tableName, UUID rowUuid) {
        this.deleteRow(node, databaseName, tableName, null, null, null, rowUuid);
    }

    @Override
    public Row<GenericTableSchema> getRow(Node node, String databaseName,
            String tableName, UUID uuid) {
        ConcurrentMap<UUID, Row<GenericTableSchema>> rows = this.getRows(node, databaseName, tableName);
        if (rows != null) {
            return rows.get(uuid);
        }
        return null;
    }

    @Override
    public ConcurrentMap<UUID, Row<GenericTableSchema>> getRows(Node node,
            String databaseName, String tableName) throws OvsdbPluginException {
        ConcurrentMap<String, Row> ovsTable = ovsdbInventoryService.getTableCache(node, databaseName, tableName);
        if (ovsTable == null) {
            return null;
        }
        ConcurrentMap<UUID, Row<GenericTableSchema>> tableDB = Maps.newConcurrentMap();
        for (String uuidStr : ovsTable.keySet()) {
            tableDB.put(new UUID(uuidStr), ovsTable.get(uuidStr));
        }
        return tableDB;
    }

    @Override
    public ConcurrentMap<UUID, Row<GenericTableSchema>> getRows(Node node,
            String databaseName, String tableName, String fiqlQuery) {
        return this.getRows(node, databaseName, tableName);
    }

    @Override
    public List<String> getTables(Node node, String databaseName) {
        ConcurrentMap<String, ConcurrentMap<String, Row>> cache  = ovsdbInventoryService.getCache(node, databaseName);
        if (cache == null) {
            return null;
        } else {
            return new ArrayList<>(cache.keySet());
        }
    }
}
