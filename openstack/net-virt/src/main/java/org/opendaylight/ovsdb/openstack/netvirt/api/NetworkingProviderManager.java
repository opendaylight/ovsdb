/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 *  Authors : Dave Tucker
 */

package org.opendaylight.ovsdb.openstack.netvirt.api;

import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;

/**
 * The NetworkingProviderManager handles the mapping between {@link org.opendaylight.controller.sal.core.Node}
 * and registered {@link org.opendaylight.ovsdb.openstack.netvirt.api.NetworkingProvider} implementations
 */
public interface NetworkingProviderManager {
    /**
     * Returns the Networking Provider for a given node
     * @param node a {@link org.opendaylight.controller.sal.core.Node}
     * @return a NetworkProvider
     * @see NetworkingProvider
     */
    NetworkingProvider getProvider(Node node);
}
