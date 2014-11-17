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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.opendaylight.ovsdb.lib.operations.Operations.op;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.propagateSystemProperty;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.opendaylight.ovsdb.integrationtest.ConfigurationBundles;
import org.opendaylight.ovsdb.integrationtest.OvsdbIntegrationTestBase;
import org.opendaylight.ovsdb.lib.MonitorCallBack;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.message.MonitorRequest;
import org.opendaylight.ovsdb.lib.message.MonitorRequestBuilder;
import org.opendaylight.ovsdb.lib.message.MonitorSelect;
import org.opendaylight.ovsdb.lib.message.TableUpdate;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.OperationResult;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.TableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.OpenVSwitch;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        BridgeIT.class,
        ControllerIT.class,
        FlowSampleCollectorSetIT.class,
        FlowTableIT.class,
        IpfixIT.class,
        ManagerIT.class
})
public class OpenVSwitchBaseTest extends OvsdbIntegrationTestBase {
    private static final Logger LOG = LoggerFactory.getLogger(OpenVSwitchBaseTest.class);
    private static boolean monitorReady = false;
    private static boolean schemaSupported = false;
    public static final String TEST_BRIDGE_NAME = "br_test";
    public static final String ASSERT_TRANS_ERROR = "Transaction should not have errors";
    public static final String ASSERT_TRANS_RESULT_EMPTY = "Transaction should not be empty";
    public static final String ASSERT_TRANS_OPERATION_COUNT = "Transaction should match number of operations";
    public static final String ASSERT_TRANS_UUID = "Transaction UUID should not be null";

    static Map<String, Map<UUID, Row>> tableCache = new HashMap<String, Map<UUID, Row>>();
    public static Map<String, Map<UUID, Row>> getTableCache () {
        return tableCache;
    }

    private static DatabaseSchema dbSchema;
    private static OvsdbClient ovsdbClient;

    public OvsdbClient getClient () {
        return ovsdbClient;
    }

    public DatabaseSchema getDbSchema () {
        return dbSchema;
    }

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

                ConfigurationBundles.controllerBundles(),
                ConfigurationBundles.ovsdbLibraryBundles(),
                ConfigurationBundles.ovsdbDefaultSchemaBundles(),
                junitBundles()
        );
    }

    /*
     * Method adds a log as each test method starts and finishes. This is useful when
     * the test suite is used because the suites only print a final summary.
     */
    @Rule
    public TestRule watcher = new TestWatcher() {
        @Override
        protected void starting(Description description) {
            LOG.info("TestWatcher: Starting test: {}",
                    description.getDisplayName());
        }

        @Override
        protected void finished(Description description) {
            LOG.info("TestWatcher: Finished test: {}", description.getDisplayName());
        }
    };

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
            fail("Exception : "+e.getMessage());
        }

        LOG.info("Schema ({}) is not supported", schema);
        return false;
    }

    public UUID getOpenVSwitchTableUuid (OvsdbClient ovs, Map<String, Map<UUID, Row>> tableCache) {
        OpenVSwitch openVSwitch = ovs.getTypedRowWrapper(OpenVSwitch.class, null);
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
}
