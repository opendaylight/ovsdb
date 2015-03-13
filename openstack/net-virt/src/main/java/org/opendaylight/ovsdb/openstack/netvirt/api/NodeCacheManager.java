/*
 * Copyright (C) 2015 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 *  Authors : Flavio Fernandes
 */
package org.opendaylight.ovsdb.openstack.netvirt.api;

import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;

import java.util.List;

/**
 * This interface is used to cache ids of nodes that are needed by net-virt.
 * The nodes are added and removed by an external listener.
 */
public interface NodeCacheManager {
    public void nodeAdded(String nodeIdentifier);
    public void nodeRemoved(String nodeIdentifier);

    public List<Node> getNodes();
}
