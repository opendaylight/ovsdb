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

public class MirrorTestCases extends OpenVswitchSchemaTestBase {

    Logger logger = LoggerFactory.getLogger(MirrorTestCases.class);

    @Before
    public void setUp() throws ExecutionException, InterruptedException, TimeoutException, IOException {
        super.setUp();
    }

    @Test
    public void testCreateTypedMirror() throws InterruptedException, ExecutionException, IllegalArgumentException{

        String mirrorUuidStr = "testMirror";
        String mirrorName = "my_name_is_mirror";
        Long outputVid = 1024L;
        Long selectVid = Long.valueOf(2048);

        Mirror mirror = ovs.createTypedRowWrapper(Mirror.class);
        mirror.setName(ImmutableSet.of(mirrorName));
        mirror.setExternalIds(ImmutableMap.of("overlays", "ftw"));
        mirror.setOutputVlan(ImmutableSet.of(outputVid));
        mirror.setSelectVlan(ImmutableSet.of(selectVid));
        mirror.setExternalIds(ImmutableMap.of("reading", "urmail"));
        Bridge bridge = ovs.getTypedRowWrapper(Bridge.class, null);

        int insertMirrorOperationIndex = 0;
        TransactionBuilder transactionBuilder = ovs.transactBuilder(OpenVswitchSchemaSuiteIT.dbSchema)
                .add(op.insert(mirror.getSchema())
                        .withId(mirrorUuidStr)
                        .value(mirror.getNameColumn())
                        .value(mirror.getExternalIdsColumn()))
                .add(op.mutate(bridge.getSchema())
                        .addMutation(bridge.getMirrorsColumn().getSchema(), Mutator.INSERT,
                                Sets.newHashSet(new UUID(mirrorUuidStr)))
                        .where(bridge.getNameColumn().getSchema().opEqual(TEST_BRIDGE_NAME))
                        .build());

        ListenableFuture<List<OperationResult>> results = transactionBuilder.execute();
        List<OperationResult> operationResults = results.get();
        Assert.assertFalse(operationResults.isEmpty());
        for (OperationResult result : operationResults) {
            Assert.assertNull(result.getError());
        }
        // Check if Results matches the number of operations in transaction
        Assert.assertEquals(transactionBuilder
                .getOperations().size(), operationResults.size());
        // Store the returned Mirror row UUID to be used in the TearDown cleanup transaction
        OpenVswitchSchemaSuiteIT.setTestMirrorUuid
                (operationResults.get(insertMirrorOperationIndex).getUuid());
        logger.info("Insert, Update & Mutate operation results for QOS " + operationResults);
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
