/*
 * Copyright (c) 2013, 2015 IBM Corporation and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.translator.crud;

import java.util.List;

import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronRouter;

/**
 * This interface defines the methods for CRUD of NB Router objects
 *
 */

public interface INeutronRouterCRUD {
    /**
     * Applications call this interface method to determine if a particular
     * Router object exists
     *
     * @param uuid
     *            UUID of the Router object
     * @return boolean
     */

    boolean routerExists(String uuid);

    /**
     * Applications call this interface method to return if a particular
     * Router object exists
     *
     * @param uuid
     *            UUID of the Router object
     * @return {@link org.opendaylight.neutron.spi.NeutronRouter}
     *          OpenStack Router class
     */

    NeutronRouter getRouter(String uuid);

    /**
     * Applications call this interface method to return all Router objects
     *
     * @return List of OpenStackRouters objects
     */

    List<NeutronRouter> getAllRouters();

    /**
     * Applications call this interface method to add a Router object to the
     * concurrent map
     *
     * @param input
     *            OpenStackRouter object
     * @return boolean on whether the object was added or not
     */

    boolean addRouter(NeutronRouter input);

    /**
     * Applications call this interface method to remove a Router object to the
     * concurrent map
     *
     * @param uuid
     *            identifier for the Router object
     * @return boolean on whether the object was removed or not
     */

    boolean removeRouter(String uuid);

    /**
     * Applications call this interface method to edit a Router object
     *
     * @param uuid
     *            identifier of the Router object
     * @param delta
     *            OpenStackRouter object containing changes to apply
     * @return boolean on whether the object was updated or not
     */

    boolean updateRouter(String uuid, NeutronRouter delta);

    /**
     * Applications call this interface method to check if a router is in use
     *
     * @param routerUUID
     *            identifier of the Router object
     * @return boolean on whether the router is in use or not
     */

    boolean routerInUse(String routerUUID);
}
