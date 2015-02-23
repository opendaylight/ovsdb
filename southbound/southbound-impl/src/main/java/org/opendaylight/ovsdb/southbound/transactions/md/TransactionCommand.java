package org.opendaylight.ovsdb.southbound.transactions.md;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;

public interface TransactionCommand {

    public void execute(ReadWriteTransaction transaction);

}
