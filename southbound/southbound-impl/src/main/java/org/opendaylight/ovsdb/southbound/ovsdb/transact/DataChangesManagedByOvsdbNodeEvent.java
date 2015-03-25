/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
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
import java.util.Set;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeRef;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class DataChangesManagedByOvsdbNodeEvent implements
        AsyncDataChangeEvent<InstanceIdentifier<?>, OvsdbBridgeAugmentation> {
    //primary fields
    private AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> event;
    private InstanceIdentifier<?> iid;

    // local caches of computed data
    private Map<InstanceIdentifier<?>, OvsdbBridgeAugmentation> createdData = null;
    private Map<InstanceIdentifier<?>, OvsdbBridgeAugmentation> updatedData = null;
    private Set<InstanceIdentifier<?>> removedPaths = null;
    private Map<InstanceIdentifier<?>, OvsdbBridgeAugmentation> originalData;

    public DataChangesManagedByOvsdbNodeEvent(InstanceIdentifier<?> iid, AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> event) {
        this.iid = iid;
        this.event = event;
    }

    private Map<InstanceIdentifier<?>, OvsdbBridgeAugmentation> filter(Map<InstanceIdentifier<?>, DataObject> data) {
        Map<InstanceIdentifier<?>, OvsdbBridgeAugmentation> result = new HashMap<InstanceIdentifier<?>, OvsdbBridgeAugmentation>();
        for(Entry<InstanceIdentifier<?>, DataObject> created: data.entrySet()) {
            if(created.getValue() != null
                    && created.getValue() instanceof OvsdbBridgeAugmentation
                    && ((OvsdbBridgeAugmentation)created.getValue()).getManagedBy() != null
                    && ((OvsdbBridgeAugmentation)created.getValue()).getManagedBy().getValue() != null
                    && ((OvsdbBridgeAugmentation)created.getValue()).getManagedBy().getValue().equals(iid)) {
                result.put(created.getKey(),((OvsdbBridgeAugmentation)created.getValue()));
            }
        }
        return result;
    }

    @Override
    public Map<InstanceIdentifier<?>, OvsdbBridgeAugmentation> getCreatedData() {
        if(this.createdData == null) {
            this.createdData = filter(event.getCreatedData());
        }
        return this.createdData;
    }

    @Override
    public Map<InstanceIdentifier<?>, OvsdbBridgeAugmentation> getUpdatedData() {
        if(this.updatedData == null) {
            this.updatedData = filter(event.getUpdatedData());
        }
        return this.updatedData;
    }

    @Override
    public Set<InstanceIdentifier<?>> getRemovedPaths() {
        if(this.removedPaths == null) {
            this.removedPaths = new HashSet<InstanceIdentifier<?>>();
            for(InstanceIdentifier<?> path: event.getRemovedPaths()) {
                DataObject original = this.event.getOriginalData().get(path);
                if(original != null
                        && original instanceof OvsdbBridgeAugmentation) {
                    OvsdbBridgeAugmentation ovsdbBridgeNode = (OvsdbBridgeAugmentation)original;
                    OvsdbNodeRef ovsdbNodeRef = ovsdbBridgeNode.getManagedBy();
                    if(ovsdbNodeRef != null) {
                        InstanceIdentifier<?> ovsdbNodeIid = ovsdbNodeRef.getValue();
                        if(ovsdbNodeIid.equals(this.iid)){
                            this.removedPaths.add(path);
                        }
                    }
                }
            }
        }
        return this.removedPaths;
    }

    @Override
    public Map<InstanceIdentifier<?>, OvsdbBridgeAugmentation> getOriginalData() {
        if(this.originalData == null) {
            this.originalData = filter(event.getOriginalData());
        }
        return this.originalData;
    }

    @Override
    public OvsdbBridgeAugmentation getOriginalSubtree() {
        if(this.event.getOriginalSubtree() instanceof OvsdbBridgeAugmentation) {
            return (OvsdbBridgeAugmentation)this.event.getOriginalSubtree();
        } else {
            return null;
        }
    }

    @Override
    public OvsdbBridgeAugmentation getUpdatedSubtree() {
        if(this.event.getUpdatedSubtree() instanceof OvsdbBridgeAugmentation) {
            return (OvsdbBridgeAugmentation)this.event.getUpdatedSubtree();
        } else {
            return null;
        }
    }
}
