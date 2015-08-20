/*
 * Copyright (c) 2014 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Sam Hague
 */
package org.opendaylight.ovsdb.integrationtest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.ovsdb.plugin.api.ConnectionConstants;
import org.opendaylight.ovsdb.plugin.api.OvsdbConnectionService;

import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class OvsdbIntegrationTestBase {
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbIntegrationTestBase.class);
    protected final static String IDENTIFIER = "TEST";
    protected final static String SERVER_IPADDRESS = "ovsdbserver.ipaddress";
    protected final static String SERVER_PORT = "ovsdbserver.port";
    protected final static String CONNECTION_TYPE = "ovsdbserver.connection";
    protected final static String CONNECTION_TYPE_ACTIVE = "active";
    protected final static String CONNECTION_TYPE_PASSIVE = "passive";
    protected final static int CONNECTION_INIT_TIMEOUT = 10000;
    protected final static String DEFAULT_SERVER_PORT = "6640";

    private static boolean bundlesReady = false;

    public Properties loadProperties() {
        return System.getProperties();
    }

    public Node getPluginTestConnection() throws IOException, InterruptedException, ExecutionException, TimeoutException {
        Properties props = loadProperties();
        String addressStr = props.getProperty(SERVER_IPADDRESS);
        String portStr = props.getProperty(SERVER_PORT, DEFAULT_SERVER_PORT);
        String connectionType = props.getProperty(CONNECTION_TYPE, "active");
        Node node = null;

        OvsdbConnectionService
                connection = (OvsdbConnectionService)ServiceHelper.getGlobalInstance(OvsdbConnectionService.class, this);
        // If the connection type is active, controller connects to the ovsdb-server
        if (connectionType.equalsIgnoreCase(CONNECTION_TYPE_ACTIVE)) {
            if (addressStr == null) {
                fail(usage());
            }

            Map<ConnectionConstants, String> params = new HashMap<>();
            params.put(ConnectionConstants.ADDRESS, addressStr);
            params.put(ConnectionConstants.PORT, portStr);
            node = connection.connect(IDENTIFIER, params);
        }  else if (connectionType.equalsIgnoreCase(CONNECTION_TYPE_PASSIVE)) {
            // Wait for CONNECTION_INIT_TIMEOUT for the Passive connection to be initiated by the ovsdb-server.
            Thread.sleep(CONNECTION_INIT_TIMEOUT);
            List<Node> nodes = connection.getNodes();
            assertNotNull(nodes);
            assertTrue(nodes.size() > 0);
            node = nodes.get(0);
        }

        if (node != null) {
            LOG.info("getPluginTestConnection: Successfully connected to {}", node);
        } else {
            fail("Connection parameter (" + CONNECTION_TYPE + ") must be active or passive");
        }
        return node;
    }

    protected String usage() {
        return "Integration Test needs a valid connection configuration as follows :\n" +
               "active connection : mvn -Pintegrationtest -Dovsdbserver.ipaddress=x.x.x.x -Dovsdbserver.port=yyyy verify\n"+
               "passive connection : mvn -Pintegrationtest -Dovsdbserver.connection=passive verify\n";
    }

    public String stateToString(int state) {
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

    public void areWeReady(BundleContext bc) throws InterruptedException {
        if (bundlesReady) {
            LOG.info("Bundles already loaded");
            return;
        }
        assertNotNull(bc);
        boolean debugit = false;
        Bundle b[] = bc.getBundles();
        for (Bundle element : b) {
            int state = element.getState();
            if (state != Bundle.ACTIVE && state != Bundle.RESOLVED) {
                LOG.info("Bundle:" + element.getSymbolicName() + " state:"
                        + stateToString(state));
                debugit = true;
            }
        }
        if (debugit) {
            LOG.debug("Do some debugging because some bundle is unresolved");
            Thread.sleep(600000);
        }

        // Assert if true, if false we are good to go!
        assertFalse("There is a problem with loading the bundles.", debugit);
        bundlesReady = true;
        LOG.info("Bundles loaded");
    }

    /*
     * Method adds a log as each test method starts and finishes. This is useful when
     * the test suite is used because the suites only print a final summary.
     */
    @Rule
    public TestRule watcher = new TestWatcher() {
        @Override
        protected void starting(Description description) {
            LOG.info("TestWatcher: Starting test: {}",
                     description.getDisplayName());
        }

        @Override
        protected void finished(Description description) {
            LOG.info("TestWatcher: Finished test: {}", description.getDisplayName());
        }
    };
}
