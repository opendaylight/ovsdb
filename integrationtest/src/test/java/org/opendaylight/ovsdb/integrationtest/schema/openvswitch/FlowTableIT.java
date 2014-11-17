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
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import javax.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.ovsdb.lib.error.SchemaVersionMismatchException;
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.notation.Version;
import org.opendaylight.ovsdb.lib.operations.OperationResult;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.FlowTable;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
public class FlowTableIT extends OpenVSwitchBaseTest {
    private static final Logger LOG = LoggerFactory.getLogger(FlowTableIT.class);
    private UUID testFlowTableUuid = null;
    Version schemaVersion;
    Version flowTableFromVersion = Version.fromString("6.5.0");
    Version prefixesAddedVersion = Version.fromString("7.4.0");
    Version externalIdAddedVerson = Version.fromString("7.5.0");

    @Inject
    private BundleContext bc;

    @Before
    public void setUp () throws ExecutionException, InterruptedException, IOException {
        areWeReady(bc);
        assertTrue(OPEN_VSWITCH_SCHEMA + " is required.", checkSchema(OPEN_VSWITCH_SCHEMA));
        assertTrue("Failed to monitor tables", monitorTables());
        schemaVersion = getClient().getDatabaseSchema("Open_vSwitch").getVersion();
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

    public void testFlowTableInsert () throws ExecutionException, IllegalArgumentException, InterruptedException {

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

    public void testFlowTableDelete () throws ExecutionException, InterruptedException {
        FlowTable flowTable = getClient().getTypedRowWrapper(FlowTable.class, null);
        Bridge bridge = getClient().getTypedRowWrapper(Bridge.class, null);

        TransactionBuilder transactionBuilder = getClient().transactBuilder(getDbSchema())
                .add(op.delete(flowTable.getSchema())
                        .where(flowTable.getUuidColumn().getSchema().opEqual(testFlowTableUuid))
                        .build())
                .add(op.comment("FlowTable: Deleting " + testFlowTableUuid))
                .add(op.mutate(bridge.getSchema())
                        .addMutation(bridge.getFlowTablesColumn().getSchema(), Mutator.DELETE, Maps.newHashMap(ImmutableMap.of(1L,testFlowTableUuid))))
                        .add(op.comment("Bridge: Mutating " + testFlowTableUuid))
                        .add(op.commit(true));

        LOG.error("transactionBuilder = {} -- {} -- {} -- {}",
                transactionBuilder, transactionBuilder.build(), transactionBuilder.getOperations(), transactionBuilder.toString());
        executeTransaction(transactionBuilder, "FlowTable delete operation results");
    }

    @Test
    public void setTestFlowTableSet () throws ExecutionException, InterruptedException {
        UUID testBridgeUuid = bridgeInsert();
        testFlowTableInsert();
        testFlowTableDelete();
        bridgeDelete(testBridgeUuid);
    }
}
