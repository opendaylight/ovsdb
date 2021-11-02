/*
 * Copyright (c) 2015 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.southbound.transactions.md;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionInstance;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class OvsdbBridgeRemovedCommandTest {
    @Mock
    private OvsdbBridgeRemovedCommand ovsdbBridgeRemovedCommand;
    @Mock
    private OvsdbConnectionInstance key;
    @Mock
    private TableUpdates updates;
    @Mock
    private DatabaseSchema dbSchema;

    @Before
    public void setUp() {
        ovsdbBridgeRemovedCommand = spy(new OvsdbBridgeRemovedCommand(null, key, updates, dbSchema));
    }

    @Test
    public void testExecute() {
        when(ovsdbBridgeRemovedCommand.getUpdates()).thenReturn(mock(TableUpdates.class));
        when(ovsdbBridgeRemovedCommand.getDbSchema()).thenReturn(mock(DatabaseSchema.class));

        try (var utils = mockStatic(TyperUtils.class)) {
            Map<UUID, Bridge> map = new HashMap<>();
            utils.when(() -> TyperUtils.extractRowsRemoved(eq(Bridge.class), any(TableUpdates.class),
                any(DatabaseSchema.class))).thenReturn(map);

            ovsdbBridgeRemovedCommand.execute(mock(ReadWriteTransaction.class));
        }
        verify(ovsdbBridgeRemovedCommand).getUpdates();
        verify(ovsdbBridgeRemovedCommand).getDbSchema();
    }
}
