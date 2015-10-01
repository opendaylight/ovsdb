package org.opendaylight.ovsdb.southbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.Controller;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.InstanceIdentifierBuilder;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@PrepareForTest({SouthboundMapper.class, InstanceIdentifier.class, Inet4Address.class,  Inet6Address.class, SouthboundUtil.class, KeyedInstanceIdentifier.class})
@RunWith(PowerMockRunner.class)
public class SouthboundMapperTest {

    @Before
    public void setUp() {
        PowerMockito.mockStatic(SouthboundMapper.class, Mockito.CALLS_REAL_METHODS);
    }

    @SuppressWarnings({ "unchecked", "deprecation" })
    @Test //TO-DO StackOverflow Error
    public void testCreateNodeId() throws Exception {
        OvsdbConnectionInstance client = mock(OvsdbConnectionInstance.class);
        NodeKey key = mock(NodeKey.class);
        InstanceIdentifier<Node> iidNode = PowerMockito.mock(InstanceIdentifier.class);
        when(client.getInstanceIdentifier()).thenReturn(iidNode);
        PowerMockito.when(iidNode.firstKeyOf(Node.class, NodeKey.class)).thenReturn(key);
        NodeId nodeId = mock(NodeId.class);
        when(key.getNodeId()).thenReturn(nodeId);
        //assertEquals(nodeId, Whitebox.invokeMethod(SouthboundMapper.class, "createNodeId", client));
    }

    @Test
    public void testCreateIpAddress() throws Exception {
        InetAddress addressInet4 = PowerMockito.mock(Inet4Address.class);
        InetAddress addressInet6 = PowerMockito.mock(Inet6Address.class);
        IpAddress ip = mock(IpAddress.class);

        when(addressInet4.getHostAddress()).thenReturn("127.0.0.1");
        when(addressInet6.getHostAddress()).thenReturn("127.0.0.1");

        //test for createIpAddress(Inet4Address address)
        Ipv4Address ipv4Add = mock(Ipv4Address.class);
        PowerMockito.whenNew(Ipv4Address.class).withArguments(anyString()).thenReturn(ipv4Add);
        PowerMockito.whenNew(IpAddress.class).withAnyArguments().thenReturn(ip);

        //test for createIpAddress(Inet6Address address)
        Ipv6Address ipv6Add = mock(Ipv6Address.class);
        PowerMockito.whenNew(Ipv6Address.class).withArguments(anyString()).thenReturn(ipv6Add);

        assertEquals("Incorrect IP address received", ip, SouthboundMapper.createIpAddress(addressInet4));
        assertEquals("Incorrect IP address received", ip, SouthboundMapper.createIpAddress(addressInet6));
    }

    @SuppressWarnings("unchecked")
    @Test //TO-DO gives StackOverflow error
    public void testCreateInstanceIdentifier() throws Exception {
        NodeId nodeId = mock(NodeId.class);
        PowerMockito.mockStatic(InstanceIdentifier.class);
        InstanceIdentifier<NetworkTopology> iidNetTopo = PowerMockito.mock(InstanceIdentifier.class);
        PowerMockito.when(InstanceIdentifier.create(eq(NetworkTopology.class))).thenReturn(iidNetTopo);
        KeyedInstanceIdentifier<Topology, TopologyKey> keyedIidTopo = PowerMockito.mock(KeyedInstanceIdentifier.class);

        //mock new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID)
        PowerMockito.whenNew(TopologyKey.class).withAnyArguments().thenReturn(mock(TopologyKey.class));
        when(iidNetTopo.child(eq(Topology.class), any(TopologyKey.class))).thenReturn(keyedIidTopo); //child() is final method
        KeyedInstanceIdentifier<Node, NodeKey> keyedIidNode = mock(KeyedInstanceIdentifier.class);

        //mock new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID)
        PowerMockito.whenNew(NodeKey.class).withAnyArguments().thenReturn(mock(NodeKey.class));
        when(keyedIidTopo.child(eq(Node.class), any(NodeKey.class))).thenReturn(keyedIidNode);

        InstanceIdentifier<Node> nodePath = keyedIidNode;
        //assertEquals(nodePath, SouthboundMapper.createInstanceIdentifier(nodeId));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreateInstanceIdentifier1() throws Exception {
        OvsdbConnectionInstance client = mock(OvsdbConnectionInstance.class, Mockito.RETURNS_DEEP_STUBS);
        Bridge bridge = mock(Bridge.class);
        InstanceIdentifier<Node> iid = mock(InstanceIdentifier.class);

        //when bridge is not empty
        Column<GenericTableSchema, Map<String, String>> column = mock(Column.class);
        when(bridge.getExternalIdsColumn()).thenReturn(column);
        Map<String, String> map = new HashMap<String, String>();
        map.put(SouthboundConstants.IID_EXTERNAL_ID_KEY, "IID_EXTERNAL_ID_KEY");
        when(column.getData()).thenReturn(map);
        PowerMockito.mockStatic(SouthboundUtil.class);
        when((InstanceIdentifier<Node>) SouthboundUtil.deserializeInstanceIdentifier(anyString())).thenReturn(iid);
        assertEquals("Incorrect Instance Identifier received", iid, SouthboundMapper.createInstanceIdentifier(client, bridge));

        //when bridge is empty
        when(bridge.getExternalIdsColumn()).thenReturn(null);
        when(client.getNodeKey().getNodeId().getValue()).thenReturn("uri");
        when(bridge.getName()).thenReturn("bridgeName");
        PowerMockito.whenNew(Uri.class).withArguments(anyString()).thenReturn(mock(Uri.class));
        PowerMockito.whenNew(NodeId.class).withAnyArguments().thenReturn(mock(NodeId.class));
        PowerMockito.mockStatic(InstanceIdentifier.class);
        InstanceIdentifierBuilder<NetworkTopology> iidNetTopo = mock(InstanceIdentifierBuilder.class);
        InstanceIdentifierBuilder<Topology> iidTopo = mock(InstanceIdentifierBuilder.class);
        InstanceIdentifierBuilder<Node> iidNode = mock(InstanceIdentifierBuilder.class);
        PowerMockito.when(InstanceIdentifier.builder(NetworkTopology.class)).thenReturn(iidNetTopo);
        PowerMockito.whenNew(TopologyKey.class).withAnyArguments().thenReturn(mock(TopologyKey.class));
        when(iidNetTopo.child(eq(Topology.class), any(TopologyKey.class))).thenReturn(iidTopo);
        when(iidTopo.child(eq(Node.class), any(NodeKey.class))).thenReturn(iidNode);
        when(iidNode.build()).thenReturn(iid);
        assertEquals("Incorrect Instance Identifier received", iid, SouthboundMapper.createInstanceIdentifier(client, bridge));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreateInstanceIdentifier2() throws Exception {
        OvsdbConnectionInstance client = mock(OvsdbConnectionInstance.class, Mockito.RETURNS_DEEP_STUBS);
        Controller controller = mock(Controller.class);
        InstanceIdentifier<Node> iid = mock(InstanceIdentifier.class);

        //when controller is not empty
        Column<GenericTableSchema, Map<String, String>> column = mock(Column.class);
        when(controller.getExternalIdsColumn()).thenReturn(column);
        Map<String, String> map = new HashMap<String, String>();
        map.put(SouthboundConstants.IID_EXTERNAL_ID_KEY, "IID_EXTERNAL_ID_KEY");
        when(column.getData()).thenReturn(map);
        PowerMockito.mockStatic(SouthboundUtil.class);
        when((InstanceIdentifier<Node>) SouthboundUtil.deserializeInstanceIdentifier(anyString())).thenReturn(iid);
        assertEquals("Incorrect Instance Identifier received", iid, SouthboundMapper.createInstanceIdentifier(client, controller, "bridgeName"));

        //when controller is empty
        when(controller.getExternalIdsColumn()).thenReturn(null);
        when(client.getNodeKey().getNodeId().getValue()).thenReturn("uri");
        PowerMockito.whenNew(Uri.class).withArguments(anyString()).thenReturn(mock(Uri.class));
        PowerMockito.whenNew(NodeId.class).withAnyArguments().thenReturn(mock(NodeId.class));
        PowerMockito.mockStatic(InstanceIdentifier.class);
        InstanceIdentifierBuilder<NetworkTopology> iidNetTopo = mock(InstanceIdentifierBuilder.class);
        InstanceIdentifierBuilder<Topology> iidTopo = mock(InstanceIdentifierBuilder.class);
        InstanceIdentifierBuilder<Node> iidNode = mock(InstanceIdentifierBuilder.class);
        PowerMockito.when(InstanceIdentifier.builder(NetworkTopology.class)).thenReturn(iidNetTopo);
        PowerMockito.whenNew(TopologyKey.class).withAnyArguments().thenReturn(mock(TopologyKey.class));
        when(iidNetTopo.child(eq(Topology.class), any(TopologyKey.class))).thenReturn(iidTopo);
        when(iidTopo.child(eq(Node.class), any(NodeKey.class))).thenReturn(iidNode);
        when(iidNode.build()).thenReturn(iid);
        assertEquals("Incorrect Instance Identifier received", iid, SouthboundMapper.createInstanceIdentifier(client, controller, "bridgeName"));
    }

    @SuppressWarnings({ "unchecked", "deprecation" })
    @Test //TO-DO StackOverflow error
    public void testCreateManagedNodeId() {
        InstanceIdentifier<Node> iid = PowerMockito.mock(InstanceIdentifier.class);
        NodeKey nodeKey = mock(NodeKey.class);
        PowerMockito.when(iid.firstKeyOf(Node.class, NodeKey.class)).thenReturn(nodeKey);
        NodeId nodeId = mock(NodeId.class);
        when(nodeKey.getNodeId()).thenReturn(nodeId);
        //assertEquals("Incorrect nodeId recieved", nodeId, SouthboundMapper.createManagedNodeId(iid));
    }

    @Test
    public void testCreateInetAddress() throws Exception {
        IpAddress ip = mock(IpAddress.class, Mockito.RETURNS_DEEP_STUBS);
        when(ip.getIpv4Address()).thenReturn(mock(Ipv4Address.class));
        when(ip.getIpv4Address().getValue()).thenReturn("99.99.99.99");
        PowerMockito.mockStatic(InetAddress.class);
        InetAddress inetAddress = mock(InetAddress.class);
        when(InetAddress.getByName(anyString())).thenReturn(inetAddress);

        //Ipv4Address not null
        assertEquals("Incorrect InetAddress received", inetAddress, SouthboundMapper.createInetAddress(ip));

        //Ipv4Address null, Ipv6Address not null
        when(ip.getIpv4Address()).thenReturn(null);
        when(ip.getIpv6Address()).thenReturn(mock(Ipv6Address.class));
        when(ip.getIpv6Address().getValue()).thenReturn("0000:0000:0000:0000:0000:9999:FE1E:8329");
        assertEquals("Incorrect InetAddress received", inetAddress, SouthboundMapper.createInetAddress(ip));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreateDatapathId() throws Exception {
        Bridge bridge = mock(Bridge.class);
        when(bridge.getDatapathIdColumn()).thenReturn(null);
        assertNull(SouthboundMapper.createDatapathId(bridge));

        Column<GenericTableSchema, Set<String>> column = mock(Column.class);
        when(bridge.getDatapathIdColumn()).thenReturn(column);
        Set<String> set = new HashSet<String>();
        set.add("dpid");
        when(column.getData()).thenReturn(set);
        assertNotNull(column.getData());

        DatapathId dataPathId = mock(DatapathId.class);

        //test createDatapathId(Set<String> dpids) and createDatapathId(String dpid)
        PowerMockito.whenNew(DatapathId.class).withAnyArguments().thenReturn(dataPathId);
        assertEquals(dataPathId, SouthboundMapper.createDatapathId(bridge));
    }
}
