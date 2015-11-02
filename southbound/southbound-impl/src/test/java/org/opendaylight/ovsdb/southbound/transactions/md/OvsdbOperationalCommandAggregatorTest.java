package org.opendaylight.ovsdb.southbound.transactions.md;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionInstance;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberModifier;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@PrepareForTest({})
@RunWith(PowerMockRunner.class)
public class OvsdbOperationalCommandAggregatorTest {
    private static final int NUMBER_OF_COMMANDS = 9;
    private List<TransactionCommand> commands = new ArrayList<TransactionCommand>();
    private OvsdbOperationalCommandAggregator ovsdbOperationalCommandAggregator;

    @Before
    public void setUp() throws Exception {
        ovsdbOperationalCommandAggregator = PowerMockito.mock(OvsdbOperationalCommandAggregator.class, Mockito.CALLS_REAL_METHODS);

        //mock commands field
        commands.add(mock(OpenVSwitchUpdateCommand.class));
        commands.add(mock(OvsdbManagersUpdateCommand.class));
        commands.add(mock(OvsdbManagersRemovedCommand.class));
        commands.add(mock(OvsdbBridgeUpdateCommand.class));
        commands.add(mock(OvsdbBridgeRemovedCommand.class));
        commands.add(mock(OvsdbControllerUpdateCommand.class));
        commands.add(mock(OvsdbControllerRemovedCommand.class));
        commands.add(mock(OvsdbPortUpdateCommand.class));
        commands.add(mock(OvsdbPortRemoveCommand.class));
        MemberModifier.field(OvsdbOperationalCommandAggregator.class, "commands").set(ovsdbOperationalCommandAggregator, commands);
    }

    @Test
    public void testOvsdbOperationalCommandAggregator() throws Exception {
        OvsdbConnectionInstance key = mock(OvsdbConnectionInstance.class);
        TableUpdates updates = mock(TableUpdates.class);
        DatabaseSchema dbSchema = mock(DatabaseSchema.class);
        OvsdbOperationalCommandAggregator ovsdbOperationalCommandAggregator1 = new OvsdbOperationalCommandAggregator(key, updates, dbSchema);
        List<TransactionCommand> testCommands = Whitebox.getInternalState(ovsdbOperationalCommandAggregator1, "commands");
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
