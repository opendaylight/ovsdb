/*
 *  Copyright (C) 2014 Red Hat, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 *  Authors : Sam Hague, Madhu Venugopal, Matt Oswalt
 */
package org.opendaylight.ovsdb.integrationtest.schema.openvswitch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.inject.Inject;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.OperationResult;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.OpenVSwitch;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
public class BridgeIT extends OpenVSwitchBaseTest {
    private static final Logger LOG = LoggerFactory.getLogger(BridgeIT.class);
    private UUID testBridgeUuid = null;

    @Inject
    private BundleContext bc;

    @Before
    public void setUp () throws ExecutionException, InterruptedException, IOException {
        areWeReady(bc);
        assertTrue(OPEN_VSWITCH_SCHEMA + " is required.", checkSchema(OPEN_VSWITCH_SCHEMA));
        assertTrue("Failed to monitor tables", monitorTables());
    }

    @Test
    public void testBridge () throws ExecutionException, InterruptedException {
        testBridgeUuid = bridgeInsert();

        // Verify that the local cache was updated with the remote changes
        Bridge bridge = getClient().createTypedRowWrapper(Bridge.class);
        Row bridgeRow = getTableCache().get(bridge.getSchema().getName()).get(testBridgeUuid);
        Bridge monitoredBridge = getClient().getTypedRowWrapper(Bridge.class, bridgeRow);
        assertEquals(TEST_BRIDGE_NAME, monitoredBridge.getNameColumn().getData());

        bridgeDelete(testBridgeUuid);
    }
}
