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
import junit.framework.Assert;
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

    /**
     * Tear down the rows in the OVSDB server created by the
     * integration tests. Deleting the parent Bridge Row in the
     * Bridge table will cause the OVSDB server to GC most most
     * of the table rows. The remaining tables with references
     * are deleted with the follwoing transaction.
     *
     * @throws InterruptedException the interrupted exception
     * @throws ExecutionException the execution exception
     * @throws IOException the iO exception
     * @throws TimeoutException the timeout exception
     */
    @Test
    public void tearDown() throws InterruptedException, ExecutionException, IOException, TimeoutException {
        Bridge bridge = this.ovs.getTypedRowWrapper(Bridge.class, null);
        OpenVSwitch openVSwitch = this.ovs.getTypedRowWrapper(OpenVSwitch.class, null);
        SSL ssl = this.ovs.getTypedRowWrapper(SSL.class, null);
        Qos qos = this.ovs.getTypedRowWrapper(Qos.class, null);
        Port port = this.ovs.getTypedRowWrapper(Port.class, null);
        Queue queue = this.ovs.getTypedRowWrapper(Queue.class, null);
        Manager manager = this.ovs.getTypedRowWrapper(Manager.class, null);

        TransactionBuilder transactionBuilder = this.ovs.transactBuilder(OpenVswitchSchemaSuiteIT.dbSchema);

        if (schemaVersion.compareTo(flowSampleCollectorSetFromVersion) >= 0) {
            FlowSampleCollectorSet flowSampleCollectorSet = this.ovs.getTypedRowWrapper(FlowSampleCollectorSet.class, null);
            transactionBuilder.add(op.delete(flowSampleCollectorSet.getSchema())
                    .where(flowSampleCollectorSet.getBridgeColumn().getSchema().opEqual(OpenVswitchSchemaSuiteIT.getTestBridgeUuid()))
                    .build());
        }
        transactionBuilder.add(op.delete(bridge.getSchema())
                .where(bridge.getNameColumn().getSchema().opEqual(TEST_BRIDGE_NAME))
                .build())
                .add(op.delete(ssl.getSchema()) // Delete an SSL row in the SSL table
                        .where(ssl.getUuidColumn().getSchema()
                                .opEqual(OpenVswitchSchemaSuiteIT.getTestSslUuid()))
                        .build())
                .add(op.delete(manager.getSchema()) // Delete a Manager row in the SSL table
                        .where(manager.getUuidColumn().getSchema()
                                .opEqual(OpenVswitchSchemaSuiteIT.getTestManagerUuid()))
                        .build())
                .add(op.delete(queue.getSchema()) // Delete a Queue row in the Queue table
                        .where(queue.getUuidColumn().getSchema()
                                .opEqual(OpenVswitchSchemaSuiteIT.getTestQueueUuid()))
                        .build())
                .add(op.delete(qos.getSchema()) // Delete a QoS row in the QOS table
                        .where(qos.getUuidColumn().getSchema()
                                .opEqual(OpenVswitchSchemaSuiteIT.getTestQosUuid()))
                        .build())
                .add(op.delete(port.getSchema()) // Delete a Port row in the Port table
                        .where(port.getUuidColumn().getSchema()
                                .opEqual(OpenVswitchSchemaSuiteIT.getTestQosPortUuid()))
                        .build())
                .add(op.mutate(openVSwitch.getSchema()) // Delete a manager_opt column in the OVS table
                        .addMutation(openVSwitch.getManagerOptionsColumn().getSchema(), Mutator.DELETE,
                                Sets.newHashSet(OpenVswitchSchemaSuiteIT.getTestManagerUuid())))
                .add(op.mutate(openVSwitch.getSchema()) // Delete an SSL column in the OVS table
                        .addMutation(openVSwitch.getSslColumn().getSchema(), Mutator.DELETE,
                                Sets.newHashSet(OpenVswitchSchemaSuiteIT.getTestSslUuid())))
                .add(op.mutate(port.getSchema()) // Delete the Qos reference in the Qos test Port row
                        .addMutation(port.getQosColumn().getSchema(), Mutator.DELETE,
                                Sets.newHashSet(OpenVswitchSchemaSuiteIT.getTestQosPortUuid())))
                .add(op.mutate(openVSwitch.getSchema()) // Delete a bridge column reference in the OVS table
                        .addMutation(openVSwitch.getBridgesColumn().getSchema(), Mutator.DELETE,
                                Sets.newHashSet(OpenVswitchSchemaSuiteIT.getTestBridgeUuid())))
                .add(op.commit(true))
                .execute();

        ListenableFuture<List<OperationResult>> results = transactionBuilder.execute();
        List<OperationResult> operationResults = results.get();
        for (OperationResult result : operationResults) {
            Assert.assertNull(result.getError());
        }
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
