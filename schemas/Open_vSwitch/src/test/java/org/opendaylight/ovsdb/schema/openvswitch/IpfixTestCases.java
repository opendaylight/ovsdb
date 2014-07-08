/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 *  Authors : Brent Salisbury, Dave Tucker
 */

package org.opendaylight.ovsdb.schema.openvswitch;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ListenableFuture;
import junit.framework.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.ovsdb.lib.error.SchemaVersionMismatchException;
import org.opendaylight.ovsdb.lib.message.UpdateNotification;
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.notation.Version;
import org.opendaylight.ovsdb.lib.operations.OperationResult;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

public class IpfixTestCases extends OpenVswitchSchemaTestBase {

    Logger logger = LoggerFactory.getLogger(IpfixTestCases.class);
    Version schemaVersion;
    Version ipfixFromVersion = Version.fromString("7.1.0");
    Version ipfixCacheFromVersion = Version.fromString("7.3.0");

    @Before
    public void setUp() throws ExecutionException, InterruptedException, TimeoutException, IOException {
        super.setUp();
        schemaVersion = ovs.getDatabaseSchema("Open_vSwitch").getVersion();
    }

    @Test
    public void testTableNotSupported() {
        // Don't run this test if we can run the IPFIX test
        Assume.assumeTrue(schemaVersion.compareTo(ipfixFromVersion) < 0);
        boolean isExceptionRaised = false;
        try {
            IPFIX ipfix = ovs.createTypedRowWrapper(IPFIX.class);
        } catch (SchemaVersionMismatchException e) {
            isExceptionRaised = true;
        }
        Assert.assertTrue(isExceptionRaised);
    }

    @Test
    public void testCreateTypedIpFix() throws InterruptedException, ExecutionException, IllegalArgumentException{
        // Don't run this test if the table is not supported
        Assume.assumeTrue(schemaVersion.compareTo(ipfixFromVersion) >= 0);
        String ipfixUuidStr = "testIpfix";
        String ipfixTarget = "172.16.20.1:4739";
        Long obsDomainId = 112L;
        Long obsPointId = 358L;
        Long cacheMax = 132L;
        Long cacheTimeout = 134L;
        Long sampling = 558L;

        IPFIX ipfix = ovs.createTypedRowWrapper(IPFIX.class);
        ipfix.setTargets(ImmutableSet.of(ipfixTarget));
        ipfix.setObsDomainId(ImmutableSet.of(obsDomainId));
        ipfix.setObsPointId(ImmutableSet.of(obsPointId));
        // Only set these rows if the schema version supports it
        if (schemaVersion.compareTo(ipfixCacheFromVersion) >= 0) {
            ipfix.setCacheMaxFlows(ImmutableSet.of(cacheMax));
            ipfix.setCacheActiveTimeout(ImmutableSet.of(cacheTimeout));
        }
        ipfix.setSampling(ImmutableSet.of(sampling));
        ipfix.setExternalIds(ImmutableMap.of("<3", "ovs"));
        Bridge bridge = ovs.getTypedRowWrapper(Bridge.class, null);
        TransactionBuilder transactionBuilder = ovs.transactBuilder()
                .add(op.insert(ipfix)
                        .withId(ipfixUuidStr))
                .add(op.mutate(bridge.getSchema())
                        .addMutation(bridge.getIpfixColumn().getSchema(), Mutator.INSERT,
                                Sets.newHashSet(new UUID(ipfixUuidStr)))
                        .where(bridge.getNameColumn().getSchema().opEqual(TEST_BRIDGE_NAME))
                        .build());
        ListenableFuture<List<OperationResult>> results = transactionBuilder.execute();
        List<OperationResult> operationResults = results.get();
        Assert.assertFalse(operationResults.isEmpty());
        // Check if Results matches the number of operations in transaction
        Assert.assertEquals(transactionBuilder.getOperations().size(), operationResults.size());
        for (OperationResult result : operationResults) Assert.assertNull(result.getError());
        logger.info("Insert & Mutate operation results for IPFIX = {} ", operationResults);
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
