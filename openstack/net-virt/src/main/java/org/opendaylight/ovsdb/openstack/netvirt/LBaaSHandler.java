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
    private volatile INeutronPortCRUD neutronPortsCache;
    private volatile LoadBalancerProvider loadBalancerProvider;
    private volatile ISwitchManager switchManager;

    @Override
    public int canCreateNeutronLoadBalancer(NeutronLoadBalancer neutronLB) {
        //Always allowed and not wait for pool and members to be created
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    public void neutronLoadBalancerCreated(NeutronLoadBalancer neutronLB) {
        logger.debug("Neutron LB Creation : {}", neutronLB.toString());
        enqueueEvent(new NorthboundEvent(neutronLB, Action.ADD));
    }

    /**
     * Assuming that the pool information is fully populated before this call is made,
     * we go with creating the LoadBalancerConfiguration object for this call with
     * all information that is necessary to insert flow_mods
     */
    private void doNeutronLoadBalancerCreate(NeutronLoadBalancer neutronLB) {
        Preconditions.checkNotNull(loadBalancerProvider);
        LoadBalancerConfiguration lbConfig = extractLBConfiguration(neutronLB);

        if (!lbConfig.isValid()) {
            logger.debug("Neutron LB pool configuration invalid for {} ", lbConfig.getName());
        } else if (this.switchManager.getNodes().size() == 0) {
            logger.debug("Noop with LB {} creation because no nodes available.", lbConfig.getName());
        } else {
            for (Node node: this.switchManager.getNodes())
                loadBalancerProvider.programLoadBalancerRules(node, lbConfig, Action.ADD);
        }
    }

    @Override
    public int canUpdateNeutronLoadBalancer(NeutronLoadBalancer delta, NeutronLoadBalancer original) {
        //Update allowed anytime, even when the LB has no active pool yet
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    public void neutronLoadBalancerUpdated(NeutronLoadBalancer neutronLB) {
        logger.debug("Neutron LB Update : {}", neutronLB.toString());
        enqueueEvent(new NorthboundEvent(neutronLB, Action.UPDATE));
    }

    @Override
    public int canDeleteNeutronLoadBalancer(NeutronLoadBalancer neutronLB) {
        //Always allowed and not wait for pool to stop using it
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
            logger.debug("Neutron LB pool configuration invalid for {} ", lbConfig.getName());
        } else if (this.switchManager.getNodes().size() == 0) {
            logger.debug("Noop with LB {} deletion because no nodes available.", lbConfig.getName());
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
        logger.debug("Processing Loadbalancer event " + abstractEvent);
        if (!(abstractEvent instanceof NorthboundEvent)) {
            logger.error("Unable to process abstract event " + abstractEvent);
            return;
        }
        NorthboundEvent ev = (NorthboundEvent) abstractEvent;
        switch (ev.getAction()) {
            case ADD:
                doNeutronLoadBalancerCreate(ev.getLoadBalancer());
                break;
            case DELETE:
                doNeutronLoadBalancerDelete(ev.getLoadBalancer());
                break;
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
        lbConfig.setVmac(NeutronCacheUtils.getMacAddress(neutronPortsCache, loadBalancerVip));

        String memberID, memberIP, memberMAC, memberProtocol, memberSubnetID;
        Integer memberPort;
        Boolean memberAdminStateIsUp;

        for (NeutronLoadBalancerPool neutronLBPool: neutronLBPoolCache.getAllNeutronLoadBalancerPools()) {
            List<NeutronLoadBalancerPoolMember> members = neutronLBPool.getLoadBalancerPoolMembers();
            memberProtocol = neutronLBPool.getLoadBalancerPoolProtocol();
            if (memberProtocol == null)
                continue;
            /*
             * Only HTTP and HTTPS are supported as of this version
             * TODO: Support all TCP load-balancers
             */
            if (!(memberProtocol.equalsIgnoreCase(LoadBalancerConfiguration.PROTOCOL_HTTP) ||
                  memberProtocol.equalsIgnoreCase(LoadBalancerConfiguration.PROTOCOL_HTTPS)))
                continue;
            for (NeutronLoadBalancerPoolMember neutronLBPoolMember: members) {
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

        for (NeutronLoadBalancer neutronLB: neutronLBCache.getAllNeutronLoadBalancers()) {
            LoadBalancerConfiguration lbConfig = extractLBConfiguration(neutronLB);
            if (!lbConfig.isValid())
                logger.debug("Neutron LB configuration invalid for {} ", lbConfig.getName());
            else {
               if (type.equals(UpdateType.ADDED))
                    loadBalancerProvider.programLoadBalancerRules(node, lbConfig, Action.ADD);

               /* When node disappears, we do nothing for now. Making a call to
                * loadBalancerProvider.programLoadBalancerRules(node, lbConfig, Action.DELETE)
                * can lead to TransactionCommitFailedException. Similarly when node is changed,
                * because of remove followed by add, we do nothing.
                */
               else //(type.equals(UpdateType.REMOVED) || type.equals(UpdateType.CHANGED))
                    continue;
            }
        }
    }

    @Override
    public void notifyNodeConnector(NodeConnector arg0, UpdateType arg1,
            Map<String, Property> arg2) {
        //NOOP
    }
}
