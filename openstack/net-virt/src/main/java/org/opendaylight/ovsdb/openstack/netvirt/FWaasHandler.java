/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.ovsdb.openstack.netvirt;

import org.opendaylight.controller.networkconfig.neutron.INeutronFirewallAware;
import org.opendaylight.controller.networkconfig.neutron.INeutronFirewallPolicyAware;
import org.opendaylight.controller.networkconfig.neutron.INeutronFirewallRuleAware;
import org.opendaylight.controller.networkconfig.neutron.NeutronFirewall;
import org.opendaylight.controller.networkconfig.neutron.NeutronFirewallPolicy;
import org.opendaylight.controller.networkconfig.neutron.NeutronFirewallRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;

/**
 * Handle requests for OpenStack Neutron v2.0 Port Firewall API calls.
 */
public class FWaasHandler extends AbstractHandler
                          implements INeutronFirewallAware,
                                     INeutronFirewallRuleAware,
                                     INeutronFirewallPolicyAware {

    static final Logger logger = LoggerFactory.getLogger(FWaasHandler.class);
    /**
     * Invoked when a Firewall Rules creation is requested
     * to indicate if the specified Rule can be created.
     *
     * @param neutronFirewall  An instance of proposed new Neutron Firewall object.
     * @return HttpURLConnection A HTTP status code to the creation request.
     */
    @Override
    public int canCreateNeutronFirewall(NeutronFirewall neutronFirewall) {
        return HttpURLConnection.HTTP_CREATED;
    }

    @Override
    public void neutronFirewallCreated(NeutronFirewall neutronFirewall) {
        logger.debug("Neutron Firewall created by Neutron: {}", neutronFirewall);
        int result = HttpURLConnection.HTTP_BAD_REQUEST;

        result = canCreateNeutronFirewall(neutronFirewall);
        if (result != HttpURLConnection.HTTP_CREATED) {
            logger.error("Neutron Firewall creation failed: {} ", result);
            return;
        }
    }

    @Override
    public int canUpdateNeutronFirewall(NeutronFirewall delta, NeutronFirewall original) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    public void neutronFirewallUpdated(NeutronFirewall neutronFirewall) {
        logger.debug("NeutronFirewall updated from Neutron: {}", neutronFirewall);
        return;
    }

    @Override
    public int canDeleteNeutronFirewall(NeutronFirewall neutronFirewall) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    public void neutronFirewallDeleted(NeutronFirewall neutronFirewall) {
        //TODO: Trigger flowmod removals
        int result = canDeleteNeutronFirewall(neutronFirewall);
        if  (result != HttpURLConnection.HTTP_OK) {
            logger.error(" delete Neutron Firewall validation failed for result - {} ", result);
            return;
        }
    }

    /**
     * Invoked when a Firewall Rule creation is requested
     * to indicate if the specified Rule can be created.
     *
     * @param neutronFirewallRule  An instance of proposed new Neutron Firewall Rule object.
     * @return HttpURLConnection A HTTP status code to the creation request.
     */
    @Override
    public int canCreateNeutronFirewallRule(NeutronFirewallRule neutronFirewallRule) {
        return HttpURLConnection.HTTP_CREATED;
    }

    @Override
    public void neutronFirewallRuleCreated(NeutronFirewallRule neutronFirewallRule) {
        logger.debug("NeutronFirewallRule created by Neutron: {}", neutronFirewallRule);

        int result = HttpURLConnection.HTTP_BAD_REQUEST;

        result = canCreateNeutronFirewallRule(neutronFirewallRule);
        if (result != HttpURLConnection.HTTP_CREATED) {
            logger.error("Neutron Firewall Rule creation failed {} ", result);
            return;
        }
    }

    @Override
    public int canUpdateNeutronFirewallRule(NeutronFirewallRule delta, NeutronFirewallRule original) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    public void neutronFirewallRuleUpdated(NeutronFirewallRule neutronFirewallRule) {
        logger.debug("Neutron Firewall Rule updated from Neutron: {}", neutronFirewallRule);
        return;
    }

    @Override
    public int canDeleteNeutronFirewallRule(NeutronFirewallRule neutronFirewallRule) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    public void neutronFirewallRuleDeleted(NeutronFirewallRule neutronFirewallRule) {
        int result = canDeleteNeutronFirewallRule(neutronFirewallRule);
        if  (result != HttpURLConnection.HTTP_OK) {
            logger.error(" delete Neutron Firewall Rule validation failed for result - {} ", result);
            return;
        }
    }

    /**
     * Invoked when a Firewall Policy creation is requested
     * to indicate if the specified Rule can be created.
     *
     * @param neutronFirewallPolicy  An instance of proposed new Neutron Firewall Policy object.
     * @return HttpURLConnection A HTTP status code to the creation request.
     */
    @Override
    public int canCreateNeutronFirewallPolicy(NeutronFirewallPolicy neutronFirewallPolicy) {
        return HttpURLConnection.HTTP_CREATED;
    }

    @Override
    public void neutronFirewallPolicyCreated(NeutronFirewallPolicy neutronFirewallPolicy) {
        logger.debug("Neutron Firewall Policy created by Neutron: {}", neutronFirewallPolicy);

        int result = HttpURLConnection.HTTP_BAD_REQUEST;

        result = canCreateNeutronFirewallPolicy(neutronFirewallPolicy);
        if (result != HttpURLConnection.HTTP_CREATED) {
            logger.debug("Neutron Firewall Policy creation failed: {} ", result);
            return;
        }
    }

    @Override
    public int canUpdateNeutronFirewallPolicy(NeutronFirewallPolicy delta, NeutronFirewallPolicy original) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    public void neutronFirewallPolicyUpdated(NeutronFirewallPolicy neutronFirewallPolicy) {
        logger.debug("Neutron Firewall Policy updated from Neutron: {}", neutronFirewallPolicy);
        return;
    }

    @Override
    public int canDeleteNeutronFirewallPolicy(NeutronFirewallPolicy neutronFirewallPolicy) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    public void neutronFirewallPolicyDeleted(NeutronFirewallPolicy neutronFirewallPolicy) {
        int result = canDeleteNeutronFirewallPolicy(neutronFirewallPolicy);
        if  (result != HttpURLConnection.HTTP_OK) {
            logger.error(" delete Neutron Firewall Policy validation failed for result - {} ", result);
            return;
        }
    }
}