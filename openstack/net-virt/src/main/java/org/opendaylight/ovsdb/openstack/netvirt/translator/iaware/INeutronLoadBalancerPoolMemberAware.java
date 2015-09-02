/*
 * Copyright (C) 2014 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.openstack.netvirt.translator.iaware;

import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronLoadBalancerPoolMember;

public interface INeutronLoadBalancerPoolMemberAware {


    /**
     * Services provide this interface method to indicate if the specified loadBalancerPoolMember can be created
     *
     * @param loadBalancerPoolMember
     *            instance of proposed new LoadBalancerPool object
     * @return integer
     *            the return value is understood to be a HTTP status code.  A return value outside of 200 through 299
     *            results in the create operation being interrupted and the returned status value reflected in the
     *            HTTP response.
     */
    int canCreateNeutronLoadBalancerPoolMember(NeutronLoadBalancerPoolMember loadBalancerPoolMember);

    /**
     * Services provide this interface method for taking action after a loadBalancerPoolMember has been created
     *
     * @param loadBalancerPoolMember
     *            instance of new LoadBalancerPool object
     */
    void neutronLoadBalancerPoolMemberCreated(NeutronLoadBalancerPoolMember loadBalancerPoolMember);

    /**
     * Services provide this interface method to indicate if the specified loadBalancerPoolMember can be changed using the specified
     * delta
     *
     * @param delta
     *            updates to the loadBalancerPoolMember object using patch semantics
     * @param original
     *            instance of the LoadBalancerPool object to be updated
     * @return integer
     *            the return value is understood to be a HTTP status code.  A return value outside of 200 through 299
     *            results in the update operation being interrupted and the returned status value reflected in the
     *            HTTP response.
     */
    int canUpdateNeutronLoadBalancerPoolMember(NeutronLoadBalancerPoolMember delta,
            NeutronLoadBalancerPoolMember original);

    /**
     * Services provide this interface method for taking action after a loadBalancerPoolMember has been updated
     *
     * @param loadBalancerPoolMember
     *            instance of modified LoadBalancerPool object
     */
    void neutronLoadBalancerPoolMemberUpdated(NeutronLoadBalancerPoolMember loadBalancerPoolMember);

    /**
     * Services provide this interface method to indicate if the specified loadBalancerPoolMember can be deleted
     *
     * @param loadBalancerPoolMember
     *            instance of the LoadBalancerPool object to be deleted
     * @return integer
     *            the return value is understood to be a HTTP status code.  A return value outside of 200 through 299
     *            results in the delete operation being interrupted and the returned status value reflected in the
     *            HTTP response.
     */
    int canDeleteNeutronLoadBalancerPoolMember(NeutronLoadBalancerPoolMember loadBalancerPoolMember);

    /**
     * Services provide this interface method for taking action after a loadBalancerPoolMember has been deleted
     *
     * @param loadBalancerPoolMember
     *            instance of deleted LoadBalancerPool object
     */
    void neutronLoadBalancerPoolMemberDeleted(NeutronLoadBalancerPoolMember loadBalancerPoolMember);
}
