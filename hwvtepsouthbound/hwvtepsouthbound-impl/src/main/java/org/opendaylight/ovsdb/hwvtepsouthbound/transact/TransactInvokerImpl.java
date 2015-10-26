/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound.transact;

import java.util.List;

import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepConnectionInstance;
import org.opendaylight.ovsdb.lib.operations.OperationResult;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ListenableFuture;

public class TransactInvokerImpl implements TransactInvoker {
    private static final Logger LOG = LoggerFactory.getLogger(TransactInvokerImpl.class);
    private HwvtepConnectionInstance connectionInstance;
    private DatabaseSchema dbSchema;

    public TransactInvokerImpl(HwvtepConnectionInstance connectionInstance, DatabaseSchema dbSchema) {
        this.connectionInstance = connectionInstance;
        this.dbSchema = dbSchema;
    }

    @Override
    public void invoke(TransactCommand command) {
        TransactionBuilder tb = new TransactionBuilder(connectionInstance, dbSchema);
        command.execute(tb);
        ListenableFuture<List<OperationResult>> result = tb.execute();
        LOG.debug("invoke: command: {}, tb: {}", command, tb);
        if (tb.getOperations().size() > 0) {
            try {
                List<OperationResult> got = result.get();
                LOG.debug("OVSDB transaction result: {}", got);
            } catch (Exception e) {
                LOG.warn("Transact execution exception: ", e);
            }
            LOG.trace("invoke exit command: {}, tb: {}", command, tb);
        }
    }

}
