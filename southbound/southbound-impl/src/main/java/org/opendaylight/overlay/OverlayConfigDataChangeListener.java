/*
* Copyright (c) 2015 Intel Corp. and others.  All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/
package org.opendaylight.overlay;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.collect.Maps;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.overlay.transactions.md.DataChangesManagedByOverlayNodeEvent;
import org.opendaylight.overlay.transactions.md.utils.TransactionCommand;
import org.opendaylight.overlay.transactions.md.config.OverlayConfigCommandAggregator;
import org.opendaylight.overlay.transactions.md.utils.TransactionInvoker;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OverlayConfigDataChangeListener implements DataChangeListener, AutoCloseable {

    private ListenerRegistration<DataChangeListener> registration;
    private DataBroker dbOverlay;
    private TransactionInvoker txInvoker;
    private static final Logger LOG = LoggerFactory.getLogger(OverlayConfigDataChangeListener.class);

    OverlayConfigDataChangeListener(DataBroker db) {
        LOG.info("Registering OverlayDataChangeListener");
        InstanceIdentifier<Node> path = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(OverlayConstants.OVERLAY_TOPOLOGY_ID))
                .child(Node.class);
        registration =
                dbOverlay.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION, path,
                        this, AsyncDataBroker.DataChangeScope.SUBTREE);
    }

    @Override
    public void close() throws Exception {
        registration.close();
    }

    @Override
    public void onDataChanged(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        updateData(changes);
    }

    private void updateData(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        for (Node nodeInstance : nodeInstancesFromChanges(changes)) {
            transact(new OverlayConfigCommandAggregator(
                    new DataChangesManagedByOverlayNodeEvent(
                            OverlayMapper.createInstanceIdentifier(nodeInstance.getNodeId()),
                            changes)));
        }
    }

    public Set<Node> nodeInstancesFromChanges(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        Set<Node> result = new HashSet<Node>();
        result.addAll(nodeInstancesFromMap(changes.getCreatedData()));
        result.addAll(nodeInstancesFromMap(changes.getUpdatedData()));
        result.addAll(nodeInstancesFromMap(
                Maps.filterKeys(changes.getOriginalData(), Predicates.in(changes.getRemovedPaths()))));
        return result;
    }

    public Set<Node> nodeInstancesFromMap(Map<InstanceIdentifier<?>, DataObject> map) {
        Preconditions.checkNotNull(map);
        Set<Node> result = new HashSet<Node>();
        for (Entry<InstanceIdentifier<?>, DataObject> created : map.entrySet()) {
            if (created.getValue() instanceof Node) {
                LOG.debug("Received request to create {}", created.getValue());
                result.add((Node) created.getValue());
            }
        }
        return result;
    }

    public void transact(TransactionCommand command) {
        txInvoker.invoke(command);
    }
}
