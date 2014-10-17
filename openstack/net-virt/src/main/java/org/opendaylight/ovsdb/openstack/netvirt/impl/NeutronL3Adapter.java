/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Dave Tucker, Flavio Fernandes
 */

package org.opendaylight.ovsdb.openstack.netvirt.impl;

import org.opendaylight.controller.networkconfig.neutron.INeutronNetworkCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronPortCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronSubnetCRUD;
import org.opendaylight.controller.networkconfig.neutron.NeutronFloatingIP;
import org.opendaylight.controller.networkconfig.neutron.NeutronNetwork;
import org.opendaylight.controller.networkconfig.neutron.NeutronPort;
import org.opendaylight.controller.networkconfig.neutron.NeutronRouter;
import org.opendaylight.controller.networkconfig.neutron.NeutronRouter_Interface;
import org.opendaylight.controller.networkconfig.neutron.NeutronSubnet;
import org.opendaylight.controller.networkconfig.neutron.Neutron_IPs;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.HexEncode;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.openstack.netvirt.api.Constants;
import org.opendaylight.ovsdb.openstack.netvirt.api.MultiTenantAwareRouter;
import org.opendaylight.ovsdb.openstack.netvirt.api.NetworkingProviderManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.TenantNetworkManager;
import org.opendaylight.ovsdb.plugin.api.OvsdbConfigurationService;
import org.opendaylight.ovsdb.plugin.api.OvsdbConnectionService;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.openstack.netvirt.api.Action;
import org.opendaylight.ovsdb.openstack.netvirt.api.ArpProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.InboundNatProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.L3ForwardingProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.OutboundNatProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.RoutingProvider;
import org.opendaylight.ovsdb.schema.openvswitch.Interface;

import com.google.common.base.Preconditions;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Neutron L3 Adapter implements a hub-like adapter for the various Neutron events. Based on
 * these events, the abstract router callbacks can be generated to the multi-tenant aware router,
 * as well as the multi-tenant router forwarding provider.
 */
public class NeutronL3Adapter {

    /**
     * Logger instance.
     */
    static final Logger logger = LoggerFactory.getLogger(NeutronL3Adapter.class);

    // The implementation for each of these services is resolved by the OSGi Service Manager
    private volatile org.opendaylight.ovsdb.openstack.netvirt.api.ConfigurationService configurationService;
    private volatile TenantNetworkManager tenantNetworkManager;
    private volatile NetworkingProviderManager networkingProviderManager;
    private volatile OvsdbConfigurationService ovsdbConfigurationService;
    private volatile OvsdbConnectionService connectionService;
    private volatile INeutronNetworkCRUD neutronNetworkCache;
    private volatile INeutronSubnetCRUD neutronSubnetCache;
    private volatile INeutronPortCRUD neutronPortCache;
    private volatile MultiTenantAwareRouter multiTenantAwareRouter;
    private volatile L3ForwardingProvider l3ForwardingProvider;
    private volatile InboundNatProvider inboundNatProvider;
    private volatile OutboundNatProvider outboundNatProvider;
    private volatile ArpProvider arpProvider;
    private volatile RoutingProvider routingProvider;

    private Set<String> inboundIpRewriteCache;
    private Set<String> outboundIpRewriteCache;
    private Set<String> inboundIpRewriteExclusionCache;
    private Set<String> outboundIpRewriteExclusionCache;
    private Set<String> routerInterfacesCache;
    private Set<String> staticArpEntryCache;
    private Set<String> l3ForwardingCache;
    private Set<String> defaultRouteCache;
    private Map<String, String> networkIdToRouterMacCache;
    private Map<String, NeutronRouter_Interface> subnetIdToRouterInterfaceCache;
    private Boolean enabled = false;

    void init() {
        final String enabledPropertyStr = getProperty(this.getClass(), "ovsdb.l3.fwd.enabled");
        if (enabledPropertyStr != null && enabledPropertyStr.equalsIgnoreCase("yes")) {
            this.inboundIpRewriteCache = new HashSet<>();
            this.outboundIpRewriteCache = new HashSet<>();
            this.inboundIpRewriteExclusionCache = new HashSet<>();
            this.outboundIpRewriteExclusionCache = new HashSet<>();
            this.routerInterfacesCache = new HashSet<>();
            this.staticArpEntryCache = new HashSet<>();
            this.l3ForwardingCache = new HashSet<>();
            this.defaultRouteCache = new HashSet<>();
            this.networkIdToRouterMacCache = new HashMap<>();
            this.subnetIdToRouterInterfaceCache = new HashMap<>();

            this.enabled = true;
            logger.info("OVSDB L3 forwarding is enabled");
        } else {
            logger.debug("OVSDB L3 forwarding is disabled");
        }
    }

    // TODO: move getProperty() to a common module
    private static String getProperty(Class<?> classParam, final String propertyStr) {
        Bundle bundle = FrameworkUtil.getBundle(classParam);
        BundleContext bundleContext = (bundle == null) ? null : bundle.getBundleContext();
        String value = (bundleContext == null) ? null : bundleContext.getProperty(propertyStr);
        return (value == null) ? System.getProperty(propertyStr) : value;
    }

    //
    // Callbacks from OVSDB's northbound handlers
    //

    public void handleNeutronSubnetEvent(final NeutronSubnet subnet, Action action) {
        logger.debug("Neutron subnet {} event : {}", action, subnet.toString());
        if (!this.enabled)
            return;
    }

    public void handleNeutronPortEvent(final NeutronPort neutronPort, Action action) {
        logger.debug("Neutron port {} event : {}", action, neutronPort.toString());
        if (!this.enabled)
            return;

        final boolean isDelete = action == Action.DELETE;

        // Treat the port event as a router interface event if the port belongs to router. This is a
        // helper for handling cases when handleNeutronRouterInterfaceEvent is not available
        //
        if (neutronPort.getDeviceOwner().equalsIgnoreCase("network:router_interface")) {
            for (Neutron_IPs neutronIP : neutronPort.getFixedIPs()) {
                NeutronRouter_Interface neutronRouterInterface =
                        new NeutronRouter_Interface(neutronIP.getSubnetUUID(), neutronPort.getPortUUID());
                neutronRouterInterface.setID(neutronIP.getSubnetUUID());  // id of router interface to be same as subnet
                neutronRouterInterface.setTenantID(neutronPort.getTenantID());

                this.handleNeutronRouterInterfaceEvent(null /*neutronRouter*/, neutronRouterInterface, action);
            }
        } else {
            // We made it here, port is not used as a router interface. If this is not a delete action, make sure that
            // all nodes that are supposed to have a router interface for the port's subnet(s), have it configured. We
            // need to do this check here because a router interface is not added to a node until tenant becomes needed
            // there.
            //
            if (!isDelete) {
                for (Neutron_IPs neutronIP : neutronPort.getFixedIPs()) {
                    NeutronRouter_Interface neutronRouterInterface =
                            subnetIdToRouterInterfaceCache.get(neutronIP.getSubnetUUID());
                    if (neutronRouterInterface != null) {
                        this.handleNeutronRouterInterfaceEvent(null /*neutronRouter*/, neutronRouterInterface, action);
                    }
                }
            }
            this.updateL3ForNeutronPort(neutronPort, isDelete);
        }
    }

    public void handleNeutronRouterEvent(final NeutronRouter neutronRouter, Action action) {
        logger.debug("Neutron router {} event : {}", action, neutronRouter.toString());
        if (!this.enabled)
            return;
    }

    public void handleNeutronRouterInterfaceEvent(final NeutronRouter neutronRouter,
                                                  final NeutronRouter_Interface neutronRouterInterface,
                                                  Action action) {
        logger.debug("Router interface {} got event {}. Subnet {}",
                     neutronRouterInterface.getPortUUID(),
                     action,
                     neutronRouterInterface.getSubnetUUID());
        if (!this.enabled)
            return;

        final boolean isDelete = action == Action.DELETE;

        this.programFlowsForNeutronRouterInterface(neutronRouterInterface, isDelete);

        // As neutron router interface is added/removed, we need to iterate through all the neutron ports and
        // see if they are affected by l3
        //
        for (NeutronPort neutronPort : neutronPortCache.getAllPorts()) {
            boolean currPortShouldBeDeleted = false;
            // Note: delete in this case only applies to 1)router interface delete and 2)ports on the same subnet
            if (isDelete) {
                for (Neutron_IPs neutronIP : neutronPort.getFixedIPs()) {
                    if (neutronRouterInterface.getSubnetUUID().equalsIgnoreCase(neutronIP.getSubnetUUID())) {
                        currPortShouldBeDeleted = true;
                        break;
                    }
                }
            }
            this.updateL3ForNeutronPort(neutronPort, currPortShouldBeDeleted);
        }
    }

    public void handleNeutronFloatingIPEvent(final NeutronFloatingIP neutronFloatingIP,
                                             Action action) {
        logger.debug(" Floating IP {} {}<->{}, network uuid {}", action,
                     neutronFloatingIP.getFixedIPAddress(),
                     neutronFloatingIP.getFloatingIPAddress(),
                     neutronFloatingIP.getFloatingNetworkUUID());
        if (!this.enabled)
            return;

        this.programFlowsForFloatingIP(neutronFloatingIP, action == Action.DELETE);
    }

    public void handleNeutronNetworkEvent(final NeutronNetwork neutronNetwork, Action action) {
        logger.debug("neutronNetwork {}: network: {}", action, neutronNetwork);
        if (!this.enabled)
            return;
    }

    //
    // Callbacks from OVSDB's southbound handler
    //
    public void handleInterfaceEvent(final Node node, final Interface intf, final NeutronNetwork neutronNetwork,
                                     Action action) {
        logger.debug("southbound interface {} node:{} interface:{}, neutronNetwork:{}",
                     action, node, intf.getName(), neutronNetwork);
        if (!this.enabled)
            return;

        // See if there is an external uuid, so we can find the respective neutronPort
        Map<String, String> externalIds = intf.getExternalIdsColumn().getData();
        if (externalIds == null) {
            return;
        }
        String neutronPortId = externalIds.get(Constants.EXTERNAL_ID_INTERFACE_ID);
        if (neutronPortId == null) {
            return;
        }
        final NeutronPort neutronPort = neutronPortCache.getPort(neutronPortId);
        if (neutronPort == null) {
            logger.warn("southbound interface {} node:{} interface:{}, neutronNetwork:{} did not find port:{}",
                        action, node, intf.getName(), neutronNetwork, neutronPortId);
            return;
        }
        this.handleNeutronPortEvent(neutronPort, action);
    }

    //
    // Internal helpers
    //
    private void updateL3ForNeutronPort(final NeutronPort neutronPort, final boolean isDelete) {

        final String networkUUID = neutronPort.getNetworkUUID();
        final String routerMacAddress = networkIdToRouterMacCache.get(networkUUID);

        // If there is no router interface handling the networkUUID, we are done
        if (routerMacAddress == null || routerMacAddress.isEmpty()) {
            return;
        }

        // If this is the neutron port for the router interface itself, ignore it as well. Ports that represent the
        // router interface are handled via handleNeutronRouterInterfaceEvent.
        if (routerMacAddress.equalsIgnoreCase(neutronPort.getMacAddress())) {
            return;
        }

        final NeutronNetwork neutronNetwork = neutronNetworkCache.getNetwork(networkUUID);
        final String providerSegmentationId = neutronNetwork != null ?
                                              neutronNetwork.getProviderSegmentationID() : null;
        final String tenantMac = neutronPort.getMacAddress();

        if (providerSegmentationId == null || providerSegmentationId.isEmpty() ||
            tenantMac == null || tenantMac.isEmpty()) {
            return;  // done: go no further w/out all the info needed...
        }

        final Action action = isDelete ? Action.DELETE : Action.ADD;
        List<Node> nodes = connectionService.getNodes();
        if (nodes.isEmpty()) {
            logger.trace("updateL3ForNeutronPort has no nodes to work with");
        }
        for (Node node : nodes) {
            final Long dpid = getDpid(node);
            final boolean tenantNetworkPresentInNode =
                    tenantNetworkManager.isTenantNetworkPresentInNode(node, providerSegmentationId);
            for (Neutron_IPs neutronIP : neutronPort.getFixedIPs()) {
                final String tenantIpStr = neutronIP.getIpAddress();
                if (tenantIpStr.isEmpty()) {
                    continue;
                }

                // Configure L3 fwd. We do that regardless of tenant network present, because these rules are
                // still needed when routing to subnets non-local to node (bug 2076).
                programL3ForwardingStage1(node, dpid, providerSegmentationId, tenantMac, tenantIpStr, action);

                // Configure distributed ARP responder. Only needed if tenant network exists in node.
                programStaticArpStage1(node, dpid, providerSegmentationId, tenantMac, tenantIpStr,
                                       tenantNetworkPresentInNode ? action : Action.DELETE);
            }
        }
    }

    private void programL3ForwardingStage1(Node node, Long dpid, String providerSegmentationId,
                                           String macAddress, String ipStr,
                                           Action actionForNode) {
        // Based on the local cache, figure out whether programming needs to occur. To do this, we
        // will look at desired action for node.
        //
        final String cacheKey = node.toString() + ":" + providerSegmentationId + ":" + ipStr;
        final Boolean isProgrammed = l3ForwardingCache.contains(cacheKey);

        if (actionForNode == Action.DELETE && isProgrammed == Boolean.FALSE) {
            logger.trace("programL3ForwardingStage1 for node {} providerId {} mac {} ip {} action {} is already done",
                         node.getNodeIDString(), providerSegmentationId, macAddress, ipStr, actionForNode);
            return;
        }
        if (actionForNode == Action.ADD && isProgrammed == Boolean.TRUE) {
            logger.trace("programL3ForwardingStage1 for node {} providerId {} mac {} ip {} action {} is already done",
                         node.getNodeIDString(), providerSegmentationId, macAddress, ipStr, actionForNode);
            return;
        }

        Status status = this.programL3ForwardingStage2(node, dpid, providerSegmentationId,
                                                       macAddress, ipStr, actionForNode);
        if (status.isSuccess()) {
            // Update cache
            if (actionForNode == Action.ADD) {
                l3ForwardingCache.add(cacheKey);
            } else {
                l3ForwardingCache.remove(cacheKey);
            }
        }
    }

    private Status programL3ForwardingStage2(Node node, Long dpid, String providerSegmentationId,
                                             String macAddress,
                                             String address,
                                             Action actionForNode) {
        Status status;
        try {
            InetAddress inetAddress = InetAddress.getByName(address);
            status = l3ForwardingProvider == null ?
                     new Status(StatusCode.SUCCESS) :
                     l3ForwardingProvider.programForwardingTableEntry(node, dpid, providerSegmentationId,
                                                                      inetAddress, macAddress, actionForNode);
        } catch (UnknownHostException e) {
            status = new Status(StatusCode.BADREQUEST);
        }

        if (status.isSuccess()) {
            logger.debug("ProgramL3Forwarding {} for mac:{} addr:{} node:{} action:{}",
                         l3ForwardingProvider == null ? "skipped" : "programmed",
                         macAddress, address, node, actionForNode);
        } else {
            logger.error("ProgramL3Forwarding failed for mac:{} addr:{} node:{} action:{} status:{}",
                         macAddress, address, node, actionForNode, status);
        }
        return status;
    }

    // --

    private void programFlowsForNeutronRouterInterface(final NeutronRouter_Interface destNeutronRouterInterface,
                                                       Boolean isDelete) {
        Preconditions.checkNotNull(destNeutronRouterInterface);

        final NeutronPort neutronPort = neutronPortCache.getPort(destNeutronRouterInterface.getPortUUID());
        final String macAddress = neutronPort != null ? neutronPort.getMacAddress() : null;
        final List<Neutron_IPs> ipList = neutronPort != null ? neutronPort.getFixedIPs() : null;
        final NeutronSubnet subnet = neutronSubnetCache.getSubnet(destNeutronRouterInterface.getSubnetUUID());
        final NeutronNetwork neutronNetwork = subnet != null ?
                                              neutronNetworkCache.getNetwork(subnet.getNetworkUUID()) : null;
        final String destinationSegmentationId = neutronNetwork != null ?
                                                 neutronNetwork.getProviderSegmentationID() : null;
        final String gatewayIp = subnet != null ? subnet.getGatewayIP() : null;
        final Boolean isExternal = neutronNetwork != null ? neutronNetwork.getRouterExternal() : Boolean.TRUE;
        final String cidr = subnet != null ? subnet.getCidr() : null;
        final int mask = getMaskLenFromCidr(cidr);

        logger.trace("programFlowsForNeutronRouterInterface called for interface {} isDelete {}",
                     destNeutronRouterInterface, isDelete);

        if (destinationSegmentationId == null || destinationSegmentationId.isEmpty() ||
            cidr == null || cidr.isEmpty() ||
            macAddress == null || macAddress.isEmpty() ||
            ipList == null || ipList.isEmpty()) {
            logger.debug("programFlowsForNeutronRouterInterface is bailing seg:{} cidr:{} mac:{}  ip:{}",
                         destinationSegmentationId, cidr, macAddress, ipList);
            return;  // done: go no further w/out all the info needed...
        }

        final Action action = isDelete ? Action.DELETE : Action.ADD;

        // Keep cache for finding router's mac from network uuid -- add
        //
        if (! isDelete) {
            networkIdToRouterMacCache.put(neutronNetwork.getNetworkUUID(), macAddress);
            subnetIdToRouterInterfaceCache.put(subnet.getSubnetUUID(), destNeutronRouterInterface);
        }

        List<Node> nodes = connectionService.getNodes();
        if (nodes.isEmpty()) {
            logger.trace("programFlowsForNeutronRouterInterface has no nodes to work with");
        }
        for (Node node : nodes) {
            final Long dpid = getDpid(node);
            final Action actionForNode =
                    tenantNetworkManager.isTenantNetworkPresentInNode(node, destinationSegmentationId) ?
                    action : Action.DELETE;

            for (Neutron_IPs neutronIP : ipList) {
                final String ipStr = neutronIP.getIpAddress();
                if (ipStr.isEmpty()) {
                    logger.debug("programFlowsForNeutronRouterInterface is skipping node {} ip {}",
                                 node.getID(), ipStr);
                    continue;
                }

                // Iterate through all other interfaces and add/remove reflexive flows to this interface
                //
                for (NeutronRouter_Interface srcNeutronRouterInterface : subnetIdToRouterInterfaceCache.values()) {
                    programFlowsForNeutronRouterInterfacePair(node, dpid,
                                                              srcNeutronRouterInterface, destNeutronRouterInterface,
                                                              neutronNetwork, destinationSegmentationId,
                                                              macAddress, ipStr, mask, actionForNode,
                                                              true /*isReflexsive*/);
                }

                programStaticArpStage1(node, dpid, destinationSegmentationId, macAddress, ipStr, actionForNode);
            }

            // Compute action to be programmed. In the case of rewrite exclusions, we must never program rules
            // for the external neutron networks.
            //
            {
                final Action actionForRewriteExclusion = isExternal ? Action.DELETE : actionForNode;
                programIpRewriteExclusionStage1(node, dpid, destinationSegmentationId, true /* isInbound */,
                                                cidr, actionForRewriteExclusion);
                programIpRewriteExclusionStage1(node, dpid, destinationSegmentationId, false /* isInbound */,
                                                cidr, actionForRewriteExclusion);
            }

            // Default route. For non-external subnet, make sure that there is none configured.
            //
            if (gatewayIp != null && !gatewayIp.isEmpty()) {
                final Action actionForNodeDefaultRoute =
                        isExternal ? actionForNode : Action.DELETE;
                final String defaultGatewayMacAddress = configurationService.getDefaultGatewayMacAddress(node);
                programDefaultRouteStage1(node, dpid, destinationSegmentationId, defaultGatewayMacAddress, gatewayIp,
                                          actionForNodeDefaultRoute);
            }
        }

        // Keep cache for finding router's mac from network uuid -- remove
        //
        if (isDelete) {
            networkIdToRouterMacCache.remove(neutronNetwork.getNetworkUUID());
            subnetIdToRouterInterfaceCache.remove(subnet.getSubnetUUID());
        }
    }

    private void programFlowsForNeutronRouterInterfacePair(final Node node,
                                                           final Long dpid,
                                                           final NeutronRouter_Interface srcNeutronRouterInterface,
                                                           final NeutronRouter_Interface dstNeutronRouterInterface,
                                                           final NeutronNetwork dstNeutronNetwork,
                                                           final String destinationSegmentationId,
                                                           final String dstMacAddress,
                                                           final String destIpStr,
                                                           final int destMask,
                                                           final Action actionForNode,
                                                           Boolean isReflexsive) {
        Preconditions.checkNotNull(srcNeutronRouterInterface);
        Preconditions.checkNotNull(dstNeutronRouterInterface);

        final String sourceSubnetId = srcNeutronRouterInterface.getSubnetUUID();
        if (sourceSubnetId == null) {
            logger.error("Could not get provider Subnet ID from router interface {}",
                         srcNeutronRouterInterface.getID());
            return;
        }

        final NeutronSubnet sourceSubnet = neutronSubnetCache.getSubnet(sourceSubnetId);
        final String sourceNetworkId = sourceSubnet == null ? null : sourceSubnet.getNetworkUUID();
        if (sourceNetworkId == null) {
            logger.error("Could not get provider Network ID from subnet {}", sourceSubnetId);
            return;
        }

        final NeutronNetwork sourceNetwork = neutronNetworkCache.getNetwork(sourceNetworkId);
        if (sourceNetwork == null) {
            logger.error("Could not get provider Network for Network ID {}", sourceNetworkId);
            return;
        }

        if (! sourceNetwork.getTenantID().equals(dstNeutronNetwork.getTenantID())) {
            // Isolate subnets from different tenants within the same router
            return;
        }
        final String sourceSegmentationId = sourceNetwork.getProviderSegmentationID();
        if (sourceSegmentationId == null) {
            logger.error("Could not get provider Segmentation ID for Subnet {}", sourceSubnetId);
            return;
        }
        if (sourceSegmentationId.equals(destinationSegmentationId)) {
            // Skip 'self'
            return;
        }

        programRouterInterfaceStage1(node, dpid, sourceSegmentationId, destinationSegmentationId,
                                     dstMacAddress, destIpStr, destMask, actionForNode);

        // Flip roles src->dst; dst->src
        if (isReflexsive) {
            final NeutronPort sourceNeutronPort = neutronPortCache.getPort(srcNeutronRouterInterface.getPortUUID());
            final String macAddress2 = sourceNeutronPort != null ? sourceNeutronPort.getMacAddress() : null;
            final List<Neutron_IPs> ipList2 = sourceNeutronPort != null ? sourceNeutronPort.getFixedIPs() : null;
            final String cidr2 = sourceSubnet.getCidr();
            final int mask2 = getMaskLenFromCidr(cidr2);

            if (cidr2 == null || cidr2.isEmpty() ||
                macAddress2 == null || macAddress2.isEmpty() ||
                ipList2 == null || ipList2.isEmpty()) {
                logger.trace("programFlowsForNeutronRouterInterfacePair reflexive is bailing seg:{} cidr:{} mac:{} ip:{}",
                             sourceSegmentationId, cidr2, macAddress2, ipList2);
                return;  // done: go no further w/out all the info needed...
            }

            for (Neutron_IPs neutronIP2 : ipList2) {
                final String ipStr2 = neutronIP2.getIpAddress();
                if (ipStr2.isEmpty()) {
                    continue;
                }
                programFlowsForNeutronRouterInterfacePair(node, dpid, dstNeutronRouterInterface,
                                                          srcNeutronRouterInterface,
                                                          sourceNetwork, sourceSegmentationId,
                                                          macAddress2, ipStr2, mask2, actionForNode,
                                                          false /*isReflexsive*/);
            }
        }
    }

    private void programRouterInterfaceStage1(Node node, Long dpid, String sourceSegmentationId,
                                              String destinationSegmentationId,
                                              String macAddress, String ipStr, int mask,
                                              Action actionForNode) {
        // Based on the local cache, figure out whether programming needs to occur. To do this, we
        // will look at desired action for node.
        //
        final String cacheKey = node.toString() + ":" + sourceSegmentationId + ":" + destinationSegmentationId + ":" +
                                ipStr + "/" + Integer.toString(mask);
        final Boolean isProgrammed = routerInterfacesCache.contains(cacheKey);

        if (actionForNode == Action.DELETE && isProgrammed == Boolean.FALSE) {
            logger.trace("programRouterInterfaceStage1 for node {} sourceSegId {} destSegId {} mac {} ip {} mask {}" +
                         "action {} is already done",
                         node.getNodeIDString(), sourceSegmentationId, destinationSegmentationId,
                         ipStr, mask, actionForNode);
            return;
        }
        if (actionForNode == Action.ADD && isProgrammed == Boolean.TRUE) {
            logger.trace("programRouterInterfaceStage1 for node {} sourceSegId {} destSegId {} mac {} ip {} mask {}" +
                         "action {} is already done",
                         node.getNodeIDString(), sourceSegmentationId, destinationSegmentationId,
                         ipStr, mask, actionForNode);
            return;
        }

        Status status = this.programRouterInterfaceStage2(node, dpid, sourceSegmentationId, destinationSegmentationId,
                                                          macAddress, ipStr, mask, actionForNode);
        if (status.isSuccess()) {
            // Update cache
            if (actionForNode == Action.ADD) {
                // TODO: multiTenantAwareRouter.addInterface(UUID.fromString(tenant), ...);
                routerInterfacesCache.add(cacheKey);
            } else {
                // TODO: multiTenantAwareRouter.removeInterface(...);
                routerInterfacesCache.remove(cacheKey);
            }
        }
    }

    private Status programRouterInterfaceStage2(Node node, Long dpid, String sourceSegmentationId,
                                                String destinationSegmentationId,
                                                String macAddress,
                                                String address, int mask,
                                                Action actionForNode) {
        Status status;
        try {
            InetAddress inetAddress = InetAddress.getByName(address);
            status = routingProvider == null ?
                     new Status(StatusCode.SUCCESS) :
                     routingProvider.programRouterInterface(node, dpid, sourceSegmentationId, destinationSegmentationId,
                                                            macAddress, inetAddress, mask, actionForNode);
        } catch (UnknownHostException e) {
            status = new Status(StatusCode.BADREQUEST);
        }

        if (status.isSuccess()) {
            logger.debug("ProgramRouterInterface {} for mac:{} addr:{}/{} node:{} action:{}",
                         routingProvider == null ? "skipped" : "programmed",
                         macAddress, address, mask, node, actionForNode);
        } else {
            logger.error("ProgramRouterInterface failed for mac:{} addr:{}/{} node:{} action:{} status:{}",
                         macAddress, address, mask, node, actionForNode, status);
        }
        return status;
    }

    private void programStaticArpStage1(Node node, Long dpid, String providerSegmentationId,
                                        String macAddress, String ipStr,
                                        Action actionForNode) {
        // Based on the local cache, figure out whether programming needs to occur. To do this, we
        // will look at desired action for node.
        //
        final String cacheKey = node.toString() + ":" + providerSegmentationId + ":" + ipStr;
        final Boolean isProgrammed = staticArpEntryCache.contains(cacheKey);

        if (actionForNode == Action.DELETE && isProgrammed == Boolean.FALSE) {
            logger.trace("programStaticArpStage1 node {} providerId {} mac {} ip {} action {} is already done",
                         node.getNodeIDString(), providerSegmentationId, macAddress, ipStr, actionForNode);
            return;
        }
        if (actionForNode == Action.ADD && isProgrammed == Boolean.TRUE) {
            logger.trace("programStaticArpStage1 node {} providerId {} mac {} ip {} action {} is already done",
                         node.getNodeIDString(), providerSegmentationId, macAddress, ipStr, actionForNode);
            return;
        }

        Status status = this.programStaticArpStage2(node, dpid, providerSegmentationId,
                                                    macAddress, ipStr, actionForNode);
        if (status.isSuccess()) {
            // Update cache
            if (actionForNode == Action.ADD) {
                staticArpEntryCache.add(cacheKey);
            } else {
                staticArpEntryCache.remove(cacheKey);
            }
        }
    }

    private Status programStaticArpStage2(Node node, Long dpid, String providerSegmentationId,
                                                String macAddress,
                                                String address,
                                                Action actionForNode) {
        Status status;
        try {
            InetAddress inetAddress = InetAddress.getByName(address);
            status = arpProvider == null ?
                     new Status(StatusCode.SUCCESS) :
                     arpProvider.programStaticArpEntry(node, dpid, providerSegmentationId,
                                                       macAddress, inetAddress, actionForNode);
        } catch (UnknownHostException e) {
            status = new Status(StatusCode.BADREQUEST);
        }

        if (status.isSuccess()) {
            logger.debug("ProgramStaticArp {} for mac:{} addr:{} node:{} action:{}",
                         arpProvider == null ? "skipped" : "programmed",
                         macAddress, address, node, actionForNode);
        } else {
            logger.error("ProgramStaticArp failed for mac:{} addr:{} node:{} action:{} status:{}",
                         macAddress, address, node, actionForNode, status);
        }
        return status;
    }

    private void programIpRewriteExclusionStage1(Node node, Long dpid, String providerSegmentationId,
                                                 final boolean isInbound, String cidr,
                                                 Action actionForRewriteExclusion) {
        // Based on the local cache, figure out whether programming needs to occur. To do this, we
        // will look at desired action for node.
        //
        final String cacheKey = node.toString() + ":" + providerSegmentationId + ":" + cidr;
        final Boolean isProgrammed = isInbound ?
                                     inboundIpRewriteExclusionCache.contains(cacheKey):
                                     outboundIpRewriteExclusionCache.contains(cacheKey);

        if (actionForRewriteExclusion == Action.DELETE && isProgrammed == Boolean.FALSE) {
            logger.trace("programIpRewriteExclusionStage1 node {} providerId {} {} cidr {} action {} is already done",
                         node.getNodeIDString(), providerSegmentationId, isInbound ? "inbound" : "outbound", cidr,
                         actionForRewriteExclusion);
            return;
        }
        if (actionForRewriteExclusion == Action.ADD && isProgrammed == Boolean.TRUE) {
            logger.trace("programIpRewriteExclusionStage1 node {} providerId {} {} cidr {} action {} is already done",
                         node.getNodeIDString(), providerSegmentationId, isInbound ? "inbound" : "outbound", cidr,
                         actionForRewriteExclusion);
            return;
        }

        Status status = this.programIpRewriteExclusionStage2(node, dpid, providerSegmentationId, cidr,
                                                             isInbound, actionForRewriteExclusion);
        if (status.isSuccess()) {
            // Update cache
            if (actionForRewriteExclusion == Action.ADD) {
                if (isInbound) {
                    inboundIpRewriteExclusionCache.add(cacheKey);
                } else {
                    outboundIpRewriteExclusionCache.add(cacheKey);
                }
            } else {
                if (isInbound) {
                    inboundIpRewriteExclusionCache.remove(cacheKey);
                } else {
                    outboundIpRewriteExclusionCache.remove(cacheKey);
                }
            }
        }
    }

    private Status programIpRewriteExclusionStage2(Node node, Long dpid, String providerSegmentationId, String cidr,
                                                   final boolean isInbound, Action actionForNode) {
        Status status;
        if (isInbound) {
            status = inboundNatProvider == null ? new Status(StatusCode.SUCCESS) :
                     inboundNatProvider.programIpRewriteExclusion(node, dpid, providerSegmentationId, cidr,
                                                                  actionForNode);
        } else {
            status = outboundNatProvider == null ? new Status(StatusCode.SUCCESS) :
                     outboundNatProvider.programIpRewriteExclusion(node, dpid, providerSegmentationId, cidr,
                                                                   actionForNode);
        }

        if (status.isSuccess()) {
            final boolean isSkipped = isInbound ? inboundNatProvider == null : outboundNatProvider == null;
            logger.debug("IpRewriteExclusion {} {} for cidr:{} node:{} action:{}",
                         (isInbound ? "inbound" : "outbound"), (isSkipped ? "skipped" : "programmed"),
                         cidr, node, actionForNode);
        } else {
            logger.error("IpRewriteExclusion {} failed for cidr:{} node:{} action:{} status:{}",
                         (isInbound ? "inbound" : "outbound"), cidr, node, actionForNode, status);
        }
        return status;
    }

    private void programDefaultRouteStage1(Node node, Long dpid, String providerSegmentationId,
                                           String defaultGatewayMacAddress, String gatewayIp,
                                           Action actionForNodeDefaultRoute) {
        // Based on the local cache, figure out whether programming needs to occur. To do this, we
        // will look at desired action for node.
        //
        final String cacheKey = node.toString() + ":" + providerSegmentationId + ":" + gatewayIp;
        final Boolean isProgrammed = defaultRouteCache.contains(cacheKey);

        if (actionForNodeDefaultRoute == Action.DELETE && isProgrammed == Boolean.FALSE) {
            logger.trace("programDefaultRouteStage1 node {} providerId {} mac {} gw {} action {} is already done",
                         node.getNodeIDString(), providerSegmentationId, defaultGatewayMacAddress, gatewayIp,
                         actionForNodeDefaultRoute);
            return;
        }
        if (actionForNodeDefaultRoute == Action.ADD && isProgrammed == Boolean.TRUE) {
            logger.trace("programDefaultRouteStage1 node {} providerId {} mac {} gw {} action {} is already done",
                         node.getNodeIDString(), providerSegmentationId, defaultGatewayMacAddress, gatewayIp,
                         actionForNodeDefaultRoute);
            return;
        }

        Status status = this.programDefaultRouteStage2(node, dpid, providerSegmentationId,
                                                       defaultGatewayMacAddress, gatewayIp, actionForNodeDefaultRoute);
        if (status.isSuccess()) {
            // Update cache
            if (actionForNodeDefaultRoute == Action.ADD) {
                defaultRouteCache.add(cacheKey);
            } else {
                defaultRouteCache.remove(cacheKey);
            }
        }
    }

    private Status programDefaultRouteStage2(Node node, Long dpid, String providerSegmentationId,
                                          String defaultGatewayMacAddress,
                                          String gatewayIp,
                                          Action actionForNodeDefaultRoute) {
        // TODO: As of Helium, mac address for default gateway is required (bug 1705).
        if (defaultGatewayMacAddress == null) {
            logger.error("ProgramDefaultRoute mac not provided. gatewayIp:{} node:{} action:{}",
                         defaultGatewayMacAddress, gatewayIp, node, actionForNodeDefaultRoute);
            return new Status(StatusCode.NOTIMPLEMENTED);  // Bug 1705
        }

        Status status;
        try {
            InetAddress inetAddress = InetAddress.getByName(gatewayIp);
            status = routingProvider == null ?
                     new Status(StatusCode.SUCCESS) :
                     routingProvider.programDefaultRouteEntry(node, dpid, providerSegmentationId,
                                                              defaultGatewayMacAddress, inetAddress,
                                                              actionForNodeDefaultRoute);
        } catch (UnknownHostException e) {
            status = new Status(StatusCode.BADREQUEST);
        }

        if (status.isSuccess()) {
            logger.debug("ProgramDefaultRoute {} for mac:{} gatewayIp:{} node:{} action:{}",
                         routingProvider == null ? "skipped" : "programmed",
                         defaultGatewayMacAddress, gatewayIp, node, actionForNodeDefaultRoute);
        } else {
            logger.error("ProgramDefaultRoute failed for mac:{} gatewayIp:{} node:{} action:{} status:{}",
                         defaultGatewayMacAddress, gatewayIp, node, actionForNodeDefaultRoute, status);
        }
        return status;
    }

    private void programFlowsForFloatingIP(final NeutronFloatingIP neutronFloatingIP, Boolean isDelete) {
        Preconditions.checkNotNull(neutronFloatingIP);

        final String networkUUID = neutronFloatingIP.getFloatingNetworkUUID();
        final String routerMacAddress = networkIdToRouterMacCache.get(networkUUID);

        // If there is no router interface handling the networkUUID, we are done
        if (routerMacAddress == null || routerMacAddress.isEmpty()) {
            return;
        }

        final NeutronNetwork neutronNetwork = neutronNetworkCache.getNetwork(networkUUID);
        final String providerSegmentationId = neutronNetwork != null ?
                                              neutronNetwork.getProviderSegmentationID() : null;
        final String fixedIPAddress = neutronFloatingIP.getFixedIPAddress();
        final String floatingIpAddress = neutronFloatingIP.getFloatingIPAddress();

        if (providerSegmentationId == null || providerSegmentationId.isEmpty() ||
            // routerMacAddress == null || routerMacAddress.isEmpty() ||
            fixedIPAddress == null || fixedIPAddress.isEmpty() ||
            floatingIpAddress == null || floatingIpAddress.isEmpty()) {
            return;  // done: go no further w/out all the info needed...
        }

        final Action action = isDelete ? Action.DELETE : Action.ADD;
        List<Node> nodes = connectionService.getNodes();
        if (nodes.isEmpty()) {
            logger.trace("programFlowsForFloatingIP has no nodes to work with");
        }
        for (Node node : nodes) {
            final Long dpid = getDpid(node);
            final Action actionForNode =
                    tenantNetworkManager.isTenantNetworkPresentInNode(node, providerSegmentationId) ?
                    action : Action.DELETE;

            // Rewrite from float to fixed and vice-versa
            //
            programIpRewriteStage1(node, dpid, providerSegmentationId, true /* isInbound */,
                                   floatingIpAddress, fixedIPAddress, actionForNode);
            programIpRewriteStage1(node, dpid, providerSegmentationId, false /* isInboubd */,
                                   fixedIPAddress, floatingIpAddress, actionForNode);

            // Respond to arps for the floating ip address
            //
            programStaticArpStage1(node, dpid, providerSegmentationId, routerMacAddress, floatingIpAddress,
                                   actionForNode);
        }
    }

    private void programIpRewriteStage1(Node node, Long dpid, String providerSegmentationId,
                                        final boolean isInbound,
                                        String matchAddress, String rewriteAddress,
                                        Action actionForNode) {
        // Based on the local cache, figure out whether programming needs to occur. To do this, we
        // will look at desired action for node.
        //
        final String cacheKey = node.toString() + ":" + providerSegmentationId + ":" +
                                matchAddress + ":" + rewriteAddress;
        final Boolean isProgrammed = isInbound ?
                                     inboundIpRewriteCache.contains(cacheKey) :
                                     outboundIpRewriteCache.contains(cacheKey);

        if (actionForNode == Action.DELETE && isProgrammed == Boolean.FALSE) {
            logger.trace("programIpRewriteStage1 node {} providerId {} {} matchAddr {} rewriteAddr {} action {}" +
                         " is already done",
                         node.getNodeIDString(), providerSegmentationId, isInbound ? "inbound": "outbound",
                         matchAddress, rewriteAddress, actionForNode);
            return;
        }
        if (actionForNode == Action.ADD && isProgrammed == Boolean.TRUE) {
            logger.trace("programIpRewriteStage1 node {} providerId {} {} matchAddr {} rewriteAddr {} action {}" +
                         " is already done",
                         node.getNodeIDString(), providerSegmentationId, isInbound ? "inbound": "outbound",
                         matchAddress, rewriteAddress, actionForNode);
            return;
        }

        Status status = this.programIpRewriteStage2(node, dpid, providerSegmentationId, isInbound,
                                                    matchAddress, rewriteAddress, actionForNode);
        if (status.isSuccess()) {
            // Update cache
            if (actionForNode == Action.ADD) {
                if (isInbound) {
                    inboundIpRewriteCache.add(cacheKey);
                } else {
                    outboundIpRewriteCache.add(cacheKey);
                }
            } else {
                if (isInbound) {
                    inboundIpRewriteCache.remove(cacheKey);
                } else {
                    outboundIpRewriteCache.remove(cacheKey);
                }
            }
        }
    }

    private Status programIpRewriteStage2(Node node, Long dpid, String providerSegmentationId,
                                          final boolean isInbound,
                                          String matchAddress, String rewriteAddress,
                                          Action actionForNode) {
        Status status;
        try {
            InetAddress inetMatchAddress = InetAddress.getByName(matchAddress);
            InetAddress inetRewriteAddress = InetAddress.getByName(rewriteAddress);
            if (isInbound) {
                status = inboundNatProvider == null ?
                         new Status(StatusCode.SUCCESS) :
                         inboundNatProvider.programIpRewriteRule(node, dpid, providerSegmentationId,
                                                                 inetMatchAddress, inetRewriteAddress, actionForNode);
            } else {
                status = outboundNatProvider == null ?
                         new Status(StatusCode.SUCCESS) :
                         outboundNatProvider.programIpRewriteRule(node, dpid, providerSegmentationId,
                                                                  inetMatchAddress, inetRewriteAddress, actionForNode);
            }
        } catch (UnknownHostException e) {
            status = new Status(StatusCode.BADREQUEST);
        }

        if (status.isSuccess()) {
            final boolean isSkipped = isInbound ? inboundNatProvider == null : outboundNatProvider == null;
            logger.debug("ProgramIpRewrite {} {} for match:{} rewrite:{} node:{} action:{}",
                         (isInbound ? "inbound" : "outbound"), (isSkipped ? "skipped" : "programmed"),
                         matchAddress, rewriteAddress, node, actionForNode);
        } else {
            logger.error("ProgramIpRewrite {} failed for match:{} rewrite:{} node:{} action:{} status:{}",
                         (isInbound ? "inbound" : "outbound"),
                         matchAddress, rewriteAddress, node, actionForNode, status);
        }
        return status;
    }

    //
    // More Internals
    //

    private int getMaskLenFromCidr(String cidr) {
        if (cidr == null) return 0;
        String[] splits = cidr.split("/");
        if (splits.length != 2) return 0;

        int result;
        try {
            result = Integer.parseInt(splits[1].trim());
        }
        catch (NumberFormatException nfe)
        {
            result = 0;
        }
        return result;
    }

    private Long getDpid (Node node) {
        Preconditions.checkNotNull(ovsdbConfigurationService);

        String bridgeName = configurationService.getIntegrationBridgeName();
        String bridgeUuid = this.getInternalBridgeUUID(node, bridgeName);
        if (bridgeUuid == null) {
            logger.error("Unable to spot Bridge Identifier for {} in {}", bridgeName, node);
            return 0L;
        }

        try {
            Row bridgeRow =  ovsdbConfigurationService
                    .getRow(node, ovsdbConfigurationService.getTableName(node, Bridge.class), bridgeUuid);
            Bridge bridge = ovsdbConfigurationService.getTypedRow(node, Bridge.class, bridgeRow);
            Set<String> dpids = bridge.getDatapathIdColumn().getData();
            if (dpids == null || dpids.size() == 0) return 0L;
            return HexEncode.stringToLong((String) dpids.toArray()[0]);
        } catch (Exception e) {
            logger.error("Error finding Bridge's OF DPID", e);
            return 0L;
        }
    }

    private String getInternalBridgeUUID (Node node, String bridgeName) {
        Preconditions.checkNotNull(ovsdbConfigurationService);
        try {
            Map<String, Row> bridgeTable =
                    ovsdbConfigurationService.getRows(node,
                                                      ovsdbConfigurationService.getTableName(node, Bridge.class));
            if (bridgeTable == null) return null;
            for (String key : bridgeTable.keySet()) {
                Bridge bridge = ovsdbConfigurationService.getTypedRow(node, Bridge.class, bridgeTable.get(key));
                if (bridge.getName().equals(bridgeName)) return key;
            }
        } catch (Exception e) {
            logger.error("Error getting Bridge Identifier for {} / {}", node, bridgeName, e);
        }
        return null;
    }
}


