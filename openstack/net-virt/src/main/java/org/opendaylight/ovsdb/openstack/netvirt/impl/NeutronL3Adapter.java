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
import org.opendaylight.ovsdb.openstack.netvirt.AbstractEvent;
import org.opendaylight.ovsdb.openstack.netvirt.NorthboundEvent;
import org.opendaylight.ovsdb.openstack.netvirt.api.MultiTenantAwareRouter;
import org.opendaylight.ovsdb.openstack.netvirt.api.MultiTenantRouterForwardingProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.NetworkingProviderManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.TenantNetworkManager;
import org.opendaylight.ovsdb.plugin.api.OvsdbConfigurationService;
import org.opendaylight.ovsdb.plugin.api.OvsdbConnectionService;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.Interface;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
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
    private volatile MultiTenantRouterForwardingProvider multiTenantRouterForwardingProvider;

    private Set<String> ipRewriteExclusionCache;
    private Set<String> routerInterfacesCache;
    private Set<String> staticArpEntryCache;
    private Set<String> defaultRouteCache;

    void init() {
        this.ipRewriteExclusionCache = new HashSet<>();
        this.routerInterfacesCache = new HashSet<>();
        this.staticArpEntryCache = new HashSet<>();
        this.defaultRouteCache = new HashSet<>();
    }


    //
    // Callbacks from OVSDB's northbound handlers
    //

    public void handleNeutronSubnetEvent(final NeutronSubnet subnet, NorthboundEvent.Action action) {
        logger.debug("Neutron subnet {} event : {}", action, subnet.toString());

        // TODO
    }

    public void handleNeutronPortEvent(final NeutronPort neutronPort, NorthboundEvent.Action action) {
        logger.debug("Neutron port {} event : {}", action, neutronPort.toString());

        // TODO

    }

    public void handleNeutronRouterEvent(final NeutronRouter neutronRouter, NorthboundEvent.Action action) {
        logger.debug("Neutron router {} event : {}", action, neutronRouter.toString());



        // TODO

    }

    public void handleNeutronRouterInterfaceEvent(final NeutronRouter neutronRouter,
                                                  final NeutronRouter_Interface neutronRouterInterface,
                                                  NorthboundEvent.Action action) {
        logger.debug(" Router {} interface {} got event {}. Subnet {}",
                     neutronRouter.getName(),
                     neutronRouterInterface.getPortUUID(),
                     action,
                     neutronRouterInterface.getSubnetUUID());

        this.programFlowsForNeutronRouterInterface(neutronRouterInterface, action == AbstractEvent.Action.DELETE);
    }

    public void handleNeutronFloatingIPEvent(final NeutronFloatingIP neutronFloatingIP,
                                             NorthboundEvent.Action action) {
        logger.debug(" Floating IP {} {}, uuid {}", action,
                     neutronFloatingIP.getFixedIPAddress(),
                     neutronFloatingIP.getFloatingIPUUID());

        // TODO
    }

    public void handleNeutronNetworkEvent(final NeutronNetwork neutronNetwork, NorthboundEvent.Action action) {
        logger.debug("neutronNetwork {}: network: {}", action, neutronNetwork);

        // TODO
    }

    //
    // Callbacks from OVSDB's southbound handler
    //

    public void handleInterfaceEvent(final Node node, final Interface intf, NeutronNetwork neutronNetwork,
                                     AbstractEvent.Action action) {
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

        final AbstractEvent.Action action =
                isDelete ? AbstractEvent.Action.DELETE : AbstractEvent.Action.ADD;

        List<Node> nodes = connectionService.getNodes();
        for (Node node : nodes) {
            final Long dpid = getDpid(node);
            final AbstractEvent.Action actionForNode =
                    tenantNetworkManager.isTenantNetworkPresentInNode(node, providerSegmentationId) ?
                    action : AbstractEvent.Action.DELETE;

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
                final AbstractEvent.Action actionForRewriteExclusion =
                        isExternal ? AbstractEvent.Action.DELETE : actionForNode;
                programIpRewriteExclusionStage1(node, dpid, providerSegmentationId, cidr, actionForRewriteExclusion);
            }

            // Default route. For non-external subnets, make sure that there is none configured.
            //
            if (gatewayIp != null && !gatewayIp.isEmpty()) {
                final AbstractEvent.Action actionForNodeDefaultRoute =
                        isExternal ? actionForNode : AbstractEvent.Action.DELETE;
                final String defaultGatewayMacAddress = "00:01:02:03:04:05";  // FIXME!
                programDefaultRouteStage1(node, dpid, providerSegmentationId, defaultGatewayMacAddress, gatewayIp,
                                          actionForNodeDefaultRoute);
            }
        }
    }

    private void programRouterInterfaceStage1(Node node, Long dpid, String providerSegmentationId,
                                              String macAddress, String ipStr, int mask,
                                              AbstractEvent.Action actionForNode) {
        // Based on the local cache, figure out whether programming needs to occur. To do this, we
        // will look at desired action for node.
        //
        final String cacheKey = node.toString() + ":" + providerSegmentationId + ":" +
                                ipStr + "/" + Integer.toString(mask);
        final Boolean isProgrammed = routerInterfacesCache.contains(cacheKey);

        if (actionForNode == AbstractEvent.Action.DELETE && isProgrammed == Boolean.FALSE) return;
        if (actionForNode == AbstractEvent.Action.ADD && isProgrammed == Boolean.TRUE) return;

        Status status = this.programRouterInterfaceStage2(node, dpid, providerSegmentationId,
                                                          macAddress, ipStr, mask, actionForNode);
        if (status.isSuccess()) {
            // Update cache
            if (actionForNode == AbstractEvent.Action.ADD) {
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
                                                AbstractEvent.Action actionForNode) {
        Status status;
        try {
            InetAddress inetAddress = InetAddress.getByName(address);
            status = multiTenantRouterForwardingProvider == null ?
                     new Status(StatusCode.SUCCESS) :
                     multiTenantRouterForwardingProvider
                             .programRouterInterface(node, dpid, providerSegmentationId,
                                                     macAddress, inetAddress, mask, actionForNode);
        } catch (UnknownHostException e) {
            status = new Status(StatusCode.BADREQUEST);
        }

        if (status.isSuccess()) {
            logger.debug("ProgramRouterInterface {} for mac:{} addr:{}/{} node:{} action:{}",
                         multiTenantRouterForwardingProvider == null ? "skipped" : "programmed",
                         macAddress, address, mask, node, actionForNode);
        } else {
            logger.error("ProgramRouterInterface failed for mac:{} addr:{}/{} node:{} action:{} status:{}",
                         macAddress, address, mask, node, actionForNode, status);
        }
        return status;
    }

    private void programStaticArpStage1(Node node, Long dpid, String providerSegmentationId,
                                        String macAddress, String ipStr,
                                        AbstractEvent.Action actionForNode) {
        // Based on the local cache, figure out whether programming needs to occur. To do this, we
        // will look at desired action for node.
        //
        final String cacheKey = node.toString() + ":" + providerSegmentationId + ":" + ipStr;
        final Boolean isProgrammed = staticArpEntryCache.contains(cacheKey);

        if (actionForNode == AbstractEvent.Action.DELETE && isProgrammed == Boolean.FALSE) return;
        if (actionForNode == AbstractEvent.Action.ADD && isProgrammed == Boolean.TRUE) return;

        Status status = this.programStaticArpStage2(node, dpid, providerSegmentationId,
                                                    macAddress, ipStr, actionForNode);
        if (status.isSuccess()) {

            // Update cache
            if (actionForNode == AbstractEvent.Action.ADD) {
                staticArpEntryCache.add(cacheKey);
            } else {
                staticArpEntryCache.remove(cacheKey);
            }
        }
    }

    private Status programStaticArpStage2(Node node, Long dpid, String providerSegmentationId,
                                                String macAddress,
                                                String address,
                                                AbstractEvent.Action actionForNode) {
        Status status;
        try {
            InetAddress inetAddress = InetAddress.getByName(address);
            status = multiTenantRouterForwardingProvider == null ?
                     new Status(StatusCode.SUCCESS) :
                     multiTenantRouterForwardingProvider
                             .programStaticArpEntry(node, dpid, providerSegmentationId,
                                                    macAddress, inetAddress, actionForNode);
        } catch (UnknownHostException e) {
            status = new Status(StatusCode.BADREQUEST);
        }

        if (status.isSuccess()) {
            logger.debug("ProgramStaticArp {} for mac:{} addr:{} node:{} action:{}",
                         multiTenantRouterForwardingProvider == null ? "skipped" : "programmed",
                         macAddress, address, node, actionForNode);
        } else {
            logger.error("ProgramStaticArp failed for mac:{} addr:{} node:{} action:{} status:{}",
                         macAddress, address, node, actionForNode, status);
        }
        return status;
    }

    private void programIpRewriteExclusionStage1(Node node, Long dpid, String providerSegmentationId,
                                                 String cidr,
                                                 AbstractEvent.Action actionForRewriteExclusion) {
        // Based on the local cache, figure out whether programming needs to occur. To do this, we
        // will look at desired action for node.
        //
        final String cacheKey = node.toString() + ":" + providerSegmentationId + ":" + cidr;
        final Boolean isProgrammed = ipRewriteExclusionCache.contains(cacheKey);

        if (actionForRewriteExclusion == AbstractEvent.Action.DELETE && isProgrammed == Boolean.FALSE) return;
        if (actionForRewriteExclusion == AbstractEvent.Action.ADD && isProgrammed == Boolean.TRUE) return;

        Status status = this.programIpRewriteExclusionStage2(node, dpid, providerSegmentationId, cidr,
                                                             actionForRewriteExclusion);
        if (status.isSuccess()) {
            // Update cache
            if (actionForRewriteExclusion == AbstractEvent.Action.ADD) {
                ipRewriteExclusionCache.add(cacheKey);
            } else {
                ipRewriteExclusionCache.remove(cacheKey);
            }
        }
    }

    private Status programIpRewriteExclusionStage2(Node node, Long dpid, String providerSegmentationId, String cidr,
                                                   AbstractEvent.Action actionForNode) {
        Status status = multiTenantRouterForwardingProvider == null ?
                        new Status(StatusCode.SUCCESS) :
                        multiTenantRouterForwardingProvider
                                .programIpRewriteExclusion(node, dpid, providerSegmentationId, cidr, actionForNode);
        if (status.isSuccess()) {
            logger.debug("IpRewriteExclusion {} for cidr:{} node:{} action:{}",
                         multiTenantRouterForwardingProvider == null ? "skipped" : "programmed",
                         cidr, node, actionForNode);
        } else {
            logger.error("IpRewriteExclusion failed for cidr:{} node:{} action:{} status:{}",
                         cidr, node, actionForNode, status);
        }
        return status;
    }

    private void programDefaultRouteStage1(Node node, Long dpid, String providerSegmentationId,
                                           String defaultGatewayMacAddress, String gatewayIp,
                                           AbstractEvent.Action actionForNodeDefaultRoute) {
        // Based on the local cache, figure out whether programming needs to occur. To do this, we
        // will look at desired action for node.
        //
        final String cacheKey = node.toString() + ":" + providerSegmentationId + ":" + gatewayIp;
        final Boolean isProgrammed = defaultRouteCache.contains(cacheKey);

        if (actionForNodeDefaultRoute == AbstractEvent.Action.DELETE && isProgrammed == Boolean.FALSE) return;
        if (actionForNodeDefaultRoute == AbstractEvent.Action.ADD && isProgrammed == Boolean.TRUE) return;

        Status status = this.programDefaultRouteStage2(node, dpid, providerSegmentationId,
                                                       defaultGatewayMacAddress, gatewayIp, actionForNodeDefaultRoute);
        if (status.isSuccess()) {
            // Update cache
            if (actionForNodeDefaultRoute == AbstractEvent.Action.ADD) {
                defaultRouteCache.add(cacheKey);
            } else {
                defaultRouteCache.remove(cacheKey);
            }
        }
    }

    private Status programDefaultRouteStage2(Node node, Long dpid, String providerSegmentationId,
                                          String defaultGatewayMacAddress,
                                          String gatewayIp,
                                          AbstractEvent.Action actionForNodeDefaultRoute) {
        Status status;
        try {
            InetAddress inetAddress = InetAddress.getByName(gatewayIp);
            status = multiTenantRouterForwardingProvider == null ?
                     new Status(StatusCode.SUCCESS) :
                     multiTenantRouterForwardingProvider
                             .programDefaultRouteEntry(node, dpid, providerSegmentationId,
                                                       defaultGatewayMacAddress, inetAddress,
                                                       actionForNodeDefaultRoute);
        } catch (UnknownHostException e) {
            status = new Status(StatusCode.BADREQUEST);
        }

        if (status.isSuccess()) {
            logger.debug("ProgramDefaultRoute {} for mac:{} gatewayIp:{} node:{} action:{}",
                         multiTenantRouterForwardingProvider == null ? "skipped" : "programmed",
                         defaultGatewayMacAddress, gatewayIp, node, actionForNodeDefaultRoute);
        } else {
            logger.error("ProgramDefaultRoute failed for mac:{} gatewayIp:{} node:{} action:{} status:{}",
                         defaultGatewayMacAddress, gatewayIp, node, actionForNodeDefaultRoute, status);
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
