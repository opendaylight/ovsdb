/*
 * Copyright (c) 2015 Inocybe Technologies. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.Mutate;
import org.opendaylight.ovsdb.lib.operations.Operation;
import org.opendaylight.ovsdb.lib.operations.Operations;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.ColumnSchema;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;


import static org.mockito.Mockito.verify;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;

@RunWith(PowerMockRunner.class)
@PrepareForTest({TyperUtils.class})
public class ProtocolRemovedCommandTest {
    private ProtocolRemovedCommand protocolRemovedCommand;

    @Before
    public  void setUpBeforeClass() throws Exception {
        protocolRemovedCommand = mock(ProtocolRemovedCommand.class, Mockito.CALLS_REAL_METHODS);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecute() throws Exception{
        TransactionBuilder transaction = mock(TransactionBuilder.class);


        Bridge bridge = mock(Bridge.class);
        when(transaction.getDatabaseSchema()).thenReturn(mock(DatabaseSchema.class));
        PowerMockito.mockStatic(TyperUtils.class);
        PowerMockito.when(TyperUtils.getTypedRowWrapper(any(DatabaseSchema.class),eq(Bridge.class))).thenReturn(bridge);


        Mutate<GenericTableSchema> mutate = mock(Mutate.class);
        Operations op  =(Operations) setField("op");
        when(op.mutate(any(Bridge.class))).thenReturn(mutate);
        Column<GenericTableSchema, Set<UUID>> column = mock (Column.class);
        //when(bridge.getProtocolsColumn()).thenReturn(column);
        when(column.getSchema()).thenReturn(mock(ColumnSchema.class));
        when(column.getData()).thenReturn(new HashSet<UUID>());
        when(mutate.addMutation(any(ColumnSchema.class), any(Mutator.class),any(Set.class))).thenReturn(mutate);
        when(transaction.add(any(Operation.class))).thenReturn(transaction);

        protocolRemovedCommand.execute(transaction);
        verify(transaction).add(any(Operation.class));

   }

    private Object setField (String fieldName) throws Exception{
        Field field = Operations.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(field.get(Operations.class), mock(Operations.class));
        return field.get(Operations.class);
    }
}
