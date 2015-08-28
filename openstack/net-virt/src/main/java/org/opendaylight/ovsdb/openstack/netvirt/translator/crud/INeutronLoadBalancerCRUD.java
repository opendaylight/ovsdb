/*
 * Copyright (C) 2014 Red Hat, Inc.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.translator.crud;

import java.util.List;

import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronLoadBalancer;

/**
 * This interface defines the methods for CRUD of NB OpenStack LoadBalancer objects
 *
 */

public interface INeutronLoadBalancerCRUD {
    /**
     * Applications call this interface method to determine if a particular
     *LoadBalancer object exists
     *
     * @param uuid
     *            UUID of the LoadBalancer object
     * @return boolean
     */

    boolean neutronLoadBalancerExists(String uuid);

    /**
     * Applications call this interface method to return if a particular
     * LoadBalancer object exists
     *
     * @param uuid
     *            UUID of the LoadBalancer object
     * @return {@link NeutronLoadBalancer}
     *          OpenStackLoadBalancer class
     */

    NeutronLoadBalancer getNeutronLoadBalancer(String uuid);

    /**
     * Applications call this interface method to return all LoadBalancer objects
     *
     * @return List of OpenStackNetworks objects
     */

    List<NeutronLoadBalancer> getAllNeutronLoadBalancers();

    /**
     * Applications call this interface method to add a LoadBalancer object to the
     * concurrent map
     *
     * @param input
     *            OpenStackNetwork object
     * @return boolean on whether the object was added or not
     */

    boolean addNeutronLoadBalancer(NeutronLoadBalancer input);

    /**
     * Applications call this interface method to remove a Neutron LoadBalancer object to the
     * concurrent map
     *
     * @param uuid
     *            identifier for the LoadBalancer object
     * @return boolean on whether the object was removed or not
     */

    boolean removeNeutronLoadBalancer(String uuid);

    /**
     * Applications call this interface method to edit a LoadBalancer object
     *
     * @param uuid
     *            identifier of the LoadBalancer object
     * @param delta
     *            OpenStackLoadBalancer object containing changes to apply
     * @return boolean on whether the object was updated or not
     */

    boolean updateNeutronLoadBalancer(String uuid, NeutronLoadBalancer delta);

    /**
     * Applications call this interface method to see if a MAC address is in use
     *
     * @param uuid
     *            identifier of the LoadBalancer object
     * @return boolean on whether the macAddress is already associated with a
     * port or not
     */

    boolean neutronLoadBalancerInUse(String uuid);

}
