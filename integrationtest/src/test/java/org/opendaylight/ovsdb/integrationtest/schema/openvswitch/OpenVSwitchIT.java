/*
 *  Copyright (C) 2014 Red Hat, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 *  Authors : Sam Hague, Matt Oswalt
 */
package org.opendaylight.ovsdb.integrationtest.schema.openvswitch;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;
import static org.opendaylight.ovsdb.lib.operations.Operations.op;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.propagateSystemProperty;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.ovsdb.integrationtest.ConfigurationBundles;
import org.opendaylight.ovsdb.integrationtest.OvsdbIntegrationTestBase;
import org.opendaylight.ovsdb.lib.MonitorCallBack;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.error.SchemaVersionMismatchException;
import org.opendaylight.ovsdb.lib.message.*;
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.notation.Version;
import org.opendaylight.ovsdb.lib.operations.OperationResult;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.TableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;
import org.opendaylight.ovsdb.schema.openvswitch.*;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;
import org.ops4j.pax.exam.util.PathUtils;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.*;
import com.google.common.util.concurrent.ListenableFuture;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
public class OpenVSwitchIT extends OvsdbIntegrationTestBase {
    private static final Logger LOG = LoggerFactory.getLogger(OpenVSwitchIT.class);
    private static boolean monitorReady = false;
    private static boolean schemaSupported = false;
    private static final String TEST_BRIDGE_NAME = "br_test";
    private static final String TEST_MANAGER_UUID_STR = "managerUuidName";
    private static final String ASSERT_TRANS_ERROR = "Transaction should not have errors";
    private static final String ASSERT_TRANS_RESULT_EMPTY = "Transaction should not be empty";
    private static final String ASSERT_TRANS_OPERATION_COUNT = "Transaction should match number of operations";
    private static final String ASSERT_TRANS_UUID = "Transaction UUID should not be null";
    private Version schemaVersion;
    private UUID testBridgeUuid = null;
    private UUID testController1Uuid = null;
    private UUID testController2Uuid = null;
    private UUID testFlowSampleCollectorSetUuid = null;
    private UUID testFlowTableUuid = null;
    private UUID testInterfaceUuid = null;
    private UUID testIpfixUuid = null;
    private UUID testManagerUuid = null;
    private UUID testMirrorUuid = null;
    private UUID testNetFlowUuid = null;
    private UUID testPortUuid = null;
    private UUID testQosUuid = null;
    private UUID testQueueUuid = null;
    private UUID testSFlowUuid = null;
    private UUID testSslUuid = null;
    private Version flowSampleCollectorSetFromVersion = Version.fromString("7.1.0");
    private Version flowTableFromVersion = Version.fromString("6.5.0");
    private Version prefixesAddedVersion = Version.fromString("7.4.0");
    private Version externalIdAddedVerson = Version.fromString("7.5.0");
    private Version ipfixFromVersion = Version.fromString("7.1.0");
    private Version ipfixCacheFromVersion = Version.fromString("7.3.0");

    private static Map<String, Map<UUID, Row>> tableCache = new HashMap<>();
    private static Map<String, Map<UUID, Row>> getTableCache () {
        return tableCache;
    }

    private static OvsdbClient ovsdbClient;
    private OvsdbClient getClient () {
        return ovsdbClient;
    }

    private static DatabaseSchema dbSchema;
    private DatabaseSchema getDbSchema () {
        return dbSchema;
    }

    @Inject
    private BundleContext bc;

    @Configuration
    public Option[] config() throws Exception {
        return options(
                systemProperty("logback.configurationFile").value(
                        "file:" + PathUtils.getBaseDir()
                                + "/src/test/resources/logback.xml"
                ),
                // To start OSGi console for inspection remotely
                systemProperty("osgi.console").value("2401"),

                propagateSystemProperty("ovsdbserver.ipaddress"),
                propagateSystemProperty("ovsdbserver.port"),

                ConfigurationBundles.mdsalBundles(),
                ConfigurationBundles.controllerBundles(),
                ConfigurationBundles.ovsdbLibraryBundles(),
                ConfigurationBundles.ovsdbDefaultSchemaBundles()
        );
    }

    @Before
    public void setUp () throws ExecutionException, InterruptedException, IOException {
        areWeReady(bc);
        assertTrue(OPEN_VSWITCH_SCHEMA + " is required.", checkSchema(OPEN_VSWITCH_SCHEMA));
        assertTrue("Failed to monitor tables", monitorTables());
        schemaVersion = getClient().getDatabaseSchema(OPEN_VSWITCH_SCHEMA).getVersion();
        LOG.info("{} schema version = {}", OPEN_VSWITCH_SCHEMA, schemaVersion);
    }

    public boolean checkSchema (String schema) {
        if (schemaSupported) {
            LOG.info("Schema ({}) is supported", schema);
            return true;
        }
        try {
            ovsdbClient = getTestConnection();
            assertNotNull("Invalid Client. Check connection params", ovsdbClient);
            //Thread.sleep(3000); // Wait for a few seconds to get the Schema exchange done
            if (isSchemaSupported(ovsdbClient, schema)) {
                dbSchema = ovsdbClient.getSchema(schema).get();
                assertNotNull(dbSchema);
                LOG.info("{} schema in {} with tables: {}",
                        schema, ovsdbClient.getConnectionInfo(), dbSchema.getTables());
                schemaSupported = true;
                return true;
            }
        } catch (Exception e) {
            LOG.error("Error checking schema", e);
            fail("Exception : "+e.getMessage());
        }

        LOG.info("Schema ({}) is not supported", schema);
        return false;
    }

    public UUID getOpenVSwitchTableUuid (OvsdbClient ovs, Map<String, Map<UUID, Row>> tableCache) {
        OpenVSwitch openVSwitch = getClient().getTypedRowWrapper(OpenVSwitch.class, null);
        Map<UUID, Row> ovsTable = tableCache.get(openVSwitch.getSchema().getName());
        if (ovsTable != null) {
            if (ovsTable.keySet().size() >= 1) {
                return (UUID)ovsTable.keySet().toArray()[0];
            }
        }
        return null;
    }

    public boolean isSchemaSupported (OvsdbClient client, String schema) throws ExecutionException, InterruptedException {
        ListenableFuture<List<String>> databases = client.getDatabases();
        List<String> dbNames = databases.get();
        assertNotNull(dbNames);
        if (dbNames.contains(schema)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * As per RFC 7047, section 4.1.5, if a Monitor request is sent without any columns, the update response will not include
     * the _uuid column.
     * ----------------------------------------------------------------------------------------------------------------------------------
     * Each <monitor-request> specifies one or more columns and the manner in which the columns (or the entire table) are to be monitored.
     * The "columns" member specifies the columns whose values are monitored. It MUST NOT contain duplicates.
     * If "columns" is omitted, all columns in the table, except for "_uuid", are monitored.
     * ----------------------------------------------------------------------------------------------------------------------------------
     * In order to overcome this limitation, this method
     *
     * @return MonitorRequest that includes all the Bridge Columns including _uuid
     */
    public <T extends TypedBaseTable<GenericTableSchema>> MonitorRequest<GenericTableSchema> getAllColumnsMonitorRequest (Class <T> klazz) {
        TypedBaseTable<GenericTableSchema> table = getClient().createTypedRowWrapper(klazz);
        GenericTableSchema tableSchema = table.getSchema();
        Set<String> columns = tableSchema.getColumns();
        MonitorRequestBuilder<GenericTableSchema> bridgeBuilder = MonitorRequestBuilder.builder(table.getSchema());
        for (String column : columns) {
            bridgeBuilder.addColumn(column);
        }
        return bridgeBuilder.with(new MonitorSelect(true, true, true, true)).build();
    }

    public <T extends TableSchema<T>> MonitorRequest<T> getAllColumnsMonitorRequest (T tableSchema) {
        Set<String> columns = tableSchema.getColumns();
        MonitorRequestBuilder<T> monitorBuilder = MonitorRequestBuilder.builder(tableSchema);
        for (String column : columns) {
            monitorBuilder.addColumn(column);
        }
        return monitorBuilder.with(new MonitorSelect(true, true, true, true)).build();
    }

    public boolean monitorTables () throws ExecutionException, InterruptedException, IOException {
        if (monitorReady) {
            LOG.info("Monitoring is already initialized.");
            return monitorReady;
        }

        assertNotNull(getDbSchema());

        List<MonitorRequest<GenericTableSchema>> monitorRequests = Lists.newArrayList();
        Set<String> tables = getDbSchema().getTables();
        assertNotNull("ovsdb tables should not be null", tables);

        for (String tableName : tables) {
            GenericTableSchema tableSchema = getDbSchema().table(tableName, GenericTableSchema.class);
            monitorRequests.add(this.getAllColumnsMonitorRequest(tableSchema));
        }
        TableUpdates updates = getClient().monitor(getDbSchema(), monitorRequests, new UpdateMonitor());
        assertNotNull(updates);
        this.updateTableCache(updates);

        monitorReady = true;
        LOG.info("Monitoring is initialized.");
        return monitorReady;
    }

    private void updateTableCache (TableUpdates updates) {
        for (String tableName : updates.getUpdates().keySet()) {
            Map<UUID, Row> tUpdate = getTableCache().get(tableName);
            TableUpdate update = updates.getUpdates().get(tableName);
            for (UUID uuid : (Set<UUID>)update.getRows().keySet()) {
                if (update.getNew(uuid) != null) {
                    if (tUpdate == null) {
                        tUpdate = new HashMap<>();
                        getTableCache().put(tableName, tUpdate);
                    }
                    tUpdate.put(uuid, update.getNew(uuid));
                } else {
                    tUpdate.remove(uuid);
                }
            }
        }
    }

    private class UpdateMonitor implements MonitorCallBack {
        @Override
        public void update(TableUpdates result, DatabaseSchema dbSchema) {
            updateTableCache(result);
        }

        @Override
        public void exception(Throwable t) {
            LOG.error("Exception t = " + t);
        }
    }

    public List<OperationResult> executeTransaction (TransactionBuilder transactionBuilder, String text)
            throws ExecutionException, InterruptedException {
        ListenableFuture<List<OperationResult>> results = transactionBuilder.execute();
        List<OperationResult> operationResults = results.get();
        LOG.info("{}: {}", text, operationResults);
        org.junit.Assert.assertFalse(ASSERT_TRANS_RESULT_EMPTY, operationResults.isEmpty());
        assertEquals(ASSERT_TRANS_OPERATION_COUNT, transactionBuilder.getOperations().size(), operationResults.size());
        for (OperationResult result : operationResults) {
            assertNull(ASSERT_TRANS_ERROR, result.getError());
        }
        //Thread.sleep(500); // Wait for a few seconds to ensure the cache updates
        return operationResults;
    }

    public UUID bridgeInsert () throws ExecutionException, InterruptedException {
        Bridge bridge = getClient().createTypedRowWrapper(Bridge.class);
        bridge.setName(TEST_BRIDGE_NAME);
        bridge.setStatus(ImmutableMap.of("key", "value"));
        bridge.setFloodVlans(Sets.newHashSet(34L));

        OpenVSwitch openVSwitch = getClient().createTypedRowWrapper(OpenVSwitch.class);
        openVSwitch.setBridges(Sets.newHashSet(new UUID(TEST_BRIDGE_NAME)));

        TransactionBuilder transactionBuilder = getClient().transactBuilder(getDbSchema())
                .add(op.insert(bridge.getSchema())
                        .withId(TEST_BRIDGE_NAME)
                        .value(bridge.getNameColumn()))
                .add(op.comment("Bridge: Inserting " + TEST_BRIDGE_NAME))
                .add(op.update(bridge.getSchema())
                        .set(bridge.getStatusColumn())
                        .set(bridge.getFloodVlansColumn())
                        .where(bridge.getNameColumn().getSchema().opEqual(bridge.getName()))
                        .and(bridge.getNameColumn().getSchema().opEqual(bridge.getName()))
                        .build())
                .add(op.comment("Bridge: Updating " + TEST_BRIDGE_NAME))
                .add(op.mutate(openVSwitch.getSchema())
                        .addMutation(openVSwitch.getBridgesColumn().getSchema(), Mutator.INSERT,
                                openVSwitch.getBridgesColumn().getData()))
                .add(op.comment("Open_vSwitch: Mutating " + TEST_BRIDGE_NAME));

        List<OperationResult> operationResults = executeTransaction(transactionBuilder,
                "Bridge Insert, Update and Mutate operation results");
        UUID bridgeUuid = operationResults.get(0).getUuid();
        assertNotNull(ASSERT_TRANS_UUID, bridgeUuid);
        return bridgeUuid;
    }

    public void bridgeDelete (UUID bridgeUuid) throws ExecutionException, InterruptedException {
        Bridge bridge = getClient().getTypedRowWrapper(Bridge.class, null);
        OpenVSwitch openVSwitch = getClient().getTypedRowWrapper(OpenVSwitch.class, null);

        TransactionBuilder transactionBuilder = getClient().transactBuilder(getDbSchema())
                .add(op.delete(bridge.getSchema())
                        .where(bridge.getNameColumn().getSchema().opEqual(TEST_BRIDGE_NAME))
                        .build())
                .add(op.comment("Bridge: Deleting " + TEST_BRIDGE_NAME))
                .add(op.mutate(openVSwitch.getSchema())
                        .addMutation(openVSwitch.getBridgesColumn().getSchema(), Mutator.DELETE,
                                Sets.newHashSet(bridgeUuid)))
                .add(op.comment("Open_vSwitch: Mutating " + TEST_BRIDGE_NAME + " " + bridgeUuid))
                .add(op.commit(true));

        executeTransaction(transactionBuilder, "Bridge delete operation results");
    }

    @Test
    public void testBridge () throws ExecutionException, InterruptedException {
        testBridgeUuid = bridgeInsert();

        // Verify that the local cache was updated with the remote changes
        Bridge bridge = getClient().createTypedRowWrapper(Bridge.class);
        Row bridgeRow = getTableCache().get(bridge.getSchema().getName()).get(testBridgeUuid);
        Bridge monitoredBridge = getClient().getTypedRowWrapper(Bridge.class, bridgeRow);
        assertEquals(TEST_BRIDGE_NAME, monitoredBridge.getNameColumn().getData());

        bridgeDelete(testBridgeUuid);
    }

    private void controllerInsert () throws ExecutionException, InterruptedException {
        String controllerUuidStr = "controller";
        Controller controller1 = getClient().createTypedRowWrapper(Controller.class);
        controller1.setTarget("tcp:1.1.1.1:6640");
        Controller controller2 = getClient().createTypedRowWrapper(Controller.class);
        controller2.setTarget("tcp:2.2.2.2:6640");
        Bridge bridge = getClient().getTypedRowWrapper(Bridge.class, null);

        // Insert row to Controller table with address in target column
        // Update row in Bridge table with controller uuid in controller column
        TransactionBuilder transactionBuilder = getClient().transactBuilder(getDbSchema())
                .add(op.insert(controller1.getSchema())
                        .withId(controllerUuidStr)
                        .value(controller1.getTargetColumn()))
                .add(op.comment("Controller: Inserting controller1 " + controller1.getTargetColumn().getData()))
                .add(op.mutate(bridge.getSchema())
                        .addMutation(bridge.getControllerColumn().getSchema(), Mutator.INSERT,
                                Sets.newHashSet(new UUID(controllerUuidStr)))
                        .where(bridge.getNameColumn().getSchema().opEqual(TEST_BRIDGE_NAME))
                        .build())
                .add(op.comment("Bridge: Mutating controller1 " + controller1.getTargetColumn().getData()));

        List<OperationResult> operationResults = executeTransaction(transactionBuilder,
                "Controller: Insert & Mutate operation results for controller1");
        testController1Uuid = operationResults.get(0).getUuid();
        assertNotNull(ASSERT_TRANS_UUID, testController1Uuid);

        // Verify that the local cache was updated with the remote changes
        Row controllerRow = getTableCache().get(controller1.getSchema().getName()).get(testController1Uuid);
        Controller monitoredController = getClient().getTypedRowWrapper(Controller.class, controllerRow);
        assertEquals(controller1.getTargetColumn().getData(), monitoredController.getTargetColumn().getData());

        Row bridgeRow = getTableCache().get(bridge.getSchema().getName()).get(testBridgeUuid);
        Bridge monitoredBridge = getClient().getTypedRowWrapper(Bridge.class, bridgeRow);
        assertEquals(1, monitoredBridge.getControllerColumn().getData().size());

        transactionBuilder = getClient().transactBuilder(getDbSchema())
                .add(op.insert(controller2.getSchema())
                        .withId(controllerUuidStr)
                        .value(controller2.getTargetColumn()))
                .add(op.comment("Controller: Inserting controller2 " + controller2.getTargetColumn().getData()))
                .add(op.mutate(bridge.getSchema())
                        .addMutation(bridge.getControllerColumn().getSchema(), Mutator.INSERT,
                                Sets.newHashSet(new UUID(controllerUuidStr)))
                        .where(bridge.getNameColumn().getSchema().opEqual(TEST_BRIDGE_NAME))
                        .build())
                .add(op.comment("Bridge: Mutating controller2 " + controller2.getTargetColumn().getData()));

        operationResults = executeTransaction(transactionBuilder,
                new String("Controller: Insert & Mutate operation results for controller2"));
        testController2Uuid = operationResults.get(0).getUuid();
        assertNotNull(ASSERT_TRANS_UUID, testController2Uuid);

        // Verify that the local cache was updated with the remote changes
        controllerRow = getTableCache().get(controller2.getSchema().getName()).get(testController2Uuid);
        monitoredController = getClient().getTypedRowWrapper(Controller.class, controllerRow);
        assertEquals(controller2.getTargetColumn().getData(), monitoredController.getTargetColumn().getData());

        bridgeRow = getTableCache().get(bridge.getSchema().getName()).get(testBridgeUuid);
        monitoredBridge = getClient().getTypedRowWrapper(Bridge.class, bridgeRow);
        assertEquals(2, monitoredBridge.getControllerColumn().getData().size());
    }

    private void controllerDelete () throws ExecutionException, InterruptedException {
        Controller controller = getClient().getTypedRowWrapper(Controller.class, null);
        Bridge bridge = getClient().getTypedRowWrapper(Bridge.class, null);
        DatabaseSchema dbSchema = getClient().getSchema(OPEN_VSWITCH_SCHEMA).get();

        TransactionBuilder transactionBuilder = getClient().transactBuilder(dbSchema)
                .add(op.delete(controller.getSchema())
                        .where(controller.getUuidColumn().getSchema().opEqual(testController1Uuid))
                        .build())
                .add(op.comment("Controller: Deleting " + testController1Uuid))
                .add(op.mutate(bridge.getSchema()) // Delete a controller column in the Bridge table
                        .addMutation(bridge.getControllerColumn().getSchema(), Mutator.DELETE,
                                Sets.newHashSet(testController1Uuid)))
                .add(op.comment("Bridge: Mutating " + testController1Uuid))
                .add(op.commit(true));

        executeTransaction(transactionBuilder, "Controller: Delete operation results for controller1");

        transactionBuilder
                .add(op.delete(controller.getSchema())
                        .where(controller.getUuidColumn().getSchema().opEqual(testController2Uuid))
                        .build())
                .add(op.comment("Controller: Deleting " + testController2Uuid))
                .add(op.mutate(bridge.getSchema()) // Delete a controller column in the Bridge table
                        .addMutation(bridge.getControllerColumn().getSchema(), Mutator.DELETE,
                                Sets.newHashSet(testController2Uuid)))
                .add(op.comment("Bridge: Mutating " + testController2Uuid))
                .add(op.commit(true));

        executeTransaction(transactionBuilder, "Controller: Delete operation results for controller2");
    }

    @Test
    public void testController () throws ExecutionException, InterruptedException {
        testBridgeUuid = bridgeInsert();
        controllerInsert();
        controllerDelete();
        bridgeDelete(testBridgeUuid);
    }

    @Test
    public void testFlowSampleCollectorSetTableNotSupported () {
        // Don't run this test if the table is not supported
        assumeTrue(schemaVersion.compareTo(flowSampleCollectorSetFromVersion) < 0);

        boolean isExceptionRaised = false;
        try {
            FlowSampleCollectorSet flowSampleCollectorSet = getClient().createTypedRowWrapper(FlowSampleCollectorSet.class);
        } catch (SchemaVersionMismatchException e) {
            isExceptionRaised = true;
        }
        assertTrue(isExceptionRaised);
    }

    public void flowSampleCollectorSetInsert () throws ExecutionException, InterruptedException {
        // Don't run this test if the table is not supported
        assumeTrue(schemaVersion.compareTo(flowSampleCollectorSetFromVersion) >= 0);

        FlowSampleCollectorSet flowSampleCollectorSet =
                getClient().createTypedRowWrapper(FlowSampleCollectorSet.class);
        flowSampleCollectorSet.setId(Long.valueOf(1));
        flowSampleCollectorSet.setExternalIds(ImmutableMap.of("I <3", "ovs"));
        flowSampleCollectorSet.setBridge(testBridgeUuid);

        TransactionBuilder transactionBuilder = getClient().transactBuilder(getDbSchema())
                .add(op.insert(flowSampleCollectorSet.getSchema())
                        .value(flowSampleCollectorSet.getIdColumn())
                        .value(flowSampleCollectorSet.getExternalIdsColumn())
                        .value(flowSampleCollectorSet.getBridgeColumn()))
                .add(op.comment("FlowSampleCollectorSet: Inserting"));

        List<OperationResult> operationResults = executeTransaction(transactionBuilder,
                "FlowSampleCollectorSet: Insert results");
        testFlowSampleCollectorSetUuid = operationResults.get(0).getUuid();
        assertNotNull(ASSERT_TRANS_UUID, testFlowSampleCollectorSetUuid);

        // Verify that the local cache was updated with the remote changes
        Row flowSampleCollectorSetRow = getTableCache().get(flowSampleCollectorSet.getSchema().getName())
                .get(testFlowSampleCollectorSetUuid);
        FlowSampleCollectorSet monitoredflowSampleCollectorSet =
                getClient().getTypedRowWrapper(FlowSampleCollectorSet.class, flowSampleCollectorSetRow);
        assertEquals(flowSampleCollectorSet.getIdColumn().getData(),
                monitoredflowSampleCollectorSet.getIdColumn().getData());
    }

    public void flowSampleCollectorSetDelete () throws ExecutionException, InterruptedException {
        assumeTrue(schemaVersion.compareTo(flowSampleCollectorSetFromVersion) >= 0);

        FlowSampleCollectorSet flowSampleCollectorSet = getClient().getTypedRowWrapper(FlowSampleCollectorSet.class, null);
        TransactionBuilder transactionBuilder = getClient().transactBuilder(getDbSchema())
                .add(op.delete(flowSampleCollectorSet.getSchema())
                        .where(flowSampleCollectorSet.getUuidColumn().getSchema().opEqual(testFlowSampleCollectorSetUuid))
                        .build())
                .add(op.comment("FlowSampleCollectorSet: Deleting " + testFlowSampleCollectorSetUuid))
                .add(op.commit(true));

        executeTransaction(transactionBuilder, "Bridge delete operation results");
    }

    @Test
    public void testFlowSampleCollectorSet () throws ExecutionException, InterruptedException {
        // Don't run this test if the table is not supported
        assumeTrue(schemaVersion.compareTo(flowSampleCollectorSetFromVersion) >= 0);

        testBridgeUuid = bridgeInsert();
        flowSampleCollectorSetInsert();
        flowSampleCollectorSetDelete();
        bridgeDelete(testBridgeUuid);
    }

    @Test
    public void testFlowTableTableNotSupported () {
        // Don't run this test if the table is not supported
        assumeTrue(schemaVersion.compareTo(flowTableFromVersion) < 0);

        boolean isExceptionRaised = false;
        try {
            FlowTable flowTable = getClient().createTypedRowWrapper(FlowTable.class);
        } catch (SchemaVersionMismatchException e) {
            isExceptionRaised = true;
        }
        assertTrue(isExceptionRaised);
    }

    public void flowTableInsert () throws ExecutionException, InterruptedException {
        // Don't run this test if the table is not supported
        assumeTrue(schemaVersion.compareTo(flowTableFromVersion) >= 0);

        String flowTableUuidStr = "testFlowTable";
        String tableName = "flow_table_row_name";
        String overflowPolicy = "evict";
        String groups = "group name";
        String prefixes = "wildcarding prefixes";
        Long flowLimit = 50000L;
        Map<Long, UUID> flowTableBrRef = new HashMap<>();
        flowTableBrRef.put(1L, new UUID(flowTableUuidStr));
        FlowTable flowTable = getClient().createTypedRowWrapper(FlowTable.class);
        flowTable.setName(ImmutableSet.of(tableName));
        flowTable.setOverflowPolicy(ImmutableSet.of(overflowPolicy));
        flowTable.setGroups(ImmutableSet.of(groups));
        if (schemaVersion.compareTo(prefixesAddedVersion) >= 0) {
            flowTable.setPrefixes(ImmutableSet.of(prefixes));
        }
        if (schemaVersion.compareTo(externalIdAddedVerson) >= 0) {
            flowTable.setExternalIds(ImmutableMap.of("I <3", "OVS"));
        }
        flowTable.setFlowLimit(ImmutableSet.of(flowLimit));
        Bridge bridge = getClient().getTypedRowWrapper(Bridge.class, null);

        TransactionBuilder transactionBuilder = getClient().transactBuilder(getDbSchema())
                .add(op.insert(flowTable)
                        .withId(flowTableUuidStr))
                .add(op.comment("Flowtable: Inserting " + flowTableUuidStr))
                .add(op.mutate(bridge.getSchema())
                        .addMutation(bridge.getFlowTablesColumn().getSchema(), Mutator.INSERT, flowTableBrRef)
                        .where(bridge.getNameColumn().getSchema().opEqual(TEST_BRIDGE_NAME))
                        .build())
                .add(op.comment("Bridge: Mutating " + TEST_BRIDGE_NAME));

        List<OperationResult> operationResults = executeTransaction(transactionBuilder,
                "FlowTable: Insert and Mutate results");
        testFlowTableUuid = operationResults.get(0).getUuid();
        assertNotNull(ASSERT_TRANS_UUID, testFlowTableUuid);

        // Verify that the local cache was updated with the remote changes
        Row flowTableRow = getTableCache().get(flowTable.getSchema().getName()).get(testFlowTableUuid);
        FlowTable monitoredFlowTable = getClient().getTypedRowWrapper(FlowTable.class, flowTableRow);
        assertEquals(flowTable.getNameColumn().getData(), monitoredFlowTable.getNameColumn().getData());
    }

    public void flowTableDelete () throws ExecutionException, InterruptedException {
        // Don't run this test if the table is not supported
        assumeTrue(schemaVersion.compareTo(flowTableFromVersion) >= 0);

        FlowTable flowTable = getClient().getTypedRowWrapper(FlowTable.class, null);
        Bridge bridge = getClient().getTypedRowWrapper(Bridge.class, null);

        TransactionBuilder transactionBuilder = getClient().transactBuilder(getDbSchema())
                .add(op.delete(flowTable.getSchema())
                        .where(flowTable.getUuidColumn().getSchema().opEqual(testFlowTableUuid))
                        .build())
                .add(op.comment("FlowTable: Deleting " + testFlowTableUuid))
                .add(op.mutate(bridge.getSchema())
                        .addMutation(bridge.getFlowTablesColumn().getSchema(), Mutator.DELETE,
                                Maps.newHashMap(ImmutableMap.of(1L, testFlowTableUuid))))
                .add(op.comment("Bridge: Mutating " + testFlowTableUuid))
                .add(op.commit(true));

        executeTransaction(transactionBuilder, "FlowTable delete operation results");
    }

    @Test
    public void setTestFlowTableSet () throws ExecutionException, InterruptedException {
        // Don't run this test if the table is not supported
        assumeTrue(schemaVersion.compareTo(flowTableFromVersion) >= 0);

        UUID testBridgeUuid = bridgeInsert();
        flowTableInsert();
        flowTableDelete();
        bridgeDelete(testBridgeUuid);
    }

    @Test
    public void testIpfixTableNotSupported () {
        // Don't run this test if the table is not supported
        assumeTrue(schemaVersion.compareTo(ipfixFromVersion) < 0);

        boolean isExceptionRaised = false;
        try {
            IPFIX ipfix = getClient().createTypedRowWrapper(IPFIX.class);
        } catch (SchemaVersionMismatchException e) {
            isExceptionRaised = true;
        }
        assertTrue(isExceptionRaised);
    }

    public void ipfixInsert () throws ExecutionException, InterruptedException {
        // Don't run this test if the table is not supported
        assumeTrue(schemaVersion.compareTo(ipfixFromVersion) >= 0);

        String ipfixUuidStr = "testIpfix";
        String ipfixTarget = "172.16.20.1:4739";
        Long obsDomainId = 112L;
        Long obsPointId = 358L;
        Long cacheMax = 132L;
        Long cacheTimeout = 134L;
        Long sampling = 558L;

        IPFIX ipfix = getClient().createTypedRowWrapper(IPFIX.class);
        ipfix.setTargets(ImmutableSet.of(ipfixTarget));
        ipfix.setObsDomainId(ImmutableSet.of(obsDomainId));
        ipfix.setObsPointId(ImmutableSet.of(obsPointId));
        // Only set these rows if the schema version supports it
        if (schemaVersion.compareTo(ipfixCacheFromVersion) >= 0) {
            ipfix.setCacheMaxFlows(ImmutableSet.of(cacheMax));
            ipfix.setCacheActiveTimeout(ImmutableSet.of(cacheTimeout));
        }
        ipfix.setSampling(ImmutableSet.of(sampling));
        ipfix.setExternalIds(ImmutableMap.of("I <3", "ovs"));
        Bridge bridge = getClient().getTypedRowWrapper(Bridge.class, null);

        TransactionBuilder transactionBuilder = getClient().transactBuilder(getDbSchema())
                .add(op.insert(ipfix)
                        .withId(ipfixUuidStr))
                .add(op.comment("IPFIX: Inserting " + ipfixUuidStr))
                .add(op.mutate(bridge.getSchema())
                        .addMutation(bridge.getIpfixColumn().getSchema(), Mutator.INSERT,
                                Sets.newHashSet(new UUID(ipfixUuidStr)))
                        .where(bridge.getNameColumn().getSchema().opEqual(TEST_BRIDGE_NAME))
                        .build())
                .add(op.comment("Bridge: Mutating " + ipfixUuidStr));

        List<OperationResult> operationResults = executeTransaction(transactionBuilder,
                "IPFIX: Insert and Mutate results");
        testIpfixUuid = operationResults.get(0).getUuid();
        assertNotNull(ASSERT_TRANS_UUID, testIpfixUuid);

        // Verify that the local cache was updated with the remote changes
        Row ipfixRow = getTableCache().get(ipfix.getSchema().getName()).get(testIpfixUuid);
        IPFIX monitoredIPFIX = getClient().getTypedRowWrapper(IPFIX.class, ipfixRow);
        assertEquals(testIpfixUuid, monitoredIPFIX.getUuidColumn().getData());
    }

    public void ipfixDelete () throws ExecutionException, InterruptedException {
        // Don't run this test if the table is not supported
        assumeTrue(schemaVersion.compareTo(ipfixFromVersion) >= 0);

        FlowTable flowTable = getClient().getTypedRowWrapper(FlowTable.class, null);
        Bridge bridge = getClient().getTypedRowWrapper(Bridge.class, null);

        TransactionBuilder transactionBuilder = getClient().transactBuilder(getDbSchema())
                .add(op.delete(flowTable.getSchema())
                        .where(flowTable.getUuidColumn().getSchema().opEqual(testIpfixUuid))
                        .build())
                .add(op.comment("IPFIX: Deleting " + testIpfixUuid))
                .add(op.mutate(bridge.getSchema()) // Delete a controller column in the Bridge table
                        .addMutation(bridge.getMirrorsColumn().getSchema(), Mutator.DELETE,
                                Sets.newHashSet(testIpfixUuid)))
                .add(op.comment("Bridge: Mutating " + testIpfixUuid))
                .add(op.commit(true));

        executeTransaction(transactionBuilder, "IPFIX delete operation results");
    }

    @Test
    public void testIpfix () throws ExecutionException, InterruptedException {
        // Don't run this test if the table is not supported
        assumeTrue(schemaVersion.compareTo(ipfixFromVersion) >= 0);

        testBridgeUuid = bridgeInsert();
        ipfixInsert();
        ipfixDelete();
        bridgeDelete(testBridgeUuid);
    }

    public void managerInsert() throws ExecutionException, InterruptedException {
        ImmutableMap<String, String> externalIds = ImmutableMap.of("slaveof", "themaster");
        UUID openVSwitchRowUuid = getOpenVSwitchTableUuid(getClient(), getTableCache());
        OpenVSwitch openVSwitch = getClient().getTypedRowWrapper(OpenVSwitch.class, null);
        Manager manager = getClient().createTypedRowWrapper(Manager.class);
        manager.setInactivityProbe(Sets.newHashSet(8192L));
        manager.setMaxBackoff(Sets.newHashSet(4094L));
        manager.setTarget(Sets.newHashSet("172.16.50.50:6640"));
        manager.setExternalIds(externalIds);

        TransactionBuilder transactionBuilder = getClient().transactBuilder(getDbSchema())
                .add(op.insert(manager.getSchema())
                        .withId(TEST_MANAGER_UUID_STR)
                        .value(manager.getTargetColumn())
                        .value(manager.getInactivityProbeColumn())
                        .value(manager.getMaxBackoffColumn())
                        .value(manager.getExternalIdsColumn()))
                .add(op.comment("Manager: Inserting Slave Manager " + TEST_MANAGER_UUID_STR))
                .add(op.mutate(openVSwitch.getSchema())
                        .addMutation(openVSwitch.getManagerOptionsColumn().getSchema(), Mutator.INSERT,
                                Sets.newHashSet(new UUID(TEST_MANAGER_UUID_STR)))
                        .where(openVSwitch.getUuidColumn().getSchema().opEqual(openVSwitchRowUuid))
                        .build())
                .add(op.comment("Open_vSwitch: Mutating " + TEST_MANAGER_UUID_STR));

        List<OperationResult> operationResults = executeTransaction(transactionBuilder,
                "Manager: Insert & Mutate operation results");
        testManagerUuid = operationResults.get(0).getUuid();
        assertNotNull(ASSERT_TRANS_UUID, testManagerUuid);

        // Verify that the local cache was updated with the remote changes
        Row managerRow = getTableCache().get(manager.getSchema().getName()).get(testManagerUuid);
        Manager monitoredManager = getClient().getTypedRowWrapper(Manager.class, managerRow);
        assertEquals(externalIds, monitoredManager.getExternalIdsColumn().getData());
    }

    public void managerDelete () throws ExecutionException, InterruptedException {
        Manager manager = getClient().getTypedRowWrapper(Manager.class, null);
        OpenVSwitch openVSwitch = getClient().getTypedRowWrapper(OpenVSwitch.class, null);

        TransactionBuilder transactionBuilder = getClient().transactBuilder(getDbSchema())
                .add(op.delete(manager.getSchema())
                        .where(manager.getUuidColumn().getSchema().opEqual(testManagerUuid))
                        .build())
                .add(op.comment("Manager: Deleting " + TEST_MANAGER_UUID_STR))
                .add(op.mutate(openVSwitch.getSchema())
                        .addMutation(openVSwitch.getManagerOptionsColumn().getSchema(), Mutator.DELETE,
                                Sets.newHashSet(testManagerUuid)))
                .add(op.comment("Open_vSwitch: Mutating " + TEST_MANAGER_UUID_STR + " " + testManagerUuid))
                .add(op.commit(true));

        executeTransaction(transactionBuilder, "Manager: Delete operation results");
    }

    @Test
    public void testManager () throws ExecutionException, InterruptedException {
        managerInsert();
        managerDelete();
    }

    public void mirrorInsert () throws ExecutionException, InterruptedException {
        String mirrorUuidStr = "testMirror";
        String mirrorName = "my_name_is_mirror";
        Long outputVid = 1024L;
        Long selectVid = Long.valueOf(2048);

        Mirror mirror = getClient().createTypedRowWrapper(Mirror.class);
        mirror.setName(ImmutableSet.of(mirrorName));
        mirror.setExternalIds(ImmutableMap.of("overlays", "ftw"));
        mirror.setOutputVlan(ImmutableSet.of(outputVid));
        mirror.setSelectVlan(ImmutableSet.of(selectVid));
        mirror.setExternalIds(ImmutableMap.of("reading", "urmail"));
        Bridge bridge = getClient().getTypedRowWrapper(Bridge.class, null);

        TransactionBuilder transactionBuilder = getClient().transactBuilder(getDbSchema())
                .add(op.insert(mirror.getSchema())
                        .withId(mirrorUuidStr)
                        .value(mirror.getNameColumn())
                        .value(mirror.getExternalIdsColumn()))
                .add(op.comment("Mirror: Inserting " + mirrorUuidStr))
                .add(op.mutate(bridge.getSchema())
                        .addMutation(bridge.getMirrorsColumn().getSchema(), Mutator.INSERT,
                                Sets.newHashSet(new UUID(mirrorUuidStr)))
                        .where(bridge.getNameColumn().getSchema().opEqual(TEST_BRIDGE_NAME))
                        .build())
                .add(op.comment("Bridge: Mutating " + TEST_BRIDGE_NAME));

        List<OperationResult> operationResults = executeTransaction(transactionBuilder,
                "Insert and Mutate operation results for Mirror");
        testMirrorUuid = operationResults.get(0).getUuid();
        assertNotNull(ASSERT_TRANS_UUID, testMirrorUuid);

        // Verify that the local cache was updated with the remote changes
        Row mirrorRow = getTableCache().get(mirror.getSchema().getName()).get(testMirrorUuid);
        Mirror monitoredMirror = getClient().getTypedRowWrapper(Mirror.class, mirrorRow);
        assertEquals(mirror.getExternalIdsColumn().getData(), monitoredMirror.getExternalIdsColumn().getData());
    }

    private void mirrorDelete () throws ExecutionException, InterruptedException {
        Mirror mirror = getClient().getTypedRowWrapper(Mirror.class, null);
        Bridge bridge = getClient().getTypedRowWrapper(Bridge.class, null);
        DatabaseSchema dbSchema = getClient().getSchema(OPEN_VSWITCH_SCHEMA).get();

        TransactionBuilder transactionBuilder = getClient().transactBuilder(dbSchema)
                .add(op.delete(mirror.getSchema())
                        .where(mirror.getUuidColumn().getSchema().opEqual(testMirrorUuid))
                        .build())
                .add(op.comment("Mirror: Deleting " + testMirrorUuid))
                .add(op.mutate(bridge.getSchema()) // Delete a controller column in the Bridge table
                        .addMutation(bridge.getMirrorsColumn().getSchema(), Mutator.DELETE,
                                Sets.newHashSet(testMirrorUuid)))
                .add(op.comment("Bridge: Mutating " + testMirrorUuid))
                .add(op.commit(true));

        executeTransaction(transactionBuilder, "Mirror: Delete operation results");
    }

    @Test
    public void testMirror () throws ExecutionException, InterruptedException {
        testBridgeUuid = bridgeInsert();
        mirrorInsert();
        mirrorDelete();
        bridgeDelete(testBridgeUuid);
    }

    public void netFlowInsert () throws ExecutionException, InterruptedException {
        String netFlowUuidStr = "testNetFlow";
        String netFlowTargets = "172.16.20.200:6343";
        Long engineType = 128L;
        Long engineID = 32L;
        Long activityTimeout = 1L;
        NetFlow netFlow = getClient().createTypedRowWrapper(NetFlow.class);
        netFlow.setTargets(ImmutableSet.of(netFlowTargets));
        netFlow.setEngineType(ImmutableSet.of(engineType));
        netFlow.setEngineId(ImmutableSet.of(engineID));
        netFlow.setActivityTimeout(ImmutableSet.of(activityTimeout));
        netFlow.setExternalIds(ImmutableMap.of("big", "baby"));
        Bridge bridge = getClient().getTypedRowWrapper(Bridge.class, null);
        TransactionBuilder transactionBuilder = getClient().transactBuilder(getDbSchema())
                .add(op.insert(netFlow.getSchema())
                        .withId(netFlowUuidStr)
                        .value(netFlow.getTargetsColumn())
                        .value(netFlow.getEngineTypeColumn())
                        .value(netFlow.getEngineIdColumn())
                        .value(netFlow.getActiveTimeoutColumn())
                        .value(netFlow.getExternalIdsColumn()))
                .add(op.comment("Mirror: Inserting " + netFlowUuidStr))
                .add(op.mutate(bridge.getSchema())
                        .addMutation(bridge.getNetflowColumn().getSchema(), Mutator.INSERT,
                                Sets.newHashSet(new UUID(netFlowUuidStr)))
                        .where(bridge.getNameColumn().getSchema().opEqual(TEST_BRIDGE_NAME))
                        .build())
                .add(op.comment("Bridge: Mutating " + TEST_BRIDGE_NAME));

        List<OperationResult> operationResults = executeTransaction(transactionBuilder,
                "Insert and Mutate operation results for NetFlow");
        testNetFlowUuid = operationResults.get(0).getUuid();
        assertNotNull(ASSERT_TRANS_UUID, testNetFlowUuid);

        // Verify that the local cache was updated with the remote changes
        Row netFlowRow = getTableCache().get(netFlow.getSchema().getName()).get(testNetFlowUuid);
        Mirror monitoredNetFlow = getClient().getTypedRowWrapper(Mirror.class, netFlowRow);
        assertEquals(netFlow.getExternalIdsColumn().getData(), monitoredNetFlow.getExternalIdsColumn().getData());
    }

    private void netFlowDelete () throws ExecutionException, InterruptedException {
        NetFlow netFlow = getClient().getTypedRowWrapper(NetFlow.class, null);
        Bridge bridge = getClient().getTypedRowWrapper(Bridge.class, null);
        DatabaseSchema dbSchema = getClient().getSchema(OPEN_VSWITCH_SCHEMA).get();

        TransactionBuilder transactionBuilder = getClient().transactBuilder(dbSchema)
                .add(op.delete(netFlow.getSchema())
                        .where(netFlow.getUuidColumn().getSchema().opEqual(testNetFlowUuid))
                        .build())
                .add(op.comment("NetFlow: Deleting " + testNetFlowUuid))
                .add(op.mutate(bridge.getSchema()) // Delete a controller column in the Bridge table
                        .addMutation(bridge.getNetflowColumn().getSchema(), Mutator.DELETE,
                                Sets.newHashSet(testNetFlowUuid)))
                .add(op.comment("Bridge: Mutating " + testNetFlowUuid))
                .add(op.commit(true));

        executeTransaction(transactionBuilder, "NetFlow: Delete operation results");
    }

    @Test
    public void testNetFlow () throws ExecutionException, InterruptedException {
        testBridgeUuid = bridgeInsert();
        netFlowInsert();
        netFlowDelete();
        bridgeDelete(testBridgeUuid);
    }

    public void portAndInterfaceInsert () throws ExecutionException, InterruptedException {
        String portUuidStr = "testPort";
        String intfUuidStr = "testIntf";
        String tunnelEncap = "vxlan";
        Port port = getClient().createTypedRowWrapper(Port.class);
        port.setName("testPort");
        port.setTag(ImmutableSet.of(1L));
        port.setMac(ImmutableSet.of("00:00:00:00:00:01"));
        port.setInterfaces(ImmutableSet.of(new UUID(intfUuidStr)));

        Interface intf = getClient().createTypedRowWrapper(Interface.class);
        intf.setName(port.getNameColumn().getData());
        intf.setType(tunnelEncap);
        intf.setExternalIds(ImmutableMap.of("vm-id", "12345abcedf78910"));
        // For per Flow TEPs use remote_ip=flow
        // For per Port TEPs use remote_ip=x.x.x.x (ipv4)
        intf.setOptions(ImmutableMap.of("local_ip", "172.16.24.145",
                "remote_ip", "flow",
                "key", "flow",
                "dst_port", "8472"));

        Bridge bridge = getClient().getTypedRowWrapper(Bridge.class, null);
        TransactionBuilder transactionBuilder = getClient().transactBuilder(getDbSchema())
                .add(op.insert(port.getSchema())
                        .withId(portUuidStr)
                        .value(port.getNameColumn())
                        .value(port.getMacColumn()))
                .add(op.comment("Port: Inserting " + portUuidStr))
                .add(op.insert(intf.getSchema())
                        .withId(intfUuidStr)
                        .value(intf.getNameColumn()))
                .add(op.comment("Interface: Inserting " + intfUuidStr))
                .add(op.update(port.getSchema())
                        .set(port.getTagColumn())
                        .set(port.getMacColumn())
                        .set(port.getInterfacesColumn())
                        .where(port.getNameColumn().getSchema().opEqual(port.getName()))
                        .build())
                .add(op.comment("Port: Updating " + portUuidStr))
                .add(op.update(intf.getSchema())
                        .set(intf.getTypeColumn())
                        .set(intf.getExternalIdsColumn())
                        .set(intf.getOptionsColumn())
                        .where(intf.getNameColumn().getSchema().opEqual(intf.getName()))
                        .build())
                .add(op.comment("Interface: Updating " + intfUuidStr))
                .add(op.mutate(bridge.getSchema())
                        .addMutation(bridge.getPortsColumn().getSchema(), Mutator.INSERT, Sets.newHashSet(new UUID(portUuidStr)))
                        .where(bridge.getNameColumn().getSchema().opEqual(TEST_BRIDGE_NAME))
                        .build())
                .add(op.comment("Bridge: Mutating " + TEST_BRIDGE_NAME));

        List<OperationResult> operationResults = executeTransaction(transactionBuilder,
                "Insert and Mutate operation results for Port and Interface");
        testPortUuid = operationResults.get(0).getUuid();
        assertNotNull(ASSERT_TRANS_UUID, testPortUuid);
        testInterfaceUuid = operationResults.get(2).getUuid();
        assertNotNull(ASSERT_TRANS_UUID, testInterfaceUuid);

        // Verify that the local cache was updated with the remote changes
        Row portRow = getTableCache().get(port.getSchema().getName()).get(testPortUuid);
        Port monitoredPort = getClient().getTypedRowWrapper(Port.class, portRow);
        assertEquals(port.getNameColumn().getData(), monitoredPort.getNameColumn().getData());

        Row interfaceRow = getTableCache().get(intf.getSchema().getName()).get(testInterfaceUuid);
        Interface monitoredInterface = getClient().getTypedRowWrapper(Interface.class, interfaceRow);
        assertEquals(intf.getNameColumn().getData(), monitoredInterface.getNameColumn().getData());
    }

    private void portAndInterfaceDelete () throws ExecutionException, InterruptedException {
        Port port = getClient().getTypedRowWrapper(Port.class, null);
        Interface intf = getClient().getTypedRowWrapper(Interface.class, null);
        Bridge bridge = getClient().getTypedRowWrapper(Bridge.class, null);
        DatabaseSchema dbSchema = getClient().getSchema(OPEN_VSWITCH_SCHEMA).get();

        TransactionBuilder transactionBuilder = getClient().transactBuilder(dbSchema)
                .add(op.delete(port.getSchema())
                        .where(port.getUuidColumn().getSchema().opEqual(testPortUuid))
                        .build())
                .add(op.comment("Port: Deleting " + testPortUuid))
                .add(op.delete(intf.getSchema())
                        .where(intf.getUuidColumn().getSchema().opEqual(testInterfaceUuid))
                        .build())
                .add(op.comment("Interface: Deleting " + testInterfaceUuid))
                .add(op.mutate(bridge.getSchema()) // Delete a port column in the Bridge table
                        .addMutation(bridge.getPortsColumn().getSchema(), Mutator.DELETE,
                                Sets.newHashSet(testPortUuid)))
                .add(op.comment("Bridge: Mutating " + testPortUuid))
                .add(op.commit(true));

        executeTransaction(transactionBuilder, "Port and Interface: Delete operation results");
    }

    @Test
    public void testPortAndInterface () throws ExecutionException, InterruptedException {
        testBridgeUuid = bridgeInsert();
        portAndInterfaceInsert();
        portAndInterfaceDelete();
        bridgeDelete(testBridgeUuid);
    }

    public void qosInsert() throws ExecutionException, InterruptedException {
        String portUuidStr = "testQosPortUuid";
        String intfUuidStr = "testQosIntfUuid";
        String qosUuidStr = "testQosUuid";
        String qosPort = "testQosPort";

        Port port = getClient().createTypedRowWrapper(Port.class);
        port.setName(qosPort);
        port.setInterfaces(ImmutableSet.of(new UUID(intfUuidStr)));
        port.setQos(ImmutableSet.of(new UUID(qosUuidStr)));
        port.setOtherConfig(ImmutableMap.of("m0r3", "c0ff33"));

        Interface intf = getClient().createTypedRowWrapper(Interface.class);
        intf.setName(port.getNameColumn().getData());
        intf.setOtherConfig(ImmutableMap.of("proto", "duction"));
        intf.setExternalIds(ImmutableMap.of("stringly", "typed"));

        Qos qos = getClient().createTypedRowWrapper(Qos.class);
        qos.setOtherConfig(ImmutableMap.of("mmm", "kay"));
        qos.setType(ImmutableSet.of("404"));

        Bridge bridge = getClient().getTypedRowWrapper(Bridge.class, null);

        TransactionBuilder transactionBuilder = getClient().transactBuilder(getDbSchema())
                .add(op.insert(port.getSchema())
                        .withId(portUuidStr)
                        .value(port.getNameColumn()))
                .add(op.comment("Port: Inserting " + portUuidStr))
                .add(op.insert(intf.getSchema())
                        .withId(intfUuidStr)
                        .value(intf.getExternalIdsColumn())
                        .value(intf.getNameColumn())
                        .value(intf.getOtherConfigColumn()))
                .add(op.comment("Interface: Inserting " + intfUuidStr))
                .add(op.insert(qos.getSchema())
                        .withId(qosUuidStr)
                        .value(qos.getTypeColumn())
                        .value(qos.getOtherConfigColumn()))
                .add(op.comment("QOS: Inserting " + qosUuidStr))
                .add(op.update(port.getSchema())
                        .set(port.getOtherConfigColumn())
                        .set(port.getInterfacesColumn())
                        .set(port.getQosColumn())
                        .where(port.getNameColumn().getSchema().opEqual(port.getName()))
                        .build())
                .add(op.comment("Interface: Updating " + intfUuidStr))
                .add(op.mutate(bridge.getSchema())
                        .addMutation(bridge.getPortsColumn().getSchema(), Mutator.INSERT,
                                Sets.newHashSet(new UUID(portUuidStr)))
                        .where(bridge.getNameColumn().getSchema().opEqual(TEST_BRIDGE_NAME))
                        .build())
                .add(op.comment("Bridge: Mutating " + TEST_BRIDGE_NAME));

        int insertPortOperationIndex = 0;
        int insertInterfaceOperationIndex = 2;
        int insertQosOperationIndex = 4;
        List<OperationResult> operationResults = executeTransaction(transactionBuilder,
                "Insert and Mutate operation results for Port and Interface");
        testPortUuid = operationResults.get(insertPortOperationIndex).getUuid();
        assertNotNull(ASSERT_TRANS_UUID, testPortUuid);
        testInterfaceUuid = operationResults.get(insertInterfaceOperationIndex).getUuid();
        assertNotNull(ASSERT_TRANS_UUID, testInterfaceUuid);
        testQosUuid = operationResults.get(insertQosOperationIndex).getUuid();
        assertNotNull(ASSERT_TRANS_UUID, testQosUuid);

        // Verify that the local cache was updated with the remote changes
        Row portRow = getTableCache().get(port.getSchema().getName()).get(testPortUuid);
        Port monitoredPort = getClient().getTypedRowWrapper(Port.class, portRow);
        assertEquals(port.getNameColumn().getData(), monitoredPort.getNameColumn().getData());

        Row interfaceRow = getTableCache().get(intf.getSchema().getName()).get(testInterfaceUuid);
        Interface monitoredInterface = getClient().getTypedRowWrapper(Interface.class, interfaceRow);
        assertEquals(intf.getNameColumn().getData(), monitoredInterface.getNameColumn().getData());

        Row qosRow = getTableCache().get(qos.getSchema().getName()).get(testQosUuid);
        Qos monitoredQos = getClient().getTypedRowWrapper(Qos.class, qosRow);
        assertEquals(qos.getTypeColumn().getData().toArray()[0], monitoredQos.getTypeColumn().getData());
    }

    private void qosDelete () throws ExecutionException, InterruptedException {
        Port port = getClient().getTypedRowWrapper(Port.class, null);
        Interface intf = getClient().getTypedRowWrapper(Interface.class, null);
        Qos qos = getClient().getTypedRowWrapper(Qos.class, null);
        Bridge bridge = getClient().getTypedRowWrapper(Bridge.class, null);
        DatabaseSchema dbSchema = getClient().getSchema(OPEN_VSWITCH_SCHEMA).get();

        TransactionBuilder transactionBuilder = getClient().transactBuilder(dbSchema)
                .add(op.delete(port.getSchema())
                        .where(port.getUuidColumn().getSchema().opEqual(testPortUuid))
                        .build())
                .add(op.comment("Port: Deleting " + testPortUuid))
                .add(op.delete(intf.getSchema())
                        .where(intf.getUuidColumn().getSchema().opEqual(testInterfaceUuid))
                        .build())
                .add(op.comment("Interface: Deleting " + testInterfaceUuid))
                .add(op.delete(qos.getSchema())
                        .where(qos.getUuidColumn().getSchema().opEqual(testQosUuid))
                        .build())
                .add(op.comment("Qos: Deleting " + testQosUuid))
                .add(op.mutate(bridge.getSchema()) // Delete a port column in the Bridge table
                        .addMutation(bridge.getPortsColumn().getSchema(), Mutator.DELETE,
                                Sets.newHashSet(testPortUuid)))
                .add(op.comment("Bridge: Mutating " + testPortUuid))
                .add(op.mutate(port.getSchema()) // Delete a qos column in the Port table
                        .addMutation(port.getQosColumn().getSchema(), Mutator.DELETE,
                                Sets.newHashSet(testQosUuid)))
                .add(op.comment("Port: Mutating " + testPortUuid))
                .add(op.commit(true));

        executeTransaction(transactionBuilder, "Qos, Port and Interface: Delete operation results");
    }

    @Test
    public void testQos () throws ExecutionException, InterruptedException {
        testBridgeUuid = bridgeInsert();
        qosInsert();
        qosDelete();
        bridgeDelete(testBridgeUuid);
    }

    public void queueInsert() throws InterruptedException, ExecutionException {
        /**
         * This is an arbitrary String that is a placeholder for
         * the future UUID generated by the OVSDB Server in the
         * future transaction. While it is possible to generate
         * ones own UUID for the transaction it is not recommended
         * since it wouldn't add any conceivable value.
         */
        String queueUuidStr = "queueUuidStr";
        Long dscpVal = Long.valueOf(4);
        Queue queue = getClient().createTypedRowWrapper(Queue.class);
        // Example of explicit ImmutableSet/Map Attribute declaration
        ImmutableSet<Long> dscp = ImmutableSet.of(dscpVal);
        ImmutableMap<String, String> externalIds = ImmutableMap.of("little", "coat");
        // Example of condensing the attributes bindings in one line
        queue.setOtherConfig(ImmutableMap.of("war", "onfun"));
        // Bind the Attributes to the transaction. These values end up in columns.
        queue.setExternalIds(externalIds);
        queue.setDscp(dscp);
        // Get the parent Qos table row UUID to insert the queue.
        Qos qos = getClient().getTypedRowWrapper(Qos.class, null);
        // Queue key that is mapped to the queue record/value/ofp_action_enqueue
        Long queueKey = 0L;
        // Reference the Port row to insert the Queue with UID or Port name
        TransactionBuilder transactionBuilder = getClient().transactBuilder(getDbSchema())
                .add(op.insert(queue.getSchema())
                        .withId(queueUuidStr)
                        .value(queue.getDscpColumn())
                        .value(queue.getExternalIdsColumn())
                        .value(queue.getOtherConfigColumn()))
                .add(op.comment("Queue: Inserting " + queueUuidStr))
                .add(op.mutate(qos.getSchema())
                        .addMutation(qos.getQueuesColumn().getSchema(), Mutator.INSERT,
                                ImmutableMap.of(queueKey, new UUID(queueUuidStr)))
                        .where(qos.getUuidColumn().getSchema().opEqual(testQosUuid))
                        .build())
                .add(op.comment("Qos: Mutating " + testQosUuid));

        // The transaction index for the Queue insert is used to store the Queue UUID
        int insertQueueOperationIndex = 0;
        List<OperationResult> operationResults = executeTransaction(transactionBuilder,
                "Insert and Mutate operation results for Queue");
        testQueueUuid = operationResults.get(insertQueueOperationIndex).getUuid();
        assertNotNull(ASSERT_TRANS_UUID, testQueueUuid);

        // Verify that the local cache was updated with the remote changes
        Row queueRow = getTableCache().get(queue.getSchema().getName()).get(testQueueUuid);
        Queue monitoredQueue = getClient().getTypedRowWrapper(Queue.class, queueRow);
        assertEquals(queue.getExternalIdsColumn().getData(), monitoredQueue.getExternalIdsColumn().getData());
    }

    private void queueDelete () throws ExecutionException, InterruptedException {
        Queue queue = getClient().getTypedRowWrapper(Queue.class, null);
        Qos qos = getClient().getTypedRowWrapper(Qos.class, null);
        DatabaseSchema dbSchema = getClient().getSchema(OPEN_VSWITCH_SCHEMA).get();

        TransactionBuilder transactionBuilder = getClient().transactBuilder(dbSchema)
                .add(op.delete(queue.getSchema())
                        .where(queue.getUuidColumn().getSchema().opEqual(testQueueUuid))
                        .build())
                .add(op.comment("Queue: Deleting " + testQueueUuid))
                .add(op.mutate(qos.getSchema()) // Delete a queue column in the Qos table
                        .addMutation(qos.getQueuesColumn().getSchema(), Mutator.DELETE,
                                Maps.newHashMap(ImmutableMap.of(0L,testQueueUuid))))
                .add(op.comment("Queue: Mutating " + testQueueUuid))
                .add(op.commit(true));

        executeTransaction(transactionBuilder, "Queue: Delete operation results");
    }

    @Test
    public void testQueue () throws ExecutionException, InterruptedException {
        testBridgeUuid = bridgeInsert();
        qosInsert();
        queueInsert();
        queueDelete();
        qosDelete();
        bridgeDelete(testBridgeUuid);
    }

    public void sFlowInsert () throws ExecutionException, InterruptedException {
        String sFlowUuidStr = "testSFlow";
        String sFlowTarget = "172.16.20.200:6343";
        Long header = 128L;
        Long obsPointId = 358L;
        Long polling =10L;
        String agent = "172.16.20.210";
        Long sampling = 64L;
        SFlow sFlow = getClient().createTypedRowWrapper(SFlow.class);
        sFlow.setTargets(ImmutableSet.of(sFlowTarget));
        sFlow.setHeader(ImmutableSet.of(header));
        sFlow.setPolling(ImmutableSet.of(obsPointId));
        sFlow.setPolling(ImmutableSet.of(polling));
        sFlow.setAgent(ImmutableSet.of(agent));
        sFlow.setSampling(ImmutableSet.of(sampling));
        sFlow.setExternalIds(ImmutableMap.of("kit", "tah"));
        Bridge bridge = getClient().getTypedRowWrapper(Bridge.class, null);
        TransactionBuilder transactionBuilder = getClient().transactBuilder(getDbSchema())
                .add(op.insert(sFlow.getSchema())
                        .withId(sFlowUuidStr)
                        .value(sFlow.getTargetsColumn())
                        .value(sFlow.getHeaderColumn())
                        .value(sFlow.getPollingColumn())
                        .value(sFlow.getAgentColumn())
                        .value(sFlow.getSamplingColumn())
                        .value(sFlow.getExternalIdsColumn()))
                .add(op.comment("sFlow: Inserting " + sFlowUuidStr))
                .add(op.mutate(bridge.getSchema())
                        .addMutation(bridge.getSflowColumn().getSchema(), Mutator.INSERT,
                                Sets.newHashSet(new UUID(sFlowUuidStr)))
                        .where(bridge.getNameColumn().getSchema().opEqual(TEST_BRIDGE_NAME))
                        .build())
                .add(op.comment("Bridge: Mutating " + TEST_BRIDGE_NAME));
        int insertSFlowOperationIndex = 0;
        List<OperationResult> operationResults = executeTransaction(transactionBuilder,
                "Insert and Mutate operation results for sFlow");
        testSFlowUuid = operationResults.get(insertSFlowOperationIndex).getUuid();
        assertNotNull(ASSERT_TRANS_UUID, testSFlowUuid);

        // Verify that the local cache was updated with the remote changes
        Row sFlowRow = getTableCache().get(sFlow.getSchema().getName()).get(testSFlowUuid);
        Queue monitoredSFlow = getClient().getTypedRowWrapper(Queue.class, sFlowRow);
        assertEquals(sFlow.getExternalIdsColumn().getData(), monitoredSFlow.getExternalIdsColumn().getData());
    }

    private void sFlowDelete () throws ExecutionException, InterruptedException {
        SFlow sFlow = getClient().getTypedRowWrapper(SFlow.class, null);
        Bridge bridge = getClient().getTypedRowWrapper(Bridge.class, null);
        DatabaseSchema dbSchema = getClient().getSchema(OPEN_VSWITCH_SCHEMA).get();

        TransactionBuilder transactionBuilder = getClient().transactBuilder(dbSchema)
                .add(op.delete(sFlow.getSchema())
                        .where(sFlow.getUuidColumn().getSchema().opEqual(testSFlowUuid))
                        .build())
                .add(op.comment("SFlow: Deleting " + testSFlowUuid))
                .add(op.mutate(bridge.getSchema()) // Delete an sflow column in the Bridge table
                        .addMutation(bridge.getSflowColumn().getSchema(), Mutator.DELETE,
                                Sets.newHashSet(testSFlowUuid)))
                .add(op.comment("Bridge: Mutating " + testSFlowUuid))
                .add(op.commit(true));

        executeTransaction(transactionBuilder, "Queue: Delete operation results");
    }

    @Test
    public void testSFlow () throws ExecutionException, InterruptedException {
        testBridgeUuid = bridgeInsert();
        sFlowInsert();
        sFlowDelete();
        bridgeDelete(testBridgeUuid);
    }

    public void sslInsert () throws ExecutionException, InterruptedException {

        String sslUuidStr = "sslUuidName";
        String caCert = "PARC";
        String certificate = "01101110 01100101 01110010 01100100";
        String privateKey = "SSL_Table_Test_Secret";
        ImmutableMap<String, String> externalIds = ImmutableMap.of("roomba", "powered");

        SSL ssl = getClient().createTypedRowWrapper(SSL.class);
        ssl.setCaCert(caCert);
        ssl.setCertificate(certificate);
        ssl.setPrivateKey(privateKey);
        ssl.setExternalIds(externalIds);
        // Get the parent OVS table UUID in it's single row
        UUID openVSwitchRowUuid = getOpenVSwitchTableUuid(getClient(), getTableCache());
        OpenVSwitch openVSwitch = getClient().getTypedRowWrapper(OpenVSwitch.class, null);
        TransactionBuilder transactionBuilder = getClient().transactBuilder(getDbSchema())
                .add(op.insert(ssl.getSchema())
                        .withId(sslUuidStr)
                        .value(ssl.getCertificateColumn())
                        .value(ssl.getPrivateKeyColumn())
                        .value(ssl.getCaCertColumn())
                        .value(ssl.getExternalIdsColumn()))
                .add(op.comment("SSL: Inserting " + sslUuidStr))
                .add(op.mutate(openVSwitch.getSchema())
                        .addMutation(openVSwitch.getSslColumn().getSchema(), Mutator.INSERT,
                                Sets.newHashSet(new UUID(sslUuidStr)))
                        .where(openVSwitch.getUuidColumn().getSchema().opEqual(openVSwitchRowUuid))
                        .build())
                .add(op.comment("Open_vSwitch: Mutating " + sslUuidStr));

        // The transaction index for the SSL insert is used to store the SSL UUID
        int insertSslOperationIndex = 0;
        List<OperationResult> operationResults = executeTransaction(transactionBuilder,
                "Insert and Mutate operation results for SSL");
        testSslUuid = operationResults.get(insertSslOperationIndex).getUuid();
        assertNotNull(ASSERT_TRANS_UUID, testSslUuid);

        // Verify that the local cache was updated with the remote changes
        Row sslRow = getTableCache().get(ssl.getSchema().getName()).get(testSslUuid);
        SSL monitoredSsl = getClient().getTypedRowWrapper(SSL.class, sslRow);
        assertEquals(ssl.getExternalIdsColumn().getData(), monitoredSsl.getExternalIdsColumn().getData());
    }

    public void sslDelete () throws ExecutionException, InterruptedException {
        SSL ssl = getClient().getTypedRowWrapper(SSL.class, null);
        OpenVSwitch openVSwitch = getClient().getTypedRowWrapper(OpenVSwitch.class, null);

        TransactionBuilder transactionBuilder = getClient().transactBuilder(getDbSchema())
                .add(op.delete(ssl.getSchema())
                        .where(ssl.getUuidColumn().getSchema().opEqual(testSslUuid))
                        .build())
                .add(op.comment("SSL: Deleting " + testSslUuid))
                .add(op.mutate(openVSwitch.getSchema())
                        .addMutation(openVSwitch.getSslColumn().getSchema(), Mutator.DELETE,
                                Sets.newHashSet(testSslUuid)))
                .add(op.comment("Open_vSwitch: Mutating " + testSslUuid))
                .add(op.commit(true));

        executeTransaction(transactionBuilder, "SSL delete operation results");
    }

    @Test
    public void testSsl () throws ExecutionException, InterruptedException {
        sslInsert();
        sslDelete();
    }

    @Test
    public void testTyperUtilsSpecialMethodsToString () {
        Bridge bridge = getClient().createTypedRowWrapper(Bridge.class);
        assertNotNull(bridge);
        bridge.setName(TEST_BRIDGE_NAME);
        bridge.setStatus(ImmutableMap.of("key", "value"));
        bridge.setFloodVlans(Sets.newHashSet(34L));
        assertNotNull(bridge.toString());

        Bridge nullRowBridge = getClient().getTypedRowWrapper(Bridge.class, null);
        assertNotNull(nullRowBridge.toString());
    }

    @Test
    public void testTyperUtilsSpecialMethodsEquals () {
        Bridge bridge = getClient().createTypedRowWrapper(Bridge.class);
        assertNotNull(bridge);
        bridge.setName(TEST_BRIDGE_NAME);
        bridge.setStatus(ImmutableMap.of("key", "value"));
        bridge.setFloodVlans(Sets.newHashSet(34L));

        assertTrue("Equals check on same Bridge object", bridge.equals(bridge));

        Bridge bridge2 = getClient().createTypedRowWrapper(Bridge.class);
        assertNotNull(bridge2);
        bridge2.setName(bridge.getName());
        bridge2.setStatus(bridge.getStatusColumn().getData());
        bridge2.setFloodVlans(bridge.getFloodVlansColumn().getData());

        assertTrue("Equals check for different Bridge objects with same content", bridge.equals(bridge2));

        bridge2.setStpEnable(true);
        assertFalse("Equals check for different Bridge objects with different content", bridge.equals(bridge2));

        Port port = getClient().createTypedRowWrapper(Port.class);
        port.setName(bridge.getName());
        assertFalse("Equals check for a Bridge object and Port Object", bridge.equals(port));
        assertFalse("Equals check for a Typed Proxy object and non-proxy object", port.equals("String"));

        Bridge nullRowBridge = getClient().getTypedRowWrapper(Bridge.class, null);
        assertTrue("Equals check on Bridge object with null Row", nullRowBridge.equals(nullRowBridge));
    }

    @Test
    public void testTyperUtilsSpecialMethodsHashCode () {
        Bridge bridge = getClient().createTypedRowWrapper(Bridge.class);

        assertNotNull(bridge);
        bridge.setName(TEST_BRIDGE_NAME);
        bridge.setStatus(ImmutableMap.of("key", "value"));
        bridge.setFloodVlans(Sets.newHashSet(34L));

        assertNotSame(bridge.hashCode(), 0);
        Bridge nullRowBridge = getClient().getTypedRowWrapper(Bridge.class, null);
        assertSame(nullRowBridge.hashCode(), 0);
    }
}
