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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionInstance;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.BridgeOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntry;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberMatcher;
import org.powermock.api.support.membermodification.MemberModifier;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@PrepareForTest({SouthboundMapper.class, OvsdbControllerRemovedCommand.class})
@RunWith(PowerMockRunner.class)
public class OvsdbControllerRemovedCommandTest {
    private Map<UUID, Bridge> oldBridgeRows;
    private Map<UUID, Bridge> updatedBridgeRows;
    private OvsdbControllerRemovedCommand ovsdbControllerRemovedCommand;
    @Before
    public void setUp() throws Exception {
        ovsdbControllerRemovedCommand = PowerMockito.mock(OvsdbControllerRemovedCommand.class, Mockito.CALLS_REAL_METHODS);
    }

    @Test
    public void testOvsdbControllerRemovedCommand() {
        OvsdbConnectionInstance key = mock(OvsdbConnectionInstance.class);
        TableUpdates updates = mock(TableUpdates.class);
        DatabaseSchema dbSchema = mock(DatabaseSchema.class);
        OvsdbControllerRemovedCommand ovsdbControllerRemovedCommand1 = new OvsdbControllerRemovedCommand(key, updates, dbSchema);
        assertEquals(key, Whitebox.getInternalState(ovsdbControllerRemovedCommand1, "key"));
        assertEquals(updates, Whitebox.getInternalState(ovsdbControllerRemovedCommand1, "updates"));
        assertEquals(dbSchema, Whitebox.getInternalState(ovsdbControllerRemovedCommand1, "dbSchema"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecute() throws Exception {
        ReadWriteTransaction transaction = mock(ReadWriteTransaction.class);
        updatedBridgeRows = new HashMap<>();
        UUID uuid = mock(UUID.class);
        Bridge bridge = mock(Bridge.class);
        updatedBridgeRows.put(uuid, bridge);
        MemberModifier.field(OvsdbControllerRemovedCommand.class, "updatedBridgeRows").set(ovsdbControllerRemovedCommand, updatedBridgeRows);
        PowerMockito.mockStatic(SouthboundMapper.class);
        when(SouthboundMapper.createInstanceIdentifier(any(OvsdbConnectionInstance.class), any(Bridge.class))).thenReturn(mock(InstanceIdentifier.class));
        MemberModifier.suppress(MemberMatcher.method(OvsdbControllerRemovedCommand.class, "deleteControllers", ReadWriteTransaction.class, List.class));
        MemberModifier.suppress(MemberMatcher.method(OvsdbControllerRemovedCommand.class, "controllerEntriesToRemove", InstanceIdentifier.class, Bridge.class));
        ovsdbControllerRemovedCommand.execute(transaction);
        PowerMockito.verifyPrivate(ovsdbControllerRemovedCommand).invoke("deleteControllers", any(ReadWriteTransaction.class), any(List.class));
        PowerMockito.verifyPrivate(ovsdbControllerRemovedCommand).invoke("controllerEntriesToRemove", any(ReadWriteTransaction.class), any(Bridge.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDeleteControllers() throws Exception {
        ReadWriteTransaction transaction = mock(ReadWriteTransaction.class);
        List<InstanceIdentifier<ControllerEntry>> controllerEntryIids = new ArrayList<>();

        doNothing().when(transaction).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        Whitebox.invokeMethod(ovsdbControllerRemovedCommand, "deleteControllers", transaction, controllerEntryIids);
        verify(transaction, times(0)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));

        InstanceIdentifier<ControllerEntry> controllerEntry = mock(InstanceIdentifier.class);
        controllerEntryIids.add(controllerEntry);
        Whitebox.invokeMethod(ovsdbControllerRemovedCommand, "deleteControllers", transaction, controllerEntryIids);
        verify(transaction).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testControllerEntriesToRemove() throws Exception {
        InstanceIdentifier<Node> bridgeIid = mock(InstanceIdentifier.class);
        Bridge bridge = mock(Bridge.class);

        UUID uuid = mock(UUID.class);
        Bridge oldBridgeNode = mock(Bridge.class);
        oldBridgeRows = new HashMap<>();
        oldBridgeRows.put(uuid, oldBridgeNode);
        when(bridge.getUuid()).thenReturn(uuid);
        MemberModifier.field(OvsdbControllerRemovedCommand.class, "oldBridgeRows").set(ovsdbControllerRemovedCommand, oldBridgeRows);
        Column<GenericTableSchema, Set<UUID>> column = mock(Column.class);
        Set<UUID> set = new HashSet<>();
        UUID controllerUuid = mock(UUID.class);
        set.add(controllerUuid);
        when(column.getData()).thenReturn(set);
        when(oldBridgeNode.getControllerColumn()).thenReturn(column);
        when(column.getData()).thenReturn(set);
        when(bridge.getControllerColumn()).thenReturn(column);
        List<InstanceIdentifier<BridgeOtherConfigs>> resultControllerEntries = Whitebox.invokeMethod(ovsdbControllerRemovedCommand, "controllerEntriesToRemove", bridgeIid, bridge);
        assertEquals(ArrayList.class, resultControllerEntries.getClass());
        verify(oldBridgeNode, times(2)).getControllerColumn();
    }
}
