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
import java.net.UnknownHostException;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.OvsdbConnection;
import org.opendaylight.ovsdb.lib.OvsdbConnectionListener;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilities for OVSDB integration tests.
 */
public final class LibraryIntegrationTestUtils {
    private static final Logger LOG = LoggerFactory.getLogger(LibraryIntegrationTestUtils.class);
    public static final String SERVER_IPADDRESS = "ovsdbserver.ipaddress";
    public static final String SERVER_PORT = "ovsdbserver.port";
    public static final String CONNECTION_TYPE = "ovsdbserver.connection";
    public static final String OPEN_VSWITCH = "Open_vSwitch";
    public static final String HARDWARE_VTEP = "hardware_vtep";
    private static final String CONNECTION_TYPE_ACTIVE = "active";
    private static final String CONNECTION_TYPE_PASSIVE = "passive";
    private static final String DEFAULT_SERVER_PORT = "6640";

    /**
     * Prevent instantiation of a utility class.
     */
    private LibraryIntegrationTestUtils() {
        // Nothing to do
    }

    public static OvsdbClient getTestConnection(Object provider) throws IOException,
            InterruptedException, ExecutionException, TimeoutException {
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
            } catch (UnknownHostException e) {
                LOG.warn("Unable to resolve {}", addressStr, e);
                return null;
            }

            Integer port;
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                LOG.warn("Invalid port number: {}", portStr, e);
                return null;
            }

            OvsdbConnection connection =
                    (OvsdbConnection) ServiceHelper.getGlobalInstance(OvsdbConnection.class, provider);
            return connection.connect(address, port);
        } else if (connectionType.equalsIgnoreCase(CONNECTION_TYPE_PASSIVE)) {
            ExecutorService executor = Executors.newFixedThreadPool(1);
            Future<OvsdbClient> passiveConnection = executor.submit(new PassiveListener());
            return passiveConnection.get(60, TimeUnit.SECONDS);
        }
        throw new IllegalArgumentException("Connection parameter (" + CONNECTION_TYPE
                + ") must be either active or passive");
    }

    private static String usage() {
        return "Integration Test needs a valid connection configuration as follows :\n"
                + "active connection : mvn -Pintegrationtest -Dovsdbserver.ipaddress=x.x.x.x "
                + " -Dovsdbserver.port=yyyy verify\n"
                + "passive connection : mvn -Pintegrationtest -Dovsdbserver.connection=passive verify\n";
    }

    private static final class PassiveListener implements Callable<OvsdbClient>, OvsdbConnectionListener {
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
        public void connected(OvsdbClient newClient) {
            this.client = newClient;
        }

        @Override
        public void disconnected(OvsdbClient newClient) {
            if (!Objects.equals(this.client.getConnectionInfo(), newClient.getConnectionInfo())) {
                throw new IllegalStateException("disconnected unexpected client");
            }
            this.client = null;
        }
    }
}
