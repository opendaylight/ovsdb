/*
 * Copyright (c) 2014 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal
 */
package org.opendaylight.ovsdb.integrationtest.plugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.propagateSystemProperty;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import javax.inject.Inject;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.opendaylight.ovsdb.integrationtest.ConfigurationBundles;
import org.opendaylight.ovsdb.integrationtest.OvsdbIntegrationTestBase;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.OvsdbConnectionInfo;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.plugin.api.Connection;
import org.opendaylight.ovsdb.plugin.api.OvsdbConfigurationService;
import org.opendaylight.ovsdb.plugin.api.OvsdbConnectionService;
import org.opendaylight.ovsdb.plugin.api.OvsdbInventoryListener;
import org.opendaylight.ovsdb.plugin.api.OvsdbInventoryService;
import org.opendaylight.ovsdb.plugin.api.StatusWithUuid;
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

@RunWith(PaxExam.class)
public class OvsdbPluginV3IT extends OvsdbIntegrationTestBase {
    private Logger log = LoggerFactory.getLogger(OvsdbPluginV3IT.class);
    @Inject
    private BundleContext bc;
    private OvsdbConfigurationService ovsdbConfigurationService = null;
    private String databaseName = "Open_vSwitch";

    @Inject
    private OvsdbInventoryService ovsdbInventoryService;

    private Node node = null;
    private OvsdbClient client = null;

    // Configure the OSGi container
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
            ConfigurationBundles.ovsdbPluginBundles(),
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

        assertFalse(debugit);
        try {
            node = getPluginTestConnection();
        } catch (Exception e) {
            fail("Exception : "+e.getMessage());
        }
        this.ovsdbConfigurationService = (OvsdbConfigurationService)ServiceHelper.getGlobalInstance(OvsdbConfigurationService.class, this);
    }

    @Test
    public void apiTests() throws Exception {
        Thread.sleep(5000);
        OvsdbConnectionService
                connectionService = (OvsdbConnectionService)ServiceHelper.getGlobalInstance(OvsdbConnectionService.class, this);

        // Check for the ovsdb Connection as seen by the Plugin layer
        assertNotNull(connectionService.getNodes());
        assertTrue(connectionService.getNodes().size() > 0);
        Node node = connectionService.getNodes().get(0);
        Connection connection = connectionService.getConnection(node);
        OvsdbConnectionInfo connectionInfo = connection.getClient().getConnectionInfo();
        String identifier = IDENTIFIER;
        if (connectionInfo.getType().equals(OvsdbConnectionInfo.ConnectionType.PASSIVE)) {
            identifier = connectionInfo.getRemoteAddress().getHostAddress()+":"+connectionInfo.getRemotePort();
        }
        assertEquals(Node.fromString("OVS|" + identifier), connectionService.getNodes().get(0));
        System.out.println("Nodes = "+ connectionService.getNodes());
        /*
         * Test sequence :
         * 1. Print Cache and Assert to make sure the bridge is not created yet.
         * 2. Create a bridge with a valid parent_uuid & Assert to make sure the return status is success.
         * 3. Assert to make sure the bridge is created with a valid Uuid.
         * 4. Delete the bridge & Assert to make sure the return status is success.
         * 5. Assert to make sure the bridge is deleted
         */

        this.endToEndApiTest(connection, getOpenVSwitchTableUUID(connection));
    }

    @Test
    public void testInventoryListeners() throws UnknownHostException {
        DependencyManager dm = new DependencyManager(bc);

        OvsdbInventoryListener listenerA = Mockito.mock(FakeListener.class);
        OvsdbInventoryListener listenerB = Mockito.mock(FakeListener.class);

        Component componentA = dm.createComponent();
        componentA.setInterface(OvsdbInventoryListener.class.getName(), null);
        componentA.setImplementation(listenerA);
        dm.add(componentA);

        Component componentB = dm.createComponent();
        componentB.setInterface(OvsdbInventoryListener.class.getName(), null);
        componentB.setImplementation(listenerB);
        dm.add(componentB);

        Node newNode = Node.fromString("OVS:10.10.10.10:65342");
        InetAddress address = InetAddress.getByName("10.10.10.10");
        int port = 65342;

        // Trigger event
        ovsdbInventoryService.notifyNodeAdded(newNode, address, port);

        Mockito.verify(listenerA, Mockito.times(1)).nodeAdded(newNode, address, port);
        Mockito.verify(listenerB, Mockito.times(1)).nodeAdded(newNode, address, port);

        dm.remove(componentA);
        dm.remove(componentB);

    }

    public void endToEndApiTest(Connection connection, String parentUuid) throws Exception {
        // 1. Print Cache and Assert to make sure the bridge is not created yet.
        printCache();

        // 2. Create a bridge with a valid parent_uuid & Assert to make sure the return status is success.
        StatusWithUuid status = insertBridge(connection, parentUuid);
        assertTrue(status.isSuccess());

        Thread.sleep(2000);

        // 3. Assert to make sure the bridge is created with a valid Uuid.
        printCache();
        Bridge bridge = connection.getClient().getTypedRowWrapper(Bridge.class, null);
        Row bridgeRow = ovsdbConfigurationService.getRow(node, databaseName, bridge.getSchema().getName(), status.getUuid());
        assertNotNull(bridgeRow);
        bridge = connection.getClient().getTypedRowWrapper(Bridge.class, bridgeRow);
        System.out.println("Bridge UUID "+bridge.getUuid()+" Status Uuid "+status.getUuid());
        assertEquals(bridge.getUuid(), status.getUuid());

        bridge = connection.getClient().createTypedRowWrapper(Bridge.class);
        bridge.setDatapathType("netdev");
        try {
            ovsdbConfigurationService.updateRow(node, databaseName, bridge.getSchema().getName(), status.getUuid(), bridge.getRow(), false);
        } catch (Exception e) {
            fail("Failed to updated Bridge "+e.getMessage());
        }

        // 4. Delete the bridge & Assert to make sure the return status is success.
        try {
            ovsdbConfigurationService.deleteRow(node, databaseName, bridge.getSchema().getName(), null, new UUID(parentUuid) ,null, status.getUuid());
        } catch (Exception e) {
            fail(e.getMessage());
        }
        Thread.sleep(2000);

        // 5. Assert to make sure the bridge is deleted
        bridgeRow = ovsdbConfigurationService.getRow(node, databaseName, bridge.getSchema().getName(), status.getUuid());
        assertNull(bridgeRow);
    }

    public StatusWithUuid insertBridge(Connection connection, String parentUuid) throws Exception {
        Bridge bridge = connection.getClient().createTypedRowWrapper(Bridge.class);
        bridge.setName("br_test1");
        bridge.setStatus(ImmutableMap.of("key", "value"));
        bridge.setFloodVlans(Sets.newHashSet(34L));
        try {
        Row<GenericTableSchema> row = ovsdbConfigurationService.insertTree(node, databaseName, bridge.getSchema().getName(), new UUID(parentUuid), bridge.getRow());
        bridge = connection.getClient().getTypedRowWrapper(Bridge.class, row);
        return new StatusWithUuid(StatusCode.SUCCESS, bridge.getUuid());
        } catch (Exception e) {
            return new StatusWithUuid(StatusCode.INTERNALERROR);
        }
    }

    public String getOpenVSwitchTableUUID(Connection connection) throws Exception {
        OpenVSwitch openVSwitch = connection.getClient().getTypedRowWrapper(OpenVSwitch.class, null);
        ConcurrentMap<UUID, Row<GenericTableSchema>> rows = ovsdbConfigurationService.getRows(node, databaseName, openVSwitch.getSchema().getName());
        if (rows == null || rows.size() == 0) return null;
        return rows.keySet().toArray()[0].toString();
    }

    public void printCache() throws Exception {
        List<String> tables = ovsdbConfigurationService.getTables(node, databaseName);
        System.out.println("Tables = "+tables);
        assertNotNull(tables);
        for (String table : tables) {
            System.out.println("Table "+table);
            ConcurrentMap<UUID, Row<GenericTableSchema>> rows = ovsdbConfigurationService.getRows(node, databaseName, table);
            System.out.println(rows);
        }
    }

    public class FakeListener implements OvsdbInventoryListener {

        @Override
        public void nodeAdded(Node node, InetAddress address, int port) {

        }

        @Override
        public void nodeRemoved(Node node) {

        }

        @Override
        public void rowAdded(Node node, String tableName, String uuid, Row row) {

        }

        @Override
        public void rowUpdated(Node node, String tableName, String uuid, Row old, Row row) {

        }

        @Override
        public void rowRemoved(Node node, String tableName, String uuid, Row row, Object context) {

        }
    }

}
