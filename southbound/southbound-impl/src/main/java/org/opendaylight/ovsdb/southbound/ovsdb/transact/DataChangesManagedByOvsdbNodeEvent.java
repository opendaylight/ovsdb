package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbManagedNodeAugmentation;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class DataChangesManagedByOvsdbNodeEvent implements
        AsyncDataChangeEvent<InstanceIdentifier<?>, OvsdbManagedNodeAugmentation> {
    //primary fields
    private AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> event;
    private InstanceIdentifier<?> iid;

    // local caches of computed data
    private Map<InstanceIdentifier<?>, OvsdbManagedNodeAugmentation> createdData = null;
    private Map<InstanceIdentifier<?>, OvsdbManagedNodeAugmentation> updatedData = null;
    private Set<InstanceIdentifier<?>> removedPaths = null;
    private Map<InstanceIdentifier<?>, OvsdbManagedNodeAugmentation> originalData;

    public DataChangesManagedByOvsdbNodeEvent(InstanceIdentifier<?> iid, AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> event) {
        this.iid = iid;
        this.event = event;
    }

    private Map<InstanceIdentifier<?>, OvsdbManagedNodeAugmentation> filter(Map<InstanceIdentifier<?>, DataObject> data) {
        Map<InstanceIdentifier<?>, OvsdbManagedNodeAugmentation> result = new HashMap<InstanceIdentifier<?>, OvsdbManagedNodeAugmentation>();
        for(Entry<InstanceIdentifier<?>, DataObject> created: data.entrySet()) {
            if(created.getValue() != null
                    && created.getValue() instanceof OvsdbManagedNodeAugmentation
                    && ((OvsdbManagedNodeAugmentation)created.getValue()).getManagedBy() != null
                    && ((OvsdbManagedNodeAugmentation)created.getValue()).getManagedBy().getValue() != null
                    && ((OvsdbManagedNodeAugmentation)created.getValue()).getManagedBy().getValue().equals(iid)) {
                result.put(created.getKey(),((OvsdbManagedNodeAugmentation)created.getValue()));
            }
        }
        return result;
    }

    @Override
    public Map<InstanceIdentifier<?>, OvsdbManagedNodeAugmentation> getCreatedData() {
        if(this.createdData == null) {
            this.createdData = filter(event.getCreatedData());
        }
        return this.createdData;
    }

    @Override
    public Map<InstanceIdentifier<?>, OvsdbManagedNodeAugmentation> getUpdatedData() {
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
                        && original instanceof OvsdbManagedNodeAugmentation
                        && ((OvsdbManagedNodeAugmentation)original).getManagedBy().equals(this.iid)) {
                    this.removedPaths.add(path);
                }
            }
        }
        return this.removedPaths;
    }

    @Override
    public Map<InstanceIdentifier<?>, OvsdbManagedNodeAugmentation> getOriginalData() {
        if(this.originalData == null) {
            this.originalData = filter(event.getOriginalData());
        }
        return this.originalData;
    }

    @Override
    public OvsdbManagedNodeAugmentation getOriginalSubtree() {
        if(this.event.getOriginalSubtree() instanceof OvsdbManagedNodeAugmentation) {
            return (OvsdbManagedNodeAugmentation)this.event.getOriginalSubtree();
        } else {
            return null;
        }
    }

    @Override
    public OvsdbManagedNodeAugmentation getUpdatedSubtree() {
        if(this.event.getUpdatedSubtree() instanceof OvsdbManagedNodeAugmentation) {
            return (OvsdbManagedNodeAugmentation)this.event.getUpdatedSubtree();
        } else {
            return null;
        }
    }
}
