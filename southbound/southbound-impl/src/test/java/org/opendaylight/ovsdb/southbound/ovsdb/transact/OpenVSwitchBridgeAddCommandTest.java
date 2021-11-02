/*
 * Copyright © 2015, 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
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

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class OpenVSwitchBridgeAddCommandTest {
    private OpenVSwitchBridgeAddCommand ovsBridgeAddCommand;

    @Before
    public void setUp() {
        ovsBridgeAddCommand = spy(OpenVSwitchBridgeAddCommand.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testExecute() {
        Bridge bridge = mock(Bridge.class);
        when(bridge.getSchema()).thenReturn(mock(GenericTableSchema.class));
        TransactionBuilder transaction = mock(TransactionBuilder.class);
        when(transaction.getTypedRowWrapper(eq(Bridge.class))).thenReturn(bridge);

        try (var utils = mockStatic(TransactUtils.class)) {
            utils.when(() -> TransactUtils.extractInsert(any(TransactionBuilder.class), any(GenericTableSchema.class)))
                .thenReturn(List.of(mock(Insert.class)));

            OpenVSwitch ovs = mock(OpenVSwitch.class);
            when(transaction.getTypedRowWrapper(eq(OpenVSwitch.class))).thenReturn(ovs);

            utils.when(() -> TransactUtils.extractNamedUuid(any(Insert.class))).thenReturn(mock(UUID.class));
            doNothing().when(ovs).setBridges(anySet());

            Mutate<GenericTableSchema> mutate = mock(Mutate.class);
            Operations op = OvsdbNodeUpdateCommandTest.setOpField();
            when(op.mutate(any(OpenVSwitch.class))).thenReturn(mutate);
            Column<GenericTableSchema, Set<UUID>> column = mock(Column.class);
            when(ovs.getBridgesColumn()).thenReturn(column);
            when(column.getSchema()).thenReturn(mock(ColumnSchema.class));
            when(column.getData()).thenReturn(new HashSet<>());
            when(mutate.addMutation(any(ColumnSchema.class), any(Mutator.class), anySet())).thenReturn(mutate);
            when(transaction.add(any(Operation.class))).thenReturn(transaction);

            ovsBridgeAddCommand.execute(transaction, mock(BridgeOperationalState.class), mock(DataChangeEvent.class),
                mock(InstanceIdentifierCodec.class));
            verify(transaction).add(any(Operation.class));
        }
    }
}
