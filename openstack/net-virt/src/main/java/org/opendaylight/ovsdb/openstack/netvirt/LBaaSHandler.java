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
import org.opendaylight.controller.networkconfig.neutron.INeutronLoadBalancerCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronLoadBalancerPoolCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronLoadBalancerPoolMemberCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronPortCRUD;
import org.opendaylight.controller.networkconfig.neutron.NeutronLoadBalancer;
import org.opendaylight.controller.networkconfig.neutron.NeutronLoadBalancerPool;
import org.opendaylight.controller.networkconfig.neutron.NeutronLoadBalancerPoolMember;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.controller.switchmanager.IInventoryListener;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.Action;
import org.opendaylight.ovsdb.openstack.netvirt.api.LoadBalancerConfiguration;
import org.opendaylight.ovsdb.openstack.netvirt.api.LoadBalancerProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

/**
 * Handle requests for OpenStack Neutron v2.0 LBaaS API calls for /v2.0/loadbalancers.
 */

//TODO: Implement INeutronLoadBalancerHealthMonitorAware, INeutronLoadBalancerListenerAware, INeutronLoadBalancerPoolMemberAware,

public class LBaaSHandler extends AbstractHandler
        implements INeutronLoadBalancerAware, IInventoryListener {

    private static final Logger logger = LoggerFactory.getLogger(LBaaSHandler.class);

    // The implementation for each of these services is resolved by the OSGi Service Manager
    private volatile INeutronLoadBalancerCRUD neutronLBCache;
    private volatile INeutronLoadBalancerPoolCRUD neutronLBPoolCache;
    private volatile INeutronLoadBalancerPoolMemberCRUD neutronLBPoolMemberCache;
    private volatile INeutronPortCRUD neutronPortsCache;
    private volatile LoadBalancerProvider loadBalancerProvider;
    private volatile ISwitchManager switchManager;

    @Override
    public int canCreateNeutronLoadBalancer(NeutronLoadBalancer neutronLB) {
        LoadBalancerConfiguration lbConfig = extractLBConfiguration(neutronLB);
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
    public void neutronLoadBalancerCreated(NeutronLoadBalancer neutronLB) {
        logger.debug("Neutron LB Creation : {}", neutronLB.toString());
        enqueueEvent(new NorthboundEvent(neutronLB, Action.ADD));
    }

    private void doNeutronLoadBalancerCreate(NeutronLoadBalancer neutronLB) {
        Preconditions.checkNotNull(loadBalancerProvider);
        LoadBalancerConfiguration lbConfig = extractLBConfiguration(neutronLB);

        if (!lbConfig.isValid()) {
            logger.trace("Neutron LB pool configuration invalid for {} ", lbConfig.getName());
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
    public void neutronLoadBalancerUpdated(NeutronLoadBalancer neutronLB) {
        enqueueEvent(new NorthboundEvent(neutronLB, Action.UPDATE));
        return;
    }

    @Override
    public int canDeleteNeutronLoadBalancer(NeutronLoadBalancer neutronLB) {
        LoadBalancerConfiguration lbConfig = extractLBConfiguration(neutronLB);
        if (!lbConfig.isValid())
            return HttpURLConnection.HTTP_NOT_ACCEPTABLE;
        else
            return HttpURLConnection.HTTP_OK;
    }

    @Override
    public void neutronLoadBalancerDeleted(NeutronLoadBalancer neutronLB) {
        logger.debug("Neutron LB Deletion : {}", neutronLB.toString());
        enqueueEvent(new NorthboundEvent(neutronLB, Action.DELETE));
    }

    private void doNeutronLoadBalancerDelete(NeutronLoadBalancer neutronLB) {
        Preconditions.checkNotNull(loadBalancerProvider);
        LoadBalancerConfiguration lbConfig = extractLBConfiguration(neutronLB);

        if (!lbConfig.isValid()) {
            logger.trace("Neutron LB pool configuration invalid for {} ", lbConfig.getName());
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
    public LoadBalancerConfiguration extractLBConfiguration(NeutronLoadBalancer neutronLB) {
        String loadBalancerName = neutronLB.getLoadBalancerName();
        String loadBalancerVip = neutronLB.getLoadBalancerVipAddress();
        String loadBalancerSubnetID = neutronLB.getLoadBalancerVipSubnetID();
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
                if (neutronLBPoolMember.getPoolMemberSubnetID().equals(loadBalancerSubnetID) &&
                    neutronLBPoolMember.getPoolMemberAdminStateIsUp()) {
                    memberID = neutronLBPoolMember.getPoolMemberID();
                    memberIP = neutronLBPoolMember.getPoolMemberAddress();
                    memberPort = neutronLBPoolMember.getPoolMemberProtoPort();
                    memberMAC = NeutronCacheUtils.getMacAddress(neutronPortsCache, memberIP);
                    if (memberMAC == null)
                        continue;
                    lbConfig.addMember(memberID, memberIP, memberMAC, memberProtocol, memberPort);
                }
            }
        }
        return lbConfig;
    }

    /**
     * On the addition of a new node, we iterate through all existing loadbalancer
     * instances and program the node for all of them. It is sufficient to do that only
     * when a node is added, and only for the LB instances (and not individual members).
     */
    @Override
    public void notifyNode(Node node, UpdateType type, Map<String, Property> propMap) {
        logger.debug("notifyNode: Node {} update {} from Controller's inventory Service", node, type);
        Preconditions.checkNotNull(loadBalancerProvider);

        if (type.equals(UpdateType.ADDED)) {
            for (NeutronLoadBalancer neutronLB: neutronLBCache.getAllNeutronLoadBalancers()) {
                LoadBalancerConfiguration lbConfig = extractLBConfiguration(neutronLB);
                if (!lbConfig.isValid())
                    logger.trace("Neutron LB configuration invalid for {} ", lbConfig.getName());
                else
                    loadBalancerProvider.programLoadBalancerRules(node, lbConfig, Action.ADD);
            }
        }
    }

    @Override
    public void notifyNodeConnector(NodeConnector arg0, UpdateType arg1,
            Map<String, Property> arg2) {
        //NOOP
    }
}
