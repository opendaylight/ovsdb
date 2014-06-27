/*
 * Copyright (c) 2014 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal
 */
package org.opendaylight.ovsdb.integrationtest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

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

import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.OvsdbConnection;
import org.opendaylight.ovsdb.lib.OvsdbConnectionListener;

public abstract class OvsdbIntegrationTestBase {
    protected final static String IDENTIFIER = "TEST";
    protected final static String SERVER_IPADDRESS = "ovsdbserver.ipaddress";
    protected final static String SERVER_PORT = "ovsdbserver.port";
    protected final static String CONNECTION_TYPE = "ovsdbserver.connection";
    protected final static String CONNECTION_TYPE_ACTIVE = "active";
    protected final static String CONNECTION_TYPE_PASSIVE = "passive";

    protected final static String DEFAULT_SERVER_PORT = "6640";

    /**
     * Represents the Open Vswitch Schema
     */
    public final static String OPEN_VSWITCH_SCHEMA = "Open_vSwitch";
    public final static String HARDWARE_VTEP = "hardware_vtep";

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
            connection.registerForPassiveConnection(this);
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
}
