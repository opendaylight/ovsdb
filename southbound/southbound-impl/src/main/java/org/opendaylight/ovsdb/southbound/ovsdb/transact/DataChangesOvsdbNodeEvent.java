/*
 * Copyright (c) 2015 Intel Corporation and others.  All rights reserved.
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class DataChangesOvsdbNodeEvent implements
        AsyncDataChangeEvent<InstanceIdentifier<?>, OvsdbNodeAugmentation> {
    //primary fields
    private AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> event;
    private InstanceIdentifier<?> iid;

    // local caches of computed data
    private Map<InstanceIdentifier<?>, OvsdbNodeAugmentation> createdData = null;
    private Map<InstanceIdentifier<?>, OvsdbNodeAugmentation> updatedData = null;
    private Set<InstanceIdentifier<?>> removedPaths = null;
    private Map<InstanceIdentifier<?>, OvsdbNodeAugmentation> originalData;

    public DataChangesOvsdbNodeEvent(InstanceIdentifier<?> iid,
                                              AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> event) {
        this.iid = iid;
        this.event = event;
    }

    private Map<InstanceIdentifier<?>, OvsdbNodeAugmentation> filter(Map<InstanceIdentifier<?>,
            DataObject> data) {
        Map<InstanceIdentifier<?>, OvsdbNodeAugmentation> result
            = new HashMap<InstanceIdentifier<?>, OvsdbNodeAugmentation>();
        for (Entry<InstanceIdentifier<?>, DataObject> created: data.entrySet()) {
            if (created.getValue() != null
                    && created.getValue() instanceof OvsdbNodeAugmentation
                    && ((OvsdbNodeAugmentation)created.getValue()).getIp() != null
                    && ((OvsdbNodeAugmentation)created.getValue()).getIp().getValue() != null
                    && ((OvsdbNodeAugmentation)created.getValue()).getPort() != null
                    && ((OvsdbNodeAugmentation)created.getValue()).getPort().getValue() != null) {
                result.put(created.getKey(),((OvsdbNodeAugmentation)created.getValue()));
            }
        }
        return result;
    }

    @Override
    public Map<InstanceIdentifier<?>, OvsdbNodeAugmentation> getCreatedData() {
        if (this.createdData == null) {
            this.createdData = filter(event.getCreatedData());
        }
        return this.createdData;
    }

    @Override
    public Map<InstanceIdentifier<?>, OvsdbNodeAugmentation> getUpdatedData() {
        if (this.updatedData == null) {
            this.updatedData = filter(event.getUpdatedData());
        }
        return this.updatedData;
    }

    @Override
    public Set<InstanceIdentifier<?>> getRemovedPaths() {
        if (this.removedPaths == null) {
            this.removedPaths = new HashSet<InstanceIdentifier<?>>();
            for (InstanceIdentifier<?> path: event.getRemovedPaths()) {
                DataObject original = this.event.getOriginalData().get(path);
                if (original != null
                        && original instanceof OvsdbNodeAugmentation) {
                    this.removedPaths.add(path);
                }
            }
        }
        return this.removedPaths;
    }

    @Override
    public Map<InstanceIdentifier<?>, OvsdbNodeAugmentation> getOriginalData() {
        if (this.originalData == null) {
            this.originalData = filter(event.getOriginalData());
        }
        return this.originalData;
    }

    @Override
    public OvsdbNodeAugmentation getOriginalSubtree() {
        if (this.event.getOriginalSubtree() instanceof OvsdbNodeAugmentation) {
            return (OvsdbNodeAugmentation)this.event.getOriginalSubtree();
        } else {
            return null;
        }
    }

    @Override
    public OvsdbNodeAugmentation getUpdatedSubtree() {
        if (this.event.getUpdatedSubtree() instanceof OvsdbNodeAugmentation) {
            return (OvsdbNodeAugmentation)this.event.getUpdatedSubtree();
        } else {
            return null;
        }
    }
}
