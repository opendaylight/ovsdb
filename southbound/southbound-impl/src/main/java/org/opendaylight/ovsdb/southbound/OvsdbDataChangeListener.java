package org.opendaylight.ovsdb.southbound;

import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.southbound.ovsdb.transact.DataChangesManagedByOvsdbNodeEvent;
import org.opendaylight.ovsdb.southbound.ovsdb.transact.TransactCommandAggregator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.collect.Maps;

public class OvsdbDataChangeListener implements DataChangeListener, AutoCloseable {

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
        // Connect first if we have to:
        connect(changes);

        // Second update connections if we have to
        updateConnections(changes);

        // Then handle updates to the actual data
        updateData(changes);

        // Finally disconnect if we need to
        disconnect(changes);

    }

    private void updateData(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        for (OvsdbConnectionInstance connectionInstance : connectionInstancesFromChanges(changes)) {
            connectionInstance.transact(new TransactCommandAggregator(
                    db,
                    new DataChangesManagedByOvsdbNodeEvent(
                            SouthboundMapper.createInstanceIdentifier(connectionInstance.getKey()),
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
                OvsdbClient client = cm.getClient(value);
                if (client == null) {
                    for (Entry<InstanceIdentifier<?>, DataObject> original : changes.getOriginalData().entrySet()) {
                        if (original.getValue() instanceof OvsdbNodeAugmentation) {
                            try {
                                cm.disconnect((OvsdbNodeAugmentation) original.getValue());
                                cm.connect(value);
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
                try {
                    cm.connect((OvsdbNodeAugmentation) created.getValue());
                } catch (UnknownHostException e) {
                    LOG.warn("Failed to connect to ovsdbNode", e);
                }
            }
        }
    }

    public Set<OvsdbConnectionInstance> connectionInstancesFromChanges(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        Set<OvsdbConnectionInstance> result = new HashSet<OvsdbConnectionInstance>();
        result.addAll(connectionInstancesFromMap(changes.getCreatedData()));
        result.addAll(connectionInstancesFromMap(changes.getUpdatedData()));
        result.addAll(connectionInstancesFromMap(
                Maps.filterKeys(changes.getOriginalData(), Predicates.in(changes.getRemovedPaths()))));
        return result;
    }

    public Set<OvsdbConnectionInstance> connectionInstancesFromMap(Map<InstanceIdentifier<?>, DataObject> map) {
        Preconditions.checkNotNull(map);
        Set<OvsdbConnectionInstance> result = new HashSet<OvsdbConnectionInstance>();
        for ( Entry<InstanceIdentifier<?>, DataObject> created : map.entrySet()) {
            if (created.getValue() instanceof Node) {
                LOG.debug("Received request to create {}",created.getValue());
                OvsdbBridgeAugmentation bridge =
                        ((Node)created.getValue()).getAugmentation(OvsdbBridgeAugmentation.class);
                if (bridge != null) {
                    OvsdbConnectionInstance client = cm.getConnectionInstance(bridge);
                    if (client != null) {
                        LOG.debug("Found client for {}", created.getValue());
                        result.add(client);
                    } else {
                        LOG.debug("Did not find client for {}",created.getValue());
                    }
                } else {
                    OvsdbNodeAugmentation ovsNode =
                            ((Node)created.getValue()).getAugmentation(OvsdbNodeAugmentation.class);
                    if (ovsNode != null) {
                        OvsdbConnectionInstance client = cm.getConnectionInstance(ovsNode);
                        if (client != null) {
                            LOG.debug("Found client for {}", created.getValue());
                            result.add(client);
                        } else {
                            LOG.debug("Did not find client for {}",created.getValue());
                        }
                    }
                }
            }
        }
        return result;
    }

}
