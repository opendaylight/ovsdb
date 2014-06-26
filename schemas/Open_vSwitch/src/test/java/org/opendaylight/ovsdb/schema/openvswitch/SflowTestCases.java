/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 *  Authors : Brent Salisbury
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
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertNull;
import static org.opendaylight.ovsdb.lib.operations.Operations.op;

public class SflowTestCases extends OpenVswitchSchemaTestBase {

    Logger logger = LoggerFactory.getLogger(SflowTestCases.class);

    @Before
    public void setUp() throws ExecutionException, InterruptedException, TimeoutException, IOException {
        super.setUp();
    }

    @Test
    public void testCreateTypedSflow() throws InterruptedException, ExecutionException, IllegalArgumentException{
        String sFlowUuidStr = "testSFlow";
        String sFlowTarget = "172.16.20.200:6343";
        Integer header = 128;
        Integer obsPointId = 358;
        Integer polling = 10;
        String agent = "172.16.20.210";
        Integer sampling = 64;
        SFlow sFlow = ovs.createTypedRowWrapper(SFlow.class);
        sFlow.setTargets(ImmutableSet.of(sFlowTarget));
        sFlow.setHeader(ImmutableSet.of(header));
        sFlow.setPolling(ImmutableSet.of(obsPointId));
        sFlow.setPolling(ImmutableSet.of(polling));
        sFlow.setAgent(ImmutableSet.of(agent));
        sFlow.setSampling(ImmutableSet.of(sampling));
        sFlow.setExternalIds(ImmutableMap.of("kit", "tah"));
        Bridge bridge = ovs.getTypedRowWrapper(Bridge.class, null);
        TransactionBuilder transactionBuilder = ovs.transactBuilder()
                .add(op.insert(sFlow.getSchema())
                        .withId(sFlowUuidStr)
                        .value(sFlow.getTargetsColumn())
                        .value(sFlow.getHeaderColumn())
                        .value(sFlow.getPollingColumn())
                        .value(sFlow.getAgentColumn())
                        .value(sFlow.getSamplingColumn())
                        .value(sFlow.getExternalIdsColumn()))
                .add(op.mutate(bridge.getSchema())
                        .addMutation(bridge.getSflowColumn().getSchema(), Mutator.INSERT,
                                Sets.newHashSet(new UUID(sFlowUuidStr)))
                        .where(bridge.getNameColumn().getSchema().opEqual(TEST_BRIDGE_NAME))
                        .build());
        ListenableFuture<List<OperationResult>> results = transactionBuilder.execute();
        List<OperationResult> operationResults = results.get();
        for (OperationResult result : operationResults) {
            assertNull(result.getError());
        }
        Assert.assertFalse(operationResults.isEmpty());
        // Check if Results matches the number of operations in transaction
        Assert.assertEquals(transactionBuilder.getOperations().size(), operationResults.size());
        logger.info("Insert & Mutate operation results for SFlow = {} ", operationResults);
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
