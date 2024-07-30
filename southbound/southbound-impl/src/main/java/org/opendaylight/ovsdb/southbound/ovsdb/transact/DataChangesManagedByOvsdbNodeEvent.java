/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.ovsdb.southbound.SouthboundUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class DataChangesManagedByOvsdbNodeEvent implements DataChangeEvent {

    private final InstanceIdentifier<?> iid;
    private final DataBroker db;
    private final DataChangeEvent event;
    private Map<InstanceIdentifier<?>, DataObject> createdData = null;
    private Map<InstanceIdentifier<?>, DataObject> updatedData = null;
    private Map<InstanceIdentifier<?>, DataObject> originalData = null;
    private Set<InstanceIdentifier<?>> removedPaths;

    public DataChangesManagedByOvsdbNodeEvent(DataBroker dataBroker, InstanceIdentifier<?> iid,
            DataChangeEvent event) {
        this.db = dataBroker;
        this.iid = iid;
        this.event = event;
    }

    private Map<InstanceIdentifier<?>, DataObject> filter(Map<InstanceIdentifier<?>,
            DataObject> data) {
        Map<InstanceIdentifier<?>, DataObject> result
            = new HashMap<>();
        for (Entry<InstanceIdentifier<?>, DataObject> entry: data.entrySet()) {
            if (isManagedBy(entry.getKey())) {
                result.put(entry.getKey(),entry.getValue());
            } else {
                Class<?> type = entry.getKey().getTargetType();
                if (type.equals(OvsdbNodeAugmentation.class)
                        || type.equals(OvsdbTerminationPointAugmentation.class)
                        || type.equals(Node.class)) {
                    result.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return result;
    }

    @Override
    public Map<InstanceIdentifier<?>, DataObject> getCreatedData() {
        if (this.createdData  == null) {
            this.createdData = filter(event.getCreatedData());
        }
        return this.createdData;
    }

    @Override
    public Map<InstanceIdentifier<?>, DataObject> getUpdatedData() {
        if (this.updatedData == null) {
            this.updatedData = filter(event.getUpdatedData());
        }
        return this.updatedData;
    }

    @Override
    public Set<InstanceIdentifier<?>> getRemovedPaths() {
        if (this.removedPaths == null) {
            this.removedPaths = new HashSet<>();
            for (InstanceIdentifier<?> path: event.getRemovedPaths()) {
                if (isManagedBy(path)) {
                    this.removedPaths.add(path);
                }
            }
        }
        return this.removedPaths;
    }

    private boolean isManagedBy(InstanceIdentifier<?> bridgeIid) {

        // Did we just create the containing node?
        InstanceIdentifier<?> managedBy = getManagedByIid(event.getCreatedData() , bridgeIid);
        if (managedBy != null && managedBy.equals(iid)) {
            return true;
        }

        // Did we just update the containing node?
        managedBy = getManagedByIid(event.getUpdatedData() , bridgeIid);
        if (managedBy != null && managedBy.equals(iid)) {
            return true;
        }

        // Did we have the containing node already (note: we never get here unless we are deleting it)
        managedBy = getManagedByIid(event.getOriginalData() , bridgeIid);
        if (managedBy != null && managedBy.equals(iid)) {
            return true;
        }

        managedBy = getManagedByIidFromOperDS(bridgeIid);
        if (managedBy != null && managedBy.equals(iid)) {
            return true;
        }
        return false;

    }

    private InstanceIdentifier<?> getManagedByIidFromOperDS(InstanceIdentifier<?> bridgeIid) {
        // Get the InstanceIdentifier of the containing node
        InstanceIdentifier<Node> nodeEntryIid = bridgeIid.firstIdentifierOf(Node.class);

        Optional<?> bridgeNode =  SouthboundUtil.readNode(db.newReadWriteTransaction(),nodeEntryIid);
        if (bridgeNode.isPresent() && bridgeNode.orElseThrow() instanceof Node node) {
            OvsdbBridgeAugmentation bridge = node.augmentation(OvsdbBridgeAugmentation.class);
            if (bridge != null) {
                final var managedBy = bridge.getManagedBy();
                if (managedBy != null && managedBy.getValue() instanceof DataObjectIdentifier<?> doi) {
                    return doi.toLegacy();
                }
            }
        }
        return null;
    }

    private InstanceIdentifier<?> getManagedByIid(Map<InstanceIdentifier<?>, DataObject> map,
            InstanceIdentifier<?> iidToCheck) {
        // Get the InstanceIdentifier of the containing node
        InstanceIdentifier<Node> nodeEntryIid = iidToCheck.firstIdentifierOf(Node.class);

        // Look for the Node in the created/updated data
        DataObject dataObject = null;
        if (map != null && map.get(nodeEntryIid) != null) {
            dataObject = map.get(nodeEntryIid);
        }
        // If we are contained in a bridge managed by this iid
        if (dataObject != null && dataObject instanceof Node) {
            Node node = (Node)dataObject;
            OvsdbBridgeAugmentation bridge = node.augmentation(OvsdbBridgeAugmentation.class);
            if (bridge != null) {
                final var managedBy = bridge.getManagedBy();
                if (managedBy != null && managedBy.getValue() instanceof DataObjectIdentifier<?> doi
                    && iid.equals(doi.toLegacy())) {
                    return iid;
                }
            }
        }
        return null;
    }

    @Override
    public Map<InstanceIdentifier<?>, DataObject> getOriginalData() {
        if (this.originalData == null) {
            this.originalData = filter(event.getOriginalData());
        }
        return this.originalData;
    }
}
