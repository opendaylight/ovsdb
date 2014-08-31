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
import org.opendaylight.controller.networkconfig.neutron.INeutronLoadBalancerPoolCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronLoadBalancerPoolMemberAware;
import org.opendaylight.controller.networkconfig.neutron.INeutronLoadBalancerPoolMemberCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronPortCRUD;
import org.opendaylight.controller.networkconfig.neutron.NeutronLoadBalancer;
import org.opendaylight.controller.networkconfig.neutron.NeutronLoadBalancerPool;
import org.opendaylight.controller.networkconfig.neutron.NeutronLoadBalancerPoolMember;
import org.opendaylight.controller.networkconfig.neutron.NeutronPort;
import org.opendaylight.controller.networkconfig.neutron.Neutron_IPs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

import java.net.HttpURLConnection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Handle requests for OpenStack Neutron v2.0 LBaaS API calls.
 */

//TODO: Implement INeutronLoadBalancerHealthMonitorAware, INeutronLoadBalancerListenerAware

public class LBaaSHandler extends AbstractHandler
        implements INeutronLoadBalancerAware, INeutronLoadBalancerPoolAware,
            INeutronLoadBalancerPoolMemberAware {

    static final Logger logger = LoggerFactory.getLogger(LBaaSHandler.class);

    // The implementation for each of these services is resolved by the OSGi Service Manager
    private volatile INeutronLoadBalancerPoolCRUD neutronLBPoolCache;
    private volatile INeutronLoadBalancerPoolMemberCRUD neutronLBPoolMemberCache;
    private volatile INeutronPortCRUD neutronPortsCache;
    private Map<String, LoadBalancerConfiguration> loadbalancers = Maps.newHashMap();

    @Override
    public int canCreateNeutronLoadBalancer(NeutronLoadBalancer neutronLoadBalancer) {
        if (loadbalancers.containsKey(neutronLoadBalancer.getLoadBalancerID()))
            return HttpURLConnection.HTTP_CONFLICT;
        return HttpURLConnection.HTTP_OK;
    }

    /**
     * Assuming that the pool information is fully populated before this call is made,
     * we go with creating the LoadBalancerConfiguration object for this call with
     * all information that is necessary to insert flow_mods
     */
    @Override
    public void neutronLoadBalancerCreated(NeutronLoadBalancer neutronLoadBalancer) {
        logger.debug("Neutron LB Creation : {}", neutronLoadBalancer.toString());

        int result = canCreateNeutronLoadBalancer(neutronLoadBalancer);
        if (result != HttpURLConnection.HTTP_OK) {
            logger.debug("Neutron Load Balancer creation failed {} ", result);
            return;
        }

        String loadBalancerID = neutronLoadBalancer.getLoadBalancerID();
        String loadBalancerName = neutronLoadBalancer.getLoadBalancerName();
        String loadBalancerVip = neutronLoadBalancer.getLoadBalancerVipAddress();
        String loadBalancerSubnetID = neutronLoadBalancer.getLoadBalancerVipSubnetID();
        LoadBalancerConfiguration newLB = new LoadBalancerConfiguration(loadBalancerName, loadBalancerVip);

        String memberID, memberIP, memberMAC, memberProtocol;
        Integer memberPort;

        for (NeutronLoadBalancerPool neutronLBPool: neutronLBPoolCache.getAllNeutronLoadBalancerPools()) {
            List<? extends NeutronLoadBalancerPoolMember> members =
                (List<? extends NeutronLoadBalancerPoolMember>)neutronLBPool.getLoadBalancerPoolMembers();
            memberProtocol = neutronLBPool.getLoadBalancerPoolProtocol();
            /*
             * Only HTTP and HTTPS are supported as of this version
             * TODO: Support all TCP load-balancers
             */
            if (memberProtocol != "HTTP" || memberProtocol != "HTTP")
                continue;
            for (NeutronLoadBalancerPoolMember neutronLBPoolMember: members) {
                if (neutronLBPoolMember.getPoolMemberSubnetID() == loadBalancerSubnetID) {
                    memberID = neutronLBPoolMember.getPoolMemberID();
                    memberIP = neutronLBPoolMember.getPoolMemberAddress();
                    memberPort = neutronLBPoolMember.getPoolMemberProtoPort();
                    memberMAC = this.getMacAddress(memberIP);
                    if (memberMAC == null)
                        continue;
                    newLB.addMember(memberID, memberIP, memberMAC, memberProtocol, memberPort);
                }
            }
        }
        if (newLB.isValid()) {
            logger.trace("Neutron LB pool configuration invalid for {} ", loadBalancerName);
            return;
        } else {
            loadbalancers.put(loadBalancerID, newLB);
            //TODO: Trigger flowmod addition
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
        if (!loadbalancers.containsKey(neutronLoadBalancer.getLoadBalancerID()))
            return HttpURLConnection.HTTP_NOT_ACCEPTABLE;
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    public void neutronLoadBalancerDeleted(NeutronLoadBalancer neutronLoadBalancer) {
        logger.debug("Neutron LB Deletion : {}", neutronLoadBalancer.toString());

        int result = canDeleteNeutronLoadBalancer(neutronLoadBalancer);
        if  (result != HttpURLConnection.HTTP_OK) {
            logger.error(" delete Neutron NeutronLoadBalancer Pool validation failed for result - {} ", result);
            return;
        }
        loadbalancers.remove(neutronLoadBalancer.getLoadBalancerID());
        //TODO: Trigger flowmod removals
    }

    /**
     * Invoked when a NeutronLoadBalancer Pools creation is requested
     * to indicate if the specified Rule can be created.
     *
     * @param neutronLBPool  An instance of proposed new Neutron LoadBalancer Pool object.
     * @return A HTTP status code to the creation request.
     */

    @Override
    public int canCreateNeutronLoadBalancerPool(NeutronLoadBalancerPool neutronLBPool) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    public void neutronLoadBalancerPoolCreated(NeutronLoadBalancerPool neutronLBPool) {
        logger.debug("Neutron LB Pool Creation : {}", neutronLBPool.toString());
        int result = canCreateNeutronLoadBalancerPool(neutronLBPool);
        if (result != HttpURLConnection.HTTP_OK) {
            logger.debug("Neutron Load Balancer creation failed {} ", result);
            return;
        }
    }

    @Override
    public int canUpdateNeutronLoadBalancerPool(NeutronLoadBalancerPool delta, NeutronLoadBalancerPool original) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    public void neutronLoadBalancerPoolUpdated(NeutronLoadBalancerPool neutronLBPool) {
        logger.debug("Neutron LB Pool updated : {}", neutronLBPool.toString());
        return;
    }

    @Override
    public int canDeleteNeutronLoadBalancerPool(NeutronLoadBalancerPool neutronLBPool) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    public void neutronLoadBalancerPoolDeleted(NeutronLoadBalancerPool neutronLBPool) {
        logger.debug("Neutron LB Pool Deletion : {}", neutronLBPool.toString());

        int result = canDeleteNeutronLoadBalancerPool(neutronLBPool);
        if  (result != HttpURLConnection.HTTP_OK) {
            logger.error(" delete Neutron NeutronLoadBalancer Pool validation failed for result - {} ", result);
            return;
        }
    }
    /**
     * Invoked when a NeutronLoadBalancer Pool Members creation is requested
     * to indicate if the specified Rule can be created.
     *
     * @param neutronLBPoolMember  An instance of proposed new Neutron LoadBalancer Pool Member object.
     * @return A HTTP status code to the creation request.
     */

    @Override
    public int canCreateNeutronLoadBalancerPoolMember(NeutronLoadBalancerPoolMember neutronLBPoolMember) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    public void neutronLoadBalancerPoolMemberCreated(NeutronLoadBalancerPoolMember neutronLBPoolMember) {
        logger.debug("Neutron LB Pool Member Creation : {}", neutronLBPoolMember.toString());

        int result = canCreateNeutronLoadBalancerPoolMember(neutronLBPoolMember);
        if (result != HttpURLConnection.HTTP_OK) {
            logger.debug("Neutron Load Balancer creation failed {} ", result);
            return;
        }
    }

    @Override
    public int canUpdateNeutronLoadBalancerPoolMember(NeutronLoadBalancerPoolMember delta, NeutronLoadBalancerPoolMember original) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    public void neutronLoadBalancerPoolMemberUpdated(NeutronLoadBalancerPoolMember neutronLBPoolMember) {
        logger.debug("Neutron LB Pool Member updated : {}", neutronLBPoolMember.toString());
        return;
    }

    @Override
    public int canDeleteNeutronLoadBalancerPoolMember(NeutronLoadBalancerPoolMember neutronLBPoolMember) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    public void neutronLoadBalancerPoolMemberDeleted(NeutronLoadBalancerPoolMember neutronLBPoolMember) {
        logger.debug("Neutron LB Pool Member Deletion : {}", neutronLBPoolMember.toString());

        int result = canDeleteNeutronLoadBalancerPoolMember(neutronLBPoolMember);
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

    /**
     * Look up in the NeutronPortsCRUD cache and return the MAC address for a corresponding IP address
     * @param ipAddr IP address of a member or VM
     * @return MAC address registered with that IP address
     */
    private String getMacAddress(String ipAddr) {
            List<Neutron_IPs> fixedIPs;
            Iterator<Neutron_IPs> fixedIPIterator;
            Neutron_IPs ip;

            List<NeutronPort> allPorts = neutronPortsCache.getAllPorts();
         Iterator<NeutronPort> i = allPorts.iterator();
         while (i.hasNext()) {
             NeutronPort port = i.next();
             fixedIPs = port.getFixedIPs();
             if (fixedIPs != null && fixedIPs.size() > 0) {
                 fixedIPIterator = fixedIPs.iterator();
                 while (fixedIPIterator.hasNext()) {
                     ip = fixedIPIterator.next();
                     if (ip.getIpAddress() == ipAddr)
                         return port.getMacAddress();
                 }
             }
         }
        return null;
    }
}
