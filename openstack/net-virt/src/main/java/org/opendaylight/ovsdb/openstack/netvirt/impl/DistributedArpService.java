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
    // The implementation for each of these services is resolved by the OSGi Service Manager
    private volatile ConfigurationService configurationService;
    private volatile TenantNetworkManager tenantNetworkManager;
    private volatile NodeCacheManager nodeCacheManager;
    private volatile INeutronNetworkCRUD neutronNetworkCache;
    private volatile INeutronPortCRUD neutronPortCache;
    private volatile ArpProvider arpProvider;

    private Southbound southbound;
    private Boolean flgDistributedARPEnabled = true;

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
    public void handleArpPortEvent(NeutronPort neutronPort, Action action) {
        LOG.debug("neutronPort Event {} action event {} ", neutronPort, action);
        if (action == Action.DELETE) {
            this.handleNeutornPortForArp(neutronPort, action);
        } else {
            for (NeutronPort neutronPort1 : neutronPortCache.getAllPorts()) {
               this.handleNeutornPortForArp(neutronPort1, action);
            }
        }
    }

     /**
     * Arp rules are added/removed based on neutron port event
     *
     */
    boolean programStaticArpStage1(Long dpid, String segOrOfPort,
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

        Status status = this.programStaticArpStage2(dpid, segOrOfPort, macAddress, ipStr, action);
        return status.isSuccess();
    }

     /**
     * Arp rules are programmed by invoke arpProvider
     *
     */
    private Status programStaticArpStage2(Long dpid,
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
            LOG.debug("ProgramStaticArp {} for mac:{} addr:{} dpid:{} segOrOfPort:{} action:{}",
                         arpProvider == null ? "skipped" : "programmed",
                         macAddress, address, dpid, segOrOfPort, action);
        } else {
            LOG.error("ProgramStaticArp failed for mac:{} addr:{} dpid:{} segOrOfPort:{} action:{} status:{}",
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
    private void handleNeutornPortForArp(NeutronPort neutronPort, Action action) {

        final String networkUUID = neutronPort.getNetworkUUID();
        final NeutronNetwork neutronNetwork = neutronNetworkCache.getNetwork(networkUUID);
        final String providerSegmentationId = neutronNetwork != null ?
                                              neutronNetwork.getProviderSegmentationID() : null;
        final String tenantMac = neutronPort.getMacAddress();
        if (providerSegmentationId == null || providerSegmentationId.isEmpty() ||
            tenantMac == null || tenantMac.isEmpty()) {
            // done: go no further w/out all the info needed...
            return;
        }

        final boolean isDelete = action == Action.DELETE;
        final Action action1 = isDelete ? Action.DELETE : Action.ADD;


        List<Node> nodes = nodeCacheManager.getBridgeNodes();
        if (nodes.isEmpty()) {
            LOG.trace("updateL3ForNeutronPort has no nodes to work with");
        }
        for (Node node : nodes) {
            final Long dpid = getDpidForArpIntegrationBridge(node);
            if (dpid == null) {
                continue;
            }
            if (neutronPort.getFixedIPs() == null) {
                continue;
            }
            for (Neutron_IPs neutronIP : neutronPort.getFixedIPs()) {
                final String tenantIpStr = neutronIP.getIpAddress();
                if (tenantIpStr.isEmpty()) {
                    continue;
                }
                // Configure distributed ARP responder
                if (flgDistributedARPEnabled) {
                    // Arp rule is only needed when segmentation exists in the given node (bug 4752).
                    boolean arpNeeded = tenantNetworkManager.isTenantNetworkPresentInNode(node, providerSegmentationId);
                    final Action actionForNode = arpNeeded ? action1 : Action.DELETE;
                    programStaticArpStage1(dpid, providerSegmentationId, tenantMac, tenantIpStr, actionForNode);
                }
            }
        }
    }

    /**
     * Check if node is integration bridge, then return its datapathID.
     * @param bridgeNode An instance of Node object.
     */
    private Long getDpidForArpIntegrationBridge(Node node) {
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
    public void handleArpInterfaceEvent(final Node bridgeNode, final OvsdbTerminationPointAugmentation intf,
                                     final NeutronNetwork neutronNetwork, Action action) {
        LOG.debug("southbound interface {} node:{} interface:{}, neutronNetwork:{}",
                     action, bridgeNode.getNodeId().getValue(), intf.getName(), neutronNetwork);
        final NeutronPort neutronPort = tenantNetworkManager.getTenantPort(intf);
        if (neutronPort != null) {
            this.handleArpPortEvent(neutronPort, action);
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
