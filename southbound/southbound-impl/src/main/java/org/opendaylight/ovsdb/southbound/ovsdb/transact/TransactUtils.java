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
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactUtils {
    private static final Logger LOG = LoggerFactory.getLogger(TransactUtils.class);

    public static Map<InstanceIdentifier<Node>,OvsdbBridgeAugmentation> extractOvsdbManagedNodeCreate(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        return extractOvsdbManagedNode(changes.getCreatedData());
    }

    public static Map<InstanceIdentifier<Node>,OvsdbBridgeAugmentation> extractOvsdbManagedNode(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        return extractOvsdbManagedNode(changes.getUpdatedData());
    }

    public static Set<InstanceIdentifier<Node>> extractOvsdbManagedNodeRemoved(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        Set<InstanceIdentifier<Node>> result = new HashSet<InstanceIdentifier<Node>>();
        if (changes != null && changes.getRemovedPaths() != null) {
            for (InstanceIdentifier<?> iid : changes.getRemovedPaths()) {
                if (iid.getTargetType().equals(OvsdbBridgeAugmentation.class)) {
                    @SuppressWarnings("unchecked") // Actually checked above
                    InstanceIdentifier<Node> iidn = (InstanceIdentifier<Node>)iid;
                    result.add(iidn);
                }
            }
        }
        return result;
    }

    public static Map<InstanceIdentifier<Node>,OvsdbBridgeAugmentation> extractOvsdbManagedNodeOriginal(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes)  {
        return extractOvsdbManagedNode(changes.getOriginalData());
    }


    public static Map<InstanceIdentifier<Node>,OvsdbBridgeAugmentation> extractOvsdbManagedNode(
            Map<InstanceIdentifier<?>, DataObject> changes) {
        Map<InstanceIdentifier<Node>,OvsdbBridgeAugmentation> result
            = new HashMap<InstanceIdentifier<Node>,OvsdbBridgeAugmentation>();
        if (changes != null && changes.entrySet() != null) {
            for (Entry<InstanceIdentifier<?>, DataObject> created : changes.entrySet()) {
                if (created.getValue() instanceof OvsdbBridgeAugmentation) {
                    OvsdbBridgeAugmentation value = (OvsdbBridgeAugmentation) created.getValue();
                    Class<?> type = created.getKey().getTargetType();
                    if (type.equals(OvsdbBridgeAugmentation.class)) {
                        @SuppressWarnings("unchecked") // Actually checked above
                        InstanceIdentifier<Node> iid = (InstanceIdentifier<Node>) created.getKey();
                        OvsdbBridgeAugmentation ovsdbManagedNode = (OvsdbBridgeAugmentation) value;
                        result.put(iid, ovsdbManagedNode);
                    }
                }
            }
        }
        return result;
    }

    public static Map<InstanceIdentifier<Node>,Node> extractNodeUpdated(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        return extractNode(changes.getUpdatedData());
    }

    public static Map<InstanceIdentifier<Node>,Node> extractNodeCreated(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        return extractNode(changes.getCreatedData());
    }

    public static Map<InstanceIdentifier<Node>,Node> extractNode(
            Map<InstanceIdentifier<?>, DataObject> changes) {
        Map<InstanceIdentifier<Node>,Node> result
            = new HashMap<InstanceIdentifier<Node>,Node>();
        if (changes != null && changes.entrySet() != null) {
            for (Entry<InstanceIdentifier<?>, DataObject> created : changes.entrySet()) {
                if (created.getValue() instanceof Node) {
                    Node value = (Node) created.getValue();
                    Class<?> type = created.getKey().getTargetType();
                    if (type.equals(Node.class)) {
                        @SuppressWarnings("unchecked") // Actually checked above
                        InstanceIdentifier<Node> iid = (InstanceIdentifier<Node>) created.getKey();
                        result.put(iid, value);
                    }
                }
            }
        }
        return result;
    }

}
