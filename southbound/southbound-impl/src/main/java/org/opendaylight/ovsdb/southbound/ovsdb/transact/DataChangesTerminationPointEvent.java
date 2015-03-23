/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class DataChangesTerminationPointEvent implements AsyncDataChangeEvent<InstanceIdentifier<?>, OvsdbTerminationPointAugmentation>{

    private AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> event;
    private InstanceIdentifier<?> iid;

    // local caches of computed data
    private Map<InstanceIdentifier<?>, OvsdbTerminationPointAugmentation> createdData = null;
    private Map<InstanceIdentifier<?>, OvsdbTerminationPointAugmentation> updatedData = null;
    private Set<InstanceIdentifier<?>> removedPaths = null;
    private Map<InstanceIdentifier<?>, OvsdbTerminationPointAugmentation> originalData;

    public DataChangesTerminationPointEvent(InstanceIdentifier<?> iid, AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> event) {
        this.iid = iid;
        this.event = event;
    }

    private Map<InstanceIdentifier<?>, OvsdbTerminationPointAugmentation> filter(Map<InstanceIdentifier<?>, DataObject> data) {
        Map<InstanceIdentifier<?>, OvsdbTerminationPointAugmentation> result = new HashMap<InstanceIdentifier<?>, OvsdbTerminationPointAugmentation>();
        for(Entry<InstanceIdentifier<?>, DataObject> created: data.entrySet()) {
            if(created.getValue() != null
                    && created.getValue() instanceof OvsdbTerminationPointAugmentation
                    && ((OvsdbTerminationPointAugmentation)created.getValue()).getAttachedTo() != null
                    && ((OvsdbTerminationPointAugmentation)created.getValue()).getAttachedTo().getValue() != null) {
                result.put(created.getKey(),((OvsdbTerminationPointAugmentation)created.getValue()));
            }
        }
        return result;
    }

    @Override
    public Map<InstanceIdentifier<?>, OvsdbTerminationPointAugmentation> getCreatedData() {
        if(this.createdData == null) {
            this.createdData = filter(event.getCreatedData());
        }
        return this.createdData;
    }

    @Override
    public Map<InstanceIdentifier<?>, OvsdbTerminationPointAugmentation> getUpdatedData() {
        if(this.updatedData == null) {
            this.updatedData = filter(event.getUpdatedData());
        }
        return this.updatedData;
    }

    @Override
    public Set<InstanceIdentifier<?>> getRemovedPaths() {
        if(this.removedPaths != null) {
            this.removedPaths = new HashSet<InstanceIdentifier<?>>();
            for(InstanceIdentifier<?> path: event.getRemovedPaths()) {
                DataObject original = this.event.getOriginalData().get(path);
                if(original != null
                        && original instanceof OvsdbTerminationPointAugmentation) {
                    this.removedPaths.add(path);
                }
            }
        }
        return this.removedPaths;
    }

    @Override
    public Map<InstanceIdentifier<?>, OvsdbTerminationPointAugmentation> getOriginalData() {
        if(this.originalData == null) {
            this.originalData = filter(event.getOriginalData());
        }
        return this.originalData;
    }

    @Override
    public OvsdbTerminationPointAugmentation getOriginalSubtree() {
        if(this.event.getOriginalSubtree() instanceof OvsdbTerminationPointAugmentation) {
            return (OvsdbTerminationPointAugmentation)this.event.getOriginalSubtree();
        } else {
            return null;
        }
    }

    @Override
    public OvsdbTerminationPointAugmentation getUpdatedSubtree() {
        if(this.event.getUpdatedSubtree() instanceof OvsdbTerminationPointAugmentation) {
            return (OvsdbTerminationPointAugmentation)this.event.getUpdatedSubtree();
        } else {
            return null;
        }
    }

}
