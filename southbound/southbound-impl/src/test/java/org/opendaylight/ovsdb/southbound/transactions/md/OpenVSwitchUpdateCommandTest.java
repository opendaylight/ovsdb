/*
 * Copyright © 2015, 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.transactions.md;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.OpenVSwitch;
import org.opendaylight.ovsdb.southbound.InstanceIdentifierCodec;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionInstance;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.ovsdb.southbound.SouthboundUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathTypeNetdev;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathTypeSystem;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.DatapathTypeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.DatapathTypeEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.InterfaceTypeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.InterfaceTypeEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchExternalIdsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchOtherConfigsKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberMatcher;
import org.powermock.api.support.membermodification.MemberModifier;
import org.powermock.reflect.Whitebox;

@RunWith(MockitoJUnitRunner.class)
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
    // TODO This test needs to be re-done
    @Ignore("Broken mock-based test")
    public void testExecute() throws Exception {
        PowerMockito.mockStatic(TyperUtils.class);
        UUID uuid = mock(UUID.class);
        OpenVSwitch ovs = mock(OpenVSwitch.class);
        when(TyperUtils.extractRowsUpdated(eq(OpenVSwitch.class), any(TableUpdates.class), any(DatabaseSchema.class)))
                .thenReturn(ImmutableMap.of(uuid, ovs));
        OpenVSwitch ovs1 = mock(OpenVSwitch.class);
        when(TyperUtils.extractRowsOld(eq(OpenVSwitch.class), any(TableUpdates.class), any(DatabaseSchema.class)))
                .thenReturn(ImmutableMap.of(uuid, ovs1));

        //mock getUpdates() and getDbSchema()
        when(openVSwitchUpdateCommand.getUpdates()).thenReturn(updates);
        when(openVSwitchUpdateCommand.getDbSchema()).thenReturn(dbSchema);

        //Test getInstanceIdentifier(): case 1:
        // ovs.getExternalIdsColumn().getData().containsKey(SouthboundConstants.IID_EXTERNAL_ID_KEY)) == true
        Column<GenericTableSchema, Map<String, String>> column = mock(Column.class);
        when(ovs.getExternalIdsColumn()).thenReturn(column);
        when(column.getData()).thenReturn(ImmutableMap.of(SouthboundConstants.IID_EXTERNAL_ID_KEY, "iidString"));
        PowerMockito.mockStatic(SouthboundUtil.class);
        MemberModifier.suppress(MemberMatcher.method(OpenVSwitchUpdateCommand.class, "getOvsdbConnectionInstance"));
        InstanceIdentifier<Node> iid = mock(InstanceIdentifier.class);
//        when((InstanceIdentifier<Node>) SouthboundUtil.deserializeInstanceIdentifier(
//                any(InstanceIdentifierCodec.class), anyString())).thenReturn(iid);
        OvsdbConnectionInstance ovsdbConnectionInstance = mock(OvsdbConnectionInstance.class);
        when(ovsdbConnectionInstance.getInstanceIdentifier()).thenReturn(iid);
        when(openVSwitchUpdateCommand.getOvsdbConnectionInstance()).thenReturn(ovsdbConnectionInstance);
        doNothing().when(ovsdbConnectionInstance).setInstanceIdentifier(any(InstanceIdentifier.class));

        OvsdbNodeAugmentationBuilder ovsdbNodeBuilder = mock(OvsdbNodeAugmentationBuilder.class);
        PowerMockito.whenNew(OvsdbNodeAugmentationBuilder.class).withNoArguments().thenReturn(ovsdbNodeBuilder);

        //suppress the setter methods of the class
        MemberModifier.suppress(MemberMatcher.method(OpenVSwitchUpdateCommand.class, "setOvsVersion",
                OvsdbNodeAugmentationBuilder.class, OpenVSwitch.class));
        MemberModifier.suppress(MemberMatcher.method(OpenVSwitchUpdateCommand.class, "setDbVersion",
                OvsdbNodeAugmentationBuilder.class, OpenVSwitch.class));
        MemberModifier.suppress(MemberMatcher.method(OpenVSwitchUpdateCommand.class, "setDataPathTypes",
                OvsdbNodeAugmentationBuilder.class, OpenVSwitch.class));
        MemberModifier.suppress(MemberMatcher.method(OpenVSwitchUpdateCommand.class, "setInterfaceTypes",
                OvsdbNodeAugmentationBuilder.class, OpenVSwitch.class));
        MemberModifier.suppress(MemberMatcher.method(OpenVSwitchUpdateCommand.class, "setExternalIds",
                ReadWriteTransaction.class, OvsdbNodeAugmentationBuilder.class, OpenVSwitch.class, OpenVSwitch.class));
        MemberModifier.suppress(MemberMatcher.method(OpenVSwitchUpdateCommand.class, "setOtherConfig",
                InstanceIdentifierCodec.class, ReadWriteTransaction.class, OvsdbNodeAugmentationBuilder.class,
                OpenVSwitch.class, OpenVSwitch.class));
        MemberModifier.suppress(MemberMatcher.method(OpenVSwitchUpdateCommand.class, "getConnectionInfo"));
        when(openVSwitchUpdateCommand.getConnectionInfo()).thenReturn(mock(ConnectionInfo.class));
        when(ovsdbNodeBuilder.setConnectionInfo(any(ConnectionInfo.class))).thenReturn(ovsdbNodeBuilder);

        NodeBuilder nodeBuilder = mock(NodeBuilder.class);
        PowerMockito.whenNew(NodeBuilder.class).withNoArguments().thenReturn(nodeBuilder);

        //suppress getNodeId()
        MemberModifier.suppress(
                MemberMatcher.method(OpenVSwitchUpdateCommand.class, "getNodeId", InstanceIdentifierCodec.class,
                        OpenVSwitch.class));
        when(nodeBuilder.setNodeId(any(NodeId.class))).thenReturn(nodeBuilder);
        when(ovsdbNodeBuilder.build()).thenReturn(mock(OvsdbNodeAugmentation.class));
        when(nodeBuilder.addAugmentation(any(OvsdbNodeAugmentation.class))).thenReturn(nodeBuilder);
        when(nodeBuilder.build()).thenReturn(mock(Node.class));
        ReadWriteTransaction transaction = mock(ReadWriteTransaction.class);
        doNothing().when(transaction).merge(any(LogicalDatastoreType.class), any(InstanceIdentifier.class),
                any(Node.class));

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

    @Test
    @SuppressWarnings("unchecked")
    public void testSetOtherConfig() throws Exception {
        OpenVSwitch openVSwitch = mock(OpenVSwitch.class);

        Column<GenericTableSchema, Map<String, String>> column = mock(Column.class);
        when(openVSwitch.getOtherConfigColumn()).thenReturn(column);
        when(column.getData()).thenReturn(ImmutableMap.of("foo", "bar"));

        OpenVSwitch oldEntry = mock(OpenVSwitch.class);
        when(oldEntry.getOtherConfigColumn()).thenReturn(column);
        doNothing().when(openVSwitchUpdateCommand).removeOldConfigs(any(ReadWriteTransaction.class), any(Map.class),
            any(OpenVSwitch.class));
        doNothing().when(openVSwitchUpdateCommand).setNewOtherConfigs(any(OvsdbNodeAugmentationBuilder.class),
            any(Map.class));

        ReadWriteTransaction transaction = mock(ReadWriteTransaction.class);
        OvsdbNodeAugmentationBuilder ovsdbNodeBuilder = mock(OvsdbNodeAugmentationBuilder.class);
        Whitebox.invokeMethod(openVSwitchUpdateCommand, "setOtherConfig",
                transaction, ovsdbNodeBuilder, oldEntry, openVSwitch);
        verify(openVSwitch, times(2)).getOtherConfigColumn();
        verify(oldEntry, times(2)).getOtherConfigColumn();
        verify(openVSwitchUpdateCommand).removeOldConfigs(any(ReadWriteTransaction.class), any(Map.class),
            any(OpenVSwitch.class));
    }

    @Test
    public void testRemoveOldConfigs() throws Exception {
        ReadWriteTransaction transaction = mock(ReadWriteTransaction.class);
        doNothing().when(transaction).delete(any(LogicalDatastoreType.class), any(KeyedInstanceIdentifier.class));

        doReturn(new NodeId("foo")).when(openVSwitchUpdateCommand).getNodeId(any());
        OpenVSwitch ovs = mock(OpenVSwitch.class);
        Whitebox.invokeMethod(openVSwitchUpdateCommand, "removeOldConfigs",
                transaction, ImmutableMap.of("OpenvswitchOtherConfigsKey", "OpenvswitchOtherConfigsValue"), ovs);
        verify(transaction).delete(any(LogicalDatastoreType.class), any(KeyedInstanceIdentifier.class));
    }

    @Test
    public void testSetNewOtherConfigs() throws Exception {
        OvsdbNodeAugmentationBuilder ovsdbNodeBuilder = new OvsdbNodeAugmentationBuilder();
        Whitebox.invokeMethod(openVSwitchUpdateCommand, "setNewOtherConfigs", ovsdbNodeBuilder,
            ImmutableMap.of("otherConfigKey", "otherConfigValue"));

        final Map<OpenvswitchOtherConfigsKey, OpenvswitchOtherConfigs> otherConfigsList =
                ovsdbNodeBuilder.getOpenvswitchOtherConfigs();
        assertEquals(1, otherConfigsList.size());
        final OpenvswitchOtherConfigs otherConfig = otherConfigsList.values().iterator().next();
        assertEquals("otherConfigKey", otherConfig.getOtherConfigKey());
        assertEquals("otherConfigValue", otherConfig.getOtherConfigValue());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSetExternalIds() throws Exception {
        OpenVSwitch oldEntry = mock(OpenVSwitch.class);
        OpenVSwitch openVSwitch = mock(OpenVSwitch.class);

        Column<GenericTableSchema, Map<String, String>> column = mock(Column.class);
        when(openVSwitch.getExternalIdsColumn()).thenReturn(column);
        when(column.getData()).thenReturn(ImmutableMap.of("foo", "bar"));
        when(oldEntry.getExternalIdsColumn()).thenReturn(column);
        doNothing().when(openVSwitchUpdateCommand).removeExternalIds(any(ReadWriteTransaction.class), any(Map.class),
                any(OpenVSwitch.class));
        doNothing().when(openVSwitchUpdateCommand).setNewExternalIds(
            any(OvsdbNodeAugmentationBuilder.class), any(Map.class));

        ReadWriteTransaction transaction = mock(ReadWriteTransaction.class);
        OvsdbNodeAugmentationBuilder ovsdbNodeBuilder = mock(OvsdbNodeAugmentationBuilder.class);
        Whitebox.invokeMethod(openVSwitchUpdateCommand, "setExternalIds",
                transaction, ovsdbNodeBuilder, oldEntry, openVSwitch);
        verify(openVSwitch, times(2)).getExternalIdsColumn();
        verify(oldEntry, times(2)).getExternalIdsColumn();
        verify(openVSwitchUpdateCommand).removeExternalIds(any(ReadWriteTransaction.class), any(Map.class),
                any(OpenVSwitch.class));
    }

    @Test
    public void testRemoveExternalIds() throws Exception {
        ReadWriteTransaction transaction = mock(ReadWriteTransaction.class);
        doNothing().when(transaction).delete(any(LogicalDatastoreType.class), any(KeyedInstanceIdentifier.class));

        //suppress getNodeId()
        OpenVSwitch ovs = mock(OpenVSwitch.class);
        doReturn(mock(NodeId.class)).when(openVSwitchUpdateCommand).getNodeId(any());
        Whitebox.invokeMethod(openVSwitchUpdateCommand, "removeExternalIds",
                transaction, ImmutableMap.of("OpenvswitchExternalIdKey", "OpenvswitchExternalIdValue"), ovs);
        verify(transaction).delete(any(LogicalDatastoreType.class), any(KeyedInstanceIdentifier.class));
    }

    @Test
    public void testSetNewExternalIds() throws Exception {
        OvsdbNodeAugmentationBuilder ovsdbNodeBuilder = new OvsdbNodeAugmentationBuilder();
        Whitebox.invokeMethod(openVSwitchUpdateCommand, "setNewExternalIds", ovsdbNodeBuilder,
            ImmutableMap.of("externalIdsKey", "externalIdsValue"));

        final Map<OpenvswitchExternalIdsKey, OpenvswitchExternalIds> externalIdsList =
                ovsdbNodeBuilder.getOpenvswitchExternalIds();
        assertEquals(1, externalIdsList.size());
        final OpenvswitchExternalIds externalId = externalIdsList.values().iterator().next();
        assertEquals("externalIdsKey", externalId.getExternalIdKey());
        assertEquals("externalIdsValue", externalId.getExternalIdValue());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSetInterfaceTypes() throws Exception {
        OpenVSwitch openVSwitch = mock(OpenVSwitch.class);

        Column<GenericTableSchema, Set<String>> column = mock(Column.class);
        when(openVSwitch.getIfaceTypesColumn()).thenReturn(column);
        when(column.getData()).thenReturn(ImmutableSet.of(
            "dpdk",
            "dpdkr",
            "dpdkvhostuser",
            "dpdkvhostuserclient",
            "geneve",
            "gre",
            "internal",
            "ipsec_gre",
            "lisp",
            "patch",
            "stt",
            "system",
            "tap",
            "vxlan"));
        OvsdbNodeAugmentationBuilder ovsdbNodeBuilder = new OvsdbNodeAugmentationBuilder();

        Whitebox.invokeMethod(openVSwitchUpdateCommand, "setInterfaceTypes", ovsdbNodeBuilder, openVSwitch);
        verify(openVSwitch).getIfaceTypesColumn();

        Map<InterfaceTypeEntryKey, InterfaceTypeEntry> interfaceTypeEntries = ovsdbNodeBuilder.getInterfaceTypeEntry();
        assertEquals(14, interfaceTypeEntries.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSetDataPathTypes() throws Exception {
        OpenVSwitch openVSwitch = mock(OpenVSwitch.class);
        Column<GenericTableSchema, Set<String>> column = mock(Column.class);
        when(openVSwitch.getDatapathTypesColumn()).thenReturn(column);
        when(column.getData()).thenReturn(ImmutableSet.of("netdev", "system"));

        OvsdbNodeAugmentationBuilder ovsdbNodeBuilder = new OvsdbNodeAugmentationBuilder();
        Whitebox.invokeMethod(openVSwitchUpdateCommand, "setDataPathTypes", ovsdbNodeBuilder, openVSwitch);

        verify(openVSwitch).getDatapathTypesColumn();
        Map<DatapathTypeEntryKey, DatapathTypeEntry> entries = ovsdbNodeBuilder.getDatapathTypeEntry();
        assertEquals(2, entries.size());
        assertTrue(entries.containsKey(new DatapathTypeEntryKey(DatapathTypeNetdev.VALUE)));
        assertTrue(entries.containsKey(new DatapathTypeEntryKey(DatapathTypeSystem.VALUE)));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSetOvsVersion() throws Exception {
        OpenVSwitch openVSwitch = mock(OpenVSwitch.class);
        Column<GenericTableSchema, Set<String>> column = mock(Column.class);
        when(openVSwitch.getOvsVersionColumn()).thenReturn(column);
        when(column.getData()).thenReturn(ImmutableSet.of("v2.3.0"));
        OvsdbNodeAugmentationBuilder ovsdbNodeBuilder = mock(OvsdbNodeAugmentationBuilder.class);
        when(ovsdbNodeBuilder.setOvsVersion(anyString())).thenReturn(ovsdbNodeBuilder);

        Whitebox.invokeMethod(openVSwitchUpdateCommand, "setOvsVersion", ovsdbNodeBuilder, openVSwitch);
        verify(ovsdbNodeBuilder).setOvsVersion(anyString());
        verify(openVSwitch).getOvsVersionColumn();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSetDbVersion() throws Exception {
        OpenVSwitch openVSwitch = mock(OpenVSwitch.class);
        Column<GenericTableSchema, Set<String>> column = mock(Column.class);
        when(openVSwitch.getDbVersionColumn()).thenReturn(column);
        when(column.getData()).thenReturn(ImmutableSet.of("7.6.1"));
        OvsdbNodeAugmentationBuilder ovsdbNodeBuilder = mock(OvsdbNodeAugmentationBuilder.class);

        Whitebox.invokeMethod(openVSwitchUpdateCommand, "setDbVersion", ovsdbNodeBuilder, openVSwitch);
        verify(ovsdbNodeBuilder).setDbVersion(anyString());
        verify(openVSwitch).getDbVersionColumn();
    }
}
