/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification.ModificationType;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HwvtepOperGlobalListener implements ClusteredDataTreeChangeListener<Node>, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(HwvtepOperGlobalListener.class);

    private final Timer timer = new Timer();
    private ListenerRegistration<HwvtepOperGlobalListener> registration;
    private final HwvtepConnectionManager hcm;
    private final DataBroker db;
    private static final Map<InstanceIdentifier<Node>, Node> CONNECTED_NODES = new ConcurrentHashMap<>();

    HwvtepOperGlobalListener(final DataBroker db, final HwvtepConnectionManager hcm) {
        LOG.info("Registering HwvtepOperGlobalListener");
        this.db = db;
        this.hcm = hcm;
        registerListener();
    }

    private void registerListener() {
        final DataTreeIdentifier<Node> treeId =
                        new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL, getWildcardPath());

        registration = db.registerDataTreeChangeListener(treeId, HwvtepOperGlobalListener.this);
    }

    @Override
    public void close() {
        if (registration != null) {
            registration.close();
        }
    }

    @Override
    public void onDataTreeChanged(final Collection<DataTreeModification<Node>> changes) {
        changes.forEach(change -> {
            InstanceIdentifier<Node> key = change.getRootPath().getRootIdentifier();
            DataObjectModification<Node> mod = change.getRootNode();
            InstanceIdentifier<Node> nodeIid = change.getRootPath().getRootIdentifier();
            Node node = getCreated(mod);
            if (node != null) {
                CONNECTED_NODES.put(key, node);
            }
            node = getRemoved(mod);
            if (node != null) {
                CONNECTED_NODES.remove(key);
                HwvtepConnectionInstance connectionInstance = hcm.getConnectionInstanceFromNodeIid(nodeIid);
                if (connectionInstance != null && Objects.equals(connectionInstance.getConnectionInfo().getRemotePort(),
                            HwvtepSouthboundUtil.getRemotePort(node))) {
                    //Oops some one deleted the node held by me This should never happen
                    try {
                        connectionInstance.refreshOperNode();
                    } catch (ExecutionException | InterruptedException e) {
                        LOG.error("Failed to refresh operational nodes ", e);
                    }
                }

            }
        });
    }

    private static Node getCreated(final DataObjectModification<Node> mod) {
        if (mod.getModificationType() == ModificationType.WRITE && mod.getDataBefore() == null) {
            return mod.getDataAfter();
        }
        return null;
    }

    private static Node getRemoved(final DataObjectModification<Node> mod) {
        if (mod.getModificationType() == ModificationType.DELETE) {
            return mod.getDataBefore();
        }
        return null;
    }

    public Map<InstanceIdentifier<Node>, Node> getConnectedNodes() {
        return Collections.unmodifiableMap(CONNECTED_NODES);
    }

    private static InstanceIdentifier<Node> getWildcardPath() {
        return InstanceIdentifier.create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(HwvtepSouthboundConstants.HWVTEP_TOPOLOGY_ID))
                .child(Node.class);
    }

    public static Node getNode(final InstanceIdentifier<Node> key) {
        return CONNECTED_NODES.get(key);
    }
}
