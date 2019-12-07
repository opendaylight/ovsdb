/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.southbound.transactions.md;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import java.util.concurrent.ExecutionException;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagedNodeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagerEntry;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsdbNodeRemoveCommand implements TransactionCommand {
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbNodeRemoveCommand.class);
    private static final long ONE_CONNECTED_MANAGER = 1;
    private static final long ONE_ACTIVE_CONNECTION_IN_PASSIVE_MODE = 1;

    private final InstanceIdentifier<Node> node;

    public OvsdbNodeRemoveCommand(final InstanceIdentifier<Node> node) {
        this.node = requireNonNull(node);
    }

    @Override
    public void execute(final ReadWriteTransaction transaction) {
        CheckedFuture<Optional<Node>, ReadFailedException> ovsdbNodeFuture = transaction.read(
                LogicalDatastoreType.OPERATIONAL, node);
        Optional<Node> ovsdbNodeOptional;
        try {
            ovsdbNodeOptional = ovsdbNodeFuture.get();
            if (ovsdbNodeOptional.isPresent()) {
                Node ovsdbNode = ovsdbNodeOptional.get();
                OvsdbNodeAugmentation ovsdbNodeAugmentation = ovsdbNode.augmentation(OvsdbNodeAugmentation.class);
                if (checkIfOnlyConnectedManager(ovsdbNodeAugmentation)) {
                    if (ovsdbNodeAugmentation != null) {
                        if (ovsdbNodeAugmentation.getManagedNodeEntry() != null) {
                            for (ManagedNodeEntry managedNode : ovsdbNodeAugmentation.getManagedNodeEntry()) {
                                transaction.delete(
                                        LogicalDatastoreType.OPERATIONAL, managedNode.getBridgeRef().getValue());
                            }
                        } else {
                            LOG.debug("{} had no managed nodes", ovsdbNode.getNodeId().getValue());
                        }
                    } else {
                        LOG.warn("{} had no OvsdbNodeAugmentation", ovsdbNode.getNodeId().getValue());
                    }
                    LOG.debug("Deleting node {} from oper", node);
                    transaction.delete(LogicalDatastoreType.OPERATIONAL, node);
                } else {
                    LOG.debug("Other southbound plugin instances in cluster are connected to the device,"
                            + " not deleting OvsdbNode from operational data store.");
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Failure to delete ovsdbNode", e);
        }
    }

    @VisibleForTesting
    boolean checkIfOnlyConnectedManager(final OvsdbNodeAugmentation ovsdbNodeAugmentation) {
        ManagerEntry onlyConnectedManager = null;
        if (ovsdbNodeAugmentation != null) {
            int connectedManager = 0;
            if (ovsdbNodeAugmentation.getManagerEntry() != null) {
                for (ManagerEntry manager : ovsdbNodeAugmentation.getManagerEntry()) {
                    if (manager.isConnected()) {
                        connectedManager++;
                        if (connectedManager > ONE_CONNECTED_MANAGER) {
                            return false;
                        }
                        onlyConnectedManager = manager;
                    }
                }
            }
            if (connectedManager == 0) {
                return true;
            }

            /*When switch is listening in passive mode, this number represent number of active connection to the device
            This is to handle the controller initiated connection scenario, where all the controller will connect, but
            switch will have only one manager.
            */
            if (onlyConnectedManager.getNumberOfConnections().toJava() > ONE_ACTIVE_CONNECTION_IN_PASSIVE_MODE) {
                return false;
            }
        }
        return true;
    }
}
