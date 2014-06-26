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
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ListenableFuture;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.ovsdb.lib.message.UpdateNotification;
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.OperationResult;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

public class NetflowTestCases extends OpenVswitchSchemaTestBase {

    Logger logger = LoggerFactory.getLogger(NetflowTestCases.class);

    @Before
    public void setUp() throws ExecutionException, InterruptedException, TimeoutException, IOException {
        super.setUp();
    }

    @Test
    public void testCreateTypedNetFlow() throws InterruptedException, ExecutionException, IllegalArgumentException{
        String netFlowUuidStr = "testNetFlow";
        String netFlowTargets = "172.16.20.200:6343";
        BigInteger engineType = BigInteger.valueOf(128);
        BigInteger engineID = BigInteger.valueOf(32);
        Integer activityTimeout = 1;
        NetFlow netFlow = ovs.createTypedRowWrapper(NetFlow.class);
        netFlow.setTargets(ImmutableSet.of(netFlowTargets));
        netFlow.setEngineType(ImmutableSet.of(engineType));
        netFlow.setEngineId(ImmutableSet.of(engineID));
        netFlow.setActivityTimeout(ImmutableSet.of(activityTimeout));
        netFlow.setExternalIds(ImmutableMap.of("big", "baby"));
        Bridge bridge = ovs.getTypedRowWrapper(Bridge.class, null);
        TransactionBuilder transactionBuilder = ovs.transactBuilder()
                .add(op.insert(netFlow.getSchema())
                        .withId(netFlowUuidStr)
                        .value(netFlow.getTargetsColumn())
                        .value(netFlow.getEngineTypeColumn())
                        .value(netFlow.getEngineIdColumn())
                        .value(netFlow.getActiveTimeoutColumn())
                        .value(netFlow.getExternalIdsColumn()))
                .add(op.mutate(bridge.getSchema())
                        .addMutation(bridge.getNetflowColumn().getSchema(), Mutator.INSERT,
                                Sets.newHashSet(new UUID(netFlowUuidStr)))
                        .where(bridge.getNameColumn().getSchema().opEqual(TEST_BRIDGE_NAME))
                        .build());
        ListenableFuture<List<OperationResult>> results = transactionBuilder.execute();
        List<OperationResult> operationResults = results.get();
        for (OperationResult result : operationResults) Assert.assertNull(result.getError());
        Assert.assertFalse(operationResults.isEmpty());
        // Check if Results matches the number of operations in transaction
        Assert.assertEquals(transactionBuilder.getOperations().size(), operationResults.size());
        logger.info("Insert & Mutate operation results for NetFlow = {} ", operationResults);
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
