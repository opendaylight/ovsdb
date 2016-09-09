/*
 * Copyright (C) 2014 EBay Software Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.integrationtest.ovsdbclient;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.it.LibraryIntegrationTestBase;
import org.opendaylight.ovsdb.lib.it.LibraryIntegrationTestUtils;
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.OperationResult;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.ColumnSchema;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.TableSchema;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class OvsdbClientTestTypedIT extends LibraryIntegrationTestBase {
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbClientTestTypedIT.class);
    private OvsdbClient ovs;
    private DatabaseSchema dbSchema = null;
    private static final String TEST_BRIDGE_NAME = "br_test";
    private static UUID testBridgeUuid = null;

    /**
     * Test creation of statically typed bridge table as defined in
     * ovs-vswitchd.conf.db with get/set for all relevant columns. The
     * SETDATA methods for "name", "status" and "flood_vlans" columns
     * are verified.
     */
    @Test
    public void testTypedBridgeCreate() throws IOException, InterruptedException, ExecutionException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        TestBridge rBridge = ovs.createTypedRowWrapper(TestBridge.class);
        rBridge.setName(TEST_BRIDGE_NAME);
        rBridge.setStatus(ImmutableMap.of("key","value"));
        rBridge.setFloodVlans(Sets.newHashSet(34));

        TableSchema ovsTable = dbSchema.table("Open_vSwitch");
        ColumnSchema<Set<UUID>> bridges = ovsTable.multiValuedColumn("bridges", UUID.class);

        String namedUuid = "br_test";
        int insertOperationIndex = 0;

        TransactionBuilder transactionBuilder = ovs.transactBuilder(dbSchema)
                .add(op.insert(rBridge)
                        .withId(namedUuid))
                .add(op.mutate(ovsTable)
                        .addMutation(bridges, Mutator.INSERT, Sets.newHashSet(new UUID(namedUuid))));

        ListenableFuture<List<OperationResult>> results = transactionBuilder.execute();
        List<OperationResult> operationResults = results.get();
        assertFalse(operationResults.isEmpty());
        // Check if Results matches the number of operations in transaction
        assertEquals(transactionBuilder.getOperations().size(), operationResults.size());
        LOG.info("Insert & Update operation results = {}", operationResults);
        testBridgeUuid = operationResults.get(insertOperationIndex).getUuid();
    }

    public void testGetDBs() throws ExecutionException, InterruptedException {
        ListenableFuture<List<String>> databases = ovs.getDatabases();
        List<String> dbNames = databases.get();
        assertNotNull(dbNames);
        boolean hasOpenVswitchSchema = false;
        for(String dbName : dbNames) {
           if (dbName.equals(LibraryIntegrationTestUtils.OPEN_VSWITCH)) {
                hasOpenVswitchSchema = true;
                break;
           }
        }
        assertTrue(LibraryIntegrationTestUtils.OPEN_VSWITCH
                + " schema is not supported by the switch", hasOpenVswitchSchema);
    }

    @Before
    public void setup() throws Exception {
        schema = LibraryIntegrationTestUtils.OPEN_VSWITCH;
        super.setup2();

        if (ovs != null) {
            return;
        }
        ovs = LibraryIntegrationTestUtils.getTestConnection(this);
        assertNotNull("Failed to get connection to ovsdb node", ovs);
        LOG.info("Connection Info: {}", ovs.getConnectionInfo().toString());
        testGetDBs();
        dbSchema = ovs.getSchema(LibraryIntegrationTestUtils.OPEN_VSWITCH).get();
    }

    @After
    public void tearDown() throws InterruptedException, ExecutionException {
        TableSchema bridge = dbSchema.table("Bridge");
        ColumnSchema<String> name = bridge.column("name", String.class);
        TableSchema ovsTable = dbSchema.table("Open_vSwitch");
        ColumnSchema<Set<UUID>> bridges = ovsTable.multiValuedColumn("bridges", UUID.class);

        ListenableFuture<List<OperationResult>> results = ovs.transactBuilder(dbSchema)
                .add(op.delete(bridge)
                        .where(name.opEqual(TEST_BRIDGE_NAME))
                        .build())
                .add(op.mutate(ovsTable)
                        .addMutation(bridges, Mutator.DELETE, Sets.newHashSet(testBridgeUuid)))
                .add(op.commit(true))
                .execute();

        List<OperationResult> operationResults = results.get();
        LOG.info("Delete operation results = {}", operationResults);
        ovs.disconnect();
    }
}
