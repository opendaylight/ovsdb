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

    /**
     * Create a new manager string in addition to whatever is already there
     * Will modify the Global table to include the UUID to the new Manager row
     */
    @Test
    public void createManager() throws IOException, InterruptedException, ExecutionException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        //proceed only if schema was already retrieved successfully
        Assert.assertNotNull(HardwareVtepSchemaSuiteIT.dbSchema);

        //create new manager and set target string
        Manager mgr = ovs.createTypedRowWrapper(Manager.class);
        mgr.setTarget("ptcp:6641");

        String transactionUuidStr = "foobar";
        int insertOperationIndex = 0;

        Global glbl = this.ovs.createTypedRowWrapper(Global.class);
        glbl.setManagers(Sets.newHashSet(new UUID(transactionUuidStr)));

        TransactionBuilder transactionBuilder = ovs.transactBuilder(HardwareVtepSchemaSuiteIT.dbSchema)
                .add(op.insert(mgr.getSchema())
                        .withId(transactionUuidStr)
                        .value(mgr.getTargetColumn()))
                .add(op.mutate(glbl.getSchema())
                        .addMutation(glbl.getManagersColumn().getSchema(), Mutator.INSERT,
                                glbl.getManagersColumn().getData()));


        ListenableFuture<List<OperationResult>> results = transactionBuilder.execute();
        List<OperationResult> operationResults = results.get();

        //ensure we received the results of the operation
        Assert.assertFalse(operationResults.isEmpty());
        // Check if Results matches the number of operations in transaction
        Assert.assertEquals(transactionBuilder.getOperations().size(), operationResults.size());

        logger.info("Insert & Mutate operation results for new manager = " + operationResults);

        // Check for any errors
        for (OperationResult result : operationResults) {
            Assert.assertNull(result.getError());
        }

        HardwareVtepSchemaSuiteIT.setTestManagerUuid(operationResults.get(insertOperationIndex).getUuid());

        Thread.sleep(3000); // Wait for cache to catchup

        Row managerRow = HardwareVtepSchemaSuiteIT.getTableCache().get(mgr.getSchema().getName()).get(HardwareVtepSchemaSuiteIT.getTestManagerUuid());
        Manager monitoredManager = ovs.getTypedRowWrapper(Manager.class, managerRow);
        Assert.assertEquals(monitoredManager.getTargetColumn().getData(), mgr.getTargetColumn().getData());
        Assert.assertNotNull(monitoredManager.getUuid());
        Assert.assertNotNull(monitoredManager.getVersion());
        Assert.assertNotNull(this.getGlobalTableUuid(ovs, HardwareVtepSchemaSuiteIT.getTableCache()));
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