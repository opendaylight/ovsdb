/*
 * Copyright (c) 2015, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataObjectModification.ModificationType;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.ovsdb.hwvtepsouthbound.transact.HwvtepOperationalState;
import org.opendaylight.ovsdb.hwvtepsouthbound.transact.TransactCommandAggregator;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HwvtepDataChangeListener implements ClusteredDataTreeChangeListener<Node>, AutoCloseable {

    private ListenerRegistration<HwvtepDataChangeListener> registration;
    private final HwvtepConnectionManager hcm;
    private final DataBroker db;
    private static final Logger LOG = LoggerFactory.getLogger(HwvtepDataChangeListener.class);

    HwvtepDataChangeListener(DataBroker db, HwvtepConnectionManager hcm) {
        LOG.info("Registering HwvtepDataChangeListener");
        this.db = db;
        this.hcm = hcm;
        registerListener();
    }

    private void registerListener() {
        final DataTreeIdentifier<Node> treeId =
                DataTreeIdentifier.create(LogicalDatastoreType.CONFIGURATION, getWildcardPath());

        LOG.trace("Registering on path: {}", treeId);
        registration = db.registerDataTreeChangeListener(treeId, HwvtepDataChangeListener.this);
    }

    @Override
    public void close() {
        if (registration != null) {
            registration.close();
        }
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<Node>> changes) {
        LOG.trace("onDataTreeChanged: {}", changes);

        /* TODO:
         * Currently only handling changes to Global.
         * Rest will be added later.
         */
        connect(changes);

        updateConnections(changes);

        updateData(changes);

        disconnect(changes);

        disconnectViaCli(changes);
    }

    private void connect(Collection<DataTreeModification<Node>> changes) {
        for (DataTreeModification<Node> change : changes) {
            final InstanceIdentifier<Node> key = change.getRootPath().getRootIdentifier();
            final DataObjectModification<Node> mod = change.getRootNode();
            Node node = getCreated(mod);
            if (node != null) {
                HwvtepGlobalAugmentation hwvtepGlobal = node.augmentation(HwvtepGlobalAugmentation.class);
                // We can only connect if user configured connection info
                if (hwvtepGlobal != null && hwvtepGlobal.getConnectionInfo() != null) {
                    ConnectionInfo connection = hwvtepGlobal.getConnectionInfo();
                    InstanceIdentifier<Node> iid = hcm.getInstanceIdentifier(connection);
                    if (iid != null) {
                        LOG.warn("Connection to device {} already exists. Plugin does not allow multiple connections "
                                        + "to same device, hence dropping the request {}", connection, hwvtepGlobal);
                    } else {
                        try {
                            hcm.connect(key, hwvtepGlobal);
                        } catch (UnknownHostException | ConnectException e) {
                            LOG.warn("Failed to connect to HWVTEP node", e);
                        }
                    }
                }
            }
        }
    }

    private void updateConnections(Collection<DataTreeModification<Node>> changes) {
        for (DataTreeModification<Node> change : changes) {
            final InstanceIdentifier<Node> key = change.getRootPath().getRootIdentifier();
            final DataObjectModification<Node> mod = change.getRootNode();
            Node updated = getUpdated(mod);
            if (updated != null) {
                Node original = getOriginal(mod);
                HwvtepGlobalAugmentation hgUpdated = updated.augmentation(HwvtepGlobalAugmentation.class);
                HwvtepGlobalAugmentation hgOriginal = original.augmentation(HwvtepGlobalAugmentation.class);
                // Check if user has updated connection information
                if (hgUpdated != null && hgOriginal != null && hgUpdated.getConnectionInfo() != null
                                && !hgUpdated.getConnectionInfo().equals(hgOriginal.getConnectionInfo())) {
                    OvsdbClient client = hcm.getClient(hgUpdated.getConnectionInfo());
                    if (client == null) {
                        try {
                            hcm.disconnect(hgOriginal);
                            hcm.stopConnectionReconciliationIfActive(key, hgOriginal);
                            OvsdbClient newClient = hcm.connect(key, hgUpdated);
                            if (newClient == null) {
                                hcm.reconcileConnection(key, hgUpdated);
                            }
                        } catch (UnknownHostException | ConnectException e) {
                            LOG.warn("Failed to update connection on HWVTEP Node", e);
                        }
                    }
                }
            }
        }
    }

    private void updateData(Collection<DataTreeModification<Node>> changes) {
        /* TODO:
         * Get connection instances for each change
         * Update data for each connection
         * Requires Command patterns. TBD.
         */
        for (Entry<HwvtepConnectionInstance, Collection<DataTreeModification<Node>>> changesEntry :
                changesByConnectionInstance(changes).entrySet()) {
            HwvtepConnectionInstance connectionInstance = changesEntry.getKey();
            connectionInstance.transact(new TransactCommandAggregator(
                new HwvtepOperationalState(db, connectionInstance, changesEntry.getValue()),changesEntry.getValue()));
            connectionInstance.getDeviceInfo().onConfigDataAvailable();
        }
    }

    private void disconnect(Collection<DataTreeModification<Node>> changes) {
        for (DataTreeModification<Node> change : changes) {
            final InstanceIdentifier<Node> key = change.getRootPath().getRootIdentifier();
            final DataObjectModification<Node> mod = change.getRootNode();
            Node deleted = getRemoved(mod);
            if (deleted != null) {
                HwvtepGlobalAugmentation hgDeleted = deleted.augmentation(HwvtepGlobalAugmentation.class);
                if (hgDeleted != null) {
                    try {
                        hcm.disconnect(hgDeleted);
                        hcm.stopConnectionReconciliationIfActive(key, hgDeleted);
                    } catch (UnknownHostException e) {
                        LOG.warn("Failed to disconnect HWVTEP Node", e);
                    }
                }
            }
        }
    }

    private static Node getCreated(DataObjectModification<Node> mod) {
        if (mod.getModificationType() == ModificationType.WRITE && mod.getDataBefore() == null) {
            return mod.getDataAfter();
        }
        return null;
    }

    private static Node getRemoved(DataObjectModification<Node> mod) {
        if (mod.getModificationType() == ModificationType.DELETE) {
            return mod.getDataBefore();
        }
        return null;
    }

    private static Node getUpdated(DataObjectModification<Node> mod) {
        Node node = null;
        switch (mod.getModificationType()) {
            case SUBTREE_MODIFIED:
                node = mod.getDataAfter();
                break;
            case WRITE:
                if (mod.getDataBefore() != null) {
                    node = mod.getDataAfter();
                }
                break;
            default:
                break;
        }
        return node;
    }

    private static Node getOriginal(DataObjectModification<Node> mod) {
        Node node = null;
        switch (mod.getModificationType()) {
            case SUBTREE_MODIFIED:
            case DELETE:
                node = mod.getDataBefore();
                break;
            case WRITE:
                if (mod.getDataBefore() != null) {
                    node = mod.getDataBefore();
                }
                break;
            default:
                break;
        }
        return node;
    }

    private static InstanceIdentifier<Node> getWildcardPath() {
        return InstanceIdentifier.create(NetworkTopology.class)
                        .child(Topology.class, new TopologyKey(HwvtepSouthboundConstants.HWVTEP_TOPOLOGY_ID))
                        .child(Node.class);
    }

    private Map<HwvtepConnectionInstance, Collection<DataTreeModification<Node>>> changesByConnectionInstance(
            Collection<DataTreeModification<Node>> changes) {
        Map<HwvtepConnectionInstance, Collection<DataTreeModification<Node>>> result = new HashMap<>();
        for (DataTreeModification<Node> change : changes) {
            final DataObjectModification<Node> mod = change.getRootNode();
            //From original node to get connection instance
            Node node = mod.getDataBefore() != null ? mod.getDataBefore() : mod.getDataAfter();
            HwvtepConnectionInstance connection = hcm.getConnectionInstanceFromNodeIid(
                    change.getRootPath().getRootIdentifier());
            if (connection != null) {
                if (!result.containsKey(connection)) {
                    List<DataTreeModification<Node>> tempChanges = new ArrayList<>();
                    tempChanges.add(change);
                    result.put(connection, tempChanges);
                } else {
                    result.get(connection).add(change);
                }
            } else {
                LOG.warn("Failed to get the connection of changed node: {}", node.key().getNodeId().getValue());
            }
        }
        LOG.trace("Connection Change Map: {}", result);
        return result;
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void disconnectViaCli(Collection<DataTreeModification<Node>> changes) {
        for (DataTreeModification<Node> change : changes) {
            String nodeId = change.getRootPath().getRootIdentifier().firstKeyOf(Node.class).getNodeId().getValue();
            if (!nodeId.contains("/disconnect")) {
                continue;
            }
            int reconcileIndex = nodeId.indexOf("/disconnect");
            String globalNodeId = nodeId.substring(0, reconcileIndex);
            InstanceIdentifier<Node> globalNodeIid = change.getRootPath()
                .getRootIdentifier().firstIdentifierOf(Topology.class)
                .child(Node.class, new NodeKey(new NodeId(globalNodeId)));
            HwvtepConnectionInstance connectionInstance = hcm.getConnectionInstanceFromNodeIid(globalNodeIid);
            if (connectionInstance != null) {
                LOG.error("Disconnecting from controller {}", nodeId);
                new Thread(() -> {
                    ReadWriteTransaction tx = db.newReadWriteTransaction();
                    tx.delete(LogicalDatastoreType.CONFIGURATION, change.getRootPath().getRootIdentifier());
                    try {
                        tx.commit().get();
                    } catch (ExecutionException | InterruptedException e) {
                        LOG.error("Failed to delete the node {}", change.getRootPath().getRootIdentifier());
                    }
                }).start();
                try {
                    connectionInstance.disconnect();
                } catch (Exception e) {
                    LOG.debug("Failed to disconnect");
                }
            }
        }
    }
}
