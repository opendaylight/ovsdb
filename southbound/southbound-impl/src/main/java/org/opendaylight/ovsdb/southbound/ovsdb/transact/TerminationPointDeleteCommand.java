/*
 * Copyright © 2015, 2017 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.Port;
import org.opendaylight.ovsdb.southbound.InstanceIdentifierCodec;
import org.opendaylight.ovsdb.southbound.SouthboundUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transaction handler for the TerminationPoints.
 *
 * @author avishnoi@brocade.com (Anil Vishnoi)
 */
public class TerminationPointDeleteCommand implements TransactCommand {
    private static final Logger LOG = LoggerFactory.getLogger(TerminationPointDeleteCommand.class);

    @Override
    public void execute(final TransactionBuilder transaction, final BridgeOperationalState state,
            final DataChangeEvent events, final InstanceIdentifierCodec instanceIdentifierCodec) {
        execute(transaction, state, TransactUtils.extractOriginal(events, OvsdbTerminationPointAugmentation.class),
                TransactUtils.extractOriginal(events, Node.class),
                TransactUtils.extractRemoved(events, OvsdbTerminationPointAugmentation.class));
    }

    @Override
    public void execute(final TransactionBuilder transaction, final BridgeOperationalState state,
            final Collection<DataTreeModification<Node>> modifications,
            final InstanceIdentifierCodec instanceIdentifierCodec) {
        execute(transaction, state,
                TransactUtils.extractOriginal(modifications, OvsdbTerminationPointAugmentation.class),
                TransactUtils.extractOriginal(modifications, Node.class),
                TransactUtils.extractRemoved(modifications, OvsdbTerminationPointAugmentation.class));
    }

    private static void execute(final TransactionBuilder transaction, final BridgeOperationalState state,
            final Map<InstanceIdentifier<OvsdbTerminationPointAugmentation>,
                OvsdbTerminationPointAugmentation> originals,
            final Map<InstanceIdentifier<Node>, Node> originalNodes,
            final Set<InstanceIdentifier<OvsdbTerminationPointAugmentation>> removedTps) {
        for (InstanceIdentifier<OvsdbTerminationPointAugmentation> removedTpIid: removedTps) {
            LOG.debug("Received request to delete termination point {}", removedTpIid);
            OvsdbTerminationPointAugmentation original = originals.get(removedTpIid);
            String bridgeName = SouthboundUtil.getBridgeNameFromOvsdbNodeId(removedTpIid.firstIdentifierOf(Node.class));
            if (bridgeName == null) {
                LOG.error("Missing Bridge Name for Node {} during deletion of Port {}",
                        removedTpIid.firstIdentifierOf(Node.class), original.getName());
                continue;
            }
            LOG.trace("Deleting port {} from bridge {}", original.getName(), bridgeName);
            Port port = transaction.getTypedRowSchema(Port.class);
            Optional<OvsdbTerminationPointAugmentation> tpAugmentation =
                    state.getOvsdbTerminationPointAugmentation(removedTpIid);
            if (tpAugmentation.isPresent()) {
                OvsdbTerminationPointAugmentation tp = tpAugmentation.orElseThrow();
                if (tp.getPortUuid() != null) {
                    UUID portUuid = new UUID(tp.getPortUuid().getValue());
                    Bridge bridge = transaction.getTypedRowSchema(Bridge.class);

                    transaction.add(op.delete(port.getSchema())
                            .where(port.getUuidColumn().getSchema().opEqual(portUuid)).build());
                    transaction.add(op.comment("Port: Deleting " + original.getName()
                            + " attached to " + bridgeName));

                    transaction.add(op.mutate(bridge.getSchema())
                            .addMutation(bridge.getPortsColumn().getSchema(),
                                    Mutator.DELETE, Collections.singleton(portUuid))
                            .where(bridge.getNameColumn().getSchema().opEqual(bridgeName)).build());

                    transaction.add(op.comment("Bridge: Mutating " + bridgeName
                            + " to remove port " + portUuid));
                    LOG.info("Deleted Termination Point : {} with uuid : {}",
                            original.getName(), portUuid);
                } else {
                    LOG.warn("Unable to delete port {} from bridge  {} because it was not found in the operational "
                            + "store, operational store,  and thus we cannot retrieve its UUID",
                            bridgeName,original.getName());
                }
            }
        }
    }
}
