package org.opendaylight.ovsdb.southbound.transactions.md;

import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;

public abstract class AbstractTransactionCommand implements TransactionCommand {

    private TableUpdates updates;
    private DatabaseSchema dbSchema;
    private OvsdbConnectionInstance key;

    public TableUpdates getUpdates() {
        return updates;
    }

    public DatabaseSchema getDbSchema() {
        return dbSchema;
    }

    public ConnectionInfo getConnectionInfo() {
        return key.getMDConnectionInfo();
    }

    public OvsdbConnectionInstance getOvsdbConnectionInstance() {
        return key;
    }

    protected AbstractTransactionCommand() {
        // NO OP
    }

    public AbstractTransactionCommand(OvsdbConnectionInstance key,TableUpdates updates, DatabaseSchema dbSchema) {
        this.updates = updates;
        this.dbSchema = dbSchema;
        this.key = key;
    }

}