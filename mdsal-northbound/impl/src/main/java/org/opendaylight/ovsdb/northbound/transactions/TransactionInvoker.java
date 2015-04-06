package org.opendaylight.ovsdb.northbound.transactions;

public interface TransactionInvoker {

    public void invoke(TransactionCommand command);

}
