/*
 * Copyright (c) 2015 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.transactions.md;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.Version;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.southbound.InstanceIdentifierCodec;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionInstance;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.powermock.reflect.Whitebox;

@RunWith(MockitoJUnitRunner.class)
public class OvsdbOperationalCommandAggregatorTest {

    private static final int NUMBER_OF_COMMANDS = 15;
    private final List<TransactionCommand> commands = new ArrayList<>();
    private OvsdbOperationalCommandAggregator ovsdbOperationalCommandAggregator;

    @Before
    public void setUp() throws Exception {
        ovsdbOperationalCommandAggregator = mock(OvsdbOperationalCommandAggregator.class,
                Mockito.CALLS_REAL_METHODS);

        //mock commands field
        commands.add(mock(OpenVSwitchUpdateCommand.class));
        commands.add(mock(OvsdbManagersUpdateCommand.class));
        commands.add(mock(OvsdbManagersRemovedCommand.class));
        commands.add(mock(OvsdbAutoAttachUpdateCommand.class));
        commands.add(mock(OvsdbAutoAttachRemovedCommand.class));
        commands.add(mock(OvsdbQosUpdateCommand.class));
        commands.add(mock(OvsdbQosRemovedCommand.class));
        commands.add(mock(OvsdbQueueUpdateCommand.class));
        commands.add(mock(OvsdbQueueRemovedCommand.class));
        commands.add(mock(OvsdbBridgeUpdateCommand.class));
        commands.add(mock(OvsdbBridgeRemovedCommand.class));
        commands.add(mock(OvsdbControllerUpdateCommand.class));
        commands.add(mock(OvsdbControllerRemovedCommand.class));
        commands.add(mock(OvsdbPortUpdateCommand.class));
        commands.add(mock(OvsdbPortRemoveCommand.class));
        Whitebox.getField(OvsdbOperationalCommandAggregator.class, "commands").set(ovsdbOperationalCommandAggregator,
                commands);
    }

    @Test
    public void testOvsdbOperationalCommandAggregator() throws Exception {
        OvsdbConnectionInstance key = mock(OvsdbConnectionInstance.class);
        TableUpdates updates = mock(TableUpdates.class);
        DatabaseSchema dbSchema = mock(DatabaseSchema.class);
        when(dbSchema.getVersion())
                .thenReturn(Version.fromString(SouthboundConstants.AUTOATTACH_SUPPORTED_OVS_SCHEMA_VERSION));
        OvsdbOperationalCommandAggregator ovsdbOperationalCommandAggregator1 = new OvsdbOperationalCommandAggregator(
                mock(InstanceIdentifierCodec.class), key, updates, dbSchema, false);
        List<TransactionCommand> testCommands = Whitebox.getInternalState(ovsdbOperationalCommandAggregator1,
                "commands");
        assertEquals(NUMBER_OF_COMMANDS, testCommands.size());
    }

    @Test
    public void testExecute() {
        ReadWriteTransaction transaction = mock(ReadWriteTransaction.class);
        for (TransactionCommand command: commands) {
            doNothing().when(command).execute(any(ReadWriteTransaction.class));
        }
        ovsdbOperationalCommandAggregator.execute(transaction);
        for (TransactionCommand command: commands) {
            verify(command).execute(any(ReadWriteTransaction.class));
        }
    }
}
