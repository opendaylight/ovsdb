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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionInstance;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberMatcher;
import org.powermock.api.support.membermodification.MemberModifier;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@PrepareForTest({ OvsdbBridgeRemovedCommand.class, TyperUtils.class, SouthboundMapper.class })
@RunWith(PowerMockRunner.class)
public class OvsdbBridgeRemovedCommandTest {
    @Mock private OvsdbBridgeRemovedCommand ovsdbBridgeRemovedCommand;
    @Mock private OvsdbConnectionInstance key;
    @Mock private TableUpdates updates;
    @Mock private DatabaseSchema dbSchema;

    @Before
    public void setUp() throws Exception {
        ovsdbBridgeRemovedCommand = mock(OvsdbBridgeRemovedCommand.class, Mockito.CALLS_REAL_METHODS);
        MemberModifier.field(OvsdbBridgeRemovedCommand.class, "key").set(ovsdbBridgeRemovedCommand, key);
        MemberModifier.field(OvsdbBridgeRemovedCommand.class, "updates").set(ovsdbBridgeRemovedCommand, updates);
        MemberModifier.field(OvsdbBridgeRemovedCommand.class, "dbSchema").set(ovsdbBridgeRemovedCommand, dbSchema);
    }

    @Test
    public void testExecute() throws Exception {
        //suppress calls to parent get methods
        MemberModifier.suppress(MemberMatcher.method(OvsdbBridgeRemovedCommand.class, "getUpdates"));
        when(ovsdbBridgeRemovedCommand.getUpdates()).thenReturn(mock(TableUpdates.class));
        MemberModifier.suppress(MemberMatcher.method(OvsdbBridgeRemovedCommand.class, "getDbSchema"));
        when(ovsdbBridgeRemovedCommand.getDbSchema()).thenReturn(mock(DatabaseSchema.class));

        PowerMockito.mockStatic(TyperUtils.class);
        Map<UUID, Bridge> map = new HashMap<>();
        when(TyperUtils.extractRowsRemoved(eq(Bridge.class), any(TableUpdates.class), any(DatabaseSchema.class)))
                .thenReturn(map);

        ReadWriteTransaction transaction = mock(ReadWriteTransaction.class);
        ovsdbBridgeRemovedCommand.execute(transaction);
        verify(ovsdbBridgeRemovedCommand).getUpdates();
        verify(ovsdbBridgeRemovedCommand).getDbSchema();
    }
}
