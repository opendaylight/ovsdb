/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Matt Oswalt
 */

package org.opendaylight.ovsdb.schema.hardwarevtep;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import junit.framework.Assert;

import org.junit.Before;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.OvsdbConnection;
import org.opendaylight.ovsdb.lib.OvsdbConnectionListener;
import org.opendaylight.ovsdb.lib.impl.OvsdbConnectionService;
import org.opendaylight.ovsdb.lib.message.OvsdbRPC;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;

import com.google.common.util.concurrent.ListenableFuture;

public abstract class HardwareVtepSchemaTestBase implements OvsdbRPC.Callback{
    private final static String SERVER_IPADDRESS = "ovsdbserver.ipaddress";
    private final static String SERVER_PORT = "ovsdbserver.port";
    private final static String DEFAULT_SERVER_PORT = "6640";
    private final static String CONNECTION_TYPE = "ovsdbserver.connection";
    private final static String CONNECTION_TYPE_ACTIVE = "active";
    private final static String CONNECTION_TYPE_PASSIVE = "passive";
    private final static String TEST_MANAGER_TARGET = "ptcp:10.12.0.15:6640"; //TODO: for future use

    /**
     * Represents the Hardware VTEP Schema
     */
    public final static String HARDWARE_VTEP_SCHEMA = "hardware_vtep";
    protected OvsdbClient ovs = null;
    static Boolean SUPPORTS_HARDWARE_VTEP = false;

    DatabaseSchema dbSchema = null;

    public Properties loadProperties() {
        Properties props = new Properties(System.getProperties());
        return props;
    }

    @Before
    public  void setUp() throws IOException, ExecutionException, InterruptedException, TimeoutException {

        this.ovs = HardwareVtepSchemaSuiteIT.getOvsdbClient();

        if (this.ovs == null) {
            this.ovs = getTestConnection();
            HardwareVtepSchemaSuiteIT.setOvsdbClient(this.ovs);

            //retrieve list of databases from OVSDB server
            ListenableFuture<List<String>> databases = ovs.getDatabases();
            List<String> dbNames = databases.get();
            Assert.assertNotNull(dbNames);

            //verify that HW VTEP schema is in the list of supported databases
            boolean hasHardwareVTEPSchema = false;
            for (String dbName : dbNames) {
                if (dbName.equals(HARDWARE_VTEP_SCHEMA)) {
                    hasHardwareVTEPSchema = true;
                    break;
                }
            }
            Assert.assertTrue(HARDWARE_VTEP_SCHEMA + " schema is not supported by the switch", hasHardwareVTEPSchema);
            SUPPORTS_HARDWARE_VTEP = hasHardwareVTEPSchema;
        }
        //desired schema exists, retrieve contents of specified database
        dbSchema = this.ovs.getSchema(HARDWARE_VTEP_SCHEMA).get();

    }

    public boolean checkSchema() {
        return SUPPORTS_HARDWARE_VTEP;
    }

    public OvsdbClient getTestConnection() throws IOException, InterruptedException, ExecutionException, TimeoutException {
        Properties props = loadProperties();
        String addressStr = props.getProperty(SERVER_IPADDRESS);
        String portStr = props.getProperty(SERVER_PORT, DEFAULT_SERVER_PORT);
        String connectionType = props.getProperty(CONNECTION_TYPE, "active");

        // If the connection type is active, controller connects to the ovsdb-server
        if (connectionType.equalsIgnoreCase(CONNECTION_TYPE_ACTIVE)) {
            if (addressStr == null) {
                Assert.fail(usage());
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

            OvsdbConnection connection = OvsdbConnectionService.getService();
            return connection.connect(address, port);
        } else if (connectionType.equalsIgnoreCase(CONNECTION_TYPE_PASSIVE)) {
            ExecutorService executor = Executors.newFixedThreadPool(1);
            Future<OvsdbClient> passiveConnection = executor.submit(new PassiveListener());
            return passiveConnection.get(60, TimeUnit.SECONDS);
        }
        Assert.fail("Connection parameter ("+CONNECTION_TYPE+") must be either active or passive");
        return null;
    }

    public UUID getGlobalTableUuid(OvsdbClient ovs, Map<String, Map<UUID, Row>> tableCache) {
        Global glbl = ovs.getTypedRowWrapper(Global.class, null);
        Map<UUID, Row> glblTbl = tableCache.get(glbl.getSchema().getName());
        if (glblTbl != null) {
            if (glblTbl.keySet().size() >= 1) {
                return (UUID)glblTbl.keySet().toArray()[0];
            }
        }
        return null;
    }

    private String usage() {
        return "Integration Test needs a valid connection configuration as follows :\n" +
               "active connection : mvn -Pintegrationtest -Dovsdbserver.ipaddress=x.x.x.x -Dovsdbserver.port=yyyy verify\n"+
               "passive connection : mvn -Pintegrationtest -Dovsdbserver.connection=passive verify\n";
    }

    public class PassiveListener implements Callable<OvsdbClient>, OvsdbConnectionListener {
        OvsdbClient client = null;
        @Override
        public OvsdbClient call() throws Exception {
            OvsdbConnection connection = OvsdbConnectionService.getService();
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
            Assert.assertEquals(this.client.getConnectionInfo(), client.getConnectionInfo());
            this.client = null;
        }
    }
}
