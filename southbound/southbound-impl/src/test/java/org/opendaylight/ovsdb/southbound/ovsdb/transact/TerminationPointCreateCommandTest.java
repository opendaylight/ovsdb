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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.notation.Condition;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.DefaultOperations;
import org.opendaylight.ovsdb.lib.operations.Insert;
import org.opendaylight.ovsdb.lib.operations.Mutate;
import org.opendaylight.ovsdb.lib.operations.Operation;
import org.opendaylight.ovsdb.lib.operations.Operations;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.operations.Where;
import org.opendaylight.ovsdb.lib.schema.ColumnSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.Interface;
import org.opendaylight.ovsdb.schema.openvswitch.Port;
import org.opendaylight.ovsdb.southbound.InstanceIdentifierCodec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberMatcher;
import org.powermock.api.support.membermodification.MemberModifier;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@PrepareForTest({TransactUtils.class, TerminationPointCreateCommand.class})
@RunWith(PowerMockRunner.class)
public class TerminationPointCreateCommandTest {

    private static final String INTERFACE_NAME = "eth0";
    private static final String TERMINATION_POINT_NAME = "termination point name";
    private TerminationPointCreateCommand terminationPointCreateCommand;

    @Before
    public void setUp() {
        terminationPointCreateCommand = mock(TerminationPointCreateCommand.class, Mockito.CALLS_REAL_METHODS);
    }

    @SuppressWarnings({ "unchecked", "rawtypes"})
    @Test
    @Ignore("This needs to be rewritten")
    public void testExecute() throws Exception {
        MemberModifier.suppress(MemberMatcher.method(TerminationPointCreateCommand.class, "getChanges"));
        DataChangeEvent asynEvent = mock(DataChangeEvent.class);
        Map<InstanceIdentifier<?>, DataObject> map = new HashMap<>();
        OvsdbTerminationPointAugmentation terminationPoint = mock(OvsdbTerminationPointAugmentation.class);
        InstanceIdentifier terminationPointIid = mock(InstanceIdentifier.class);
        map.put(terminationPointIid, terminationPoint);
        when(asynEvent.getCreatedData()).thenReturn(map);
        when(terminationPoint.getName()).thenReturn(TERMINATION_POINT_NAME);

        Interface ovsInterface = mock(Interface.class);
        PowerMockito.mockStatic(TyperUtils.class);
        TransactionBuilder transaction = mock(TransactionBuilder.class);
        when(transaction.getTypedRowWrapper(eq(Interface.class))).thenReturn(ovsInterface);
        //createInterface()
        Operations op = (Operations) setField("op");
        Insert<GenericTableSchema> insert = mock(Insert.class);
        when(op.insert(any(Interface.class))).thenReturn(insert);
        when(insert.withId(anyString())).thenReturn(insert);
        MemberModifier.suppress(MemberMatcher.method(TerminationPointCreateCommand.class,
                "stampInstanceIdentifier", TransactionBuilder.class, InstanceIdentifier.class, String.class));
        when(ovsInterface.getName()).thenReturn(INTERFACE_NAME);

        Port port = mock(Port.class);
        when(transaction.getTypedRowWrapper(eq(Port.class))).thenReturn(port);
        when(op.insert(any(Port.class))).thenReturn(insert);

        Bridge bridge = mock(Bridge.class);
        when(transaction.getTypedRowWrapper(eq(Bridge.class))).thenReturn(bridge);
        doNothing().when(bridge).setName(anyString());
        PowerMockito.whenNew(UUID.class).withAnyArguments().thenReturn(mock(UUID.class));
        doNothing().when(bridge).setPorts(any(HashSet.class));

        BridgeOperationalState bridgeOpState = mock(BridgeOperationalState.class);

        terminationPointCreateCommand.execute(transaction, bridgeOpState, asynEvent,
                mock(InstanceIdentifierCodec.class));

        // TODO Actually verify something
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testStampInstanceIdentifier() {
        TransactionBuilder transaction = mock(TransactionBuilder.class);

        Port port = mock(Port.class);
        when(transaction.getTypedRowWrapper(eq(Port.class))).thenReturn(port);
        doNothing().when(port).setName(anyString());
        doNothing().when(port).setExternalIds(any(HashMap.class));
        when(port.getSchema()).thenReturn(mock(GenericTableSchema.class));
        Column<GenericTableSchema, Map<String, String>> column = mock(Column.class);
        when(port.getExternalIdsColumn()).thenReturn(column);
        when(column.getSchema()).thenReturn(mock(ColumnSchema.class));

        Mutate mutate = mock(Mutate.class);
        // FIXME: rather than static mocking, insert a mock Operations
        PowerMockito.mockStatic(TransactUtils.class);
        when(TransactUtils.stampInstanceIdentifierMutation(any(Operations.class), any(TransactionBuilder.class),
            any(InstanceIdentifier.class),     any(GenericTableSchema.class), any(ColumnSchema.class),
            any(InstanceIdentifierCodec.class))).thenReturn(mutate);

        Column<GenericTableSchema, String> nameColumn = mock(Column.class);
        when(port.getNameColumn()).thenReturn(nameColumn);
        ColumnSchema<GenericTableSchema, String> nameColumnSchema = mock(ColumnSchema.class);
        when(nameColumn.getSchema()).thenReturn(nameColumnSchema);
        when(nameColumnSchema.opEqual(anyString())).thenReturn(mock(Condition.class));
        Where where = mock(Where.class);
        when(mutate.where(any(Condition.class))).thenReturn(where);
        when(where.build()).thenReturn(mock(Operation.class));
        when(transaction.add(any(Operation.class))).thenReturn(transaction);

        String interfaceName = INTERFACE_NAME;
        InstanceIdentifier<OvsdbTerminationPointAugmentation> iid = InstanceIdentifier.create(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(new TopologyId("testTopo")))
            .child(Node.class, new NodeKey(new NodeId("testNode")))
            .child(TerminationPoint.class, new TerminationPointKey(new TpId("testTp")))
            .augmentation(OvsdbTerminationPointAugmentation.class);
        TerminationPointCreateCommand.stampInstanceIdentifier(new DefaultOperations(), transaction, iid, interfaceName,
                mock(InstanceIdentifierCodec.class));
        verify(port).setName(anyString());
        verify(port).getExternalIdsColumn();
        verify(transaction).add(any(Operation.class));
    }

    private static Object setField(final String fieldName) throws Exception {
        Field field = Operations.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(field.get(Operations.class), mock(Operations.class));
        return field.get(Operations.class);
    }
}
