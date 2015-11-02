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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.schema.openvswitch.Manager;
import org.opendaylight.ovsdb.schema.openvswitch.OpenVSwitch;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionInstance;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.ovsdb.southbound.SouthboundUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagerEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagerEntryKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberModifier;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import com.google.common.base.Optional;

@PrepareForTest({OvsdbManagersUpdateCommand.class, SouthboundMapper.class, SouthboundUtil.class, InstanceIdentifier.class})
@RunWith(PowerMockRunner.class)
public class OvsdbManagersUpdateCommandTest {

    private static final String TARGET_COLUMN_DATA = "Manager Column";
    private static final String NODE_ID = "Node ID String";
    private Map<UUID, Manager> updatedManagerRows;
    private Map<UUID, OpenVSwitch> updatedOpenVSwitchRows;
    private OvsdbManagersUpdateCommand ovsdbManagersUpdateCommand;

    @Before
    public void setUp() {
        ovsdbManagersUpdateCommand = PowerMockito.mock(OvsdbManagersUpdateCommand.class, Mockito.CALLS_REAL_METHODS);
    }

    @Test
    public void testOvsdbManagersUpdateCommand() {
        OvsdbConnectionInstance key = mock(OvsdbConnectionInstance.class);
        TableUpdates updates = mock(TableUpdates.class);
        DatabaseSchema dbSchema = mock(DatabaseSchema.class);
        OvsdbManagersUpdateCommand ovsdbManagersUpdateCommand1 = new OvsdbManagersUpdateCommand(key, updates, dbSchema);
        assertEquals(key, Whitebox.getInternalState(ovsdbManagersUpdateCommand1, "key"));
        assertEquals(updates, Whitebox.getInternalState(ovsdbManagersUpdateCommand1, "updates"));
        assertEquals(dbSchema, Whitebox.getInternalState(ovsdbManagersUpdateCommand1, "dbSchema"));
    }

    @Test
    public void testExecute() throws Exception {
        ReadWriteTransaction transaction= mock(ReadWriteTransaction.class);
        updatedManagerRows = new HashMap<>();
        updatedManagerRows.put(mock(UUID.class), mock(Manager.class));
        MemberModifier.field(OvsdbManagersUpdateCommand.class, "updatedManagerRows").set(ovsdbManagersUpdateCommand, updatedManagerRows);
        Map<Uri, Manager> updatedManagerRowsWithUri = new HashMap<>();
        PowerMockito.doReturn(updatedManagerRowsWithUri).when(ovsdbManagersUpdateCommand, "getUriManagerMap", any(Map.class));

        updatedOpenVSwitchRows = new HashMap<>();
        updatedOpenVSwitchRows.put(mock(UUID.class), mock(OpenVSwitch.class));
        MemberModifier.field(OvsdbManagersUpdateCommand.class, "updatedOpenVSwitchRows").set(ovsdbManagersUpdateCommand, updatedOpenVSwitchRows);

        //mock updateManagers()
        PowerMockito.doNothing().when(ovsdbManagersUpdateCommand, "updateManagers", any(ReadWriteTransaction.class), any(Map.class), any(Map.class));

        ovsdbManagersUpdateCommand.execute(transaction);
        PowerMockito.verifyPrivate(ovsdbManagersUpdateCommand).invoke("getUriManagerMap", any(Map.class));
        PowerMockito.verifyPrivate(ovsdbManagersUpdateCommand).invoke("updateManagers", any(ReadWriteTransaction.class), any(Map.class), any(Map.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpdateManagers() throws Exception {
        ReadWriteTransaction transaction = mock(ReadWriteTransaction.class);
        Map<UUID, Manager> updatedManagerRows = new HashMap<>();
        Map<UUID, OpenVSwitch> updatedOpenVSwitchRows = new HashMap<>();
        OpenVSwitch openVSwitch = mock(OpenVSwitch.class);
        updatedOpenVSwitchRows.put(mock(UUID.class), openVSwitch);

        PowerMockito.mockStatic(SouthboundMapper.class);
        List<ManagerEntry> managerEntries = new ArrayList<>();
        managerEntries.add(mock(ManagerEntry.class));
        when(SouthboundMapper.createManagerEntries(any(OpenVSwitch.class), any(Map.class))).thenReturn(managerEntries);

        //mock getManagerEntryIid()
        PowerMockito.doReturn(mock(InstanceIdentifier.class)).when(ovsdbManagersUpdateCommand, "getManagerEntryIid", any(ManagerEntry.class));
        doNothing().when(transaction).merge(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(ManagerEntry.class));
        Whitebox.invokeMethod(ovsdbManagersUpdateCommand, "updateManagers", transaction, updatedManagerRows, updatedOpenVSwitchRows);
        verify(transaction).merge(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(ManagerEntry.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpdateManagers1() throws Exception {
        ReadWriteTransaction transaction = mock(ReadWriteTransaction.class);
        Map<Uri, Manager> updatedManagerRows = new HashMap<>();
        OvsdbConnectionInstance ovsdbConnectionInstance = mock(OvsdbConnectionInstance.class);
        when(ovsdbManagersUpdateCommand.getOvsdbConnectionInstance()).thenReturn(ovsdbConnectionInstance);
        InstanceIdentifier<Node> connectionIId = mock(InstanceIdentifier.class);
        when(ovsdbConnectionInstance.getInstanceIdentifier()).thenReturn(connectionIId);

        Optional<Node> ovsdbNode = mock(Optional.class);
        PowerMockito.mockStatic(SouthboundUtil.class);
        when(SouthboundUtil.readNode(any(ReadWriteTransaction.class), any(InstanceIdentifier.class))).thenReturn(ovsdbNode);
        when(ovsdbNode.isPresent()).thenReturn(true);
        when(ovsdbNode.get()).thenReturn(mock(Node.class));
        Whitebox.invokeMethod(ovsdbManagersUpdateCommand, "updateManagers", transaction, updatedManagerRows);
        verify(ovsdbNode, times(2)).get();
    }

    @Test
    public void testGetManagerEntryIid() throws Exception {
        ManagerEntry managerEntry = mock(ManagerEntry.class);
        OvsdbConnectionInstance client = mock(OvsdbConnectionInstance.class, Mockito.RETURNS_DEEP_STUBS);
        when(ovsdbManagersUpdateCommand.getOvsdbConnectionInstance()).thenReturn(client);
        when(client.getNodeKey().getNodeId().getValue()).thenReturn(NODE_ID);
        PowerMockito.whenNew(Uri.class).withAnyArguments().thenReturn(mock(Uri.class));

        NodeId nodeId = mock(NodeId.class);
        PowerMockito.whenNew(NodeId.class).withAnyArguments().thenReturn(nodeId);
        NodeKey nodeKey = mock(NodeKey.class);
        PowerMockito.whenNew(NodeKey.class).withAnyArguments().thenReturn(nodeKey);
        when(managerEntry.getKey()).thenReturn(mock(ManagerEntryKey.class));
        assertEquals(KeyedInstanceIdentifier.class, Whitebox.invokeMethod(ovsdbManagersUpdateCommand, "getManagerEntryIid", managerEntry).getClass());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetUriManagerMap() throws Exception {
        Map<UUID,Manager> uuidManagerMap = new HashMap<>();
        Manager manager = mock(Manager.class);
        uuidManagerMap.put(mock(UUID.class), manager);
        Map<Uri, Manager> testUriManagerMap = new HashMap<>();

        Column<GenericTableSchema, String> column = mock(Column.class);
        when(manager.getTargetColumn()).thenReturn(column);
        when(column.getData()).thenReturn(TARGET_COLUMN_DATA);

        Uri uri = mock(Uri.class);
        PowerMockito.whenNew(Uri.class).withAnyArguments().thenReturn(uri);
        testUriManagerMap.put(uri, manager);
        assertEquals(testUriManagerMap, Whitebox.invokeMethod(ovsdbManagersUpdateCommand, "getUriManagerMap", uuidManagerMap));
    }
}
