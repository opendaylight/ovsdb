/*
* Copyright (c) 2015 Intel Corp. and others.  All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/
package org.opendaylight.overlay.transactions.md;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class DataChangesManagedByOverlayNodeEvent implements
        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> {

    private InstanceIdentifier<?> iid;
    private AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> event;
    private Map<InstanceIdentifier<?>, DataObject> createdData = null;
    private Map<InstanceIdentifier<?>, DataObject> updatedData = null;
    private Map<InstanceIdentifier<?>, DataObject> originalData = null;
    private Set<InstanceIdentifier<?>> removedPaths;

    public DataChangesManagedByOverlayNodeEvent(InstanceIdentifier<?> iid,
                                                AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> event) {
        this.iid = iid;
        this.event = event;
    }

    private Map<InstanceIdentifier<?>, DataObject> filter(Map<InstanceIdentifier<?>,
            DataObject> data) {
        Map<InstanceIdentifier<?>, DataObject> result = new HashMap<InstanceIdentifier<?>, DataObject>();
        for (Entry<InstanceIdentifier<?>, DataObject> entry : data.entrySet()) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    @Override
    public Map<InstanceIdentifier<?>, DataObject> getCreatedData() {
        if (this.createdData == null) {
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
            this.removedPaths = new HashSet<InstanceIdentifier<?>>();
            for (InstanceIdentifier<?> path : event.getRemovedPaths()) {
                this.removedPaths.add(path);
            }
        }
        return this.removedPaths;
    }

    @Override
    public Map<InstanceIdentifier<?>, DataObject> getOriginalData() {
        if (this.originalData == null) {
            this.originalData = filter(event.getOriginalData());
        }
        return this.originalData;
    }

    @Override
    public DataObject getOriginalSubtree() {
        return event.getOriginalSubtree();
    }

    @Override
    public DataObject getUpdatedSubtree() {
        return event.getUpdatedSubtree();
    }

}
