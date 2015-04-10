package org.opendaylight.overlay.transactions.md.utils;

public interface TransactionInvoker {

    public void invoke(TransactionCommand command);

}
