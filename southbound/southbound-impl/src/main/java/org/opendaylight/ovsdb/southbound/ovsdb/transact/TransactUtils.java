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
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactUtils {
    private static final Logger LOG = LoggerFactory.getLogger(TransactUtils.class);

    public static Map<InstanceIdentifier<Node>,OvsdbBridgeAugmentation> extractOvsdbManagedNodeCreate(
            AsyncDataChangeEvent<InstanceIdentifier<?>, OvsdbBridgeAugmentation> changes) {
        return extractOvsdbManagedNode(changes.getCreatedData());
    }

    public static Map<InstanceIdentifier<Node>,OvsdbBridgeAugmentation> extractOvsdbManagedNode(
            AsyncDataChangeEvent<InstanceIdentifier<?>, OvsdbBridgeAugmentation> changes) {
        return extractOvsdbManagedNode(changes.getUpdatedData());
    }

    public static Set<InstanceIdentifier<Node>> extractOvsdbManagedNodeRemoved(AsyncDataChangeEvent<InstanceIdentifier<?>, OvsdbBridgeAugmentation> changes) {
        Set<InstanceIdentifier<Node>> result = new HashSet<InstanceIdentifier<Node>>();
        for(InstanceIdentifier<?> iid : changes.getRemovedPaths()) {
            if(iid.getTargetType().equals(OvsdbBridgeAugmentation.class)) {
                @SuppressWarnings("unchecked") // Actually checked above
                InstanceIdentifier<Node> iidn = (InstanceIdentifier<Node>)iid;
                result.add(iidn);
            }
        }
        return result;
    }


    public static Map<InstanceIdentifier<Node>,OvsdbBridgeAugmentation> extractOvsdbManagedNode(
            Map<InstanceIdentifier<?>, OvsdbBridgeAugmentation> changes) {
        Map<InstanceIdentifier<Node>,OvsdbBridgeAugmentation> result = new HashMap<InstanceIdentifier<Node>,OvsdbBridgeAugmentation>();
        for( Entry<InstanceIdentifier<?>, OvsdbBridgeAugmentation> created : changes.entrySet()) {
            OvsdbBridgeAugmentation value = created.getValue();
            Class<?> type = created.getKey().getTargetType();
            if(type.equals(OvsdbBridgeAugmentation.class)) {
                @SuppressWarnings("unchecked") // Actually checked above
                InstanceIdentifier<Node> iid = (InstanceIdentifier<Node>) created.getKey();
                OvsdbBridgeAugmentation ovsdbManagedNode = (OvsdbBridgeAugmentation) value;
                result.put(iid, ovsdbManagedNode);
            }
        }
        return result;
    }

}
