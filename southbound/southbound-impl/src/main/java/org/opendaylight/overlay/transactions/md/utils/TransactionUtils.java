/*
* Copyright (c) 2015 Intel Corp. and others.  All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/
package org.opendaylight.overlay.transactions.md.utils;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class TransactionUtils {
    private static final Logger LOG = LoggerFactory.getLogger(TransactionUtils.class);

    public static Map<InstanceIdentifier<Node>, Node> extractNode(
            Map<InstanceIdentifier<?>, DataObject> changes) {
        Map<InstanceIdentifier<Node>, Node> result = new HashMap<InstanceIdentifier<Node>, Node>();
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

    public static Map<InstanceIdentifier<Link>, Link> extractLink(
            Map<InstanceIdentifier<?>, DataObject> changes) {
        Map<InstanceIdentifier<Link>, Link> result = new HashMap<InstanceIdentifier<Link>, Link>();
        if (changes != null && changes.entrySet() != null) {
            for (Entry<InstanceIdentifier<?>, DataObject> created : changes.entrySet()) {
                if (created.getValue() instanceof Link) {
                    Link value = (Link) created.getValue();
                    Class<?> type = created.getKey().getTargetType();
                    if (type.equals(Link.class)) {
                        @SuppressWarnings("unchecked") // Actually checked above
                                InstanceIdentifier<Link> iid = (InstanceIdentifier<Link>) created.getKey();
                        result.put(iid, value);
                    }
                }
            }
        }
        return result;
    }

    public static <T extends DataObject> Map<InstanceIdentifier<T>, T> extractCreated(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes, Class<T> klazz) {
        return extract(changes.getCreatedData(), klazz);
    }

    public static <T extends DataObject> Map<InstanceIdentifier<T>, T> extractUpdated(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes, Class<T> klazz) {
        return extract(changes.getUpdatedData(), klazz);
    }

    public static <T extends DataObject> Map<InstanceIdentifier<T>, T> extractCreatedOrUpdated(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes, Class<T> klazz) {
        Map<InstanceIdentifier<T>, T> result = extractUpdated(changes, klazz);
        result.putAll(extractCreated(changes, klazz));
        return result;
    }

    public static <T extends DataObject> Map<InstanceIdentifier<T>, T> extractOriginal(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes, Class<T> klazz) {
        return extract(changes.getOriginalData(), klazz);
    }

    public static <T extends DataObject> Set<InstanceIdentifier<T>> extractRemoved(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes, Class<T> klazz) {
        Set<InstanceIdentifier<T>> result = new HashSet<InstanceIdentifier<T>>();
        if (changes != null && changes.getRemovedPaths() != null) {
            for (InstanceIdentifier<?> iid : changes.getRemovedPaths()) {
                if (iid.getTargetType().equals(klazz)) {
                    @SuppressWarnings("unchecked") // Actually checked above
                            InstanceIdentifier<T> iidn = (InstanceIdentifier<T>) iid;
                    result.add(iidn);
                }
            }
        }
        return result;
    }

    public static <T extends DataObject> Map<InstanceIdentifier<T>, T> extract(
            Map<InstanceIdentifier<?>, DataObject> changes, Class<T> klazz) {
        Map<InstanceIdentifier<T>, T> result = new HashMap<InstanceIdentifier<T>, T>();
        if (changes != null && changes.entrySet() != null) {
            for (Entry<InstanceIdentifier<?>, DataObject> created : changes.entrySet()) {
                if (klazz.isInstance(created.getValue())) {
                    T value = (T) created.getValue();
                    Class<?> type = created.getKey().getTargetType();
                    if (type.equals(klazz)) {
                        @SuppressWarnings("unchecked") // Actually checked above
                                InstanceIdentifier<T> iid = (InstanceIdentifier<T>) created.getKey();
                        result.put(iid, value);
                    }
                }
            }
        }
        return result;
    }
}
