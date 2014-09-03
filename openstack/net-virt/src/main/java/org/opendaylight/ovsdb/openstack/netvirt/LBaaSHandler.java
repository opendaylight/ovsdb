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
import org.opendaylight.controller.networkconfig.neutron.INeutronLoadBalancerPoolCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronLoadBalancerPoolMemberCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronPortCRUD;
import org.opendaylight.controller.networkconfig.neutron.NeutronLoadBalancer;
import org.opendaylight.controller.networkconfig.neutron.NeutronLoadBalancerPool;
import org.opendaylight.controller.networkconfig.neutron.NeutronLoadBalancerPoolMember;
import org.opendaylight.controller.networkconfig.neutron.NeutronPort;
import org.opendaylight.controller.networkconfig.neutron.Neutron_IPs;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.Action;
import org.opendaylight.ovsdb.openstack.netvirt.api.LoadBalancerConfiguration;
import org.opendaylight.ovsdb.openstack.netvirt.api.LoadBalancerProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import java.net.HttpURLConnection;
import java.util.Iterator;
import java.util.List;

/**
 * Handle requests for OpenStack Neutron v2.0 LBaaS API calls for /v2.0/loadbalancers.
 */

//TODO: Implement INeutronLoadBalancerHealthMonitorAware, INeutronLoadBalancerListenerAware, INeutronLoadBalancerPoolMemberAware,

public class LBaaSHandler extends AbstractHandler
        implements INeutronLoadBalancerAware {

    private static final Logger logger = LoggerFactory.getLogger(LBaaSHandler.class);

    // The implementation for each of these services is resolved by the OSGi Service Manager
    private volatile INeutronLoadBalancerPoolCRUD neutronLBPoolCache;
    private volatile INeutronLoadBalancerPoolMemberCRUD neutronLBPoolMemberCache;
    private volatile INeutronPortCRUD neutronPortsCache;
    private volatile LoadBalancerProvider loadBalancerProvider;
    private volatile ISwitchManager switchManager;

    @Override
    public int canCreateNeutronLoadBalancer(NeutronLoadBalancer neutronLoadBalancer) {
        LoadBalancerConfiguration lbConfig = extractLBConfiguration(neutronLoadBalancer);
        if (!lbConfig.isValid())
            return HttpURLConnection.HTTP_NOT_ACCEPTABLE;
        else
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
        enqueueEvent(new NorthboundEvent(neutronLoadBalancer, Action.ADD));
    }

    private void doNeutronLoadBalancerCreate(NeutronLoadBalancer neutronLoadBalancer) {
        int result = canCreateNeutronLoadBalancer(neutronLoadBalancer);
        if (result != HttpURLConnection.HTTP_OK) {
            logger.debug("Neutron Load Balancer creation failed {} ", result);
            return;
        }
        Preconditions.checkNotNull(loadBalancerProvider);
        LoadBalancerConfiguration lbConfig = extractLBConfiguration(neutronLoadBalancer);

        if (!lbConfig.isValid()) {
            logger.trace("Neutron LB pool configuration invalid for {} ", lbConfig.getName());
            return;
        } else {
            for (Node node: this.switchManager.getNodes())
                loadBalancerProvider.programLoadBalancerRules(node, lbConfig, Action.ADD);
        }
    }

    @Override
    public int canUpdateNeutronLoadBalancer(NeutronLoadBalancer delta, NeutronLoadBalancer original) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    public void neutronLoadBalancerUpdated(NeutronLoadBalancer neutronLoadBalancer) {
        enqueueEvent(new NorthboundEvent(neutronLoadBalancer, Action.UPDATE));
        return;
    }

    @Override
    public int canDeleteNeutronLoadBalancer(NeutronLoadBalancer neutronLoadBalancer) {
        LoadBalancerConfiguration lbConfig = extractLBConfiguration(neutronLoadBalancer);
        if (!lbConfig.isValid())
            return HttpURLConnection.HTTP_NOT_ACCEPTABLE;
        else
            return HttpURLConnection.HTTP_OK;
    }

    @Override
    public void neutronLoadBalancerDeleted(NeutronLoadBalancer neutronLoadBalancer) {
        logger.debug("Neutron LB Deletion : {}", neutronLoadBalancer.toString());
        enqueueEvent(new NorthboundEvent(neutronLoadBalancer, Action.DELETE));
    }

    private void doNeutronLoadBalancerDelete(NeutronLoadBalancer neutronLoadBalancer) {
        int result = canDeleteNeutronLoadBalancer(neutronLoadBalancer);
        if  (result != HttpURLConnection.HTTP_OK) {
            logger.error(" delete Neutron NeutronLoadBalancer Pool validation failed for result - {} ", result);
            return;
        }
        Preconditions.checkNotNull(loadBalancerProvider);
        LoadBalancerConfiguration lbConfig = extractLBConfiguration(neutronLoadBalancer);

        if (!lbConfig.isValid()) {
            logger.trace("Neutron LB pool configuration invalid for {} ", lbConfig.getName());
            return;
        } else {
            for (Node node: this.switchManager.getNodes())
                loadBalancerProvider.programLoadBalancerRules(node, lbConfig, Action.DELETE);
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
            case ADD:
                doNeutronLoadBalancerCreate(ev.getLoadBalancer());
            case DELETE:
                doNeutronLoadBalancerDelete(ev.getLoadBalancer());
            case UPDATE:
                /**
                 * Currently member update requires delete and re-adding
                 * Also, weights and weight updates are not supported
                 */
                doNeutronLoadBalancerDelete(ev.getLoadBalancer());
                doNeutronLoadBalancerCreate(ev.getLoadBalancer());
                break;
            default:
                logger.warn("Unable to process event action " + ev.getAction());
                break;
        }
    }

    /**
     * Useful utility for extracting the loadbalancer instance
     * configuration from the neutron LB cache
     */
    public LoadBalancerConfiguration extractLBConfiguration(NeutronLoadBalancer neutronLoadBalancer) {
        String loadBalancerName = neutronLoadBalancer.getLoadBalancerName();
        String loadBalancerVip = neutronLoadBalancer.getLoadBalancerVipAddress();
        String loadBalancerSubnetID = neutronLoadBalancer.getLoadBalancerVipSubnetID();
        LoadBalancerConfiguration lbConfig = new LoadBalancerConfiguration(loadBalancerName, loadBalancerVip);

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
            if (!(memberProtocol.equalsIgnoreCase(LoadBalancerConfiguration.PROTOCOL_HTTP) ||
                  memberProtocol.equalsIgnoreCase(LoadBalancerConfiguration.PROTOCOL_HTTPS)))
                continue;
            for (NeutronLoadBalancerPoolMember neutronLBPoolMember: members) {
                if (neutronLBPoolMember.getPoolMemberSubnetID().equals(loadBalancerSubnetID)) {
                    memberID = neutronLBPoolMember.getPoolMemberID();
                    memberIP = neutronLBPoolMember.getPoolMemberAddress();
                    memberPort = neutronLBPoolMember.getPoolMemberProtoPort();
                    memberMAC = this.getMacAddress(memberIP);
                    if (memberMAC == null)
                        continue;
                    lbConfig.addMember(memberID, memberIP, memberMAC, memberProtocol, memberPort);
                }
            }
        }
        return lbConfig;
    }

    /**
     * Look up in the NeutronPortsCRUD cache and return the MAC address for a corresponding IP address
     * @param ipAddr IP address of a member or VM
     * @return MAC address registered with that IP address
     */
    public String getMacAddress(String ipAddr) {
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
                     if (ip.getIpAddress().equals(ipAddr))
                         return port.getMacAddress();
                 }
             }
         }
        return null;
    }
}
