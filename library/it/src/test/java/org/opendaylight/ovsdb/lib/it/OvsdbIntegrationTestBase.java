/*
 *
 *  * Copyright © 2015 Red Hat, Inc. and others. All rights reserved.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 *
 */
package org.opendaylight.ovsdb.lib.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.when;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.opendaylight.controller.mdsal.it.base.AbstractMdsalTestBase;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.OvsdbConnection;
import org.opendaylight.ovsdb.lib.OvsdbConnectionListener;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.ops4j.pax.exam.Option;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO This is duplicated from integrationtest, remove one of the copies
public abstract class OvsdbIntegrationTestBase extends AbstractMdsalTestBase {
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
    public final static String OPEN_VSWITCH_SCHEMA = "Open_vSwitch";
    public final static String HARDWARE_VTEP = "hardware_vtep";

    @Override
    public Option[] config() {
        Option[] parentOptions = super.config();
        Option[] propertiesOptions = getPropertiesOptions();
        Option[] options = new Option[parentOptions.length + propertiesOptions.length];
        System.arraycopy(parentOptions, 0, options, 0, parentOptions.length);
        System.arraycopy(propertiesOptions, 0, options, parentOptions.length, propertiesOptions.length);
        return options;
    }

    public Option[] getPropertiesOptions() {
        Properties props = System.getProperties();
        String addressStr = props.getProperty(SERVER_IPADDRESS);
        String portStr = props.getProperty(SERVER_PORT, DEFAULT_SERVER_PORT);
        String connectionType = props.getProperty(CONNECTION_TYPE, CONNECTION_TYPE_ACTIVE);

        LOG.info("getPropertiesOptions: Using the following properties: mode= {}, ip:port= {}:{}",
                connectionType, addressStr, portStr);

        return new Option[] {
                when(addressStr != null).useOptions(
                        editConfigurationFilePut("etc/custom.properties",
                                "ovsdbserver.ipaddress", addressStr)),
                editConfigurationFilePut("etc/custom.properties",
                        "ovsdbserver.port", portStr),
                editConfigurationFilePut("etc/custom.properties",
                        "ovsdbserver.connection", connectionType),
        };
    }

    public Properties loadProperties() {
        Properties props = new Properties(System.getProperties());
        return props;
    }

    public OvsdbClient getTestConnection() throws IOException, InterruptedException, ExecutionException, TimeoutException {
        Properties props = loadProperties();
        String addressStr = props.getProperty(SERVER_IPADDRESS);
        String portStr = props.getProperty(SERVER_PORT, DEFAULT_SERVER_PORT);
        String connectionType = props.getProperty(CONNECTION_TYPE, "active");

        // If the connection type is active, controller connects to the ovsdb-server
        if (connectionType.equalsIgnoreCase(CONNECTION_TYPE_ACTIVE)) {
            if (addressStr == null) {
                fail(usage());
            }

            InetAddress address;
            try {
                address = InetAddress.getByName(addressStr);
            } catch (Exception e) {
                System.out.println("Unable to resolve " + addressStr);
                e.printStackTrace();
                return null;
            }

            Integer port;
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                System.out.println("Invalid port number : " + portStr);
                e.printStackTrace();
                return null;
            }

            OvsdbConnection connection = (OvsdbConnection)ServiceHelper.getGlobalInstance(OvsdbConnection.class, this);
            return connection.connect(address, port);
        } else if (connectionType.equalsIgnoreCase(CONNECTION_TYPE_PASSIVE)) {
            ExecutorService executor = Executors.newFixedThreadPool(1);
            Future<OvsdbClient> passiveConnection = executor.submit(new PassiveListener());
            return passiveConnection.get(60, TimeUnit.SECONDS);
        }
        fail("Connection parameter ("+CONNECTION_TYPE+") must be either active or passive");
        return null;
    }

    protected String usage() {
        return "Integration Test needs a valid connection configuration as follows :\n" +
               "active connection : mvn -Pintegrationtest -Dovsdbserver.ipaddress=x.x.x.x -Dovsdbserver.port=yyyy verify\n"+
               "passive connection : mvn -Pintegrationtest -Dovsdbserver.connection=passive verify\n";
    }

    public class PassiveListener implements Callable<OvsdbClient>, OvsdbConnectionListener {
        OvsdbClient client = null;
        @Override
        public OvsdbClient call() throws Exception {
            OvsdbConnection connection = (OvsdbConnection)ServiceHelper.getGlobalInstance(OvsdbConnection.class, this);
            connection.registerConnectionListener(this);
            while (client == null) {
                Thread.sleep(500);
            }
            return client;
        }

        @Override
        public void connected(OvsdbClient client) {
            this.client = client;
        }

        @Override
        public void disconnected(OvsdbClient client) {
            assertEquals(this.client.getConnectionInfo(), client.getConnectionInfo());
            this.client = null;
        }
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
