/*
 * Copyright (c) 2013, 2015 IBM Corporation and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.translator.crud;

import java.util.List;

import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronSubnet;

/**
 * This interface defines the methods for CRUD of NB Subnet objects
 *
 */

public interface INeutronSubnetCRUD {
    /**
     * Applications call this interface method to determine if a particular
     * Subnet object exists
     *
     * @param uuid
     *            UUID of the Subnet object
     * @return boolean
     */

    boolean subnetExists(String uuid);

    /**
     * Applications call this interface method to return if a particular
     * Subnet object exists
     *
     * @param uuid
     *            UUID of the Subnet object
     * @return {@link org.opendaylight.neutron.spi.NeutronSubnet}
     *          OpenStack Subnet class
     */

    NeutronSubnet getSubnet(String uuid);

    /**
     * Applications call this interface method to return all Subnet objects
     *
     * @return List of OpenStackSubnets objects
     */

    List<NeutronSubnet> getAllSubnets();

    /**
     * Applications call this interface method to add a Subnet object to the
     * concurrent map
     *
     * @param input
     *            OpenStackSubnet object
     * @return boolean on whether the object was added or not
     */

    boolean addSubnet(NeutronSubnet input);

    /**
     * Applications call this interface method to remove a Subnet object to the
     * concurrent map
     *
     * @param uuid
     *            identifier for the Subnet object
     * @return boolean on whether the object was removed or not
     */

    boolean removeSubnet(String uuid);

    /**
     * Applications call this interface method to edit a Subnet object
     *
     * @param uuid
     *            identifier of the Subnet object
     * @param delta
     *            OpenStackSubnet object containing changes to apply
     * @return boolean on whether the object was updated or not
     */

    boolean updateSubnet(String uuid, NeutronSubnet delta);

    /**
     * Applications call this interface method to determine if a Subnet object
     * is use
     *
     * @param subnetUUID
     *            identifier of the subnet object
     *
     * @return boolean on whether the subnet is in use or not
     *
     * @deprecated - will be removed in Boron
     */

    boolean subnetInUse(String subnetUUID);
}
