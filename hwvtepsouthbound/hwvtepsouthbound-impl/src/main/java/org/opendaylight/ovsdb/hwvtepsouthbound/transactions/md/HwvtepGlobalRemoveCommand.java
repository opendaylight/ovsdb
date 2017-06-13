/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound.transactions.md;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepConnectionInstance;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.Managers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.Switches;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

public class HwvtepGlobalRemoveCommand extends AbstractTransactionCommand {
    private static final Logger LOG = LoggerFactory.getLogger(HwvtepGlobalRemoveCommand.class);
    private static final long ONE_CONNECTED_MANAGER = 1;
    private static final long ONE_ACTIVE_CONNECTION_IN_PASSIVE_MODE = 1;
    private final InstanceIdentifier<Node> nodeInstanceIdentifier;

    public HwvtepGlobalRemoveCommand(HwvtepConnectionInstance key, TableUpdates updates, DatabaseSchema dbSchema) {
        super(key, updates, dbSchema);
        this.nodeInstanceIdentifier = key.getInstanceIdentifier();
    }

    public HwvtepGlobalRemoveCommand(InstanceIdentifier<Node> nodeInstanceIdentifier) {
        super(null, null, null);
        this.nodeInstanceIdentifier = nodeInstanceIdentifier;
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        CheckedFuture<Optional<Node>, ReadFailedException> hwvtepGlobalFuture = transaction.read(
                LogicalDatastoreType.OPERATIONAL, nodeInstanceIdentifier);
        try {
            Optional<Node> hwvtepGlobalOptional = hwvtepGlobalFuture.get();
            if (hwvtepGlobalOptional.isPresent()) {
                Node hwvtepNode = hwvtepGlobalOptional.get();
                HwvtepGlobalAugmentation hgAugmentation = hwvtepNode.getAugmentation(HwvtepGlobalAugmentation.class);
                if (checkIfOnlyConnectedManager(hgAugmentation)) {
                    if (hgAugmentation != null) {
                        if (hgAugmentation.getSwitches() != null) {
                            for (Switches hwSwitch : hgAugmentation.getSwitches()) {
                                LOG.debug("Deleting hwvtep switch {}", hwSwitch);
                                transaction.delete(
                                        LogicalDatastoreType.OPERATIONAL, hwSwitch.getSwitchRef().getValue());
                            }
                        } else {
                            LOG.debug("{} had no switches", hwvtepNode.getNodeId().getValue());
                        }
                    } else {
                        LOG.warn("{} had no HwvtepGlobalAugmentation", hwvtepNode.getNodeId().getValue());
                    }
                    transaction.delete(LogicalDatastoreType.OPERATIONAL, nodeInstanceIdentifier);
                } else {
                    LOG.debug("Other southbound plugin instances in cluster are connected to the device,"
                            + " not deleting OvsdbNode form data store.");
                }
            }
        } catch (Exception e) {
            LOG.warn("Failure to delete ovsdbNode", e);
        }
    }

    private boolean checkIfOnlyConnectedManager(HwvtepGlobalAugmentation hgAugmentation) {
        Managers onlyConnectedManager = null;
        if (hgAugmentation != null) {
            int connectedManager = 0;
            if (hgAugmentation.getManagers() != null) {
                for (Managers manager : hgAugmentation.getManagers()) {
                    if (manager.isIsConnected()) {
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
        }
        /*When switch is listening in passive mode, this number represent number of active connection to the device
        This is to handle the controller initiated connection scenario, where all the controller will connect, but
        switch will have only one manager.
        */
        /* CLUSTERING-TODO-ITEM: For hwvtep we don't have getNumberOfConnections()
         * FIXME: Add it to yang?
        if (onlyConnectedManager.getNumberOfConnections() > ONE_ACTIVE_CONNECTION_IN_PASSIVE_MODE) {
            return false;
        }*/
        return true;
    }
}
