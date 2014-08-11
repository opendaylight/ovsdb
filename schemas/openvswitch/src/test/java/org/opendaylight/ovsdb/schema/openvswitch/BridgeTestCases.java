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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ListenableFuture;
;
/**
 * BridgeTestCases covers all the Integration Test Cases required for OVS Bridge.
 *
 * Since OVSDB library is schema independent, these Open_vSwitch schema specific
 * test cases covers the testing needs for both the Schema wrapper layer : schema.openvswitch
 * and the underlying library which performs the schema independent way to access the network
 * entity that talks OVSDB protocol.
 *
 */
public class BridgeTestCases extends OpenVswitchSchemaTestBase {
    Logger logger = LoggerFactory.getLogger(BridgeTestCases.class);

    @Override
    @Before
    public void setUp() throws ExecutionException, InterruptedException, TimeoutException, IOException {
        super.setUp();
     }

    /**
     * Creates a Open_vSwitch Bridge using the schema.openvswitch wrapper Bridge class.
     * This test case performs the following tests and asserts on failures :
     * 1. Creates and asserts on a wrapper software-only Bridge object using the Schema-independent library.
     * 2. Populates Bridge Columns such as Name, Status, Vlans using the wrapper layer.
     * 3. Tests OVSDB libraries transact functionality by inserting the bridge, Mutating the parent Open_vSwitch
     *    table and Updating the bridge all in a single Transaction.
     * 4. Validates the Transaction success condition by
     *    1. Comparing the number of requests with the number or response objects,
     *    2. Checking for any error in any of the response objects.
     * 5. Confirm the Creation of the Bridge by validating against the Table cache that gets populated by the
     *    Monitor operation.
     */
    @Test
    public void createTypedBridge() throws IOException, InterruptedException, ExecutionException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Bridge bridge = this.ovs.createTypedRowWrapper(Bridge.class);
        Assert.assertNotNull(bridge);
        bridge.setName(TEST_BRIDGE_NAME);
        bridge.setStatus(ImmutableMap.of("key", "value"));
        bridge.setFloodVlans(Sets.newHashSet(34L));

        OpenVSwitch openVSwitch = this.ovs.createTypedRowWrapper(OpenVSwitch.class);
        openVSwitch.setBridges(Sets.newHashSet(new UUID(TEST_BRIDGE_NAME)));

        int insertOperationIndex = 0;

        TransactionBuilder transactionBuilder = this.ovs.transactBuilder(OpenVswitchSchemaSuiteIT.dbSchema)
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
        Assert.assertFalse(operationResults.isEmpty());
        // Check if Results matches the number of operations in transaction
        Assert.assertEquals(transactionBuilder.getOperations().size(), operationResults.size());
        logger.info("Insert & Update operation results = " + operationResults);
        for (OperationResult result : operationResults) {
            Assert.assertNull(result.getError());
        }
        OpenVswitchSchemaSuiteIT.setTestBridgeUuid(operationResults.get(insertOperationIndex).getUuid());

        Thread.sleep(3000); // Wait for cache to catchup
        Row bridgeRow = OpenVswitchSchemaSuiteIT.getTableCache().get(bridge.getSchema().getName()).get(OpenVswitchSchemaSuiteIT.getTestBridgeUuid());
        Bridge monitoredBridge = ovs.getTypedRowWrapper(Bridge.class, bridgeRow);
        Assert.assertEquals(monitoredBridge.getNameColumn().getData(), bridge.getNameColumn().getData());
        Assert.assertNotNull(monitoredBridge.getUuid());
        Assert.assertNotNull(monitoredBridge.getVersion());
        Assert.assertNotNull(this.getOpenVSwitchTableUuid(ovs, OpenVswitchSchemaSuiteIT.getTableCache()));
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
