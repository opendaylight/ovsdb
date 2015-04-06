package org.opendaylight.ovsdb.northbound.transactions;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;

public interface TransactionCommand {

    public void execute(ReadWriteTransaction transaction);

}
