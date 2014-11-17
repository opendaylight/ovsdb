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
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.ovsdb.lib.error.SchemaVersionMismatchException;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.notation.Version;
import org.opendaylight.ovsdb.lib.operations.OperationResult;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.schema.openvswitch.FlowSampleCollectorSet;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
public class FlowSampleCollectorSetIT extends OpenVSwitchBaseTest {
    private static final Logger LOG = LoggerFactory.getLogger(FlowSampleCollectorSetIT.class);
    private UUID testBridgeUuid = null;
    private UUID testFlowSampleCollectorSetUuid = null;
    Version schemaVersion;
    Version flowSampleCollectorSetFromVersion = Version.fromString("7.1.0");

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

    public void testFlowSampleCollectorSetInsert () throws
            ExecutionException, IllegalArgumentException, InterruptedException {

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

    public void testFlowSampleCollectorSetDelete () throws ExecutionException, InterruptedException {
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
        testBridgeUuid = bridgeInsert();
        testFlowSampleCollectorSetInsert();
        testFlowSampleCollectorSetDelete();
        bridgeDelete(testBridgeUuid);
    }
}
