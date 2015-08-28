/*
 * Copyright (C) 2014 Red Hat, Inc.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.translator.crud;

import java.util.List;

import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronSecurityRule;

/**
 * This interface defines the methods for CRUD of NB OpenStack Security Rule objects
 */

public interface INeutronSecurityRuleCRUD {
    /**
     * Applications call this interface method to determine if a particular
     * Security Rule object exists
     *
     * @param uuid UUID of theSecurity Rule object
     * @return boolean
     */

    boolean neutronSecurityRuleExists(String uuid);

    /**
     * Applications call this interface method to return if a particular
     * Security Rule object exists
     *
     * @param uuid UUID of the security rule object
     * @return {@link org.opendaylight.neutron.spi.NeutronSecurityRule}
     * OpenStackSecurity Rule class
     */

    NeutronSecurityRule getNeutronSecurityRule(String uuid);

    /**
     * Applications call this interface method to return all Security Rule objects
     *
     * @return List of OpenStack SecurityRules objects
     */

    List<NeutronSecurityRule> getAllNeutronSecurityRules();

    /**
     * Applications call this interface method to add a Security Rule object to the
     * concurrent map
     *
     * @param input OpenStack security rule object
     * @return boolean on whether the object was added or not
     */

    boolean addNeutronSecurityRule(NeutronSecurityRule input);

    /**
     * Applications call this interface method to remove a Neutron Security Rule object to the
     * concurrent map
     *
     * @param uuid identifier for the security rule object
     * @return boolean on whether the object was removed or not
     */

    boolean removeNeutronSecurityRule(String uuid);

    /**
     * Applications call this interface method to edit aSecurity Rule object
     *
     * @param uuid  identifier of the security rule object
     * @param delta OpenStackSecurity Rule object containing changes to apply
     * @return boolean on whether the object was updated or not
     */

    boolean updateNeutronSecurityRule(String uuid, NeutronSecurityRule delta);

    /**
     * Applications call this interface method to see if a MAC address is in use
     *
     * @param uuid identifier of the security rule object
     * @return boolean on whether the macAddress is already associated with a
     * port or not
     */

    boolean neutronSecurityRuleInUse(String uuid);

}
