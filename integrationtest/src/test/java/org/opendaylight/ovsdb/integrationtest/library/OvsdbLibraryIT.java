/*
 * Copyright (c) 2014 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal
 */
package org.opendaylight.ovsdb.integrationtest.library;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.opendaylight.ovsdb.lib.operations.Operations.op;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.propagateSystemProperty;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.ovsdb.integrationtest.ConfigurationBundles;
import org.opendaylight.ovsdb.integrationtest.OvsdbIntegrationTestBase;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.OperationResult;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.OpenVSwitch;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.util.PathUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ListenableFuture;

@RunWith(PaxExam.class)
public class OvsdbLibraryIT extends OvsdbIntegrationTestBase {
    private Logger log = LoggerFactory.getLogger(OvsdbLibraryIT.class);
    @Inject
    private BundleContext bc;
    private OvsdbClient client = null;

    @Configuration
    public Option[] config() {
        return options(
            //
            systemProperty("logback.configurationFile").value(
                    "file:" + PathUtils.getBaseDir()
                    + "/src/test/resources/logback.xml"
            ),
            // To start OSGi console for inspection remotely
            systemProperty("osgi.console").value("2401"),

            propagateSystemProperty("ovsdbserver.ipaddress"),
            propagateSystemProperty("ovsdbserver.port"),

            ConfigurationBundles.controllerBundles(),
            ConfigurationBundles.ovsdbLibraryBundles(),
            ConfigurationBundles.ovsdbDefaultSchemaBundles(),
            junitBundles()
        );
    }

    private String stateToString(int state) {
        switch (state) {
        case Bundle.ACTIVE:
            return "ACTIVE";
        case Bundle.INSTALLED:
            return "INSTALLED";
        case Bundle.RESOLVED:
            return "RESOLVED";
        case Bundle.UNINSTALLED:
            return "UNINSTALLED";
        default:
            return "Not CONVERTED";
        }
    }

    @Before
    public void areWeReady() throws InterruptedException {
        assertNotNull(bc);
        boolean debugit = false;
        Bundle b[] = bc.getBundles();
        for (Bundle element : b) {
            int state = element.getState();
            if (state != Bundle.ACTIVE && state != Bundle.RESOLVED) {
                log.info("Bundle:" + element.getSymbolicName() + " state:"
                          + stateToString(state));
                debugit = true;
            }
        }
        if (debugit) {
            log.debug("Do some debugging because some bundle is unresolved");
        }

        // Assert if true, if false we are good to go!
        assertFalse(debugit);
        try {
            client = getTestConnection();
        } catch (Exception e) {
            fail("Exception : "+e.getMessage());
        }
    }

    public boolean isSchemaSupported(String schema) throws ExecutionException, InterruptedException {
        ListenableFuture<List<String>> databases = client.getDatabases();
        List<String> dbNames = databases.get();
        assertNotNull(dbNames);
        if (dbNames.contains(schema)) return true;
        return false;
    }

    static String testBridgeName = "br_test";
    static UUID testBridgeUuid = null;
    private void createTypedBridge() throws IOException, InterruptedException, ExecutionException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Bridge bridge = client.createTypedRowWrapper(Bridge.class);
        bridge.setName(testBridgeName);
        bridge.setStatus(ImmutableMap.of("key","value"));
        bridge.setFloodVlans(Sets.newHashSet(34));

        OpenVSwitch openVSwitch = client.createTypedRowWrapper(OpenVSwitch.class);
        openVSwitch.setBridges(Sets.newHashSet(new UUID(testBridgeName)));

        int insertOperationIndex = 0;

        TransactionBuilder transactionBuilder = client.transactBuilder()
                .add(op.insert(bridge.getSchema())
                        .withId(testBridgeName)
                        .value(bridge.getNameColumn()))
                .add(op.update(bridge.getSchema())
                        .set(bridge.getStatusColumn())
                        .set(bridge.getFloodVlansColumn())
                        .where(bridge.getNameColumn().getSchema().opEqual(bridge.getName()))
                        .and(bridge.getNameColumn().getSchema().opEqual(bridge.getName())).build())
                .add(op.mutate(openVSwitch.getSchema())
                        .addMutation(openVSwitch.getBridgesColumn().getSchema(), Mutator.INSERT,
                                     openVSwitch.getBridgesColumn().getData()));

        ListenableFuture<List<OperationResult>> results = transactionBuilder.execute();
        List<OperationResult> operationResults = results.get();
        assertFalse(operationResults.isEmpty());
        // Check if Results matches the number of operations in transaction
        assertEquals(transactionBuilder.getOperations().size(), operationResults.size());
        System.out.println("Insert & Update operation results = " + operationResults);
        for (OperationResult result : operationResults) {
            assertNull(result.getError());
        }
        testBridgeUuid = operationResults.get(insertOperationIndex).getUuid();
        assertNotNull(testBridgeUuid);
    }

    @Test
    public void tableTest() throws Exception {
        assertNotNull("Invalid Client. Check connection params", client);
        Thread.sleep(3000); // Wait for a few seconds to get the Schema exchange done
        if (isSchemaSupported(OPEN_VSWITCH_SCHEMA)) {
            DatabaseSchema dbSchema = client.getSchema(OPEN_VSWITCH_SCHEMA, true).get();
            assertNotNull(dbSchema);
            System.out.println(OPEN_VSWITCH_SCHEMA + " schema in "+ client.getConnectionInfo() +
                                                     " with Tables : " + dbSchema.getTables());

            // A simple Typed Test to make sure a Typed wrapper bundle can coexist in an OSGi environment
            createTypedBridge();
        }

        if (isSchemaSupported(HARDWARE_VTEP)) {
            DatabaseSchema dbSchema = client.getSchema(HARDWARE_VTEP, true).get();
            assertNotNull(dbSchema);
            System.out.println(HARDWARE_VTEP + " schema in "+ client.getConnectionInfo() +
                                               " with Tables : " + dbSchema.getTables());
        }
    }

    @After
    public void tearDown() throws InterruptedException, ExecutionException {
        Bridge bridge = client.getTypedRowWrapper(Bridge.class, null);
        OpenVSwitch openVSwitch = client.getTypedRowWrapper(OpenVSwitch.class, null);

        ListenableFuture<List<OperationResult>> results = client.transactBuilder()
                .add(op.delete(bridge.getSchema())
                        .where(bridge.getNameColumn().getSchema().opEqual(testBridgeName))
                        .build())
                .add(op.mutate(openVSwitch.getSchema())
                        .addMutation(openVSwitch.getBridgesColumn().getSchema(), Mutator.DELETE, Sets.newHashSet(testBridgeUuid)))
                .add(op.commit(true))
                .execute();

        List<OperationResult> operationResults = results.get();
        System.out.println("Delete operation results = " + operationResults);
    }
}
