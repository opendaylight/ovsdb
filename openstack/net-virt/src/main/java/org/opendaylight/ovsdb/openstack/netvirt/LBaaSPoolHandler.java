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
import org.opendaylight.controller.networkconfig.neutron.INeutronLoadBalancerPoolAware;
import org.opendaylight.controller.networkconfig.neutron.INeutronLoadBalancerPoolCRUD;
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
import com.google.common.collect.Lists;

import java.net.HttpURLConnection;
import java.util.List;

/**
 * Handle requests for OpenStack Neutron v2.0 LBaaS API calls for
 * /v2.0/pools . It is possible that each pool spans multiple subnets.
 * In that case, the user should be creating a separate VIP for each subnet.
 */

public class LBaaSPoolHandler extends AbstractHandler
        implements INeutronLoadBalancerPoolAware {

    private static final Logger logger = LoggerFactory.getLogger(LBaaSPoolHandler.class);

    // The implementation for each of these services is resolved by the OSGi Service Manager
    private volatile INeutronLoadBalancerPoolCRUD neutronLBPoolCache;
    private volatile INeutronLoadBalancerCRUD neutronLBCache;
    private volatile INeutronPortCRUD neutronPortsCache;
    private volatile LoadBalancerProvider loadBalancerProvider;
    private volatile ISwitchManager switchManager;

    @Override
    public int canCreateNeutronLoadBalancerPool(NeutronLoadBalancerPool neutronLBPool) {
        String poolProtocol = neutronLBPool.getLoadBalancerPoolProtocol();
        if (poolProtocol == null)
            return HttpURLConnection.HTTP_BAD_REQUEST;
        else if (!(poolProtocol.equalsIgnoreCase(LoadBalancerConfiguration.PROTOCOL_HTTP) ||
                poolProtocol.equalsIgnoreCase(LoadBalancerConfiguration.PROTOCOL_HTTPS)))
            return HttpURLConnection.HTTP_NOT_ACCEPTABLE;
        else
            return HttpURLConnection.HTTP_OK;
    }

    @Override
    public void neutronLoadBalancerPoolCreated(NeutronLoadBalancerPool neutronLBPool) {
        logger.debug("Neutron LB Pool Creation : {}", neutronLBPool.toString());
        enqueueEvent(new NorthboundEvent(neutronLBPool, Action.ADD));
    }

    /**
     * Assuming that the pool information is fully populated before this call is made,
     * we go with creating the LoadBalancerConfiguration object for this call with
     * all information that is necessary to insert flow_mods
     */
    private void doNeutronLoadBalancerPoolCreate(NeutronLoadBalancerPool neutronLBPool) {
        Preconditions.checkNotNull(loadBalancerProvider);
        List<LoadBalancerConfiguration> lbConfigList = extractLBConfiguration(neutronLBPool);
        if (lbConfigList == null) {
            logger.debug("Neutron LB configuration invalid for pool {} ", neutronLBPool.getLoadBalancerPoolID());
        } else if (lbConfigList.size() == 0) {
            logger.debug("No Neutron LB VIP not created yet for pool {} ", neutronLBPool.getLoadBalancerPoolID());
        } else if (this.switchManager.getNodes().size() == 0) {
            logger.debug("Noop with LB pool {} creation because no nodes available.", neutronLBPool.getLoadBalancerPoolID());
        } else {
            for (LoadBalancerConfiguration lbConfig: lbConfigList) {
                if (!lbConfig.isValid()) {
                    logger.debug("Neutron LB pool configuration invalid for {} ", lbConfig.getName());
                    continue;
                } else {
                    for (Node node: this.switchManager.getNodes())
                        loadBalancerProvider.programLoadBalancerRules(node, lbConfig, Action.ADD);
                }
            }
        }
    }

    @Override
    public int canUpdateNeutronLoadBalancerPool(NeutronLoadBalancerPool delta, NeutronLoadBalancerPool original) {
        return HttpURLConnection.HTTP_NOT_IMPLEMENTED;
    }

    @Override
    public void neutronLoadBalancerPoolUpdated(NeutronLoadBalancerPool neutronLBPool) {
        logger.debug("Neutron LB Pool Update : {}", neutronLBPool.toString());
        enqueueEvent(new NorthboundEvent(neutronLBPool, Action.UPDATE));
    }

    @Override
    public int canDeleteNeutronLoadBalancerPool(NeutronLoadBalancerPool neutronLBPool) {
        String poolProtocol = neutronLBPool.getLoadBalancerPoolProtocol();
        if (poolProtocol == null)
            return HttpURLConnection.HTTP_BAD_REQUEST;
        else if (!(poolProtocol.equalsIgnoreCase(LoadBalancerConfiguration.PROTOCOL_HTTP) ||
                poolProtocol.equalsIgnoreCase(LoadBalancerConfiguration.PROTOCOL_HTTPS)))
            return HttpURLConnection.HTTP_NOT_ACCEPTABLE;
        else
            return HttpURLConnection.HTTP_OK;
    }

    @Override
    public void neutronLoadBalancerPoolDeleted(NeutronLoadBalancerPool neutronLBPool) {
        logger.debug("Neutron LB Pool Deletion : {}", neutronLBPool.toString());
        enqueueEvent(new NorthboundEvent(neutronLBPool, Action.DELETE));
    }

    private void doNeutronLoadBalancerPoolDelete(NeutronLoadBalancerPool neutronLBPool) {
        Preconditions.checkNotNull(loadBalancerProvider);

        List<LoadBalancerConfiguration> lbConfigList = extractLBConfiguration(neutronLBPool);
        if (lbConfigList == null) {
            logger.debug("Neutron LB configuration invalid for pool {} ", neutronLBPool.getLoadBalancerPoolID());
        } else if (lbConfigList.size() == 0) {
            logger.debug("No Neutron LB VIP not created yet for pool {} ", neutronLBPool.getLoadBalancerPoolID());
        } else if (this.switchManager.getNodes().size() == 0) {
            logger.debug("Noop with LB pool {} deletion because no nodes available.", neutronLBPool.getLoadBalancerPoolID());
        } else {
            for (LoadBalancerConfiguration lbConfig: lbConfigList) {
                if (!lbConfig.isValid()) {
                    logger.debug("Neutron LB pool configuration invalid for {} ", lbConfig.getName());
                    continue;
                } else {
                    for (Node node: this.switchManager.getNodes())
                        loadBalancerProvider.programLoadBalancerRules(node, lbConfig, Action.DELETE);
                }
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
        logger.debug("Processing Loadbalancer Pool event " + abstractEvent);
        if (!(abstractEvent instanceof NorthboundEvent)) {
            logger.error("Unable to process abstract event " + abstractEvent);
            return;
        }
        NorthboundEvent ev = (NorthboundEvent) abstractEvent;
        switch (ev.getAction()) {
            case ADD:
                doNeutronLoadBalancerPoolCreate(ev.getLoadBalancerPool());
                break;
            case DELETE:
                doNeutronLoadBalancerPoolDelete(ev.getLoadBalancerPool());
                break;
            case UPDATE:
                /**
                 * Typical upgrade involves changing algorithm. Right now
                 * we do not support this flexibility. TODO
                 */
                logger.warn("Load balancer pool update is not supported");
                break;
            default:
                logger.warn("Unable to process event action " + ev.getAction());
                break;
        }
    }

    /**
     * Useful utility for extracting the loadbalancer instance. With
     * each LB pool, we allow multiple VIP and LB to be instantiated.
     */
    public List<LoadBalancerConfiguration> extractLBConfiguration(NeutronLoadBalancerPool neutronLBPool) {
        String poolProtocol = neutronLBPool.getLoadBalancerPoolProtocol();
        if (poolProtocol == null)
            return null;
        if (!(poolProtocol.equalsIgnoreCase(LoadBalancerConfiguration.PROTOCOL_HTTP) ||
                poolProtocol.equalsIgnoreCase(LoadBalancerConfiguration.PROTOCOL_HTTPS)))
            return null;

        List<NeutronLoadBalancerPoolMember> poolMembers = neutronLBPool.getLoadBalancerPoolMembers();
        if (poolMembers.size() == 0) {
            logger.debug("Neutron LB pool is empty: {}", neutronLBPool);
            return null;
        }

        List<LoadBalancerConfiguration> lbConfigList = Lists.newLinkedList();

        /* Iterate over all the Loadbalancers created so far and identify VIP
         */
        String loadBalancerSubnetID, loadBalancerVip=null, loadBalancerName=null;
        for (NeutronLoadBalancer neutronLB: neutronLBCache.getAllNeutronLoadBalancers()) {
            loadBalancerSubnetID = neutronLB.getLoadBalancerVipSubnetID();
            loadBalancerName = neutronLB.getLoadBalancerName();
            loadBalancerVip = neutronLB.getLoadBalancerVipAddress();
            LoadBalancerConfiguration lbConfig = new LoadBalancerConfiguration(loadBalancerName, loadBalancerVip);
            lbConfig.setVmac(NeutronCacheUtils.getMacAddress(neutronPortsCache, loadBalancerVip));

            /* Iterate over all the members in this pool and find those in same
             * subnet as the VIP. Those will be included in the lbConfigList
             */
            String memberSubnetID, memberIP, memberID, memberMAC;
            Integer memberPort;
            Boolean memberAdminStateIsUp;
            for (NeutronLoadBalancerPoolMember neutronLBPoolMember: neutronLBPool.getLoadBalancerPoolMembers()) {
                memberAdminStateIsUp = neutronLBPoolMember.getPoolMemberAdminStateIsUp();
                memberSubnetID = neutronLBPoolMember.getPoolMemberSubnetID();
                if (memberSubnetID == null || memberAdminStateIsUp == null)
                    continue;
                else if (memberSubnetID.equals(loadBalancerSubnetID) && memberAdminStateIsUp.booleanValue()) {
                    memberID = neutronLBPoolMember.getPoolMemberID();
                    memberIP = neutronLBPoolMember.getPoolMemberAddress();
                    memberPort = neutronLBPoolMember.getPoolMemberProtoPort();
                    if (memberSubnetID == null || memberID == null || memberIP == null || memberPort == null) {
                        logger.debug("Neutron LB pool member details incomplete: {}", neutronLBPoolMember);
                        continue;
                    }
                    memberMAC = NeutronCacheUtils.getMacAddress(neutronPortsCache, memberIP);
                    if (memberMAC == null)
                        continue;
                    lbConfig.addMember(memberID, memberIP, memberMAC, poolProtocol, memberPort);
                }
            }

            if (lbConfig.getMembers().size() > 0)
                lbConfigList.add(lbConfig);
        }

        return lbConfigList;
    }
}
