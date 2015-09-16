package org.opendaylight.ovsdb.southbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.OvsdbConnection;
import org.opendaylight.ovsdb.lib.impl.OvsdbConnectionService;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.southbound.transactions.md.TransactionCommand;
import org.opendaylight.ovsdb.southbound.transactions.md.TransactionInvoker;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberMatcher;
import org.powermock.api.support.membermodification.MemberModifier;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

@PrepareForTest({SouthboundMapper.class, OvsdbConnectionManager.class, OvsdbConnectionService.class, InstanceIdentifier.class, SouthboundUtil.class, Optional.class})
@RunWith(PowerMockRunner.class)
public class OvsdbConnectionManagerTest {

    @Mock private OvsdbConnectionManager ovsdbConnectionManager;
    @Mock private DataBroker db;
    @Mock private TransactionInvoker txInvoker;
    private Map<ConnectionInfo,OvsdbConnectionInstance> clients;
    private Map<ConnectionInfo,InstanceIdentifier<Node>> instanceIdentifiers;
    @Mock private InstanceIdentifier<Node> iid;

    @Before
    public void setUp() throws Exception {
        ovsdbConnectionManager = PowerMockito.mock(OvsdbConnectionManager.class, Mockito.CALLS_REAL_METHODS);
        MemberModifier.field(OvsdbConnectionManager.class, "db").set(ovsdbConnectionManager, db);
        MemberModifier.field(OvsdbConnectionManager.class, "txInvoker").set(ovsdbConnectionManager, txInvoker);
    }
    @Test
    public void testConnected() {
        OvsdbConnectionInstance client = mock(OvsdbConnectionInstance.class);
        OvsdbClient externalClient = mock(OvsdbClient.class);
        MemberModifier.suppress(MemberMatcher.method(OvsdbConnectionManager.class, "connectedButCallBacksNotRegistered", OvsdbClient.class));
        when(ovsdbConnectionManager.connectedButCallBacksNotRegistered(any(OvsdbClient.class))).thenReturn(client);
        doNothing().when(client).registerCallbacks();
        ovsdbConnectionManager.connected(externalClient);
        verify(client).registerCallbacks();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testConnectedButCallBacksNotRegistered() throws Exception {
        OvsdbClient externalClient = mock(OvsdbClient.class, Mockito.RETURNS_DEEP_STUBS);
        OvsdbConnectionInstance client = mock(OvsdbConnectionInstance.class);

        InetAddress ip = mock(InetAddress.class);

        when(externalClient.getConnectionInfo().getRemoteAddress()).thenReturn(ip);
        when(externalClient.getConnectionInfo().getRemotePort()).thenReturn(new Integer(8080));

        ConnectionInfo key = mock(ConnectionInfo.class);
        PowerMockito.mockStatic(SouthboundMapper.class);
        when(SouthboundMapper.createConnectionInfo(any(OvsdbClient.class))).thenReturn(key);

        MemberModifier.suppress(MemberMatcher.method(OvsdbConnectionManager.class, "getInstanceIdentifier", ConnectionInfo.class));
        InstanceIdentifier<Node> iid = mock(InstanceIdentifier.class);
        when(ovsdbConnectionManager.getInstanceIdentifier(key)).thenReturn(iid);

        MemberModifier.suppress(MemberMatcher.method(OvsdbConnectionManager.class, "putConnectionInstance", ConnectionInfo.class, OvsdbConnectionInstance.class));
        doNothing().when(client).createTransactInvokers();
        PowerMockito.whenNew(OvsdbConnectionInstance.class).
        withArguments(any(ConnectionInfo.class), any(OvsdbClient.class),any(TransactionInvoker.class), any(InstanceIdentifier.class)).
        thenReturn(client);

        assertEquals("Error, did not receive correct OvsdbConnectionInstance object", client, ovsdbConnectionManager.connectedButCallBacksNotRegistered(externalClient));
    }

    @Test
    public void testDisconnected() throws Exception {
        OvsdbClient client = mock(OvsdbClient.class, Mockito.RETURNS_DEEP_STUBS);
        OvsdbConnectionInstance ovsdbConnectionInstance = mock(OvsdbConnectionInstance.class);
        InetAddress ip = mock(InetAddress.class);
        when(client.getConnectionInfo().getRemoteAddress()).thenReturn(ip);
        when(client.getConnectionInfo().getRemotePort()).thenReturn(new Integer(8080));

        ConnectionInfo key = mock(ConnectionInfo.class);
        PowerMockito.mockStatic(SouthboundMapper.class);
        when(SouthboundMapper.createConnectionInfo(any(OvsdbClient.class))).thenReturn(key);

        MemberModifier.suppress(MemberMatcher.method(OvsdbConnectionManager.class, "getConnectionInstance", ConnectionInfo.class));
        when(ovsdbConnectionManager.getConnectionInstance(any(ConnectionInfo.class))).thenReturn(ovsdbConnectionInstance);
        doNothing().when(txInvoker).invoke(any(TransactionCommand.class));

        clients = new ConcurrentHashMap<ConnectionInfo,OvsdbConnectionInstance>();
        clients.put(key, ovsdbConnectionInstance);
        MemberModifier.field(OvsdbConnectionManager.class, "clients").set(ovsdbConnectionManager, clients);
        ovsdbConnectionManager.disconnected(client);
        Map<ConnectionInfo,OvsdbConnectionInstance> testClients = Whitebox.getInternalState(ovsdbConnectionManager, "clients");
        assertEquals("Error, size of the hashmap is incorrect", 0, testClients.size());
    }

    @Test
    public void testDisconnect() throws Exception {
        OvsdbNodeAugmentation ovsdbNode = mock(OvsdbNodeAugmentation.class);
        ConnectionInfo connectionInfo = mock(ConnectionInfo.class);
        when(ovsdbNode.getConnectionInfo()).thenReturn(connectionInfo);
        MemberModifier.suppress(MemberMatcher.method(OvsdbConnectionManager.class, "getConnectionInstance", ConnectionInfo.class));
        OvsdbConnectionInstance ovsdbConnectionInstance = mock(OvsdbConnectionInstance.class);
        when(ovsdbConnectionManager.getConnectionInstance(any(ConnectionInfo.class))).thenReturn(ovsdbConnectionInstance);
        ovsdbConnectionManager.disconnect(ovsdbNode);
        verify((OvsdbClient)ovsdbConnectionInstance).disconnect();
    }

    @Test
    public void testInit() {
        ConnectionInfo key = mock(ConnectionInfo.class);
        MemberModifier.suppress(MemberMatcher.method(OvsdbConnectionManager.class, "getConnectionInstance", ConnectionInfo.class));
        OvsdbConnectionInstance ovsdbConnectionInstance = mock(OvsdbConnectionInstance.class);
        when(ovsdbConnectionManager.getConnectionInstance(any(ConnectionInfo.class))).thenReturn(ovsdbConnectionInstance);

        //client not null
        ovsdbConnectionManager.init(key);
        verify(ovsdbConnectionInstance).registerCallbacks();
    }

    @Test
    public void testClose() throws Exception {
        ConnectionInfo key1 = mock(ConnectionInfo.class);
        ConnectionInfo key2 = mock(ConnectionInfo.class);
        OvsdbConnectionInstance ovsdbConnectionInstance1 = mock(OvsdbConnectionInstance.class);
        OvsdbConnectionInstance ovsdbConnectionInstance2 = mock(OvsdbConnectionInstance.class);
        clients = new ConcurrentHashMap<ConnectionInfo,OvsdbConnectionInstance>();
        clients.put(key1, ovsdbConnectionInstance1);
        clients.put(key2, ovsdbConnectionInstance2);
        MemberModifier.field(OvsdbConnectionManager.class, "clients").set(ovsdbConnectionManager, clients);
        ovsdbConnectionManager.close();
        verify(ovsdbConnectionInstance1).disconnect();
        verify(ovsdbConnectionInstance2).disconnect();
    }

    @Test
    public void testPutAndGetConnectionInstance() throws Exception {
        ConnectionInfo key = mock(ConnectionInfo.class);
        ConnectionInfo connectionInfo = mock(ConnectionInfo.class);
        OvsdbConnectionInstance instance = mock(OvsdbConnectionInstance.class);
        PowerMockito.mockStatic(SouthboundMapper.class);
        when(SouthboundMapper.suppressLocalIpPort(key)).thenReturn(connectionInfo);

        clients = new ConcurrentHashMap<ConnectionInfo,OvsdbConnectionInstance>();
        MemberModifier.field(OvsdbConnectionManager.class, "clients").set(ovsdbConnectionManager, clients);

        //Test putConnectionInstance()
        Whitebox.invokeMethod(ovsdbConnectionManager, "putConnectionInstance", key, instance);
        Map<ConnectionInfo,OvsdbConnectionInstance> testClients = Whitebox.getInternalState(ovsdbConnectionManager, "clients");
        assertEquals("Error, size of the hashmap is incorrect", 1, testClients.size());

        //Test getConnectionInstance(ConnectionInfo key)
        assertEquals("Error, returned incorrect OvsdbConnectionInstance object", instance, ovsdbConnectionManager.getConnectionInstance(key));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPutandGetInstanceIdentifier() throws Exception {
        ConnectionInfo key = mock(ConnectionInfo.class);
        ConnectionInfo connectionInfo = mock(ConnectionInfo.class);
        InstanceIdentifier<Node> iid = mock(InstanceIdentifier.class);
        PowerMockito.mockStatic(SouthboundMapper.class);
        when(SouthboundMapper.suppressLocalIpPort(key)).thenReturn(connectionInfo);

        instanceIdentifiers = new ConcurrentHashMap<ConnectionInfo,InstanceIdentifier<Node>>();
        MemberModifier.field(OvsdbConnectionManager.class, "instanceIdentifiers").set(ovsdbConnectionManager, instanceIdentifiers);

        //Test putInstanceIdentifier()
        Whitebox.invokeMethod(ovsdbConnectionManager, "putInstanceIdentifier", key, iid);
        Map<ConnectionInfo,OvsdbConnectionInstance> testIids = Whitebox.getInternalState(ovsdbConnectionManager, "instanceIdentifiers");
        assertEquals("Error, size of the hashmap is incorrect", 1, testIids.size());

        //Test getInstanceIdentifier()
        assertEquals("Error returning correct InstanceIdentifier object",iid , ovsdbConnectionManager.getInstanceIdentifier(key));
    }

    @Test
    public void testGetClient() {
        OvsdbClient ovsdbClient = mock(OvsdbConnectionInstance.class);

        //Test getClient(ConnectionInfo connectionInfo)
        ConnectionInfo key = mock(ConnectionInfo.class);
        MemberModifier.suppress(MemberMatcher.method(OvsdbConnectionManager.class, "getConnectionInstance", ConnectionInfo.class));
        when(ovsdbConnectionManager.getConnectionInstance(key)).thenReturn((OvsdbConnectionInstance)ovsdbClient);
        assertEquals("Error getting correct OvsdbClient object", ovsdbClient, ovsdbConnectionManager.getClient(key));

        //Test getClient(OvsdbBridgeAttributes mn)
        OvsdbBridgeAttributes mn = mock(OvsdbBridgeAttributes.class);
        MemberModifier.suppress(MemberMatcher.method(OvsdbConnectionManager.class, "getConnectionInstance", OvsdbBridgeAttributes.class));
        when(ovsdbConnectionManager.getConnectionInstance(mn)).thenReturn((OvsdbConnectionInstance)ovsdbClient);
        assertEquals("Error getting correct OvsdbClient object", ovsdbClient, ovsdbConnectionManager.getClient(mn));

        //Test getClient(Node node)
        Node node = mock(Node.class);
        MemberModifier.suppress(MemberMatcher.method(OvsdbConnectionManager.class, "getConnectionInstance", Node.class));
        when(ovsdbConnectionManager.getConnectionInstance(node)).thenReturn((OvsdbConnectionInstance)ovsdbClient);
        assertEquals("Error getting correct OvsdbClient object", ovsdbClient, ovsdbConnectionManager.getClient(node));
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
        InetAddress ip = mock(InetAddress.class);
        when(SouthboundMapper.createInetAddress(any(IpAddress.class))).thenReturn(ip);

        OvsdbClient client = mock(OvsdbClient.class);
        OvsdbConnection ovsdbConnection = mock(OvsdbConnection.class);
        PowerMockito.mockStatic(OvsdbConnectionService.class);
        when(OvsdbConnectionService.getService()).thenReturn(ovsdbConnection);
        PortNumber port = mock(PortNumber.class);
        when(connectionInfo.getRemotePort()).thenReturn(port);
        when(port.getValue()).thenReturn(new Integer(8080));
        when(ovsdbConnection.connect(any(InetAddress.class), anyInt())).thenReturn(client);

        //client not null case
        MemberModifier.suppress(MemberMatcher.method(OvsdbConnectionManager.class, "putInstanceIdentifier", ConnectionInfo.class, InstanceIdentifier.class));
        MemberModifier.suppress(MemberMatcher.method(OvsdbConnectionManager.class, "connectedButCallBacksNotRegistered", OvsdbClient.class));

        PowerMockito.when(PowerMockito.mock(InstanceIdentifier.class).firstIdentifierOf(Node.class)).thenReturn(iid);
        PowerMockito.doNothing().when(ovsdbConnectionManager, "putInstanceIdentifier", any(ConnectionInfo.class), any(InstanceIdentifier.class));

        OvsdbConnectionInstance ovsdbConnectionInstance = mock(OvsdbConnectionInstance.class);
        when(ovsdbConnectionManager.connectedButCallBacksNotRegistered(any(OvsdbClient.class))).thenReturn(ovsdbConnectionInstance);

        assertEquals("ERROR", client, ovsdbConnectionManager.connect(PowerMockito.mock(InstanceIdentifier.class), ovsdbNode));
    }
}
