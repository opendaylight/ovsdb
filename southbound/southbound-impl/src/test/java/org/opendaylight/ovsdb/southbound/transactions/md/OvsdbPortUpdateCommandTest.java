/*
 * Copyright (c) 2015 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.southbound.transactions.md;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.Interface;
import org.opendaylight.ovsdb.schema.openvswitch.Port;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionInstance;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.ovsdb.southbound.SouthboundUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeInternal;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbPortInterfaceAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbPortInterfaceAttributes.VlanMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagedNodeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceExternalIdsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceOtherConfigsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.PortExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.PortExternalIdsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.PortOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.PortOtherConfigsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.TrunksBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberMatcher;
import org.powermock.api.support.membermodification.MemberModifier;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

@PrepareForTest({TyperUtils.class, OvsdbPortUpdateCommand.class, SouthboundUtil.class, SouthboundMapper.class})
@RunWith(PowerMockRunner.class)public class OvsdbPortUpdateCommandTest {

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
    private Map<UUID, Interface> interfaceUpdatedRows;
    private Map<UUID, Interface> interfaceOldRows;
    private Map<UUID, Bridge> bridgeUpdatedRows;
    private OvsdbPortUpdateCommand ovsdbPortUpdateCommand;

    @Before
    public void setUp() throws Exception {
        ovsdbPortUpdateCommand = PowerMockito.mock(OvsdbPortUpdateCommand.class, Mockito.CALLS_REAL_METHODS);
    }

    @Test
    public void testOvsdbPortUpdateCommand() throws Exception {
        OvsdbConnectionInstance key = mock(OvsdbConnectionInstance.class);
        TableUpdates updates = mock(TableUpdates.class);
        DatabaseSchema dbSchema = mock(DatabaseSchema.class);

        PowerMockito.mockStatic(TyperUtils.class);
        PowerMockito.when(TyperUtils.extractRowsUpdated(Port.class, updates, dbSchema)).thenReturn(portUpdatedRows);
        PowerMockito.when(TyperUtils.extractRowsOld(Port.class, updates, dbSchema)).thenReturn(portOldRows);
        PowerMockito.when(TyperUtils.extractRowsUpdated(Interface.class, updates, dbSchema)).thenReturn(interfaceUpdatedRows);
        PowerMockito.when(TyperUtils.extractRowsOld(Interface.class, updates, dbSchema)).thenReturn(interfaceOldRows);
        PowerMockito.when(TyperUtils.extractRowsUpdated(Bridge.class, updates, dbSchema)).thenReturn(bridgeUpdatedRows);

        OvsdbPortUpdateCommand ovsdbPortUpdateCommand1 = new OvsdbPortUpdateCommand(key, updates, dbSchema);
        assertEquals(portUpdatedRows, Whitebox.getInternalState(ovsdbPortUpdateCommand1, "portUpdatedRows"));
        assertEquals(portOldRows, Whitebox.getInternalState(ovsdbPortUpdateCommand1, "portOldRows"));
        assertEquals(dbSchema, Whitebox.getInternalState(ovsdbPortUpdateCommand1, "dbSchema"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecute() throws Exception {
        ReadWriteTransaction transaction= mock(ReadWriteTransaction.class);
        portUpdatedRows = new HashMap<>();
        interfaceOldRows = new HashMap<>();
        portUpdatedRows.put(mock(UUID.class), mock(Port.class));
        interfaceOldRows.put(mock(UUID.class), mock(Interface.class));
        MemberModifier.field(OvsdbPortUpdateCommand.class, "portUpdatedRows").set(ovsdbPortUpdateCommand, portUpdatedRows);
        MemberModifier.field(OvsdbPortUpdateCommand.class, "interfaceOldRows").set(ovsdbPortUpdateCommand, interfaceOldRows);

        OvsdbConnectionInstance ovsdbConnectionInstance = mock(OvsdbConnectionInstance.class);
        when(ovsdbPortUpdateCommand.getOvsdbConnectionInstance()).thenReturn(ovsdbConnectionInstance);
        InstanceIdentifier<Node> connectionIId = mock(InstanceIdentifier.class);
        when(ovsdbConnectionInstance.getInstanceIdentifier()).thenReturn(connectionIId);

        //case 1: portUpdatedRows & interfaceOldRows not null, not empty
        Optional<Node> node = mock(Optional.class);
        PowerMockito.doReturn(node).when(ovsdbPortUpdateCommand, "readNode", any(ReadWriteTransaction.class), any(InstanceIdentifier.class));
        when(node.isPresent()).thenReturn(true);
        when(node.get()).thenReturn(mock(Node.class));
        PowerMockito.suppress(MemberMatcher.method(OvsdbPortUpdateCommand.class, "updateTerminationPoints", ReadWriteTransaction.class, Node.class));
        ovsdbPortUpdateCommand.execute(transaction);
        verify(ovsdbConnectionInstance).getInstanceIdentifier();
        PowerMockito.verifyPrivate(ovsdbPortUpdateCommand).invoke("updateTerminationPoints", any(ReadWriteTransaction.class), any(Node.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpdateTerminationPoints() throws Exception {
        ReadWriteTransaction transaction = mock(ReadWriteTransaction.class);
        Node node = mock(Node.class);

        portUpdatedRows = new HashMap<>();
        Port port = mock(Port.class);
        UUID uuid = mock(UUID.class);
        portUpdatedRows.put(uuid, port);
        MemberModifier.field(OvsdbPortUpdateCommand.class, "portUpdatedRows").set(ovsdbPortUpdateCommand, portUpdatedRows);
        Column<GenericTableSchema, String> bridgeColumn = mock(Column.class);
        when(port.getNameColumn()).thenReturn(bridgeColumn);
        when(bridgeColumn.getData()).thenReturn(TERMINATION_POINT_NAME);

        Optional<InstanceIdentifier<Node>> bridgeIid = mock(Optional.class);
        PowerMockito.doReturn(bridgeIid).when(ovsdbPortUpdateCommand, "getTerminationPointBridge", any(UUID.class));

        //bridgeIid.isPresent() is true
        when(bridgeIid.isPresent()).thenReturn(true);
        when(bridgeIid.get()).thenReturn(mock(InstanceIdentifier.class));
        NodeId bridgeId = mock(NodeId.class);
        PowerMockito.mockStatic(SouthboundMapper.class);
        PowerMockito.when(SouthboundMapper.createManagedNodeId(any(InstanceIdentifier.class))).thenReturn(bridgeId);

        PowerMockito.whenNew(TpId.class).withAnyArguments().thenReturn(mock(TpId.class));
        TerminationPointKey tpKey = mock(TerminationPointKey.class);
        PowerMockito.whenNew(TerminationPointKey.class).withAnyArguments().thenReturn(tpKey);
        TerminationPointBuilder tpBuilder = mock(TerminationPointBuilder.class);
        PowerMockito.whenNew(TerminationPointBuilder.class).withNoArguments().thenReturn(tpBuilder);
        when(tpBuilder.setKey(any(TerminationPointKey.class))).thenReturn(tpBuilder);
        when(tpKey.getTpId()).thenReturn(mock(TpId.class));
        when(tpBuilder.setTpId(any(TpId.class))).thenReturn(tpBuilder);
        InstanceIdentifier<TerminationPoint> tpPath = mock(InstanceIdentifier.class);
        PowerMockito.doReturn(tpPath).when(ovsdbPortUpdateCommand, "getInstanceIdentifier", any(InstanceIdentifier.class), any(Port.class));

        OvsdbTerminationPointAugmentationBuilder tpAugmentationBuilder = mock(OvsdbTerminationPointAugmentationBuilder.class);
        PowerMockito.whenNew(OvsdbTerminationPointAugmentationBuilder.class).withNoArguments().thenReturn(tpAugmentationBuilder);
        PowerMockito.suppress(MemberMatcher.method(OvsdbPortUpdateCommand.class, "buildTerminationPoint", OvsdbTerminationPointAugmentationBuilder.class, Port.class));

        Column<GenericTableSchema, Set<UUID>> interfacesColumn = mock(Column.class);
        when(port.getInterfacesColumn()).thenReturn(interfacesColumn);
        Set<UUID> setUUID = new HashSet<>();
        UUID interfaceUUID = mock(UUID.class);
        setUUID.add(interfaceUUID);
        when(interfacesColumn.getData()).thenReturn(setUUID);

        interfaceUpdatedRows = new HashMap<>();
        interfaceOldRows = new HashMap<>();
        Interface iface = mock(Interface.class);
        interfaceUpdatedRows.put(interfaceUUID, iface);
        Interface interfaceUpdate = mock(Interface.class);
        interfaceUpdatedRows.put(uuid, interfaceUpdate);
        interfaceOldRows.put(interfaceUUID, iface);
        MemberModifier.field(OvsdbPortUpdateCommand.class, "interfaceUpdatedRows").set(ovsdbPortUpdateCommand, interfaceUpdatedRows);
        MemberModifier.field(OvsdbPortUpdateCommand.class, "interfaceOldRows").set(ovsdbPortUpdateCommand, interfaceOldRows);
        PowerMockito.suppress(MemberMatcher.method(OvsdbPortUpdateCommand.class, "buildTerminationPoint", OvsdbTerminationPointAugmentationBuilder.class, Interface.class));

        when(tpAugmentationBuilder.build()).thenReturn(mock(OvsdbTerminationPointAugmentation.class));
        when(tpBuilder.addAugmentation(eq(OvsdbTerminationPointAugmentation.class), any(OvsdbTerminationPointAugmentation.class))).thenReturn(tpBuilder);
        when(tpBuilder.build()).thenReturn(mock(TerminationPoint.class));
        portOldRows = new HashMap<>();
        portOldRows.put(uuid, port);
        MemberModifier.field(OvsdbPortUpdateCommand.class, "portOldRows").set(ovsdbPortUpdateCommand, portOldRows);
        doNothing().when(transaction).merge(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(TerminationPoint.class));
        doNothing().when(transaction).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(TerminationPoint.class));

        Column<GenericTableSchema, String> interfaceColumn = mock(Column.class);
        when(interfaceUpdate.getNameColumn()).thenReturn(interfaceColumn);
        when(interfaceColumn.getData()).thenReturn(INTERFACE_NAME);

        PowerMockito.doReturn(bridgeIid).when(ovsdbPortUpdateCommand, "getTerminationPointBridge", any(ReadWriteTransaction.class), any(Node.class), anyString());
        PowerMockito.when(SouthboundMapper.createManagedNodeId(any(InstanceIdentifier.class))).thenReturn(bridgeId);
        PowerMockito.whenNew(TopologyKey.class).withAnyArguments().thenReturn(mock(TopologyKey.class));
        PowerMockito.whenNew(NodeKey.class).withAnyArguments().thenReturn(mock(NodeKey.class));

        Whitebox.invokeMethod(ovsdbPortUpdateCommand, "updateTerminationPoints", transaction, node);
        PowerMockito.verifyPrivate(ovsdbPortUpdateCommand).invoke("getInstanceIdentifier", any(OvsdbTerminationPointAugmentationBuilder.class), any(Port.class));
        verify(transaction, times(2)).merge(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(TerminationPoint.class));
    }

    @Test
    public void testBuildTerminationPoint() throws Exception {
        OvsdbTerminationPointAugmentationBuilder tpAugmentationBuilder = mock(OvsdbTerminationPointAugmentationBuilder.class);
        Port portUpdate = mock(Port.class);
        when(portUpdate.getName()).thenReturn(PORT_NAME);
        when(portUpdate.getUuid()).thenReturn(mock(UUID.class));
        PowerMockito.whenNew(Uuid.class).withAnyArguments().thenReturn(mock(Uuid.class));
        when(tpAugmentationBuilder.setName(anyString())).thenReturn(tpAugmentationBuilder);
        when(tpAugmentationBuilder.setPortUuid(any(Uuid.class))).thenReturn(tpAugmentationBuilder);
        MemberModifier.suppress(MemberMatcher.method(OvsdbPortUpdateCommand.class, "updatePort", Port.class, OvsdbTerminationPointAugmentationBuilder.class));

        Whitebox.invokeMethod(ovsdbPortUpdateCommand, "buildTerminationPoint", tpAugmentationBuilder, portUpdate);
        verify(tpAugmentationBuilder).setName(anyString());
        verify(tpAugmentationBuilder).setPortUuid(any(Uuid.class));
        PowerMockito.verifyPrivate(ovsdbPortUpdateCommand).invoke("updatePort", any(Port.class), any(OvsdbTerminationPointAugmentationBuilder.class));
    }

    @Test
    public void testBuildTerminationPoint1() throws Exception {
        OvsdbTerminationPointAugmentationBuilder tpAugmentationBuilder = mock(OvsdbTerminationPointAugmentationBuilder.class);
        Interface interfaceUpdate = mock(Interface.class);
        when(interfaceUpdate.getName()).thenReturn(INTERFACE_NAME);
        when(interfaceUpdate.getUuid()).thenReturn(mock(UUID.class));
        PowerMockito.whenNew(Uuid.class).withAnyArguments().thenReturn(mock(Uuid.class));
        when(tpAugmentationBuilder.setName(anyString())).thenReturn(tpAugmentationBuilder);
        when(tpAugmentationBuilder.setInterfaceUuid(any(Uuid.class))).thenReturn(tpAugmentationBuilder);
        MemberModifier.suppress(MemberMatcher.method(OvsdbPortUpdateCommand.class, "updateInterfaces", Interface.class, OvsdbTerminationPointAugmentationBuilder.class));

        Whitebox.invokeMethod(ovsdbPortUpdateCommand, "buildTerminationPoint", tpAugmentationBuilder, interfaceUpdate);
        verify(tpAugmentationBuilder).setName(anyString());
        verify(tpAugmentationBuilder).setInterfaceUuid(any(Uuid.class));
        PowerMockito.verifyPrivate(ovsdbPortUpdateCommand).invoke("updateInterfaces", any(Interface.class), any(OvsdbTerminationPointAugmentationBuilder.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testReadNode() throws Exception {
        ReadWriteTransaction transaction = mock(ReadWriteTransaction.class);
        InstanceIdentifier<Node> nodePath = mock(InstanceIdentifier.class);
        Optional<Node> node = mock(Optional.class);
        CheckedFuture<Optional<Node>, ReadFailedException> checkedFuture = mock(CheckedFuture.class);
        when(transaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(checkedFuture);
        when(checkedFuture.checkedGet()).thenReturn(node);
        assertEquals(node, Whitebox.invokeMethod(ovsdbPortUpdateCommand, "readNode", transaction, nodePath));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetTerminationPointBridge() throws Exception {
        UUID portUUID = mock(UUID.class);
        bridgeUpdatedRows = new HashMap<>();
        UUID bridgeUUID = mock(UUID.class);
        Bridge bridge = mock(Bridge.class);
        bridgeUpdatedRows.put(bridgeUUID, bridge);
        MemberModifier.field(OvsdbPortUpdateCommand.class, "bridgeUpdatedRows").set(ovsdbPortUpdateCommand, bridgeUpdatedRows);

        Column<GenericTableSchema, Set<UUID>> column = mock(Column.class);
        when(bridge.getPortsColumn()).thenReturn(column);
        Set<UUID> set = new HashSet<>();
        set.add(portUUID);
        when(column.getData()).thenReturn(set);

        PowerMockito.mockStatic(SouthboundMapper.class);
        when(ovsdbPortUpdateCommand.getOvsdbConnectionInstance()).thenReturn(mock(OvsdbConnectionInstance.class));
        InstanceIdentifier<Node> nodeIid = mock(InstanceIdentifier.class);
        PowerMockito.when(SouthboundMapper.createInstanceIdentifier(any(OvsdbConnectionInstance.class), any(Bridge.class))).thenReturn(nodeIid);

        Optional<InstanceIdentifier<Node>> testResult = Optional.of(nodeIid);
        assertEquals(testResult, Whitebox.invokeMethod(ovsdbPortUpdateCommand, "getTerminationPointBridge", portUUID));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetTerminationPointBridge1() throws Exception {
        ReadWriteTransaction transaction = mock(ReadWriteTransaction.class);
        Node node = mock(Node.class);
        OvsdbNodeAugmentation ovsdbNode = mock(OvsdbNodeAugmentation.class);
        when(node.getAugmentation(OvsdbNodeAugmentation.class)).thenReturn(ovsdbNode);
        List<ManagedNodeEntry> managedNodes = new ArrayList<>();
        ManagedNodeEntry managedNodeEntry = mock(ManagedNodeEntry.class);
        managedNodes.add(managedNodeEntry);
        when(ovsdbNode.getManagedNodeEntry()).thenReturn(managedNodes);

        Node managedNode = mock(Node.class);
        OvsdbBridgeRef ovsdbBridgeRef = mock(OvsdbBridgeRef.class);
        when(managedNodeEntry.getBridgeRef()).thenReturn(ovsdbBridgeRef);
        InstanceIdentifier<Node> iidNode = mock(InstanceIdentifier.class);
        when((InstanceIdentifier<Node>) ovsdbBridgeRef.getValue()).thenReturn(iidNode);
        Optional<Node> optionalNode = Optional.of(managedNode);
        PowerMockito.doReturn(optionalNode).when(ovsdbPortUpdateCommand, "readNode", any(ReadWriteTransaction.class), any(InstanceIdentifier.class));

        TerminationPointBuilder tpBuilder = mock(TerminationPointBuilder.class);
        PowerMockito.whenNew(TerminationPointBuilder.class).withNoArguments().thenReturn(tpBuilder);
        PowerMockito.whenNew(TpId.class).withAnyArguments().thenReturn(mock(TpId.class));
        PowerMockito.whenNew(TerminationPointKey.class).withAnyArguments().thenReturn(mock(TerminationPointKey.class));
        when(tpBuilder.setKey(any(TerminationPointKey.class))).thenReturn(tpBuilder);

        List<TerminationPoint> terminationPointList = new ArrayList<>();
        TerminationPoint terminationPoint = mock(TerminationPoint.class);
        terminationPointList.add(terminationPoint);
        when(tpBuilder.build()).thenReturn(terminationPoint);
        when(managedNode.getTerminationPoint()).thenReturn(terminationPointList);

        when(managedNode.getAugmentation(OvsdbBridgeAugmentation.class)).thenReturn(mock(OvsdbBridgeAugmentation.class));

        Optional<InstanceIdentifier<Node>> testResult = Optional.of(iidNode);
        Optional<InstanceIdentifier<Node>>  result = Whitebox.invokeMethod(ovsdbPortUpdateCommand, "getTerminationPointBridge", transaction, node, TP_NAME);
        assertEquals(testResult, result);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpdateInterfaces() throws Exception {
        Interface interfaceUpdate = mock(Interface.class);
        OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder = mock(OvsdbTerminationPointAugmentationBuilder.class);
        Column<GenericTableSchema, String> typeColumn = mock(Column.class);
        when(interfaceUpdate.getTypeColumn()).thenReturn(typeColumn);
        when(typeColumn.getData()).thenReturn(OVSDB_INTERFACE_TYPE);
        MemberModifier.suppress(MemberMatcher.method(OvsdbPortUpdateCommand.class, "updateInterface", Interface.class, String.class, OvsdbTerminationPointAugmentationBuilder.class));

        Whitebox.invokeMethod(ovsdbPortUpdateCommand, "updateInterfaces", interfaceUpdate, ovsdbTerminationPointBuilder);
        PowerMockito.verifyPrivate(ovsdbPortUpdateCommand).invoke("updateInterface", any(Interface.class), anyString(), any(OvsdbTerminationPointAugmentationBuilder.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpdateInterface() throws Exception {
        Interface interf = mock(Interface.class);
        OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder = mock(OvsdbTerminationPointAugmentationBuilder.class);
        when(interf.getUuid()).thenReturn(mock(UUID.class));
        PowerMockito.whenNew(Uuid.class).withAnyArguments().thenReturn(mock(Uuid.class));
        when(ovsdbTerminationPointBuilder.setInterfaceUuid(any(Uuid.class))).thenReturn(ovsdbTerminationPointBuilder);
        PowerMockito.mockStatic(SouthboundMapper.class);
        PowerMockito.when(SouthboundMapper.createInterfaceType(anyString())).thenAnswer(new Answer<Class<? extends InterfaceTypeBase>>() {
            public Class<? extends InterfaceTypeBase> answer(
                    InvocationOnMock invocation) throws Throwable {
                return InterfaceTypeInternal.class;
            }
        });
        when(ovsdbTerminationPointBuilder.setInterfaceType(any(Class.class))).thenReturn(ovsdbTerminationPointBuilder);
        MemberModifier.suppress(MemberMatcher.method(OvsdbPortUpdateCommand.class, "updateOfPort", Interface.class, OvsdbTerminationPointAugmentationBuilder.class));
        MemberModifier.suppress(MemberMatcher.method(OvsdbPortUpdateCommand.class, "updateOfPortRequest", Interface.class, OvsdbTerminationPointAugmentationBuilder.class));
        MemberModifier.suppress(MemberMatcher.method(OvsdbPortUpdateCommand.class, "updateInterfaceExternalIds", Interface.class, OvsdbTerminationPointAugmentationBuilder.class));
        MemberModifier.suppress(MemberMatcher.method(OvsdbPortUpdateCommand.class, "updateOptions", Interface.class, OvsdbTerminationPointAugmentationBuilder.class));
        MemberModifier.suppress(MemberMatcher.method(OvsdbPortUpdateCommand.class, "updateInterfaceOtherConfig", Interface.class, OvsdbTerminationPointAugmentationBuilder.class));
        MemberModifier.suppress(MemberMatcher.method(OvsdbPortUpdateCommand.class, "updateInterfaceLldp", Interface.class, OvsdbTerminationPointAugmentationBuilder.class));

        Whitebox.invokeMethod(ovsdbPortUpdateCommand, "updateInterface", interf, OVSDB_INTERFACE_TYPE, ovsdbTerminationPointBuilder);
        verify(ovsdbTerminationPointBuilder).setInterfaceUuid(any(Uuid.class));
        verify(ovsdbTerminationPointBuilder).setInterfaceType(any(Class.class));
        PowerMockito.verifyPrivate(ovsdbPortUpdateCommand).invoke("updateOfPort", any(Interface.class), any(OvsdbTerminationPointAugmentationBuilder.class));
        PowerMockito.verifyPrivate(ovsdbPortUpdateCommand).invoke("updateOfPortRequest", any(Interface.class), any(OvsdbTerminationPointAugmentationBuilder.class));
        PowerMockito.verifyPrivate(ovsdbPortUpdateCommand).invoke("updateInterfaceExternalIds", any(Interface.class), any(OvsdbTerminationPointAugmentationBuilder.class));
        PowerMockito.verifyPrivate(ovsdbPortUpdateCommand).invoke("updateOptions", any(Interface.class), any(OvsdbTerminationPointAugmentationBuilder.class));
        PowerMockito.verifyPrivate(ovsdbPortUpdateCommand).invoke("updateInterfaceOtherConfig", any(Interface.class), any(OvsdbTerminationPointAugmentationBuilder.class));
    }

    @Test
    public void testUpdatePort() throws Exception {
        Port port = mock(Port.class);
        OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder = mock(OvsdbTerminationPointAugmentationBuilder.class);

        MemberModifier.suppress(MemberMatcher.method(OvsdbPortUpdateCommand.class, "updateVlan", Port.class, OvsdbTerminationPointAugmentationBuilder.class));
        MemberModifier.suppress(MemberMatcher.method(OvsdbPortUpdateCommand.class, "updateVlanTrunks", Port.class, OvsdbTerminationPointAugmentationBuilder.class));
        MemberModifier.suppress(MemberMatcher.method(OvsdbPortUpdateCommand.class, "updateVlanMode", Port.class, OvsdbTerminationPointAugmentationBuilder.class));
        MemberModifier.suppress(MemberMatcher.method(OvsdbPortUpdateCommand.class, "updatePortExternalIds", Port.class, OvsdbTerminationPointAugmentationBuilder.class));
        MemberModifier.suppress(MemberMatcher.method(OvsdbPortUpdateCommand.class, "updatePortOtherConfig", Port.class, OvsdbTerminationPointAugmentationBuilder.class));

        Whitebox.invokeMethod(ovsdbPortUpdateCommand, "updatePort", port, ovsdbTerminationPointBuilder);

        PowerMockito.verifyPrivate(ovsdbPortUpdateCommand).invoke("updateVlan", any(Port.class), any(OvsdbTerminationPointAugmentationBuilder.class));
        PowerMockito.verifyPrivate(ovsdbPortUpdateCommand).invoke("updateVlanTrunks", any(Port.class), any(OvsdbTerminationPointAugmentationBuilder.class));
        PowerMockito.verifyPrivate(ovsdbPortUpdateCommand).invoke("updateVlanMode", any(Port.class), any(OvsdbTerminationPointAugmentationBuilder.class));
        PowerMockito.verifyPrivate(ovsdbPortUpdateCommand).invoke("updatePortExternalIds", any(Port.class), any(OvsdbTerminationPointAugmentationBuilder.class));
        PowerMockito.verifyPrivate(ovsdbPortUpdateCommand).invoke("updatePortOtherConfig", any(Port.class), any(OvsdbTerminationPointAugmentationBuilder.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpdateVlan() throws Exception {
        Port port = mock(Port.class);
        OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder = mock(OvsdbTerminationPointAugmentationBuilder.class);
        Column<GenericTableSchema, Set<Long>> column = mock(Column.class);
        when(port.getTagColumn()).thenReturn(column);
        Set<Long> vlanId = new HashSet<>();
        vlanId.add((long) 808);
        when(column.getData()).thenReturn(vlanId);
        PowerMockito.whenNew(VlanId.class).withAnyArguments().thenReturn(mock(VlanId.class));
        when(ovsdbTerminationPointBuilder.setVlanTag(any(VlanId.class))).thenReturn(ovsdbTerminationPointBuilder);
        Whitebox.invokeMethod(ovsdbPortUpdateCommand, "updateVlan", port, ovsdbTerminationPointBuilder);
        verify(ovsdbTerminationPointBuilder).setVlanTag(any(VlanId.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpdateVlanTrunks() throws Exception {
        Port port = mock(Port.class);
        OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder = mock(OvsdbTerminationPointAugmentationBuilder.class);
        Column<GenericTableSchema, Set<Long>> column = mock(Column.class);
        when(port.getTrunksColumn()).thenReturn(column);
        Set<Long> portTrunks = new HashSet<>();
        portTrunks.add((long) 300);
        when(column.getData()).thenReturn(portTrunks);

        TrunksBuilder trunksBuilder = mock(TrunksBuilder.class);
        PowerMockito.whenNew(TrunksBuilder.class).withNoArguments().thenReturn(trunksBuilder);
        PowerMockito.whenNew(VlanId.class).withAnyArguments().thenReturn(mock(VlanId.class));
        when(trunksBuilder.setTrunk(any(VlanId.class))).thenReturn(trunksBuilder);
        when(ovsdbTerminationPointBuilder.setTrunks(any(List.class))).thenReturn(ovsdbTerminationPointBuilder);
        Whitebox.invokeMethod(ovsdbPortUpdateCommand, "updateVlanTrunks", port, ovsdbTerminationPointBuilder);
        verify(trunksBuilder).setTrunk(any(VlanId.class));
        verify(ovsdbTerminationPointBuilder).setTrunks(any(List.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpdateVlanMode() throws Exception {
        Port port = mock(Port.class);
        OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder = mock(OvsdbTerminationPointAugmentationBuilder.class);
        Column<GenericTableSchema, Set<String>> column = mock(Column.class);
        when(port.getVlanModeColumn()).thenReturn(column);
        Set<String> set = new HashSet<>();
        set.add(VLAN_MODE_ACCESS);
        when(column.getData()).thenReturn(set);
        when(ovsdbTerminationPointBuilder.setVlanMode(OvsdbPortInterfaceAttributes.VlanMode.Access)).thenReturn(ovsdbTerminationPointBuilder);
        Whitebox.invokeMethod(ovsdbPortUpdateCommand, "updateVlanMode", port, ovsdbTerminationPointBuilder);
        verify(ovsdbTerminationPointBuilder).setVlanMode(any(VlanMode.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpdateOfPort() throws Exception {
        Interface interf = mock(Interface.class);
        OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder = mock(OvsdbTerminationPointAugmentationBuilder.class);
        Set<Long> ofPorts = new HashSet<>();
        ofPorts.add((long) 10000);
        Column<GenericTableSchema, Set<Long>> column = mock(Column.class);
        when(interf.getOpenFlowPortColumn()).thenReturn(column);
        when(column.getData()).thenReturn(ofPorts);
        when(ovsdbTerminationPointBuilder.setOfport(any(Long.class))).thenReturn(ovsdbTerminationPointBuilder);
        when(interf.getName()).thenReturn(INTERFACE_NAME);
        Whitebox.invokeMethod(ovsdbPortUpdateCommand, "updateOfPort", interf, ovsdbTerminationPointBuilder);
        verify(ovsdbTerminationPointBuilder).setOfport(any(Long.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpdateOfPortRequest() throws Exception {
        Interface interf = mock(Interface.class);
        OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder = mock(OvsdbTerminationPointAugmentationBuilder.class);
        Set<Long> ofPortRequests = new HashSet<>();
        ofPortRequests.add((long) 10000);
        Column<GenericTableSchema, Set<Long>> column = mock(Column.class);
        when(interf.getOpenFlowPortRequestColumn()).thenReturn(column);
        when(column.getData()).thenReturn(ofPortRequests);
        when(ovsdbTerminationPointBuilder.setOfportRequest(any(Integer.class))).thenReturn(ovsdbTerminationPointBuilder);
        when(interf.getName()).thenReturn(INTERFACE_NAME);
        Whitebox.invokeMethod(ovsdbPortUpdateCommand, "updateOfPortRequest", interf, ovsdbTerminationPointBuilder);
        verify(ovsdbTerminationPointBuilder).setOfportRequest(any(Integer.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpdateInterfaceExternalIds() throws Exception {
        Interface interf = mock(Interface.class);
        OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder = mock(OvsdbTerminationPointAugmentationBuilder.class);
        Column<GenericTableSchema, Map<String, String>> column = mock(Column.class);
        when(interf.getExternalIdsColumn()).thenReturn(column);
        Map<String, String> map = new HashMap<>();
        when(column.getData()).thenReturn(map);
        map.put(EXTERNAL_ID_KEY, EXTERNAL_ID_VALUE);
        when(column.getData()).thenReturn(map);

        InterfaceExternalIdsBuilder interfaceExternalIdsBuilder = mock(InterfaceExternalIdsBuilder.class);
        PowerMockito.whenNew(InterfaceExternalIdsBuilder.class).withNoArguments().thenReturn(interfaceExternalIdsBuilder);

        when(interfaceExternalIdsBuilder.setExternalIdKey(anyString())).thenReturn(interfaceExternalIdsBuilder);
        when(interfaceExternalIdsBuilder.setExternalIdValue(anyString())).thenReturn(interfaceExternalIdsBuilder);
        when(interfaceExternalIdsBuilder.build()).thenReturn(mock(InterfaceExternalIds.class));
        when(ovsdbTerminationPointBuilder.setInterfaceExternalIds(any(List.class))).thenReturn(ovsdbTerminationPointBuilder);

        Whitebox.invokeMethod(ovsdbPortUpdateCommand, "updateInterfaceExternalIds", interf, ovsdbTerminationPointBuilder);
        verify(interfaceExternalIdsBuilder).setExternalIdKey(anyString());
        verify(interfaceExternalIdsBuilder).setExternalIdValue(anyString());

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpdatePortExternalIds() throws Exception {
        Port port = mock(Port.class);
        OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder = mock(OvsdbTerminationPointAugmentationBuilder.class);
        Column<GenericTableSchema, Map<String, String>> column = mock(Column.class);
        when(port.getExternalIdsColumn()).thenReturn(column);
        Map<String, String> map = new HashMap<>();
        when(column.getData()).thenReturn(map);
        map.put(EXTERNAL_ID_KEY, EXTERNAL_ID_VALUE);
        when(column.getData()).thenReturn(map);

        PortExternalIdsBuilder portExternalIdsBuilder = mock(PortExternalIdsBuilder.class);
        PowerMockito.whenNew(PortExternalIdsBuilder.class).withNoArguments().thenReturn(portExternalIdsBuilder);

        when(portExternalIdsBuilder.setExternalIdKey(anyString())).thenReturn(portExternalIdsBuilder);
        when(portExternalIdsBuilder.setExternalIdValue(anyString())).thenReturn(portExternalIdsBuilder);
        when(portExternalIdsBuilder.build()).thenReturn(mock(PortExternalIds.class));
        when(ovsdbTerminationPointBuilder.setPortExternalIds(any(List.class))).thenReturn(ovsdbTerminationPointBuilder);

        Whitebox.invokeMethod(ovsdbPortUpdateCommand, "updatePortExternalIds", port, ovsdbTerminationPointBuilder);
        verify(portExternalIdsBuilder).setExternalIdKey(anyString());
        verify(portExternalIdsBuilder).setExternalIdValue(anyString());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpdatePortOtherConfig() throws Exception {
        Port port = mock(Port.class);
        OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder = mock(OvsdbTerminationPointAugmentationBuilder.class);
        Column<GenericTableSchema, Map<String, String>> column = mock(Column.class);
        when(port.getOtherConfigColumn()).thenReturn(column);
        Map<String, String> map = new HashMap<>();
        map.put(OTHER_CONFIG_KEY, OTHER_CONFIG_VALUE);
        when(column.getData()).thenReturn(map);

        PortOtherConfigsBuilder portOtherConfigsBuilder = mock(PortOtherConfigsBuilder.class);
        PowerMockito.whenNew(PortOtherConfigsBuilder.class).withNoArguments().thenReturn(portOtherConfigsBuilder);

        when(portOtherConfigsBuilder.setOtherConfigKey(anyString())).thenReturn(portOtherConfigsBuilder);
        when(portOtherConfigsBuilder.setOtherConfigValue(anyString())).thenReturn(portOtherConfigsBuilder);
        when(portOtherConfigsBuilder.build()).thenReturn(mock(PortOtherConfigs.class));
        when(ovsdbTerminationPointBuilder.setInterfaceOtherConfigs(any(List.class))).thenReturn(ovsdbTerminationPointBuilder);

        Whitebox.invokeMethod(ovsdbPortUpdateCommand, "updatePortOtherConfig", port, ovsdbTerminationPointBuilder);
        verify(portOtherConfigsBuilder).setOtherConfigKey(anyString());
        verify(portOtherConfigsBuilder).setOtherConfigValue(anyString());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpdateInterfaceOtherConfig() throws Exception {
        Interface interf = mock(Interface.class);
        OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder = mock(OvsdbTerminationPointAugmentationBuilder.class);
        Map<String, String> interfaceOtherConfigMap = new HashMap<>();
        interfaceOtherConfigMap.put(OTHER_CONFIG_KEY, OTHER_CONFIG_VALUE);
        Column<GenericTableSchema, Map<String, String>> column = mock(Column.class);
        when(interf.getOtherConfigColumn()).thenReturn(column);
        when(column.getData()).thenReturn(interfaceOtherConfigMap);

        InterfaceOtherConfigsBuilder interfaceOtherConfigsBuilder = mock(InterfaceOtherConfigsBuilder.class);
        PowerMockito.whenNew(InterfaceOtherConfigsBuilder.class).withNoArguments().thenReturn(interfaceOtherConfigsBuilder);

        when(interfaceOtherConfigsBuilder.setOtherConfigKey(anyString())).thenReturn(interfaceOtherConfigsBuilder);
        when(interfaceOtherConfigsBuilder.setOtherConfigValue(anyString())).thenReturn(interfaceOtherConfigsBuilder);
        when(interfaceOtherConfigsBuilder.build()).thenReturn(mock(InterfaceOtherConfigs.class));
        when(ovsdbTerminationPointBuilder.setInterfaceOtherConfigs(any(List.class))).thenReturn(ovsdbTerminationPointBuilder);

        Whitebox.invokeMethod(ovsdbPortUpdateCommand, "updateInterfaceOtherConfig", interf, ovsdbTerminationPointBuilder);
        verify(interfaceOtherConfigsBuilder).setOtherConfigKey(anyString());
        verify(interfaceOtherConfigsBuilder).setOtherConfigValue(anyString());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetInstanceIdentifier() throws Exception {
        InstanceIdentifier<Node> bridgeIid = mock(InstanceIdentifier.class);
        Port port = mock(Port.class);
        Column<GenericTableSchema, Map<String, String>> column = mock(Column.class);
        when(port.getExternalIdsColumn()).thenReturn(column);
        Map<String, String> map = new HashMap<>();
        map.put(SouthboundConstants.IID_EXTERNAL_ID_KEY, "opendaylight-iid");
        when(column.getData()).thenReturn(map);

        PowerMockito.mockStatic(SouthboundUtil.class);
        InstanceIdentifier<TerminationPoint> terminationPointIId = mock(InstanceIdentifier.class);
        PowerMockito.when((InstanceIdentifier<TerminationPoint>) SouthboundUtil.deserializeInstanceIdentifier(anyString())).thenReturn(terminationPointIId);
        assertEquals(terminationPointIId, Whitebox.invokeMethod(ovsdbPortUpdateCommand, "getInstanceIdentifier", bridgeIid, port));
    }
}