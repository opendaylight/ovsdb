/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.southbound.ovsdb.transact.DataChangesManagedByOvsdbNodeEvent;
import org.opendaylight.ovsdb.southbound.ovsdb.transact.TransactCommandAggregator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
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

public class OvsdbManagedNodeDataChangeListener implements DataChangeListener, AutoCloseable {

    private ListenerRegistration<DataChangeListener> registration;
    private DataBroker db;
    private OvsdbConnectionManager cm;
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbManagedNodeDataChangeListener.class);

    OvsdbManagedNodeDataChangeListener(DataBroker db, OvsdbConnectionManager cm) {
        LOG.info("Registering OvsdbManagedNodeDataChangeListener");
        this.cm = cm;
        this.db = db;
        InstanceIdentifier<OvsdbBridgeAugmentation> path = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class)
                .augmentation(OvsdbBridgeAugmentation.class);
        registration = db.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION,
                path, this, DataChangeScope.ONE);

    }

    @Override
    public void onDataChanged(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        LOG.info("Received change to ovsdbManagedNode: {}", changes);
        for (OvsdbConnectionInstance connectionInstance : connectionInstancesFromChanges(changes)) {
            connectionInstance.transact(new TransactCommandAggregator(
                    db,
                    new DataChangesManagedByOvsdbNodeEvent(
                            SouthboundMapper.createInstanceIdentifier(connectionInstance.getKey()),
                            changes)));
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
            if (created.getValue() instanceof OvsdbBridgeAugmentation) {
                LOG.debug("Received request to create {}",created.getValue());
                OvsdbConnectionInstance client = cm.getConnectionInstance((OvsdbBridgeAugmentation)created.getValue());
                if (client != null) {
                    LOG.debug("Found client for {}", created.getValue());
                    result.add(client);
                } else {
                    LOG.debug("Did not find client for {}",created.getValue());
                }
            }
        }
        return result;
    }

    @Override
    public void close() throws Exception {
        registration.close();
    }

}
