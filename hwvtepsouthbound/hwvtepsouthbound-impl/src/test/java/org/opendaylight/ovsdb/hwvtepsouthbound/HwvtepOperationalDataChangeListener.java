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
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.binding.DataObjectReference;
import org.opendaylight.yangtools.binding.EntryObject;
import org.opendaylight.yangtools.concepts.Registration;

public class HwvtepOperationalDataChangeListener implements DataTreeChangeListener<Node>, AutoCloseable {
    private final Registration registration;
    private final HwvtepConnectionManager hcm;
    private final HwvtepConnectionInstance connectionInstance;

    HwvtepOperationalDataChangeListener(DataBroker db, HwvtepConnectionManager hcm,
            HwvtepConnectionInstance connectionInstance) {
        this.hcm = hcm;
        this.connectionInstance = connectionInstance;
        registration = db.registerTreeChangeListener(LogicalDatastoreType.OPERATIONAL,
            DataObjectReference.builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(HwvtepSouthboundConstants.HWVTEP_TOPOLOGY_ID))
                .child(Node.class)
                .build(), this);
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
            final DataObjectIdentifier<Node> key = change.path();
            final DataObjectModification<Node> mod = change.getRootNode();
            for (DataObjectModification<?> child : mod.modifiedChildren()) {
                updateDeviceOpData(key, child);
            }
            DataObjectModification<HwvtepGlobalAugmentation> aug =
                    mod.getModifiedAugmentation(HwvtepGlobalAugmentation.class);
            if (aug != null) {
                for (DataObjectModification<?> child : aug.modifiedChildren()) {
                    updateDeviceOpData(key, child);
                }
            }
        }
    }

    private void updateDeviceOpData(DataObjectIdentifier<Node> key, DataObjectModification<?> mod) {
        Class<? extends EntryObject<?, ?>> childClass = (Class<? extends EntryObject<?, ?>>) mod.dataType();
        var instanceIdentifier = getKey(key, mod, mod.dataAfter());
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

    private static DataObjectIdentifier getKey(DataObjectIdentifier<Node> key,
                                             DataObjectModification<?> child, DataObject data) {
        Class<? extends DataObject> childClass = child.dataType();
        if (LogicalSwitches.class == childClass) {
            LogicalSwitches ls = (LogicalSwitches)data;
            return key.toBuilder()
                .augmentation(HwvtepGlobalAugmentation.class)
                .child(LogicalSwitches.class, ls.key())
                .build();
        } else if (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology
                .topology.node.TerminationPoint.class == childClass) {
            TerminationPoint tp = (TerminationPoint)data;
            return key.toBuilder()
                .child(TerminationPoint.class, tp.key())
                .build();
        } else if (RemoteUcastMacs.class == childClass) {
            RemoteUcastMacs mac = (RemoteUcastMacs)data;
            return key.toBuilder()
                .augmentation(HwvtepGlobalAugmentation.class)
                .child(RemoteUcastMacs.class, mac.key())
                .build();
        } else if (RemoteMcastMacs.class == childClass) {
            RemoteMcastMacs mac = (RemoteMcastMacs)data;
            return key.toBuilder()
                .augmentation(HwvtepGlobalAugmentation.class)
                .child(RemoteMcastMacs.class, mac.key())
                .build();
        } else {
            return null;
        }
    }

    Class<? extends ChildOf<? super HwvtepGlobalAugmentation>> getClass(Class<? extends DataObject> cls) {
        return (Class<? extends ChildOf<? super HwvtepGlobalAugmentation>>) cls;
    }
}
