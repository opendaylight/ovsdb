/*
 * Copyright © 2015, 2017 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import com.google.common.base.Optional;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.Port;
import org.opendaylight.ovsdb.southbound.InstanceIdentifierCodec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
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
    public void execute(TransactionBuilder transaction, BridgeOperationalState state,
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> events,
            InstanceIdentifierCodec instanceIdentifierCodec) {
        execute(transaction, state, TransactUtils.extractOriginal(events, OvsdbTerminationPointAugmentation.class),
                TransactUtils.extractOriginal(events, Node.class),
                TransactUtils.extractRemoved(events, OvsdbTerminationPointAugmentation.class));
    }

    @Override
    public void execute(TransactionBuilder transaction, BridgeOperationalState state,
            Collection<DataTreeModification<Node>> modifications,
            InstanceIdentifierCodec instanceIdentifierCodec) {
        execute(transaction, state,
                TransactUtils.extractOriginal(modifications, OvsdbTerminationPointAugmentation.class),
                TransactUtils.extractOriginal(modifications, Node.class),
                TransactUtils.extractRemoved(modifications, OvsdbTerminationPointAugmentation.class));
    }

    private void execute(TransactionBuilder transaction, BridgeOperationalState state,
                         Map<InstanceIdentifier<OvsdbTerminationPointAugmentation>,
                                 OvsdbTerminationPointAugmentation> originals,
                         Map<InstanceIdentifier<Node>, Node> originalNodes,
                         Set<InstanceIdentifier<OvsdbTerminationPointAugmentation>> removedTps) {
        for (InstanceIdentifier<OvsdbTerminationPointAugmentation> removedTpIid: removedTps) {
            LOG.debug("Received request to delete termination point {}", removedTpIid);

            OvsdbTerminationPointAugmentation original = originals.get(removedTpIid);
            Node originalNode = originalNodes.get(removedTpIid.firstIdentifierOf(Node.class));
            OvsdbBridgeAugmentation originalOvsdbBridgeAugmentation =
                    originalNode.getAugmentation(OvsdbBridgeAugmentation.class);
            String bridgeName = null;
            if (originalOvsdbBridgeAugmentation != null) {
                bridgeName = originalOvsdbBridgeAugmentation.getBridgeName().getValue();
            } else {
                Optional<OvsdbBridgeAugmentation> bridgeAug = state.getOvsdbBridgeAugmentation(removedTpIid);
                if (bridgeAug.isPresent()) {
                    bridgeName = bridgeAug.get().getBridgeName().getValue();
                } else {
                    LOG.error("Bridge does not exist for termination point {}", removedTpIid);
                }
            }

            Port port = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), Port.class,null);
            Optional<OvsdbTerminationPointAugmentation> tpAugmentation =
                    state.getOvsdbTerminationPointAugmentation(removedTpIid);

            if (tpAugmentation.isPresent()) {
                OvsdbTerminationPointAugmentation tp = tpAugmentation.get();
                if (tp.getPortUuid() != null) {
                    UUID portUuid = new UUID(tp.getPortUuid().getValue());
                    Bridge bridge = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(),
                            Bridge.class,null);

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
