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

/**
 * Contract for a transactional command.
 */
public interface TransactCommand {
    /**
     * Queue the command defined by the class implementing this interface in the given transaction builder, with the
     * given bridge state, in reaction to the given events.
     *
     * @param transaction The transaction builder.
     * @param state The bridge state.
     * @param events The events to be represented.
     * @param instanceIdentifierCodec The instance identifier codec to use.
     */
    void execute(TransactionBuilder transaction, BridgeOperationalState state,
                 AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> events,
                 InstanceIdentifierCodec instanceIdentifierCodec);

    /**
     * Queue the command defined by the class implementing this interface in the given transaction builder, with the
     * given bridge state, in reaction to the given modifications.
     *
     * @param transaction The transaction builder.
     * @param state The bridge state.
     * @param modifications The modifications to be represented.
     * @param instanceIdentifierCodec The instance identifier codec to use.
     */
    void execute(TransactionBuilder transaction, BridgeOperationalState state,
                 Collection<DataTreeModification<Node>> modifications,
                 InstanceIdentifierCodec instanceIdentifierCodec);
}
