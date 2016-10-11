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
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.HashSet;
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
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.Port;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionInstance;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberModifier;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@RunWith(PowerMockRunner.class)
@PrepareForTest({TyperUtils.class, SouthboundMapper.class, InstanceIdentifier.class})
public class OvsdbPortRemoveCommandTest {

    private static final String PORT_NAME = "port0";
    private OvsdbPortRemoveCommand ovsdbPortRemoveCommand;

    @Before
    public void setUp() throws Exception {
        ovsdbPortRemoveCommand = PowerMockito.mock(OvsdbPortRemoveCommand.class, Mockito.CALLS_REAL_METHODS);
    }

    @Test
    public void testOvsdbPortRemoveCommandTest() {
        OvsdbConnectionInstance key = mock(OvsdbConnectionInstance.class);
        TableUpdates updates = mock(TableUpdates.class);
        DatabaseSchema dbSchema = mock(DatabaseSchema.class);
        OvsdbPortRemoveCommand ovsdbPortRemoveCommand1 = new OvsdbPortRemoveCommand(key, updates, dbSchema);
        assertEquals(key, Whitebox.getInternalState(ovsdbPortRemoveCommand1, "key"));
        assertEquals(updates, Whitebox.getInternalState(ovsdbPortRemoveCommand1, "updates"));
        assertEquals(dbSchema, Whitebox.getInternalState(ovsdbPortRemoveCommand1, "dbSchema"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testExecute() throws Exception {
        when(ovsdbPortRemoveCommand.getUpdates()).thenReturn(mock(TableUpdates.class));
        when(ovsdbPortRemoveCommand.getDbSchema()).thenReturn(mock(DatabaseSchema.class));
        PowerMockito.mockStatic(TyperUtils.class);
        UUID uuid = mock(UUID.class);
        Map<UUID, Port> portRemovedRows = new HashMap<>();
        Port port = mock(Port.class);
        portRemovedRows.put(uuid, port);
        when(TyperUtils.extractRowsRemoved(eq(Port.class), any(TableUpdates.class), any(DatabaseSchema.class)))
                .thenReturn(portRemovedRows);
        Map<UUID, Bridge> bridgeUpdatedRows = new HashMap<>();
        Bridge updatedBridgeData = mock(Bridge.class);
        bridgeUpdatedRows.put(uuid, updatedBridgeData);
        when(TyperUtils.extractRowsUpdated(eq(Bridge.class), any(TableUpdates.class), any(DatabaseSchema.class)))
                .thenReturn(bridgeUpdatedRows);
        Map<UUID, Bridge> bridgeUpdatedOldRows = new HashMap<>();
        Bridge oldBridgeData = mock(Bridge.class);
        bridgeUpdatedOldRows.put(uuid, oldBridgeData);
        when(TyperUtils.extractRowsOld(eq(Bridge.class), any(TableUpdates.class), any(DatabaseSchema.class)))
                .thenReturn(bridgeUpdatedOldRows);

        Column<GenericTableSchema, Set<UUID>> column = mock(Column.class);
        when(oldBridgeData.getPortsColumn()).thenReturn(column);
        Set<UUID> uuids = new HashSet<>();
        uuids.add(uuid);
        when(column.getData()).thenReturn(uuids);

        Column<GenericTableSchema, UUID> uuidColumn = mock(Column.class);
        when(port.getUuidColumn()).thenReturn(uuidColumn);
        when(uuidColumn.getData()).thenReturn(uuid);

        when(port.getName()).thenReturn(PORT_NAME);
        PowerMockito.mockStatic(SouthboundMapper.class);
        InstanceIdentifier<Node> nodeIID = mock(InstanceIdentifier.class);
        when(ovsdbPortRemoveCommand.getOvsdbConnectionInstance()).thenReturn(mock(OvsdbConnectionInstance.class));
        when(SouthboundMapper.createInstanceIdentifier(any(OvsdbConnectionInstance.class), any(Bridge.class)))
                .thenReturn(nodeIID);
        MemberModifier.suppress(MemberModifier.methodsDeclaredIn(InstanceIdentifier.class));
        ReadWriteTransaction transaction = mock(ReadWriteTransaction.class);
        doNothing().when(transaction).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));

        ovsdbPortRemoveCommand.execute(transaction);
        verify(transaction).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
    }
}
