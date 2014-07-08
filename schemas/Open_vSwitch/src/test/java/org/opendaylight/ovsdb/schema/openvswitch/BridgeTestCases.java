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

public class BridgeTestCases extends OpenVswitchSchemaTestBase {
    Logger logger = LoggerFactory.getLogger(BridgeTestCases.class);

    @Override
    @Before
    public void setUp() throws ExecutionException, InterruptedException, TimeoutException, IOException {
        super.setUp();
     }

    @Test
    public void createTypedBridge() throws IOException, InterruptedException, ExecutionException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Bridge bridge = this.ovs.createTypedRowWrapper(Bridge.class);
        Assert.assertNotNull(bridge);
        bridge.setName(TEST_BRIDGE_NAME);
        bridge.setStatus(ImmutableMap.of("key", "value"));
        //Long vlanhash = Long.valueOf(34);
        bridge.setFloodVlans(Sets.newHashSet(34L));
        //bridge.setFloodVlans(Sets.newHashSet(vlanhash));

        OpenVSwitch openVSwitch = this.ovs.createTypedRowWrapper(OpenVSwitch.class);
        openVSwitch.setBridges(Sets.newHashSet(new UUID(TEST_BRIDGE_NAME)));

        int insertOperationIndex = 0;

        TransactionBuilder transactionBuilder = this.ovs.transactBuilder()
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
