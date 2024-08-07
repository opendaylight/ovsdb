/*
 * Copyright © 2015, 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.support.membermodification.MemberMatcher.method;
import static org.powermock.api.support.membermodification.MemberModifier.suppress;
import static org.powermock.reflect.Whitebox.setInternalState;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.Futures;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.eos.binding.api.Entity;
import org.opendaylight.mdsal.eos.binding.api.EntityOwnershipService;
import org.opendaylight.mdsal.eos.common.api.EntityOwnershipStateChange;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.OvsdbConnection;
import org.opendaylight.ovsdb.lib.OvsdbConnectionInfo;
import org.opendaylight.ovsdb.lib.impl.OvsdbConnectionService;
import org.opendaylight.ovsdb.lib.operations.DefaultOperations;
import org.opendaylight.ovsdb.lib.operations.Operations;
import org.opendaylight.ovsdb.southbound.reconciliation.ReconciliationManager;
import org.opendaylight.ovsdb.southbound.transactions.md.TransactionCommand;
import org.opendaylight.ovsdb.southbound.transactions.md.TransactionInvoker;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
    SouthboundMapper.class, OvsdbConnectionManager.class, OvsdbConnectionService.class, SouthboundUtil.class
})
public class OvsdbConnectionManagerTest {

    @Mock private OvsdbConnectionManager ovsdbConnManager;
    @Mock private DataBroker db;
    @Mock private TransactionInvoker txInvoker;
    @Mock private EntityOwnershipService entityOwnershipService;
    @Mock private OvsdbConnection ovsdbConnection;
    @Mock private OvsdbClient externalClient;
    @Mock private ReconciliationManager reconciliationManager;
    private Map<ConnectionInfo, OvsdbConnectionInstance> clients;
    private Map<ConnectionInfo, InstanceIdentifier<Node>> instanceIdentifiers;
    private Map<Entity, OvsdbConnectionInstance> entityConnectionMap;

    private final InstanceIdentifier<Node> iid = InstanceIdentifier.create(NetworkTopology.class)
        .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
        .child(Node.class, new NodeKey(new NodeId("testNode")));

    @Before
    public void setUp() throws Exception {
        ovsdbConnManager = PowerMockito.mock(OvsdbConnectionManager.class, Mockito.CALLS_REAL_METHODS);
        setInternalState(ovsdbConnManager, "db", db);
        setInternalState(ovsdbConnManager, "txInvoker", txInvoker);
        setInternalState(ovsdbConnManager, "entityOwnershipService", entityOwnershipService);
        setInternalState(ovsdbConnManager, "reconciliationManager", reconciliationManager);
        setInternalState(ovsdbConnManager, "ovsdbConnection", ovsdbConnection);
        setInternalState(ovsdbConnManager, "alreadyProcessedClients", new ConcurrentHashMap<>());
        setInternalState(ovsdbConnManager, "ops", new DefaultOperations());
        entityConnectionMap = new ConcurrentHashMap<>();

        OvsdbConnectionInfo info = mock(OvsdbConnectionInfo.class);
        doReturn(InetAddress.getByAddress(new byte[] { 1, 2, 3, 4})).when(info).getRemoteAddress();
        doReturn(8080).when(info).getRemotePort();
        doReturn(InetAddress.getByAddress(new byte[] { 5, 6, 7, 8})).when(info).getLocalAddress();
        doReturn(8080).when(info).getLocalPort();

        externalClient = mock(OvsdbClient.class, Mockito.RETURNS_DEEP_STUBS);
        doReturn(info).when(externalClient).getConnectionInfo();
        doReturn(Futures.immediateFuture(List.of("Open_vSwitch"))).when(externalClient).getDatabases();

        PowerMockito.mockStatic(SouthboundUtil.class);
        when(SouthboundUtil.connectionInfoToString(any(ConnectionInfo.class))).thenReturn("192.18.120.31:8080");
    }

    @Test
    public void testConnected() throws Exception {
        OvsdbConnectionInstance client = mock(OvsdbConnectionInstance.class);
        suppress(method(OvsdbConnectionManager.class, "connectedButCallBacksNotRegistered",
                OvsdbClient.class));
        when(ovsdbConnManager.connectedButCallBacksNotRegistered(any(OvsdbClient.class))).thenReturn(client);
        doNothing().when(client).registerCallbacks(any());

        //TODO: Write unit tests for EntityOwnershipService
        when(client.getInstanceIdentifier()).thenReturn(InstanceIdentifier.create(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
            .child(Node.class, new NodeKey(new NodeId("testNode"))));
        setInternalState(ovsdbConnManager, "entityConnectionMap", entityConnectionMap);
        suppress(method(OvsdbConnectionManager.class, "getEntityFromConnectionInstance",
                OvsdbConnectionInstance.class));

        //TODO: Write unit tests for entity ownership service related code.
        suppress(method(OvsdbConnectionManager.class, "registerEntityForOwnership",
                OvsdbConnectionInstance.class));

        ReadTransaction tx = mock(ReadTransaction.class);
        when(db.newReadOnlyTransaction()).thenReturn(tx);
        when(tx.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class)))
                .thenReturn(mock(FluentFuture.class));
        when(client.getInstanceIdentifier()).thenReturn(InstanceIdentifier.create(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
            .child(Node.class, new NodeKey(new NodeId("testNode"))));

        ovsdbConnManager.connected(externalClient);
    }

    @Test
    public void testConnectedButCallBacksNotRegistered() throws Exception {
        ConnectionInfo key = mock(ConnectionInfo.class);

        PowerMockito.mockStatic(SouthboundMapper.class);
        when(SouthboundMapper.createConnectionInfo(any(OvsdbClient.class))).thenReturn(key);

        suppress(method(OvsdbConnectionManager.class, "getInstanceIdentifier", ConnectionInfo.class));
        when(ovsdbConnManager.getInstanceIdentifier(key)).thenReturn(InstanceIdentifier.create(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
            .child(Node.class, new NodeKey(new NodeId("testNode"))));

        suppress(method(OvsdbConnectionManager.class, "getConnectionInstance", ConnectionInfo.class));
        when(ovsdbConnManager.getConnectionInstance(key)).thenReturn(null);

        OvsdbConnectionInstance client = mock(OvsdbConnectionInstance.class);
        suppress(method(OvsdbConnectionManager.class, "putConnectionInstance", ConnectionInfo.class,
                OvsdbConnectionInstance.class));
        doNothing().when(client).createTransactInvokers();
        PowerMockito.whenNew(OvsdbConnectionInstance.class).withArguments(any(ConnectionInfo.class),
            any(OvsdbClient.class), any(Operations.class), any(TransactionInvoker.class), any(InstanceIdentifier.class))
                .thenReturn(client);

        assertEquals("Error, did not receive correct OvsdbConnectionInstance object", client,
                ovsdbConnManager.connectedButCallBacksNotRegistered(externalClient));
    }

    @Test
    public void testDisconnected() throws Exception {
        ConnectionInfo key = mock(ConnectionInfo.class);
        PowerMockito.mockStatic(SouthboundMapper.class);
        when(SouthboundMapper.createConnectionInfo(any(OvsdbClient.class))).thenReturn(key);

        OvsdbConnectionInstance ovsdbConnectionInstance = mock(OvsdbConnectionInstance.class);
        clients = new ConcurrentHashMap<>();
        clients.put(key, ovsdbConnectionInstance);
        setInternalState(ovsdbConnManager, "clients", clients);

        suppress(method(OvsdbConnectionManager.class, "getConnectionInstance", ConnectionInfo.class));
        when(ovsdbConnManager.getConnectionInstance(any(ConnectionInfo.class))).thenReturn(ovsdbConnectionInstance);
        doNothing().when(txInvoker).invoke(any(TransactionCommand.class));

        when(SouthboundMapper.suppressLocalIpPort(any(ConnectionInfo.class))).thenReturn(key);

        // TODO: Write unit tests for EntityOwnershipService
        suppress(method(OvsdbConnectionManager.class, "unregisterEntityForOwnership",
                OvsdbConnectionInstance.class));
        instanceIdentifiers = new ConcurrentHashMap<>();
        setInternalState(ovsdbConnManager, "instanceIdentifiers", instanceIdentifiers);
        setInternalState(ovsdbConnManager, "nodeIdVsConnectionInstance", new ConcurrentHashMap<>());

        suppress(method(OvsdbConnectionManager.class, "reconcileConnection",
                InstanceIdentifier.class, OvsdbNodeAugmentation.class));
        ReadTransaction tx = mock(ReadTransaction.class);
        when(db.newReadOnlyTransaction()).thenReturn(tx);
        when(tx.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class)))
                .thenReturn(mock(FluentFuture.class));
        when(tx.exists(any(LogicalDatastoreType.class), any(InstanceIdentifier.class)))
            .thenReturn(mock(FluentFuture.class));
        when(ovsdbConnectionInstance.getInstanceIdentifier()).thenReturn(
            InstanceIdentifier.create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class, new NodeKey(new NodeId("testNode"))));
        ovsdbConnManager.disconnected(externalClient);
        Map<ConnectionInfo, OvsdbConnectionInstance> testClients = Whitebox.getInternalState(ovsdbConnManager,
                "clients");
        assertEquals("Error, size of the hashmap is incorrect", 0, testClients.size());
    }

    @Test
    public void testDisconnect() throws Exception {
        OvsdbNodeAugmentation ovsdbNode = mock(OvsdbNodeAugmentation.class);
        ConnectionInfo connectionInfo = mock(ConnectionInfo.class);
        when(ovsdbNode.getConnectionInfo()).thenReturn(connectionInfo);
        suppress(method(OvsdbConnectionManager.class, "getConnectionInstance", ConnectionInfo.class));
        OvsdbConnectionInstance ovsdbConnectionInstance = mock(OvsdbConnectionInstance.class);
        when(ovsdbConnManager.getConnectionInstance(any(ConnectionInfo.class))).thenReturn(ovsdbConnectionInstance);
        when(ovsdbConnectionInstance.getInstanceIdentifier()).thenReturn(
            InstanceIdentifier.create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class, new NodeKey(new NodeId("testNode"))));

        suppress(method(OvsdbConnectionManager.class, "removeInstanceIdentifier", ConnectionInfo.class));

        // TODO: Write unit tests for entity ownership service related code.
        suppress(method(OvsdbConnectionManager.class, "unregisterEntityForOwnership",
                OvsdbConnectionInstance.class));
        ovsdbConnManager.disconnect(ovsdbNode);
        verify(ovsdbConnectionInstance).disconnect();
    }

    @Test
    @Ignore
    public void testInit() {
        mock(ConnectionInfo.class);
        suppress(method(OvsdbConnectionManager.class, "getConnectionInstance", ConnectionInfo.class));
        OvsdbConnectionInstance ovsdbConnectionInstance = mock(OvsdbConnectionInstance.class);
        when(ovsdbConnManager.getConnectionInstance(any(ConnectionInfo.class))).thenReturn(ovsdbConnectionInstance);

        // client not null
        // ovsdbConnectionManager.init(key);
        verify(ovsdbConnectionInstance).registerCallbacks(any());
    }

    @Test
    public void testClose() throws Exception {
        ConnectionInfo key1 = mock(ConnectionInfo.class);
        ConnectionInfo key2 = mock(ConnectionInfo.class);
        OvsdbConnectionInstance ovsdbConnectionInstance1 = mock(OvsdbConnectionInstance.class);
        OvsdbConnectionInstance ovsdbConnectionInstance2 = mock(OvsdbConnectionInstance.class);
        clients = new ConcurrentHashMap<>();
        clients.put(key1, ovsdbConnectionInstance1);
        clients.put(key2, ovsdbConnectionInstance2);
        setInternalState(ovsdbConnManager, "clients", clients);
        ovsdbConnManager.close();
        verify(ovsdbConnectionInstance1).disconnect();
        verify(ovsdbConnectionInstance2).disconnect();
    }

    @Test
    public void testPutAndGetConnectionInstance() throws Exception {
        ConnectionInfo key = mock(ConnectionInfo.class);
        ConnectionInfo connectionInfo = mock(ConnectionInfo.class);
        PowerMockito.mockStatic(SouthboundMapper.class);
        when(SouthboundMapper.suppressLocalIpPort(key)).thenReturn(connectionInfo);

        clients = new ConcurrentHashMap<>();
        setInternalState(ovsdbConnManager, "clients", clients);

        // Test putConnectionInstance()
        OvsdbConnectionInstance instance = mock(OvsdbConnectionInstance.class);
        Whitebox.invokeMethod(ovsdbConnManager, "putConnectionInstance", key, instance);
        Map<ConnectionInfo, OvsdbConnectionInstance> testClients = Whitebox.getInternalState(ovsdbConnManager,
                "clients");
        assertEquals("Error, size of the hashmap is incorrect", 1, testClients.size());

        // Test getConnectionInstance(ConnectionInfo key)
        assertEquals("Error, returned incorrect OvsdbConnectionInstance object", instance,
                ovsdbConnManager.getConnectionInstance(key));
    }

    @Test
    public void testPutandGetInstanceIdentifier() throws Exception {
        ConnectionInfo key = mock(ConnectionInfo.class);
        ConnectionInfo connectionInfo = mock(ConnectionInfo.class);
        PowerMockito.mockStatic(SouthboundMapper.class);
        when(SouthboundMapper.suppressLocalIpPort(key)).thenReturn(connectionInfo);

        instanceIdentifiers = new ConcurrentHashMap<>();
        setInternalState(ovsdbConnManager, "instanceIdentifiers", instanceIdentifiers);

        //Test putInstanceIdentifier()
        Whitebox.invokeMethod(ovsdbConnManager, "putInstanceIdentifier", key, iid);
        Map<ConnectionInfo, OvsdbConnectionInstance> testIids = Whitebox.getInternalState(ovsdbConnManager,
                "instanceIdentifiers");
        assertEquals("Error, size of the hashmap is incorrect", 1, testIids.size());

        //Test getInstanceIdentifier()
        assertEquals("Error returning correct InstanceIdentifier object", iid,
                ovsdbConnManager.getInstanceIdentifier(key));

        //Test removeInstanceIdentifier()
        Whitebox.invokeMethod(ovsdbConnManager, "removeInstanceIdentifier", key);
        Map<ConnectionInfo, OvsdbConnectionInstance> testRemoveIids = Whitebox.getInternalState(ovsdbConnManager,
                "instanceIdentifiers");
        assertEquals("Error, size of the hashmap is incorrect", 0, testRemoveIids.size());
    }

    @Test
    public void testGetClient() {
        OvsdbConnectionInstance ovsdbClient = mock(OvsdbConnectionInstance.class);
        OvsdbClient client = mock(OvsdbClient.class);
        when(ovsdbClient.getOvsdbClient()).thenReturn(client);

        //Test getClient(ConnectionInfo connectionInfo)
        ConnectionInfo key = mock(ConnectionInfo.class);
        suppress(method(OvsdbConnectionManager.class, "getConnectionInstance", ConnectionInfo.class));
        when(ovsdbConnManager.getConnectionInstance(key)).thenReturn(ovsdbClient);
        assertEquals("Error getting correct OvsdbClient object", ovsdbClient.getOvsdbClient(),
                ovsdbConnManager.getClient(key));

        //Test getClient(OvsdbBridgeAttributes mn)
        OvsdbBridgeAttributes mn = mock(OvsdbBridgeAttributes.class);
        suppress(method(OvsdbConnectionManager.class, "getConnectionInstance",
                OvsdbBridgeAttributes.class));
        when(ovsdbConnManager.getConnectionInstance(mn)).thenReturn(ovsdbClient);
        assertEquals("Error getting correct OvsdbClient object", ovsdbClient.getOvsdbClient(),
                ovsdbConnManager.getClient(mn));

        //Test getClient(Node node)
        Node node = mock(Node.class);
        suppress(method(OvsdbConnectionManager.class, "getConnectionInstance", Node.class));
        when(ovsdbConnManager.getConnectionInstance(node)).thenReturn(ovsdbClient);
        assertEquals("Error getting correct OvsdbClient object", ovsdbClient.getOvsdbClient(),
                ovsdbConnManager.getClient(node));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testConnect() throws Exception {
        OvsdbNodeAugmentation ovsdbNode = mock(OvsdbNodeAugmentation.class);
        ConnectionInfo connectionInfo = mock(ConnectionInfo.class);
        when(ovsdbNode.getConnectionInfo()).thenReturn(connectionInfo);
        IpAddress ipAddr = mock(IpAddress.class);
        when(connectionInfo.getRemoteIp()).thenReturn(ipAddr);

        PowerMockito.mockStatic(SouthboundMapper.class);
        InetAddress ip = InetAddress.getByAddress(new byte[] { 1, 2, 3, 4 });
        when(SouthboundMapper.createInetAddress(any(IpAddress.class))).thenReturn(ip);

        PowerMockito.mockStatic(OvsdbConnectionService.class);
//        when(OvsdbConnectionService.getService()).thenReturn(ovsdbConnection);
        PortNumber port = mock(PortNumber.class);
        when(connectionInfo.getRemotePort()).thenReturn(port);
        when(port.getValue()).thenReturn(Uint16.valueOf(8080));
        OvsdbClient client = mock(OvsdbClient.class);
        when(ovsdbConnection.connect(any(InetAddress.class), anyInt())).thenReturn(client);

        //client not null case
        suppress(method(OvsdbConnectionManager.class, "putInstanceIdentifier", ConnectionInfo.class,
                InstanceIdentifier.class));
        suppress(method(OvsdbConnectionManager.class, "connectedButCallBacksNotRegistered",
                OvsdbClient.class));

        doNothing().when(ovsdbConnManager).putInstanceIdentifier(any(ConnectionInfo.class),
            any(InstanceIdentifier.class));

        OvsdbConnectionInstance ovsdbConnectionInstance = mock(OvsdbConnectionInstance.class);
        when(ovsdbConnManager.connectedButCallBacksNotRegistered(any(OvsdbClient.class)))
                .thenReturn(ovsdbConnectionInstance);

        when(ovsdbConnectionInstance.getInstanceIdentifier()).thenReturn(
            InstanceIdentifier.create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class, new NodeKey(new NodeId("testNode"))));
        setInternalState(ovsdbConnManager, "entityConnectionMap", entityConnectionMap);
        suppress(method(OvsdbConnectionManager.class, "getEntityFromConnectionInstance",
                OvsdbConnectionInstance.class));
        //TODO: Write unit tests for entity ownership service related code.
        suppress(method(OvsdbConnectionManager.class, "registerEntityForOwnership",
                OvsdbConnectionInstance.class));
        assertEquals("ERROR", client, ovsdbConnManager.connect(InstanceIdentifier.create(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
            .child(Node.class, new NodeKey(new NodeId("testNode"))), ovsdbNode));
    }

    @Test
    public void testHandleOwnershipChanged() throws Exception {
        Entity entity = new Entity("entityType", "entityName");
        ConnectionInfo key = mock(ConnectionInfo.class);

        OvsdbConnectionInstance ovsdbConnInstance = new OvsdbConnectionInstance(key, externalClient,
            new DefaultOperations(), txInvoker, iid);
        entityConnectionMap.put(entity, ovsdbConnInstance);

        setInternalState(ovsdbConnManager, "entityConnectionMap", entityConnectionMap);
        doNothing().when(ovsdbConnManager).putConnectionInstance(any(ConnectionInfo.class),
            any(OvsdbConnectionInstance.class));
        ovsdbConnManager.handleOwnershipChanged(entity, EntityOwnershipStateChange.from(true, false, false));
        verify(ovsdbConnManager).putConnectionInstance(key, ovsdbConnInstance);
    }
}
