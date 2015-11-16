/*
 * Copyright (c) 2015 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.southbound.transactions.md;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
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
import org.opendaylight.ovsdb.schema.openvswitch.OpenVSwitch;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionInstance;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.ovsdb.southbound.SouthboundUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathTypeSystem;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeInternal;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.DatapathTypeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.DatapathTypeEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.InterfaceTypeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.InterfaceTypeEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchExternalIdsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchOtherConfigsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberMatcher;
import org.powermock.api.support.membermodification.MemberModifier;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@PrepareForTest({TyperUtils.class, OpenVSwitchUpdateCommand.class, SouthboundUtil.class, SouthboundMapper.class, InstanceIdentifier.class})
@RunWith(PowerMockRunner.class)
public class OpenVSwitchUpdateCommandTest {
    @Mock private OpenVSwitchUpdateCommand openVSwitchUpdateCommand;
    @Mock private TableUpdates updates;
    @Mock private DatabaseSchema dbSchema;

    @Before
    public void setUp() {
        openVSwitchUpdateCommand = mock(OpenVSwitchUpdateCommand.class, Mockito.CALLS_REAL_METHODS);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecute() throws Exception {
        PowerMockito.mockStatic(TyperUtils.class);
        Map<UUID, OpenVSwitch> updatedOpenVSwitchRows = new HashMap<>();
        UUID uuid = mock(UUID.class);
        OpenVSwitch ovs = mock(OpenVSwitch.class);
        updatedOpenVSwitchRows.put(uuid, ovs);
        when(TyperUtils.extractRowsUpdated(eq(OpenVSwitch.class), any(TableUpdates.class), any(DatabaseSchema.class))).thenReturn(updatedOpenVSwitchRows);
        Map<UUID, OpenVSwitch> deletedOpenVSwitchRows = new HashMap<>();
        OpenVSwitch ovs1 = mock(OpenVSwitch.class);

        deletedOpenVSwitchRows.put(uuid, ovs1);
        when(TyperUtils.extractRowsOld(eq(OpenVSwitch.class), any(TableUpdates.class), any(DatabaseSchema.class))).thenReturn(deletedOpenVSwitchRows);

        //mock getUpdates() and getDbSchema()
        when(openVSwitchUpdateCommand.getUpdates()).thenReturn(updates);
        when(openVSwitchUpdateCommand.getDbSchema()).thenReturn(dbSchema);

        //Test getInstanceIdentifier(): case 1: ovs.getExternalIdsColumn().getData().containsKey(SouthboundConstants.IID_EXTERNAL_ID_KEY)) == true
        Column<GenericTableSchema, Map<String, String>> column = mock(Column.class);
        Map<String, String> map = new HashMap<>();
        map.put(SouthboundConstants.IID_EXTERNAL_ID_KEY, "iidString");
        when(ovs.getExternalIdsColumn()).thenReturn(column);
        when(column.getData()).thenReturn(map);
        InstanceIdentifier<Node> iid = mock(InstanceIdentifier.class);
        PowerMockito.mockStatic(SouthboundUtil.class);
        when((InstanceIdentifier<Node>) SouthboundUtil.deserializeInstanceIdentifier(anyString())).thenReturn(iid);
        MemberModifier.suppress(MemberMatcher.method(OpenVSwitchUpdateCommand.class, "getOvsdbConnectionInstance"));
        OvsdbConnectionInstance ovsdbConnectionInstance = mock(OvsdbConnectionInstance.class);
        when(openVSwitchUpdateCommand.getOvsdbConnectionInstance()).thenReturn(ovsdbConnectionInstance);
        doNothing().when(ovsdbConnectionInstance).setInstanceIdentifier(any(InstanceIdentifier.class));
        when(ovsdbConnectionInstance.getInstanceIdentifier()).thenReturn(iid);

        OvsdbNodeAugmentationBuilder ovsdbNodeBuilder = mock(OvsdbNodeAugmentationBuilder.class);
        PowerMockito.whenNew(OvsdbNodeAugmentationBuilder.class).withNoArguments().thenReturn(ovsdbNodeBuilder);

        //suppress the setter methods of the class
        MemberModifier.suppress(MemberMatcher.method(OpenVSwitchUpdateCommand.class, "setVersion", OvsdbNodeAugmentationBuilder.class, OpenVSwitch.class));
        MemberModifier.suppress(MemberMatcher.method(OpenVSwitchUpdateCommand.class, "setDataPathTypes", OvsdbNodeAugmentationBuilder.class, OpenVSwitch.class));
        MemberModifier.suppress(MemberMatcher.method(OpenVSwitchUpdateCommand.class, "setInterfaceTypes", OvsdbNodeAugmentationBuilder.class, OpenVSwitch.class));
        MemberModifier.suppress(MemberMatcher.method(OpenVSwitchUpdateCommand.class, "setExternalIds", ReadWriteTransaction.class, OvsdbNodeAugmentationBuilder.class, OpenVSwitch.class, OpenVSwitch.class));
        MemberModifier.suppress(MemberMatcher.method(OpenVSwitchUpdateCommand.class, "setOtherConfig", ReadWriteTransaction.class, OvsdbNodeAugmentationBuilder.class, OpenVSwitch.class, OpenVSwitch.class));
        MemberModifier.suppress(MemberMatcher.method(OpenVSwitchUpdateCommand.class, "getConnectionInfo"));
        when(openVSwitchUpdateCommand.getConnectionInfo()).thenReturn(mock(ConnectionInfo.class));
        when(ovsdbNodeBuilder.setConnectionInfo(any(ConnectionInfo.class))).thenReturn(ovsdbNodeBuilder);

        NodeBuilder nodeBuilder = mock(NodeBuilder.class);
        PowerMockito.whenNew(NodeBuilder.class).withNoArguments().thenReturn(nodeBuilder);

        //suppress getNodeId()
        MemberModifier.suppress(MemberMatcher.method(OpenVSwitchUpdateCommand.class, "getNodeId", OpenVSwitch.class));
        when(nodeBuilder.setNodeId(any(NodeId.class))).thenReturn(nodeBuilder);
        when(ovsdbNodeBuilder.build()).thenReturn(mock(OvsdbNodeAugmentation.class));
        when(nodeBuilder.addAugmentation(eq(OvsdbNodeAugmentation.class), any(OvsdbNodeAugmentation.class))).thenReturn(nodeBuilder);
        when(nodeBuilder.build()).thenReturn(mock(Node.class));
        ReadWriteTransaction transaction= mock(ReadWriteTransaction.class);
        doNothing().when(transaction).merge(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class));

        openVSwitchUpdateCommand.execute(transaction);
        verify(openVSwitchUpdateCommand, times(2)).getUpdates();
        verify(openVSwitchUpdateCommand, times(2)).getDbSchema();

        //Test getInstanceIdentifier(): case 2: ovs.getExternalIdsColumn() is null
        when(ovs.getExternalIdsColumn()).thenReturn(null);
        when(ovs.getUuid()).thenReturn(uuid);
        openVSwitchUpdateCommand.execute(transaction);
        verify(openVSwitchUpdateCommand, times(4)).getUpdates();
        verify(openVSwitchUpdateCommand, times(4)).getDbSchema();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSetOtherConfig() throws Exception {
        ReadWriteTransaction transaction = mock(ReadWriteTransaction.class);
        OvsdbNodeAugmentationBuilder ovsdbNodeBuilder = mock(OvsdbNodeAugmentationBuilder.class);
        OpenVSwitch oldEntry = mock(OpenVSwitch.class);
        OpenVSwitch openVSwitch = mock(OpenVSwitch.class);

        Column<GenericTableSchema, Map<String, String>> column = mock(Column.class);
        when(openVSwitch.getOtherConfigColumn()).thenReturn(column);
        Map<String, String> map = new HashMap<>();
        when(column.getData()).thenReturn(map);
        when(oldEntry.getOtherConfigColumn()).thenReturn(column);
        MemberModifier.suppress(MemberMatcher.method(OpenVSwitchUpdateCommand.class, "removeOldConfigs", ReadWriteTransaction.class, Map.class, OpenVSwitch.class));
        MemberModifier.suppress(MemberMatcher.method(OpenVSwitchUpdateCommand.class, "setNewOtherConfigs", OvsdbNodeAugmentationBuilder.class, Map.class));

        Whitebox.invokeMethod(openVSwitchUpdateCommand, "setOtherConfig", transaction, ovsdbNodeBuilder, oldEntry, openVSwitch);
        verify(openVSwitch, times(2)).getOtherConfigColumn();
        verify(oldEntry, times(2)).getOtherConfigColumn();
        PowerMockito.verifyPrivate(openVSwitchUpdateCommand).invoke("removeOldConfigs", any(ReadWriteTransaction.class), any(Map.class), any(OpenVSwitch.class));
    }

    @Test
    public void testRemoveOldConfigs() throws Exception {
        ReadWriteTransaction transaction = mock(ReadWriteTransaction.class);
        Map<String, String> oldOtherConfigs = new HashMap<>();
        oldOtherConfigs.put("OpenvswitchOtherConfigsKey", "OpenvswitchOtherConfigsValue");
        OpenVSwitch ovs = mock(OpenVSwitch.class);
        doNothing().when(transaction).delete(any(LogicalDatastoreType.class), any(KeyedInstanceIdentifier.class));

        //suppress getNodeId()
        MemberModifier.suppress(MemberMatcher.method(OpenVSwitchUpdateCommand.class, "getNodeId", OpenVSwitch.class));
        PowerMockito.whenNew(NodeKey.class).withAnyArguments().thenReturn(mock(NodeKey.class));
        Whitebox.invokeMethod(openVSwitchUpdateCommand, "removeOldConfigs", transaction, oldOtherConfigs, ovs);
        verify(transaction).delete(any(LogicalDatastoreType.class), any(KeyedInstanceIdentifier.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSetNewOtherConfigs() throws Exception {
        OvsdbNodeAugmentationBuilder ovsdbNodeBuilder = mock(OvsdbNodeAugmentationBuilder.class);
        Map<String, String> otherConfigs = new HashMap<>();
        otherConfigs.put("otherConfigKey", "otherConfigValue");

        OpenvswitchOtherConfigsBuilder openvswitchOtherConfigsBuilder = mock(OpenvswitchOtherConfigsBuilder.class);
        PowerMockito.whenNew(OpenvswitchOtherConfigsBuilder.class).withNoArguments().thenReturn(openvswitchOtherConfigsBuilder);
        when(openvswitchOtherConfigsBuilder.setOtherConfigKey(anyString())).thenReturn(openvswitchOtherConfigsBuilder);
        when(openvswitchOtherConfigsBuilder.setOtherConfigValue(anyString())).thenReturn(openvswitchOtherConfigsBuilder);
        when(openvswitchOtherConfigsBuilder.build()).thenReturn(mock(OpenvswitchOtherConfigs.class));
        when(ovsdbNodeBuilder.setOpenvswitchOtherConfigs(any(List.class))).thenReturn(ovsdbNodeBuilder);

        Whitebox.invokeMethod(openVSwitchUpdateCommand, "setNewOtherConfigs", ovsdbNodeBuilder, otherConfigs);
        verify(openvswitchOtherConfigsBuilder).setOtherConfigKey(anyString());
        verify(openvswitchOtherConfigsBuilder).setOtherConfigValue(anyString());
        verify(ovsdbNodeBuilder).setOpenvswitchOtherConfigs(any(List.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSetExternalIds() throws Exception {
        ReadWriteTransaction transaction = mock(ReadWriteTransaction.class);
        OvsdbNodeAugmentationBuilder ovsdbNodeBuilder = mock(OvsdbNodeAugmentationBuilder.class);
        OpenVSwitch oldEntry = mock(OpenVSwitch.class);
        OpenVSwitch openVSwitch = mock(OpenVSwitch.class);

        Column<GenericTableSchema, Map<String, String>> column = mock(Column.class);
        when(openVSwitch.getExternalIdsColumn()).thenReturn(column);
        Map<String, String> map = new HashMap<>();
        when(column.getData()).thenReturn(map);
        when(oldEntry.getExternalIdsColumn()).thenReturn(column);
        MemberModifier.suppress(MemberMatcher.method(OpenVSwitchUpdateCommand.class, "removeExternalIds", ReadWriteTransaction.class, Map.class, OpenVSwitch.class));
        MemberModifier.suppress(MemberMatcher.method(OpenVSwitchUpdateCommand.class, "setNewExternalIds", OvsdbNodeAugmentationBuilder.class, Map.class));

        Whitebox.invokeMethod(openVSwitchUpdateCommand, "setExternalIds", transaction, ovsdbNodeBuilder, oldEntry, openVSwitch);
        verify(openVSwitch, times(2)).getExternalIdsColumn();
        verify(oldEntry, times(2)).getExternalIdsColumn();
        PowerMockito.verifyPrivate(openVSwitchUpdateCommand).invoke("removeExternalIds", any(ReadWriteTransaction.class), any(Map.class), any(OpenVSwitch.class));
    }

    @Test
    public void testRemoveExternalIds() throws Exception {
        ReadWriteTransaction transaction = mock(ReadWriteTransaction.class);
        Map<String, String> oldExternalIds = new HashMap<>();
        oldExternalIds.put("OpenvswitchExternalIdKey", "OpenvswitchExternalIdValue");
        OpenVSwitch ovs = mock(OpenVSwitch.class);
        doNothing().when(transaction).delete(any(LogicalDatastoreType.class), any(KeyedInstanceIdentifier.class));

        //suppress getNodeId()
        MemberModifier.suppress(MemberMatcher.method(OpenVSwitchUpdateCommand.class, "getNodeId", OpenVSwitch.class));
        PowerMockito.whenNew(NodeKey.class).withAnyArguments().thenReturn(mock(NodeKey.class));
        Whitebox.invokeMethod(openVSwitchUpdateCommand, "removeExternalIds", transaction, oldExternalIds, ovs);
        verify(transaction).delete(any(LogicalDatastoreType.class), any(KeyedInstanceIdentifier.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSetNewExternalIds() throws Exception {
        OvsdbNodeAugmentationBuilder ovsdbNodeBuilder = mock(OvsdbNodeAugmentationBuilder.class);
        Map<String, String> externalIds = new HashMap<>();
        externalIds.put("externalIdsKey", "externalIdsValue");

        OpenvswitchExternalIdsBuilder openvswitchExternalIdsBuilder = mock(OpenvswitchExternalIdsBuilder.class);
        PowerMockito.whenNew(OpenvswitchExternalIdsBuilder.class).withNoArguments().thenReturn(openvswitchExternalIdsBuilder);
        when(openvswitchExternalIdsBuilder.setExternalIdKey(anyString())).thenReturn(openvswitchExternalIdsBuilder);
        when(openvswitchExternalIdsBuilder.setExternalIdValue(anyString())).thenReturn(openvswitchExternalIdsBuilder);
        when(openvswitchExternalIdsBuilder.build()).thenReturn(mock(OpenvswitchExternalIds.class));
        when(ovsdbNodeBuilder.setOpenvswitchExternalIds(any(List.class))).thenReturn(ovsdbNodeBuilder);

        Whitebox.invokeMethod(openVSwitchUpdateCommand, "setNewExternalIds", ovsdbNodeBuilder, externalIds);
        verify(openvswitchExternalIdsBuilder).setExternalIdKey(anyString());
        verify(openvswitchExternalIdsBuilder).setExternalIdValue(anyString());
        verify(ovsdbNodeBuilder).setOpenvswitchExternalIds(any(List.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSetInterfaceTypes() throws Exception {
        OvsdbNodeAugmentationBuilder ovsdbNodeBuilder = mock(OvsdbNodeAugmentationBuilder.class);
        OpenVSwitch openVSwitch = mock(OpenVSwitch.class);

        Column<GenericTableSchema, Set<String>> column = mock(Column.class);
        when(openVSwitch.getIfaceTypesColumn()).thenReturn(column );
        Set<String> set = new HashSet<>();
        set.add("dpdk");
        set.add("dpdkr");
        set.add("dpdkvhostuser");
        set.add("geneve");
        set.add("gre");
        set.add("internal");
        set.add("ipsec_gre");
        set.add("lisp");
        set.add("patch");
        set.add("stt");
        set.add("system");
        set.add("tap");
        set.add("vxlan");
        when(column.getData()).thenReturn(set);
        PowerMockito.mockStatic(SouthboundMapper.class);
        when(SouthboundMapper.createInterfaceType(anyString())).thenAnswer(new Answer<Class<? extends InterfaceTypeBase>>() {
            public Class<? extends InterfaceTypeBase> answer(
                    InvocationOnMock invocation) throws Throwable {
                return InterfaceTypeInternal.class;
            }
        });

        InterfaceTypeEntry ifEntry = mock(InterfaceTypeEntry.class);
        InterfaceTypeEntryBuilder interfaceTypeEntryBuilder = mock(InterfaceTypeEntryBuilder.class);
        PowerMockito.whenNew(InterfaceTypeEntryBuilder.class).withNoArguments().thenReturn(interfaceTypeEntryBuilder);
        when(interfaceTypeEntryBuilder.setInterfaceType(InterfaceTypeInternal.class)).thenReturn(interfaceTypeEntryBuilder);
        when(interfaceTypeEntryBuilder.build()).thenReturn(ifEntry);

        when(ovsdbNodeBuilder.setInterfaceTypeEntry(any(List.class))).thenReturn(ovsdbNodeBuilder);
        Whitebox.invokeMethod(openVSwitchUpdateCommand, "setInterfaceTypes", ovsdbNodeBuilder, openVSwitch);
        verify(openVSwitch).getIfaceTypesColumn();
        verify(interfaceTypeEntryBuilder,times(13)).setInterfaceType(InterfaceTypeInternal.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSetDataPathTypes() throws Exception {
        OvsdbNodeAugmentationBuilder ovsdbNodeBuilder = mock(OvsdbNodeAugmentationBuilder.class);
        OpenVSwitch openVSwitch = mock(OpenVSwitch.class);
        Column<GenericTableSchema, Set<String>> column = mock(Column.class);
        when(openVSwitch.getDatapathTypesColumn()).thenReturn(column );
        Set<String> set = new HashSet<>();
        set.add("netdev");
        set.add("system");
        when(column.getData()).thenReturn(set);
        PowerMockito.mockStatic(SouthboundMapper.class);
        when(SouthboundMapper.createDatapathType(anyString())).thenAnswer(new Answer<Class<? extends DatapathTypeBase>>() {
            public Class<? extends DatapathTypeBase> answer(
                    InvocationOnMock invocation) throws Throwable {
                return DatapathTypeSystem.class;
            }
        });
        DatapathTypeEntry dpEntry = mock(DatapathTypeEntry.class);
        DatapathTypeEntryBuilder datapathTypeEntryBuilder = mock(DatapathTypeEntryBuilder.class);
        PowerMockito.whenNew(DatapathTypeEntryBuilder.class).withNoArguments().thenReturn(datapathTypeEntryBuilder);
        when(datapathTypeEntryBuilder.setDatapathType(DatapathTypeSystem.class)).thenReturn(datapathTypeEntryBuilder);
        when(datapathTypeEntryBuilder.build()).thenReturn(dpEntry);

        when(ovsdbNodeBuilder.setDatapathTypeEntry(any(List.class))).thenReturn(ovsdbNodeBuilder);
        Whitebox.invokeMethod(openVSwitchUpdateCommand, "setDataPathTypes", ovsdbNodeBuilder, openVSwitch);
        verify(openVSwitch).getDatapathTypesColumn();
        verify(datapathTypeEntryBuilder,times(2)).setDatapathType(DatapathTypeSystem.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSetVersion() throws Exception {
        OvsdbNodeAugmentationBuilder ovsdbNodeBuilder = mock(OvsdbNodeAugmentationBuilder.class);
        OpenVSwitch openVSwitch = mock(OpenVSwitch.class);
        Column<GenericTableSchema, Set<String>> column = mock(Column.class);
        when(openVSwitch.getOvsVersionColumn()).thenReturn(column);
        Set<String> set = new HashSet<>();
        set.add("v2.3.0");
        when(column.getData()).thenReturn(set);
        when(ovsdbNodeBuilder.setOvsVersion(anyString())).thenReturn(ovsdbNodeBuilder);

        Whitebox.invokeMethod(openVSwitchUpdateCommand, "setVersion", ovsdbNodeBuilder, openVSwitch);
        verify(ovsdbNodeBuilder).setOvsVersion(anyString());
        verify(openVSwitch).getOvsVersionColumn();


    }
}