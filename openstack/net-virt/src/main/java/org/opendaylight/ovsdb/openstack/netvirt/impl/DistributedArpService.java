/*
 * Copyright (c) 2016 NEC Corporation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.impl;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;

import com.google.common.base.Preconditions;

import org.opendaylight.ovsdb.openstack.netvirt.api.Action;
import org.opendaylight.ovsdb.openstack.netvirt.api.ArpProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.Southbound;
import org.opendaylight.ovsdb.openstack.netvirt.api.Status;
import org.opendaylight.ovsdb.openstack.netvirt.api.StatusCode;
import org.opendaylight.ovsdb.openstack.netvirt.ConfigInterface;
import org.opendaylight.ovsdb.openstack.netvirt.api.ConfigurationService;
import org.opendaylight.ovsdb.openstack.netvirt.api.TenantNetworkManager;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronNetwork;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronPort;
import org.opendaylight.ovsdb.openstack.netvirt.translator.crud.INeutronNetworkCRUD;
import org.opendaylight.ovsdb.openstack.netvirt.translator.crud.INeutronPortCRUD;
import org.opendaylight.ovsdb.openstack.netvirt.translator.Neutron_IPs;
import org.opendaylight.ovsdb.openstack.netvirt.api.NodeCacheManager;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ARP flows programmed for Neutron port.
 */
public class DistributedArpService implements ConfigInterface {

    private static final Logger LOG = LoggerFactory.getLogger(DistributedArpService.class);
    private static final String DHCP_DEVICE_OWNER = "network:dhcp";
    private static final String ROUTER_INTERFACE_DEVICE_OWNER = "network:router_interface";
    // The implementation for each of these services is resolved by the OSGi Service Manager
    private volatile ConfigurationService configurationService;
    private volatile TenantNetworkManager tenantNetworkManager;
    private volatile NodeCacheManager nodeCacheManager;
    private volatile INeutronNetworkCRUD neutronNetworkCache;
    private volatile INeutronPortCRUD neutronPortCache;
    private volatile ArpProvider arpProvider;
    private volatile NeutronL3Adapter neutronL3Adapter;

    private Southbound southbound;
    private Boolean flgDistributedARPEnabled = true;

    private HashMap<String, List<Neutron_IPs>> dhcpPortIpCache = new HashMap();

    private void initMembers() {
        Preconditions.checkNotNull(configurationService);
        if (configurationService.isDistributedArpDisabled()) {
            this.flgDistributedARPEnabled = false;
            LOG.debug("Distributed ARP responder is disabled");
        } else {
            LOG.debug("Distributed ARP responder is enabled");
        }
    }

     /**
     * Process the port event to write Arp rules for neutron ports.
     *
     * @param action the {@link org.opendaylight.ovsdb.openstack.netvirt.api.Action} action to be handled.
     * @param neutronPort An instance of NeutronPort object.
     */
    public void handlePortEvent(NeutronPort neutronPort, Action action) {
        LOG.debug("neutronPort Event {} action event {} ", neutronPort, action);
        if (action == Action.DELETE) {
            this.handleNeutronPortForArp(neutronPort, action);
        } else {
            for (NeutronPort neutronPort1 : neutronPortCache.getAllPorts()) {
               this.handleNeutronPortForArp(neutronPort1, action);
            }
        }
    }

     /**
     * Arp rules are added/removed based on neutron port event
     *
     */
    boolean programStaticRuleStage1(Long dpid, String segOrOfPort,
                                           String macAddress, String ipStr,
                                           Action action) {
        if (action == Action.DELETE ) {
            LOG.trace("Deleting Flow : programStaticArpStage1 dpid {} segOrOfPort {} mac {} ip {} action {}",
                         dpid, segOrOfPort, macAddress, ipStr, action);
        }
        if (action == Action.ADD) {
            LOG.trace("Adding Flow : programStaticArpStage1 dpid {} segOrOfPort {} mac {} ip {} action {}",
                         dpid, segOrOfPort, macAddress, ipStr, action);
        }

        Status status = this.programStaticRuleStage2(dpid, segOrOfPort, macAddress, ipStr, action);
        return status.isSuccess();
    }

     /**
     * Arp rules are programmed by invoke arpProvider
     *
     */
    private Status programStaticRuleStage2(Long dpid,
                                          String segOrOfPort,
                                          String macAddress,
                                          String address,
                                          Action action) {
        Status status;
        try {
            InetAddress inetAddress = InetAddress.getByName(address);
            status = arpProvider == null ?
                     new Status(StatusCode.SUCCESS) :
                     arpProvider.programStaticArpEntry(dpid, segOrOfPort,
                                                       macAddress, inetAddress, action);
        } catch (UnknownHostException e) {
            status = new Status(StatusCode.BADREQUEST);
        }

        if (status.isSuccess()) {
            LOG.debug("programStaticRuleStage2 {} for mac:{} addr:{} dpid:{} segOrOfPort:{} action:{}",
                         arpProvider == null ? "skipped" : "programmed",
                         macAddress, address, dpid, segOrOfPort, action);
        } else {
            LOG.error("programStaticRuleStage2 failed for mac:{} addr:{} dpid:{} segOrOfPort:{} action:{} status:{}",
                         macAddress, address, dpid, segOrOfPort, action, status);
        }
        return status;
    }

    /**
     * Write Arp rules based on event for neutron port.
     *
     * @param action the {@link org.opendaylight.ovsdb.openstack.netvirt.api.Action} action to be handled.
     * @param neutronPort An instance of NeutronPort object.
     */
    private void handleNeutronPortForArp(NeutronPort neutronPort, Action action) {
        if (!flgDistributedARPEnabled) {
            return;
        }

        //treat UPDATE as ADD
        final Action actionToPerform = action == Action.DELETE ? Action.DELETE : Action.ADD;

        final String networkUUID = neutronPort.getNetworkUUID();
        NeutronNetwork neutronNetwork = neutronNetworkCache.getNetwork(networkUUID);
        if (null == neutronNetwork) {
            neutronNetwork = neutronL3Adapter.getNetworkFromCleanupCache(networkUUID);
        }
        final String providerSegmentationId = neutronNetwork != null ?
                                              neutronNetwork.getProviderSegmentationID() : null;
        final String macAddress = neutronPort.getMacAddress();
        if (providerSegmentationId == null || providerSegmentationId.isEmpty() ||
            macAddress == null || macAddress.isEmpty()) {
            // done: go no further w/out all the info needed...
            return;
        }

        List<Node> nodes = nodeCacheManager.getBridgeNodes();
        if (nodes.isEmpty()) {
            LOG.trace("updateL3ForNeutronPort has no nodes to work with");
            //Do not exit, we still may need to clean up this entry from the dhcpPortToIpCache
        }

        //Neutron removes the DHCP port's IP before deleting it. As such,
        //when it comes time to delete the port, the ARP rule can not
        //be removed because we simply don't know the IP. To mitigate this,
        //we cache the dhcp ports IPs (BUG 5408).
        String owner = neutronPort.getDeviceOwner();
        boolean isDhcpPort = owner != null && owner.equals(DHCP_DEVICE_OWNER);
        List<Neutron_IPs> fixedIps = neutronPort.getFixedIPs();
        if((null == fixedIps || fixedIps.isEmpty())
                        && actionToPerform == Action.DELETE && isDhcpPort){
            fixedIps = dhcpPortIpCache.get(neutronPort.getPortUUID());
            if(fixedIps == null) {
                return;
            }
        }

        for (Node node : nodes) {
            // Arp rule is only needed when segmentation exists in the given node (bug 4752)
            // or in case the port is a router interface
            boolean isRouterInterface = owner != null && owner.equals(ROUTER_INTERFACE_DEVICE_OWNER);
            boolean arpNeeded = isRouterInterface ||
                    tenantNetworkManager.isTenantNetworkPresentInNode(node, providerSegmentationId);
            final Action actionForNode = arpNeeded ? actionToPerform : Action.DELETE;

            final Long dpid = getDatapathIdIntegrationBridge(node);
            if (dpid == null) {
                continue;
            }

            for (Neutron_IPs neutronIP : fixedIps) {
                final String ipAddress = neutronIP.getIpAddress();
                if (ipAddress.isEmpty()) {
                    continue;
                }

                programStaticRuleStage1(dpid, providerSegmentationId, macAddress, ipAddress, actionForNode);
            }
        }

        //use action instead of actionToPerform - only write to the cache when the port is created
        if(isDhcpPort && action == Action.ADD){
            dhcpPortIpCache.put(neutronPort.getPortUUID(), fixedIps);
        } else if (isDhcpPort && action == Action.DELETE) {
            dhcpPortIpCache.remove(neutronPort.getPortUUID());
        }
    }

    /**
     * Check if node is integration bridge, then return its datapathID.
     * @param node An instance of Node object.
     */
    private Long getDatapathIdIntegrationBridge(Node node) {
        if (southbound.getBridge(node, configurationService.getIntegrationBridgeName()) != null) {
            return southbound.getDataPathId(node);
        }
        return null;
    }

    /**
     * Process the event.
     *
     * @param bridgeNode An instance of Node object.
     * @param intf An {@link org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105
     * .OvsdbTerminationPointAugmentation} instance of OvsdbTerminationPointAugmentation object.
     * @param neutronNetwork An {@link org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronNetwork} instance of NeutronNetwork
     * object.
     * @param action the {@link org.opendaylight.ovsdb.openstack.netvirt.api.Action} action to be handled.
     */
    public void processInterfaceEvent(final Node bridgeNode, final OvsdbTerminationPointAugmentation intf,
                                     final NeutronNetwork neutronNetwork, Action action) {
        LOG.debug("southbound interface {} node:{} interface:{}, neutronNetwork:{}",
                     action, bridgeNode.getNodeId().getValue(), intf.getName(), neutronNetwork);
        final NeutronPort neutronPort = tenantNetworkManager.getTenantPort(intf);
        if (neutronPort != null) {
            this.handlePortEvent(neutronPort, action);
        }
    }

    @Override
    public void setDependencies(ServiceReference serviceReference) {
        tenantNetworkManager =
                (TenantNetworkManager) ServiceHelper.getGlobalInstance(TenantNetworkManager.class, this);
        configurationService =
                (ConfigurationService) ServiceHelper.getGlobalInstance(ConfigurationService.class, this);
        arpProvider =
                (ArpProvider) ServiceHelper.getGlobalInstance(ArpProvider.class, this);
        nodeCacheManager =
                (NodeCacheManager) ServiceHelper.getGlobalInstance(NodeCacheManager.class, this);
        southbound =
                (Southbound) ServiceHelper.getGlobalInstance(Southbound.class, this);
        neutronL3Adapter =
                (NeutronL3Adapter) ServiceHelper.getGlobalInstance(NeutronL3Adapter.class, this);
        initMembers();
    }

    @Override
    public void setDependencies(Object impl) {
        if (impl instanceof INeutronNetworkCRUD) {
            neutronNetworkCache = (INeutronNetworkCRUD)impl;
        } else if (impl instanceof INeutronPortCRUD) {
            neutronPortCache = (INeutronPortCRUD)impl;
        } else if (impl instanceof ArpProvider) {
            arpProvider = (ArpProvider)impl;
        }
    }

}
