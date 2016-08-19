/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.ha.listeners;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class HACache {
    static ConcurrentHashMap<InstanceIdentifier<Node>,Set<InstanceIdentifier<Node>>> haParentToChildMap = new ConcurrentHashMap<>();
    static ConcurrentHashMap<InstanceIdentifier<Node>,InstanceIdentifier<Node>> haChildToParentMap = new ConcurrentHashMap<>();
    static ConcurrentHashMap<InstanceIdentifier<Node>,Boolean> haParentNodes = new ConcurrentHashMap<>();
    static ConcurrentHashMap<InstanceIdentifier<Node>,Boolean> haChildNodes = new ConcurrentHashMap<>();

    public static boolean isHAParentNode(InstanceIdentifier<Node> node) {
        return haParentNodes.containsKey(node);
    }

    public static boolean isHAChildNode(InstanceIdentifier<Node> node) {
        return haChildNodes.containsKey(node);
    }

    public static Set<InstanceIdentifier<Node>> getChildren(InstanceIdentifier<Node> parent) {
        return haParentToChildMap.get(parent);
    }

    public static InstanceIdentifier<Node> getParent(InstanceIdentifier<Node> child) {
        return haChildToParentMap.get(child);
    }

    public static synchronized void cleanupParent(InstanceIdentifier<Node> parent) {
        haParentNodes.remove(parent);

        if (haParentToChildMap.get(parent) != null) {
            Set<InstanceIdentifier<Node>> childs = haParentToChildMap.get(parent);
            for (InstanceIdentifier<Node> child : childs) {
                haChildNodes.remove(child);
                haChildToParentMap.remove(child);
            }
        }
        haParentToChildMap.remove(parent);
    }

    public static synchronized void addChild(InstanceIdentifier<Node> parent, InstanceIdentifier<Node> child) {
        if (haParentToChildMap.get(parent) == null) {
            haParentToChildMap.put(parent, new HashSet<InstanceIdentifier<Node>>());
        }
        haParentToChildMap.get(parent).add(child);
        haChildToParentMap.put(child, parent);
        haParentNodes.put(parent, Boolean.TRUE);
        haChildNodes.put(child, Boolean.TRUE);
    }
}
