/*
 * Copyright © 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.lib.it;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.OvsdbConnection;
import org.opendaylight.ovsdb.lib.OvsdbConnectionListener;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.osgi.framework.Bundle;

/**
 * Utilities for OVSDB integration tests.
 */
public final class LibraryIntegrationTestUtils {
    public static final String SERVER_IPADDRESS = "ovsdbserver.ipaddress";
    public static final String SERVER_PORT = "ovsdbserver.port";
    public static final String CONNECTION_TYPE = "ovsdbserver.connection";
    public final static String OPEN_VSWITCH_SCHEMA = "Open_vSwitch";
    public final static String HARDWARE_VTEP = "hardware_vtep";
    private static final String CONNECTION_TYPE_ACTIVE = "active";
    private static final String CONNECTION_TYPE_PASSIVE = "passive";
    private static final String DEFAULT_SERVER_PORT = "6640";

    /**
     * Prevent instantiation of a utility class.
     */
    private LibraryIntegrationTestUtils() {
        // Nothing to do
    }

    public static OvsdbClient getTestConnection(BindingAwareProvider provider) throws IOException, InterruptedException, ExecutionException, TimeoutException {
        Properties props = System.getProperties();
        String addressStr = props.getProperty(SERVER_IPADDRESS);
        String portStr = props.getProperty(SERVER_PORT, DEFAULT_SERVER_PORT);
        String connectionType = props.getProperty(CONNECTION_TYPE, "active");

        // If the connection type is active, controller connects to the ovsdb-server
        if (connectionType.equalsIgnoreCase(CONNECTION_TYPE_ACTIVE)) {
            if (addressStr == null) {
                throw new IllegalArgumentException(usage());
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

            OvsdbConnection connection = (OvsdbConnection) ServiceHelper.getGlobalInstance(OvsdbConnection.class, provider);
            return connection.connect(address, port);
        } else if (connectionType.equalsIgnoreCase(CONNECTION_TYPE_PASSIVE)) {
            ExecutorService executor = Executors.newFixedThreadPool(1);
            Future<OvsdbClient> passiveConnection = executor.submit(new PassiveListener());
            return passiveConnection.get(60, TimeUnit.SECONDS);
        }
        throw new IllegalArgumentException("Connection parameter (" + CONNECTION_TYPE + ") must be either active or passive");
    }

    public static String bundleStateToString(int state) {
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

    private static String usage() {
        return "Integration Test needs a valid connection configuration as follows :\n" +
                "active connection : mvn -Pintegrationtest -Dovsdbserver.ipaddress=x.x.x.x -Dovsdbserver.port=yyyy verify\n"+
                "passive connection : mvn -Pintegrationtest -Dovsdbserver.connection=passive verify\n";
    }

    private static class PassiveListener implements Callable<OvsdbClient>, OvsdbConnectionListener {
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
            if (!Objects.equals(this.client.getConnectionInfo(), client.getConnectionInfo())) {
                throw new IllegalStateException("disconnected unexpected client");
            }
            this.client = null;
        }
    }

}
