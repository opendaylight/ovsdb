package org.opendaylight.overlay.transactions.md.utils;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;

public interface TransactionCommand {

    public void execute(ReadWriteTransaction transaction);

}
