/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 *  Authors : Dave Tucker
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

/**
 * Created by dave on 26/06/2014.
 */
public class PortAndInterfaceTestCases extends OpenVswitchSchemaTestBase {

    Logger logger = LoggerFactory.getLogger(PortAndInterfaceTestCases.class);

    @Before
    public void setUp() throws ExecutionException, InterruptedException, TimeoutException, IOException {
        super.setUp();
    }


    @Test
    public void testCreateTypedPortandInterface() throws InterruptedException, ExecutionException {
        String portUuidStr = "testPort";
        String intfUuidStr = "testIntf";
        Port port = ovs.createTypedRowWrapper(Port.class);
        port.setName("testPort");
        port.setTag(ImmutableSet.of(BigInteger.ONE));
        port.setMac(ImmutableSet.of("00:00:00:00:00:01"));
        port.setInterfaces(ImmutableSet.of(new UUID(intfUuidStr)));

        Interface intf = ovs.createTypedRowWrapper(Interface.class);
        intf.setName(port.getNameColumn().getData());
        intf.setExternalIds(ImmutableMap.of("vm-id", "12345abcedf78910"));

        Bridge bridge = ovs.getTypedRowWrapper(Bridge.class, null);
        TransactionBuilder transactionBuilder = ovs.transactBuilder()
                .add(op.insert(port.getSchema())
                        .withId(portUuidStr)
                        .value(port.getNameColumn())
                        .value(port.getMacColumn()))
                .add(op.insert(intf.getSchema())
                        .withId(intfUuidStr)
                        .value(intf.getNameColumn()))
                .add(op.update(port.getSchema())
                        .set(port.getTagColumn())
                        .set(port.getMacColumn())
                        .set(port.getInterfacesColumn())
                        .where(port.getNameColumn().getSchema().opEqual(port.getName()))
                        .build())
                .add(op.update(intf.getSchema())
                        .set(intf.getExternalIdsColumn())
                        .where(intf.getNameColumn().getSchema().opEqual(intf.getName()))
                        .build())
                .add(op.mutate(bridge.getSchema())
                        .addMutation(bridge.getPortsColumn().getSchema(), Mutator.INSERT, Sets.newHashSet(new UUID(portUuidStr)))
                        .where(bridge.getNameColumn().getSchema().opEqual(TEST_BRIDGE_NAME))
                        .build());
        ListenableFuture<List<OperationResult>> results = transactionBuilder.execute();
        List<OperationResult> operationResults = results.get();
        Assert.assertFalse(operationResults.isEmpty());
        // Check if Results matches the number of operations in transaction
        Assert.assertEquals(transactionBuilder.getOperations().size(), operationResults.size());
        logger.info("Insert & Mutate operation results for Port and Interface = " + operationResults);
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
