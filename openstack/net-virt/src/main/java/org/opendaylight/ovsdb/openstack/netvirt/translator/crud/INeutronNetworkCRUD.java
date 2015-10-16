/*
 * Copyright (c) 2013, 2015 IBM Corporation and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.translator.crud;

import java.util.List;

import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronNetwork;

/**
 * This interface defines the methods for CRUD of NB network objects
 *
 */

public interface INeutronNetworkCRUD {
    /**
     * Applications call this interface method to determine if a particular
     * Network object exists
     *
     * @param uuid
     *            UUID of the Network object
     * @return boolean
     */

    boolean networkExists(String uuid);

    /**
     * Applications call this interface method to return if a particular
     * Network object exists
     *
     * @param uuid
     *            UUID of the Network object
     * @return {@link org.opendaylight.neutron.spi.NeutronNetwork}
     *          OpenStack Network class
     */

    NeutronNetwork getNetwork(String uuid);

    /**
     * Applications call this interface method to return all Network objects
     *
     * @return List of OpenStackNetworks objects
     */

    List<NeutronNetwork> getAllNetworks();

    /**
     * Applications call this interface method to add a Network object to the
     * concurrent map
     *
     * @param input
     *            OpenStackNetwork object
     * @return boolean on whether the object was added or not
     */

    boolean addNetwork(NeutronNetwork input);

    /**
     * Applications call this interface method to remove a Network object to the
     * concurrent map
     *
     * @param uuid
     *            identifier for the network object
     * @return boolean on whether the object was removed or not
     */

    boolean removeNetwork(String uuid);

    /**
     * Applications call this interface method to edit a Network object
     *
     * @param uuid
     *            identifier of the network object
     * @param delta
     *            OpenStackNetwork object containing changes to apply
     * @return boolean on whether the object was updated or not
     */

    boolean updateNetwork(String uuid, NeutronNetwork delta);

    /**
     * Applications call this interface method to determine if a Network object
     * is use
     *
     * @param netUUID
     *            identifier of the network object
     *
     * @return boolean on whether the network is in use or not
     */

    boolean networkInUse(String netUUID);
}
