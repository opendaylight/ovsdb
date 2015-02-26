package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;

public interface TransactCommand {

    public void execute(TransactionBuilder transaction);

}
