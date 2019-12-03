/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import com.google.common.base.Strings;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.ovsdb.lib.operations.OperationResult;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.typed.TypedDatabaseSchema;
import org.opendaylight.ovsdb.southbound.InstanceIdentifierCodec;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionInstance;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactInvokerImpl implements TransactInvoker {
    private static final Logger LOG = LoggerFactory.getLogger(TransactInvokerImpl.class);
    private final OvsdbConnectionInstance connectionInstance;
    private final TypedDatabaseSchema dbSchema;

    public TransactInvokerImpl(final OvsdbConnectionInstance connectionInstance, final TypedDatabaseSchema dbSchema) {
        this.connectionInstance = connectionInstance;
        this.dbSchema = dbSchema;
    }

    @Override
    public void invoke(final TransactCommand command, final BridgeOperationalState state,
            final DataChangeEvent events, final InstanceIdentifierCodec instanceIdentifierCodec) {
        TransactionBuilder tb = new TransactionBuilder(connectionInstance.getOvsdbClient(), dbSchema);
        command.execute(tb, state, events, instanceIdentifierCodec);
        invoke(command, tb);
    }

    @Override
    public void invoke(final TransactCommand command, final BridgeOperationalState state,
            final Collection<DataTreeModification<Node>> modifications,
            final InstanceIdentifierCodec instanceIdentifierCodec) {
        TransactionBuilder tb = new TransactionBuilder(connectionInstance.getOvsdbClient(), dbSchema);
        command.execute(tb, state, modifications, instanceIdentifierCodec);
        invoke(command, tb);
    }

    private static void invoke(final TransactCommand command, final TransactionBuilder tb) {
        ListenableFuture<List<OperationResult>> result = tb.execute();
        LOG.debug("invoke: command: {}, tb: {}", command, tb);
        if (tb.getOperations().size() > 0) {
            try {
                if (!result.isCancelled()) {
                    List<OperationResult> got = result.get();
                    if (got != null) {
                        got.stream()
                                .filter(response -> !Strings.isNullOrEmpty(response.getError()))
                                .peek(response -> LOG.error("Failed to transact to device {}", response.getError()));
                    }
                    LOG.debug("OVSDB transaction result: {}", got);
                } else {
                    LOG.debug("Operation task cancelled for transaction : {}", tb);
                }
            } catch (InterruptedException | ExecutionException | CancellationException e) {
                LOG.warn("Transact execution exception: ", e);
            }
            LOG.trace("invoke exit command: {}, tb: {}", command, tb);
        }
    }
}
