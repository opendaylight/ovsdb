/*
 * Copyright (c) 2015 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.notation.Condition;
import org.opendaylight.ovsdb.lib.operations.Operation;
import org.opendaylight.ovsdb.lib.operations.Operations;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.operations.Update;
import org.opendaylight.ovsdb.lib.operations.Where;
import org.opendaylight.ovsdb.lib.schema.ColumnSchema;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.Interface;
import org.opendaylight.ovsdb.schema.openvswitch.Port;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbPortInterfaceAttributes.VlanMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberMatcher;
import org.powermock.api.support.membermodification.MemberModifier;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@RunWith(PowerMockRunner.class)
@PrepareForTest({TerminationPointUpdateCommand.class, TransactUtils.class, TyperUtils.class, VlanMode.class, TerminationPointCreateCommand.class, InstanceIdentifier.class})
public class TerminationPointUpdateCommandTest {

    private static final String TERMINATION_POINT_NAME = "termination point name";
    private TerminationPointUpdateCommand terminationPointUpdateCommand;

    @Before
    public void setUp() {
        terminationPointUpdateCommand = mock(TerminationPointUpdateCommand.class, Mockito.CALLS_REAL_METHODS);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testTerminationPointUpdateCommand() {
        BridgeOperationalState state = mock(BridgeOperationalState.class);
        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes = mock(AsyncDataChangeEvent.class);
        TerminationPointUpdateCommand terminationPointUpdateCommand1 = new TerminationPointUpdateCommand(state, changes);
        assertEquals(state, Whitebox.getInternalState(terminationPointUpdateCommand1, "operationalState"));
        assertEquals(changes, Whitebox.getInternalState(terminationPointUpdateCommand1, "changes"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecute() {
        Map<InstanceIdentifier<OvsdbTerminationPointAugmentation>, OvsdbTerminationPointAugmentation> created = new HashMap<>();
        created.put(mock(InstanceIdentifier.class), mock(OvsdbTerminationPointAugmentation.class));
        PowerMockito.mockStatic(TransactUtils.class);
        MemberModifier.suppress(MemberMatcher.method(TerminationPointUpdateCommand.class, "getChanges"));
        when(terminationPointUpdateCommand.getChanges()).thenReturn(mock(AsyncDataChangeEvent.class));
        PowerMockito.when(TransactUtils.extractCreated(any(AsyncDataChangeEvent.class), eq(OvsdbTerminationPointAugmentation.class))).thenReturn(created);
        MemberModifier.suppress(MemberMatcher.method(TerminationPointUpdateCommand.class, "updateTerminationPoint",
                TransactionBuilder.class, InstanceIdentifier.class, OvsdbTerminationPointAugmentation.class));
        doNothing().when(terminationPointUpdateCommand)
                .updateTerminationPoint(any(TransactionBuilder.class), any(InstanceIdentifier.class), any(OvsdbTerminationPointAugmentation.class));

        Map<InstanceIdentifier<OvsdbTerminationPointAugmentation>, OvsdbTerminationPointAugmentation> updated = new HashMap<>();
        updated.put(mock(InstanceIdentifier.class), mock(OvsdbTerminationPointAugmentation.class));
        PowerMockito.when(TransactUtils.extractUpdated(any(AsyncDataChangeEvent.class), eq(OvsdbTerminationPointAugmentation.class))).thenReturn(updated);

        TransactionBuilder transactionBuilder = mock(TransactionBuilder.class);
        terminationPointUpdateCommand.execute(transactionBuilder);
        verify(terminationPointUpdateCommand, times(2)).
                updateTerminationPoint(any(TransactionBuilder.class), any(InstanceIdentifier.class), any(OvsdbTerminationPointAugmentation.class));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testUpdateTerminationPoint() throws Exception {
        TransactionBuilder transaction = mock(TransactionBuilder.class);
        InstanceIdentifier<OvsdbTerminationPointAugmentation> iid = mock(InstanceIdentifier.class);
        OvsdbTerminationPointAugmentation terminationPoint = mock(OvsdbTerminationPointAugmentation.class);
        when(terminationPoint.getName()).thenReturn(TERMINATION_POINT_NAME);

        // Test updateInterface()
        Interface ovsInterface = mock(Interface.class);
        when(transaction.getDatabaseSchema()).thenReturn(mock(DatabaseSchema.class));
        PowerMockito.mockStatic(TyperUtils.class);
        when(TyperUtils.getTypedRowWrapper(any(DatabaseSchema.class), eq(Interface.class))).thenReturn(ovsInterface);

        Interface extraInterface = mock(Interface.class);
        when(TyperUtils.getTypedRowWrapper(any(DatabaseSchema.class), eq(Interface.class))).thenReturn(extraInterface);
        doNothing().when(extraInterface).setName(anyString());

        Operations op = (Operations) setField("op");
        Update update = mock(Update.class);
        when(op.update(any(Interface.class))).thenReturn(update);

        Column<GenericTableSchema, String> column = mock(Column.class);
        when(extraInterface.getNameColumn()).thenReturn(column);
        ColumnSchema<GenericTableSchema, String> columnSchema = mock(ColumnSchema.class);
        when(column.getSchema()).thenReturn(columnSchema);
        when(columnSchema.opEqual(anyString())).thenReturn(mock(Condition.class));
        Where where = mock(Where.class);
        when(update.where(any(Condition.class))).thenReturn(where);
        when(where.build()).thenReturn(mock(Operation.class));
        when(transaction.add(any(Operation.class))).thenReturn(transaction);
        PowerMockito.suppress(MemberMatcher.methodsDeclaredIn(TerminationPointCreateCommand.class));
        PowerMockito.suppress(MemberMatcher.methodsDeclaredIn(InstanceIdentifier.class));

        // Test updatePort()
        Port port = mock(Port.class);
        when(TyperUtils.getTypedRowWrapper(any(DatabaseSchema.class), eq(Port.class))).thenReturn(port);
        Port extraPort = mock(Port.class);
        when(TyperUtils.getTypedRowWrapper(any(DatabaseSchema.class), eq(Port.class))).thenReturn(extraPort);
        doNothing().when(extraPort).setName(anyString());
        when(op.update(any(Port.class))).thenReturn(update);
        when(extraPort.getNameColumn()).thenReturn(column);

        terminationPointUpdateCommand.updateTerminationPoint(transaction, iid, terminationPoint);
        verify(transaction, times(2)).add(any(Operation.class));
    }

    private Object setField(String fieldName) throws Exception {
        Field field = Operations.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(field.get(Operations.class), mock(Operations.class));
        return field.get(Operations.class);
    }
}
