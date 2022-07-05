/*
 * Copyright © 2015, 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.transactions.md;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.support.membermodification.MemberMatcher.field;
import static org.powermock.api.support.membermodification.MemberMatcher.method;
import static org.powermock.api.support.membermodification.MemberModifier.suppress;

import com.google.common.util.concurrent.FluentFuture;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.Interface;
import org.opendaylight.ovsdb.schema.openvswitch.Port;
import org.opendaylight.ovsdb.southbound.InstanceIdentifierCodec;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionInstance;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.ovsdb.southbound.SouthboundUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeInternal;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbPortInterfaceAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbPortInterfaceAttributes.VlanMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagedNodeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagedNodeEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.TrunksBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberMatcher;
import org.powermock.api.support.membermodification.MemberModifier;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@RunWith(PowerMockRunner.class)
@PrepareForTest({TyperUtils.class, OvsdbPortUpdateCommand.class, SouthboundUtil.class, SouthboundMapper.class})
public class OvsdbPortUpdateCommandTest {

    private static final String OTHER_CONFIG_KEY = "key";
    private static final String OTHER_CONFIG_VALUE = "value";
    private static final String EXTERNAL_ID_KEY = "key";
    private static final String EXTERNAL_ID_VALUE = "value";
    private static final String INTERFACE_NAME = "interface_name";
    private static final String VLAN_MODE_ACCESS = "access";
    private static final String OVSDB_INTERFACE_TYPE = "internal";
    private static final String PORT_NAME = "port_name";
    private static final String TP_NAME = "tp_name";
    private static final String TERMINATION_POINT_NAME = "termination_point_name";

    private Map<UUID, Port> portUpdatedRows;
    private Map<UUID, Port> portOldRows;
    private Map<UUID, Interface> ifUpdatedRows;
    private Map<UUID, Interface> interfaceOldRows;
    private Map<UUID, Bridge> bridgeUpdatedRows;
    private OvsdbPortUpdateCommand ovsdbPortUpdateCommand;

    @Before
    public void setUp() throws Exception {
        ovsdbPortUpdateCommand = PowerMockito.mock(OvsdbPortUpdateCommand.class, Mockito.CALLS_REAL_METHODS);
    }

    @Test
    public void testOvsdbPortUpdateCommand() throws Exception {
        TableUpdates updates = mock(TableUpdates.class);
        DatabaseSchema dbSchema = mock(DatabaseSchema.class);

        PowerMockito.mockStatic(TyperUtils.class);
        PowerMockito.when(TyperUtils.extractRowsUpdated(Port.class, updates, dbSchema)).thenReturn(portUpdatedRows);
        PowerMockito.when(TyperUtils.extractRowsOld(Port.class, updates, dbSchema)).thenReturn(portOldRows);
        PowerMockito.when(TyperUtils.extractRowsUpdated(Interface.class, updates, dbSchema)).thenReturn(ifUpdatedRows);
        PowerMockito.when(TyperUtils.extractRowsOld(Interface.class, updates, dbSchema)).thenReturn(interfaceOldRows);
        PowerMockito.when(TyperUtils.extractRowsUpdated(Bridge.class, updates, dbSchema)).thenReturn(bridgeUpdatedRows);

        OvsdbConnectionInstance key = mock(OvsdbConnectionInstance.class);
        OvsdbPortUpdateCommand ovsdbPortUpdateCommand1 =
                new OvsdbPortUpdateCommand(mock(InstanceIdentifierCodec.class), key, updates, dbSchema);
        assertEquals(portUpdatedRows, Whitebox.getInternalState(ovsdbPortUpdateCommand1, "portUpdatedRows"));
        assertEquals(portOldRows, Whitebox.getInternalState(ovsdbPortUpdateCommand1, "portOldRows"));
        assertEquals(dbSchema, Whitebox.getInternalState(ovsdbPortUpdateCommand1, "dbSchema"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecute() throws Exception {
        portUpdatedRows = new HashMap<>();
        interfaceOldRows = new HashMap<>();
        portUpdatedRows.put(mock(UUID.class), mock(Port.class));
        interfaceOldRows.put(mock(UUID.class), mock(Interface.class));
        field(OvsdbPortUpdateCommand.class, "portUpdatedRows").set(ovsdbPortUpdateCommand, portUpdatedRows);
        field(OvsdbPortUpdateCommand.class, "interfaceOldRows").set(ovsdbPortUpdateCommand, interfaceOldRows);

        OvsdbConnectionInstance ovsdbConnectionInstance = mock(OvsdbConnectionInstance.class);
        when(ovsdbPortUpdateCommand.getOvsdbConnectionInstance()).thenReturn(ovsdbConnectionInstance);
        InstanceIdentifier<Node> connectionIId = mock(InstanceIdentifier.class);
        when(ovsdbConnectionInstance.getInstanceIdentifier()).thenReturn(connectionIId);

        //case 1: portUpdatedRows & interfaceOldRows not null, not empty
        Optional<Node> node = Optional.of(mock(Node.class));
        PowerMockito.doReturn(node).when(ovsdbPortUpdateCommand, "readNode", any(ReadWriteTransaction.class),
                any(InstanceIdentifier.class));
        doNothing().when(ovsdbPortUpdateCommand).updateTerminationPoints(any(ReadWriteTransaction.class),
            any(Node.class));
        ReadWriteTransaction transaction = mock(ReadWriteTransaction.class);
        PowerMockito.mockStatic(SouthboundUtil.class);
        PowerMockito.when(SouthboundUtil.readNode(any(ReadWriteTransaction.class), any(InstanceIdentifier.class)))
            .thenReturn(node);
        ovsdbPortUpdateCommand.execute(transaction);
        verify(ovsdbConnectionInstance).getInstanceIdentifier();
        verify(ovsdbPortUpdateCommand).updateTerminationPoints(any(ReadWriteTransaction.class), any(Node.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUpdateTerminationPoints() throws Exception {
        portUpdatedRows = new HashMap<>();
        Port port = mock(Port.class);
        UUID uuid = mock(UUID.class);
        portUpdatedRows.put(uuid, port);
        field(OvsdbPortUpdateCommand.class, "portUpdatedRows").set(ovsdbPortUpdateCommand, portUpdatedRows);
        Column<GenericTableSchema, String> bridgeColumn = mock(Column.class);
        when(port.getNameColumn()).thenReturn(bridgeColumn);
        when(bridgeColumn.getData()).thenReturn(TERMINATION_POINT_NAME);

        InstanceIdentifier<Node> nodeIid = InstanceIdentifier.create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class, new NodeKey(new NodeId("nodeId")));

        Optional<InstanceIdentifier<Node>> bridgeIid = Optional.of(nodeIid);
        PowerMockito.doReturn(bridgeIid).when(ovsdbPortUpdateCommand, "getTerminationPointBridge", any(UUID.class));

        NodeId bridgeId = mock(NodeId.class);
        PowerMockito.mockStatic(SouthboundMapper.class);
        PowerMockito.when(SouthboundMapper.createManagedNodeId(any(InstanceIdentifier.class))).thenReturn(bridgeId);

        PowerMockito.whenNew(TpId.class).withAnyArguments().thenReturn(mock(TpId.class));
        TerminationPointKey tpKey = mock(TerminationPointKey.class);
        PowerMockito.whenNew(TerminationPointKey.class).withAnyArguments().thenReturn(tpKey);
        TerminationPointBuilder tpBuilder = mock(TerminationPointBuilder.class);
        PowerMockito.whenNew(TerminationPointBuilder.class).withNoArguments().thenReturn(tpBuilder);
        when(tpBuilder.withKey(any(TerminationPointKey.class))).thenReturn(tpBuilder);
        when(tpKey.getTpId()).thenReturn(mock(TpId.class));
        when(tpBuilder.setTpId(any(TpId.class))).thenReturn(tpBuilder);
        InstanceIdentifier<TerminationPoint> tpPath = mock(InstanceIdentifier.class);
        PowerMockito.doReturn(tpPath).when(ovsdbPortUpdateCommand, "getInstanceIdentifier",
                any(InstanceIdentifier.class), any(Port.class));

        OvsdbTerminationPointAugmentationBuilder tpAugmentationBuilder = mock(
                OvsdbTerminationPointAugmentationBuilder.class);
        PowerMockito.whenNew(OvsdbTerminationPointAugmentationBuilder.class).withNoArguments()
                .thenReturn(tpAugmentationBuilder);
        PowerMockito.suppress(MemberMatcher.method(OvsdbPortUpdateCommand.class, "buildTerminationPoint",
                ReadWriteTransaction.class, InstanceIdentifier.class, OvsdbTerminationPointAugmentationBuilder.class,
                Node.class, Entry.class));

        Column<GenericTableSchema, Set<UUID>> interfacesColumn = mock(Column.class);
        when(port.getInterfacesColumn()).thenReturn(interfacesColumn);
        Set<UUID> uuids = new HashSet<>();
        UUID uuid2 = mock(UUID.class);
        uuids.add(uuid2);
        when(interfacesColumn.getData()).thenReturn(uuids);

        ifUpdatedRows = new HashMap<>();
        interfaceOldRows = new HashMap<>();
        Interface iface = mock(Interface.class);
        ifUpdatedRows.put(uuid2, iface);
        Interface interfaceUpdate = mock(Interface.class);
        ifUpdatedRows.put(uuid, interfaceUpdate);
        interfaceOldRows.put(uuid2, iface);
        field(OvsdbPortUpdateCommand.class, "interfaceUpdatedRows").set(ovsdbPortUpdateCommand, ifUpdatedRows);
        field(OvsdbPortUpdateCommand.class, "interfaceOldRows").set(ovsdbPortUpdateCommand, interfaceOldRows);
        PowerMockito.suppress(MemberMatcher.method(OvsdbPortUpdateCommand.class, "buildTerminationPoint",
                OvsdbTerminationPointAugmentationBuilder.class, Interface.class));

        when(tpAugmentationBuilder.build()).thenReturn(mock(OvsdbTerminationPointAugmentation.class));
        when(tpBuilder.addAugmentation(any(OvsdbTerminationPointAugmentation.class))).thenReturn(tpBuilder);
        when(tpBuilder.build()).thenReturn(mock(TerminationPoint.class));
        portOldRows = new HashMap<>();
        portOldRows.put(uuid, port);
        MemberModifier.field(OvsdbPortUpdateCommand.class, "portOldRows").set(ovsdbPortUpdateCommand, portOldRows);
        ReadWriteTransaction transaction = mock(ReadWriteTransaction.class);
        doNothing().when(transaction).merge(any(LogicalDatastoreType.class), any(InstanceIdentifier.class),
                any(TerminationPoint.class));
        doNothing().when(transaction).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class),
                any(TerminationPoint.class));

        Column<GenericTableSchema, String> interfaceColumn = mock(Column.class);
        when(interfaceUpdate.getNameColumn()).thenReturn(interfaceColumn);
        when(interfaceColumn.getData()).thenReturn(INTERFACE_NAME);
        when(ovsdbPortUpdateCommand.getOvsdbConnectionInstance()).thenReturn(mock(OvsdbConnectionInstance.class));

        PowerMockito.doReturn(bridgeIid).when(ovsdbPortUpdateCommand, "getTerminationPointBridge",
                any(ReadWriteTransaction.class), any(Node.class), anyString());
        PowerMockito.when(SouthboundMapper.createManagedNodeId(any(InstanceIdentifier.class))).thenReturn(bridgeId);
        PowerMockito.whenNew(TopologyKey.class).withAnyArguments().thenReturn(mock(TopologyKey.class));
        PowerMockito.whenNew(NodeKey.class).withAnyArguments().thenReturn(mock(NodeKey.class));

        Node node = mock(Node.class);
        Whitebox.invokeMethod(ovsdbPortUpdateCommand, "updateTerminationPoints", transaction, node);
        verify(ovsdbPortUpdateCommand).getInstanceIdentifier(any(InstanceIdentifier.class),
            any(Port.class));
        verify(transaction, times(2)).merge(any(LogicalDatastoreType.class), any(InstanceIdentifier.class),
                any(TerminationPoint.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testBuildTerminationPoint() throws Exception {
        Port portUpdate = mock(Port.class);
        Entry<UUID,Port> portEntry = mock(Entry.class);
        when(portEntry.getValue()).thenReturn(mock(Port.class));
        when(portEntry.getValue().getName()).thenReturn(PORT_NAME);
        when(portEntry.getValue().getUuid()).thenReturn(mock(UUID.class));
        when(portUpdate.getName()).thenReturn(PORT_NAME);
        when(portUpdate.getUuid()).thenReturn(mock(UUID.class));
        PowerMockito.whenNew(Uuid.class).withAnyArguments().thenReturn(mock(Uuid.class));

        OvsdbTerminationPointAugmentationBuilder tpAugmentationBuilder = mock(
                OvsdbTerminationPointAugmentationBuilder.class);

        when(tpAugmentationBuilder.setName(anyString())).thenReturn(tpAugmentationBuilder);
        when(tpAugmentationBuilder.setPortUuid(any(Uuid.class))).thenReturn(tpAugmentationBuilder);
        doNothing().when(ovsdbPortUpdateCommand).updatePort(any(ReadWriteTransaction.class),
            any(Node.class), any(InstanceIdentifier.class), any(Entry.class),
            any(OvsdbTerminationPointAugmentationBuilder.class));

        Node node = mock(Node.class);
        ReadWriteTransaction transaction = mock(ReadWriteTransaction.class);
        InstanceIdentifier<TerminationPoint> tpPath = mock(InstanceIdentifier.class);

        Whitebox.invokeMethod(ovsdbPortUpdateCommand, "buildTerminationPoint", transaction, tpPath,
                tpAugmentationBuilder, node, portEntry);
        verify(tpAugmentationBuilder).setName(anyString());
        verify(tpAugmentationBuilder).setPortUuid(any(Uuid.class));
        verify(ovsdbPortUpdateCommand).updatePort(any(ReadWriteTransaction.class),
                any(Node.class), any(InstanceIdentifier.class), any(Entry.class),
                any(OvsdbTerminationPointAugmentationBuilder.class));
    }

    @Test
    public void testBuildTerminationPoint1() throws Exception {
        Interface interfaceUpdate = mock(Interface.class);
        when(interfaceUpdate.getName()).thenReturn(INTERFACE_NAME);
        when(interfaceUpdate.getUuid()).thenReturn(mock(UUID.class));
        PowerMockito.whenNew(Uuid.class).withAnyArguments().thenReturn(mock(Uuid.class));
        OvsdbTerminationPointAugmentationBuilder tpAugmentationBuilder = mock(
                OvsdbTerminationPointAugmentationBuilder.class);
        when(tpAugmentationBuilder.setName(anyString())).thenReturn(tpAugmentationBuilder);
        when(tpAugmentationBuilder.setInterfaceUuid(any(Uuid.class))).thenReturn(tpAugmentationBuilder);

        doNothing().when(ovsdbPortUpdateCommand).updateInterfaces(any(Interface.class),
            any(OvsdbTerminationPointAugmentationBuilder.class));

        Whitebox.invokeMethod(ovsdbPortUpdateCommand, "buildTerminationPoint", tpAugmentationBuilder, interfaceUpdate);
        verify(tpAugmentationBuilder).setName(anyString());
        verify(tpAugmentationBuilder).setInterfaceUuid(any(Uuid.class));
        verify(ovsdbPortUpdateCommand).updateInterfaces(any(Interface.class),
                any(OvsdbTerminationPointAugmentationBuilder.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testReadNode() throws Exception {
        ReadWriteTransaction transaction = mock(ReadWriteTransaction.class);
        InstanceIdentifier<Node> nodePath = mock(InstanceIdentifier.class);
        Optional<Node> node = Optional.of(mock(Node.class));
        FluentFuture<Optional<Node>> fluentFuture = mock(FluentFuture.class);
        when(transaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class)))
                .thenReturn(fluentFuture);
        when(fluentFuture.get()).thenReturn(node);
        assertEquals(node, Whitebox.invokeMethod(ovsdbPortUpdateCommand, "readNode", transaction, nodePath));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetTerminationPointBridge1() throws Exception {
        Node node = mock(Node.class);
        OvsdbNodeAugmentation ovsdbNode = mock(OvsdbNodeAugmentation.class);
        when(node.augmentation(OvsdbNodeAugmentation.class)).thenReturn(ovsdbNode);

        InstanceIdentifier<Node> iidNode = mock(InstanceIdentifier.class);

        ManagedNodeEntry managedNodeEntry = new ManagedNodeEntryBuilder()
                .setBridgeRef(new OvsdbBridgeRef(iidNode))
                .build();
        when(ovsdbNode.nonnullManagedNodeEntry()).thenCallRealMethod();
        when(ovsdbNode.getManagedNodeEntry()).thenReturn(Map.of(managedNodeEntry.key(), managedNodeEntry));

        Node managedNode = mock(Node.class);
        Optional<Node> optionalNode = Optional.of(managedNode);
        PowerMockito.doReturn(optionalNode).when(ovsdbPortUpdateCommand, "readNode", any(ReadWriteTransaction.class),
                any(InstanceIdentifier.class));

        PowerMockito.mockStatic(SouthboundUtil.class);
        PowerMockito.when(SouthboundUtil.readNode(any(ReadWriteTransaction.class),
                any(InstanceIdentifier.class)))
                .thenReturn(optionalNode);

        TerminationPoint terminationPoint = new TerminationPointBuilder().setTpId(new TpId(TP_NAME)).build();

        TerminationPointBuilder tpBuilder = mock(TerminationPointBuilder.class);
        when(tpBuilder.withKey(any(TerminationPointKey.class))).thenReturn(tpBuilder);
        when(tpBuilder.build()).thenReturn(terminationPoint);

        PowerMockito.whenNew(TerminationPointBuilder.class).withNoArguments().thenReturn(tpBuilder);

        when(managedNode.nonnullTerminationPoint()).thenCallRealMethod();
        when(managedNode.getTerminationPoint()).thenReturn(Map.of(terminationPoint.key(), terminationPoint));

        when(managedNode.augmentation(OvsdbBridgeAugmentation.class))
                .thenReturn(mock(OvsdbBridgeAugmentation.class));

        Optional<InstanceIdentifier<Node>> testResult = Optional.of(iidNode);
        ReadWriteTransaction transaction = mock(ReadWriteTransaction.class);
        Optional<InstanceIdentifier<Node>> result = Whitebox.invokeMethod(ovsdbPortUpdateCommand,
                "getTerminationPointBridge", transaction, node, TP_NAME);

        assertEquals(testResult, result);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpdateInterfaces() throws Exception {
        Interface interfaceUpdate = mock(Interface.class);
        Column<GenericTableSchema, String> typeColumn = mock(Column.class);
        when(interfaceUpdate.getTypeColumn()).thenReturn(typeColumn);
        when(typeColumn.getData()).thenReturn(OVSDB_INTERFACE_TYPE);
        doNothing().when(ovsdbPortUpdateCommand).updateInterface(any(Interface.class), anyString(),
            any(OvsdbTerminationPointAugmentationBuilder.class));

        OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder = mock(
                OvsdbTerminationPointAugmentationBuilder.class);
        Whitebox.invokeMethod(ovsdbPortUpdateCommand, "updateInterfaces", interfaceUpdate,
                ovsdbTerminationPointBuilder);
        verify(ovsdbPortUpdateCommand).updateInterface(any(Interface.class), anyString(),
                any(OvsdbTerminationPointAugmentationBuilder.class));
    }

    @Test
    public void testUpdateInterface() throws Exception {
        Interface interf = mock(Interface.class);
        OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder = mock(
                OvsdbTerminationPointAugmentationBuilder.class);
        when(interf.getUuid()).thenReturn(mock(UUID.class));
        PowerMockito.whenNew(Uuid.class).withAnyArguments().thenReturn(mock(Uuid.class));
        when(ovsdbTerminationPointBuilder.setInterfaceUuid(any(Uuid.class))).thenReturn(ovsdbTerminationPointBuilder);
        PowerMockito.mockStatic(SouthboundMapper.class);
        PowerMockito.when(SouthboundMapper.createInterfaceType(anyString())).thenReturn(InterfaceTypeInternal.VALUE);
        when(ovsdbTerminationPointBuilder.setInterfaceType(any())).thenReturn(ovsdbTerminationPointBuilder);
        suppress(method(OvsdbPortUpdateCommand.class, "updateOfPort", Interface.class,
                OvsdbTerminationPointAugmentationBuilder.class));
        suppress(method(OvsdbPortUpdateCommand.class, "updateOfPortRequest", Interface.class,
                OvsdbTerminationPointAugmentationBuilder.class));
        suppress(method(OvsdbPortUpdateCommand.class, "updateInterfaceExternalIds", Interface.class,
                OvsdbTerminationPointAugmentationBuilder.class));
        suppress(method(OvsdbPortUpdateCommand.class, "updateOptions", Interface.class,
                OvsdbTerminationPointAugmentationBuilder.class));
        suppress(method(OvsdbPortUpdateCommand.class, "updateInterfaceOtherConfig", Interface.class,
                OvsdbTerminationPointAugmentationBuilder.class));
        suppress(method(OvsdbPortUpdateCommand.class, "updateInterfaceLldp", Interface.class,
                OvsdbTerminationPointAugmentationBuilder.class));
        suppress(method(OvsdbPortUpdateCommand.class, "updateInterfaceBfd", Interface.class,
                OvsdbTerminationPointAugmentationBuilder.class));
        suppress(method(OvsdbPortUpdateCommand.class, "updateInterfaceBfdStatus", Interface.class,
                OvsdbTerminationPointAugmentationBuilder.class));
        suppress(method(OvsdbPortUpdateCommand.class, "updateIfIndex", Interface.class,
                OvsdbTerminationPointAugmentationBuilder.class));

        Whitebox.invokeMethod(ovsdbPortUpdateCommand, "updateInterface", interf, OVSDB_INTERFACE_TYPE,
                ovsdbTerminationPointBuilder);
        verify(ovsdbTerminationPointBuilder).setInterfaceUuid(any(Uuid.class));
        verify(ovsdbTerminationPointBuilder).setInterfaceType(any());
        verify(ovsdbPortUpdateCommand).updateOfPort(any(Interface.class),
                any(OvsdbTerminationPointAugmentationBuilder.class));
        verify(ovsdbPortUpdateCommand).updateOfPortRequest(any(Interface.class),
                any(OvsdbTerminationPointAugmentationBuilder.class));
        verify(ovsdbPortUpdateCommand).updateInterfaceExternalIds(any(Interface.class),
                any(OvsdbTerminationPointAugmentationBuilder.class));
        verify(ovsdbPortUpdateCommand).updateOptions(any(Interface.class),
                any(OvsdbTerminationPointAugmentationBuilder.class));
        verify(ovsdbPortUpdateCommand).updateInterfaceOtherConfig(any(Interface.class),
                any(OvsdbTerminationPointAugmentationBuilder.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUpdatePort() throws Exception {
        suppress(method(OvsdbPortUpdateCommand.class, "updateVlan", Port.class,
            OvsdbTerminationPointAugmentationBuilder.class));
        suppress(method(OvsdbPortUpdateCommand.class, "updateVlanTrunks", Port.class,
            OvsdbTerminationPointAugmentationBuilder.class));
        suppress(method(OvsdbPortUpdateCommand.class, "updateVlanMode", Port.class,
            OvsdbTerminationPointAugmentationBuilder.class));
        suppress(method(OvsdbPortUpdateCommand.class, "updatePortExternalIds", Port.class,
            OvsdbTerminationPointAugmentationBuilder.class));
        suppress(method(OvsdbPortUpdateCommand.class, "updatePortOtherConfig", Port.class,
            OvsdbTerminationPointAugmentationBuilder.class));
        suppress(method(OvsdbPortUpdateCommand.class, "updatePortOtherConfig", Port.class,
            OvsdbTerminationPointAugmentationBuilder.class));
        suppress(method(OvsdbPortUpdateCommand.class, "updateQos", ReadWriteTransaction.class, Node.class,
                InstanceIdentifier.class, Entry.class, OvsdbTerminationPointAugmentationBuilder.class));

        Node node = mock(Node.class);
        Entry<UUID, Port> port = new SimpleEntry<>(mock(UUID.class), mock(Port.class));
        OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder = mock(
                OvsdbTerminationPointAugmentationBuilder.class);
        ReadWriteTransaction transaction = mock(ReadWriteTransaction.class);
        InstanceIdentifier<TerminationPoint> tpPath = mock(InstanceIdentifier.class);
        Whitebox.invokeMethod(ovsdbPortUpdateCommand, "updatePort", transaction, node, tpPath, port,
                ovsdbTerminationPointBuilder);

        verify(ovsdbPortUpdateCommand).updateVlan(any(Port.class),
                any(OvsdbTerminationPointAugmentationBuilder.class));
        verify(ovsdbPortUpdateCommand).updateVlanTrunks(any(Port.class),
                any(OvsdbTerminationPointAugmentationBuilder.class));
        verify(ovsdbPortUpdateCommand).updateVlanMode(any(Port.class),
                any(OvsdbTerminationPointAugmentationBuilder.class));
        verify(ovsdbPortUpdateCommand).updatePortExternalIds(any(Port.class),
                any(OvsdbTerminationPointAugmentationBuilder.class));
        verify(ovsdbPortUpdateCommand).updatePortOtherConfig(any(Port.class),
                any(OvsdbTerminationPointAugmentationBuilder.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpdateVlan() throws Exception {
        Port port = mock(Port.class);
        Column<GenericTableSchema, Set<Long>> column = mock(Column.class);
        when(port.getTagColumn()).thenReturn(column);
        Set<Long> vlanId = new HashSet<>();
        vlanId.add((long) 808);
        when(column.getData()).thenReturn(vlanId);
        PowerMockito.whenNew(VlanId.class).withAnyArguments().thenReturn(mock(VlanId.class));
        OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder = mock(
                OvsdbTerminationPointAugmentationBuilder.class);
        when(ovsdbTerminationPointBuilder.setVlanTag(any(VlanId.class))).thenReturn(ovsdbTerminationPointBuilder);
        Whitebox.invokeMethod(ovsdbPortUpdateCommand, "updateVlan", port, ovsdbTerminationPointBuilder);
        verify(ovsdbTerminationPointBuilder).setVlanTag(any(VlanId.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpdateVlanTrunks() throws Exception {
        Port port = mock(Port.class);
        Column<GenericTableSchema, Set<Long>> column = mock(Column.class);
        when(port.getTrunksColumn()).thenReturn(column);
        Set<Long> portTrunks = new HashSet<>();
        portTrunks.add((long) 300);
        when(column.getData()).thenReturn(portTrunks);

        TrunksBuilder trunksBuilder = mock(TrunksBuilder.class);
        PowerMockito.whenNew(TrunksBuilder.class).withNoArguments().thenReturn(trunksBuilder);
        PowerMockito.whenNew(VlanId.class).withAnyArguments().thenReturn(mock(VlanId.class));
        when(trunksBuilder.setTrunk(any(VlanId.class))).thenReturn(trunksBuilder);
        OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder = mock(
                OvsdbTerminationPointAugmentationBuilder.class);
        when(ovsdbTerminationPointBuilder.setTrunks(any(List.class))).thenReturn(ovsdbTerminationPointBuilder);
        Whitebox.invokeMethod(ovsdbPortUpdateCommand, "updateVlanTrunks", port, ovsdbTerminationPointBuilder);
        verify(trunksBuilder).setTrunk(any(VlanId.class));
        verify(ovsdbTerminationPointBuilder).setTrunks(any(List.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpdateVlanMode() throws Exception {
        Port port = mock(Port.class);
        Column<GenericTableSchema, Set<String>> column = mock(Column.class);
        when(port.getVlanModeColumn()).thenReturn(column);
        Set<String> set = new HashSet<>();
        set.add(VLAN_MODE_ACCESS);
        when(column.getData()).thenReturn(set);
        OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder = mock(
                OvsdbTerminationPointAugmentationBuilder.class);
        when(ovsdbTerminationPointBuilder.setVlanMode(OvsdbPortInterfaceAttributes.VlanMode.Access))
                .thenReturn(ovsdbTerminationPointBuilder);
        Whitebox.invokeMethod(ovsdbPortUpdateCommand, "updateVlanMode", port, ovsdbTerminationPointBuilder);
        verify(ovsdbTerminationPointBuilder).setVlanMode(any(VlanMode.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpdateOfPort() throws Exception {
        Interface interf = mock(Interface.class);
        Set<Long> ofPorts = new HashSet<>();
        ofPorts.add((long) 10000);
        Column<GenericTableSchema, Set<Long>> column = mock(Column.class);
        when(interf.getOpenFlowPortColumn()).thenReturn(column);
        when(column.getData()).thenReturn(ofPorts);
        OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder = mock(
                OvsdbTerminationPointAugmentationBuilder.class);
        when(ovsdbTerminationPointBuilder.setOfport(any(Uint32.class))).thenReturn(ovsdbTerminationPointBuilder);
        when(interf.getName()).thenReturn(INTERFACE_NAME);
        Whitebox.invokeMethod(ovsdbPortUpdateCommand, "updateOfPort", interf, ovsdbTerminationPointBuilder);
        verify(ovsdbTerminationPointBuilder).setOfport(any(Uint32.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpdateOfPortRequest() throws Exception {
        Interface interf = mock(Interface.class);
        Set<Long> ofPortRequests = new HashSet<>();
        ofPortRequests.add((long) 10000);
        Column<GenericTableSchema, Set<Long>> column = mock(Column.class);
        when(interf.getOpenFlowPortRequestColumn()).thenReturn(column);
        when(column.getData()).thenReturn(ofPortRequests);
        OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder = mock(
                OvsdbTerminationPointAugmentationBuilder.class);
        when(ovsdbTerminationPointBuilder.setOfportRequest(any(Uint16.class)))
                .thenReturn(ovsdbTerminationPointBuilder);
        when(interf.getName()).thenReturn(INTERFACE_NAME);
        Whitebox.invokeMethod(ovsdbPortUpdateCommand, "updateOfPortRequest", interf, ovsdbTerminationPointBuilder);
        verify(ovsdbTerminationPointBuilder).setOfportRequest(any(Uint16.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpdateInterfaceExternalIds() throws Exception {
        Interface interf = mock(Interface.class);
        Column<GenericTableSchema, Map<String, String>> column = mock(Column.class);
        when(interf.getExternalIdsColumn()).thenReturn(column);
        Map<String, String> map = new HashMap<>();
        when(column.getData()).thenReturn(map);
        map.put(EXTERNAL_ID_KEY, EXTERNAL_ID_VALUE);
        when(column.getData()).thenReturn(map);

        var builder = new OvsdbTerminationPointAugmentationBuilder();
        ovsdbPortUpdateCommand.updateInterfaceExternalIds(interf, builder);
        var list = builder.build().nonnullInterfaceExternalIds().values();
        assertEquals(1, list.size());
        var result = list.iterator().next();
        assertEquals(EXTERNAL_ID_KEY, result.getExternalIdKey());
        assertEquals(EXTERNAL_ID_VALUE, result.getExternalIdValue());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpdatePortExternalIds() throws Exception {
        Port port = mock(Port.class);
        Column<GenericTableSchema, Map<String, String>> column = mock(Column.class);
        when(port.getExternalIdsColumn()).thenReturn(column);
        Map<String, String> map = new HashMap<>();
        when(column.getData()).thenReturn(map);
        map.put(EXTERNAL_ID_KEY, EXTERNAL_ID_VALUE);
        when(column.getData()).thenReturn(map);

        var builder = new OvsdbTerminationPointAugmentationBuilder();
        ovsdbPortUpdateCommand.updatePortExternalIds(port, builder);
        var list = builder.build().nonnullPortExternalIds().values();
        assertEquals(1, list.size());
        var result = list.iterator().next();
        assertEquals(EXTERNAL_ID_KEY, result.getExternalIdKey());
        assertEquals(EXTERNAL_ID_VALUE, result.getExternalIdValue());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpdatePortOtherConfig() throws Exception {
        Port port = mock(Port.class);
        Column<GenericTableSchema, Map<String, String>> column = mock(Column.class);
        when(port.getOtherConfigColumn()).thenReturn(column);
        Map<String, String> map = new HashMap<>();
        map.put(OTHER_CONFIG_KEY, OTHER_CONFIG_VALUE);
        when(column.getData()).thenReturn(map);

        var builder = new OvsdbTerminationPointAugmentationBuilder();
        ovsdbPortUpdateCommand.updatePortOtherConfig(port, builder);
        var list = builder.build().nonnullPortOtherConfigs().values();
        assertEquals(1, list.size());
        var result = list.iterator().next();
        assertEquals(OTHER_CONFIG_KEY, result.getOtherConfigKey());
        assertEquals(OTHER_CONFIG_VALUE, result.getOtherConfigValue());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpdateInterfaceOtherConfig() throws Exception {
        Interface interf = mock(Interface.class);
        Map<String, String> interfaceOtherConfigMap = new HashMap<>();
        interfaceOtherConfigMap.put(OTHER_CONFIG_KEY, OTHER_CONFIG_VALUE);
        Column<GenericTableSchema, Map<String, String>> column = mock(Column.class);
        when(interf.getOtherConfigColumn()).thenReturn(column);
        when(column.getData()).thenReturn(interfaceOtherConfigMap);

        var builder = new OvsdbTerminationPointAugmentationBuilder();
        ovsdbPortUpdateCommand.updateInterfaceOtherConfig(interf, builder);
        var list = builder.build().nonnullInterfaceOtherConfigs().values();
        assertEquals(1, list.size());
        var result = list.iterator().next();
        assertEquals(OTHER_CONFIG_KEY, result.getOtherConfigKey());
        assertEquals(OTHER_CONFIG_VALUE, result.getOtherConfigValue());
    }

    @SuppressWarnings("unchecked")
    @Test
    // TODO This test needs to be re-done
    @Ignore("Broken mock-based test")
    public void testGetInstanceIdentifier() throws Exception {
        Port port = mock(Port.class);
        Column<GenericTableSchema, Map<String, String>> column = mock(Column.class);
        when(port.getExternalIdsColumn()).thenReturn(column);
        Map<String, String> map = new HashMap<>();
        map.put(SouthboundConstants.IID_EXTERNAL_ID_KEY, "opendaylight-iid");
        when(column.getData()).thenReturn(map);

        PowerMockito.mockStatic(SouthboundUtil.class);
        InstanceIdentifier<TerminationPoint> terminationPointIId = mock(InstanceIdentifier.class);
//        PowerMockito
//                .when((InstanceIdentifier<TerminationPoint>) SouthboundUtil.deserializeInstanceIdentifier(
//                        any(InstanceIdentifierCodec.class), anyString()))
//                .thenReturn(terminationPointIId);
        InstanceIdentifier<Node> bridgeIid = mock(InstanceIdentifier.class);
        assertEquals(terminationPointIId,
                Whitebox.invokeMethod(ovsdbPortUpdateCommand, "getInstanceIdentifier", bridgeIid, port));
    }
}
