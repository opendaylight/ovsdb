/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
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

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

public class QosTestCases extends OpenVswitchSchemaTestBase {
    Logger logger = LoggerFactory.getLogger(QosTestCases.class);

    @Before
    public void setUp() throws ExecutionException, InterruptedException, TimeoutException, IOException {
        super.setUp();
    }

    /**
     * Test creates a functional port (including Interface table row)
     * on br_test with a reference to a QoS row. The QueueTestCases
     * IT test builds upon these elements and adds a Queue that is
     * referenced in the QoS queue column.
     *
     * @throws InterruptedException the interrupted exception
     * @throws ExecutionException   the execution exception
     */
    @Test
    public void testCreateTypedQos() throws InterruptedException, ExecutionException {

        String portUuidStr = "testQosPortUuid";
        String intfUuidStr = "testQosIntfUuid";
        String qosUuidStr = "testQosUuid";
        String qosPort = "testQosPort";


        Port port = ovs.createTypedRowWrapper(Port.class);
        port.setName(qosPort);
        port.setInterfaces(ImmutableSet.of(new UUID(intfUuidStr)));
        port.setQos(ImmutableSet.of(new UUID(qosUuidStr)));
        port.setOtherConfig(ImmutableMap.of("m0r3", "c0ff33"));

        Interface intf = ovs.createTypedRowWrapper(Interface.class);
        intf.setName(port.getNameColumn().getData());
        intf.setOtherConfig(ImmutableMap.of("proto", "duction"));
        intf.setExternalIds(ImmutableMap.of("stringly", "typed"));

        Qos qos = ovs.createTypedRowWrapper(Qos.class);
        qos.setOtherConfig(ImmutableMap.of("mmm", "kay"));
        qos.setType(ImmutableSet.of("404"));

        int insertPortOperationIndex = 0;
        int insertQosOperationIndex = 2;
        Bridge bridge = ovs.getTypedRowWrapper(Bridge.class, null);

        TransactionBuilder transactionBuilder = ovs.transactBuilder()
                .add(op.insert(port.getSchema())
                        .withId(portUuidStr)
                        .value(port.getNameColumn()))
                .add(op.insert(intf.getSchema())
                        .withId(intfUuidStr)
                        .value(intf.getExternalIdsColumn())
                        .value(intf.getNameColumn())
                        .value(intf.getOtherConfigColumn()))
                .add(op.insert(qos.getSchema())
                        .withId(qosUuidStr)
                        .value(qos.getTypeColumn())
                        .value(qos.getOtherConfigColumn()))
                .add(op.update(port.getSchema())
                        .set(port.getOtherConfigColumn())
                        .set(port.getInterfacesColumn())
                        .set(port.getQosColumn())
                        .where(port.getNameColumn().getSchema().opEqual(port.getName()))
                        .build())
                .add(op.mutate(bridge.getSchema())
                        .addMutation(bridge.getPortsColumn().getSchema(), Mutator.INSERT,
                                Sets.newHashSet(new UUID(portUuidStr)))
                        .where(bridge.getNameColumn().getSchema().opEqual(TEST_BRIDGE_NAME))
                        .build());

        ListenableFuture<List<OperationResult>> results = transactionBuilder.execute();
        List<OperationResult> operationResults = results.get();
        Assert.assertFalse(operationResults.isEmpty());
        for (OperationResult result : operationResults) {
            Assert.assertNull(result.getError());
        }
        // Check if Results matches the number of operations in transaction
        Assert.assertEquals(transactionBuilder
                .getOperations().size(), operationResults.size());
        // Store the returned QoS row UUID to be used in the TearDown deletion transaction
        OpenVswitchSchemaSuiteIT.setTestQosUuid
                (operationResults.get(insertQosOperationIndex).getUuid());
        // Store the returned Port row UUID to be used in the TearDown deletion transaction
        OpenVswitchSchemaSuiteIT.setTestQosPortUuid
                (operationResults.get(insertPortOperationIndex).getUuid());
        logger.info("Insert, Update & Mutate operation results for QOS " + operationResults);
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