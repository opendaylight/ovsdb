/*
 *
 *  Copyright (C) 2014 Red Hat, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 *  Authors : Sam Hague
 * /
 */
package org.opendaylight.ovsdb.integrationtest.schema.openvswitch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.ovsdb.lib.error.SchemaVersionMismatchException;
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.notation.Version;
import org.opendaylight.ovsdb.lib.operations.OperationResult;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.FlowTable;
import org.opendaylight.ovsdb.schema.openvswitch.IPFIX;
import org.opendaylight.ovsdb.schema.openvswitch.OpenVSwitch;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
public class IpfixIT extends OpenVSwitchBaseTest {
    private static final Logger LOG = LoggerFactory.getLogger(IpfixIT.class);
    private UUID testIpfixUuid = null;
    Version schemaVersion;
    Version ipfixFromVersion = Version.fromString("7.1.0");
    Version ipfixCacheFromVersion = Version.fromString("7.3.0");

    @Inject
    private BundleContext bc;

    @Before
    public void setUp () throws ExecutionException, InterruptedException, IOException {
        areWeReady(bc);
        assertTrue(OPEN_VSWITCH_SCHEMA + " is required.", checkSchema(OPEN_VSWITCH_SCHEMA));
        assertTrue("Failed to monitor tables", monitorTables());
        schemaVersion = getClient().getDatabaseSchema("Open_vSwitch").getVersion();
    }

    @Test
    public void testIpfixTableNotSupported () {
        // Don't run this test if the table is not supported
        assumeTrue(schemaVersion.compareTo(ipfixFromVersion) < 0);
        boolean isExceptionRaised = false;
        try {
            IPFIX ipfix = getClient().createTypedRowWrapper(IPFIX.class);
        } catch (SchemaVersionMismatchException e) {
            isExceptionRaised = true;
        }
        assertTrue(isExceptionRaised);
    }

    public void testIpfixInsert () throws ExecutionException, IllegalArgumentException, InterruptedException {

        // Don't run this test if the table is not supported
        assumeTrue(schemaVersion.compareTo(ipfixFromVersion) >= 0);

        String ipfixUuidStr = "testIpfix";
        String ipfixTarget = "172.16.20.1:4739";
        Long obsDomainId = 112L;
        Long obsPointId = 358L;
        Long cacheMax = 132L;
        Long cacheTimeout = 134L;
        Long sampling = 558L;

        IPFIX ipfix = getClient().createTypedRowWrapper(IPFIX.class);
        ipfix.setTargets(ImmutableSet.of(ipfixTarget));
        ipfix.setObsDomainId(ImmutableSet.of(obsDomainId));
        ipfix.setObsPointId(ImmutableSet.of(obsPointId));
        // Only set these rows if the schema version supports it
        if (schemaVersion.compareTo(ipfixCacheFromVersion) >= 0) {
            ipfix.setCacheMaxFlows(ImmutableSet.of(cacheMax));
            ipfix.setCacheActiveTimeout(ImmutableSet.of(cacheTimeout));
        }
        ipfix.setSampling(ImmutableSet.of(sampling));
        ipfix.setExternalIds(ImmutableMap.of("I <3", "ovs"));
        Bridge bridge = getClient().getTypedRowWrapper(Bridge.class, null);

        TransactionBuilder transactionBuilder = getClient().transactBuilder(getDbSchema())
                .add(op.insert(ipfix)
                        .withId(ipfixUuidStr))
                .add(op.comment("IPFIX: Inserting " + ipfixUuidStr))
                .add(op.mutate(bridge.getSchema())
                        .addMutation(bridge.getIpfixColumn().getSchema(), Mutator.INSERT,
                                Sets.newHashSet(new UUID(ipfixUuidStr)))
                        .where(bridge.getNameColumn().getSchema().opEqual(TEST_BRIDGE_NAME))
                        .build())
                .add(op.comment("Bridge: Mutating " + ipfixUuidStr));

        List<OperationResult> operationResults = executeTransaction(transactionBuilder,
                "IPFIX: Insert and Mutate results");
        testIpfixUuid = operationResults.get(0).getUuid();
        assertNotNull(ASSERT_TRANS_UUID, testIpfixUuid);

        // Verify that the local cache was updated with the remote changes
        Row row = getTableCache().get(ipfix.getSchema().getName()).get(testIpfixUuid);
        IPFIX monitoredTable = getClient().getTypedRowWrapper(IPFIX.class, row);
        assertNotNull(ASSERT_TRANS_UUID + "1", monitoredTable);

        assertEquals(testIpfixUuid, monitoredTable.getUuidColumn().getData());
    }

    public void testIpfixDelete () throws ExecutionException, InterruptedException {
        FlowTable flowTable = getClient().getTypedRowWrapper(FlowTable.class, null);
        OpenVSwitch openVSwitch = getClient().getTypedRowWrapper(OpenVSwitch.class, null);

        TransactionBuilder transactionBuilder = getClient().transactBuilder(getDbSchema())
                .add(op.delete(flowTable.getSchema())
                        .where(flowTable.getUuidColumn().getSchema().opEqual(testIpfixUuid))
                        .build())
                .add(op.comment("IPFIX: Deleting " + testIpfixUuid))
                .add(op.mutate(openVSwitch.getSchema())
                        .addMutation(openVSwitch.getManagerOptionsColumn().getSchema(), Mutator.DELETE,
                                Sets.newHashSet(testIpfixUuid)))
                .add(op.comment("Open_vSwitch: Mutating " + testIpfixUuid))
                .add(op.commit(true));

        executeTransaction(transactionBuilder, "IPFIX delete operation results");
    }

    @Test
    public void testIpfix () throws ExecutionException, InterruptedException {
        UUID testBridgeUuid = bridgeInsert();
        testIpfixInsert();
        testIpfixDelete();
        bridgeDelete(testBridgeUuid);
    }
}
