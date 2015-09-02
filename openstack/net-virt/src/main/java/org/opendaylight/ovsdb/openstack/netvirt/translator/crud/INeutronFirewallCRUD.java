/*
 * Copyright (C) 2014 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.translator.crud;

import java.util.List;

import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronFirewall;

/**
 * This interface defines the methods for CRUD of NB OpenStack Firewall objects
 */

public interface INeutronFirewallCRUD {
    /**
     * Applications call this interface method to determine if a particular
     *Firewall object exists
     *
     * @param uuid
     *            UUID of the Firewall object
     * @return boolean
     */

    boolean neutronFirewallExists(String uuid);

    /**
     * Applications call this interface method to return if a particular
     * Firewall object exists
     *
     * @param uuid
     *            UUID of the Firewall object
     * @return {@link org.opendaylight.neutron.spi.NeutronFirewall}
     *          OpenStackFirewall class
     */

    NeutronFirewall getNeutronFirewall(String uuid);

    /**
     * Applications call this interface method to return all Firewall objects
     *
     * @return List of OpenStackNetworks objects
     */

    List<NeutronFirewall> getAllNeutronFirewalls();

    /**
     * Applications call this interface method to add a Firewall object to the
     * concurrent map
     *
     * @param input
     *            OpenStackNetwork object
     * @return boolean on whether the object was added or not
     */

    boolean addNeutronFirewall(NeutronFirewall input);

    /**
     * Applications call this interface method to remove a Neutron Firewall object to the
     * concurrent map
     *
     * @param uuid
     *            identifier for the Firewall object
     * @return boolean on whether the object was removed or not
     */

    boolean removeNeutronFirewall(String uuid);

    /**
     * Applications call this interface method to edit a Firewall object
     *
     * @param uuid
     *            identifier of the Firewall object
     * @param delta
     *            OpenStackFirewall object containing changes to apply
     * @return boolean on whether the object was updated or not
     */

    boolean updateNeutronFirewall(String uuid, NeutronFirewall delta);

    /**
     * Applications call this interface method to see if a MAC address is in use
     *
     * @param uuid
     *            identifier of the Firewall object
     * @return boolean on whether the macAddress is already associated with a
     * port or not
     */

    boolean neutronFirewallInUse(String uuid);

}
