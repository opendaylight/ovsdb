/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Srini Seetharaman
 */

package org.opendaylight.ovsdb.openstack.netvirt;

import org.opendaylight.controller.networkconfig.neutron.INeutronLoadBalancerAware;
//import org.opendaylight.controller.networkconfig.neutron.INeutronLoadBalancerHealthMonitorAware;
//import org.opendaylight.controller.networkconfig.neutron.INeutronLoadBalancerListenerAware;
import org.opendaylight.controller.networkconfig.neutron.INeutronLoadBalancerPoolAware;
import org.opendaylight.controller.networkconfig.neutron.INeutronLoadBalancerPoolMemberAware;
import org.opendaylight.controller.networkconfig.neutron.NeutronLoadBalancer;
import org.opendaylight.controller.networkconfig.neutron.NeutronLoadBalancerPool;
import org.opendaylight.controller.networkconfig.neutron.NeutronLoadBalancerPoolMember;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;

/**
 * Handle requests for OpenStack Neutron v2.0 LBaaS API calls.
 */
public class LBaaSHandler extends AbstractHandler
        implements INeutronLoadBalancerAware, INeutronLoadBalancerPoolAware,
            INeutronLoadBalancerPoolMemberAware {

    static final Logger logger = LoggerFactory.getLogger(LBaaSHandler.class);

    @Override
    public int canCreateNeutronLoadBalancer(NeutronLoadBalancer neutronLoadBalancer) {
        return HttpURLConnection.HTTP_CREATED;
    }

    @Override
    public void neutronLoadBalancerCreated(NeutronLoadBalancer neutronLoadBalancer) {
        int result = HttpURLConnection.HTTP_BAD_REQUEST;

        result = canCreateNeutronLoadBalancer(neutronLoadBalancer);
        if (result != HttpURLConnection.HTTP_CREATED) {
            logger.debug("Neutron Load Balancer creation failed {} ", result);
            return;
        }
    }

    @Override
    public int canUpdateNeutronLoadBalancer(NeutronLoadBalancer delta, NeutronLoadBalancer original) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    public void neutronLoadBalancerUpdated(NeutronLoadBalancer neutronLoadBalancer) {
        return;
    }

    @Override
    public int canDeleteNeutronLoadBalancer(NeutronLoadBalancer neutronLoadBalancer) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    public void neutronLoadBalancerDeleted(NeutronLoadBalancer neutronLoadBalancer) {
        //TODO: Trigger flowmod removals
        int result = canDeleteNeutronLoadBalancer(neutronLoadBalancer);
        if  (result != HttpURLConnection.HTTP_OK) {
            logger.error(" delete Neutron NeutronLoadBalancer Pool validation failed for result - {} ", result);
            return;
        }
    }

    /**
     * Invoked when a NeutronLoadBalancer Pools creation is requested
     * to indicate if the specified Rule can be created.
     *
     * @param neutronLoadBalancerPool  An instance of proposed new Neutron LoadBalancer Pool object.
     * @return A HTTP status code to the creation request.
     */

    @Override
    public int canCreateNeutronLoadBalancerPool(NeutronLoadBalancerPool neutronLoadBalancerPool) {
        return HttpURLConnection.HTTP_CREATED;
    }

    @Override
    public void neutronLoadBalancerPoolCreated(NeutronLoadBalancerPool neutronLoadBalancerPool) {
        int result = HttpURLConnection.HTTP_BAD_REQUEST;

        result = canCreateNeutronLoadBalancerPool(neutronLoadBalancerPool);
        if (result != HttpURLConnection.HTTP_CREATED) {
            logger.debug("Neutron Load Balancer creation failed {} ", result);
            return;
        }
    }

    @Override
    public int canUpdateNeutronLoadBalancerPool(NeutronLoadBalancerPool delta, NeutronLoadBalancerPool original) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    public void neutronLoadBalancerPoolUpdated(NeutronLoadBalancerPool neutronLoadBalancerPool) {
        return;
    }

    @Override
    public int canDeleteNeutronLoadBalancerPool(NeutronLoadBalancerPool neutronLoadBalancerPool) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    public void neutronLoadBalancerPoolDeleted(NeutronLoadBalancerPool neutronLoadBalancerPool) {
        int result = canDeleteNeutronLoadBalancerPool(neutronLoadBalancerPool);
        if  (result != HttpURLConnection.HTTP_OK) {
            logger.error(" delete Neutron NeutronLoadBalancer Pool validation failed for result - {} ", result);
            return;
        }
    }
    /**
     * Invoked when a NeutronLoadBalancer Pool Members creation is requested
     * to indicate if the specified Rule can be created.
     *
     * @param neutronLoadBalancerPoolMember  An instance of proposed new Neutron LoadBalancer Pool Member object.
     * @return A HTTP status code to the creation request.
     */

    @Override
    public int canCreateNeutronLoadBalancerPoolMember(NeutronLoadBalancerPoolMember neutronLoadBalancerPoolMember) {
        return HttpURLConnection.HTTP_CREATED;
    }

    @Override
    public void neutronLoadBalancerPoolMemberCreated(NeutronLoadBalancerPoolMember neutronLoadBalancerPoolMember) {
        int result = HttpURLConnection.HTTP_BAD_REQUEST;

        result = canCreateNeutronLoadBalancerPoolMember(neutronLoadBalancerPoolMember);
        if (result != HttpURLConnection.HTTP_CREATED) {
            logger.debug("Neutron Load Balancer creation failed {} ", result);
            return;
        }
    }

    @Override
    public int canUpdateNeutronLoadBalancerPoolMember(NeutronLoadBalancerPoolMember delta, NeutronLoadBalancerPoolMember original) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    public void neutronLoadBalancerPoolMemberUpdated(NeutronLoadBalancerPoolMember neutronLoadBalancerPoolMember) {
        return;
    }

    @Override
    public int canDeleteNeutronLoadBalancerPoolMember(NeutronLoadBalancerPoolMember neutronLoadBalancerPoolMember) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    public void neutronLoadBalancerPoolMemberDeleted(NeutronLoadBalancerPoolMember neutronLoadBalancerPoolMember) {
        int result = canDeleteNeutronLoadBalancerPoolMember(neutronLoadBalancerPoolMember);
        if  (result != HttpURLConnection.HTTP_OK) {
            logger.error(" delete Neutron NeutronLoadBalancer Pool Member validation failed for result - {} ", result);
            return;
        }
    }
}
