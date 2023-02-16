/*
 * Copyright © 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.reconciliation.configuration;

import java.util.Collection;
import org.opendaylight.mdsal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataObjectModification.ModificationType;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepConnectionInstance;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepConnectionManager;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundMapper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.PhysicalSwitchAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HwvtepReconciliationManager implements ClusteredDataTreeChangeListener<Node>, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(HwvtepReconciliationManager.class);

    private final ListenerRegistration<HwvtepReconciliationManager> registration;
    private final HwvtepConnectionManager hcm;

    public HwvtepReconciliationManager(DataBroker db, HwvtepConnectionManager hcm) {
        this.hcm = hcm;

        final InstanceIdentifier<Node> iid = HwvtepSouthboundMapper.createInstanceIdentifier();
        final DataTreeIdentifier<Node> treeId = DataTreeIdentifier.create(LogicalDatastoreType.OPERATIONAL, iid);
        LOG.trace("Registering listener for path {}", treeId);
        registration = db.registerDataTreeChangeListener(treeId, HwvtepReconciliationManager.this);
    }

    @Override
    public void close() {
        if (registration != null) {
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
                if (deleted.augmentation(HwvtepGlobalAugmentation.class) != null) {
                    LOG.trace("Cancel config reconciliation for node {}", deleted.key());
                    hcm.stopConfigurationReconciliation(key);
                }
            }
        }
    }

    private void processConnectedNodes(Collection<DataTreeModification<Node>> changes) {
        for (DataTreeModification<Node> change : changes) {
            DataObjectModification<Node> mod = change.getRootNode();
            Node node = getCreated(mod);
            if (node != null) {
                PhysicalSwitchAugmentation physicalSwitch = node.augmentation(PhysicalSwitchAugmentation.class);
                if (physicalSwitch != null) {
                    HwvtepConnectionInstance connection =
                            hcm.getConnectionInstance(physicalSwitch);
                    if (connection != null) {
                        LOG.info("HwvtepReconciliationManager Reconcile config for node {}, IP : {}", node.key(),
                                connection.getConnectionInfo().getRemoteAddress());
                        hcm.reconcileConfigurations(connection, node);
                    }
                }
            }
        }
    }

    private static Node getCreated(DataObjectModification<Node> mod) {
        if (mod.getModificationType() == ModificationType.WRITE && mod.getDataBefore() == null) {
            return mod.getDataAfter();
        }
        return null;
    }

    private static Node getRemoved(DataObjectModification<Node> mod) {
        if (mod.getModificationType() == ModificationType.DELETE) {
            return mod.getDataBefore();
        }
        return null;
    }
}
