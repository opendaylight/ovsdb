/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 *  Authors : Madhu Venugopal, Dave Tucker
 */

package org.opendaylight.ovsdb.schema.openvswitch;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.ovsdb.lib.message.UpdateNotification;
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.OperationResult;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ListenableFuture;

public class ControllerTestCases extends OpenVswitchSchemaTestBase {
    Logger logger = LoggerFactory.getLogger(ControllerTestCases.class);

    @Override
    @Before
    public void setUp() throws ExecutionException, InterruptedException, TimeoutException, IOException {
        super.setUp();
    }

    @Test
    public void createTypedController() throws IOException, InterruptedException, ExecutionException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Controller controller1 = ovs.createTypedRowWrapper(Controller.class);
        controller1.setTarget("tcp:1.1.1.1:6640");
        Controller controller2 = ovs.createTypedRowWrapper(Controller.class);
        controller2.setTarget("tcp:2.2.2.2:6640");

        Bridge bridge = ovs.getTypedRowWrapper(Bridge.class, null);

        String transactionUuidStr = "controller";
        TransactionBuilder transactionBuilder = ovs.transactBuilder()
                .add(op.insert(controller1.getSchema())
                        .withId(transactionUuidStr)
                        .value(controller1.getTargetColumn()))
                .add(op.mutate(bridge.getSchema())
                        .addMutation(bridge.getControllerColumn().getSchema(), Mutator.INSERT,
                                Sets.newHashSet(new UUID(transactionUuidStr)))
                        .where(bridge.getNameColumn().getSchema().opEqual(TEST_BRIDGE_NAME))
                        .build());

        ListenableFuture<List<OperationResult>> results = transactionBuilder.execute();
        List<OperationResult> operationResults = results.get();
        Assert.assertFalse(operationResults.isEmpty());
        // Check if Results matches the number of operations in transaction
        Assert.assertEquals(transactionBuilder.getOperations().size(), operationResults.size());
        logger.info("Insert & Mutate operation results for controller1 = " + operationResults);
        // Check for any errors
        for (OperationResult result : operationResults) {
            Assert.assertNull(result.getError());
        }

        Thread.sleep(3000); // Wait for cache to catchup
        Row bridgeRow = OpenVswitchSchemaSuiteIT.getTableCache().get(bridge.getSchema().getName()).get(OpenVswitchSchemaSuiteIT.getTestBridgeUuid());
        Bridge monitoredBridge = ovs.getTypedRowWrapper(Bridge.class, bridgeRow);
        Assert.assertEquals(1, monitoredBridge.getControllerColumn().getData().size());

        transactionBuilder = ovs.transactBuilder()
                .add(op.insert(controller2.getSchema())
                        .withId(transactionUuidStr)
                        .value(controller2.getTargetColumn()))
                .add(op.mutate(bridge.getSchema())
                        .addMutation(bridge.getControllerColumn().getSchema(), Mutator.INSERT,
                                Sets.newHashSet(new UUID(transactionUuidStr)))
                        .where(bridge.getNameColumn().getSchema().opEqual(TEST_BRIDGE_NAME))
                        .build());

        results = transactionBuilder.execute();
        operationResults = results.get();
        Assert.assertFalse(operationResults.isEmpty());
        // Check if Results matches the number of operations in transaction
        Assert.assertEquals(transactionBuilder.getOperations().size(), operationResults.size());
        logger.info("Insert & Mutate operation results for controller2 = " + operationResults);
        // Check for any errors
        for (OperationResult result : operationResults) {
            Assert.assertNull(result.getError());
        }
        Thread.sleep(3000); // Wait for cache to catchup
        bridgeRow = OpenVswitchSchemaSuiteIT.getTableCache().get(bridge.getSchema().getName()).get(OpenVswitchSchemaSuiteIT.getTestBridgeUuid());
        monitoredBridge = ovs.getTypedRowWrapper(Bridge.class, bridgeRow);
        Assert.assertEquals(2, monitoredBridge.getControllerColumn().getData().size());
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
