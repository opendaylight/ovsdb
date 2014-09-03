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

import org.opendaylight.controller.networkconfig.neutron.INeutronLoadBalancerCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronLoadBalancerPoolCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronLoadBalancerPoolMemberAware;
import org.opendaylight.controller.networkconfig.neutron.INeutronPortCRUD;
import org.opendaylight.controller.networkconfig.neutron.NeutronLoadBalancer;
import org.opendaylight.controller.networkconfig.neutron.NeutronLoadBalancerPool;
import org.opendaylight.controller.networkconfig.neutron.NeutronLoadBalancerPoolMember;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.Action;
import org.opendaylight.ovsdb.openstack.netvirt.api.LoadBalancerConfiguration;
import org.opendaylight.ovsdb.openstack.netvirt.api.LoadBalancerProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import java.net.HttpURLConnection;
import java.util.List;

/**
 * Handle requests for OpenStack Neutron v2.0 LBaaS API calls for
 * /v2.0/pools/{pool_id}/members
 */

public class LBaaSPoolMemberHandler extends AbstractHandler
        implements INeutronLoadBalancerPoolMemberAware {

    private static final Logger logger = LoggerFactory.getLogger(LBaaSPoolMemberHandler.class);

    // The implementation for each of these services is resolved by the OSGi Service Manager
    private volatile INeutronLoadBalancerPoolCRUD neutronLBPoolCache;
    private volatile INeutronLoadBalancerCRUD neutronLBCache;
    private volatile INeutronPortCRUD neutronPortsCache;
    private volatile LoadBalancerProvider loadBalancerProvider;
    private volatile ISwitchManager switchManager;

    @Override
    public int canCreateNeutronLoadBalancerPoolMember(NeutronLoadBalancerPoolMember neutronLBPoolMember) {
        LoadBalancerConfiguration lbConfig = extractLBConfiguration(neutronLBPoolMember);
        if (lbConfig == null)
            return HttpURLConnection.HTTP_BAD_REQUEST;
        else if (!lbConfig.isValid())
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
    public void neutronLoadBalancerPoolMemberCreated(NeutronLoadBalancerPoolMember neutronLBPoolMember) {
        logger.debug("Neutron LB Pool Member Creation : {}", neutronLBPoolMember.toString());
        enqueueEvent(new NorthboundEvent(neutronLBPoolMember, Action.ADD));
    }

    private void doNeutronLoadBalancerPoolMemberCreate(NeutronLoadBalancerPoolMember neutronLBPoolMember) {
        Preconditions.checkNotNull(loadBalancerProvider);
        LoadBalancerConfiguration lbConfig = extractLBConfiguration(neutronLBPoolMember);
        if (lbConfig == null) {
            logger.trace("Neutron LB configuration invalid for member {} ", neutronLBPoolMember.getPoolMemberAddress());
        }
        else if (!lbConfig.isValid()) {
            logger.trace("Neutron LB pool configuration invalid for {} ", lbConfig.getName());
        } else {
            for (Node node: this.switchManager.getNodes())
                loadBalancerProvider.programLoadBalancerPoolMemberRules(node, lbConfig,
                        lbConfig.getMembers().get(neutronLBPoolMember.getPoolMemberID()), Action.ADD);
        }
    }

    @Override
    public int canUpdateNeutronLoadBalancerPoolMember(NeutronLoadBalancerPoolMember delta, NeutronLoadBalancerPoolMember original) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    public void neutronLoadBalancerPoolMemberUpdated(NeutronLoadBalancerPoolMember neutronLBPoolMember) {
        enqueueEvent(new NorthboundEvent(neutronLBPoolMember, Action.UPDATE));
        return;
    }

    @Override
    public int canDeleteNeutronLoadBalancerPoolMember(NeutronLoadBalancerPoolMember neutronLBPoolMember) {
        LoadBalancerConfiguration lbConfig = extractLBConfiguration(neutronLBPoolMember);
        if (lbConfig == null)
            return HttpURLConnection.HTTP_BAD_REQUEST;
        else if (!lbConfig.isValid())
            return HttpURLConnection.HTTP_NOT_ACCEPTABLE;
        else
            return HttpURLConnection.HTTP_OK;
    }

    @Override
    public void neutronLoadBalancerPoolMemberDeleted(NeutronLoadBalancerPoolMember neutronLBPoolMember) {
        logger.debug("Neutron LB Pool Member Deletion : {}", neutronLBPoolMember.toString());

        /* As of now, deleting a member involves recomputing member indices.
         * This is best done through a complete update of the load balancer instance.
         */
        for (NeutronLoadBalancer neutronLB: neutronLBCache.getAllNeutronLoadBalancers()) {
            String loadBalancerSubnetID = neutronLB.getLoadBalancerVipSubnetID();
            if (neutronLBPoolMember.getPoolMemberSubnetID().equals(loadBalancerSubnetID)) {
                enqueueEvent(new NorthboundEvent(neutronLB, Action.UPDATE));
                break;
            }
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
                doNeutronLoadBalancerPoolMemberCreate(ev.getLoadBalancerPoolMember());
            case DELETE:
                logger.warn("Load balancer pool member delete event should not have been triggered");
            case UPDATE:
                /**
                 * Typical upgrade involves changing weights. Since weights are not
                 * supported yet, updates are not supported either.
                 */
                logger.warn("Load balancer pool member update is not supported");
                break;
            default:
                logger.warn("Unable to process event action " + ev.getAction());
                break;
        }
    }

    /**
     * Useful utility for extracting the loadbalancer instance
     * configuration from the neutron LB cache based on member info
     */
    public LoadBalancerConfiguration extractLBConfiguration(NeutronLoadBalancerPoolMember neutronLBPoolMember) {
        String memberIP = neutronLBPoolMember.getPoolMemberAddress();
        String memberMAC = NeutronCacheUtils.getMacAddress(neutronPortsCache, memberIP);
        if (memberMAC == null)
            return null;

        String memberID = neutronLBPoolMember.getPoolMemberID();
        Integer memberPort = neutronLBPoolMember.getPoolMemberProtoPort();
        String memberProtocol = null;
        boolean found = false;

        for (NeutronLoadBalancerPool neutronLBPool: neutronLBPoolCache.getAllNeutronLoadBalancerPools()) {
            List<? extends NeutronLoadBalancerPoolMember> members =
                    (List<? extends NeutronLoadBalancerPoolMember>)neutronLBPool.getLoadBalancerPoolMembers();
            for (NeutronLoadBalancerPoolMember member: members) {
                //TODO: Allow member to be present in more than 1 pool
                if (member.getPoolMemberID().equals(neutronLBPoolMember.getPoolMemberID())) {
                    found = true;
                    memberProtocol = neutronLBPool.getLoadBalancerPoolProtocol();
                    if (!(memberProtocol.equalsIgnoreCase(LoadBalancerConfiguration.PROTOCOL_HTTP) ||
                            memberProtocol.equalsIgnoreCase(LoadBalancerConfiguration.PROTOCOL_HTTPS)))
                        memberProtocol = null;
                    break;
                }
            }
            if (found)
                break;
        }
        if (memberProtocol == null)
            return null;

        String loadBalancerSubnetID, loadBalancerVip, loadBalancerName;
        for (NeutronLoadBalancer neutronLB: neutronLBCache.getAllNeutronLoadBalancers()) {
            loadBalancerSubnetID = neutronLB.getLoadBalancerVipSubnetID();
            if (neutronLBPoolMember.getPoolMemberSubnetID().equals(loadBalancerSubnetID)) {
                loadBalancerName = neutronLB.getLoadBalancerName();
                loadBalancerVip = neutronLB.getLoadBalancerVipAddress();
                LoadBalancerConfiguration lbConfig = new LoadBalancerConfiguration(loadBalancerName, loadBalancerVip);
                lbConfig.addMember(memberID, memberIP, memberMAC, memberProtocol, memberPort);
                return lbConfig;
            }
        }
        return null;
    }
}
