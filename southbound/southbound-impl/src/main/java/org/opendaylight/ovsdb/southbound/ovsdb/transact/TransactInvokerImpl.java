/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import java.util.Collection;
import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.ovsdb.lib.operations.OperationResult;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionInstance;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ListenableFuture;

public class TransactInvokerImpl implements TransactInvoker {
    private static final Logger LOG = LoggerFactory.getLogger(TransactInvokerImpl.class);
    private OvsdbConnectionInstance connectionInstance;
    private DatabaseSchema dbSchema;

    public TransactInvokerImpl(OvsdbConnectionInstance connectionInstance, DatabaseSchema dbSchema) {
        this.connectionInstance = connectionInstance;
        this.dbSchema = dbSchema;
    }

    @Override
    public void invoke(TransactCommand command, BridgeOperationalState state,
                       AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> events) {
        TransactionBuilder tb = new TransactionBuilder(connectionInstance, dbSchema);
        command.execute(tb, state, events);
        invoke(command, tb);
    }

    @Override
    public void invoke(TransactCommand command, BridgeOperationalState state,
                       Collection<DataTreeModification<Node>> modifications) {
        TransactionBuilder tb = new TransactionBuilder(connectionInstance, dbSchema);
        command.execute(tb, state, modifications);
        invoke(command, tb);
    }

    private void invoke(TransactCommand command, TransactionBuilder tb) {
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
