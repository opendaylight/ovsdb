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

import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

import org.opendaylight.neutron.spi.INeutronLoadBalancerCRUD;
import org.opendaylight.neutron.spi.INeutronLoadBalancerPoolAware;
import org.opendaylight.neutron.spi.INeutronNetworkCRUD;
import org.opendaylight.neutron.spi.INeutronPortCRUD;
import org.opendaylight.neutron.spi.INeutronSubnetCRUD;
import org.opendaylight.neutron.spi.NeutronLoadBalancer;
import org.opendaylight.neutron.spi.NeutronLoadBalancerPool;
import org.opendaylight.neutron.spi.NeutronLoadBalancerPoolMember;
import org.opendaylight.ovsdb.openstack.netvirt.api.Action;
import org.opendaylight.ovsdb.openstack.netvirt.api.EventDispatcher;
import org.opendaylight.ovsdb.openstack.netvirt.api.LoadBalancerConfiguration;
import org.opendaylight.ovsdb.openstack.netvirt.api.LoadBalancerProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.NodeCacheManager;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

/**
 * Handle requests for OpenStack Neutron v2.0 LBaaS API calls for
 * /v2.0/pools . It is possible that each pool spans multiple subnets.
 * In that case, the user should be creating a separate VIP for each subnet.
 */

public class LBaaSPoolHandler extends AbstractHandler
        implements INeutronLoadBalancerPoolAware, ConfigInterface {

    private static final Logger logger = LoggerFactory.getLogger(LBaaSPoolHandler.class);

    // The implementation for each of these services is resolved by the OSGi Service Manager
    private volatile INeutronLoadBalancerCRUD neutronLBCache;
    private volatile INeutronPortCRUD neutronPortCache;
    private volatile INeutronNetworkCRUD neutronNetworkCache;
    private volatile INeutronSubnetCRUD neutronSubnetCache;
    private volatile LoadBalancerProvider loadBalancerProvider;
    private volatile NodeCacheManager nodeCacheManager;

    @Override
    public int canCreateNeutronLoadBalancerPool(NeutronLoadBalancerPool neutronLBPool) {
        String poolProtocol = neutronLBPool.getLoadBalancerPoolProtocol();
        if (poolProtocol == null) {
            return HttpURLConnection.HTTP_BAD_REQUEST;
        } else if (!(poolProtocol.equalsIgnoreCase(LoadBalancerConfiguration.PROTOCOL_TCP) ||
                poolProtocol.equalsIgnoreCase(LoadBalancerConfiguration.PROTOCOL_HTTP) ||
                poolProtocol.equalsIgnoreCase(LoadBalancerConfiguration.PROTOCOL_HTTPS))) {
            return HttpURLConnection.HTTP_NOT_ACCEPTABLE;
        } else {
            return HttpURLConnection.HTTP_OK;
        }
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
        final List<Node> nodes = nodeCacheManager.getBridgeNodes();
        if (lbConfigList == null) {
            logger.debug("Neutron LB configuration invalid for pool {} ", neutronLBPool.getLoadBalancerPoolID());
        } else if (lbConfigList.size() == 0) {
            logger.debug("No Neutron LB VIP not created yet for pool {} ", neutronLBPool.getLoadBalancerPoolID());
        } else if (nodes.isEmpty()) {
            logger.debug("Noop with LB pool {} creation because no nodes available.", neutronLBPool.getLoadBalancerPoolID());
        } else {
            for (LoadBalancerConfiguration lbConfig: lbConfigList) {
                if (!lbConfig.isValid()) {
                    logger.debug("Neutron LB pool configuration invalid for {} ", lbConfig.getName());
                    continue;
                } else {
                    for (Node node : nodes) {
                        loadBalancerProvider.programLoadBalancerRules(node, lbConfig, Action.ADD);
                    }
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
        if (poolProtocol == null) {
            return HttpURLConnection.HTTP_BAD_REQUEST;
        } else if (!(poolProtocol.equalsIgnoreCase(LoadBalancerConfiguration.PROTOCOL_TCP) ||
                poolProtocol.equalsIgnoreCase(LoadBalancerConfiguration.PROTOCOL_HTTP) ||
                poolProtocol.equalsIgnoreCase(LoadBalancerConfiguration.PROTOCOL_HTTPS))) {
            return HttpURLConnection.HTTP_NOT_ACCEPTABLE;
        } else {
            return HttpURLConnection.HTTP_OK;
        }
    }

    @Override
    public void neutronLoadBalancerPoolDeleted(NeutronLoadBalancerPool neutronLBPool) {
        logger.debug("Neutron LB Pool Deletion : {}", neutronLBPool.toString());
        enqueueEvent(new NorthboundEvent(neutronLBPool, Action.DELETE));
    }

    private void doNeutronLoadBalancerPoolDelete(NeutronLoadBalancerPool neutronLBPool) {
        Preconditions.checkNotNull(loadBalancerProvider);

        List<LoadBalancerConfiguration> lbConfigList = extractLBConfiguration(neutronLBPool);
        final List<Node> nodes = nodeCacheManager.getBridgeNodes();
        if (lbConfigList == null) {
            logger.debug("Neutron LB configuration invalid for pool {} ", neutronLBPool.getLoadBalancerPoolID());
        } else if (lbConfigList.size() == 0) {
            logger.debug("No Neutron LB VIP not created yet for pool {} ", neutronLBPool.getLoadBalancerPoolID());
        } else if (nodes.isEmpty()) {
            logger.debug("Noop with LB pool {} deletion because no nodes available.", neutronLBPool.getLoadBalancerPoolID());
        } else {
            for (LoadBalancerConfiguration lbConfig: lbConfigList) {
                if (!lbConfig.isValid()) {
                    logger.debug("Neutron LB pool configuration invalid for {} ", lbConfig.getName());
                    continue;
                } else {
                    for (Node node : nodes) {
                        loadBalancerProvider.programLoadBalancerRules(node, lbConfig, Action.DELETE);
                    }
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
        if (poolProtocol == null) {
            return null;
        }
        if (!(poolProtocol.equalsIgnoreCase(LoadBalancerConfiguration.PROTOCOL_HTTP) ||
                poolProtocol.equalsIgnoreCase(LoadBalancerConfiguration.PROTOCOL_HTTPS))) {
            return null;
        }

        List<NeutronLoadBalancerPoolMember> poolMembers = neutronLBPool.getLoadBalancerPoolMembers();
        if (poolMembers.size() == 0) {
            logger.debug("Neutron LB pool is empty: {}", neutronLBPool);
            return null;
        }

        List<LoadBalancerConfiguration> lbConfigList = Lists.newLinkedList();

        /* Iterate over all the Loadbalancers created so far and identify VIP
         */
        for (NeutronLoadBalancer neutronLB: neutronLBCache.getAllNeutronLoadBalancers()) {
            String loadBalancerSubnetID = neutronLB.getLoadBalancerVipSubnetID();
            String loadBalancerName = neutronLB.getLoadBalancerName();
            String loadBalancerVip = neutronLB.getLoadBalancerVipAddress();

            LoadBalancerConfiguration lbConfig = new LoadBalancerConfiguration(loadBalancerName, loadBalancerVip);
            Map.Entry<String,String> providerInfo = NeutronCacheUtils.getProviderInformation(neutronNetworkCache, neutronSubnetCache, loadBalancerSubnetID);
            if (providerInfo != null) {
                lbConfig.setProviderNetworkType(providerInfo.getKey());
                lbConfig.setProviderSegmentationId(providerInfo.getValue());
            }
            lbConfig.setVmac(NeutronCacheUtils.getMacAddress(neutronPortCache, loadBalancerSubnetID, loadBalancerVip));

            /* Iterate over all the members in this pool and find those in same
             * subnet as the VIP. Those will be included in the lbConfigList
             */
            String memberSubnetID, memberIP, memberID, memberMAC;
            Integer memberPort;
            Boolean memberAdminStateIsUp;
            for (NeutronLoadBalancerPoolMember neutronLBPoolMember: neutronLBPool.getLoadBalancerPoolMembers()) {
                memberAdminStateIsUp = neutronLBPoolMember.getPoolMemberAdminStateIsUp();
                memberSubnetID = neutronLBPoolMember.getPoolMemberSubnetID();
                if (memberSubnetID != null && memberAdminStateIsUp != null &&
                        memberSubnetID.equals(loadBalancerSubnetID) && memberAdminStateIsUp) {
                    memberID = neutronLBPoolMember.getPoolMemberID();
                    memberIP = neutronLBPoolMember.getPoolMemberAddress();
                    memberPort = neutronLBPoolMember.getPoolMemberProtoPort();
                    if (memberID == null || memberIP == null || memberPort == null) {
                        logger.debug("Neutron LB pool member details incomplete: {}", neutronLBPoolMember);
                        continue;
                    }
                    memberMAC = NeutronCacheUtils.getMacAddress(neutronPortCache, memberSubnetID, memberIP);
                    if (memberMAC == null) {
                        continue;
                    }
                    lbConfig.addMember(memberID, memberIP, memberMAC, poolProtocol, memberPort);
                }
            }

            if (lbConfig.getMembers().size() > 0) {
                lbConfigList.add(lbConfig);
            }
        }

        return lbConfigList;
    }

    @Override
    public void setDependencies(BundleContext bundleContext, ServiceReference serviceReference) {
        loadBalancerProvider =
                (LoadBalancerProvider) ServiceHelper.getGlobalInstance(LoadBalancerProvider.class, this);
        nodeCacheManager =
                (NodeCacheManager) ServiceHelper.getGlobalInstance(NodeCacheManager.class, this);
        eventDispatcher =
                (EventDispatcher) ServiceHelper.getGlobalInstance(EventDispatcher.class, this);
        eventDispatcher.eventHandlerAdded(
                bundleContext.getServiceReference(INeutronLoadBalancerPoolAware.class.getName()), this);
    }

    @Override
    public void setDependencies(Object impl) {
        if (impl instanceof INeutronNetworkCRUD) {
            neutronNetworkCache = (INeutronNetworkCRUD)impl;
        } else if (impl instanceof INeutronPortCRUD) {
            neutronPortCache = (INeutronPortCRUD)impl;
        } else if (impl instanceof INeutronSubnetCRUD) {
            neutronSubnetCache = (INeutronSubnetCRUD)impl;
        } else if (impl instanceof INeutronLoadBalancerCRUD) {
            neutronLBCache = (INeutronLoadBalancerCRUD)impl;
        } else if (impl instanceof LoadBalancerProvider) {
            loadBalancerProvider = (LoadBalancerProvider)impl;
        }
    }
}
