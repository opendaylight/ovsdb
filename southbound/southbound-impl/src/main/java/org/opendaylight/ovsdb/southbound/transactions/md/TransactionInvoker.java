package org.opendaylight.ovsdb.southbound.transactions.md;

public interface TransactionInvoker {

    public void invoke(TransactionCommand command);

}
