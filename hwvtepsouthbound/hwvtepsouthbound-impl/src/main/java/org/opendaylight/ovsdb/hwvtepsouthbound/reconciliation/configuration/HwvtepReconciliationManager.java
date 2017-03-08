/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.reconciliation.configuration;

import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification.ModificationType;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepConnectionInstance;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepConnectionManager;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundMapper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalSwitchAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.PhysicalSwitchAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public class HwvtepReconciliationManager implements ClusteredDataTreeChangeListener<Node>, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(HwvtepReconciliationManager.class);
    private ListenerRegistration<HwvtepReconciliationManager> registration;
    private HwvtepConnectionManager hcm;
    private DataBroker db;

    public HwvtepReconciliationManager(DataBroker db, HwvtepConnectionManager hcm) {
        this.db = db;
        this.hcm = hcm;
        registerListener(db);
    }

    private void registerListener(final DataBroker db) {
        InstanceIdentifier<Node> iid = HwvtepSouthboundMapper.createInstanceIdentifier();
        DataTreeIdentifier<Node> treeId =
                        new DataTreeIdentifier<Node>(LogicalDatastoreType.OPERATIONAL, iid);
        LOG.trace("Registering listener for path {}", treeId);
        registration = db.registerDataTreeChangeListener(treeId, HwvtepReconciliationManager.this);
    }

    @Override
    public void close() throws Exception {
        if(registration != null) {
            registration.close();
        }
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<Node>> changes) {
        processConnectedNodes(changes);
        processDisconnectedNodes(changes);
    }

    private void processDisconnectedNodes(Collection<DataTreeModification<Node>> changes) {
        for (DataTreeModification<Node> change : changes) {
            final InstanceIdentifier<Node> key = change.getRootPath().getRootIdentifier();
            final DataObjectModification<Node> mod = change.getRootNode();
            Node deleted = getRemoved(mod);
            if (deleted != null) {
                if (deleted.getAugmentation(HwvtepGlobalAugmentation.class) != null) {
                    LOG.trace("Cancel config reconciliation for node {}", deleted.getKey());
                    hcm.stopConfigurationReconciliation(key);
                }
            }
        }
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
                        LOG.trace("Reconcile config for node {}, IP : {}", node.getKey(),
                                connection.getConnectionInfo().getRemoteAddress());
                        hcm.reconcileConfigurations(connection, node);
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
}
