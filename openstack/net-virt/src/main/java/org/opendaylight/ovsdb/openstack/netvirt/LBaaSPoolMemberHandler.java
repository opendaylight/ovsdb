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

    @Override
    public void neutronLoadBalancerPoolMemberCreated(NeutronLoadBalancerPoolMember neutronLBPoolMember) {
        logger.debug("Neutron LB Pool Member Creation : {}", neutronLBPoolMember.toString());
        enqueueEvent(new NorthboundEvent(neutronLBPoolMember, Action.ADD));
    }

    /**
     * Assuming that the pool information is fully populated before this call is made,
     * we go with creating the LoadBalancerConfiguration object for this call with
     * all information that is necessary to insert flow_mods
     */
    private void doNeutronLoadBalancerPoolMemberCreate(NeutronLoadBalancerPoolMember neutronLBPoolMember) {
        Preconditions.checkNotNull(loadBalancerProvider);
        LoadBalancerConfiguration lbConfig = extractLBConfiguration(neutronLBPoolMember);
        if (lbConfig == null) {
            logger.trace("Neutron LB configuration invalid for member {} ", neutronLBPoolMember.getPoolMemberAddress());
        } else if (lbConfig.getVip() == null) {
            logger.trace("Neutron LB VIP not created yet for member {} ", neutronLBPoolMember.getPoolMemberID());
        } else if (!lbConfig.isValid()) {
            logger.trace("Neutron LB pool configuration invalid for {} ", lbConfig.getName());
        } else if (this.switchManager.getNodes().size() == 0) {
            logger.trace("Noop with LB pool member {} creation because no nodes available.", neutronLBPoolMember.getPoolMemberID());
        } else {
            for (Node node: this.switchManager.getNodes())
                loadBalancerProvider.programLoadBalancerPoolMemberRules(node, lbConfig,
                        lbConfig.getMembers().get(neutronLBPoolMember.getPoolMemberID()), Action.ADD);
        }
    }

    @Override
    public int canUpdateNeutronLoadBalancerPoolMember(NeutronLoadBalancerPoolMember delta, NeutronLoadBalancerPoolMember original) {
        return HttpURLConnection.HTTP_NOT_IMPLEMENTED;
    }

    @Override
    public void neutronLoadBalancerPoolMemberUpdated(NeutronLoadBalancerPoolMember neutronLBPoolMember) {
        logger.debug("Neutron LB Pool Member Update : {}", neutronLBPoolMember.toString());
        enqueueEvent(new NorthboundEvent(neutronLBPoolMember, Action.UPDATE));
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
        enqueueEvent(new NorthboundEvent(neutronLBPoolMember, Action.DELETE));
    }

    private void doNeutronLoadBalancerPoolMemberDelete(NeutronLoadBalancerPoolMember neutronLBPoolMember) {
        Preconditions.checkNotNull(loadBalancerProvider);

        LoadBalancerConfiguration lbConfig = extractLBConfiguration(neutronLBPoolMember);
        if (lbConfig == null) {
            logger.trace("Neutron LB configuration invalid for member {} ", neutronLBPoolMember.getPoolMemberAddress());
        } else if (lbConfig.getVip() == null) {
            logger.trace("Neutron LB VIP not created yet for member {} ", neutronLBPoolMember.getPoolMemberID());
        } else if (!lbConfig.isValid()) {
            logger.trace("Neutron LB pool configuration invalid for {} ", lbConfig.getName());
        } else if (this.switchManager.getNodes().size() == 0) {
            logger.trace("Noop with LB pool member {} deletion because no nodes available.", neutronLBPoolMember.getPoolMemberID());
        } else {
            /* As of now, deleting a member involves recomputing member indices.
             * This is best done through a complete update of the load balancer instance.
             */
            LoadBalancerConfiguration newLBConfig = new LoadBalancerConfiguration(lbConfig);
            newLBConfig.removeMember(neutronLBPoolMember.getPoolMemberID());

            for (Node node: this.switchManager.getNodes()) {
                loadBalancerProvider.programLoadBalancerRules(node, lbConfig, Action.DELETE);
                loadBalancerProvider.programLoadBalancerRules(node, newLBConfig, Action.ADD);
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
        logger.debug("Processing Loadbalancer member event " + abstractEvent);
        if (!(abstractEvent instanceof NorthboundEvent)) {
            logger.error("Unable to process abstract event " + abstractEvent);
            return;
        }
        NorthboundEvent ev = (NorthboundEvent) abstractEvent;
        switch (ev.getAction()) {
            case ADD:
                doNeutronLoadBalancerPoolMemberCreate(ev.getLoadBalancerPoolMember());
                break;
            case DELETE:
                doNeutronLoadBalancerPoolMemberDelete(ev.getLoadBalancerPoolMember());
                break;
            case UPDATE:
                /**
                 * Typical upgrade involves changing weights. Since weights are not
                 * supported yet, updates are not supported either. TODO
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
        String memberID = neutronLBPoolMember.getPoolMemberID();
        String memberIP = neutronLBPoolMember.getPoolMemberAddress();
        String memberMAC = NeutronCacheUtils.getMacAddress(neutronPortsCache, memberIP);
        if (memberMAC == null) {
            logger.trace("Neutron LB pool member {} MAC address unavailable", memberID);
            return null;
        }
        String memberSubnetID = neutronLBPoolMember.getPoolMemberSubnetID();
        Integer memberPort = neutronLBPoolMember.getPoolMemberProtoPort();
        String memberPoolID = neutronLBPoolMember.getPoolID();
        String memberProtocol = null;

        if (memberSubnetID == null || memberID == null || memberPoolID == null) {
            logger.trace("Neutron LB pool member details incomplete [id={}, pool_id={},subnet_id={}",
                    memberID, memberPoolID, memberSubnetID);
            return null;
        }
        NeutronLoadBalancerPool neutronLBPool = neutronLBPoolCache.getNeutronLoadBalancerPool(memberPoolID);
        memberProtocol = neutronLBPool.getLoadBalancerPoolProtocol();
        if (!(memberProtocol.equalsIgnoreCase(LoadBalancerConfiguration.PROTOCOL_HTTP) ||
                memberProtocol.equalsIgnoreCase(LoadBalancerConfiguration.PROTOCOL_HTTPS)))
            return null;

        String loadBalancerSubnetID, loadBalancerVip=null, loadBalancerName=null;
        for (NeutronLoadBalancer neutronLB: neutronLBCache.getAllNeutronLoadBalancers()) {
            loadBalancerSubnetID = neutronLB.getLoadBalancerVipSubnetID();
            if (memberSubnetID.equals(loadBalancerSubnetID)) {
                loadBalancerName = neutronLB.getLoadBalancerName();
                loadBalancerVip = neutronLB.getLoadBalancerVipAddress();
                break;
            }
        }

        /**
         * It is possible that the VIP has not been created yet.
         * In that case, we create dummy configuration that will not program rules.
         */
        LoadBalancerConfiguration lbConfig = new LoadBalancerConfiguration(loadBalancerName, loadBalancerVip);

        /* Extract all other active members and include in LB config
         */
        String otherMemberID, otherMemberSubnetID, otherMemberIP, otherMemberMAC, otherMemberProtocol;
        Boolean otherMemberAdminStateIsUp;
        Integer otherMemberPort;

        for (NeutronLoadBalancerPoolMember otherMember: neutronLBPool.getLoadBalancerPoolMembers()) {
            otherMemberID = otherMember.getPoolMemberID();
            if (otherMemberID.equals(memberID))
                continue; //skip

            otherMemberIP = otherMember.getPoolMemberAddress();
            otherMemberAdminStateIsUp = otherMember.getPoolMemberAdminStateIsUp();
            otherMemberSubnetID = otherMember.getPoolMemberSubnetID();
            otherMemberPort = otherMember.getPoolMemberProtoPort();
            otherMemberProtocol = memberProtocol;

            if (otherMemberIP == null || otherMemberSubnetID == null || otherMemberAdminStateIsUp == null)
                continue;
            else if (otherMemberAdminStateIsUp.booleanValue()) {
                otherMemberMAC = NeutronCacheUtils.getMacAddress(neutronPortsCache, otherMemberIP);
                if (otherMemberMAC == null)
                    continue;
                lbConfig.addMember(otherMemberID, otherMemberIP, otherMemberMAC, otherMemberProtocol, otherMemberPort);
            }
        }

        lbConfig.addMember(memberID, memberIP, memberMAC, memberProtocol, memberPort);
        return lbConfig;
    }
}
