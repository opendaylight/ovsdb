/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.southbound.transactions.md;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.FluentFuture;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionInstance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagedNodeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagedNodeEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagerEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagerEntryKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsdbNodeRemoveCommand extends AbstractTransactionCommand {
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbNodeRemoveCommand.class);
    private static final long ONE_CONNECTED_MANAGER = 1;
    private static final long ONE_ACTIVE_CONNECTION_IN_PASSIVE_MODE = 1;

    public OvsdbNodeRemoveCommand(OvsdbConnectionInstance key,TableUpdates updates,DatabaseSchema dbSchema) {
        super(key,updates,dbSchema);
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        FluentFuture<Optional<Node>> ovsdbNodeFuture = transaction.read(
                LogicalDatastoreType.OPERATIONAL, getOvsdbConnectionInstance().getInstanceIdentifier());
        Optional<Node> ovsdbNodeOptional;
        try {
            ovsdbNodeOptional = ovsdbNodeFuture.get();
            if (ovsdbNodeOptional.isPresent()) {
                Node ovsdbNode = ovsdbNodeOptional.orElseThrow();
                OvsdbNodeAugmentation ovsdbNodeAugmentation = ovsdbNode.augmentation(OvsdbNodeAugmentation.class);
                if (checkIfOnlyConnectedManager(ovsdbNodeAugmentation)) {
                    if (ovsdbNodeAugmentation != null) {
                        Map<ManagedNodeEntryKey, ManagedNodeEntry> entries =
                                ovsdbNodeAugmentation.getManagedNodeEntry();
                        if (entries != null) {
                            for (ManagedNodeEntry managedNode : entries.values()) {
                                transaction.delete(
                                        LogicalDatastoreType.OPERATIONAL, managedNode.getBridgeRef().getValue());
                            }
                        } else {
                            LOG.debug("{} had no managed nodes", ovsdbNode.getNodeId().getValue());
                        }
                    } else {
                        LOG.warn("{} had no OvsdbNodeAugmentation", ovsdbNode.getNodeId().getValue());
                    }
                    LOG.debug("Deleting node {} from oper", getOvsdbConnectionInstance().getInstanceIdentifier());
                    transaction.delete(LogicalDatastoreType.OPERATIONAL,
                            getOvsdbConnectionInstance().getInstanceIdentifier());
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
    boolean checkIfOnlyConnectedManager(OvsdbNodeAugmentation ovsdbNodeAugmentation) {
        ManagerEntry onlyConnectedManager = null;
        if (ovsdbNodeAugmentation != null) {
            int connectedManager = 0;
            final Map<ManagerEntryKey, ManagerEntry> entries = ovsdbNodeAugmentation.getManagerEntry();
            if (entries != null) {
                for (ManagerEntry manager : entries.values()) {
                    if (manager.getConnected()) {
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
