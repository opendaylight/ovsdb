/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 *  Authors : Madhu Venugopal
 */

package org.opendaylight.ovsdb.schema.hardwarevtep;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import junit.framework.Assert;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.ovsdb.lib.message.UpdateNotification;
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.operations.OperationResult;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ListenableFuture;

public class TearDown extends HardwareVtepSchemaTestBase {

    @Override
    @Before
    public void setUp() throws ExecutionException, InterruptedException, TimeoutException, IOException {
        super.setUp();
    }

    /**
     * Tear down the rows in the OVSDB server created by the
     * integration tests.
     *
     * @throws InterruptedException the interrupted exception
     * @throws ExecutionException the execution exception
     * @throws IOException the iO exception
     * @throws TimeoutException the timeout exception
     */
    @Test
    public void tearDown() throws InterruptedException, ExecutionException, IOException, TimeoutException {
        Assume.assumeTrue(super.checkSchema());
        Manager manager = this.ovs.getTypedRowWrapper(Manager.class, null);
        Global glbl = this.ovs.getTypedRowWrapper(Global.class, null);
        TransactionBuilder transactionBuilder = this.ovs.transactBuilder(HardwareVtepSchemaSuiteIT.dbSchema);

        transactionBuilder.add(op.delete(manager.getSchema())
                .where(manager.getUuidColumn().getSchema().opEqual(HardwareVtepSchemaSuiteIT.getTestManagerUuid()))
                        .build())
                .add(op.mutate(glbl.getSchema())
                        .addMutation(glbl.getManagersColumn().getSchema(), Mutator.DELETE,
                                Sets.newHashSet(HardwareVtepSchemaSuiteIT.getTestManagerUuid())))
                .add(op.commit(true))
                .execute();

        ListenableFuture<List<OperationResult>> results = transactionBuilder.execute();
        List<OperationResult> operationResults = results.get();
        for (OperationResult result : operationResults) {
            Assert.assertNull(result.getError());
        }
        System.out.println("Delete operation results = " + operationResults);
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
