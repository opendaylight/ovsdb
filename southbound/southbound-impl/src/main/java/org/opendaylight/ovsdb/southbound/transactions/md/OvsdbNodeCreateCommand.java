package org.opendaylight.ovsdb.southbound.transactions.md;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionInstance;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;

public class OvsdbNodeCreateCommand extends AbstractTransactionCommand {

    public OvsdbNodeCreateCommand(OvsdbConnectionInstance key,TableUpdates updates,DatabaseSchema dbSchema) {
        super(key,updates,dbSchema);
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        transaction.put(LogicalDatastoreType.OPERATIONAL,
                SouthboundMapper.createInstanceIdentifier(getConnectionInfo()),
                SouthboundMapper.createNode(getConnectionInfo()));
    }

}
