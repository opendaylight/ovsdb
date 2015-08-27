/*
 * Copyright (C) 2014 Red Hat, Inc.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.translator;

/**
 * This interface defines the methods a service that wishes to be aware of Firewall Policys needs to implement
 *
 */

public interface INeutronFirewallPolicyAware {

    /**
     * Services provide this interface method to indicate if the specified firewallPolicy can be created
     *
     * @param firewallPolicy
     *            instance of proposed new Firewall Policy object
     * @return integer
     *            the return value is understood to be a HTTP status code.  A return value outside of 200 through 299
     *            results in the create operation being interrupted and the returned status value reflected in the
     *            HTTP response.
     */
    int canCreateNeutronFirewallPolicy(NeutronFirewallPolicy firewallPolicy);

    /**
     * Services provide this interface method for taking action after a firewallPolicy has been created
     *
     * @param firewallPolicy
     *            instance of new Firewall Policy object
     */
    void neutronFirewallPolicyCreated(NeutronFirewallPolicy firewallPolicy);

    /**
     * Services provide this interface method to indicate if the specified firewallPolicy can be changed using the specified
     * delta
     *
     * @param delta
     *            updates to the firewallPolicy object using patch semantics
     * @param original
     *            instance of the Firewall Policy object to be updated
     * @return integer
     *            the return value is understood to be a HTTP status code.  A return value outside of 200 through 299
     *            results in the update operation being interrupted and the returned status value reflected in the
     *            HTTP response.
     */
    int canUpdateNeutronFirewallPolicy(NeutronFirewallPolicy delta, NeutronFirewallPolicy original);

    /**
     * Services provide this interface method for taking action after a firewallPolicy has been updated
     *
     * @param firewallPolicy
     *            instance of modified Firewall Policy object
     */
    void neutronFirewallPolicyUpdated(NeutronFirewallPolicy firewallPolicy);

    /**
     * Services provide this interface method to indicate if the specified firewallPolicy can be deleted
     *
     * @param firewallPolicy
     *            instance of the Firewall Policy object to be deleted
     * @return integer
     *            the return value is understood to be a HTTP status code.  A return value outside of 200 through 299
     *            results in the delete operation being interrupted and the returned status value reflected in the
     *            HTTP response.
     */
    int canDeleteNeutronFirewallPolicy(NeutronFirewallPolicy firewallPolicy);

    /**
     * Services provide this interface method for taking action after a firewallPolicy has been deleted
     *
     * @param firewallPolicy
     *            instance of deleted Firewall Policy object
     */
    void neutronFirewallPolicyDeleted(NeutronFirewallPolicy firewallPolicy);
}
