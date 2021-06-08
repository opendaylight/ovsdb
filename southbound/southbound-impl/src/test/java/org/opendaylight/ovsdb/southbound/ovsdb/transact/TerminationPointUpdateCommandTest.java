/*
 * Copyright (c) 2015 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.notation.Condition;
import org.opendaylight.ovsdb.lib.operations.Operation;
import org.opendaylight.ovsdb.lib.operations.Operations;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.operations.Update;
import org.opendaylight.ovsdb.lib.operations.Where;
import org.opendaylight.ovsdb.lib.schema.ColumnSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.schema.openvswitch.Interface;
import org.opendaylight.ovsdb.schema.openvswitch.Port;
import org.opendaylight.ovsdb.southbound.InstanceIdentifierCodec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbPortInterfaceAttributes.VlanMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberMatcher;
import org.powermock.api.support.membermodification.MemberModifier;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
    TerminationPointUpdateCommand.class, TransactUtils.class, VlanMode.class, TerminationPointCreateCommand.class,
    InstanceIdentifier.class, Operations.class
})
public class TerminationPointUpdateCommandTest {

    private static final String TERMINATION_POINT_NAME = "termination point name";
    private TerminationPointUpdateCommand terminationPointUpdateCommand;

    @Before
    public void setUp() {
        terminationPointUpdateCommand = mock(TerminationPointUpdateCommand.class, Mockito.CALLS_REAL_METHODS);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecute() {
        Map<InstanceIdentifier<OvsdbTerminationPointAugmentation>, OvsdbTerminationPointAugmentation> created
            = new HashMap<>();
        created.put(mock(InstanceIdentifier.class), mock(OvsdbTerminationPointAugmentation.class));
        PowerMockito.mockStatic(TransactUtils.class);
        PowerMockito.when(TransactUtils.extractCreated(any(DataChangeEvent.class),
                eq(OvsdbTerminationPointAugmentation.class))).thenReturn(created);
        MemberModifier.suppress(MemberMatcher.method(TerminationPointUpdateCommand.class, "updateTerminationPoint",
                TransactionBuilder.class, BridgeOperationalState.class,
                InstanceIdentifier.class, OvsdbTerminationPointAugmentation.class, InstanceIdentifierCodec.class));
        doNothing().when(terminationPointUpdateCommand).updateTerminationPoint(any(TransactionBuilder.class),
                any(BridgeOperationalState.class), any(InstanceIdentifier.class),
                any(OvsdbTerminationPointAugmentation.class), any(InstanceIdentifierCodec.class));

        Map<InstanceIdentifier<OvsdbTerminationPointAugmentation>, OvsdbTerminationPointAugmentation> updated
            = new HashMap<>();
        updated.put(mock(InstanceIdentifier.class), mock(OvsdbTerminationPointAugmentation.class));
        PowerMockito.when(TransactUtils.extractUpdated(any(DataChangeEvent.class),
                eq(OvsdbTerminationPointAugmentation.class))).thenReturn(updated);

        TransactionBuilder transactionBuilder = mock(TransactionBuilder.class);
        terminationPointUpdateCommand.execute(transactionBuilder, mock(BridgeOperationalState.class),
                mock(DataChangeEvent.class), mock(InstanceIdentifierCodec.class));
        // TODO Verify something useful
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testUpdateTerminationPoint() throws Exception {
        TransactionBuilder transaction = mock(TransactionBuilder.class);
        BridgeOperationalState state = mock(BridgeOperationalState.class);
        OvsdbTerminationPointAugmentation terminationPoint = mock(OvsdbTerminationPointAugmentation.class);
        when(terminationPoint.getName()).thenReturn(TERMINATION_POINT_NAME);
        Node node = mock(Node.class);
        when(node.augmentation(OvsdbBridgeAugmentation.class)).thenReturn(mock(OvsdbBridgeAugmentation.class));
        Optional<Node> optNode = Optional.of(node);
        when(state.getBridgeNode(any(InstanceIdentifier.class))).thenReturn(optNode);

        // Test updateInterface()
        Interface ovsInterface = mock(Interface.class);
        when(transaction.getTypedRowWrapper(eq(Interface.class))).thenReturn(ovsInterface);

        Interface extraInterface = mock(Interface.class);
        when(transaction.getTypedRowWrapper(eq(Interface.class))).thenReturn(extraInterface);
        doNothing().when(extraInterface).setName(anyString());

        Operations op = OvsdbNodeUpdateCommandTest.setOpField();
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
        when(transaction.getTypedRowWrapper(eq(Port.class))).thenReturn(port);
        Port extraPort = mock(Port.class);
        when(transaction.getTypedRowWrapper(eq(Port.class))).thenReturn(extraPort);
        doNothing().when(extraPort).setName(anyString());
        when(op.update(any(Port.class))).thenReturn(update);
        when(extraPort.getNameColumn()).thenReturn(column);

        InstanceIdentifier<OvsdbTerminationPointAugmentation> iid = mock(InstanceIdentifier.class);
        terminationPointUpdateCommand.updateTerminationPoint(transaction, state, iid, terminationPoint,
                mock(InstanceIdentifierCodec.class));
        verify(transaction, times(1)).add(any(Operation.class));
    }
}
