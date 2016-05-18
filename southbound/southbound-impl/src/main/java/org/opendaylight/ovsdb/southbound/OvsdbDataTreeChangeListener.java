/*
 * Copyright © 2016 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.southbound;

import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nonnull;

import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.southbound.ovsdb.transact.BridgeOperationalState;
import org.opendaylight.ovsdb.southbound.ovsdb.transact.TransactCommandAggregator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Data-tree change listener for OVSDB.
 */
public class OvsdbDataTreeChangeListener implements ClusteredDataTreeChangeListener<Node>, AutoCloseable {

    /** Our registration. */
    private final ListenerRegistration<OvsdbDataTreeChangeListener> registration;

    /** The connection manager. */
    private final OvsdbConnectionManager cm;

    /** The data broker. */
    private final DataBroker db;

    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbDataTreeChangeListener.class);

    /**
     * Create an instance and register the listener.
     *
     * @param db The data broker.
     * @param cm The connection manager.
     */
    OvsdbDataTreeChangeListener(DataBroker db, OvsdbConnectionManager cm) {
        LOG.info("Registering OvsdbNodeDataChangeListener");
        this.cm = cm;
        this.db = db;
        InstanceIdentifier<Node> path = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class);
        DataTreeIdentifier<Node> dataTreeIdentifier =
                new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, path);
        registration = db.registerDataTreeChangeListener(dataTreeIdentifier, this);
    }

    @Override
    public void close() {
        registration.close();
    }

    @Override
    public void onDataTreeChanged(@Nonnull Collection<DataTreeModification<Node>> changes) {
        LOG.trace("onDataTreeChanged: {}", changes);

        // Connect first if necessary
        connect(changes);

        // Update connections if necessary
        updateConnections(changes);

        // Update the actual data
        updateData(changes);

        // Disconnect if necessary
        disconnect(changes);

        LOG.trace("onDataTreeChanged: exit");
    }

    private void connect(@Nonnull Collection<DataTreeModification<Node>> changes) {
        for (DataTreeModification<Node> change : changes) {
            if (change.getRootNode().getModificationType() == DataObjectModification.ModificationType.WRITE || change
                    .getRootNode().getModificationType() == DataObjectModification.ModificationType.SUBTREE_MODIFIED) {
                DataObjectModification<OvsdbNodeAugmentation> ovsdbNodeModification =
                        change.getRootNode().getModifiedAugmentation(OvsdbNodeAugmentation.class);
                if (ovsdbNodeModification != null && ovsdbNodeModification.getDataBefore() == null
                        && ovsdbNodeModification.getDataAfter() != null) {
                    OvsdbNodeAugmentation ovsdbNode = ovsdbNodeModification.getDataAfter();
                    ConnectionInfo key = ovsdbNode.getConnectionInfo();
                    InstanceIdentifier<Node> iid = cm.getInstanceIdentifier(key);
                    if ( iid != null) {
                        LOG.warn("Connection to device {} already exists. Plugin does not allow multiple connections "
                                + "to same device, hence dropping the request {}", key, ovsdbNode);
                    } else {
                        try {
                            InstanceIdentifier<Node> instanceIdentifier = change.getRootPath().getRootIdentifier();
                            LOG.info("Connecting on key {} to {}", instanceIdentifier, ovsdbNode);
                            cm.connect(instanceIdentifier, ovsdbNode);
                        } catch (UnknownHostException e) {
                            LOG.warn("Failed to connect to ovsdbNode", e);
                        }
                    }
                }
            }
        }
    }

    private void disconnect(@Nonnull Collection<DataTreeModification<Node>> changes) {
        for (DataTreeModification<Node> change : changes) {
            if (change.getRootNode().getModificationType() == DataObjectModification.ModificationType.DELETE) {
                DataObjectModification<OvsdbNodeAugmentation> ovsdbNodeModification =
                        change.getRootNode().getModifiedAugmentation(OvsdbNodeAugmentation.class);
                if (ovsdbNodeModification != null && ovsdbNodeModification.getDataBefore() != null) {
                    OvsdbNodeAugmentation ovsdbNode = ovsdbNodeModification.getDataBefore();
                    ConnectionInfo key = ovsdbNode.getConnectionInfo();
                    InstanceIdentifier<Node> iid = cm.getInstanceIdentifier(key);
                    try {
                        LOG.info("Disconnecting from {}", ovsdbNode);
                        cm.disconnect(ovsdbNode);
                        cm.stopConnectionReconciliationIfActive(iid.firstIdentifierOf(Node.class), ovsdbNode);
                    } catch (UnknownHostException e) {
                        LOG.warn("Failed to disconnect ovsdbNode", e);
                    }
                }
            }
        }
    }

    private void updateConnections(@Nonnull Collection<DataTreeModification<Node>> changes) {
        for (DataTreeModification<Node> change : changes) {
            if (change.getRootNode().getModificationType() == DataObjectModification.ModificationType.WRITE || change
                    .getRootNode().getModificationType() == DataObjectModification.ModificationType.SUBTREE_MODIFIED) {
                DataObjectModification<OvsdbNodeAugmentation> ovsdbNodeModification =
                        change.getRootNode().getModifiedAugmentation(OvsdbNodeAugmentation.class);
                if (ovsdbNodeModification != null && ovsdbNodeModification.getDataBefore() != null
                        && ovsdbNodeModification.getDataAfter() != null) {
                    OvsdbClient client = cm.getClient(ovsdbNodeModification.getDataAfter().getConnectionInfo());
                    if (client == null) {
                        if (ovsdbNodeModification.getDataBefore() != null) {
                            try {
                                cm.disconnect(ovsdbNodeModification.getDataBefore());
                                cm.connect(change.getRootPath().getRootIdentifier(), ovsdbNodeModification
                                        .getDataAfter());
                            } catch (UnknownHostException e) {
                                LOG.warn("Error disconnecting from or connecting to ovsdbNode", e);
                            }
                        }
                    }
                }
            }
        }
    }

    private void updateData(@Nonnull Collection<DataTreeModification<Node>> changes) {
        for (Entry<InstanceIdentifier<Node>, OvsdbConnectionInstance> connectionInstanceEntry :
                connectionInstancesFromChanges(changes).entrySet()) {
            OvsdbConnectionInstance connectionInstance = connectionInstanceEntry.getValue();
            connectionInstance.transact(new TransactCommandAggregator(),
                    new BridgeOperationalState(db, changes), changes);
        }
    }

    private Map<InstanceIdentifier<Node>, OvsdbConnectionInstance> connectionInstancesFromChanges(
            @Nonnull Collection<DataTreeModification<Node>> changes) {
        Map<InstanceIdentifier<Node>,OvsdbConnectionInstance> result =
                new HashMap<>();
        for (DataTreeModification<Node> change : changes) {
            DataObjectModification<OvsdbBridgeAugmentation> bridgeModification =
                    change.getRootNode().getModifiedAugmentation(OvsdbBridgeAugmentation.class);
            OvsdbConnectionInstance client = null;
            Node node = change.getRootNode().getDataAfter();
            if (bridgeModification != null && bridgeModification.getDataAfter() != null) {
                client = cm.getConnectionInstance(bridgeModification.getDataAfter());
            } else {
                DataObjectModification<OvsdbNodeAugmentation> nodeModification =
                        change.getRootNode().getModifiedAugmentation(OvsdbNodeAugmentation.class);
                if (nodeModification != null && nodeModification.getDataAfter() != null && nodeModification
                        .getDataAfter().getConnectionInfo() != null) {
                    client = cm.getConnectionInstance(nodeModification.getDataAfter().getConnectionInfo());
                } else {
                    if (node != null) {
                        List<TerminationPoint> terminationPoints = node.getTerminationPoint();
                        if (terminationPoints != null && !terminationPoints.isEmpty()) {
                            InstanceIdentifier<Node> nodeIid = SouthboundMapper.createInstanceIdentifier(
                                    node.getNodeId());
                            client = cm.getConnectionInstance(nodeIid);
                        }
                    }
                }
            }
            if (client != null) {
                LOG.debug("Found client for {}", node);
                    /*
                     * As of now data change sets are processed by single thread, so we can assume that device will
                     * be connected and ownership will be decided before sending any instructions down to the device.
                     * Note:Processing order in onDataChange() method should not change. If processing is changed to
                     * use multiple thread, we might need to take care of corner cases, where ownership is not decided
                     * but transaction are ready to go to switch. In that scenario, either we need to queue those task
                     * till ownership is decided for that specific device.
                     * Given that each DataChangeNotification is notified through separate thread, so we are already
                     * multi threaded and i don't see any need to further parallelism per DataChange
                     * notifications processing.
                     */
                if ( cm.getHasDeviceOwnership(client.getMDConnectionInfo())) {
                    LOG.debug("*This* instance of southbound plugin is an owner of the device {}", node);
                    result.put(change.getRootPath().getRootIdentifier(), client);
                } else {
                    LOG.debug("*This* instance of southbound plugin is *not* an owner of the device {}", node);
                }
            } else {
                LOG.debug("Did not find client for {}", node);
            }
        }
        return result;
    }
}
