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

/**
 * When this interface is used, instance owner will get callbacks on
 * changes that occur in NodeCacheManager
 */
public interface NodeCacheListener {

    public void notifyNode(Node node, Action action);
}
