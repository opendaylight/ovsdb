/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.reconciliation.configuration;

import org.opendaylight.controller.md.sal.binding.api.*;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification.ModificationType;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepConnectionInstance;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepConnectionManager;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundConstants;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalSwitchAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.PhysicalSwitchAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class OperationalNodeListener implements ClusteredDataTreeChangeListener<Node>, AutoCloseable {

    private ListenerRegistration<OperationalNodeListener> registration;
    private HwvtepConnectionManager hcm;
    private DataBroker db;
    private static final Logger LOG = LoggerFactory.getLogger(OperationalNodeListener.class);

    public OperationalNodeListener(DataBroker db, HwvtepConnectionManager hcm) {
        LOG.trace("Registering HwvtepDataChangeListener for operational nodes");
        this.db = db;
        this.hcm = hcm;
        registerListener(db);
    }

    private void registerListener(final DataBroker db) {
        final DataTreeIdentifier<Node> treeId =
                        new DataTreeIdentifier<Node>(LogicalDatastoreType.OPERATIONAL, getWildcardPath());
        try {
            LOG.trace("Registering on path: {}", treeId);
            registration = db.registerDataTreeChangeListener(treeId, OperationalNodeListener.this);
        } catch (final Exception e) {
            LOG.warn("HwvtepDataChangeListener registration failed", e);
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
        LOG.error("onDataTreeChanged: {}", changes);
        processConnectedNodes(changes);
        //processDisconnectedNodes(changes);TODO cancel the reconcilation task
    }

    private void processConnectedNodes(Collection<DataTreeModification<Node>> changes) {
        for (DataTreeModification<Node> change : changes) {
            InstanceIdentifier<Node> key = change.getRootPath().getRootIdentifier();
            DataObjectModification<Node> mod = change.getRootNode();
            Node node = getCreated(mod);
            if (node != null) {
                PhysicalSwitchAugmentation physicalSwitch = node.getAugmentation(PhysicalSwitchAugmentation.class);
                if (physicalSwitch != null) {
                    HwvtepConnectionInstance connection =
                            hcm.getConnectionInstance((HwvtepPhysicalSwitchAttributes)physicalSwitch);
                    if (connection != null) {
                        LOG.trace("reconcile config for node {} from {}", node.getKey(),
                                connection.getConnectionInfo().getRemoteAddress());
                        hcm.reconcileConfigurations(connection);
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

    private InstanceIdentifier<Node> getWildcardPath() {
        InstanceIdentifier<Node> path = InstanceIdentifier
                        .create(NetworkTopology.class)
                        .child(Topology.class, new TopologyKey(HwvtepSouthboundConstants.HWVTEP_TOPOLOGY_ID))
                        .child(Node.class);
        return path;
    }
}
