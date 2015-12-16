/*
 * Copyright (C) 2014 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.translator.crud;

import java.util.List;

import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronLoadBalancerHealthMonitor;

/**
 * This interface defines the methods for CRUD of NB OpenStack LoadBalancerHealthMonitor objects
 *
 */

public interface INeutronLoadBalancerHealthMonitorCRUD {
    /**
     * Applications call this interface method to determine if a particular
     *LoadBalancerHealthMonitor object exists
     *
     * @param uuid
     *            UUID of the LoadBalancerHealthMonitor object
     * @return boolean
     */

    boolean neutronLoadBalancerHealthMonitorExists(String uuid);

    /**
     * Applications call this interface method to return if a particular
     * LoadBalancerHealthMonitor object exists
     *
     * @param uuid
     *            UUID of the LoadBalancerHealthMonitor object
     * @return {@link NeutronLoadBalancerHealthMonitor}
     *          OpenStackLoadBalancerHealthMonitor class
     */

    NeutronLoadBalancerHealthMonitor getNeutronLoadBalancerHealthMonitor(String uuid);

    /**
     * Applications call this interface method to return all LoadBalancerHealthMonitor objects
     *
     * @return List of OpenStackNetworks objects
     */

    List<NeutronLoadBalancerHealthMonitor> getAllNeutronLoadBalancerHealthMonitors();

    /**
     * Applications call this interface method to add a LoadBalancerHealthMonitor object to the
     * concurrent map
     *
     * @param input
     *            OpenStackNetwork object
     * @return boolean on whether the object was added or not
     */

    boolean addNeutronLoadBalancerHealthMonitor(NeutronLoadBalancerHealthMonitor input);

    /**
     * Applications call this interface method to remove a Neutron LoadBalancerHealthMonitor object to the
     * concurrent map
     *
     * @param uuid
     *            identifier for the LoadBalancerHealthMonitor object
     * @return boolean on whether the object was removed or not
     */

    boolean removeNeutronLoadBalancerHealthMonitor(String uuid);

    /**
     * Applications call this interface method to edit a LoadBalancerHealthMonitor object
     *
     * @param uuid
     *            identifier of the LoadBalancerHealthMonitor object
     * @param delta
     *            OpenStackLoadBalancerHealthMonitor object containing changes to apply
     * @return boolean on whether the object was updated or not
     */

    boolean updateNeutronLoadBalancerHealthMonitor(String uuid, NeutronLoadBalancerHealthMonitor delta);

    /**
     * Applications call this interface method to see if a MAC address is in use
     *
     * @param uuid
     *            identifier of the LoadBalancerHealthMonitor object
     * @return boolean on whether the macAddress is already associated with a
     * port or not
     */

    boolean neutronLoadBalancerHealthMonitorInUse(String uuid);

}
