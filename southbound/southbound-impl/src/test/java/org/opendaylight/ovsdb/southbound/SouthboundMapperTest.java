/*
 * Copyright (c) 2015 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.southbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.Controller;
import org.opendaylight.ovsdb.schema.openvswitch.Manager;
import org.opendaylight.ovsdb.schema.openvswitch.OpenVSwitch;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathTypeNetdev;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathTypeSystem;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeInternal;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeProtocolBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeProtocolOpenflow10;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ProtocolEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ProtocolEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagerEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagerEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.InstanceIdentifierBuilder;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@PrepareForTest({SouthboundMapper.class, InstanceIdentifier.class, Inet4Address.class,  Inet6Address.class, SouthboundUtil.class, SouthboundConstants.class, TyperUtils.class})
@RunWith(PowerMockRunner.class)
public class SouthboundMapperTest {


    @Before
    public void setUp() {
        PowerMockito.mockStatic(SouthboundMapper.class, Mockito.CALLS_REAL_METHODS);
    }

    @Test
    public void testCreateNodeId() throws Exception {
        PowerMockito.mockStatic(SouthboundMapper.class, Mockito.RETURNS_MOCKS);
        assertTrue("Returned value is not an NodeId", Whitebox.invokeMethod(SouthboundMapper.class, "createNodeId", null) instanceof NodeId);
    }

    @Test
    public void testCreateManagedNodeId() throws Exception {
        PowerMockito.mockStatic(SouthboundMapper.class, Mockito.RETURNS_MOCKS);
        assertTrue("Returned value is not an NodeId", SouthboundMapper.createManagedNodeId(null) instanceof NodeId);
    }

    @Test
    public void testCreateIpAddress() throws Exception {
        IpAddress ip = mock(IpAddress.class);

        //test for createIpAddress(Inet4Address address)
        InetAddress addressInet4 = PowerMockito.mock(Inet4Address.class);
        when(addressInet4.getHostAddress()).thenReturn("127.0.0.1");
        Ipv4Address ipv4Add = mock(Ipv4Address.class);
        PowerMockito.whenNew(Ipv4Address.class).withAnyArguments().thenReturn(ipv4Add);
        PowerMockito.whenNew(IpAddress.class).withAnyArguments().thenReturn(ip);
        assertEquals("Incorrect IP address received", ip, SouthboundMapper.createIpAddress(addressInet4));
    }

    @Test
    public void testCreateInstanceIdentifier() throws Exception {
        assertTrue(SouthboundMapper.createInstanceIdentifier(mock(NodeId.class)) instanceof InstanceIdentifier);
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
        Map<String, String> map = new HashMap<>();
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
        Map<String, String> map = new HashMap<>();
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
        Set<String> set = new HashSet<>();
        set.add("dpid");
        when(column.getData()).thenReturn(set);
        assertNotNull(column.getData());

        DatapathId dataPathId = mock(DatapathId.class);

        //test createDatapathId(Set<String> dpids) and createDatapathId(String dpid)
        PowerMockito.whenNew(DatapathId.class).withAnyArguments().thenReturn(dataPathId);
        assertEquals(dataPathId, SouthboundMapper.createDatapathId(bridge));
    }

    @Test
    public void testCreateDatapathType() throws Exception {
        OvsdbBridgeAugmentation mdsalbridge = mock(OvsdbBridgeAugmentation.class);
        PowerMockito.mockStatic(SouthboundConstants.class, Mockito.RETURNS_DEEP_STUBS);
        when(mdsalbridge.getDatapathType()).thenAnswer(new Answer<Class<? extends DatapathTypeBase>>() {
            public Class<? extends DatapathTypeBase> answer(
                    InvocationOnMock invocation) throws Throwable {
                return DatapathTypeNetdev.class;
            }
        });
        assertEquals("netdev", SouthboundMapper.createDatapathType(mdsalbridge));

        when(mdsalbridge.getDatapathType()).thenAnswer(new Answer<Class<? extends DatapathTypeBase>>() {
            public Class<? extends DatapathTypeBase> answer(
                    InvocationOnMock invocation) throws Throwable {
                return DatapathTypeSystem.class;
            }
        });
        assertEquals("system", SouthboundMapper.createDatapathType(mdsalbridge));
    }

    @Test
    public void testCreateDatapathType1() {
        assertEquals(DatapathTypeSystem.class, SouthboundMapper.createDatapathType(""));
        assertEquals(DatapathTypeSystem.class, SouthboundMapper.createDatapathType("system"));
        assertEquals(DatapathTypeNetdev.class, SouthboundMapper.createDatapathType("netdev"));
    }

    @Test
    public void testCreateOvsdbBridgeProtocols() {
        OvsdbBridgeAugmentation ovsdbBridgeNode = mock(OvsdbBridgeAugmentation.class);
        List<ProtocolEntry> protocolList = new ArrayList<>();
        ProtocolEntry protocolEntry = mock(ProtocolEntry.class);
        protocolList.add(protocolEntry);
        when(ovsdbBridgeNode.getProtocolEntry()).thenReturn(protocolList);
        when(protocolEntry.getProtocol()).thenAnswer(new Answer<Class<? extends OvsdbBridgeProtocolBase>>() {
            public Class<? extends OvsdbBridgeProtocolBase> answer(
                    InvocationOnMock invocation) throws Throwable {
                return OvsdbBridgeProtocolOpenflow10.class;
            }
        });
        Set<String> protocols = new HashSet<>();
        protocols.add("OpenFlow10");
        assertEquals(protocols, SouthboundMapper.createOvsdbBridgeProtocols(ovsdbBridgeNode));
    }

    @Test
    public void testCreateInterfaceType() {
        assertEquals(InterfaceTypeInternal.class, SouthboundMapper.createInterfaceType("internal"));
        assertEquals(InterfaceTypeVxlan.class, SouthboundMapper.createInterfaceType("vxlan"));
    }

    @Test
    public void testCreateOvsdbInterfaceType() {
        assertEquals("internal", SouthboundMapper.createOvsdbInterfaceType(InterfaceTypeInternal.class));
        assertEquals("vxlan", SouthboundMapper.createOvsdbInterfaceType(InterfaceTypeVxlan.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreateMdsalProtocols() throws Exception {
        Bridge bridge = mock(Bridge.class);
        Set<String> value = new HashSet<>();
        value.add("OpenFlow10");
        Column<GenericTableSchema, Set<String>> column = mock(Column.class);
        when(bridge.getProtocolsColumn()).thenReturn(column);
        when(column.getData()).thenReturn(value);

        List<ProtocolEntry> protocolList = new ArrayList<>();
        ProtocolEntry protoEntry = mock(ProtocolEntry.class);
        ProtocolEntryBuilder protocolEntryBuilder = mock(ProtocolEntryBuilder.class);
        PowerMockito.whenNew(ProtocolEntryBuilder.class).withNoArguments().thenReturn(protocolEntryBuilder);
        when(protocolEntryBuilder.setProtocol(any(Class.class))).thenReturn(protocolEntryBuilder);
        when(protocolEntryBuilder.build()).thenReturn(protoEntry);
        protocolList.add(protoEntry);
        assertEquals(protocolList, SouthboundMapper.createMdsalProtocols(bridge));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreateControllerEntries() throws Exception {
        Bridge bridge = mock(Bridge.class);
        Map<UUID, Controller> updatedControllerRows = new HashMap<>();
        Column<GenericTableSchema, Set<UUID>> column = mock(Column.class);
        when(bridge.getControllerColumn()).thenReturn(column);
        Set<UUID> controllerUUIDs = new HashSet<>();
        UUID uuid = mock(UUID.class);
        controllerUUIDs.add(uuid);
        Controller controller = mock(Controller.class);
        updatedControllerRows.put(uuid, controller);
        when(column.getData()).thenReturn(controllerUUIDs);
        List<ControllerEntry> controllerEntries = new ArrayList<>();
        ControllerEntry controllerEntry = mock(ControllerEntry.class);
        controllerEntries.add(controllerEntry);

        //Test addControllerEntries()
        Column<GenericTableSchema, String> value = mock(Column.class);
        when(controller.getTargetColumn()).thenReturn(value);
        when(value.getData()).thenReturn("targetString");
        when(controller.getUuid()).thenReturn(uuid);
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid testUuid = mock(Uuid.class);
        PowerMockito.whenNew(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid.class).withAnyArguments().thenReturn(testUuid);
        ControllerEntryBuilder controllerEntryBuilder = mock(ControllerEntryBuilder.class);
        PowerMockito.whenNew(ControllerEntryBuilder.class).withAnyArguments().thenReturn(controllerEntryBuilder);
        Uri uri = mock(Uri.class);
        PowerMockito.whenNew(Uri.class).withAnyArguments().thenReturn(uri);
        when(controllerEntryBuilder.setTarget(any(Uri.class))).thenReturn(controllerEntryBuilder);
        Column<GenericTableSchema, Boolean> colConnected = mock(Column.class);
        when(controller.getIsConnectedColumn()).thenReturn(colConnected );
        when(colConnected.getData()).thenReturn(true);
        when(controllerEntryBuilder.setIsConnected(any(Boolean.class))).thenReturn(controllerEntryBuilder);
        when(controllerEntryBuilder.setControllerUuid(any(Uuid.class))).thenReturn(controllerEntryBuilder);
        when(controllerEntryBuilder.build()).thenReturn(controllerEntry);

        assertEquals(controllerEntries, SouthboundMapper.createControllerEntries(bridge, updatedControllerRows));
    }

    @Test
    public void testCreateControllerEntries1() throws Exception {

    }

    @Test
    public void testCreateOvsdbController() throws Exception {
        OvsdbBridgeAugmentation omn = mock(OvsdbBridgeAugmentation.class);
        DatabaseSchema dbSchema = mock(DatabaseSchema.class);
        List<ControllerEntry> controllerEntries = new ArrayList<>();
        ControllerEntry controllerEntry = mock(ControllerEntry.class);
        controllerEntries.add(controllerEntry);
        when(omn.getControllerEntry()).thenReturn(controllerEntries);
        Map<UUID,Controller> controllerMap = new HashMap<>();
        PowerMockito.mockStatic(TyperUtils.class);
        Controller controller = mock(Controller.class);
        PowerMockito.when(TyperUtils.getTypedRowWrapper(any(DatabaseSchema.class), eq(Controller.class))).thenReturn(controller);
        Uri uri = mock(Uri.class);
        when(controllerEntry.getTarget()).thenReturn(uri);
        when(uri.getValue()).thenReturn("uri");
        UUID uuid = mock(UUID.class);
        PowerMockito.whenNew(UUID.class).withAnyArguments().thenReturn(uuid);

        controllerMap.put(uuid, controller);
        assertEquals(controllerMap, SouthboundMapper.createOvsdbController(omn, dbSchema));
    }

    @Test
    public void testCreateConnectionInfo() throws Exception {
        OvsdbClient client = mock(OvsdbClient.class, Mockito.RETURNS_DEEP_STUBS);
        ConnectionInfoBuilder connectionInfoBuilder = mock(ConnectionInfoBuilder.class);
        PowerMockito.whenNew(ConnectionInfoBuilder.class).withNoArguments().thenReturn(connectionInfoBuilder);

        when(client.getConnectionInfo().getRemoteAddress()).thenReturn(mock(InetAddress.class));
        when(client.getConnectionInfo().getRemotePort()).thenReturn(8080);
        when(client.getConnectionInfo().getLocalAddress()).thenReturn(mock(InetAddress.class));
        when(client.getConnectionInfo().getLocalPort()).thenReturn(8080);
        PortNumber portNum = mock(PortNumber.class);
        PowerMockito.whenNew(PortNumber.class).withAnyArguments().thenReturn(portNum);
        when(connectionInfoBuilder.setRemoteIp(any(IpAddress .class))).thenReturn(connectionInfoBuilder);
        when(connectionInfoBuilder.setRemotePort(any(PortNumber.class))).thenReturn(connectionInfoBuilder);
        when(connectionInfoBuilder.setLocalIp(any(IpAddress .class))).thenReturn(connectionInfoBuilder);
        when(connectionInfoBuilder.setLocalPort(any(PortNumber.class))).thenReturn(connectionInfoBuilder);
        ConnectionInfo connectionInfo = mock(ConnectionInfo.class);
        when(connectionInfoBuilder.build()).thenReturn(connectionInfo);
        assertEquals(connectionInfo, SouthboundMapper.createConnectionInfo(client));
    }

    @Test
    public void testSuppressLocalIpPort() throws Exception {
        ConnectionInfo connectionInfo = mock(ConnectionInfo.class);
        ConnectionInfoBuilder connectionInfoBuilder = mock(ConnectionInfoBuilder.class);
        PowerMockito.whenNew(ConnectionInfoBuilder.class).withNoArguments().thenReturn(connectionInfoBuilder);
        when(connectionInfo.getRemoteIp()).thenReturn(mock(IpAddress.class));
        when(connectionInfo.getRemotePort()).thenReturn(mock(PortNumber.class));
        when(connectionInfoBuilder.setRemoteIp(any(IpAddress .class))).thenReturn(connectionInfoBuilder);
        when(connectionInfoBuilder.setRemotePort(any(PortNumber.class))).thenReturn(connectionInfoBuilder);
        when(connectionInfoBuilder.build()).thenReturn(connectionInfo);
        assertEquals(connectionInfo, SouthboundMapper.suppressLocalIpPort(connectionInfo));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreateManagerEntries() throws Exception {
        OpenVSwitch ovsdbNode = mock(OpenVSwitch.class);
        Map<UUID, Manager> updatedManagerRows = new HashMap<>();
        Set<UUID> managerUUIDs = new HashSet<>();
        UUID managerUUID = mock(UUID.class);
        Manager manager = mock(Manager.class);
        managerUUIDs.add(managerUUID);
        updatedManagerRows.put(managerUUID, manager);
        Column<GenericTableSchema, Set<UUID>> column = mock(Column.class);
        when(ovsdbNode.getManagerOptionsColumn()).thenReturn(column);
        when(column.getData()).thenReturn(managerUUIDs);
        List<ManagerEntry> managerEntries = new ArrayList<>();
        ManagerEntry managerEntry = mock(ManagerEntry.class);
        managerEntries.add(managerEntry);

        //Test addManagerEntries(managerEntriesCreated, manager)
        Column<GenericTableSchema, String> value = mock(Column.class);
        when(manager.getTargetColumn()).thenReturn(value);
        when(value.getData()).thenReturn("dummy");

        Column<GenericTableSchema, Map<String, String>> statusColumn = mock(Column.class);
        when(manager.getStatusColumn()).thenReturn(statusColumn);
        Map<String, String> statusAttributeMap = new HashMap<>();
        when(statusColumn.getData()).thenReturn(statusAttributeMap);
        String numberOfConnectionValueStr = "999";

        //statusAttributeMap contains N_CONNECTIONS_STR key
        statusAttributeMap.put("n_connections", numberOfConnectionValueStr);

        Column<GenericTableSchema, Boolean> isConnectedColumn = mock(Column.class);
        when(manager.getIsConnectedColumn()).thenReturn(isConnectedColumn);
        when(isConnectedColumn.getData()).thenReturn(true);
        ManagerEntryBuilder managerEntryBuilder = mock(ManagerEntryBuilder.class);
        PowerMockito.whenNew(ManagerEntryBuilder.class).withNoArguments().thenReturn(managerEntryBuilder);
        PowerMockito.whenNew(Uri.class).withAnyArguments().thenReturn(mock(Uri.class));
        when(managerEntryBuilder.setTarget(any(Uri.class))).thenReturn(managerEntryBuilder);
        when(managerEntryBuilder.setNumberOfConnections(any(Long.class))).thenReturn(managerEntryBuilder);
        when(managerEntryBuilder.setConnected(true)).thenReturn(managerEntryBuilder);

        when(managerEntryBuilder.build()).thenReturn(managerEntry);

        assertEquals(managerEntries, SouthboundMapper.createManagerEntries(ovsdbNode, updatedManagerRows));

        //statusAttributeMap contains N_CONNECTIONS_STR key
        statusAttributeMap.remove("n_connections");
        assertEquals(managerEntries, SouthboundMapper.createManagerEntries(ovsdbNode, updatedManagerRows));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreateManagerEntries1() throws Exception {
        Node ovsdbNode = mock(Node.class);
        Map<Uri, Manager> updatedManagerRows = new HashMap<>();
        Uri uri = mock(Uri.class);
        Manager manager = mock(Manager.class);
        updatedManagerRows.put(uri, manager);

        List<ManagerEntry> managerEntriesCreated = new ArrayList<>();

        //ovsdbNodeAugmentation is null
        when(ovsdbNode.getAugmentation(OvsdbNodeAugmentation.class)).thenReturn(null);
        assertEquals(managerEntriesCreated, SouthboundMapper.createManagerEntries(ovsdbNode, updatedManagerRows));

        //ovsdbNodeAugmentation not null
        OvsdbNodeAugmentation ovsdbNodeAugmentation = mock(OvsdbNodeAugmentation.class);
        when(ovsdbNode.getAugmentation(OvsdbNodeAugmentation.class)).thenReturn(ovsdbNodeAugmentation);

        List<ManagerEntry> managerEntries = new ArrayList<>();
        ManagerEntry managerEntry = mock(ManagerEntry.class);
        managerEntries.add(managerEntry);
        when(ovsdbNodeAugmentation.getManagerEntry()).thenReturn(managerEntries);
        when(managerEntry.getTarget()).thenReturn(uri);

        //Test addManagerEntries(managerEntriesCreated, manager)
        Column<GenericTableSchema, String> value = mock(Column.class);
        when(manager.getTargetColumn()).thenReturn(value);
        when(value.getData()).thenReturn("dummy");

        Column<GenericTableSchema, Map<String, String>> statusColumn = mock(Column.class);
        when(manager.getStatusColumn()).thenReturn(statusColumn);
        Map<String, String> statusAttributeMap = new HashMap<>();
        when(statusColumn.getData()).thenReturn(statusAttributeMap);
        String numberOfConnectionValueStr = "999";

        //statusAttributeMap contains N_CONNECTIONS_STR key
        statusAttributeMap.put("n_connections", numberOfConnectionValueStr);

        Column<GenericTableSchema, Boolean> isConnectedColumn = mock(Column.class);
        when(manager.getIsConnectedColumn()).thenReturn(isConnectedColumn);
        when(isConnectedColumn.getData()).thenReturn(true);
        ManagerEntryBuilder managerEntryBuilder = mock(ManagerEntryBuilder.class);
        PowerMockito.whenNew(ManagerEntryBuilder.class).withNoArguments().thenReturn(managerEntryBuilder);
        PowerMockito.whenNew(Uri.class).withAnyArguments().thenReturn(mock(Uri.class));
        when(managerEntryBuilder.setTarget(any(Uri.class))).thenReturn(managerEntryBuilder);
        when(managerEntryBuilder.setNumberOfConnections(any(Long.class))).thenReturn(managerEntryBuilder);
        when(managerEntryBuilder.setConnected(true)).thenReturn(managerEntryBuilder);

        when(managerEntryBuilder.build()).thenReturn(managerEntry);

        assertEquals(managerEntries, SouthboundMapper.createManagerEntries(ovsdbNode, updatedManagerRows));

        //statusAttributeMap contains N_CONNECTIONS_STR key
        statusAttributeMap.remove("n_connections");
        assertEquals(managerEntries, SouthboundMapper.createManagerEntries(ovsdbNode, updatedManagerRows));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetInstanceIdentifier() throws Exception {
        OpenVSwitch ovs = mock(OpenVSwitch.class);
        InstanceIdentifier<Node> iid = mock(InstanceIdentifier.class);
        Column<GenericTableSchema, Map<String, String>> externalIdColumn = mock(Column.class);
        when(ovs.getExternalIdsColumn()).thenReturn(externalIdColumn);
        Map<String, String> externalIdMap = new HashMap<>();
        when(externalIdColumn.getData()).thenReturn(externalIdMap);
        // if true
        externalIdMap.put(SouthboundConstants.IID_EXTERNAL_ID_KEY, "test");
        PowerMockito.mockStatic(SouthboundUtil.class);
        when((InstanceIdentifier<Node>) SouthboundUtil.deserializeInstanceIdentifier(anyString())).thenReturn(iid);
        assertEquals("Incorrect Instance Identifier received", iid, SouthboundMapper.getInstanceIdentifier(ovs));
        // if false
        externalIdMap.clear();
        UUID uuID = new UUID("test");
        when(ovs.getUuid()).thenReturn(uuID);
        assertNotNull(SouthboundMapper.getInstanceIdentifier(ovs));
    }
}
