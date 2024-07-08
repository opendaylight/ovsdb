/*
 * Copyright © 2014, 2017 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.integrationtest.schema;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.it.LibraryIntegrationTestBase;
import org.opendaylight.ovsdb.lib.it.LibraryIntegrationTestUtils;
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.OperationResult;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.schema.hardwarevtep.Global;
import org.opendaylight.ovsdb.schema.hardwarevtep.Manager;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class HardwareVTEPIT extends LibraryIntegrationTestBase {
    private static final Logger LOG = LoggerFactory.getLogger(HardwareVTEPIT.class);
    private UUID testManagerUuid = null;

    @Override
    @Before
    public void setup() throws Exception {
        schema = LibraryIntegrationTestUtils.HARDWARE_VTEP;
        super.setup();
    }

    public UUID getGlobalTableUuid(OvsdbClient ovs, Map<String, Map<UUID, Row>> tableCache) {
        Global glbl = getClient().getTypedRowWrapper(Global.class, null);
        Map<UUID, Row> glblTbl = tableCache.get(glbl.getSchema().getName());
        if (glblTbl != null) {
            if (glblTbl.keySet().size() >= 1) {
                return (UUID)glblTbl.keySet().toArray()[0];
            }
        }
        return null;
    }

    /**
     * Create a new manager string in addition to whatever is already there
     * Will modify the Global table to include the UUID to the new Manager row.
     */
    @SuppressWarnings("unchecked")
    public void managerInsert() throws ExecutionException, InterruptedException {
        //Ensure test only proceeds if HW VTEP is supported
        assumeTrue(isSchemaSupported(getClient(), LibraryIntegrationTestUtils.HARDWARE_VTEP));

        //proceed only if schema was already retrieved successfully
        Assert.assertNotNull(getDbSchema());

        //create new manager and set target string
        Manager manager = getClient().createTypedRowWrapper(Manager.class);
        manager.setTarget("ptcp:6641");

        String transactionUuidStr = "foobar";

        Global glbl = this.getClient().createTypedRowWrapper(Global.class);
        glbl.setManagers(Collections.singleton(new UUID(transactionUuidStr)));

        TransactionBuilder transactionBuilder = getClient().transactBuilder(getDbSchema())
                .add(op.insert(manager.getSchema())
                        .withId(transactionUuidStr)
                        .value(manager.getTargetColumn()))
                .add(op.comment("Manager: Inserting " + transactionUuidStr))
                .add(op.mutate(glbl.getSchema())
                        .addMutation(glbl.getManagersColumn().getSchema(), Mutator.INSERT,
                                glbl.getManagersColumn().getData()))
                .add(op.comment("Global: Mutating " + transactionUuidStr));

        int insertOperationIndex = 0;
        List<OperationResult> operationResults = executeTransaction(transactionBuilder,
                "Manager: Insert and Mutate results");
        testManagerUuid = operationResults.get(insertOperationIndex).getUuid();
        assertNotNull(ASSERT_TRANS_UUID, testManagerUuid);

        // Verify that the local cache was updated with the remote changes
        Row managerRow = getTableCache().get(manager.getSchema().getName()).get(testManagerUuid);
        Manager monitoredManager = getClient().getTypedRowWrapper(Manager.class, managerRow);
        assertEquals(manager.getTargetColumn().getData(), monitoredManager.getTargetColumn().getData());
        assertNotNull(monitoredManager.getUuid());
        assertNotNull(monitoredManager.getVersion());
        assertNotNull(getGlobalTableUuid(getClient(), getTableCache()));
    }

    public void managerDelete() throws ExecutionException, InterruptedException {
        assumeTrue(isSchemaSupported(getClient(), LibraryIntegrationTestUtils.HARDWARE_VTEP));

        Manager manager = getClient().getTypedRowWrapper(Manager.class, null);
        Global global = getClient().getTypedRowWrapper(Global.class, null);

        TransactionBuilder transactionBuilder = getClient().transactBuilder(getDbSchema())
                .add(op.delete(manager.getSchema())
                        .where(manager.getUuidColumn().getSchema().opEqual(testManagerUuid))
                        .build())
                .add(op.comment("Manager: Deleting " + testManagerUuid))
                .add(op.mutate(global.getSchema())
                        .addMutation(global.getManagersColumn().getSchema(), Mutator.DELETE,
                                Collections.singleton(testManagerUuid)))
                .add(op.comment("Global: Mutating " + testManagerUuid))
                .add(op.commit(true));

        executeTransaction(transactionBuilder, "Manager delete operation results");
    }

    @Test
    public void testManager() throws ExecutionException, InterruptedException {
        managerInsert();
        managerDelete();
    }
}
