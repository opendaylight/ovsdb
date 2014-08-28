/*
 * Copyright (C) 2014 SDN Hub, LLC.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Srini Seetharaman
 */

package org.opendaylight.ovsdb.openstack.netvirt;

import org.opendaylight.controller.networkconfig.neutron.INeutronLoadBalancerAware;
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

//TODO: Implement INeutronLoadBalancerHealthMonitorAware, INeutronLoadBalancerListenerAware

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
        logger.debug("Neutron LB Creation : {}", neutronLoadBalancer.toString());
        //TODO: Trigger flowmod addition
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
        logger.debug("Neutron LB Deletion : {}", neutronLoadBalancer.toString());
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
        logger.debug("Neutron LB Pool Creation : {}", neutronLoadBalancerPool.toString());
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
        logger.debug("Neutron LB Pool updated : {}", neutronLoadBalancerPool.toString());
        return;
    }

    @Override
    public int canDeleteNeutronLoadBalancerPool(NeutronLoadBalancerPool neutronLoadBalancerPool) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    public void neutronLoadBalancerPoolDeleted(NeutronLoadBalancerPool neutronLoadBalancerPool) {
        logger.debug("Neutron LB Pool Deletion : {}", neutronLoadBalancerPool.toString());

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
        logger.debug("Neutron LB Pool Member Creation : {}", neutronLoadBalancerPoolMember.toString());

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
        logger.debug("Neutron LB Pool Member updated : {}", neutronLoadBalancerPoolMember.toString());
        return;
    }

    @Override
    public int canDeleteNeutronLoadBalancerPoolMember(NeutronLoadBalancerPoolMember neutronLoadBalancerPoolMember) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    public void neutronLoadBalancerPoolMemberDeleted(NeutronLoadBalancerPoolMember neutronLoadBalancerPoolMember) {
        logger.debug("Neutron LB Pool Member Deletion : {}", neutronLoadBalancerPoolMember.toString());

        int result = canDeleteNeutronLoadBalancerPoolMember(neutronLoadBalancerPoolMember);
        if  (result != HttpURLConnection.HTTP_OK) {
            logger.error(" delete Neutron NeutronLoadBalancer Pool Member validation failed for result - {} ", result);
            return;
        }
    }

    /**
     * Process the event.
     *
     * @param abstractEvent the {@link org.opendaylight.ovsdb.openstack.netvirt.AbstractEvent} event to be handled.
     * @see org.opendaylight.ovsdb.openstack.netvirt.api.EventDispatcher
     */
    @Override
    public void processEvent(AbstractEvent abstractEvent) {
        if (!(abstractEvent instanceof NorthboundEvent)) {
            logger.error("Unable to process abstract event " + abstractEvent);
            return;
        }
        NorthboundEvent ev = (NorthboundEvent) abstractEvent;
        switch (ev.getAction()) {
            // TODO: add handling of events here, once callbacks do something
            //       other than logging.
            default:
                logger.warn("Unable to process event action " + ev.getAction());
                break;
        }
    }
}

