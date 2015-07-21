/*
 * Copyright (c) 2015 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.openstack.netvirt.api;

import java.util.List;
import java.util.Map;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.osgi.framework.ServiceReference;

/**
 * This interface is used to cache ids of nodes that are needed by net-virt.
 * The nodes are added and removed by an external listener.
 *
 * @author Flavio Fernandes (ffernand@redhat.com)
 * @author Sam Hague (shague@redhat.com)
 */
public interface NodeCacheManager {
    void nodeAdded(Node node);
    void nodeRemoved(Node node);
    List<Node> getNodes();
    Map<NodeId, Node> getOvsdbNodes();
    List<Node> getBridgeNodes();
    void cacheListenerAdded(final ServiceReference ref, NodeCacheListener handler);
    void cacheListenerRemoved(final ServiceReference ref);

}
