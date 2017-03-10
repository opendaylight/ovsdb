/*
 * Copyright © 2015, 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.lib.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.propagateSystemProperties;
import static org.ops4j.pax.exam.CoreOptions.vmOption;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;

import com.google.common.util.concurrent.ListenableFuture;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.opendaylight.controller.mdsal.it.base.AbstractMdsalTestBase;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.ovsdb.lib.MonitorCallBack;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.message.MonitorRequest;
import org.opendaylight.ovsdb.lib.message.MonitorRequestBuilder;
import org.opendaylight.ovsdb.lib.message.MonitorSelect;
import org.opendaylight.ovsdb.lib.message.TableUpdate;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.notation.Version;
import org.opendaylight.ovsdb.lib.operations.OperationResult;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.TableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.karaf.options.LogLevelOption;
import org.ops4j.pax.exam.options.MavenUrlReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for library IT.
 */
public abstract class LibraryIntegrationTestBase extends AbstractMdsalTestBase {
    private static final Logger LOG = LoggerFactory.getLogger(LibraryIntegrationTestBase.class);
    protected static final String ASSERT_TRANS_ERROR = "Transaction should not have errors";
    protected static final String ASSERT_TRANS_RESULT_EMPTY = "Transaction should not be empty";
    protected static final String ASSERT_TRANS_OPERATION_COUNT = "Transaction should match number of operations";
    protected static final String ASSERT_TRANS_UUID = "Transaction UUID should not be null";
    protected static Version schemaVersion;
    private static boolean schemaSupported = false;
    protected static OvsdbClient ovsdbClient;
    private static Map<String, Map<UUID, Row>> tableCache = new HashMap<>();
    private static boolean monitorReady = false;
    public String schema;

    protected static Map<String, Map<UUID, Row>> getTableCache() {
        return tableCache;
    }

    protected OvsdbClient getClient () {
        return ovsdbClient;
    }

    protected static DatabaseSchema dbSchema;
    protected DatabaseSchema getDbSchema () {
        return dbSchema;
    }

    private static AtomicBoolean setup = new AtomicBoolean(false);
    protected static boolean getSetup() {
        return setup.get();
    }

    protected static void setSetup(boolean setup) {
        LibraryIntegrationTestBase.setup.set(setup);
    }

    @Override
    public MavenUrlReference getFeatureRepo() {
        return maven()
                .groupId("org.opendaylight.ovsdb")
                .artifactId("library-features")
                .classifier("features")
                .type("xml")
                .versionAsInProject();
    }

    @Override
    public String getFeatureName() {
        return "odl-ovsdb-library";
    }

    @Configuration
    @Override
    public Option[] config() {
        Option[] parentOptions = super.config();
        Option[] propertiesOptions = getPropertiesOptions();
        Option[] otherOptions = getOtherOptions();
        Option[] options = new Option[parentOptions.length + propertiesOptions.length + otherOptions.length];
        System.arraycopy(parentOptions, 0, options, 0, parentOptions.length);
        System.arraycopy(propertiesOptions, 0, options, parentOptions.length, propertiesOptions.length);
        System.arraycopy(otherOptions, 0, options, parentOptions.length + propertiesOptions.length,
                otherOptions.length);
        return options;
    }

    private Option[] getOtherOptions() {
        return new Option[] {
                vmOption("-javaagent:../jars/org.jacoco.agent.jar=destfile=../../jacoco-it.exec"),
                keepRuntimeFolder()
        };
    }

    public Option[] getPropertiesOptions() {
        return new Option[] {
                propagateSystemProperties(
                        LibraryIntegrationTestUtils.SERVER_IPADDRESS,
                        LibraryIntegrationTestUtils.SERVER_PORT,
                        LibraryIntegrationTestUtils.CONNECTION_TYPE),
        };
    }

    @Override
    public Option getLoggingOption() {
        Option option = editConfigurationFilePut(ORG_OPS4J_PAX_LOGGING_CFG,
                logConfiguration(getClass()),
                LogLevelOption.LogLevel.INFO.name());
        option = composite(option, super.getLoggingOption());
        return option;
    }

    protected BindingAwareBroker.ProviderContext getProviderContext() {
        BindingAwareBroker.ProviderContext providerContext = null;
        for (int i=0; i < 60; i++) {
            LOG.info("Looking for ProviderContext, try {}", i);
            providerContext = getSession();
            if (providerContext != null) {
                break;
            } else {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    LOG.warn("Interrupted while waiting for provider context", e);
                }
            }
        }
        assertNotNull("providercontext should not be null", providerContext);
        /* One more second to let the provider finish initialization */
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            LOG.warn("Interrupted while waiting for other provider", e);
        }
        return providerContext;
    }

    public boolean checkSchema (String schema) {
        if (schemaSupported) {
            LOG.info("Schema ({}) is supported", schema);
            return true;
        }
        try {
            ovsdbClient = LibraryIntegrationTestUtils.getTestConnection(this);
            assertNotNull("Invalid Client. Check connection params", ovsdbClient);
            if (isSchemaSupported(ovsdbClient, schema)) {
                dbSchema = ovsdbClient.getSchema(schema).get();
                assertNotNull(dbSchema);
                LOG.info("{} schema in {} with tables: {}",
                        schema, ovsdbClient.getConnectionInfo(), dbSchema.getTables());
                schemaSupported = true;
                return true;
            }
        } catch (Exception e) {
            fail("Exception: " + e);
        }

        LOG.info("Schema ({}) is not supported", schema);
        return false;
    }

    public boolean isSchemaSupported (String schema) throws ExecutionException,
            InterruptedException {
        return isSchemaSupported(ovsdbClient, schema);
    }

    public boolean isSchemaSupported (OvsdbClient client, String schema) throws ExecutionException, InterruptedException {
        ListenableFuture<List<String>> databases = client.getDatabases();
        List<String> dbNames = databases.get();
        assertNotNull(dbNames);
        return dbNames.contains(schema);
    }

    /**
     * As per RFC 7047, section 4.1.5, if a Monitor request is sent without any columns, the update response will not include
     * the _uuid column.
     * ----------------------------------------------------------------------------------------------------------------------------------
     * Each &lt;monitor-request&gt; specifies one or more columns and the manner in which the columns (or the entire table) are to be monitored.
     * The "columns" member specifies the columns whose values are monitored. It MUST NOT contain duplicates.
     * If "columns" is omitted, all columns in the table, except for "_uuid", are monitored.
     * ----------------------------------------------------------------------------------------------------------------------------------
     * In order to overcome this limitation, this method
     *
     * @return MonitorRequest that includes all the Bridge Columns including _uuid
     */
    public <T extends TypedBaseTable<GenericTableSchema>> MonitorRequest getAllColumnsMonitorRequest (Class <T> klazz) {
        TypedBaseTable<GenericTableSchema> table = getClient().createTypedRowWrapper(klazz);
        GenericTableSchema tableSchema = table.getSchema();
        Set<String> columns = tableSchema.getColumns();
        MonitorRequestBuilder<GenericTableSchema> bridgeBuilder = new MonitorRequestBuilder<>(table.getSchema());
        for (String column : columns) {
            bridgeBuilder.addColumn(column);
        }
        return bridgeBuilder.with(new MonitorSelect(true, true, true, true)).build();
    }

    public <T extends TableSchema<T>> MonitorRequest getAllColumnsMonitorRequest (T tableSchema) {
        Set<String> columns = tableSchema.getColumns();
        MonitorRequestBuilder<T> monitorBuilder = new MonitorRequestBuilder<>(tableSchema);
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

        List<MonitorRequest> monitorRequests = new ArrayList<>();
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

    @SuppressWarnings("unchecked")
    protected void updateTableCache(TableUpdates updates) {
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

    public void setup() throws Exception {
        if (getSetup()) {
            LOG.info("Skipping setUp, already initialized");
            return;
        }

        try {
            super.setup();
        } catch (Exception e) {
            LOG.warn("Failed to setup test", e);
            fail("Failed to setup test: " + e);
        }

        assertNotNull("ProviderContext was not found", getProviderContext());
        if (schema.equals(LibraryIntegrationTestUtils.OPEN_VSWITCH)) {
            assertTrue(schema + " is required.", checkSchema(schema));
        } else {
            assumeTrue(schema + " is required.", checkSchema(schema));
        }
        schemaVersion = getClient().getDatabaseSchema(schema).getVersion();
        LOG.info("{} schema version = {}", schema, schemaVersion);
        assertTrue("Failed to monitor tables", monitorTables());
        setSetup(true);
    }

    public void setup2() throws Exception {
        if (getSetup()) {
            LOG.info("Skipping setUp, already initialized");
            return;
        }

        try {
            super.setup();
        } catch (Exception e) {
            LOG.warn("Failed to setup test", e);
            fail("Failed to setup test: " + e);
        }

        assertNotNull("ProviderContext was not found", getProviderContext());
        setSetup(true);
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
}
