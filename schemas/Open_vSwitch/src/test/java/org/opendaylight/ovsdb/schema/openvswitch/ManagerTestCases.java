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


public class ManagerTestCases extends OpenVswitchSchemaTestBase {
    Logger logger = LoggerFactory.getLogger(ManagerTestCases.class);

    @Before
    public void setUp() throws ExecutionException, InterruptedException, TimeoutException, IOException {
        super.setUp();
    }

    @Test
    public void testCreateTypedManagerTable() throws InterruptedException, ExecutionException, IllegalArgumentException {

        String mgrUuidStr = "sslUuidName";
        ImmutableMap<String, String> externalIds = ImmutableMap.of("slaveof", "themaster");

        UUID openVSwitchRowUuid = this.getOpenVSwitchTableUuid(ovs, OpenVswitchSchemaSuiteIT.getTableCache());
        OpenVSwitch openVSwitch = ovs.getTypedRowWrapper(OpenVSwitch.class, null);

        Manager manager = ovs.createTypedRowWrapper(Manager.class);
        manager.setInactivityProbe(Sets.newHashSet(8192));
        manager.setMaxBackoff(Sets.newHashSet(4094));
        manager.setTarget(Sets.newHashSet("172.16.50.50:6640"));
        manager.setExternalIds(externalIds);

        int insertSslOperationIndex = 0;
        TransactionBuilder transactionBuilder = ovs.transactBuilder()
                .add(op.insert(manager.getSchema())
                        .withId(mgrUuidStr)
                        .value(manager.getTargetColumn())
                        .value(manager.getInactivityProbeColumn())
                        .value(manager.getMaxBackoffColumn())
                        .value(manager.getExternalIdsColumn()))
                .add(op.comment("Inserting Slave Manager"))
                .add(op.mutate(openVSwitch.getSchema())
                        .addMutation(openVSwitch.getManagerOptionsColumn().getSchema(), Mutator.INSERT,
                                Sets.newHashSet(new UUID(mgrUuidStr)))
                        .where(openVSwitch.getUuidColumn().getSchema().opEqual(openVSwitchRowUuid))
                        .build());

        ListenableFuture<List<OperationResult>> results = transactionBuilder.execute();
        List<OperationResult> operationResults = results.get();
        Assert.assertFalse(operationResults.isEmpty());
        for (OperationResult result : operationResults) {
            Assert.assertNull(result.getError());
        }
        // Store the returned SSL row UUID to be used in the TearDown deletion transaction
        OpenVswitchSchemaSuiteIT.setTestManagerUuid(operationResults.get(insertSslOperationIndex).getUuid());
        // Check if Results matches the number of operations in transaction
        Assert.assertEquals(transactionBuilder.getOperations().size(), operationResults.size());
        logger.info("Mutate operation results for Manager = {} ", operationResults);

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
