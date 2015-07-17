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
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.propagateSystemProperty;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import org.junit.After;
import org.opendaylight.ovsdb.integrationtest.ConfigurationBundles;
import org.opendaylight.ovsdb.integrationtest.OvsdbIntegrationTestBase;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.OvsdbConnectionInfo;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.plugin.api.Connection;
import org.opendaylight.ovsdb.plugin.api.OvsdbConfigurationService;
import org.opendaylight.ovsdb.plugin.api.OvsdbConnectionService;
import org.opendaylight.ovsdb.plugin.api.OvsdbInventoryListener;
import org.opendaylight.ovsdb.plugin.api.OvsdbInventoryService;
import org.opendaylight.ovsdb.plugin.api.Status;
import org.opendaylight.ovsdb.plugin.api.StatusWithUuid;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.OpenVSwitch;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.util.PathUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import javax.inject.Inject;

@RunWith(PaxExam.class)
public class OvsdbPluginIT extends OvsdbIntegrationTestBase {
    private Logger log = LoggerFactory.getLogger(OvsdbPluginIT.class);
    @Inject
    private BundleContext bc;
    private OvsdbConfigurationService ovsdbConfigurationService = null;

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

            ConfigurationBundles.mdsalBundles(),
            ConfigurationBundles.controllerBundles(),
            ConfigurationBundles.ovsdbLibraryBundles(),
            ConfigurationBundles.ovsdbDefaultSchemaBundles(),
            ConfigurationBundles.ovsdbPluginBundles()
        );
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
        assertEquals(node, connectionService.getNode("OVS|" + identifier));
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

        /*
         * Repeat all of the above tests without the parent_uuid
         */

        this.endToEndApiTest(connection, null);
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

        NodeId nodeId = new NodeId("OVS|10.10.10.10:65342");
        NodeKey nodeKey = new NodeKey(nodeId);
        node = new NodeBuilder()
                .setId(nodeId)
                .setKey(nodeKey)
                .build();
        InetAddress address = InetAddress.getByName("10.10.10.10");
        int port = 65342;

        // Trigger event
        ovsdbInventoryService.notifyNodeAdded(node, address, port);

        Mockito.verify(listenerA, Mockito.times(1)).nodeAdded(node, address, port);
        Mockito.verify(listenerB, Mockito.times(1)).nodeAdded(node, address, port);

        dm.remove(componentA);
        dm.remove(componentB);

    }

    @Test
    public void testSetOFControllers() throws Exception {
        Thread.sleep(5000);
        OvsdbConnectionService
                connectionService = (OvsdbConnectionService)ServiceHelper.getGlobalInstance(OvsdbConnectionService.class, this);

        // 1. Check for the ovsdb Connection as seen by the Plugin layer
        assertNotNull(connectionService.getNodes());
        assertTrue(connectionService.getNodes().size() > 0);
        Node node = connectionService.getNodes().get(0);
        Connection connection = connectionService.getConnection(node);
        assertNotNull(connection);

        // 2. Create a bridge with a valid parent_uuid & Assert to make sure the return status is success.
        final StatusWithUuid status = insertBridge(connection, getOpenVSwitchTableUUID(connection));
        assertTrue(status.isSuccess());

        // Thread.sleep(3000);  // wait for _real_ controller to be added to bridge... or not (see below **)

        // 3. Test against bug 960: Add same controller multiple times and make sure we do not end up with duplicates.
        ovsdbConfigurationService.setOFController(node, status.getUuid().toString());
        ovsdbConfigurationService.setOFController(node, status.getUuid().toString());
        ovsdbConfigurationService.setOFController(node, status.getUuid().toString());
        ovsdbConfigurationService.setOFController(node, status.getUuid().toString());

        Row bridgeRow = ovsdbConfigurationService.getRow(node,
                                                  ovsdbConfigurationService.getTableName(node, Bridge.class),
                                                  status.getUuid().toString());
        assertNotNull(bridgeRow);
        Bridge bridge = ovsdbConfigurationService.getTypedRow(node, Bridge.class, bridgeRow);
        assertTrue(bridge.getUuid().equals(status.getUuid()));

        final int currControllersSize = bridge.getControllerColumn().getData().size();

        log.debug("Bridge has " + bridge.getControllerColumn().getData().size() + " controllers");

        // ** Note: we assert against 2 or less -- instead of 1 -- to account for the _real_ controller's connection
        assertTrue( "Too few controllers added to bridge object. Is this bug 960?", currControllersSize >= 1 );
        assertTrue( "Too many controllers added to bridge object. Is this bug 960?", currControllersSize <= 2 );

        // Removal of bridge created in this test is done via tearDown(). It is done that way, so cleanup is ran
        // even if test fails.
    }

    @After
    public void tearDown() throws InterruptedException {
        Thread.sleep(5000);
        OvsdbConnectionService
                connectionService = (OvsdbConnectionService)ServiceHelper.getGlobalInstance(OvsdbConnectionService.class, this);

        if (connectionService.getNodes() == null) {
            return;  // no nodes: noop
        }

        int bridgesRemoved = 0;
        List<Node> nodes = connectionService.getNodes();
        for (Node node : nodes) {
            Map<String, Row> bridgeRows =
                    ovsdbConfigurationService.getRows(node, ovsdbConfigurationService.getTableName(node, Bridge.class));
            if (bridgeRows == null) {
                continue;
            }
            for (Row bridgeRow : bridgeRows.values()) {
                Bridge bridge = ovsdbConfigurationService.getTypedRow(node, Bridge.class, bridgeRow);
                log.trace("Test clean up removing Bridge " + bridge.getUuid());
                Status delStatus = ovsdbConfigurationService.deleteRow(node,
                                                                bridge.getSchema().getName(),
                                                                bridge.getUuid().toString());
                assertTrue(delStatus.isSuccess());
                bridgesRemoved++;
            }
        }

        if (bridgesRemoved > 0) {
            log.debug("Test clean up removed " + bridgesRemoved + " bridges");
            Thread.sleep(2000); // TODO : Remove this Sleep once the Select operation is resolved.
        }
    }

    public void endToEndApiTest(Connection connection, String parentUuid) throws Exception {
        // 1. Print Cache and Assert to make sure the bridge is not created yet.
        printCache();

        // 2. Create a bridge with a valid parent_uuid & Assert to make sure the return status is success.
        StatusWithUuid status = insertBridge(connection, parentUuid);
        assertTrue(status.isSuccess());

        Thread.sleep(2000); // TODO : Remove this Sleep once the Select operation is resolved.

        // 3. Assert to make sure the bridge is created with a valid Uuid.
        printCache();
        Bridge bridge = connection.getClient().getTypedRowWrapper(Bridge.class, null);
        Row bridgeRow = ovsdbConfigurationService.getRow(node, bridge.getSchema().getName(), status.getUuid().toString());
        assertNotNull(bridgeRow);
        bridge = connection.getClient().getTypedRowWrapper(Bridge.class, bridgeRow);
        assertEquals(bridge.getUuid(), status.getUuid());

        // 4. Delete the bridge & Assert to make sure the return status is success.
        Status delStatus = ovsdbConfigurationService.deleteRow(node, bridge.getSchema().getName(), status.getUuid().toString());
        assertTrue(delStatus.isSuccess());
        Thread.sleep(2000); // TODO : Remove this Sleep once the Select operation is resolved.

        // 5. Assert to make sure the bridge is deleted
        bridgeRow = ovsdbConfigurationService.getRow(node, bridge.getSchema().getName(), status.getUuid().toString());
        assertNull(bridgeRow);
    }

    public StatusWithUuid insertBridge(Connection connection, String parentUuid) throws Exception {
        Bridge bridge = connection.getClient().createTypedRowWrapper(Bridge.class);
        bridge.setName("br_test1");
        bridge.setStatus(ImmutableMap.of("key", "value"));
        bridge.setFloodVlans(Sets.newHashSet(34L));
        return ovsdbConfigurationService.insertRow(node, bridge.getSchema().getName(), parentUuid, bridge.getRow());
    }

    public String getOpenVSwitchTableUUID(Connection connection) throws Exception {
        OpenVSwitch openVSwitch = connection.getClient().getTypedRowWrapper(OpenVSwitch.class, null);
        ConcurrentMap<String, Row> row = ovsdbConfigurationService.getRows(node, openVSwitch.getSchema().getName());
        if (row == null || row.size() == 0) {
            return null;
        }
        return (String)row.keySet().toArray()[0];
    }

    public void printCache() throws Exception {
        List<String> tables = ovsdbConfigurationService.getTables(node);
        System.out.println("Tables = "+tables);
        assertNotNull(tables);
        for (String table : tables) {
            System.out.println("Table "+table);
            ConcurrentMap<String,Row> row = ovsdbConfigurationService.getRows(node, table);
            System.out.println(row);
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
