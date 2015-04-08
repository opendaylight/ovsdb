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
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
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
import com.google.common.util.concurrent.CheckedFuture;

/**
 * @author avishnoi@brocade.com (Anil Vishnoi)
 *
 */
public class TerminationPointDeleteCommand implements TransactCommand {
    private static final Logger LOG = LoggerFactory.getLogger(TerminationPointDeleteCommand.class);
    private AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes;
    private DataBroker db;

    public TerminationPointDeleteCommand(DataBroker db, AsyncDataChangeEvent<InstanceIdentifier<?>,
            DataObject> changes) {
        this.db = db;
        this.changes = changes;
    }

    @Override
    public void execute(TransactionBuilder transaction) {
        Map<InstanceIdentifier<OvsdbTerminationPointAugmentation>, OvsdbTerminationPointAugmentation> originals
            = TransactUtils.extractOriginal(changes,OvsdbTerminationPointAugmentation.class);
        Set<InstanceIdentifier<OvsdbTerminationPointAugmentation>> removedTps
            = TransactUtils.extractRemoved(changes, OvsdbTerminationPointAugmentation.class);
        for (InstanceIdentifier<OvsdbTerminationPointAugmentation> removedTpIid: removedTps) {
            LOG.info("Received request to delete termination point {}",removedTpIid);

            OvsdbTerminationPointAugmentation original = originals.get(removedTpIid);
            Port port = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), Port.class,null);
            Optional<UUID> terminationPointUuidOptional = getTerminationPointUUID(removedTpIid);

            if (terminationPointUuidOptional.isPresent()) {
                UUID portUuid = terminationPointUuidOptional.get();
                Bridge bridge = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(),
                        Bridge.class,null);
                Optional<String> bridgeName = getBridgeName(removedTpIid);

                transaction.add(op.delete(port.getSchema())
                        .where(port.getUuidColumn().getSchema().opEqual(portUuid)).build());
                transaction.add(op.comment("Port: Deleting " + original.getName()
                        + " attached to " + bridgeName));

                transaction.add(op.mutate(bridge.getSchema())
                        .addMutation(bridge.getPortsColumn().getSchema(),
                                Mutator.DELETE, Sets.newHashSet(portUuid))
                        .where(bridge.getNameColumn().getSchema().opEqual(bridgeName.get())).build());

                transaction.add(op.comment("Bridge: Mutating " + bridgeName.get()
                        + " to remove port " + portUuid));
            } else {
                LOG.warn("Unable to delete port {} because it was not found in the operational store, "
                        + "and thus we cannot retrieve its UUID", terminationPointUuidOptional.get());
            }
        }
    }

    private Optional<UUID> getTerminationPointUUID(InstanceIdentifier<OvsdbTerminationPointAugmentation> iid) {

        Optional<UUID> result = Optional.absent();
        ReadOnlyTransaction transaction = db.newReadOnlyTransaction();
        CheckedFuture<Optional<OvsdbTerminationPointAugmentation>, ReadFailedException> future
            = transaction.read(LogicalDatastoreType.OPERATIONAL, iid);
        Optional<OvsdbTerminationPointAugmentation> optional;
        try {
            optional = future.get();
            if (optional.isPresent()) {
                OvsdbTerminationPointAugmentation terminationPoint
                    = (OvsdbTerminationPointAugmentation) optional.get();
                if (terminationPoint != null && terminationPoint.getPortUuid() != null) {
                    result = Optional.of(new UUID(terminationPoint.getPortUuid().getValue()));
                }
            }
        } catch (InterruptedException e) {
            LOG.warn("Unable to retrieve termination point from operational store",e);
        } catch (ExecutionException e) {
            LOG.warn("Unable to retrieve termination point from operational store",e);
        }
        transaction.close();
        return result;
    }

    private Optional<String> getBridgeName(InstanceIdentifier<OvsdbTerminationPointAugmentation> iid) {

        Optional<String> result = Optional.absent();
        ReadOnlyTransaction transaction = db.newReadOnlyTransaction();
        InstanceIdentifier<Node> tpOvsdbManagedNodeIid = iid.firstIdentifierOf(Node.class);
        CheckedFuture<Optional<Node>, ReadFailedException> future
            = transaction.read(LogicalDatastoreType.OPERATIONAL, tpOvsdbManagedNodeIid);
        Optional<Node> optional;
        try {
            optional = future.get();
            if (optional.isPresent()) {
                OvsdbBridgeAugmentation bridge = optional.get().getAugmentation(OvsdbBridgeAugmentation.class);
                if (bridge != null && bridge.getBridgeName() != null) {
                    result = Optional.of(bridge.getBridgeName().getValue());
                }
            }
        } catch (InterruptedException e) {
            LOG.warn("Unable to retrieve bridge from operational store",e);
        } catch (ExecutionException e) {
            LOG.warn("Unable to retrieve bridge from operational store",e);
        }
        transaction.close();
        return result;
    }
}
