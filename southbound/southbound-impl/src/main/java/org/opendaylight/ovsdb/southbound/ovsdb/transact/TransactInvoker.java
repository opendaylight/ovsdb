package org.opendaylight.ovsdb.southbound.ovsdb.transact;

public interface TransactInvoker {
    public void invoke(TransactCommand command);
}
