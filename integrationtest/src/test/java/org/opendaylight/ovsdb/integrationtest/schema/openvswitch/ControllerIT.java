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
import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import com.google.common.collect.Sets;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.OperationResult;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.Controller;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
public class ControllerIT extends OpenVSwitchBaseTest {
    private static final Logger LOG = LoggerFactory.getLogger(ControllerIT.class);
    private static final String TEST_CONTROLLER_UUID_STR = "controller";
    private UUID testBridgeUuid = null;
    private UUID testController1Uuid = null;
    private UUID testController2Uuid = null;

    @Inject
    private BundleContext bc;

    @Before
    public void setUp () throws ExecutionException, InterruptedException, IOException {
        areWeReady(bc);
        assertTrue(OPEN_VSWITCH_SCHEMA + " is required.", checkSchema(OPEN_VSWITCH_SCHEMA));
        assertTrue("Failed to monitor tables", monitorTables());
    }

    private void controllerInsert () throws ExecutionException, InterruptedException {
        Controller controller1 = getClient().createTypedRowWrapper(Controller.class);
        controller1.setTarget("tcp:1.1.1.1:6640");
        Controller controller2 = getClient().createTypedRowWrapper(Controller.class);
        controller2.setTarget("tcp:2.2.2.2:6640");
        Bridge bridge = getClient().getTypedRowWrapper(Bridge.class, null);

        // Insert row to Controller table with address in target column
        // Update row in Bridge table with controller uuid in controller column
        TransactionBuilder transactionBuilder = getClient().transactBuilder(getDbSchema())
                .add(op.insert(controller1.getSchema())
                        .withId(TEST_CONTROLLER_UUID_STR)
                        .value(controller1.getTargetColumn()))
                .add(op.comment("Controller: Inserting controller1 " + controller1.getTargetColumn().getData()))
                .add(op.mutate(bridge.getSchema())
                        .addMutation(bridge.getControllerColumn().getSchema(), Mutator.INSERT,
                                Sets.newHashSet(new UUID(TEST_CONTROLLER_UUID_STR)))
                        .where(bridge.getNameColumn().getSchema().opEqual(TEST_BRIDGE_NAME))
                        .build())
                .add(op.comment("Bridge: Mutating controller1 " + controller1.getTargetColumn().getData()));

        List<OperationResult> operationResults = executeTransaction(transactionBuilder,
                "Controller: Insert & Mutate operation results for controller1");
        testController1Uuid = operationResults.get(0).getUuid();
        assertNotNull(ASSERT_TRANS_UUID, testController1Uuid);

        // Verify that the local cache was updated with the remote changes
        Row controllerRow = getTableCache().get(controller1.getSchema().getName()).get(testController1Uuid);
        Controller monitoredController = getClient().getTypedRowWrapper(Controller.class, controllerRow);
        assertEquals(controller1.getTargetColumn().getData(), monitoredController.getTargetColumn().getData());

        Row bridgeRow = getTableCache().get(bridge.getSchema().getName()).get(testBridgeUuid);
        Bridge monitoredBridge = getClient().getTypedRowWrapper(Bridge.class, bridgeRow);
        assertEquals(1, monitoredBridge.getControllerColumn().getData().size());

        transactionBuilder = getClient().transactBuilder(getDbSchema())
                .add(op.insert(controller2.getSchema())
                        .withId(TEST_CONTROLLER_UUID_STR)
                        .value(controller2.getTargetColumn()))
                .add(op.comment("Controller: Inserting controller2 " + controller2.getTargetColumn().getData()))
                .add(op.mutate(bridge.getSchema())
                        .addMutation(bridge.getControllerColumn().getSchema(), Mutator.INSERT,
                                Sets.newHashSet(new UUID(TEST_CONTROLLER_UUID_STR)))
                        .where(bridge.getNameColumn().getSchema().opEqual(TEST_BRIDGE_NAME))
                        .build())
                .add(op.comment("Bridge: Mutating controller2 " + controller2.getTargetColumn().getData()));

        operationResults = executeTransaction(transactionBuilder,
                new String("Controller: Insert & Mutate operation results for controller2"));
        testController2Uuid = operationResults.get(0).getUuid();
        assertNotNull(ASSERT_TRANS_UUID, testController2Uuid);

        // Verify that the local cache was updated with the remote changes
        controllerRow = getTableCache().get(controller2.getSchema().getName()).get(testController2Uuid);
        monitoredController = getClient().getTypedRowWrapper(Controller.class, controllerRow);
        assertEquals(controller2.getTargetColumn().getData(), monitoredController.getTargetColumn().getData());

        bridgeRow = getTableCache().get(bridge.getSchema().getName()).get(testBridgeUuid);
        monitoredBridge = getClient().getTypedRowWrapper(Bridge.class, bridgeRow);
        assertEquals(2, monitoredBridge.getControllerColumn().getData().size());
    }

    private void controllerDelete () throws ExecutionException, InterruptedException {
        Controller controller = getClient().getTypedRowWrapper(Controller.class, null);
        Bridge bridge = getClient().getTypedRowWrapper(Bridge.class, null);
        DatabaseSchema dbSchema = getClient().getSchema(OPEN_VSWITCH_SCHEMA).get();

        TransactionBuilder transactionBuilder = getClient().transactBuilder(dbSchema)
                .add(op.delete(controller.getSchema())
                        .where(controller.getUuidColumn().getSchema().opEqual(testController1Uuid))
                        .build())
                .add(op.comment("Controller: Deleting " + testController1Uuid))
                .add(op.mutate(bridge.getSchema()) // Delete a controller column in the Bridge table
                        .addMutation(bridge.getControllerColumn().getSchema(), Mutator.DELETE,
                                Sets.newHashSet(testController1Uuid)))
                .add(op.comment("Bridge: Mutating " + testController1Uuid))
                .add(op.commit(true));

        executeTransaction(transactionBuilder, "Controller: Delete operation results for controller1");

        transactionBuilder
                .add(op.delete(controller.getSchema())
                        .where(controller.getUuidColumn().getSchema().opEqual(testController2Uuid))
                        .build())
                .add(op.comment("Controller: Deleting " + testController2Uuid))
                .add(op.mutate(bridge.getSchema()) // Delete a controller column in the Bridge table
                        .addMutation(bridge.getControllerColumn().getSchema(), Mutator.DELETE,
                                Sets.newHashSet(testController2Uuid)))
                .add(op.comment("Bridge: Mutating " + testController2Uuid))
                .add(op.commit(true));

        executeTransaction(transactionBuilder, "Controller: Delete operation results for controller2");
    }

    @Test
    public void testController () throws ExecutionException, InterruptedException {
        testBridgeUuid = bridgeInsert();
        controllerInsert();
        controllerDelete();
        bridgeDelete(testBridgeUuid);
    }
}
