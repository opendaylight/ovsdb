/*
 * Copyright © 2015, 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.OperationResult;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.OpenVSwitch;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class LibraryIT extends LibraryIntegrationTestBase {
    private static final Logger LOG = LoggerFactory.getLogger(LibraryIT.class);
    private static final String TEST_BRIDGE_NAME = "br_test";
    private static UUID testBridgeUuid = null;

    @Override
    @Before
    public void setup() throws Exception {
        schema = LibraryIntegrationTestUtils.OPEN_VSWITCH;
        super.setup();
    }

    private void createTypedBridge(DatabaseSchema dbSchema) throws IOException, InterruptedException,
            ExecutionException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException {
        Bridge bridge = ovsdbClient.createTypedRowWrapper(Bridge.class);
        bridge.setName(TEST_BRIDGE_NAME);
        bridge.setStatus(ImmutableMap.of("key", "value"));
        bridge.setFloodVlans(Collections.singleton(34L));

        OpenVSwitch openVSwitch = ovsdbClient.createTypedRowWrapper(OpenVSwitch.class);
        openVSwitch.setBridges(Collections.singleton(new UUID(TEST_BRIDGE_NAME)));

        TransactionBuilder transactionBuilder = ovsdbClient.transactBuilder(dbSchema)
                .add(op.insert(bridge.getSchema())
                        .withId(TEST_BRIDGE_NAME)
                        .value(bridge.getNameColumn()))
                .add(op.update(bridge.getSchema())
                        .set(bridge.getStatusColumn())
                        .set(bridge.getFloodVlansColumn())
                        .where(bridge.getNameColumn().getSchema().opEqual(bridge.getName()))
                        .and(bridge.getNameColumn().getSchema().opEqual(bridge.getName())).build())
                .add(op.mutate(openVSwitch.getSchema())
                        .addMutation(openVSwitch.getBridgesColumn().getSchema(), Mutator.INSERT,
                                openVSwitch.getBridgesColumn().getData()));

        ListenableFuture<List<OperationResult>> results = transactionBuilder.execute();
        List<OperationResult> operationResults = results.get();
        assertFalse(operationResults.isEmpty());
        // Check if Results matches the number of operations in transaction
        assertEquals(transactionBuilder.getOperations().size(), operationResults.size());
        LOG.info("Insert & Update operation results = {}", operationResults);
        for (OperationResult result : operationResults) {
            assertNull(result.getError());
        }

        int insertOperationIndex = 0;
        testBridgeUuid = operationResults.get(insertOperationIndex).getUuid();
        assertNotNull(testBridgeUuid);
    }

    @Test
    public void tableTest() throws Exception {
        assertNotNull("Invalid Client. Check connection params", ovsdbClient);
        Thread.sleep(3000); // Wait for a few seconds to get the Schema exchange done
        if (isSchemaSupported(LibraryIntegrationTestUtils.OPEN_VSWITCH)) {
            DatabaseSchema dbSchema = ovsdbClient.getSchema(LibraryIntegrationTestUtils.OPEN_VSWITCH).get();
            assertNotNull(dbSchema);
            LOG.info("{} schema in {} with Tables: {}", LibraryIntegrationTestUtils.OPEN_VSWITCH,
                    ovsdbClient.getConnectionInfo(), dbSchema.getTables());

            // A simple Typed Test to make sure a Typed wrapper bundle can coexist in an OSGi environment
            createTypedBridge(dbSchema);
        }

        if (isSchemaSupported(LibraryIntegrationTestUtils.HARDWARE_VTEP)) {
            DatabaseSchema dbSchema = ovsdbClient.getSchema(LibraryIntegrationTestUtils.HARDWARE_VTEP).get();
            assertNotNull(dbSchema);
            LOG.info("{} schema in {} with Tables: {}", LibraryIntegrationTestUtils.HARDWARE_VTEP,
                    ovsdbClient.getConnectionInfo(), dbSchema.getTables());
        }
    }

    @After
    public void tearDown() throws InterruptedException, ExecutionException {
        Bridge bridge = ovsdbClient.getTypedRowWrapper(Bridge.class, null);
        OpenVSwitch openVSwitch = ovsdbClient.getTypedRowWrapper(OpenVSwitch.class, null);
        DatabaseSchema dbSchema = ovsdbClient.getSchema(LibraryIntegrationTestUtils.OPEN_VSWITCH).get();
        ListenableFuture<List<OperationResult>> results = ovsdbClient.transactBuilder(dbSchema)
                .add(op.delete(bridge.getSchema())
                        .where(bridge.getNameColumn().getSchema().opEqual(TEST_BRIDGE_NAME))
                        .build())
                .add(op.mutate(openVSwitch.getSchema())
                        .addMutation(openVSwitch.getBridgesColumn().getSchema(),
                                Mutator.DELETE, Collections.singleton(testBridgeUuid)))
                .add(op.commit(true))
                .execute();

        List<OperationResult> operationResults = results.get();
        LOG.info("Delete operation results = {}", operationResults);
    }
}
