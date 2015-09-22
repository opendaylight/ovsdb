package org.opendaylight.ovsdb.southbound.transactions.md;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionInstance;

public class OvsdbOperationalCommandAggregator implements TransactionCommand {


    private List<TransactionCommand> commands = new ArrayList<TransactionCommand>();

    public OvsdbOperationalCommandAggregator(OvsdbConnectionInstance key,TableUpdates updates,
            DatabaseSchema dbSchema) {
        commands.add(new OpenVSwitchUpdateCommand(key, updates, dbSchema));
        commands.add(new OvsdbManagersUpdateCommand(key, updates,  dbSchema));
        commands.add(new OvsdbManagersRemovedCommand(key, updates,  dbSchema));
        commands.add(new OvsdbBridgeUpdateCommand(key, updates,  dbSchema));
        commands.add(new OvsdbBridgeRemovedCommand(key, updates,  dbSchema));
        commands.add(new OvsdbControllerUpdateCommand(key, updates,  dbSchema));
        commands.add(new OvsdbControllerRemovedCommand(key, updates,  dbSchema));
        commands.add(new OvsdbPortUpdateCommand(key, updates, dbSchema));
        commands.add(new OvsdbPortRemoveCommand(key, updates, dbSchema));
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        for (TransactionCommand command: commands) {
            command.execute(transaction);
        }
    }
}
