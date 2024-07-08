/*
 * Copyright © 2015, 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.Insert;
import org.opendaylight.ovsdb.lib.operations.Mutate;
import org.opendaylight.ovsdb.lib.operations.Operation;
import org.opendaylight.ovsdb.lib.operations.Operations;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.ColumnSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.OpenVSwitch;
import org.opendaylight.ovsdb.southbound.InstanceIdentifierCodec;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({TransactUtils.class, Operations.class})
public class OpenVSwitchBridgeAddCommandTest {
    private OpenVSwitchBridgeAddCommand ovsBridgeAddCommand;
    private Operations op;

    @Before
    public void setUp() {
        op = mock(Operations.class);
        ovsBridgeAddCommand = spy(new OpenVSwitchBridgeAddCommand(op));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testExecute() throws Exception {
        TransactionBuilder transaction = mock(TransactionBuilder.class);
        List<Operation> operations = new ArrayList<>();
        when(transaction.getOperations()).thenReturn(operations);

        Bridge bridge = mock(Bridge.class);

        when(transaction.getTypedRowWrapper(eq(Bridge.class))).thenReturn(bridge);

        List<Insert> inserts = new ArrayList<>();
        Insert insert = mock(Insert.class);
        inserts.add(insert);
        PowerMockito.mockStatic(TransactUtils.class);
        when(bridge.getSchema()).thenReturn(mock(GenericTableSchema.class));
        PowerMockito.when(TransactUtils.extractInsert(any(TransactionBuilder.class), any(GenericTableSchema.class)))
                .thenReturn(inserts);

        OpenVSwitch ovs = mock(OpenVSwitch.class);
        when(transaction.getTypedRowWrapper(eq(OpenVSwitch.class))).thenReturn(ovs);
        PowerMockito.when(TransactUtils.extractNamedUuid(any(Insert.class))).thenReturn(mock(UUID.class));
        doNothing().when(ovs).setBridges(any(Set.class));

        Mutate<GenericTableSchema> mutate = mock(Mutate.class);
        when(op.mutate(any(OpenVSwitch.class))).thenReturn(mutate);
        Column<GenericTableSchema, Set<UUID>> column = mock(Column.class);
        when(ovs.getBridgesColumn()).thenReturn(column);
        when(column.getSchema()).thenReturn(mock(ColumnSchema.class));
        when(column.getData()).thenReturn(new HashSet<>());
        when(mutate.addMutation(any(ColumnSchema.class), any(Mutator.class), any(Set.class))).thenReturn(mutate);
        when(transaction.add(any(Operation.class))).thenReturn(transaction);

        ovsBridgeAddCommand.execute(transaction, mock(BridgeOperationalState.class), mock(DataChangeEvent.class),
                mock(InstanceIdentifierCodec.class));
        verify(transaction).add(any(Operation.class));
    }
}
