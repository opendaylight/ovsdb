/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification.ModificationType;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HwvtepOperGlobalListener implements ClusteredDataTreeChangeListener<Node>, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(HwvtepOperGlobalListener.class);

    private Timer timer = new Timer();
    private ListenerRegistration<HwvtepOperGlobalListener> registration;
    private HwvtepConnectionManager hcm;
    private DataBroker db;
    private Map<YangInstanceIdentifier, Node> connectedNodes = new ConcurrentHashMap<>();

    HwvtepOperGlobalListener(DataBroker db, HwvtepConnectionManager hcm) {
        LOG.info("Registering HwvtepOperGlobalListener");
        this.db = db;
        this.hcm = hcm;
        registerListener(db);
    }

    private void registerListener(final DataBroker db) {
        final DataTreeIdentifier<Node> treeId =
                        new DataTreeIdentifier<Node>(LogicalDatastoreType.OPERATIONAL, getWildcardPath());
        try {
            registration = db.registerDataTreeChangeListener(treeId, HwvtepOperGlobalListener.this);
        } catch (final Exception e) {
            LOG.error("HwvtepDataChangeListener registration failed", e);
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
        changes.forEach( (change) -> {
            InstanceIdentifier<Node> key = change.getRootPath().getRootIdentifier();
            DataObjectModification<Node> mod = change.getRootNode();
            InstanceIdentifier<Node> nodeIid = change.getRootPath().getRootIdentifier();
            YangInstanceIdentifier entityId =
                    HwvtepSouthboundUtil.getInstanceIdentifierCodec().getYangInstanceIdentifier(nodeIid);
            Node node = getCreated(mod);
            if (node != null) {
                connectedNodes.put(entityId, node);
            }
            node = getRemoved(mod);
            if (node != null) {
                connectedNodes.remove(entityId);
                HwvtepConnectionInstance connectionInstance = hcm.getConnectionInstanceFromNodeIid(nodeIid);
                if (Objects.equals(connectionInstance.getConnectionInfo().getRemotePort(),
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

    public Map<YangInstanceIdentifier, Node> getConnectedNodes() {
        return Collections.unmodifiableMap(connectedNodes);
    }

    private InstanceIdentifier<Node> getWildcardPath() {
        InstanceIdentifier<Node> path = InstanceIdentifier
                        .create(NetworkTopology.class)
                        .child(Topology.class, new TopologyKey(HwvtepSouthboundConstants.HWVTEP_TOPOLOGY_ID))
                        .child(Node.class);
        return path;
    }
}
