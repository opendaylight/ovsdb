/*
 * Copyright (C) 2014 Matt Oswalt
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Matt Oswalt
 */

package org.opendaylight.ovsdb.schema.hardwarevtep;

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

public class ManagerTestCases extends HardwareVtepSchemaTestBase {
    Logger logger = LoggerFactory.getLogger(ManagerTestCases.class);

    @Before
    public void setUp() throws ExecutionException, InterruptedException, TimeoutException, IOException {
        super.setUp();
     }


    @Test
    public void createTypedManager() throws IOException, InterruptedException, ExecutionException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Manager mgr = ovs.createTypedRowWrapper(Manager.class);
        mgr.setTarget("ptcp:6641");

        Global glbl = ovs.getTypedRowWrapper(Global.class, null);

        String transactionUuidStr = "foobar";
        TransactionBuilder transactionBuilder = ovs.transactBuilder()
                .add(op.insert(mgr.getSchema())
                        .withId(transactionUuidStr)
                        .value(mgr.getTargetColumn()))
                .add(op.mutate(glbl.getSchema())
                        .addMutation(glbl.getManagersColumn().getSchema(), Mutator.INSERT,
                                Sets.newHashSet(new UUID(transactionUuidStr)))
                        .where(mgr.getTargetColumn().getSchema().opEqual("ptcp:6640")) //TODO: Just a test, need to change back
                        .build());

        ListenableFuture<List<OperationResult>> results = transactionBuilder.execute();
        List<OperationResult> operationResults = results.get();
        Assert.assertFalse(operationResults.isEmpty());
        // Check if Results matches the number of operations in transaction
        Assert.assertEquals(transactionBuilder.getOperations().size(), operationResults.size());
        logger.info("Insert & Mutate operation results for new manager = " + operationResults);
        // Check for any errors
        for (OperationResult result : operationResults) {
            Assert.assertNull(result.getError());
        }

        Thread.sleep(3000); // Wait for cache to catchup
        Row bridgeRow = HardwareVtepSchemaSuiteIT.getTableCache().get(mgr.getSchema().getName()).get(HardwareVtepSchemaSuiteIT.getTestManagerUuid());
        Global monitoredGlobal = ovs.getTypedRowWrapper(Global.class, bridgeRow);
        Assert.assertEquals(1, monitoredGlobal.getManagersColumn().getData().size());
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
