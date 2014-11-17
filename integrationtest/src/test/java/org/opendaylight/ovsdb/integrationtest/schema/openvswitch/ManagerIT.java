/*
 *  Copyright (C) 2014 Red Hat, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 *  Authors : Sam Hague, Matt Oswalt
 */
package org.opendaylight.ovsdb.integrationtest.schema.openvswitch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.OperationResult;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.schema.openvswitch.Manager;
import org.opendaylight.ovsdb.schema.openvswitch.OpenVSwitch;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
public class ManagerIT extends OpenVSwitchBaseTest {
    private static final Logger LOG = LoggerFactory.getLogger(ManagerIT.class);
    private static final String TEST_MANAGER_UUID_STR = "sslUuidName";
    private UUID testManagerUuid = null;

    @Inject
    private BundleContext bc;

    @Before
    public void setUp () throws ExecutionException, InterruptedException, IOException {
        areWeReady(bc);
        assertTrue(OPEN_VSWITCH_SCHEMA + " is required.", checkSchema(OPEN_VSWITCH_SCHEMA));
        assertTrue("Failed to monitor tables", monitorTables());
    }

    public void managerInsert() throws InterruptedException, ExecutionException, IllegalArgumentException {
        ImmutableMap<String, String> externalIds = ImmutableMap.of("slaveof", "themaster");
        UUID openVSwitchRowUuid = getOpenVSwitchTableUuid(getClient(), getTableCache());
        OpenVSwitch openVSwitch = getClient().getTypedRowWrapper(OpenVSwitch.class, null);
        Manager manager = getClient().createTypedRowWrapper(Manager.class);
        manager.setInactivityProbe(Sets.newHashSet(8192L));
        manager.setMaxBackoff(Sets.newHashSet(4094L));
        manager.setTarget(Sets.newHashSet("172.16.50.50:6640"));
        manager.setExternalIds(externalIds);

        TransactionBuilder transactionBuilder = getClient().transactBuilder(getDbSchema())
                .add(op.insert(manager.getSchema())
                        .withId(TEST_MANAGER_UUID_STR)
                        .value(manager.getTargetColumn())
                        .value(manager.getInactivityProbeColumn())
                        .value(manager.getMaxBackoffColumn())
                        .value(manager.getExternalIdsColumn()))
                .add(op.comment("Manager: Inserting Slave Manager"))
                .add(op.mutate(openVSwitch.getSchema())
                        .addMutation(openVSwitch.getManagerOptionsColumn().getSchema(), Mutator.INSERT,
                                Sets.newHashSet(new UUID(TEST_MANAGER_UUID_STR)))
                        .where(openVSwitch.getUuidColumn().getSchema().opEqual(openVSwitchRowUuid))
                        .build())
                .add(op.comment("Open_vSwitch: Mutating " + TEST_MANAGER_UUID_STR));

        List<OperationResult> operationResults = executeTransaction(transactionBuilder,
                "Manager: Insert & Mutate operation results");
        testManagerUuid = operationResults.get(0).getUuid();
        assertNotNull(ASSERT_TRANS_UUID, testManagerUuid);

        // Verify that the local cache was updated with the remote changes
        Row managerRow = getTableCache().get(manager.getSchema().getName()).get(testManagerUuid);
        Manager monitoredManager = getClient().getTypedRowWrapper(Manager.class, managerRow);
        assertEquals(externalIds, monitoredManager.getExternalIdsColumn().getData());
    }

    public void managerDelete () throws ExecutionException, InterruptedException {
        Manager manager = getClient().getTypedRowWrapper(Manager.class, null);
        OpenVSwitch openVSwitch = getClient().getTypedRowWrapper(OpenVSwitch.class, null);

        TransactionBuilder transactionBuilder = getClient().transactBuilder(getDbSchema())
                .add(op.delete(manager.getSchema())
                        .where(manager.getUuidColumn().getSchema().opEqual(testManagerUuid))
                        .build())
                .add(op.comment("Manager: Deleting " + TEST_MANAGER_UUID_STR))
                .add(op.mutate(openVSwitch.getSchema())
                        .addMutation(openVSwitch.getManagerOptionsColumn().getSchema(), Mutator.DELETE,
                                Sets.newHashSet(testManagerUuid)))
                .add(op.comment("Open_vSwitch: Mutating " + TEST_MANAGER_UUID_STR + " " + testManagerUuid))
                .add(op.commit(true));

        executeTransaction(transactionBuilder, "Manager: Delete operation results");
    }

    @Test
    public void testManager () throws ExecutionException, InterruptedException {
        managerInsert();
        managerDelete();
    }
}
