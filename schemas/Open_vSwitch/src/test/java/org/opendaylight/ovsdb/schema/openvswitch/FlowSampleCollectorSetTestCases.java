/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 *  Authors : Brent Salisbury, Dave Tucker
 */

package org.opendaylight.ovsdb.schema.openvswitch;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ListenableFuture;
import junit.framework.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.ovsdb.lib.error.SchemaVersionMismatchException;
import org.opendaylight.ovsdb.lib.message.UpdateNotification;
import org.opendaylight.ovsdb.lib.notation.Version;
import org.opendaylight.ovsdb.lib.operations.OperationResult;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

public class FlowSampleCollectorSetTestCases extends OpenVswitchSchemaTestBase {
    Logger logger = LoggerFactory.getLogger(FlowSampleCollectorSet.class);
    Version schemaVersion;
    Version flowSampleCollectorSetFromVersion = Version.fromString("7.1.0");

    @Before
    public void setUp() throws ExecutionException, InterruptedException, TimeoutException, IOException {
        super.setUp();
        schemaVersion = ovs.getDatabaseSchema("Open_vSwitch").getVersion();
    }

    @Test
    public void testTableNotSupported() {
        // Don't run this test if the table is supported
        Assume.assumeTrue(schemaVersion.compareTo(flowSampleCollectorSetFromVersion) < 0);
        boolean isExceptionRaised = false;
        try {
            FlowSampleCollectorSet flowSampleCollectorSet = ovs.createTypedRowWrapper(FlowSampleCollectorSet.class);
        } catch (SchemaVersionMismatchException e) {
            isExceptionRaised = true;
        }
        Assert.assertTrue(isExceptionRaised);
    }


    @Test
    public void testCreateTypedFlowSampleCollectorSet() throws InterruptedException, ExecutionException, IllegalArgumentException{
        // Don't run this test if the table is not supported
        Assume.assumeTrue(schemaVersion.compareTo(flowSampleCollectorSetFromVersion) >= 0);
        FlowSampleCollectorSet flowSampleCollectorSet = ovs.createTypedRowWrapper(FlowSampleCollectorSet.class);
        flowSampleCollectorSet.setId(1);
        flowSampleCollectorSet.setExternalIds(ImmutableMap.of("<3", "ovs"));
        flowSampleCollectorSet.setBridge(OpenVswitchSchemaSuiteIT.getTestBridgeUuid());
        Bridge bridge = ovs.getTypedRowWrapper(Bridge.class, null);
        TransactionBuilder transactionBuilder = ovs.transactBuilder()
                .add(op.insert(flowSampleCollectorSet.getSchema())
                        .value(flowSampleCollectorSet.getIdColumn())
                        .value(flowSampleCollectorSet.getExternalIdsColumn())
                        .value(flowSampleCollectorSet.getBridgeColumn()));
        ListenableFuture<List<OperationResult>> results = transactionBuilder.execute();
        List<OperationResult> operationResults = results.get();
        Assert.assertFalse(operationResults.isEmpty());
        // Check if Results matches the number of operations in transaction
        Assert.assertEquals(transactionBuilder.getOperations().size(), operationResults.size());
        for (OperationResult result : operationResults) Assert.assertNull(result.getError());
        logger.info("Insert operation results for FlowSampleCollectorSet = {} ", operationResults);
    }

    @Override
    public void update(Object context, UpdateNotification upadateNotification) {

    }

    @Override
    public void locked(Object context, List<String> ids) {

    }

    @Override
    public void stolen(Object context, List<String> ids) {

    }
}
