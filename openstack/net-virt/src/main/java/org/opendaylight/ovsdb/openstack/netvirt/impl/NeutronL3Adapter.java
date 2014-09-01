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

    private Set<String> ipRewriteCache;
    private Set<String> ipRewriteExclusionCache;
    private Set<String> routerInterfacesCache;
    private Set<String> staticArpEntryCache;
    private Set<String> defaultRouteCache;
    private Map<String, String> networkId2MacCache;

    void init() {
        this.ipRewriteCache = new HashSet<>();
        this.ipRewriteExclusionCache = new HashSet<>();
        this.routerInterfacesCache = new HashSet<>();
        this.staticArpEntryCache = new HashSet<>();
        this.defaultRouteCache = new HashSet<>();
        this.networkId2MacCache = new HashMap();
    }

    //
    // Callbacks from OVSDB's northbound handlers
    //

    public void handleNeutronSubnetEvent(final NeutronSubnet subnet, Action action) {
        logger.debug("Neutron subnet {} event : {}", action, subnet.toString());

        // TODO
    }

    public void handleNeutronPortEvent(final NeutronPort neutronPort, Action action) {
        logger.debug("Neutron port {} event : {}", action, neutronPort.toString());

        // TODO
    }

    public void handleNeutronRouterEvent(final NeutronRouter neutronRouter, Action action) {
        logger.debug("Neutron router {} event : {}", action, neutronRouter.toString());

        // TODO
    }

    public void handleNeutronRouterInterfaceEvent(final NeutronRouter neutronRouter,
                                                  final NeutronRouter_Interface neutronRouterInterface,
                                                  Action action) {
        logger.debug(" Router {} interface {} got event {}. Subnet {}",
                     neutronRouter.getName(),
                     neutronRouterInterface.getPortUUID(),
                     action,
                     neutronRouterInterface.getSubnetUUID());

        this.programFlowsForNeutronRouterInterface(neutronRouterInterface, action == Action.DELETE);
    }

    public void handleNeutronFloatingIPEvent(final NeutronFloatingIP neutronFloatingIP,
                                             Action action) {
        logger.debug(" Floating IP {} {}<->{}, network uuid {}", action,
                     neutronFloatingIP.getFixedIPAddress(),
                     neutronFloatingIP.getFloatingIPAddress(),
                     neutronFloatingIP.getFloatingNetworkUUID());

        this.programFlowsForFloatingIP(neutronFloatingIP, action == Action.DELETE);
    }

    public void handleNeutronNetworkEvent(final NeutronNetwork neutronNetwork, Action action) {
        logger.debug("neutronNetwork {}: network: {}", action, neutronNetwork);

        // TODO
    }

    //
    // Callbacks from OVSDB's southbound handler
    //
    public void handleInterfaceEvent(final Node node, final Interface intf, final NeutronNetwork neutronNetwork,
                                     Action action) {
        logger.debug("southbound interface {} node:{} interface:{}, neutronNetwork:{}",
                     action, node, intf, neutronNetwork);

        // TODO
    }

    //
    // Internal helpers
    //
    private void programFlowsForNeutronRouterInterface(final NeutronRouter_Interface neutronRouterInterface,
                                                       Boolean isDelete) {
        Preconditions.checkNotNull(neutronRouterInterface);

        final NeutronPort neutronPort = neutronPortCache.getPort(neutronRouterInterface.getPortUUID());
        final String macAddress = neutronPort != null ? neutronPort.getMacAddress() : null;
        final List<Neutron_IPs> ipList = neutronPort != null ? neutronPort.getFixedIPs() : null;
        final NeutronSubnet subnet = neutronSubnetCache.getSubnet(neutronRouterInterface.getSubnetUUID());
        final NeutronNetwork neutronNetwork = subnet != null ?
                                              neutronNetworkCache.getNetwork(subnet.getNetworkUUID()) : null;
        final String providerSegmentationId = neutronNetwork != null ?
                                              neutronNetwork.getProviderSegmentationID() : null;
        final String gatewayIp = subnet != null ? subnet.getGatewayIP() : null;
        final Boolean isExternal = neutronNetwork != null ? neutronNetwork.getRouterExternal() : Boolean.TRUE;
        final String cidr = subnet != null ? subnet.getCidr() : null;
        final int mask = getMaskLenFromCidr(cidr);

        if (providerSegmentationId == null || providerSegmentationId.isEmpty() ||
            cidr == null || cidr.isEmpty() ||
            macAddress == null || macAddress.isEmpty() ||
            ipList == null || ipList.isEmpty()) {
            return;  // done: go no further w/out all the info needed...
        }

        final Action action =
                isDelete ? Action.DELETE : Action.ADD;

        // Keep cache for finding router's mac from network uuid
        //
        if (isDelete) {
            networkId2MacCache.remove(neutronNetwork.getNetworkUUID());
        } else {
            networkId2MacCache.put(neutronNetwork.getNetworkUUID(), macAddress);
        }

        List<Node> nodes = connectionService.getNodes();
        for (Node node : nodes) {
            final Long dpid = getDpid(node);
            final Action actionForNode =
                    tenantNetworkManager.isTenantNetworkPresentInNode(node, providerSegmentationId) ?
                    action : Action.DELETE;

            for (Neutron_IPs neutronIP : ipList) {
                final String ipStr = neutronIP.getIpAddress();
                if (ipStr.isEmpty()) continue;
                programRouterInterfaceStage1(node, dpid, providerSegmentationId, macAddress, ipStr, mask, actionForNode);
                programStaticArpStage1(node, dpid, providerSegmentationId, macAddress, ipStr, actionForNode);
            }

            // Compute action to be programmed. In the case of rewrite exclusions, we must never program rules
            // for the external neutron networks.
            //
            {
                final Action actionForRewriteExclusion =
                        isExternal ? Action.DELETE : actionForNode;
                programIpRewriteExclusionStage1(node, dpid, providerSegmentationId, cidr, actionForRewriteExclusion);
            }

            // Default route. For non-external subnets, make sure that there is none configured.
            //
            if (gatewayIp != null && !gatewayIp.isEmpty()) {
                final Action actionForNodeDefaultRoute =
                        isExternal ? actionForNode : Action.DELETE;
                final String defaultGatewayMacAddress = "00:01:02:03:04:05";  // FIXME!
                programDefaultRouteStage1(node, dpid, providerSegmentationId, defaultGatewayMacAddress, gatewayIp,
                                          actionForNodeDefaultRoute);
            }
        }
    }

    private void programRouterInterfaceStage1(Node node, Long dpid, String providerSegmentationId,
                                              String macAddress, String ipStr, int mask,
                                              Action actionForNode) {
        // Based on the local cache, figure out whether programming needs to occur. To do this, we
        // will look at desired action for node.
        //
        final String cacheKey = node.toString() + ":" + providerSegmentationId + ":" +
                                ipStr + "/" + Integer.toString(mask);
        final Boolean isProgrammed = routerInterfacesCache.contains(cacheKey);

        if (actionForNode == Action.DELETE && isProgrammed == Boolean.FALSE) return;
        if (actionForNode == Action.ADD && isProgrammed == Boolean.TRUE) return;

        Status status = this.programRouterInterfaceStage2(node, dpid, providerSegmentationId,
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

    private Status programRouterInterfaceStage2(Node node, Long dpid, String providerSegmentationId,
                                                String macAddress,
                                                String address, int mask,
                                                Action actionForNode) {
        Status status;
        try {
            InetAddress inetAddress = InetAddress.getByName(address);
            status = routingProvider == null ?
                     new Status(StatusCode.SUCCESS) :
                     routingProvider.programRouterInterface(node, dpid, providerSegmentationId,
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

        if (actionForNode == Action.DELETE && isProgrammed == Boolean.FALSE) return;
        if (actionForNode == Action.ADD && isProgrammed == Boolean.TRUE) return;

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

    /* ToDo: IP Rewrites have been broken in to two tables
       As such we need to modify the interfaces to program in to the correct tables
     */
    private void programIpRewriteExclusionStage1(Node node, Long dpid, String providerSegmentationId,
                                                 String cidr,
                                                 Action actionForRewriteExclusion) {
        // Based on the local cache, figure out whether programming needs to occur. To do this, we
        // will look at desired action for node.
        //
        final String cacheKey = node.toString() + ":" + providerSegmentationId + ":" + cidr;
        final Boolean isProgrammed = ipRewriteExclusionCache.contains(cacheKey);

        if (actionForRewriteExclusion == Action.DELETE && isProgrammed == Boolean.FALSE) return;
        if (actionForRewriteExclusion == Action.ADD && isProgrammed == Boolean.TRUE) return;

        Status status = this.programIpRewriteExclusionStage2(node, dpid, providerSegmentationId, cidr,
                                                             actionForRewriteExclusion);
        if (status.isSuccess()) {
            // Update cache
            if (actionForRewriteExclusion == Action.ADD) {
                ipRewriteExclusionCache.add(cacheKey);
            } else {
                ipRewriteExclusionCache.remove(cacheKey);
            }
        }
    }

    private Status programIpRewriteExclusionStage2(Node node, Long dpid, String providerSegmentationId, String cidr,
                                                   Action actionForNode) {
        Status status = inboundNatProvider == null ?
                        new Status(StatusCode.SUCCESS) :
                        inboundNatProvider.programIpRewriteExclusion(node, dpid, providerSegmentationId, cidr, actionForNode);
        if (status.isSuccess()) {
            logger.debug("IpRewriteExclusion {} for cidr:{} node:{} action:{}",
                         inboundNatProvider == null ? "skipped" : "programmed",
                         cidr, node, actionForNode);
        } else {
            logger.error("IpRewriteExclusion failed for cidr:{} node:{} action:{} status:{}",
                         cidr, node, actionForNode, status);
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

        if (actionForNodeDefaultRoute == Action.DELETE && isProgrammed == Boolean.FALSE) return;
        if (actionForNodeDefaultRoute == Action.ADD && isProgrammed == Boolean.TRUE) return;

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
        final NeutronNetwork neutronNetwork = neutronNetworkCache.getNetwork(networkUUID);
        final String providerSegmentationId = neutronNetwork != null ?
                                              neutronNetwork.getProviderSegmentationID() : null;
        final String routerMacAddress = networkId2MacCache.get(networkUUID);
        final String fixedIPAddress = neutronFloatingIP.getFixedIPAddress();
        final String floatingIpAddress = neutronFloatingIP.getFloatingIPAddress();

        if (providerSegmentationId == null || providerSegmentationId.isEmpty() ||
            routerMacAddress == null || routerMacAddress.isEmpty() ||
            fixedIPAddress == null || fixedIPAddress.isEmpty() ||
            floatingIpAddress == null || floatingIpAddress.isEmpty()) {
            return;  // done: go no further w/out all the info needed...
        }

        final Action action = isDelete ? Action.DELETE : Action.ADD;
        List<Node> nodes = connectionService.getNodes();
        for (Node node : nodes) {
            final Long dpid = getDpid(node);
            final Action actionForNode =
                    tenantNetworkManager.isTenantNetworkPresentInNode(node, providerSegmentationId) ?
                    action : Action.DELETE;

            // Rewrite from float to fixed and vice-versa
            //
            programIpRewriteStage1(node, dpid, providerSegmentationId, fixedIPAddress, floatingIpAddress, actionForNode);
            programIpRewriteStage1(node, dpid, providerSegmentationId, floatingIpAddress, fixedIPAddress, actionForNode);

            // Respond to arps for the floating ip address
            //
            programStaticArpStage1(node, dpid, providerSegmentationId, routerMacAddress, floatingIpAddress,
                                   actionForNode);
        }
    }

    private void programIpRewriteStage1(Node node, Long dpid, String providerSegmentationId,
                                        String matchAddress, String rewriteAddress,
                                        Action actionForNode) {
        // Based on the local cache, figure out whether programming needs to occur. To do this, we
        // will look at desired action for node.
        //
        final String cacheKey = node.toString() + ":" + providerSegmentationId + ":" +
                                matchAddress + ":" + rewriteAddress;
        final Boolean isProgrammed = ipRewriteCache.contains(cacheKey);

        if (actionForNode == Action.DELETE && isProgrammed == Boolean.FALSE) return;
        if (actionForNode == Action.ADD && isProgrammed == Boolean.TRUE) return;

        Status status = this.programIpRewriteStage2(node, dpid, providerSegmentationId,
                                                    matchAddress, rewriteAddress, actionForNode);
        if (status.isSuccess()) {
            // Update cache
            if (actionForNode == Action.ADD) {
                ipRewriteCache.add(cacheKey);
            } else {
                ipRewriteCache.remove(cacheKey);
            }
        }
    }

    private Status programIpRewriteStage2(Node node, Long dpid, String providerSegmentationId,
                                          String matchAddress, String rewriteAddress,
                                          Action actionForNode) {
        Status status;
        try {
            InetAddress inetMatchAddress = InetAddress.getByName(matchAddress);
            InetAddress inetRewriteAddress = InetAddress.getByName(rewriteAddress);
            status = inboundNatProvider == null ?
                     new Status(StatusCode.SUCCESS) :
                     inboundNatProvider.programIpRewriteRule(node, dpid, providerSegmentationId,
                                                             inetMatchAddress, inetRewriteAddress, actionForNode);
        } catch (UnknownHostException e) {
            status = new Status(StatusCode.BADREQUEST);
        }

        if (status.isSuccess()) {
            logger.debug("ProgramIpRewrite {} for match:{} rewrite:{} node:{} action:{}",
                         inboundNatProvider == null ? "skipped" : "programmed",
                         matchAddress, rewriteAddress, node, actionForNode);
        } else {
            logger.error("ProgramIpRewrite failed for match:{} rewrite:{} node:{} action:{} status:{}",
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
            Map<String, Row>
                    bridgeTable = ovsdbConfigurationService.getRows(node, ovsdbConfigurationService.getTableName(node, Bridge.class));
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

/*
JUNK

        // NeutronPort neutronPort = neutronPortCache.getPort(neutronRouterInterface.getPortUUID());
        NeutronSubnet subnet = neutronSubnetCache.getSubnet(neutronRouterInterface.getSubnetUUID());
        NeutronNetwork neutronNetwork = neutronNetworkCache.getNetwork(subnet.getNetworkUUID());
        String providerSegmentationId = neutronNetwork.getProviderSegmentationID();
        Boolean isExternal = neutronNetwork.getRouterExternal();
        String cidr = subnet.getCidr();

        List<Node> nodes = connectionService.getNodes();
        for (Node node : nodes) {
            if (tenantNetworkManager.isTenantNetworkPresentInNode(node, providerSegmentationId)) {
                Long dpid = getDpid(node);
                Status status = multiTenantRouterForwardingProvider
                        .programIpRewriteExclusion(node, dpid, providerSegmentationId, cidr, action);
            }
        }
*/
