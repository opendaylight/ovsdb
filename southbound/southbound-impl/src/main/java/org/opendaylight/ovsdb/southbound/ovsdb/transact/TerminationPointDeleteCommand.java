/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.Port;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;

/**
 * @author avishnoi@brocade.com (Anil Vishnoi)
 *
 */
public class TerminationPointDeleteCommand extends AbstractTransactCommand {
    private static final Logger LOG = LoggerFactory.getLogger(TerminationPointDeleteCommand.class);

    public TerminationPointDeleteCommand(BridgeOperationalState state,
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        super(state, changes);
    }

    @Override
    public void execute(TransactionBuilder transaction) {
        Map<InstanceIdentifier<OvsdbTerminationPointAugmentation>, OvsdbTerminationPointAugmentation> originals
            = TransactUtils.extractOriginal(getChanges(),OvsdbTerminationPointAugmentation.class);
        Map<InstanceIdentifier<Node>, Node> originalNodes
            = TransactUtils.extractOriginal(getChanges(),Node.class);
        Set<InstanceIdentifier<OvsdbTerminationPointAugmentation>> removedTps
            = TransactUtils.extractRemoved(getChanges(), OvsdbTerminationPointAugmentation.class);
        for (InstanceIdentifier<OvsdbTerminationPointAugmentation> removedTpIid: removedTps) {
            LOG.info("Received request to delete termination point {}",removedTpIid);

            OvsdbTerminationPointAugmentation original = originals.get(removedTpIid);
            Node originalNode = originalNodes.get(removedTpIid.firstIdentifierOf(Node.class));
            OvsdbBridgeAugmentation originalOvsdbBridgeAugmentation =
                    originalNode.getAugmentation(OvsdbBridgeAugmentation.class);
            String bridgeName = originalOvsdbBridgeAugmentation != null
                     ? originalOvsdbBridgeAugmentation.getBridgeName().getValue() : "Bridge name not found";
            Port port = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), Port.class,null);
            Optional<OvsdbTerminationPointAugmentation> tpAugmentation =
                    getOperationalState().getOvsdbTerminationPointAugmentation(removedTpIid);

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
                                    Mutator.DELETE, Sets.newHashSet(portUuid))
                            .where(bridge.getNameColumn().getSchema().opEqual(bridgeName)).build());

                    transaction.add(op.comment("Bridge: Mutating " + bridgeName
                            + " to remove port " + portUuid));
                } else {
                    LOG.warn("Unable to delete port {} from bridge  {} because it was not found in the operational "
                            + "store, operational store,  and thus we cannot retrieve its UUID",
                            bridgeName,original.getName());
                }
            }
        }
    }
}
