package org.opendaylight.ovsdb.southbound.transactions.md;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;

public interface TransactionCommand {

    void execute(ReadWriteTransaction transaction);

}
