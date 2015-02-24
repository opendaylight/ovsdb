package org.opendaylight.ovsdb.southbound.transactions.md;

import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.southbound.OvsdbClientKey;

public abstract class AbstractTransactionCommand implements TransactionCommand {

    private TableUpdates updates;
    private DatabaseSchema dbSchema;
    private OvsdbClientKey key;

    public TableUpdates getUpdates() {
        return updates;
    }

    public DatabaseSchema getDbSchema() {
        return dbSchema;
    }

    public OvsdbClientKey getKey() {
        return key;
    }

    protected AbstractTransactionCommand() {
        // NO OP
    }

    public AbstractTransactionCommand(OvsdbClientKey key,TableUpdates updates, DatabaseSchema dbSchema) {
        this.updates = updates;
        this.dbSchema = dbSchema;
        this.key = key;
    }

}