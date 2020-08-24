/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound;

import java.util.Collection;
import org.opendaylight.mdsal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteMcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteUcastMacs;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HwvtepOperationalDataChangeListener implements ClusteredDataTreeChangeListener<Node>, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(HwvtepOperationalDataChangeListener.class);

    private final ListenerRegistration<HwvtepOperationalDataChangeListener> registration;
    private final HwvtepConnectionManager hcm;
    private final DataBroker db;
    private final HwvtepConnectionInstance connectionInstance;

    HwvtepOperationalDataChangeListener(DataBroker db, HwvtepConnectionManager hcm,
            HwvtepConnectionInstance connectionInstance) {
        this.db = db;
        this.hcm = hcm;
        this.connectionInstance = connectionInstance;
        DataTreeIdentifier<Node> treeId = DataTreeIdentifier
            .create(LogicalDatastoreType.OPERATIONAL, getWildcardPath());
        registration = db.registerDataTreeChangeListener(treeId, HwvtepOperationalDataChangeListener.this);
    }

    @Override
    public void close() throws Exception {
        if (registration != null) {
            registration.close();
        }
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<Node>> changes) {
        for (DataTreeModification<Node> change : changes) {
            final InstanceIdentifier<Node> key = change.getRootPath().getRootIdentifier();
            final DataObjectModification<Node> mod = change.getRootNode();
            for (DataObjectModification<? extends DataObject> child : mod.getModifiedChildren()) {
                updateDeviceOpData(key, child);
            }
            DataObjectModification<HwvtepGlobalAugmentation> aug =
                    mod.getModifiedAugmentation(HwvtepGlobalAugmentation.class);
            if (aug != null) {
                for (DataObjectModification<? extends DataObject> child : aug.getModifiedChildren()) {
                    updateDeviceOpData(key, child);
                }
            }
        }
    }

    private void updateDeviceOpData(InstanceIdentifier<Node> key, DataObjectModification<? extends DataObject> mod) {
        Class<? extends Identifiable> childClass = (Class<? extends Identifiable>) mod.getDataType();
        InstanceIdentifier instanceIdentifier = getKey(key, mod, mod.getDataAfter());
        switch (mod.getModificationType()) {
            case WRITE:
                connectionInstance.getDeviceInfo().updateDeviceOperData(childClass, instanceIdentifier,
                        new UUID("uuid"), mod.getDataAfter());
                break;
            case DELETE:
                connectionInstance.getDeviceInfo().clearDeviceOperData(childClass, instanceIdentifier);
                break;
            case SUBTREE_MODIFIED:
                break;
            default:
                break;
        }
    }

    private static InstanceIdentifier getKey(InstanceIdentifier<Node> key,
                                             DataObjectModification<? extends DataObject> child, DataObject data) {
        Class<? extends DataObject> childClass = child.getDataType();
        InstanceIdentifier instanceIdentifier = null;
        if (LogicalSwitches.class == childClass) {
            LogicalSwitches ls = (LogicalSwitches)data;
            instanceIdentifier = key.augmentation(HwvtepGlobalAugmentation.class).child(LogicalSwitches.class,
                    ls.key());
        } else if (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology
                .topology.node.TerminationPoint.class == childClass) {
            TerminationPoint tp = (TerminationPoint)data;
            instanceIdentifier = key.child(TerminationPoint.class, tp.key());
        } else if (RemoteUcastMacs.class == childClass) {
            RemoteUcastMacs mac = (RemoteUcastMacs)data;
            instanceIdentifier = key.augmentation(HwvtepGlobalAugmentation.class).child(RemoteUcastMacs.class,
                    mac.key());
        } else if (RemoteMcastMacs.class ==  childClass) {
            RemoteMcastMacs mac = (RemoteMcastMacs)data;
            instanceIdentifier = key.augmentation(HwvtepGlobalAugmentation.class).child(RemoteMcastMacs.class,
                    mac.key());
        }
        return instanceIdentifier;
    }

    Class<? extends ChildOf<? super HwvtepGlobalAugmentation>> getClass(Class<? extends DataObject> cls) {
        return (Class<? extends ChildOf<? super HwvtepGlobalAugmentation>>) cls;
    }

    private static InstanceIdentifier<Node> getWildcardPath() {
        return InstanceIdentifier.create(NetworkTopology.class)
                        .child(Topology.class, new TopologyKey(HwvtepSouthboundConstants.HWVTEP_TOPOLOGY_ID))
                        .child(Node.class);
    }
}
