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

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.ovsdb.lib.message.UpdateNotification;
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.notation.Version;
import org.opendaylight.ovsdb.lib.operations.OperationResult;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

public class TearDown extends OpenVswitchSchemaTestBase {

    Version schemaVersion;
    Version flowSampleCollectorSetFromVersion = Version.fromString("7.1.0");

    @Before
    public void setUp() throws ExecutionException, InterruptedException, TimeoutException, IOException {
        super.setUp();
        schemaVersion = ovs.getDatabaseSchema("Open_vSwitch").getVersion();
    }

    @Test
    public void tearDown() throws InterruptedException, ExecutionException, IOException, TimeoutException {
        Bridge bridge = this.ovs.getTypedRowWrapper(Bridge.class, null);
        OpenVSwitch openVSwitch = this.ovs.getTypedRowWrapper(OpenVSwitch.class, null);

        TransactionBuilder transactionBuilder = this.ovs.transactBuilder();

        if (schemaVersion.compareTo(flowSampleCollectorSetFromVersion) >= 0) {
            FlowSampleCollectorSet flowSampleCollectorSet = this.ovs.getTypedRowWrapper(FlowSampleCollectorSet.class, null);
            transactionBuilder.add(op.delete(flowSampleCollectorSet.getSchema())
                    .where(flowSampleCollectorSet.getBridgeColumn().getSchema().opEqual(OpenVswitchSchemaSuiteIT.getTestBridgeUuid()))
                    .build());
        }
        transactionBuilder.add(op.delete(bridge.getSchema())
                .where(bridge.getNameColumn().getSchema().opEqual(TEST_BRIDGE_NAME))
                .build())
                .add(op.mutate(openVSwitch.getSchema())
                        .addMutation(openVSwitch.getBridgesColumn().getSchema(), Mutator.DELETE, Sets.newHashSet(OpenVswitchSchemaSuiteIT.getTestBridgeUuid())))
                .add(op.commit(true))
                .execute();

        ListenableFuture<List<OperationResult>> results = transactionBuilder.execute();
        List<OperationResult> operationResults = results.get();
        System.out.println("Delete operation results = " + operationResults);
        // tableCache = new HashMap<String, Map<UUID, Row>>();
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
