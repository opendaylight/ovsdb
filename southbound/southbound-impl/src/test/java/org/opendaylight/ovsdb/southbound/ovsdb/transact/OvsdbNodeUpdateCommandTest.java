/*
 * Copyright © 2015, 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.operations.Mutate;
import org.opendaylight.ovsdb.lib.operations.Operations;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.ColumnSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.schema.openvswitch.OpenVSwitch;
import org.opendaylight.ovsdb.southbound.InstanceIdentifierCodec;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchExternalIdsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchOtherConfigsBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.reflect.Whitebox;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class OvsdbNodeUpdateCommandTest {
    private static final String EXTERNAL_ID_KEY = "external id key";
    private static final String EXTERNAL_ID_VALUE = "external id value";
    private static final String OTHER_CONFIG_KEY = "other config key";
    private static final String OTHER_CONFIG_VALUE = "other config value";

    @Mock
    private DataChangeEvent changes;
    private OvsdbNodeUpdateCommand ovsdbNodeUpdateCommand;

    @Before
    public void setUp() {
        ovsdbNodeUpdateCommand = spy(OvsdbNodeUpdateCommand.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testExecute() throws Exception {
        Map<InstanceIdentifier<OvsdbNodeAugmentation>, OvsdbNodeAugmentation> updated = new HashMap<>();
        InstanceIdentifier<OvsdbNodeAugmentation> iid = mock(InstanceIdentifier.class);
        OvsdbNodeAugmentation ovsdbNode = mock(OvsdbNodeAugmentation.class);
        updated.put(iid, ovsdbNode);

        try (var utils = mockStatic(TransactUtils.class)) {
            utils.when(() -> TransactUtils.extractCreatedOrUpdated(any(DataChangeEvent.class),
                eq(OvsdbNodeAugmentation.class))).thenReturn(updated);

            ConnectionInfo connectionInfo = mock(ConnectionInfo.class);
            when(ovsdbNode.getConnectionInfo()).thenReturn(connectionInfo);
            when(connectionInfo.getRemoteIp()).thenReturn(mock(IpAddress.class));
            when(connectionInfo.getRemotePort()).thenReturn(mock(PortNumber.class));

            OpenVSwitch ovs = mock(OpenVSwitch.class);
            TransactionBuilder transaction = mock(TransactionBuilder.class);
            when(transaction.getTypedRowWrapper(eq(OpenVSwitch.class))).thenReturn(ovs);

            OpenvswitchExternalIds externalId = new OpenvswitchExternalIdsBuilder()
                .setExternalIdKey(EXTERNAL_ID_KEY)
                .setExternalIdValue(EXTERNAL_ID_VALUE)
                .build();
            when(ovsdbNode.getOpenvswitchExternalIds()).thenReturn(Map.of(externalId.key(), externalId));
            doNothing().when(ovs).setExternalIds(anyMap());

            Mutate<GenericTableSchema> mutate = mock(Mutate.class);
            Operations op = setOpField();
            Column<GenericTableSchema, Map<String, String>> column = mock(Column.class);
            when(ovs.getExternalIdsColumn()).thenReturn(column);
            when(column.getSchema()).thenReturn(mock(ColumnSchema.class));
            when(column.getData()).thenReturn(new HashMap<>());
            when(op.mutate(any(OpenVSwitch.class))).thenReturn(mutate);
            when(transaction.add(null)).thenReturn(transaction);

            OpenvswitchOtherConfigs otherConfig = new OpenvswitchOtherConfigsBuilder()
                .setOtherConfigKey(OTHER_CONFIG_KEY)
                .setOtherConfigValue(OTHER_CONFIG_VALUE)
                .build();

            when(ovsdbNode.getOpenvswitchOtherConfigs()).thenReturn(Map.of(otherConfig.key(), otherConfig));
            doNothing().when(ovs).setOtherConfig(anyMap());
            when(ovs.getOtherConfigColumn()).thenReturn(column);

            ovsdbNodeUpdateCommand.execute(transaction, mock(BridgeOperationalState.class), changes,
                mock(InstanceIdentifierCodec.class));
            verify(ovs, times(3)).getExternalIdsColumn();
            verify(transaction, times(2)).add(eq(null));
        }
    }

    static Operations setOpField() {
        Operations mockOp = mock(Operations.class);
        Whitebox.setInternalState(Operations.class, "op", mockOp);
        return mockOp;
    }
}
