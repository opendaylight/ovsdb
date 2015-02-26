/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import java.util.List;

import org.opendaylight.ovsdb.lib.operations.OperationResult;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ListenableFuture;

public class TransactInvokerImpl implements TransactInvoker {
    private static final Logger LOG = LoggerFactory.getLogger(BridgeCreateCommand.class);
    private OvsdbConnectionInstance ci;
    private DatabaseSchema dbSchema;

    public TransactInvokerImpl(OvsdbConnectionInstance ci, DatabaseSchema dbSchema) {
        this.ci =ci;
        this.dbSchema = dbSchema;
    }

    @Override
    public void invoke(TransactCommand command) {
        TransactionBuilder tb = new TransactionBuilder(ci, dbSchema);
        command.execute(tb);
        ListenableFuture<List<OperationResult>> result = tb.execute();
        try {
            List<OperationResult> got = result.get();
            LOG.info("Results of create bridge request {}",got);
        } catch (Exception e){
            LOG.info("Execution exception: {}",e);
        }
    }

}
