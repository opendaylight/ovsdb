/*
 * Copyright (c) 2015 Inocybe Technologies. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.operations.Mutate;
import org.opendaylight.ovsdb.lib.operations.Operation;
import org.opendaylight.ovsdb.lib.operations.Operations;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.ColumnSchema;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.southbound.InstanceIdentifierCodec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeProtocolBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeProtocolOpenflow10;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ProtocolEntry;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberMatcher;
import org.powermock.api.support.membermodification.MemberModifier;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({InstanceIdentifier.class, TransactUtils.class, TyperUtils.class })
public class ProtocolRemovedCommandTest {

    private Set<InstanceIdentifier<ProtocolEntry>> removed = new HashSet<>();
    @Mock private Map<InstanceIdentifier<OvsdbBridgeAugmentation>, OvsdbBridgeAugmentation> updatedBridges;

    @SuppressWarnings("unchecked")
    @Test
    @Ignore("This needs to be rewritten")
    public void testExecute() throws Exception {
        PowerMockito.suppress(MemberMatcher.methodsDeclaredIn(InstanceIdentifier.class));

        ProtocolEntry protocol = mock(ProtocolEntry.class);
        when(protocol.getProtocol()).thenAnswer(new Answer<Class<? extends OvsdbBridgeProtocolBase>>() {
            public Class<? extends OvsdbBridgeProtocolBase> answer(
                    InvocationOnMock invocation) throws Exception {
                return OvsdbBridgeProtocolOpenflow10.class;
            }
        });

        BridgeOperationalState bridgeOpState = mock(BridgeOperationalState.class);
        when(bridgeOpState.getProtocolEntry(any(InstanceIdentifier.class))).thenReturn(Optional.of(protocol));

        InstanceIdentifier<ProtocolEntry> protocolIid = mock(InstanceIdentifier.class);
        removed.add(protocolIid);
        ProtocolRemovedCommand protocolRemovedCommand = mock(ProtocolRemovedCommand.class, Mockito.CALLS_REAL_METHODS);
        MemberModifier.field(ProtocolRemovedCommand.class,"removed").set(protocolRemovedCommand, removed);

        MemberModifier.field(ProtocolRemovedCommand.class,"updatedBridges").set(protocolRemovedCommand, updatedBridges);
        when(updatedBridges.get(any(InstanceIdentifier.class))).thenReturn(mock(OvsdbBridgeAugmentation.class));

        Operations op = (Operations) setField("op");
        Mutate mutate = mock(Mutate.class);
        when(op.mutate(any(Bridge.class))).thenReturn(mutate);
        Column<Set<String>> column = mock(Column.class);
        Bridge bridge = mock(Bridge.class);
        when(bridge.getProtocolsColumn()).thenReturn(column);
        when(column.getSchema()).thenReturn(mock(ColumnSchema.class));
        when(column.getData()).thenReturn(new HashSet<String>());
        when(mutate.addMutation(any(ColumnSchema.class), any(Mutator.class), any(Set.class))).thenReturn(mutate);

        PowerMockito.mockStatic(TyperUtils.class);
        when(TyperUtils.getTypedRowWrapper(any(DatabaseSchema.class), any(Class.class))).thenReturn(bridge);

        TransactionBuilder transaction = mock(TransactionBuilder.class);
        protocolRemovedCommand.execute(transaction, bridgeOpState, mock(AsyncDataChangeEvent.class),
                mock(InstanceIdentifierCodec.class));
        Mockito.verify(transaction).add(any(Operation.class));
    }

    private Object setField(String fieldName) throws Exception {
        Field field = Operations.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(field.get(Operations.class), mock(Operations.class));
        return field.get(Operations.class);
    }
}
