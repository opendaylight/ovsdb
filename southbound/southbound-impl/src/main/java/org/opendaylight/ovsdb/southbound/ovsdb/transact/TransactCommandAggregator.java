/*
 * Copyright (c) 2014, 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import java.util.Collection;

import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.southbound.InstanceIdentifierCodec;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This transactional command aggregates all the Southbound commands.
 */
public class TransactCommandAggregator implements TransactCommand {
    private static final Logger LOG = LoggerFactory.getLogger(TransactCommandAggregator.class);

    private static final Class<? extends TransactCommand>[] COMMAND_CLASSES =
            new Class[] {
                BridgeUpdateCommand.class,
                OpenVSwitchBridgeAddCommand.class,
                ControllerUpdateCommand.class,
                ControllerRemovedCommand.class,
                ProtocolUpdateCommand.class,
                ProtocolRemovedCommand.class,
                BridgeRemovedCommand.class,
                TerminationPointCreateCommand.class,
                TerminationPointDeleteCommand.class,
                OvsdbNodeUpdateCommand.class,
                AutoAttachUpdateCommand.class,
                AutoAttachRemovedCommand.class,
                QosUpdateCommand.class,
                QosRemovedCommand.class,
                QueueUpdateCommand.class,
                QueueRemovedCommand.class,
                TerminationPointUpdateCommand.class,
            };

    @Override
    public void execute(TransactionBuilder transaction, BridgeOperationalState state,
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> events,
            InstanceIdentifierCodec instanceIdentifierCodec) {
        for (Class<? extends TransactCommand> commandClass : COMMAND_CLASSES) {
            try {
                commandClass.newInstance().execute(transaction, state, events, instanceIdentifierCodec);
            } catch (InstantiationException | IllegalAccessException e) {
                LOG.error("Error instantiating {}", commandClass, e);
            }
        }
    }

    @Override
    public void execute(TransactionBuilder transaction, BridgeOperationalState state,
            Collection<DataTreeModification<Node>> modifications, InstanceIdentifierCodec instanceIdentifierCodec) {
        for (Class<? extends TransactCommand> commandClass : COMMAND_CLASSES) {
            try {
                commandClass.newInstance().execute(transaction, state, modifications, instanceIdentifierCodec);
            } catch (InstantiationException | IllegalAccessException e) {
                LOG.error("Error instantiating {}", commandClass, e);
            }
        }
    }
}
