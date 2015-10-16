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

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.math.NumberUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.Controller;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionInstance;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.ovsdb.southbound.SouthboundUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathTypeSystem;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbFailModeStandalone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.BridgeExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.BridgeExternalIdsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.BridgeOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.BridgeOtherConfigsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ProtocolEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagedNodeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagedNodeEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberMatcher;
import org.powermock.api.support.membermodification.MemberModifier;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import com.google.common.base.Optional;
import com.google.common.net.InetAddresses;

@PrepareForTest({TyperUtils.class, OvsdbBridgeUpdateCommand.class, SouthboundUtil.class, InstanceIdentifier.class, SouthboundMapper.class, InetAddresses.class, NumberUtils.class, NetworkInterface.class})
@RunWith(PowerMockRunner.class)
public class OvsdbBridgeUpdateCommandTest {
    private Map<UUID,Bridge> updatedBridgeRows = new HashMap<UUID,Bridge>();;
    private Map<UUID, Bridge> oldBridgeRows = new HashMap<UUID,Bridge>();
    private OvsdbBridgeUpdateCommand ovsdbBridgeUpdateCommand;

    @Before
    public void setUp() throws Exception {
        ovsdbBridgeUpdateCommand = PowerMockito.mock(OvsdbBridgeUpdateCommand.class, Mockito.CALLS_REAL_METHODS);
        MemberModifier.field(OvsdbBridgeUpdateCommand.class, "updatedBridgeRows").set(ovsdbBridgeUpdateCommand, updatedBridgeRows);
    }

    @Test
    public void testOvsdbBridgeUpdateCommand() {
        OvsdbConnectionInstance key = mock(OvsdbConnectionInstance.class);
        TableUpdates updates = mock(TableUpdates.class);
        DatabaseSchema dbSchema = mock(DatabaseSchema.class);
        OvsdbBridgeUpdateCommand ovsdbBridgeUpdateCommand1 = new OvsdbBridgeUpdateCommand(key, updates, dbSchema);
        assertEquals(key, Whitebox.getInternalState(ovsdbBridgeUpdateCommand1, "key"));
        assertEquals(updates, Whitebox.getInternalState(ovsdbBridgeUpdateCommand1, "updates"));
        assertEquals(dbSchema, Whitebox.getInternalState(ovsdbBridgeUpdateCommand1, "dbSchema"));
    }

    @Test
    public void testExecute() throws Exception {
        ReadWriteTransaction transaction = mock(ReadWriteTransaction.class);
        updatedBridgeRows.put(mock(UUID.class), mock(Bridge.class));
        MemberModifier.suppress(MemberMatcher.method(OvsdbBridgeUpdateCommand.class, "updateBridge", ReadWriteTransaction.class, Bridge.class));
        ovsdbBridgeUpdateCommand.execute(transaction);
        PowerMockito.verifyPrivate(ovsdbBridgeUpdateCommand).invoke("updateBridge", any(ReadWriteTransaction.class), any(Bridge.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpdateBridge() throws Exception {
        ReadWriteTransaction transaction = mock(ReadWriteTransaction.class);
        Bridge bridge = mock(Bridge.class);
        OvsdbConnectionInstance ovsdbConnectionInstance = mock(OvsdbConnectionInstance.class);
        when(ovsdbBridgeUpdateCommand.getOvsdbConnectionInstance()).thenReturn(ovsdbConnectionInstance);
        InstanceIdentifier<Node> connectionIId = mock(InstanceIdentifier.class);
        when(ovsdbConnectionInstance.getInstanceIdentifier()).thenReturn(connectionIId);
        Optional<Node> connection = mock(Optional.class);
        PowerMockito.mockStatic(SouthboundUtil.class);
        when(SouthboundUtil.readNode(any(ReadWriteTransaction.class), any(InstanceIdentifier.class))).thenReturn(connection);
        when(connection.isPresent()).thenReturn(true);
        MemberModifier.suppress(MemberMatcher.method(OvsdbBridgeUpdateCommand.class, "buildConnectionNode", Bridge.class));
        doNothing().when(transaction).merge(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class));

        //suppress calls to private methods
        MemberModifier.suppress(MemberMatcher.method(OvsdbBridgeUpdateCommand.class, "getInstanceIdentifier", Bridge.class));
        MemberModifier.suppress(MemberMatcher.method(OvsdbBridgeUpdateCommand.class, "buildBridgeNode", Bridge.class));
        MemberModifier.suppress(MemberMatcher.method(OvsdbBridgeUpdateCommand.class, "deleteEntries", ReadWriteTransaction.class, List.class));
        MemberModifier.suppress(MemberMatcher.method(OvsdbBridgeUpdateCommand.class, "protocolEntriesToRemove", InstanceIdentifier.class, Bridge.class));
        MemberModifier.suppress(MemberMatcher.method(OvsdbBridgeUpdateCommand.class, "externalIdsToRemove", InstanceIdentifier.class, Bridge.class));
        MemberModifier.suppress(MemberMatcher.method(OvsdbBridgeUpdateCommand.class, "bridgeOtherConfigsToRemove", InstanceIdentifier.class, Bridge.class));

        Whitebox.invokeMethod(ovsdbBridgeUpdateCommand, "updateBridge", transaction, bridge);
        PowerMockito.verifyPrivate(ovsdbBridgeUpdateCommand, times(3)).invoke("deleteEntries", any(ReadWriteTransaction.class), any(Bridge.class));
        verify(ovsdbConnectionInstance).getInstanceIdentifier();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDeleteEntries() throws Exception {
        ReadWriteTransaction transaction = mock(ReadWriteTransaction.class);
        List<InstanceIdentifier<DataObject>> entryIids = new ArrayList<InstanceIdentifier<DataObject>>();
        InstanceIdentifier<DataObject> iid = mock(InstanceIdentifier.class);
        entryIids.add(iid);
        doNothing().when(transaction).delete(any(LogicalDatastoreType.class), (InstanceIdentifier<?>) any(List.class));
        Whitebox.invokeMethod(ovsdbBridgeUpdateCommand, "deleteEntries", transaction, entryIids);
        verify(transaction).delete(any(LogicalDatastoreType.class), (InstanceIdentifier<?>) any(List.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testToRemoveMethods() throws Exception {
        InstanceIdentifier<Node> bridgeIid = PowerMockito.mock(InstanceIdentifier.class);
        Bridge bridge = mock(Bridge.class);
        UUID uuid = mock(UUID.class);
        Bridge oldBridge = mock(Bridge.class);
        oldBridgeRows.put(uuid, oldBridge);
        when(bridge.getUuid()).thenReturn(uuid);
        MemberModifier.field(OvsdbBridgeUpdateCommand.class, "oldBridgeRows").set(ovsdbBridgeUpdateCommand, oldBridgeRows);
        Column<GenericTableSchema, Map<String, String>> column = mock(Column.class);
        Map<String, String> map = new HashMap<String, String>();
        map.put("key", "value");
        when(column.getData()).thenReturn(map);

        //test bridgeOtherConfigsToRemove()
        when(oldBridge.getOtherConfigColumn()).thenReturn(column);
        when(bridge.getOtherConfigColumn()).thenReturn(column);
        List<InstanceIdentifier<BridgeOtherConfigs>> resultBridgeOtherConfigs = Whitebox.invokeMethod(ovsdbBridgeUpdateCommand, "bridgeOtherConfigsToRemove", bridgeIid, bridge);
        assertEquals(ArrayList.class, resultBridgeOtherConfigs.getClass());
        verify(oldBridge, times(2)).getOtherConfigColumn();

        //test externalIdsToRemove()
        when(oldBridge.getExternalIdsColumn()).thenReturn(column);
        when(column.getData()).thenReturn(map);
        when(bridge.getExternalIdsColumn()).thenReturn(column);
        List<InstanceIdentifier<BridgeExternalIds>> resultBridgeExternalIds = Whitebox.invokeMethod(ovsdbBridgeUpdateCommand, "externalIdsToRemove", bridgeIid, bridge);
        assertEquals(ArrayList.class, resultBridgeExternalIds.getClass());
        verify(oldBridge, times(2)).getExternalIdsColumn();

        //test protocolEntriesToRemove()
        Column<GenericTableSchema, Set<String>> column1 = mock(Column.class);
        Set<String> set = new HashSet<String>();
        set.add("element");
        when(column1.getData()).thenReturn(set);
        when(oldBridge.getProtocolsColumn()).thenReturn(column1);
        when(column.getData()).thenReturn(map);
        when(bridge.getProtocolsColumn()).thenReturn(column1);
        List<InstanceIdentifier<ProtocolEntry>> resultProtocolEntry = Whitebox.invokeMethod(ovsdbBridgeUpdateCommand, "protocolEntriesToRemove", bridgeIid, bridge);
        assertEquals(ArrayList.class, resultProtocolEntry.getClass());
        verify(oldBridge, times(2)).getProtocolsColumn();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testBuildConnectionNode() throws Exception {
        Bridge bridge = mock(Bridge.class);
        NodeBuilder connectionNode = mock(NodeBuilder.class);
        PowerMockito.whenNew(NodeBuilder.class).withNoArguments().thenReturn(connectionNode);
        OvsdbConnectionInstance ovsdbConnectionInstance = mock(OvsdbConnectionInstance.class);
        when(ovsdbBridgeUpdateCommand.getOvsdbConnectionInstance()).thenReturn(ovsdbConnectionInstance);
        when(ovsdbConnectionInstance.getNodeId()).thenReturn(mock(NodeId.class));
        when(connectionNode.setNodeId(any(NodeId.class))).thenReturn(connectionNode);

        OvsdbNodeAugmentationBuilder ovsdbConnectionAugmentationBuilder = mock(OvsdbNodeAugmentationBuilder.class);
        PowerMockito.whenNew(OvsdbNodeAugmentationBuilder.class).withNoArguments().thenReturn(ovsdbConnectionAugmentationBuilder);
        PowerMockito.mockStatic(SouthboundMapper.class);
        InstanceIdentifier<Node> bridgeIid = mock(InstanceIdentifier.class);
        when(SouthboundMapper.createInstanceIdentifier(any(OvsdbConnectionInstance.class), any(Bridge.class))).thenReturn(bridgeIid);
        ManagedNodeEntry managedBridge = mock(ManagedNodeEntry.class);
        ManagedNodeEntryBuilder managedNodeEntryBuilder = mock(ManagedNodeEntryBuilder.class);
        PowerMockito.whenNew(ManagedNodeEntryBuilder.class).withNoArguments().thenReturn(managedNodeEntryBuilder);
        PowerMockito.whenNew(OvsdbBridgeRef.class).withAnyArguments().thenReturn(mock(OvsdbBridgeRef.class));
        when(managedNodeEntryBuilder.setBridgeRef(any(OvsdbBridgeRef.class))).thenReturn(managedNodeEntryBuilder);
        when(managedNodeEntryBuilder.build()).thenReturn(managedBridge);
        when(ovsdbConnectionAugmentationBuilder.setManagedNodeEntry(any(List.class))).thenReturn(ovsdbConnectionAugmentationBuilder);

        when(ovsdbConnectionAugmentationBuilder.build()).thenReturn(mock(OvsdbNodeAugmentation.class) );
        when(connectionNode.addAugmentation(eq(OvsdbNodeAugmentation.class), any(OvsdbNodeAugmentation.class))).thenReturn(connectionNode);

        //for logger
        List<ManagedNodeEntry> value = new ArrayList<>();
        value.add(managedBridge);
        when(ovsdbConnectionAugmentationBuilder.getManagedNodeEntry()).thenReturn(value);

        Node node = mock(Node.class);
        when(connectionNode.build()).thenReturn(node);
        assertEquals(node, Whitebox.invokeMethod(ovsdbBridgeUpdateCommand, "buildConnectionNode", bridge));
    }

    @Test
    public void testBuildBridgeNode() throws Exception {
        Bridge bridge= mock(Bridge.class);
        NodeBuilder bridgeNodeBuilder = mock(NodeBuilder.class);
        PowerMockito.whenNew(NodeBuilder.class).withNoArguments().thenReturn(bridgeNodeBuilder);
        //suppress call to getNodeId()
        MemberModifier.suppress(MemberMatcher.method(OvsdbBridgeUpdateCommand.class, "getNodeId", Bridge.class));
        when(bridgeNodeBuilder.setNodeId(any(NodeId.class))).thenReturn(bridgeNodeBuilder);
        OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder = mock(OvsdbBridgeAugmentationBuilder.class);
        PowerMockito.whenNew(OvsdbBridgeAugmentationBuilder.class).withNoArguments().thenReturn(ovsdbBridgeAugmentationBuilder);
        when(bridge.getName()).thenReturn("bridge name");
        PowerMockito.whenNew(OvsdbBridgeName.class).withAnyArguments().thenReturn(mock(OvsdbBridgeName.class));
        when(ovsdbBridgeAugmentationBuilder.setBridgeName(any(OvsdbBridgeName.class))).thenReturn(ovsdbBridgeAugmentationBuilder);
        when(bridge.getUuid()).thenReturn(mock(UUID.class));
        PowerMockito.whenNew(Uuid.class).withAnyArguments().thenReturn(mock(Uuid.class));
        when(ovsdbBridgeAugmentationBuilder.setBridgeUuid(any(Uuid.class))).thenReturn(ovsdbBridgeAugmentationBuilder);

        //suppress calls to the set methods
        MemberModifier.suppress(MemberMatcher.method(OvsdbBridgeUpdateCommand.class, "setDataPath", OvsdbBridgeAugmentationBuilder.class, Bridge.class));
        MemberModifier.suppress(MemberMatcher.method(OvsdbBridgeUpdateCommand.class, "setDataPathType", OvsdbBridgeAugmentationBuilder.class, Bridge.class));
        MemberModifier.suppress(MemberMatcher.method(OvsdbBridgeUpdateCommand.class, "setProtocol", OvsdbBridgeAugmentationBuilder.class, Bridge.class));
        MemberModifier.suppress(MemberMatcher.method(OvsdbBridgeUpdateCommand.class, "setExternalIds", OvsdbBridgeAugmentationBuilder.class, Bridge.class));
        MemberModifier.suppress(MemberMatcher.method(OvsdbBridgeUpdateCommand.class, "setOtherConfig", OvsdbBridgeAugmentationBuilder.class, Bridge.class));
        MemberModifier.suppress(MemberMatcher.method(OvsdbBridgeUpdateCommand.class, "setFailMode", OvsdbBridgeAugmentationBuilder.class, Bridge.class));
        MemberModifier.suppress(MemberMatcher.method(OvsdbBridgeUpdateCommand.class, "setOpenFlowNodeRef", OvsdbBridgeAugmentationBuilder.class, Bridge.class));
        MemberModifier.suppress(MemberMatcher.method(OvsdbBridgeUpdateCommand.class, "setManagedBy", OvsdbBridgeAugmentationBuilder.class));

        when(ovsdbBridgeAugmentationBuilder.build()).thenReturn(mock(OvsdbBridgeAugmentation.class));
        when(bridgeNodeBuilder.addAugmentation(eq(OvsdbBridgeAugmentation.class), any(OvsdbBridgeAugmentation.class))).thenReturn(bridgeNodeBuilder);
        Node node = mock(Node.class);
        when(bridgeNodeBuilder.build()).thenReturn(node );
        assertEquals(node, Whitebox.invokeMethod(ovsdbBridgeUpdateCommand, "buildBridgeNode", bridge));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSetManagedByAndSetDataPathType() throws Exception {
        OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder = mock(OvsdbBridgeAugmentationBuilder.class);

        OvsdbConnectionInstance ovsdbConnectionInstance = mock(OvsdbConnectionInstance.class);
        when(ovsdbBridgeUpdateCommand.getOvsdbConnectionInstance()).thenReturn(ovsdbConnectionInstance);
        when(ovsdbConnectionInstance.getInstanceIdentifier()).thenReturn(mock(InstanceIdentifier.class));
        PowerMockito.whenNew(OvsdbNodeRef.class).withAnyArguments().thenReturn(mock(OvsdbNodeRef.class));
        when(ovsdbBridgeAugmentationBuilder.setManagedBy(any(OvsdbNodeRef.class))).thenReturn(ovsdbBridgeAugmentationBuilder);
        Whitebox.invokeMethod(ovsdbBridgeUpdateCommand, "setManagedBy", ovsdbBridgeAugmentationBuilder);
        verify(ovsdbBridgeAugmentationBuilder).setManagedBy(any(OvsdbNodeRef.class));
        verify(ovsdbConnectionInstance).getInstanceIdentifier();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSetDataPathType() throws Exception {
        OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder = mock(OvsdbBridgeAugmentationBuilder.class);
        Bridge bridge = mock(Bridge.class);
        Column<GenericTableSchema, String> column = mock(Column.class);
        when(bridge.getDatapathTypeColumn()).thenReturn(column);
        when(column.getData()).thenReturn("system");
        PowerMockito.mockStatic(SouthboundMapper.class);
        when(SouthboundMapper.createDatapathType(anyString())).thenAnswer(new Answer<Class<? extends DatapathTypeBase>>() {
            public Class<? extends DatapathTypeBase> answer(
                    InvocationOnMock invocation) throws Throwable {
                return (Class<? extends DatapathTypeBase>) DatapathTypeSystem.class;
            }
        });
        when(ovsdbBridgeAugmentationBuilder.setDatapathType(any(Class.class))).thenReturn(ovsdbBridgeAugmentationBuilder);
        Whitebox.invokeMethod(ovsdbBridgeUpdateCommand, "setDataPathType", ovsdbBridgeAugmentationBuilder, bridge);
        verify(bridge).getDatapathTypeColumn();
        verify(column).getData();
        verify(ovsdbBridgeAugmentationBuilder).setDatapathType(any(Class.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSetFailMode() throws Exception {
        OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder = mock(OvsdbBridgeAugmentationBuilder.class);
        Bridge bridge = mock(Bridge.class);
        Column<GenericTableSchema, Set<String>> column = mock(Column.class);
        when(bridge.getFailModeColumn()).thenReturn(column);
        Set<String> set = new HashSet<String>();
        set.add("standalone");
        when(column.getData()).thenReturn(set);
        when(ovsdbBridgeAugmentationBuilder.setFailMode(OvsdbFailModeStandalone.class)).thenReturn(ovsdbBridgeAugmentationBuilder);
        Whitebox.invokeMethod(ovsdbBridgeUpdateCommand, "setFailMode", ovsdbBridgeAugmentationBuilder, bridge);
        verify(bridge, times(5)).getFailModeColumn();
        verify(ovsdbBridgeAugmentationBuilder).setFailMode(OvsdbFailModeStandalone.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSetOtherConfig() throws Exception {
        OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder = mock(OvsdbBridgeAugmentationBuilder.class);
        Bridge bridge = mock(Bridge.class);
        Column<GenericTableSchema, Map<String, String>> column = mock(Column.class);
        when(bridge.getOtherConfigColumn()).thenReturn(column);
        Map<String, String> map = new HashMap<String, String>();
        map.put("key", "value");
        when(column.getData()).thenReturn(map);

        BridgeOtherConfigsBuilder bridgeOtherConfigsBuilder = mock(BridgeOtherConfigsBuilder.class);
        PowerMockito.whenNew(BridgeOtherConfigsBuilder.class).withNoArguments().thenReturn(bridgeOtherConfigsBuilder);
        when(bridgeOtherConfigsBuilder.setBridgeOtherConfigKey(anyString())).thenReturn(bridgeOtherConfigsBuilder);
        when(bridgeOtherConfigsBuilder.setBridgeOtherConfigValue(anyString())).thenReturn(bridgeOtherConfigsBuilder);
        when(bridgeOtherConfigsBuilder.build()).thenReturn(mock(BridgeOtherConfigs.class));

        when(ovsdbBridgeAugmentationBuilder.setBridgeOtherConfigs(any(List.class))).thenReturn(ovsdbBridgeAugmentationBuilder);
        Whitebox.invokeMethod(ovsdbBridgeUpdateCommand, "setOtherConfig", ovsdbBridgeAugmentationBuilder, bridge);
        verify(bridge).getOtherConfigColumn();
        verify(bridgeOtherConfigsBuilder).setBridgeOtherConfigKey(anyString());
        verify(bridgeOtherConfigsBuilder).setBridgeOtherConfigValue(anyString());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSetExternalIds() throws Exception {
        OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder = mock(OvsdbBridgeAugmentationBuilder.class);
        Bridge bridge = mock(Bridge.class);
        Column<GenericTableSchema, Map<String, String>> column = mock(Column.class);
        when(bridge.getExternalIdsColumn()).thenReturn(column);
        Map<String, String> map = new HashMap<String, String>();
        map.put("key", "value");
        when(column.getData()).thenReturn(map);

        BridgeExternalIdsBuilder bridgeExternalIdsBuilder = mock(BridgeExternalIdsBuilder.class);
        PowerMockito.whenNew(BridgeExternalIdsBuilder.class).withNoArguments().thenReturn(bridgeExternalIdsBuilder);
        when(bridgeExternalIdsBuilder.setBridgeExternalIdKey(anyString())).thenReturn(bridgeExternalIdsBuilder);
        when(bridgeExternalIdsBuilder.setBridgeExternalIdValue(anyString())).thenReturn(bridgeExternalIdsBuilder);
        when(bridgeExternalIdsBuilder.build()).thenReturn(mock(BridgeExternalIds.class));

        when(ovsdbBridgeAugmentationBuilder.setBridgeOtherConfigs(any(List.class))).thenReturn(ovsdbBridgeAugmentationBuilder);
        Whitebox.invokeMethod(ovsdbBridgeUpdateCommand, "setExternalIds", ovsdbBridgeAugmentationBuilder, bridge);
        verify(bridge).getExternalIdsColumn();
        verify(bridgeExternalIdsBuilder).setBridgeExternalIdKey(anyString());
        verify(bridgeExternalIdsBuilder).setBridgeExternalIdValue(anyString());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSetProtocolAndSetDataPath() throws Exception {
        OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder = mock(OvsdbBridgeAugmentationBuilder.class);
        Bridge bridge = mock(Bridge.class);
        PowerMockito.mockStatic(SouthboundMapper.class);

        //Test setProtocol()
        List<ProtocolEntry> listProtocolEntry = new ArrayList<ProtocolEntry>();
        listProtocolEntry.add(mock(ProtocolEntry.class));
        when(SouthboundMapper.createMdsalProtocols(any(Bridge.class))).thenReturn(listProtocolEntry);
        when(ovsdbBridgeAugmentationBuilder.setProtocolEntry(any(List.class))).thenReturn(ovsdbBridgeAugmentationBuilder);
        Whitebox.invokeMethod(ovsdbBridgeUpdateCommand, "setProtocol", ovsdbBridgeAugmentationBuilder, bridge);
        verify(ovsdbBridgeAugmentationBuilder).setProtocolEntry(any(List.class));


        //Test setDataPath()
        DatapathId dpid = mock(DatapathId.class);
        when(SouthboundMapper.createDatapathId(any(Bridge.class))).thenReturn(dpid);
        when(ovsdbBridgeAugmentationBuilder.setDatapathId(any(DatapathId.class))).thenReturn(ovsdbBridgeAugmentationBuilder);
        Whitebox.invokeMethod(ovsdbBridgeUpdateCommand, "setDataPath", ovsdbBridgeAugmentationBuilder, bridge);
        verify(ovsdbBridgeAugmentationBuilder).setDatapathId(any(DatapathId.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSetOpenFlowNodeRef() throws Exception {
        OvsdbBridgeAugmentationBuilder ovsdbBridgeAugmentationBuilder = mock(OvsdbBridgeAugmentationBuilder.class);
        Bridge bridge = mock(Bridge.class);
        PowerMockito.mockStatic(SouthboundMapper.class);

        Map<UUID, Controller> updatedControllerRows = new HashMap<UUID, Controller>();
        when(ovsdbBridgeUpdateCommand.getUpdates()).thenReturn(mock(TableUpdates.class));
        when(ovsdbBridgeUpdateCommand.getDbSchema()).thenReturn(mock(DatabaseSchema.class));
        PowerMockito.mockStatic(TyperUtils.class);
        when(TyperUtils.extractRowsUpdated(eq(Controller.class), any(TableUpdates.class), any(DatabaseSchema.class))).thenReturn(updatedControllerRows);

        List<ControllerEntry> controllerEntryList = new ArrayList<ControllerEntry>();
        ControllerEntry controllerEntry = mock(ControllerEntry.class);
        controllerEntryList.add(controllerEntry);
        when(SouthboundMapper.createControllerEntries(any(Bridge.class), any(Map.class))).thenReturn(controllerEntryList);
        when(controllerEntry.isIsConnected()).thenReturn(true);
        Uri uri = mock(Uri.class);
        when(controllerEntry.getTarget()).thenReturn(uri);
        when(uri.getValue()).thenReturn("tcp:192.168.12.56:6633");

        IpAddress bridgeControllerIpAddress = mock(IpAddress.class);
        PortNumber bridgeControllerPortNumber = mock(PortNumber.class);
        PowerMockito.mockStatic(InetAddresses.class);
        when(InetAddresses.isInetAddress("192.168.12.56")).thenReturn(true);
        PowerMockito.whenNew(IpAddress.class).withAnyArguments().thenReturn(bridgeControllerIpAddress);


        PowerMockito.mockStatic(NumberUtils.class);
        when(NumberUtils.isNumber("6633")).thenReturn(true);
        PowerMockito.whenNew(PortNumber.class).withAnyArguments().thenReturn(bridgeControllerPortNumber);

        PowerMockito.mockStatic(NetworkInterface.class);
        Enumeration<NetworkInterface> networkInterfaces = mock(Enumeration.class);
        when(NetworkInterface.getNetworkInterfaces()).thenReturn(networkInterfaces);

        when(networkInterfaces.hasMoreElements()).thenReturn(true, false);
        NetworkInterface networkInterface = PowerMockito.mock(NetworkInterface.class);
        when(networkInterfaces.nextElement()).thenReturn(networkInterface);

        Enumeration<InetAddress> networkInterfaceAddresses = mock(Enumeration.class);
        when(networkInterface.getInetAddresses()).thenReturn(networkInterfaceAddresses);
        when(networkInterfaceAddresses.hasMoreElements()).thenReturn(true, false);
        InetAddress networkInterfaceAddress = PowerMockito.mock(InetAddress.class);
        when(networkInterfaceAddresses.nextElement()).thenReturn(networkInterfaceAddress);

        Ipv4Address ipv4Address = mock(Ipv4Address.class);
        when(bridgeControllerIpAddress.getIpv4Address()).thenReturn(ipv4Address);
        when(ipv4Address.getValue()).thenReturn("127.0.0.1");
        when(networkInterfaceAddress.getHostAddress()).thenReturn("127.0.0.1");
        assertEquals(bridgeControllerIpAddress.getIpv4Address().getValue(), networkInterfaceAddress.getHostAddress());
        OvsdbConnectionInstance ovsdbConnectionInstance = mock(OvsdbConnectionInstance.class);
        when(ovsdbBridgeUpdateCommand.getOvsdbConnectionInstance()).thenReturn(ovsdbConnectionInstance);
        when(ovsdbConnectionInstance.getInstanceIdentifier()).thenReturn(mock(InstanceIdentifier.class));
        when(ovsdbBridgeAugmentationBuilder.setBridgeOpenflowNodeRef(any(InstanceIdentifier.class))).thenReturn(ovsdbBridgeAugmentationBuilder);

        Whitebox.invokeMethod(ovsdbBridgeUpdateCommand, "setOpenFlowNodeRef", ovsdbBridgeAugmentationBuilder, bridge);
        verify(controllerEntry, times(2)).isIsConnected();
        verify(ovsdbBridgeAugmentationBuilder).setBridgeOpenflowNodeRef(any(InstanceIdentifier.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetInstanceIdentifier() throws Exception {
        PowerMockito.mockStatic(SouthboundMapper.class);
        when(ovsdbBridgeUpdateCommand.getOvsdbConnectionInstance()).thenReturn(mock(OvsdbConnectionInstance.class));
        InstanceIdentifier<Node> iid = mock(InstanceIdentifier.class);
        when(SouthboundMapper.createInstanceIdentifier(any(OvsdbConnectionInstance.class), any(Bridge.class))).thenReturn(iid);

        assertEquals(iid, Whitebox.invokeMethod(ovsdbBridgeUpdateCommand, "getInstanceIdentifier", mock(Bridge.class)));
    }
}
