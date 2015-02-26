package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbManagedNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactUtils {
    private static final Logger LOG = LoggerFactory.getLogger(TransactUtils.class);

    public static Map<InstanceIdentifier<Node>,OvsdbManagedNodeAugmentation> extractOvsdbManagedNodeCreate(
            AsyncDataChangeEvent<InstanceIdentifier<?>, OvsdbManagedNodeAugmentation> changes) {
        return extractOvsdbManagedNode(changes.getCreatedData());
    }

    public static Map<InstanceIdentifier<Node>,OvsdbManagedNodeAugmentation> extractOvsdbManagedNode(
            AsyncDataChangeEvent<InstanceIdentifier<?>, OvsdbManagedNodeAugmentation> changes) {
        return extractOvsdbManagedNode(changes.getUpdatedData());
    }

    public static Set<InstanceIdentifier<Node>> extractOvsdbManagedNodeRemoved(AsyncDataChangeEvent<InstanceIdentifier<?>, OvsdbManagedNodeAugmentation> changes) {
        Set<InstanceIdentifier<Node>> result = new HashSet<InstanceIdentifier<Node>>();
        for(InstanceIdentifier<?> iid : changes.getRemovedPaths()) {
            if(iid.getTargetType().equals(OvsdbManagedNodeAugmentation.class)) {
                @SuppressWarnings("unchecked") // Actually checked above
                InstanceIdentifier<Node> iidn = (InstanceIdentifier<Node>)iid;
                result.add(iidn);
            }
        }
        return result;
    }


    public static Map<InstanceIdentifier<Node>,OvsdbManagedNodeAugmentation> extractOvsdbManagedNode(
            Map<InstanceIdentifier<?>, OvsdbManagedNodeAugmentation> changes) {
        Map<InstanceIdentifier<Node>,OvsdbManagedNodeAugmentation> result = new HashMap<InstanceIdentifier<Node>,OvsdbManagedNodeAugmentation>();
        for( Entry<InstanceIdentifier<?>, OvsdbManagedNodeAugmentation> created : changes.entrySet()) {
            OvsdbManagedNodeAugmentation value = created.getValue();
            Class<?> type = created.getKey().getTargetType();
            if(type.equals(OvsdbManagedNodeAugmentation.class)) {
                @SuppressWarnings("unchecked") // Actually checked above
                InstanceIdentifier<Node> iid = (InstanceIdentifier<Node>) created.getKey();
                OvsdbManagedNodeAugmentation omn = (OvsdbManagedNodeAugmentation) value;
                result.put(iid, omn);
            }
        }
        return result;
    }

}
