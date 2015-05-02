package org.opendaylight.ovsdb.southbound.transactions.md;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class OvsdbOperationalCommandAggregator implements TransactionCommand {


    private List<TransactionCommand> commands = new ArrayList<TransactionCommand>();

    public OvsdbOperationalCommandAggregator(ConnectionInfo key,TableUpdates updates, DatabaseSchema dbSchema,
            InstanceIdentifier<Node> connectionIid) {
        commands.add(new OvsdbBridgeUpdateCommand(key, updates,  dbSchema, connectionIid));
        commands.add(new OvsdbBridgeRemovedCommand(key, updates,  dbSchema, connectionIid));
        commands.add(new OvsdbControllerUpdateCommand(key, updates,  dbSchema, connectionIid));
        commands.add(new OvsdbControllerRemovedCommand(key, updates,  dbSchema, connectionIid));
        commands.add(new OvsdbPortUpdateCommand(key, updates, dbSchema, connectionIid));
        commands.add(new OvsdbPortRemoveCommand(key, updates, dbSchema, connectionIid));
        commands.add(new OpenVSwitchUpdateCommand(key, updates, dbSchema, connectionIid));
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        for (TransactionCommand command: commands) {
            command.execute(transaction);
        }
    }
}
