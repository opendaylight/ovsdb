/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound;

import java.util.List;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
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
import org.opendaylight.yangtools.binding.ChildOf;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.binding.EntryObject;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class HwvtepOperationalDataChangeListener implements DataTreeChangeListener<Node>, AutoCloseable {
    private final Registration registration;
    private final HwvtepConnectionManager hcm;
    private final DataBroker db;
    private final HwvtepConnectionInstance connectionInstance;

    HwvtepOperationalDataChangeListener(DataBroker db, HwvtepConnectionManager hcm,
            HwvtepConnectionInstance connectionInstance) {
        this.db = db;
        this.hcm = hcm;
        this.connectionInstance = connectionInstance;
        DataTreeIdentifier<Node> treeId = DataTreeIdentifier.of(LogicalDatastoreType.OPERATIONAL, getWildcardPath());
        registration = db.registerTreeChangeListener(treeId, this);
    }

    @Override
    public void close() throws Exception {
        if (registration != null) {
            registration.close();
        }
    }

    @Override
    public void onDataTreeChanged(List<DataTreeModification<Node>> changes) {
        for (DataTreeModification<Node> change : changes) {
            final InstanceIdentifier<Node> key = change.getRootPath().path();
            final DataObjectModification<Node> mod = change.getRootNode();
            for (DataObjectModification<? extends DataObject> child : mod.modifiedChildren()) {
                updateDeviceOpData(key, child);
            }
            DataObjectModification<HwvtepGlobalAugmentation> aug =
                    mod.getModifiedAugmentation(HwvtepGlobalAugmentation.class);
            if (aug != null) {
                for (DataObjectModification<? extends DataObject> child : aug.modifiedChildren()) {
                    updateDeviceOpData(key, child);
                }
            }
        }
    }

    private void updateDeviceOpData(InstanceIdentifier<Node> key, DataObjectModification<? extends DataObject> mod) {
        Class<? extends EntryObject<?, ?>> childClass = (Class<? extends EntryObject<?, ?>>) mod.dataType();
        InstanceIdentifier instanceIdentifier = getKey(key, mod, mod.dataAfter());
        switch (mod.modificationType()) {
            case WRITE:
                connectionInstance.getDeviceInfo().updateDeviceOperData(childClass, instanceIdentifier,
                        new UUID("uuid"), mod.dataAfter());
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
        Class<? extends DataObject> childClass = child.dataType();
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
