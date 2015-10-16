/*
 * Copyright (C) 2014 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.translator.crud;

import java.util.List;

import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronSecurityGroup;

/**
 * This interface defines the methods for CRUD of NB OpenStack Security Group objects
 */

public interface INeutronSecurityGroupCRUD {
    /**
     * Applications call this interface method to determine if a particular
     * Security Group object exists
     *
     * @param uuid UUID of the Security Group object
     * @return boolean
     */

    boolean neutronSecurityGroupExists(String uuid);

    /**
     * Applications call this interface method to return if a particular
     * Security Group object exists
     *
     * @param uuid UUID of the Security Group object
     * @return {@link org.opendaylight.neutron.spi.NeutronSecurityGroup}
     * OpenStack Security Group class
     */

    NeutronSecurityGroup getNeutronSecurityGroup(String uuid);

    /**
     * Applications call this interface method to return all Security Group objects
     *
     * @return List of OpenStackSecurity Groups objects
     */

    List<NeutronSecurityGroup> getAllNeutronSecurityGroups();

    /**
     * Applications call this interface method to add a Security Group object to the
     * concurrent map
     *
     * @param input OpenStackSecurity Group object
     * @return boolean on whether the object was added or not
     */

    boolean addNeutronSecurityGroup(NeutronSecurityGroup input);

    /**
     * Applications call this interface method to remove a Neutron Security Group object to the
     * concurrent map
     *
     * @param uuid identifier for the security group object
     * @return boolean on whether the object was removed or not
     */

    boolean removeNeutronSecurityGroup(String uuid);

    /**
     * Applications call this interface method to edit a Security Group object
     *
     * @param uuid  identifier of the security group object
     * @param delta OpenStackSecurity Group object containing changes to apply
     * @return boolean on whether the object was updated or not
     */

    boolean updateNeutronSecurityGroup(String uuid, NeutronSecurityGroup delta);

    /**
     * Applications call this interface method to see if a MAC address is in use
     *
     * @param uuid identifier of the security group object
     * @return boolean on whether the Security Groups is already in use
     */

    boolean neutronSecurityGroupInUse(String uuid);

}
