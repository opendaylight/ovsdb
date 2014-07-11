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
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.notation.Version;
import org.opendaylight.ovsdb.lib.operations.OperationResult;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

public class FlowTableTestCases extends OpenVswitchSchemaTestBase {

    Logger logger = LoggerFactory.getLogger(IpfixTestCases.class);
    Version schemaVersion;
    Version flowTableFromVersion = Version.fromString("6.5.0");
    Version prefixesAddedVersion = Version.fromString("7.4.0");
    Version externalIdAddedVerson = Version.fromString("7.5.0");

    @Before
    public void setUp() throws ExecutionException, InterruptedException, TimeoutException, IOException {
        super.setUp();
        schemaVersion = ovs.getDatabaseSchema("Open_vSwitch").getVersion();
    }

    @Test
    public void testTableNotSupported() {
        // Don't run this test if the table is supported
        Assume.assumeTrue(schemaVersion.compareTo(flowTableFromVersion) < 0);
        boolean isExceptionRaised = false;
        try {
            FlowTable flowTable = ovs.createTypedRowWrapper(FlowTable.class);
        } catch (SchemaVersionMismatchException e) {
            isExceptionRaised = true;
        }
        Assert.assertTrue(isExceptionRaised);
    }

    @Test
    public void testCreateTypeFlowTable() throws InterruptedException, ExecutionException, IllegalArgumentException{
        // Don't run this test if the table is not supported
        Assume.assumeTrue(schemaVersion.compareTo(flowTableFromVersion) >= 0);

        String flowTableUuidStr = "testFlowTable";
        String tableName = "flow_table_row_name";
        String overflowPolicy = "evict";
        String groups = "group name";
        String prefixes = "wildcarding prefixes";
        Long flowLimit = 50000L;
        Map<Long, UUID> flowTableBrRef = new HashMap<>();
        flowTableBrRef.put(1L, new UUID(flowTableUuidStr));
        FlowTable flowTable = ovs.createTypedRowWrapper(FlowTable.class);
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
        Bridge bridge = ovs.getTypedRowWrapper(Bridge.class, null);
        TransactionBuilder transactionBuilder = ovs.transactBuilder(OpenVswitchSchemaSuiteIT.dbSchema)
                .add(op.insert(flowTable)
                        .withId(flowTableUuidStr))
                .add(op.mutate(bridge.getSchema())
                        .addMutation(bridge.getFlowTablesColumn().getSchema(), Mutator.INSERT,(flowTableBrRef))
                        .where(bridge.getNameColumn().getSchema().opEqual(TEST_BRIDGE_NAME))
                        .build());
        ListenableFuture<List<OperationResult>> results = transactionBuilder.execute();
        List<OperationResult> operationResults = results.get();
        for (OperationResult result : operationResults) Assert.assertNull(result.getError());
        Assert.assertFalse(operationResults.isEmpty());
        // Check if Results matches the number of operations in transaction
        Assert.assertEquals(transactionBuilder.getOperations().size(), operationResults.size());
        logger.info("Insert & Mutate operation results for Flow Table = {} ", operationResults);
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
