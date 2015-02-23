package org.opendaylight.ovsdb.southbound.transactions.md;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.southbound.OvsdbClientKey;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;

public class OvsdbNodeCreateCommand implements TransactionCommand {

    private OvsdbClientKey key;

    public OvsdbNodeCreateCommand(OvsdbClientKey key,TableUpdates updates,DatabaseSchema dbSchema) {
        this.key = key;
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        transaction.put(LogicalDatastoreType.OPERATIONAL, key.toInstanceIndentifier(),
                SouthboundMapper.createNode(key));
    }

}
