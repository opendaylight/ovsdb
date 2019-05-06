/*
 * Copyright © 2016 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.southbound;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Data-tree change listener for OVSDB.
 */
public class OvsdbDataTreeChangeListener implements ClusteredDataTreeChangeListener<Node>, AutoCloseable {

    /** Our registration. */
    private final ListenerRegistration<DataTreeChangeListener<Node>> registration;

    /** The connection manager. */
    private final OvsdbConnectionManager cm;

    /** The data broker. */
    private final DataBroker db;

    /** The instance identifier codec. */
    private final InstanceIdentifierCodec instanceIdentifierCodec;

    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbDataTreeChangeListener.class);

    /**
     * Create an instance and register the listener.
     *
     * @param db The data broker.
     * @param cm The connection manager.
     */
    OvsdbDataTreeChangeListener(DataBroker db, OvsdbConnectionManager cm,
            InstanceIdentifierCodec instanceIdentifierCodec) {
        this.cm = cm;
        this.db = db;
        this.instanceIdentifierCodec = instanceIdentifierCodec;
        InstanceIdentifier<Node> path = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class);
        DataTreeIdentifier<Node> dataTreeIdentifier =
                new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, path);
        registration = db.registerDataTreeChangeListener(dataTreeIdentifier, this);
        LOG.info("OVSDB topology listener has been registered.");
    }

    @Override
    public void close() {
        registration.close();
        LOG.info("OVSDB topology listener has been closed.");
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
                if (ovsdbNodeModification != null && ovsdbNodeModification.getDataBefore() == null) {
                    OvsdbNodeAugmentation ovsdbNode = ovsdbNodeModification.getDataAfter();
                    if (ovsdbNode != null) {
                        ConnectionInfo key = ovsdbNode.getConnectionInfo();
                        if (key != null) {
                            InstanceIdentifier<Node> iid = cm.getInstanceIdentifier(key);
                            if (iid != null) {
                                LOG.warn("Connection to device {} already exists. Plugin does not allow multiple "
                                        + "connections to same device, hence dropping the request {}", key, ovsdbNode);
                            } else {
                                try {
                                    cm.connect(change.getRootPath().getRootIdentifier(), ovsdbNode);
                                    LOG.info("OVSDB node has been connected: {}",ovsdbNode);
                                } catch (UnknownHostException | ConnectException e) {
                                    LOG.warn("Failed to connect to ovsdbNode", e);
                                }
                            }
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
                if (ovsdbNodeModification != null) {
                    OvsdbNodeAugmentation ovsdbNode = ovsdbNodeModification.getDataBefore();
                    if (ovsdbNode != null) {
                        ConnectionInfo key = ovsdbNode.getConnectionInfo();
                        InstanceIdentifier<Node> iid = cm.getInstanceIdentifier(key);
                        try {
                            cm.disconnect(ovsdbNode);
                            LOG.info("OVSDB node has been disconnected:{}", ovsdbNode);
                            cm.stopConnectionReconciliationIfActive(iid.firstIdentifierOf(Node.class), ovsdbNode);
                        } catch (UnknownHostException e) {
                            LOG.warn("Failed to disconnect ovsdbNode", e);
                        }
                    }
                }
            }

            if (change.getRootNode().getModificationType() == DataObjectModification.ModificationType.WRITE) {
                DataObjectModification<OvsdbNodeAugmentation> ovsdbNodeModification =
                        change.getRootNode().getModifiedAugmentation(OvsdbNodeAugmentation.class);
                if (ovsdbNodeModification != null) {
                    DataObjectModification<ConnectionInfo> connectionInfoDOM =
                            ovsdbNodeModification.getModifiedChildContainer(ConnectionInfo.class);
                    if (connectionInfoDOM != null) {
                        if (connectionInfoDOM.getModificationType() == DataObjectModification.ModificationType.DELETE) {
                            ConnectionInfo key = connectionInfoDOM.getDataBefore();
                            if (key != null) {
                                InstanceIdentifier<Node> iid = cm.getInstanceIdentifier(key);
                                try {
                                    OvsdbNodeAugmentation ovsdbNode = ovsdbNodeModification.getDataBefore();
                                    cm.disconnect(ovsdbNode);
                                    LOG.warn("OVSDB node {} has been disconnected, because connection-info related to "
                                            + "the node is removed by user, but node still exist.", ovsdbNode);
                                    cm.stopConnectionReconciliationIfActive(iid.firstIdentifierOf(Node.class),
                                        ovsdbNode);
                                } catch (UnknownHostException e) {
                                    LOG.warn("Failed to disconnect ovsdbNode", e);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void updateConnections(@Nonnull Collection<DataTreeModification<Node>> changes) {
        for (DataTreeModification<Node> change : changes) {
            switch (change.getRootNode().getModificationType()) {
                case SUBTREE_MODIFIED:
                case WRITE:
                    DataObjectModification<OvsdbNodeAugmentation> ovsdbNodeModification =
                        change.getRootNode().getModifiedAugmentation(OvsdbNodeAugmentation.class);
                    if (ovsdbNodeModification != null) {
                        final OvsdbNodeAugmentation dataBefore = ovsdbNodeModification.getDataBefore();
                        if (dataBefore != null) {
                            OvsdbNodeAugmentation dataAfter = ovsdbNodeModification.getDataAfter();
                            if (dataAfter != null) {
                                ConnectionInfo connectionInfo = dataAfter.getConnectionInfo();
                                if (connectionInfo != null) {
                                    OvsdbClient client = cm.getClient(connectionInfo);
                                    if (client == null) {
                                        if (dataBefore != null) {
                                            try {
                                                cm.disconnect(dataBefore);
                                                cm.connect(change.getRootPath().getRootIdentifier(), dataAfter);
                                            } catch (UnknownHostException | ConnectException e) {
                                                LOG.warn("Error disconnecting from or connecting to ovsdbNode", e);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    break;
                default:
                    // FIXME: delete seems to be unhandled
                    break;
            }
        }
    }

    private void updateData(@Nonnull Collection<DataTreeModification<Node>> changes) {
        for (Entry<OvsdbConnectionInstance, Collection<DataTreeModification<Node>>> connectionInstanceEntry :
                changesPerConnectionInstance(changes).entrySet()) {
            OvsdbConnectionInstance connectionInstance = connectionInstanceEntry.getKey();
            Collection<DataTreeModification<Node>> clientChanges = connectionInstanceEntry.getValue();
            connectionInstance.transact(new TransactCommandAggregator(),
                    new BridgeOperationalState(db, clientChanges), clientChanges, instanceIdentifierCodec);
        }
    }

    private Map<OvsdbConnectionInstance, Collection<DataTreeModification<Node>>> changesPerConnectionInstance(
            @Nonnull Collection<DataTreeModification<Node>> changes) {
        Map<OvsdbConnectionInstance, Collection<DataTreeModification<Node>>> result = new HashMap<>();
        for (DataTreeModification<Node> change : changes) {
            OvsdbConnectionInstance client = null;
            Node dataAfter = change.getRootNode().getDataAfter();
            Node node =  dataAfter != null ? dataAfter : change.getRootNode().getDataBefore();
            if (node != null) {
                OvsdbNodeAugmentation ovsdbNode = node.augmentation(OvsdbNodeAugmentation.class);
                if (ovsdbNode != null) {
                    if (ovsdbNode.getConnectionInfo() != null) {
                        client = cm.getConnectionInstance(ovsdbNode.getConnectionInfo());
                    } else {
                        client = cm.getConnectionInstance(SouthboundMapper.createInstanceIdentifier(node.getNodeId()));
                    }
                } else {
                    OvsdbBridgeAugmentation bridgeAugmentation = node.augmentation(OvsdbBridgeAugmentation.class);
                    if (bridgeAugmentation != null) {
                        OvsdbNodeRef managedBy = bridgeAugmentation.getManagedBy();
                        if (managedBy != null) {
                            client = cm.getConnectionInstance((InstanceIdentifier<Node>) managedBy.getValue());
                        }
                    }
                }

                if (client == null) {
                    //Try getting from change root identifier
                    client = cm.getConnectionInstance(change.getRootPath().getRootIdentifier());
                }
            } else {
                LOG.warn("Following change don't have after/before data {}", change);
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
                if (cm.getHasDeviceOwnership(client.getMDConnectionInfo())) {
                    LOG.debug("*This* instance of southbound plugin is an owner of the device {}", node);
                    result.computeIfAbsent(client, key -> new ArrayList<>()).add(change);
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
