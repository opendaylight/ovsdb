/*
 * Copyright (c) 2014, 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import java.util.Collection;
import java.util.function.Supplier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.southbound.InstanceIdentifierCodec;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

/**
 * This transactional command aggregates all the Southbound commands.
 */
public class TransactCommandAggregator implements TransactCommand {
    // Type capture to allow using an array
    private interface CommandSupplier extends Supplier<TransactCommand> {

    }

    private static final CommandSupplier[] COMMAND_SUPPLIERS = new CommandSupplier[] {
        BridgeUpdateCommand::new,
        OpenVSwitchBridgeAddCommand::new,
        ControllerUpdateCommand::new,
        ControllerRemovedCommand::new,
        ProtocolUpdateCommand::new,
        ProtocolRemovedCommand::new,
        BridgeRemovedCommand::new,
        TerminationPointCreateCommand::new,
        TerminationPointDeleteCommand::new,
        OvsdbNodeUpdateCommand::new,
        AutoAttachUpdateCommand::new,
        AutoAttachRemovedCommand::new,
        QosUpdateCommand::new,
        QosRemovedCommand::new,
        QueueUpdateCommand::new,
        QueueRemovedCommand::new,
        TerminationPointUpdateCommand::new
    };

    @Override
    public void execute(final TransactionBuilder transaction, final BridgeOperationalState state,
            final DataChangeEvent events, final InstanceIdentifierCodec instanceIdentifierCodec) {
        for (CommandSupplier supplier : COMMAND_SUPPLIERS) {
            supplier.get().execute(transaction, state, events, instanceIdentifierCodec);
        }
    }

    @Override
    public void execute(final TransactionBuilder transaction, final BridgeOperationalState state,
            final Collection<DataTreeModification<Node>> modifications,
            final InstanceIdentifierCodec instanceIdentifierCodec) {
        for (CommandSupplier supplier : COMMAND_SUPPLIERS) {
            supplier.get().execute(transaction, state, modifications, instanceIdentifierCodec);
        }
    }
}
