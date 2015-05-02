package org.opendaylight.ovsdb.southbound.transactions.md;

import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public abstract class AbstractTransactionCommand implements TransactionCommand {

    private TableUpdates updates;
    private DatabaseSchema dbSchema;
    private ConnectionInfo key;
    private InstanceIdentifier<Node> connectionIid;

    public TableUpdates getUpdates() {
        return updates;
    }

    public DatabaseSchema getDbSchema() {
        return dbSchema;
    }

    public ConnectionInfo getConnectionInfo() {
        return key;
    }

    public InstanceIdentifier<Node> getConnectionIid() {
        return connectionIid;
    }

    protected AbstractTransactionCommand() {
        // NO OP
    }

    public AbstractTransactionCommand(ConnectionInfo key,TableUpdates updates, DatabaseSchema dbSchema,
            InstanceIdentifier<Node> connectionIid) {
        this.updates = updates;
        this.dbSchema = dbSchema;
        this.key = key;
        this.connectionIid = connectionIid;
    }

}