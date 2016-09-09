/*
 * Copyright (c) 2015 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.notation.Condition;
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.operations.Mutate;
import org.opendaylight.ovsdb.lib.operations.Operation;
import org.opendaylight.ovsdb.lib.operations.Operations;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.operations.Where;
import org.opendaylight.ovsdb.lib.schema.ColumnSchema;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeProtocolBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeProtocolOpenflow10;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ProtocolEntry;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberMatcher;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({InstanceIdentifier.class, ProtocolUpdateCommand.class, TyperUtils.class})
public class ProtocolUpdateCommandTest {

    private static final String BRIDGE_NAME_COLUMN = null;
    private Map<InstanceIdentifier<ProtocolEntry>, ProtocolEntry> protocols = new HashMap<>();
    private ProtocolUpdateCommand protocolUpdateCommand = new ProtocolUpdateCommand();
    @Mock private ProtocolEntry protocolEntry;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        protocols.put(mock(InstanceIdentifier.class), protocolEntry);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testExecute() throws Exception {
        PowerMockito.mockStatic(TransactUtils.class);
        AsyncDataChangeEvent changes = mock(AsyncDataChangeEvent.class);
        when(TransactUtils.extractCreatedOrUpdated(changes, ProtocolEntry.class)).thenReturn(protocols);
        when(TransactUtils.extractCreatedOrUpdated(changes, OvsdbBridgeAugmentation.class)).thenReturn(
                Collections.emptyMap());

        BridgeOperationalState bridgeOpState = mock(BridgeOperationalState.class);
        Optional<ProtocolEntry> operationalProtocolEntryOptional = Optional.absent();
        when(bridgeOpState.getProtocolEntry(any(InstanceIdentifier.class)))
                .thenReturn(operationalProtocolEntryOptional);

        PowerMockito.suppress(MemberMatcher.methodsDeclaredIn(InstanceIdentifier.class));

        OvsdbBridgeAugmentation ovsdbBridge = mock(OvsdbBridgeAugmentation.class);
        Optional<OvsdbBridgeAugmentation> bridgeOptional = Optional.of(ovsdbBridge);
        when(bridgeOpState.getOvsdbBridgeAugmentation(any(InstanceIdentifier.class))).thenReturn(bridgeOptional);

        OvsdbBridgeName ovsdbBridgeName = mock(OvsdbBridgeName.class);
        when(ovsdbBridge.getBridgeName()).thenReturn(ovsdbBridgeName);
        when(protocolEntry.getProtocol()).thenAnswer(new Answer<Class<? extends OvsdbBridgeProtocolBase>>() {
            public Class<? extends OvsdbBridgeProtocolBase> answer(
                    InvocationOnMock invocation) throws Exception {
                return OvsdbBridgeProtocolOpenflow10.class;
            }
        });

        Bridge bridge = mock(Bridge.class);
        PowerMockito.mockStatic(TyperUtils.class);
        TransactionBuilder transaction = mock(TransactionBuilder.class);
        when(transaction.getDatabaseSchema()).thenReturn(mock(DatabaseSchema.class));
        when(TyperUtils.getTypedRowWrapper(any(DatabaseSchema.class), eq(Bridge.class))).thenReturn(bridge);
        doNothing().when(bridge).setName(anyString());
        doNothing().when(bridge).setProtocols(any(Set.class));

        Operations op = (Operations) setField("op");
        Mutate mutate = mock(Mutate.class);
        when(op.mutate(any(Bridge.class))).thenReturn(mutate);
        Column<Set<String>> column = mock(Column.class);
        when(bridge.getProtocolsColumn()).thenReturn(column);
        when(column.getSchema()).thenReturn(mock(ColumnSchema.class));
        when(column.getData()).thenReturn(new HashSet<String>());
        when(mutate.addMutation(any(ColumnSchema.class), any(Mutator.class), any(Set.class))).thenReturn(mutate);

        Column<String> nameColumn = mock(Column.class);
        when(bridge.getNameColumn()).thenReturn(nameColumn);
        when(nameColumn.getData()).thenReturn(BRIDGE_NAME_COLUMN);
        ColumnSchema<String> columnSchema = mock(ColumnSchema.class);
        when(nameColumn.getSchema()).thenReturn(columnSchema);
        when(columnSchema.opEqual(anyString())).thenReturn(mock(Condition.class));
        Where where = mock(Where.class);
        when(mutate.where(any(Condition.class))).thenReturn(where);
        when(where.build()).thenReturn(mock(Operation.class));
        when(transaction.add(any(Operation.class))).thenReturn(transaction);

        protocolUpdateCommand.execute(transaction, bridgeOpState, changes);

        // TODO What are we trying to verify here?
        // verify(transaction).add(any(Operation.class));
    }

    private Object setField(String fieldName) throws Exception {
        Field field = Operations.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(field.get(Operations.class), mock(Operations.class));
        return field.get(Operations.class);
    }
}
