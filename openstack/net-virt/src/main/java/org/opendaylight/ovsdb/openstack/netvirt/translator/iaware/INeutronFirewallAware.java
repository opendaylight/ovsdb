/*
 * Copyright (C) 2014 Red Hat, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.translator.iaware;

import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronFirewall;

/**
 * This interface defines the methods a service that wishes to be aware of Firewall Rules needs to implement
 *
 */

public interface INeutronFirewallAware {

    /**
     * Services provide this interface method to indicate if the specified firewall can be created
     *
     * @param firewall
     *            instance of proposed new Firewall object
     * @return integer
     *            the return value is understood to be a HTTP status code.  A return value outside of 200 through 299
     *            results in the create operation being interrupted and the returned status value reflected in the
     *            HTTP response.
     */
    int canCreateNeutronFirewall(NeutronFirewall firewall);

    /**
     * Services provide this interface method for taking action after a firewall has been created
     *
     * @param firewall
     *            instance of new Firewall object
     */
    void neutronFirewallCreated(NeutronFirewall firewall);

    /**
     * Services provide this interface method to indicate if the specified firewall can be changed using the specified
     * delta
     *
     * @param delta
     *            updates to the firewall object using patch semantics
     * @param original
     *            instance of the Firewall object to be updated
     * @return integer
     *            the return value is understood to be a HTTP status code.  A return value outside of 200 through 299
     *            results in the update operation being interrupted and the returned status value reflected in the
     *            HTTP response.
     */
    int canUpdateNeutronFirewall(NeutronFirewall delta, NeutronFirewall original);

    /**
     * Services provide this interface method for taking action after a firewall has been updated
     *
     * @param firewall
     *            instance of modified Firewall object
     */
    void neutronFirewallUpdated(NeutronFirewall firewall);

    /**
     * Services provide this interface method to indicate if the specified firewall can be deleted
     *
     * @param firewall
     *            instance of the Firewall object to be deleted
     * @return integer
     *            the return value is understood to be a HTTP status code.  A return value outside of 200 through 299
     *            results in the delete operation being interrupted and the returned status value reflected in the
     *            HTTP response.
     */
    int canDeleteNeutronFirewall(NeutronFirewall firewall);

    /**
     * Services provide this interface method for taking action after a firewall has been deleted
     *
     * @param firewall
     *            instance of deleted Firewall object
     */
    void neutronFirewallDeleted(NeutronFirewall firewall);
}
