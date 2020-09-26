/*
 * Copyright © 2015, 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.southbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.Controller;
import org.opendaylight.ovsdb.schema.openvswitch.Manager;
import org.opendaylight.ovsdb.schema.openvswitch.OpenVSwitch;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathTypeNetdev;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathTypeSystem;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeInternal;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeProtocolOpenflow10;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ProtocolEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ProtocolEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagerEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagerEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint32;

public class SouthboundMapperTest {

    @Test
    public void testCreateIpAddress() throws Exception {
        IpAddress ipAddress = IpAddressBuilder.getDefaultInstance("127.0.0.1");
        InetAddress inetAddress = InetAddress.getByAddress(new byte[] {127, 0, 0, 1});
        assertEquals("Incorrect IP address created", ipAddress, SouthboundMapper.createIpAddress(inetAddress));
    }

    @Test
    public void testCreateInstanceIdentifier() throws Exception {
        NodeId nodeId = NodeId.getDefaultInstance("test");
        InstanceIdentifier<Node> iid = SouthboundMapper.createInstanceIdentifier(nodeId);
        assertEquals(nodeId, iid.firstKeyOf(Node.class).getNodeId());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreateInstanceIdentifier1() throws Exception {
        Bridge bridge = mock(Bridge.class);

        // When bridge is not empty, we expect a deserialized identifier
        Column<GenericTableSchema, Map<String, String>> column = mock(Column.class);
        when(bridge.getExternalIdsColumn()).thenReturn(column);
        Map<String, String> map = new HashMap<>();
        map.put(SouthboundConstants.IID_EXTERNAL_ID_KEY, "IID_EXTERNAL_ID_KEY");
        when(column.getData()).thenReturn(map);
        InstanceIdentifierCodec iidc = mock(InstanceIdentifierCodec.class);
        InstanceIdentifier deserializedIid = InstanceIdentifier.create(Node.class);
        when(iidc.bindingDeserializerOrNull("IID_EXTERNAL_ID_KEY")).thenReturn(deserializedIid);
        OvsdbConnectionInstance client = mock(OvsdbConnectionInstance.class, Mockito.RETURNS_DEEP_STUBS);
        assertEquals("Incorrect Instance Identifier received", deserializedIid,
                SouthboundMapper.createInstanceIdentifier(iidc, client, bridge));

        // When bridge is empty, we expect a new identifier pointing to the bridge
        when(bridge.getExternalIdsColumn()).thenReturn(null);
        when(client.getNodeKey().getNodeId().getValue()).thenReturn("uri");
        when(bridge.getName()).thenReturn("bridgeName");
        InstanceIdentifier<Node> returnedIid = SouthboundMapper.createInstanceIdentifier(iidc, client, bridge);
        assertEquals("Incorrect identifier type", Node.class, returnedIid.getTargetType());
        assertEquals("Incorrect node key", new NodeId(new Uri("uri/bridge/bridgeName")),
                returnedIid.firstKeyOf(Node.class).getNodeId());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreateInstanceIdentifier2() throws Exception {
        Controller controller = mock(Controller.class);

        // When controller is not empty, we expect a deserialized identifier
        Column<GenericTableSchema, Map<String, String>> column = mock(Column.class);
        when(controller.getExternalIdsColumn()).thenReturn(column);
        Map<String, String> map = new HashMap<>();
        map.put(SouthboundConstants.IID_EXTERNAL_ID_KEY, "IID_EXTERNAL_ID_KEY");
        when(column.getData()).thenReturn(map);
        InstanceIdentifierCodec iidc = mock(InstanceIdentifierCodec.class);
        InstanceIdentifier deserializedIid = InstanceIdentifier.create(Node.class);
        when(iidc.bindingDeserializerOrNull("IID_EXTERNAL_ID_KEY")).thenReturn(deserializedIid);
        OvsdbConnectionInstance client = mock(OvsdbConnectionInstance.class, Mockito.RETURNS_DEEP_STUBS);
        assertEquals("Incorrect Instance Identifier received", deserializedIid,
                SouthboundMapper.createInstanceIdentifier(iidc, client, controller, "bridgeName"));

        // When controller is empty, we expect a new identifier pointing to the bridge
        when(controller.getExternalIdsColumn()).thenReturn(null);
        when(client.getNodeKey().getNodeId().getValue()).thenReturn("uri");
        InstanceIdentifier<Node> returnedIid =
                SouthboundMapper.createInstanceIdentifier(iidc, client, controller, "bridgeName");
        assertEquals("Incorrect identifier type", Node.class, returnedIid.getTargetType());
        assertEquals("Incorrect node key", new NodeId(new Uri("uri/bridge/bridgeName")),
                returnedIid.firstKeyOf(Node.class).getNodeId());
    }

    @Test
    public void testCreateInetAddress() throws Exception {
        // IPv4 address
        IpAddress ipV4Address = IpAddressBuilder.getDefaultInstance("99.99.99.99");
        assertEquals("Incorrect InetAddress received", InetAddress.getByAddress(new byte[] {99, 99, 99, 99}),
                SouthboundMapper.createInetAddress(ipV4Address));

        // IPv6 address
        IpAddress ipV6Address = IpAddressBuilder.getDefaultInstance("0000:0000:0000:0000:0000:9999:FE1E:8329");
        assertEquals("Incorrect InetAddress received", InetAddress.getByAddress(
                new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, (byte) 0x99, (byte) 0x99, (byte) 0xFE, 0x1E, (byte) 0x83,
                    0x29 }),
                SouthboundMapper.createInetAddress(ipV6Address));
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
        set.add("00:11:22:33:44:55:66:77");
        when(column.getData()).thenReturn(set);
        assertNotNull(column.getData());

        DatapathId dataPathId = new DatapathId("00:11:22:33:44:55:66:77");

        assertEquals(dataPathId, SouthboundMapper.createDatapathId(bridge));
    }

    @Test
    public void testCreateDatapathType() throws Exception {
        OvsdbBridgeAugmentation mdsalbridge = mock(OvsdbBridgeAugmentation.class);
        when(mdsalbridge.getDatapathType()).thenAnswer(
                (Answer<Class<? extends DatapathTypeBase>>) invocation -> DatapathTypeNetdev.class);
        assertEquals("netdev", SouthboundMapper.createDatapathType(mdsalbridge));

        when(mdsalbridge.getDatapathType()).thenAnswer(
                (Answer<Class<? extends DatapathTypeBase>>) invocation -> DatapathTypeSystem.class);
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
        ProtocolEntry protocolEntry = new ProtocolEntryBuilder()
                .setProtocol(OvsdbBridgeProtocolOpenflow10.class)
                .build();

        when(ovsdbBridgeNode.getProtocolEntry()).thenReturn(Map.of(protocolEntry.key(), protocolEntry));
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

        List<ProtocolEntry> returnedProtocols = SouthboundMapper.createMdsalProtocols(bridge);
        assertEquals(value.size(), returnedProtocols.size());
        assertEquals(OvsdbBridgeProtocolOpenflow10.class, returnedProtocols.get(0).getProtocol());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreateControllerEntries() throws Exception {
        Bridge bridge = mock(Bridge.class);
        Column<GenericTableSchema, Set<UUID>> controllerColumn = mock(Column.class);
        when(bridge.getControllerColumn()).thenReturn(controllerColumn);
        Set<UUID> controllerUUIDs = new HashSet<>();
        String uuidString = "7da709ff-397f-4778-a0e8-994811272fdb";
        UUID uuid = new UUID(uuidString);
        controllerUUIDs.add(uuid);
        Controller controller = mock(Controller.class);
        Column<GenericTableSchema, String> targetColumn = mock(Column.class);
        when(targetColumn.getData()).thenReturn("targetData");
        when(controller.getTargetColumn()).thenReturn(targetColumn);
        when(controller.getUuid()).thenReturn(uuid);
        Column<GenericTableSchema, Boolean> isConnectedColumn = mock(Column.class);
        when(isConnectedColumn.getData()).thenReturn(true);
        when(controller.getIsConnectedColumn()).thenReturn(isConnectedColumn);
        Map<UUID, Controller> updatedControllerRows = new HashMap<>();
        updatedControllerRows.put(uuid, controller);
        when(controllerColumn.getData()).thenReturn(controllerUUIDs);

        List<ControllerEntry> controllerEntries = new ArrayList<>();
        ControllerEntry controllerEntry = new ControllerEntryBuilder()
                .setControllerUuid(Uuid.getDefaultInstance(uuidString))
                .setIsConnected(true)
                .setTarget(Uri.getDefaultInstance("targetData"))
                .build();
        controllerEntries.add(controllerEntry);

        assertEquals(controllerEntries, SouthboundMapper.createControllerEntries(bridge, updatedControllerRows));
    }

    @Test
    public void testCreateControllerEntries1() throws Exception {

    }

    @Test
    public void testCreateOvsdbController() throws Exception {
        try (InputStream resourceAsStream = SouthboundMapperTest.class.getResourceAsStream("openvswitch_schema.json")) {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(resourceAsStream);

            DatabaseSchema dbSchema = DatabaseSchema.fromJson(OvsdbSchemaContants.DATABASE_NAME,
                    jsonNode.get("result"));

            String uuidString = "7da709ff-397f-4778-a0e8-994811272fdb";
            OvsdbBridgeAugmentation omn = new OvsdbBridgeAugmentationBuilder()
                    .setControllerEntry(Collections.singletonList(new ControllerEntryBuilder()
                            .setControllerUuid(Uuid.getDefaultInstance(uuidString))
                            .setTarget(Uri.getDefaultInstance("uri"))
                            .build()))
                    .build();

            Map<UUID, Controller> returnedControllers = SouthboundMapper.createOvsdbController(omn, dbSchema);
            assertEquals(1, returnedControllers.size());
            Controller returnedController = returnedControllers.values().iterator().next();
            assertEquals("uri", returnedController.getTargetColumn().getData());
        }
    }

    @Test
    public void testCreateConnectionInfo() throws Exception {
        OvsdbClient client = mock(OvsdbClient.class, Mockito.RETURNS_DEEP_STUBS);

        InetAddress remoteAddress = InetAddress.getByAddress(new byte[] {1, 2, 3, 4});
        when(client.getConnectionInfo().getRemoteAddress()).thenReturn(remoteAddress);
        when(client.getConnectionInfo().getRemotePort()).thenReturn(8080);
        InetAddress localAddress = InetAddress.getByAddress(new byte[] {1, 2, 3, 5});
        when(client.getConnectionInfo().getLocalAddress()).thenReturn(localAddress);
        when(client.getConnectionInfo().getLocalPort()).thenReturn(8081);

        ConnectionInfo returnedConnectionInfo = SouthboundMapper.createConnectionInfo(client);
        assertEquals(IpAddressBuilder.getDefaultInstance("1.2.3.4"), returnedConnectionInfo.getRemoteIp());
        assertEquals(8080, returnedConnectionInfo.getRemotePort().getValue().toJava());
        assertEquals(IpAddressBuilder.getDefaultInstance("1.2.3.5"), returnedConnectionInfo.getLocalIp());
        assertEquals(8081, returnedConnectionInfo.getLocalPort().getValue().toJava());
    }

    @Test
    public void testSuppressLocalIpPort() throws Exception {
        ConnectionInfo connectionInfo = mock(ConnectionInfo.class);
        IpAddress ipAddress = IpAddressBuilder.getDefaultInstance("1.2.3.4");
        when(connectionInfo.getRemoteIp()).thenReturn(ipAddress);
        PortNumber portNumber = PortNumber.getDefaultInstance("8080");
        when(connectionInfo.getRemotePort()).thenReturn(portNumber);
        ConnectionInfo returnedConnectionInfo = SouthboundMapper.suppressLocalIpPort(connectionInfo);
        assertEquals(ipAddress, returnedConnectionInfo.getRemoteIp());
        assertEquals(portNumber, returnedConnectionInfo.getRemotePort());
        assertNull(returnedConnectionInfo.getLocalIp());
        assertNull(returnedConnectionInfo.getLocalPort());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreateManagerEntries() throws Exception {
        OpenVSwitch ovsdbNode = mock(OpenVSwitch.class);
        Map<UUID, Manager> updatedManagerRows = new HashMap<>();
        Set<UUID> managerUUIDs = new HashSet<>();
        UUID uuid = new UUID("7da709ff-397f-4778-a0e8-994811272fdb");
        Manager manager = mock(Manager.class);
        managerUUIDs.add(uuid);
        updatedManagerRows.put(uuid, manager);
        Column<GenericTableSchema, Set<UUID>> column = mock(Column.class);
        when(ovsdbNode.getManagerOptionsColumn()).thenReturn(column);
        when(column.getData()).thenReturn(managerUUIDs);

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

        assertEquals(Collections.singletonList(new ManagerEntryBuilder()
                .setConnected(true)
                .setNumberOfConnections(Uint32.valueOf(999))
                .setTarget(Uri.getDefaultInstance("dummy"))
                .build()), SouthboundMapper.createManagerEntries(ovsdbNode, updatedManagerRows));

        //statusAttributeMap contains N_CONNECTIONS_STR key
        statusAttributeMap.remove("n_connections");
        assertEquals(Collections.singletonList(new ManagerEntryBuilder()
                .setConnected(true)
                .setNumberOfConnections(Uint32.ONE)
                .setTarget(Uri.getDefaultInstance("dummy"))
                .build()), SouthboundMapper.createManagerEntries(ovsdbNode, updatedManagerRows));
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
        when(ovsdbNode.augmentation(OvsdbNodeAugmentation.class)).thenReturn(null);
        assertEquals(managerEntriesCreated, SouthboundMapper.createManagerEntries(ovsdbNode, updatedManagerRows));

        //ovsdbNodeAugmentation not null
        OvsdbNodeAugmentation ovsdbNodeAugmentation = mock(OvsdbNodeAugmentation.class);
        when(ovsdbNode.augmentation(OvsdbNodeAugmentation.class)).thenReturn(ovsdbNodeAugmentation);

        ManagerEntry managerEntry = new ManagerEntryBuilder().setTarget(uri).build();
        when(ovsdbNodeAugmentation.getManagerEntry()).thenReturn(Map.of(managerEntry.key(), managerEntry));

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

        assertEquals(Collections.singletonList(new ManagerEntryBuilder()
                .setConnected(true)
                .setNumberOfConnections(Uint32.valueOf(999))
                .setTarget(Uri.getDefaultInstance("dummy"))
                .build()), SouthboundMapper.createManagerEntries(ovsdbNode, updatedManagerRows));

        //statusAttributeMap contains N_CONNECTIONS_STR key
        statusAttributeMap.remove("n_connections");
        assertEquals(Collections.singletonList(new ManagerEntryBuilder()
                .setConnected(true)
                .setNumberOfConnections(Uint32.ONE)
                .setTarget(Uri.getDefaultInstance("dummy"))
                .build()), SouthboundMapper.createManagerEntries(ovsdbNode, updatedManagerRows));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetInstanceIdentifier() throws Exception {
        OpenVSwitch ovs = mock(OpenVSwitch.class);
        Column<GenericTableSchema, Map<String, String>> externalIdColumn = mock(Column.class);
        when(ovs.getExternalIdsColumn()).thenReturn(externalIdColumn);
        Map<String, String> externalIdMap = new HashMap<>();
        when(externalIdColumn.getData()).thenReturn(externalIdMap);
        // if true
        externalIdMap.put(SouthboundConstants.IID_EXTERNAL_ID_KEY, "test");
        InstanceIdentifierCodec iidc = mock(InstanceIdentifierCodec.class);
        InstanceIdentifier iid = InstanceIdentifier.create(Node.class);
        when(iidc.bindingDeserializerOrNull("test")).thenReturn(iid);
        assertEquals("Incorrect Instance Identifier received", iid, SouthboundMapper.getInstanceIdentifier(iidc, ovs));
        // if false
        externalIdMap.clear();
        UUID uuID = new UUID("test");
        when(ovs.getUuid()).thenReturn(uuID);
        assertNotNull(SouthboundMapper.getInstanceIdentifier(iidc, ovs));
    }
}
