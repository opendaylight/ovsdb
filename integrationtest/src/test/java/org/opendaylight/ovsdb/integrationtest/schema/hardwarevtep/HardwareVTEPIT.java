/*
 *  Copyright (C) 2014 Red Hat, Inc. and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 *  Authors : Sam Hague, Matt Oswalt
 */
package org.opendaylight.ovsdb.integrationtest.schema.hardwarevtep;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;
import static org.opendaylight.ovsdb.lib.operations.Operations.op;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.propagateSystemProperty;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import javax.inject.Inject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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
import org.opendaylight.ovsdb.schema.hardwarevtep.Global;
import org.opendaylight.ovsdb.schema.hardwarevtep.Manager;
import org.opendaylight.ovsdb.schema.openvswitch.OpenVSwitch;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;
import org.ops4j.pax.exam.util.PathUtils;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
public class HardwareVTEPIT  extends OvsdbIntegrationTestBase {
    private static final Logger LOG = LoggerFactory.getLogger(HardwareVTEPIT.class);
    private static boolean monitorReady = false;
    private static boolean schemaSupported = false;
    private static final String ASSERT_TRANS_ERROR = "Transaction should not have errors";
    private static final String ASSERT_TRANS_RESULT_EMPTY = "Transaction should not be empty";
    private static final String ASSERT_TRANS_OPERATION_COUNT = "Transaction should match number of operations";
    private static final String ASSERT_TRANS_UUID = "Transaction UUID should not be null";
    private UUID testManagerUuid = null;

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
        assumeTrue(HARDWARE_VTEP + " is required.", checkSchema(HARDWARE_VTEP));
        assertTrue("Failed to monitor tables", monitorTables());
        LOG.info("{} schema version = {}", OPEN_VSWITCH_SCHEMA,
                getClient().getDatabaseSchema(OPEN_VSWITCH_SCHEMA).getVersion());
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

    public UUID getGlobalTableUuid(OvsdbClient ovs, Map<String, Map<UUID, Row>> tableCache) {
        Global glbl = getClient().getTypedRowWrapper(Global.class, null);
        Map<UUID, Row> glblTbl = tableCache.get(glbl.getSchema().getName());
        if (glblTbl != null) {
            if (glblTbl.keySet().size() >= 1) {
                return (UUID)glblTbl.keySet().toArray()[0];
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

    /**
     * Create a new manager string in addition to whatever is already there
     * Will modify the Global table to include the UUID to the new Manager row
     */
    public void managerInsert () throws ExecutionException, InterruptedException {
        //Ensure test only proceeds if HW VTEP is supported
        assumeTrue(isSchemaSupported(getClient(), HARDWARE_VTEP));

        //proceed only if schema was already retrieved successfully
        Assert.assertNotNull(getDbSchema());

        //create new manager and set target string
        Manager manager = getClient().createTypedRowWrapper(Manager.class);
        manager.setTarget("ptcp:6641");

        String transactionUuidStr = "foobar";

        Global glbl = this.getClient().createTypedRowWrapper(Global.class);
        glbl.setManagers(Sets.newHashSet(new UUID(transactionUuidStr)));

        TransactionBuilder transactionBuilder = getClient().transactBuilder(getDbSchema())
                .add(op.insert(manager.getSchema())
                        .withId(transactionUuidStr)
                        .value(manager.getTargetColumn()))
                .add(op.comment("Manager: Inserting " + transactionUuidStr))
                .add(op.mutate(glbl.getSchema())
                        .addMutation(glbl.getManagersColumn().getSchema(), Mutator.INSERT,
                                glbl.getManagersColumn().getData()))
                .add(op.comment("Global: Mutating " + transactionUuidStr));

        int insertOperationIndex = 0;
        List<OperationResult> operationResults = executeTransaction(transactionBuilder,
                "Manager: Insert and Mutate results");
        testManagerUuid = operationResults.get(insertOperationIndex).getUuid();
        assertNotNull(ASSERT_TRANS_UUID, testManagerUuid);

        // Verify that the local cache was updated with the remote changes
        Row managerRow = getTableCache().get(manager.getSchema().getName()).get(testManagerUuid);
        Manager monitoredManager = getClient().getTypedRowWrapper(Manager.class, managerRow);
        assertEquals(manager.getTargetColumn().getData(), monitoredManager.getTargetColumn().getData());
        assertNotNull(monitoredManager.getUuid());
        assertNotNull(monitoredManager.getVersion());
        assertNotNull(getGlobalTableUuid(getClient(), getTableCache()));
    }

    public void managerDelete () throws ExecutionException, InterruptedException {
        assumeTrue(isSchemaSupported(getClient(), HARDWARE_VTEP));

        Manager manager = getClient().getTypedRowWrapper(Manager.class, null);
        Global global = getClient().getTypedRowWrapper(Global.class, null);

        TransactionBuilder transactionBuilder = getClient().transactBuilder(getDbSchema())
                .add(op.delete(manager.getSchema())
                        .where(manager.getUuidColumn().getSchema().opEqual(testManagerUuid))
                        .build())
                .add(op.comment("Manager: Deleting " + testManagerUuid))
                .add(op.mutate(global.getSchema())
                        .addMutation(global.getManagersColumn().getSchema(), Mutator.DELETE,
                                Sets.newHashSet(testManagerUuid)))
                .add(op.comment("Global: Mutating " + testManagerUuid))
                .add(op.commit(true));

        executeTransaction(transactionBuilder, "Manager delete operation results");
    }

    @Test
    public void testManager () throws ExecutionException, InterruptedException {
        managerInsert();
        managerDelete();
    }
}
