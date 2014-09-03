/*
 * Copyright (C) 2014 SDN Hub, LLC.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Srini Seetharaman
 */

package org.opendaylight.ovsdb.openstack.netvirt.api;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.ovsdb.openstack.netvirt.api.LoadBalancerConfiguration.LoadBalancerPoolMember;

/**
 * This interface allows load-balancer flows to be written to nodes
 */
public interface LoadBalancerProvider {

    Status programLoadBalancerRules(Node node,
            LoadBalancerConfiguration lbConfig, Action action);

    Status programLoadBalancerMemberRules(Node node,
            LoadBalancerConfiguration lbConfig, LoadBalancerPoolMember member, Action action);

}
