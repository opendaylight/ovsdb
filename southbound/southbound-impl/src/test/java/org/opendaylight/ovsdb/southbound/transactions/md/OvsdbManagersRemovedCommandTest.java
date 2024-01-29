/*
 * Copyright (c) 2015 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.transactions.md;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
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
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.schema.openvswitch.Manager;
import org.opendaylight.ovsdb.schema.openvswitch.OpenVSwitch;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionInstance;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagerEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagerEntryKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberModifier;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@PrepareForTest(OvsdbManagersRemovedCommand.class)
@RunWith(PowerMockRunner.class)
public class OvsdbManagersRemovedCommandTest {
    private static final String TARGET_COLUMN_DATA = "Target Column Data";

    private OvsdbManagersRemovedCommand ovsdbManagersRemovedCommand;
    private Map<UUID, OpenVSwitch> updatedOpenVSwitchRows;
    private Map<UUID, OpenVSwitch> oldOpenVSwitchRows;
    private Map<UUID, Manager> updatedManagerRows;

    @Before
    public void setUp() {
        ovsdbManagersRemovedCommand = PowerMockito.mock(OvsdbManagersRemovedCommand.class, Mockito.CALLS_REAL_METHODS);
    }

    @Test
    public void testOvsdbManagersRemovedCommand() {
        OvsdbConnectionInstance key = mock(OvsdbConnectionInstance.class);
        TableUpdates updates = mock(TableUpdates.class);
        DatabaseSchema dbSchema = mock(DatabaseSchema.class);
        OvsdbManagersRemovedCommand ovsdbManagersRemovedCommand1 = new OvsdbManagersRemovedCommand(key, updates,
                dbSchema);
        assertEquals(key, Whitebox.getInternalState(ovsdbManagersRemovedCommand1, "key"));
        assertEquals(updates, Whitebox.getInternalState(ovsdbManagersRemovedCommand1, "updates"));
        assertEquals(dbSchema, Whitebox.getInternalState(ovsdbManagersRemovedCommand1, "dbSchema"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecute() throws Exception {
        updatedOpenVSwitchRows = new HashMap<>();
        UUID uuid = mock(UUID.class);
        OpenVSwitch openVSwitch = mock(OpenVSwitch.class);
        updatedOpenVSwitchRows.put(uuid, openVSwitch);
        MemberModifier.field(OvsdbManagersRemovedCommand.class, "updatedOpenVSwitchRows")
                .set(ovsdbManagersRemovedCommand, updatedOpenVSwitchRows);
        OvsdbConnectionInstance ovsdbConnectionInstance = mock(OvsdbConnectionInstance.class);
        when(ovsdbManagersRemovedCommand.getOvsdbConnectionInstance()).thenReturn(ovsdbConnectionInstance);
        when(ovsdbConnectionInstance.getNodeId()).thenReturn(mock(NodeId.class));

        doNothing().when(ovsdbManagersRemovedCommand).deleteManagers(any(ReadWriteTransaction.class), any(List.class));
        doReturn(mock(List.class)).when(ovsdbManagersRemovedCommand).managerEntriesToRemove(
            any(InstanceIdentifier.class), any(OpenVSwitch.class));

        ReadWriteTransaction transaction = mock(ReadWriteTransaction.class);
        ovsdbManagersRemovedCommand.execute(transaction);
        verify(ovsdbManagersRemovedCommand).deleteManagers(any(ReadWriteTransaction.class), any(List.class));
        verify(ovsdbManagersRemovedCommand).managerEntriesToRemove(any(InstanceIdentifier.class),
            any(OpenVSwitch.class));
    }

    @Test
    public void testDeleteManagers() throws Exception {
        ReadWriteTransaction transaction = mock(ReadWriteTransaction.class);
        List<InstanceIdentifier<ManagerEntry>> managerEntryIids = new ArrayList<>();
        managerEntryIids.add(SouthboundMapper.createInstanceIdentifier(new NodeId("test"))
            .augmentation(OvsdbNodeAugmentation.class)
            .child(ManagerEntry.class, new ManagerEntryKey(new Uri("testUri"))));
        doNothing().when(transaction).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        ovsdbManagersRemovedCommand.deleteManagers(transaction, managerEntryIids);
        verify(transaction).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
    }

    @Test
    public void testManagerEntriesToRemove() throws Exception {
        OpenVSwitch openVSwitch = mock(OpenVSwitch.class);

        UUID uuid = mock(UUID.class);
        OpenVSwitch oldOvsdbNode = mock(OpenVSwitch.class);
        oldOpenVSwitchRows = new HashMap<>();
        oldOpenVSwitchRows.put(uuid, oldOvsdbNode);
        when(openVSwitch.getUuid()).thenReturn(uuid);
        MemberModifier.field(OvsdbManagersRemovedCommand.class, "oldOpenVSwitchRows").set(ovsdbManagersRemovedCommand,
                oldOpenVSwitchRows);
        @SuppressWarnings("unchecked")
        Column<GenericTableSchema, Set<UUID>> column = mock(Column.class);
        Set<UUID> set = new HashSet<>();
        UUID controllerUuid = mock(UUID.class);
        set.add(controllerUuid);
        when(column.getData()).thenReturn(set);
        when(oldOvsdbNode.getManagerOptionsColumn()).thenReturn(column);
        when(column.getData()).thenReturn(set);
        when(openVSwitch.getManagerOptionsColumn()).thenReturn(column);
        InstanceIdentifier<Node> bridgeIid = SouthboundMapper.createInstanceIdentifier(new NodeId("test"));

        List<InstanceIdentifier<ManagerEntry>> resultManagerEntries =
            ovsdbManagersRemovedCommand.managerEntriesToRemove(bridgeIid, openVSwitch);
        assertEquals(ArrayList.class, resultManagerEntries.getClass());
        verify(oldOvsdbNode, times(2)).getManagerOptionsColumn();
    }

    @Test
    public void testCheckIfManagerPresentInUpdatedManagersList() throws Exception {
        Manager updatedManager = mock(Manager.class);
        updatedManagerRows = new HashMap<>();
        UUID uuid = mock(UUID.class);
        updatedManagerRows.put(uuid, updatedManager);
        MemberModifier.field(OvsdbManagersRemovedCommand.class, "updatedManagerRows").set(ovsdbManagersRemovedCommand,
                updatedManagerRows);
        @SuppressWarnings("unchecked")
        Column<GenericTableSchema, String> column = mock(Column.class);
        Manager removedManager = mock(Manager.class);
        when(removedManager.getTargetColumn()).thenReturn(column);
        when(updatedManager.getTargetColumn()).thenReturn(column);
        when(column.getData()).thenReturn(TARGET_COLUMN_DATA);

        assertTrue(ovsdbManagersRemovedCommand.checkIfManagerPresentInUpdatedManagersList(removedManager));
    }
}
