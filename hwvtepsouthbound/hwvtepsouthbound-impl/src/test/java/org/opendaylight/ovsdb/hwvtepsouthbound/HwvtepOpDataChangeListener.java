/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound;

import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification.ModificationType;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.hwvtepsouthbound.transact.HwvtepOperationalState;
import org.opendaylight.ovsdb.hwvtepsouthbound.transact.TransactCommandAggregator;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class HwvtepOpDataChangeListener implements ClusteredDataTreeChangeListener<Node>, AutoCloseable {

    private ListenerRegistration<HwvtepOpDataChangeListener> registration;
    private HwvtepConnectionManager hcm;
    private DataBroker db;
    private static final Logger LOG = LoggerFactory.getLogger(HwvtepOpDataChangeListener.class);
    HwvtepConnectionInstance connectionInstance;
    HwvtepOpDataChangeListener(DataBroker db, HwvtepConnectionManager hcm, HwvtepConnectionInstance connectionInstance) {
        LOG.info("Registering HwvtepDataChangeListener");
        this.db = db;
        this.hcm = hcm;
        this.connectionInstance = connectionInstance;
        registerListener(db);
    }

    private void registerListener(final DataBroker db) {
        final DataTreeIdentifier<Node> treeId =
                        new DataTreeIdentifier<Node>(LogicalDatastoreType.OPERATIONAL, getWildcardPath());
        try {
            LOG.trace("Registering on path: {}", treeId);
            registration = db.registerDataTreeChangeListener(treeId, HwvtepOpDataChangeListener.this);
        } catch (final Exception e) {
            LOG.warn("HwvtepDataChangeListener registration failed", e);
            //TODO: Should we throw an exception here?
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
        LOG.trace("onDataTreeChanged: {}", changes);

        for (DataTreeModification<Node> change : changes) {
            final InstanceIdentifier<Node> key = change.getRootPath().getRootIdentifier();
            final DataObjectModification<Node> mod = change.getRootNode();
            Node created = getCreated(mod);
            Node deleted = getRemoved(mod);
            Node updated = getUpdated(mod);
            Node orig = getOriginal(mod);
            DataObjectModification<HwvtepGlobalAugmentation> aug = mod.getModifiedAugmentation(HwvtepGlobalAugmentation.class);
            ModificationType type2 = getModificationType(aug);
            if (aug != null && getModificationType(aug) != null) {
                LOG.error("modification type {}", type2);
                Collection<DataObjectModification<? extends DataObject>> children = aug.getModifiedChildren();
                for (DataObjectModification<? extends DataObject> child : children) {
                    ModificationType type = getModificationType(child);
                    if (type == null) {
                        continue;
                    }
                    InstanceIdentifier instanceIdentifier = null;
                    Class<? extends Identifiable> childClass = (Class<? extends Identifiable>) child.getDataType();
                    InstanceIdentifier.PathArgument pathArgument = child.getIdentifier();
                    switch(type) {
                        case WRITE:
                            DataObject dataAfter = child.getDataAfter();
                            Identifiable identifiable = (Identifiable) dataAfter;
                            LOG.error("Received add for key {}", pathArgument);
                            instanceIdentifier = getKey(key, child, child.getDataAfter());
                            connectionInstance.getDeviceInfo().updateDeviceOpData(childClass, instanceIdentifier,
                                    new UUID("uuid"), child.getDataAfter());
                            break;
                        case DELETE:
                            LOG.error("Received delete for key {}", pathArgument);
                            instanceIdentifier = getKey(key, child, child.getDataBefore());
                            connectionInstance.getDeviceInfo().clearDeviceOpData(childClass, instanceIdentifier);
                            break;
                        case SUBTREE_MODIFIED:
                    }
                }
            }
        }
    }

    private ModificationType getModificationType(DataObjectModification<? extends DataObject> mod) {
        try {
            return mod.getModificationType();
        } catch (IllegalStateException e) {

        }
        return null;
    }

    private InstanceIdentifier getKey(InstanceIdentifier<Node> key, DataObjectModification<? extends DataObject> child,
                                      DataObject data) {
        Class<? extends DataObject> childClass = child.getDataType();
        InstanceIdentifier instanceIdentifier = null;
        if (LogicalSwitches.class == childClass) {
            LogicalSwitches ls = (LogicalSwitches)data;
            instanceIdentifier = key.augmentation(HwvtepGlobalAugmentation.class).child(LogicalSwitches.class, ls.getKey());
        }
        return instanceIdentifier;
    }

    Class<? extends ChildOf<? super HwvtepGlobalAugmentation>> getClass(Class<? extends DataObject> cls) {
        return (Class<? extends ChildOf<? super HwvtepGlobalAugmentation>>) cls;
    }

    private <T extends DataObject> T getCreated(DataObjectModification<T> mod) {
        if((mod.getModificationType() == ModificationType.WRITE)
                        && (mod.getDataBefore() == null)){
            return mod.getDataAfter();
        }
        return null;
    }

    private <T extends DataObject> T getRemoved(DataObjectModification<T> mod) {
        if(mod.getModificationType() == ModificationType.DELETE){
            return mod.getDataBefore();
        }
        return null;
    }

    private <T extends DataObject> T getUpdated(DataObjectModification<T> mod) {
        T node = null;
        switch(mod.getModificationType()) {
            case SUBTREE_MODIFIED:
                node = mod.getDataAfter();
                break;
            case WRITE:
                if(mod.getDataBefore() !=  null) {
                    node = mod.getDataAfter();
                }
                break;
            default:
                break;
        }
        return node;
    }

    private <T extends DataObject> T getOriginal(DataObjectModification<T> mod) {
        T node = null;
        switch(mod.getModificationType()) {
            case SUBTREE_MODIFIED:
                node = mod.getDataBefore();
                break;
            case WRITE:
                if(mod.getDataBefore() !=  null) {
                    node = mod.getDataBefore();
                }
                break;
            case DELETE:
                node = mod.getDataBefore();
                break;
            default:
                break;
        }
        return node;
    }

    private InstanceIdentifier<Node> getWildcardPath() {
        InstanceIdentifier<Node> path = InstanceIdentifier
                        .create(NetworkTopology.class)
                        .child(Topology.class, new TopologyKey(HwvtepSouthboundConstants.HWVTEP_TOPOLOGY_ID))
                        .child(Node.class);
        return path;
    }
}
