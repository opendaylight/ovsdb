/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification.ModificationType;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.hwvtepsouthbound.transact.HwvtepOperationalState;
import org.opendaylight.ovsdb.hwvtepsouthbound.transact.TransactCommandAggregator;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class HwvtepDataChangeListener implements ClusteredDataTreeChangeListener<Node>, AutoCloseable {

    private ListenerRegistration<HwvtepDataChangeListener> registration;
    private HwvtepConnectionManager hcm;
    private DataBroker db;
    private static final Logger LOG = LoggerFactory.getLogger(HwvtepDataChangeListener.class);

    HwvtepDataChangeListener(DataBroker db, HwvtepConnectionManager hcm) {
        LOG.info("Registering HwvtepDataChangeListener");
        this.db = db;
        this.hcm = hcm;
        registerListener(db);
    }

    private void registerListener(final DataBroker db) {
        final DataTreeIdentifier<Node> treeId =
                        new DataTreeIdentifier<Node>(LogicalDatastoreType.CONFIGURATION, getWildcardPath());
        try {
            LOG.trace("Registering on path: {}", treeId);
            registration = db.registerDataTreeChangeListener(treeId, HwvtepDataChangeListener.this);
        } catch (final Exception e) {
            LOG.warn("HwvtepDataChangeListener registration failed");
            //TODO: Should we throw an exception here?
        }
    }

    @Override
    public void close() throws Exception {
        if(registration != null) {
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
        /*
        for (DataTreeModification<Node> change : changes) {
            final InstanceIdentifier<Node> key = change.getRootPath().getRootIdentifier();
            final DataObjectModification<Node> mod = change.getRootNode();
                switch (mod.getModificationType()) {
                case DELETE:
                    LOG.trace("Data deleted: {}", mod.getDataBefore());
                    //disconnect(mod);
                    break;
                case SUBTREE_MODIFIED:
                    LOG.trace("Data modified: {} to {}", mod.getDataBefore(),mod.getDataAfter());
                    updateConnections(mod);
                    break;
                case WRITE:
                    if (mod.getDataBefore() == null) {
                        LOG.trace("Data added: {}", mod.getDataAfter());
                        connect(mod.getDataAfter());
                    } else {
                        LOG.trace("Data modified: {} to {}", mod.getDataBefore(),mod.getDataAfter());
                        updateConnections(mod);
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unhandled modification type " + mod.getModificationType());
                }
        }
        */
    }

    private void connect(Collection<DataTreeModification<Node>> changes) {
        for (DataTreeModification<Node> change : changes) {
            final InstanceIdentifier<Node> key = change.getRootPath().getRootIdentifier();
            final DataObjectModification<Node> mod = change.getRootNode();
            Node node = getCreated(mod);
            if (node != null) {
                HwvtepGlobalAugmentation hwvtepGlobal = node.getAugmentation(HwvtepGlobalAugmentation.class);
                // We can only connect if user configured connection info
                if (hwvtepGlobal != null && hwvtepGlobal.getConnectionInfo() != null) {
                    ConnectionInfo connection = hwvtepGlobal.getConnectionInfo();
                    InstanceIdentifier<Node> iid = hcm.getInstanceIdentifier(connection);
                    if (iid != null) {
                        LOG.warn("Connection to device {} already exists. Plugin does not allow multiple connections "
                                        + "to same device, hence dropping the request {}", connection, hwvtepGlobal);
                    } else {
                        try {
                            hcm.connect(HwvtepSouthboundMapper.createInstanceIdentifier(node.getNodeId()), hwvtepGlobal);
                        } catch (UnknownHostException e) {
                            LOG.warn("Failed to connect to OVSDB node", e);
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
                HwvtepGlobalAugmentation hgUpdated = updated.getAugmentation(HwvtepGlobalAugmentation.class);
                HwvtepGlobalAugmentation hgOriginal = original.getAugmentation(HwvtepGlobalAugmentation.class);
                // Check if user has updated connection information
                if (hgUpdated != null && hgOriginal != null && hgUpdated.getConnectionInfo() != null) {
                    OvsdbClient client = hcm.getClient(hgUpdated.getConnectionInfo());
                    if (client == null) {
                        try {
                            hcm.disconnect(hgOriginal);
                            hcm.connect(HwvtepSouthboundMapper.createInstanceIdentifier(original.getNodeId()), hgUpdated);
                        } catch (UnknownHostException e) {
                            LOG.warn("Failed to update connection on OVSDB Node", e);
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
                new HwvtepOperationalState(db, changesEntry.getValue()),changesEntry.getValue()));
        }
    }

    private void disconnect(Collection<DataTreeModification<Node>> changes) {
        for (DataTreeModification<Node> change : changes) {
            final InstanceIdentifier<Node> key = change.getRootPath().getRootIdentifier();
            final DataObjectModification<Node> mod = change.getRootNode();
            Node deleted = getRemoved(mod);
            if (deleted != null) {
                HwvtepGlobalAugmentation hgDeleted = deleted.getAugmentation(HwvtepGlobalAugmentation.class);
                if (hgDeleted != null) {
                    try {
                        hcm.disconnect(hgDeleted);
                    } catch (UnknownHostException e) {
                        LOG.warn("Failed to disconnect OVSDB Node", e);
                    }
                }
            }
        }
    }

    private Node getCreated(DataObjectModification<Node> mod) {
        if((mod.getModificationType() == ModificationType.WRITE)
                        && (mod.getDataBefore() == null)){
            return mod.getDataAfter();
        }
        return null;
    }

    private Node getRemoved(DataObjectModification<Node> mod) {
        if(mod.getModificationType() == ModificationType.DELETE){
            return mod.getDataBefore();
        }
        return null;
    }

    private Node getUpdated(DataObjectModification<Node> mod) {
        Node node = null;
        switch(mod.getModificationType()) {
            case SUBTREE_MODIFIED:
                node = mod.getDataAfter();
                break;
            case WRITE:
                if(mod.getDataBefore() !=  null) {
                    node = mod.getDataAfter();
                }
                break;
            default:
                break;
        }
        return node;
    }

    private Node getOriginal(DataObjectModification<Node> mod) {
        Node node = null;
        switch(mod.getModificationType()) {
            case SUBTREE_MODIFIED:
                node = mod.getDataBefore();
                break;
            case WRITE:
                if(mod.getDataBefore() !=  null) {
                    node = mod.getDataBefore();
                }
                break;
            case DELETE:
                node = mod.getDataBefore();
                break;
            default:
                break;
        }
        return node;
    }

    private InstanceIdentifier<Node> getWildcardPath() {
        InstanceIdentifier<Node> path = InstanceIdentifier
                        .create(NetworkTopology.class)
                        .child(Topology.class, new TopologyKey(HwvtepSouthboundConstants.HWVTEP_TOPOLOGY_ID))
                        .child(Node.class);
        return path;
    }

    private Map<HwvtepConnectionInstance, Collection<DataTreeModification<Node>>> changesByConnectionInstance(
            Collection<DataTreeModification<Node>> changes) {
        Map<HwvtepConnectionInstance, Collection<DataTreeModification<Node>>> result =
                new HashMap<HwvtepConnectionInstance, Collection<DataTreeModification<Node>>>();
        for (DataTreeModification<Node> change : changes) {
            final DataObjectModification<Node> mod = change.getRootNode();
            //From original node to get connection instance
            Node node = mod.getDataBefore()!=null ? mod.getDataBefore() : mod.getDataAfter();
            HwvtepConnectionInstance connection = hcm.getConnectionInstance(node);
            if(connection == null) {
                //Let us try getting it from Operational DS
                final ReadWriteTransaction transaction = db.newReadWriteTransaction();
                InstanceIdentifier<Node> connectionIid = HwvtepSouthboundMapper.createInstanceIdentifier(node.getNodeId());
                Optional<Node> optionalNode = HwvtepSouthboundUtil.readNode(transaction, connectionIid);
                LOG.trace("Node in Operational DataStore for user node {} is {}", node, optionalNode);
                if(optionalNode.isPresent()) {
                    connection = hcm.getConnectionInstance(optionalNode.get());
                }
            }
            if (connection != null) {
                if (!result.containsKey(connection)) {
                    List<DataTreeModification<Node>> tempChanges= new ArrayList<DataTreeModification<Node>>();
                    tempChanges.add(change);
                    result.put(connection, tempChanges);
                } else {
                    result.get(connection).add(change);
                }
            } else {
                LOG.warn("Failed to get the connection of changed node: {}", node);
            }
        }
        LOG.trace("Connection Change Map: {}", result);
        return result;
    }
}
