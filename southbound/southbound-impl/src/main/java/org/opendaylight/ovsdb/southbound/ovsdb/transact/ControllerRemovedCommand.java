/*
 * Copyright © 2014, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.Operations;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.southbound.InstanceIdentifierCodec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntry;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ControllerRemovedCommand extends AbstractTransactCommand {
    private static final Logger LOG = LoggerFactory.getLogger(ControllerRemovedCommand.class);

    public ControllerRemovedCommand(final Operations op) {
        super(op);
    }

    @Override
    public void execute(final TransactionBuilder transaction, final BridgeOperationalState state,
            final DataChangeEvent events, final InstanceIdentifierCodec instanceIdentifierCodec) {
        execute(op, transaction, state, TransactUtils.extractRemoved(events, ControllerEntry.class),
                TransactUtils.extractCreatedOrUpdatedOrRemoved(events, OvsdbBridgeAugmentation.class));
    }

    @Override
    public void execute(final TransactionBuilder transaction, final BridgeOperationalState state,
            final Collection<DataTreeModification<Node>> modifications,
            final InstanceIdentifierCodec instanceIdentifierCodec) {
        execute(op, transaction, state, TransactUtils.extractRemoved(modifications, ControllerEntry.class),
                TransactUtils.extractCreatedOrUpdatedOrRemoved(modifications, OvsdbBridgeAugmentation.class));
    }

    private static void execute(final Operations op, final TransactionBuilder transaction,
            final BridgeOperationalState state,
            final Set<InstanceIdentifier<ControllerEntry>> removedControllers,
            final Map<InstanceIdentifier<OvsdbBridgeAugmentation>, OvsdbBridgeAugmentation> modifiedBridges) {
        for (InstanceIdentifier<ControllerEntry> controllerIid : removedControllers) {
            LOG.debug("Removing Registered...ODL controller : {} ", controllerIid);
            InstanceIdentifier<OvsdbBridgeAugmentation> bridgeIid =
                    controllerIid.firstIdentifierOf(OvsdbBridgeAugmentation.class);
            OvsdbBridgeAugmentation ovsdbBridge = modifiedBridges.get(bridgeIid);
            Optional<ControllerEntry> controllerEntryOptional = state.getControllerEntry(controllerIid);
            if (ovsdbBridge != null && controllerEntryOptional.isPresent()) {
                ControllerEntry controllerEntry = controllerEntryOptional.orElseThrow();
                Bridge bridge = transaction.getTypedRowWrapper(Bridge.class);
                bridge.setController(Collections.singleton(new UUID(controllerEntry.getControllerUuid().getValue())));
                transaction.add(op.mutate(bridge).addMutation(bridge.getControllerColumn().getSchema(),
                        Mutator.DELETE, bridge.getControllerColumn().getData()));
            }
        }
    }
}
