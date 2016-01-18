/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.southbound;

import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.opendaylight.controller.md.sal.binding.api.ClusteredDataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.southbound.ovsdb.transact.BridgeOperationalState;
import org.opendaylight.ovsdb.southbound.ovsdb.transact.DataChangesManagedByOvsdbNodeEvent;
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
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.collect.Maps;

public class OvsdbDataChangeListener implements ClusteredDataChangeListener, AutoCloseable {

    private ListenerRegistration<DataChangeListener> registration;
    private OvsdbConnectionManager cm;
    private DataBroker db;
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbDataChangeListener.class);

    OvsdbDataChangeListener(DataBroker db, OvsdbConnectionManager cm) {
        LOG.info("Registering OvsdbNodeDataChangeListener");
        this.cm = cm;
        this.db = db;
        InstanceIdentifier<Node> path = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class);
        registration =
                db.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION, path, this, DataChangeScope.SUBTREE);

    }

    @Override
    public void close() throws Exception {
        registration.close();
    }

    @Override
    public void onDataChanged(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        LOG.trace("onDataChanged: {}", changes);
        // Connect first if we have to:
        connect(changes);

        // Second update connections if we have to
        updateConnections(changes);

        // Then handle updates to the actual data
        updateData(changes);

        // Finally disconnect if we need to
        disconnect(changes);

 //       init(changes);

        LOG.trace("onDataChanged: exit");
    }

    private void updateData(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        for (Entry<InstanceIdentifier<Node>, OvsdbConnectionInstance> connectionInstanceEntry :
                connectionInstancesFromChanges(changes).entrySet()) {
            OvsdbConnectionInstance connectionInstance = connectionInstanceEntry.getValue();
            connectionInstance.transact(new TransactCommandAggregator(
                    new BridgeOperationalState(db, changes),
                    new DataChangesManagedByOvsdbNodeEvent(
                            connectionInstance.getInstanceIdentifier(),
                            changes)));
        }
    }

    private void disconnect(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        Map<InstanceIdentifier<?>, DataObject> originalDataObject = changes.getOriginalData();
        Set<InstanceIdentifier<?>> iiD = changes.getRemovedPaths();
        for (InstanceIdentifier instanceIdentifier : iiD) {
            if (originalDataObject.get(instanceIdentifier) instanceof OvsdbNodeAugmentation) {
                try {
                    cm.disconnect((OvsdbNodeAugmentation) originalDataObject.get(instanceIdentifier));
                } catch (UnknownHostException e) {
                    LOG.warn("Failed to disconnect ovsdbNode", e);
                }
            }
        }
    }

    private void updateConnections(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        for (Entry<InstanceIdentifier<?>, DataObject> updated : changes.getUpdatedData().entrySet()) {
            if (updated.getValue() instanceof OvsdbNodeAugmentation) {
                OvsdbNodeAugmentation value = (OvsdbNodeAugmentation) updated.getValue();
                OvsdbClient client = cm.getClient(value.getConnectionInfo());
                if (client == null) {
                    for (Entry<InstanceIdentifier<?>, DataObject> original : changes.getOriginalData().entrySet()) {
                        if (original.getValue() instanceof OvsdbNodeAugmentation) {
                            try {
                                cm.disconnect((OvsdbNodeAugmentation) original.getValue());
                                cm.connect((InstanceIdentifier<Node>) original.getKey(),value);
                            } catch (UnknownHostException e) {
                                LOG.warn("Failed to disconnect to ovsdbNode", e);
                            }
                        }
                    }
                }
            }
        }
    }

    private void connect(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        for (Entry<InstanceIdentifier<?>, DataObject> created : changes.getCreatedData().entrySet()) {
            // TODO validate we have the correct kind of InstanceIdentifier
            if (created.getValue() instanceof OvsdbNodeAugmentation) {
                OvsdbNodeAugmentation ovsdbNode = (OvsdbNodeAugmentation)created.getValue();
                ConnectionInfo key = ovsdbNode.getConnectionInfo();
                InstanceIdentifier<Node> iid = cm.getInstanceIdentifier(key);
                if ( iid != null) {
                    LOG.warn("Connection to device {} already exists. Plugin does not allow multiple connections "
                              + "to same device, hence dropping the request {}", key, ovsdbNode);
                } else {
                    try {
                        cm.connect((InstanceIdentifier<Node>) created.getKey(),
                                (OvsdbNodeAugmentation) created.getValue());
                    } catch (UnknownHostException e) {
                        LOG.warn("Failed to connect to ovsdbNode", e);
                    }
                }
            }
        }
    }
/*
    private void init(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        for (Entry<InstanceIdentifier<?>, DataObject> created : changes.getCreatedData().entrySet()) {
            if (created.getValue() instanceof OvsdbNodeAugmentation) {
                OvsdbNodeAugmentation ovsdbNode = (OvsdbNodeAugmentation)created.getValue();
                cm.init(ovsdbNode.getConnectionInfo());
            }
        }

    }
*/
    public Map<InstanceIdentifier<Node>,OvsdbConnectionInstance> connectionInstancesFromChanges(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        Map<InstanceIdentifier<Node>,OvsdbConnectionInstance> result =
                new HashMap<>();
        result.putAll(connectionInstancesFromMap(changes.getCreatedData()));
        result.putAll(connectionInstancesFromMap(changes.getUpdatedData()));
        result.putAll(connectionInstancesFromMap(
                Maps.filterKeys(changes.getOriginalData(), Predicates.in(changes.getRemovedPaths()))));
        return result;
    }

    public Map<InstanceIdentifier<Node>,OvsdbConnectionInstance> connectionInstancesFromMap(Map<InstanceIdentifier<?>,
            DataObject> map) {
        Preconditions.checkNotNull(map);
        Map<InstanceIdentifier<Node>,OvsdbConnectionInstance> result =
                new HashMap<>();
        for ( Entry<InstanceIdentifier<?>, DataObject> created : map.entrySet()) {
            if (created.getValue() instanceof Node) {
                OvsdbConnectionInstance client = null;
                LOG.debug("Received request for {}",created.getValue());
                OvsdbBridgeAugmentation bridge =
                        ((Node)created.getValue()).getAugmentation(OvsdbBridgeAugmentation.class);
                if (bridge != null) {
                    client = cm.getConnectionInstance(bridge);
                } else {
                    OvsdbNodeAugmentation ovsNode =
                            ((Node)created.getValue()).getAugmentation(OvsdbNodeAugmentation.class);
                    if (ovsNode != null && ovsNode.getConnectionInfo() != null) {
                        client = cm.getConnectionInstance(ovsNode.getConnectionInfo());
                    } else {
                        List<TerminationPoint> terminationPoint = ((Node)created.getValue()).getTerminationPoint();
                        if (!terminationPoint.isEmpty()) {
                            InstanceIdentifier<Node> nodeIid = SouthboundMapper.
                                    createInstanceIdentifier(((Node)created.getValue()).getNodeId());
                            client = cm.getConnectionInstance(nodeIid);
                        }
                    }
                }
                if(client == null) {
                    /* Let us try getting it from Operational datastore */
                    LOG.debug("Trying to get client for {} from Operational information", created.getValue());
                    InstanceIdentifier<Node> nodeIid = SouthboundMapper.
                                    createInstanceIdentifier(((Node)created.getValue()).getNodeId());
                    client = cm.getConnectionInstance(nodeIid);
                }
                if (client != null) {
                    LOG.debug("Found client for {}", created.getValue());
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
                        LOG.debug("*this* instance of southbound plugin is an "
                                + "owner of the device {}",created.getValue());
                        result.put((InstanceIdentifier<Node>) created.getKey(), client);
                    } else {
                        LOG.debug("*this* instance of southbound plugin is not an "
                                + "owner of the device {}",created.getValue());
                    }
                } else {
                    LOG.debug("Did not find client for {}",created.getValue());
                }
            }
        }
        return result;
    }

}
