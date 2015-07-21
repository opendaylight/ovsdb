package org.opendaylight.ovsdb.southbound.transactions.md;

public interface TransactionInvoker {

    void invoke(TransactionCommand command);

}
