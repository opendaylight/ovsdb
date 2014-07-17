/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Brent Salisbury, Madhu Venugopal
 */

package org.opendaylight.ovsdb.neutron;

import org.opendaylight.controller.networkconfig.neutron.INeutronSecurityGroupAware;
import org.opendaylight.controller.networkconfig.neutron.INeutronSecurityRuleAware;
import org.opendaylight.controller.networkconfig.neutron.NeutronSecurityGroup;
import org.opendaylight.controller.networkconfig.neutron.NeutronSecurityRule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;

/**
 * Handle requests for OpenStack Neutron v2.0 Port Security API calls.
 */
public class PortSecurityHandler extends AbstractHandler
        implements INeutronSecurityGroupAware, INeutronSecurityRuleAware{

    static final Logger logger = LoggerFactory.getLogger(PortSecurityHandler.class);

    @Override
    public int canCreateNeutronSecurityGroup(NeutronSecurityGroup neutronSecurityGroup) {
        return HttpURLConnection.HTTP_CREATED;
    }

    @Override
    public void neutronSecurityGroupCreated(NeutronSecurityGroup neutronSecurityGroup) {
        int result = HttpURLConnection.HTTP_BAD_REQUEST;

        result = canCreateNeutronSecurityGroup(neutronSecurityGroup);
        if (result != HttpURLConnection.HTTP_CREATED) {
            logger.debug("Neutron Security Group creation failed {} ", result);
            return;
        }
    }

    @Override
    public int canUpdateNeutronSecurityGroup(NeutronSecurityGroup delta, NeutronSecurityGroup original) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    public void neutronSecurityGroupUpdated(NeutronSecurityGroup neutronSecurityGroup) {
        return;
    }

    @Override
    public int canDeleteNeutronSecurityGroup(NeutronSecurityGroup neutronSecurityGroup) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    public void neutronSecurityGroupDeleted(NeutronSecurityGroup neutronSecurityGroup) {
        //TODO: Trigger flowmod removals
        int result = canDeleteNeutronSecurityGroup(neutronSecurityGroup);
        if  (result != HttpURLConnection.HTTP_OK) {
            logger.error(" delete Neutron Security Rule validation failed for result - {} ", result);
            return;
        }
    }

    /**
     * Invoked when a Security Rules creation is requested
     * to indicate if the specified Rule can be created.
     *
     * @param neutronSecurityRule  An instance of proposed new Neutron Security Rule object.
     * @return A HTTP status code to the creation request.
     */

    @Override
    public int canCreateNeutronSecurityRule(NeutronSecurityRule neutronSecurityRule) {
        return HttpURLConnection.HTTP_CREATED;
    }

    @Override
    public void neutronSecurityRuleCreated(NeutronSecurityRule neutronSecurityRule) {
        int result = HttpURLConnection.HTTP_BAD_REQUEST;

        result = canCreateNeutronSecurityRule(neutronSecurityRule);
        if (result != HttpURLConnection.HTTP_CREATED) {
            logger.debug("Neutron Security Group creation failed {} ", result);
            return;
        }
    }

    @Override
    public int canUpdateNeutronSecurityRule(NeutronSecurityRule delta, NeutronSecurityRule original) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    public void neutronSecurityRuleUpdated(NeutronSecurityRule neutronSecurityRule) {
        return;
    }

    @Override
    public int canDeleteNeutronSecurityRule(NeutronSecurityRule neutronSecurityRule) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    public void neutronSecurityRuleDeleted(NeutronSecurityRule neutronSecurityRule) {
        int result = canDeleteNeutronSecurityRule(neutronSecurityRule);
        if  (result != HttpURLConnection.HTTP_OK) {
            logger.error(" delete Neutron Security Rule validation failed for result - {} ", result);
            return;
        }
    }
}