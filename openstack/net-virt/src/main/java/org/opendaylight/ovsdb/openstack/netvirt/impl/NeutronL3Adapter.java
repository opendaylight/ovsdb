/*
 * Copyright (c) 2014 - 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.impl;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.opendaylight.ovsdb.openstack.netvirt.AbstractEvent;
import org.opendaylight.ovsdb.openstack.netvirt.AbstractHandler;
import org.opendaylight.ovsdb.openstack.netvirt.ConfigInterface;
import org.opendaylight.ovsdb.openstack.netvirt.NeutronL3AdapterEvent;
import org.opendaylight.ovsdb.openstack.netvirt.api.Action;
import org.opendaylight.ovsdb.openstack.netvirt.api.ArpProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.ConfigurationService;
import org.opendaylight.ovsdb.openstack.netvirt.api.Constants;
import org.opendaylight.ovsdb.openstack.netvirt.api.EventDispatcher;
import org.opendaylight.ovsdb.openstack.netvirt.api.GatewayMacResolver;
import org.opendaylight.ovsdb.openstack.netvirt.api.GatewayMacResolverListener;
import org.opendaylight.ovsdb.openstack.netvirt.api.IcmpEchoProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.InboundNatProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.L3ForwardingProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.NodeCacheManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.OutboundNatProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.RoutingProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.SecurityServicesManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.Southbound;
import org.opendaylight.ovsdb.openstack.netvirt.api.Status;
import org.opendaylight.ovsdb.openstack.netvirt.api.StatusCode;
import org.opendaylight.ovsdb.openstack.netvirt.api.TenantNetworkManager;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronFloatingIP;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronNetwork;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronPort;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronRouter;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronRouter_Interface;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronSecurityGroup;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronSubnet;
import org.opendaylight.ovsdb.openstack.netvirt.translator.Neutron_IPs;
import org.opendaylight.ovsdb.openstack.netvirt.translator.crud.INeutronFloatingIPCRUD;
import org.opendaylight.ovsdb.openstack.netvirt.translator.crud.INeutronNetworkCRUD;
import org.opendaylight.ovsdb.openstack.netvirt.translator.crud.INeutronPortCRUD;
import org.opendaylight.ovsdb.openstack.netvirt.translator.crud.INeutronSubnetCRUD;
import org.opendaylight.ovsdb.openstack.netvirt.translator.iaware.impl.NeutronIAwareUtil;
import org.opendaylight.ovsdb.utils.neutron.utils.NeutronModelsDataStoreHelper;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.l3.rev150712.routers.attributes.Routers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.l3.rev150712.routers.attributes.routers.Router;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.ports.attributes.Ports;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.ports.attributes.ports.Port;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Neutron L3 Adapter implements a hub-like adapter for the various Neutron events. Based on
 * these events, the abstract router callbacks can be generated to the multi-tenant aware router,
 * as well as the multi-tenant router forwarding provider.
 */
public class NeutronL3Adapter extends AbstractHandler implements GatewayMacResolverListener, ConfigInterface {
    private static final Logger LOG = LoggerFactory.getLogger(NeutronL3Adapter.class);

    // The implementation for each of these services is resolved by the OSGi Service Manager
    private volatile ConfigurationService configurationService;
    private volatile TenantNetworkManager tenantNetworkManager;
    private volatile NodeCacheManager nodeCacheManager;
    private volatile INeutronNetworkCRUD neutronNetworkCache;
    private volatile INeutronSubnetCRUD neutronSubnetCache;
    private volatile INeutronPortCRUD neutronPortCache;
    private volatile INeutronFloatingIPCRUD neutronFloatingIpCache;
    private volatile L3ForwardingProvider l3ForwardingProvider;
    private volatile InboundNatProvider inboundNatProvider;
    private volatile OutboundNatProvider outboundNatProvider;
    private volatile ArpProvider arpProvider;
    private volatile RoutingProvider routingProvider;
    private volatile GatewayMacResolver gatewayMacResolver;
    private volatile SecurityServicesManager securityServicesManager;
    private volatile IcmpEchoProvider icmpEchoProvider;

    private class FloatIpData {
        // br-int of node where floating ip is associated with tenant port
        private final Long dpid;
        // patch port in br-int used to reach br-ex
        private final Long ofPort;
        // segmentation id of the net where fixed ip is instantiated
        private final String segId;
        // mac address assigned to neutron port of floating ip
        private final String macAddress;
        private final String floatingIpAddress;
        // ip address given to tenant vm
        private final String fixedIpAddress;
        private final String neutronRouterMac;

        FloatIpData(final Long dpid, final Long ofPort, final String segId, final String macAddress,
                    final String floatingIpAddress, final String fixedIpAddress, final String neutronRouterMac) {
            this.dpid = dpid;
            this.ofPort = ofPort;
            this.segId = segId;
            this.macAddress = macAddress;
            this.floatingIpAddress = floatingIpAddress;
            this.fixedIpAddress = fixedIpAddress;
            this.neutronRouterMac = neutronRouterMac;
        }
    }

    private Map<String, String> networkIdToRouterMacCache;
    private Map<String, List<Neutron_IPs>> networkIdToRouterIpListCache;
    private Map<String, NeutronRouter_Interface> subnetIdToRouterInterfaceCache;

    private Map<String, Pair<Long, Uuid>> neutronPortToDpIdCache;
    private Map<String, FloatIpData> floatIpDataMapCache;

    private String externalRouterMac;
    private Boolean enabled = false;
    private Boolean isCachePopulationDone = false;
    private Map<String, NeutronPort> portCleanupCache;
    private Map<String, NeutronNetwork> networkCleanupCache;

    private Southbound southbound;
    private DistributedArpService distributedArpService;
    private NeutronModelsDataStoreHelper neutronModelsDataStoreHelper;

    private static final String OWNER_ROUTER_INTERFACE = "network:router_interface";
    private static final String OWNER_ROUTER_INTERFACE_DISTRIBUTED = "network:router_interface_distributed";
    private static final String OWNER_ROUTER_GATEWAY = "network:router_gateway";
    private static final String OWNER_FLOATING_IP = "network:floatingip";
    private static final String DEFAULT_EXT_RTR_MAC = "00:00:5E:00:01:01";

    public NeutronL3Adapter(NeutronModelsDataStoreHelper neutronHelper) {
        LOG.info(">>>>>> NeutronL3Adapter constructor {}", this.getClass());
        this.neutronModelsDataStoreHelper = neutronHelper;
    }

    private void initL3AdapterMembers() {
        Preconditions.checkNotNull(configurationService);

        if (configurationService.isL3ForwardingEnabled()) {
            this.networkIdToRouterMacCache = new HashMap<>();
            this.networkIdToRouterIpListCache = new HashMap<>();
            this.subnetIdToRouterInterfaceCache = new HashMap<>();
            this.neutronPortToDpIdCache = new HashMap<>();
            this.floatIpDataMapCache = new HashMap<>();

            this.externalRouterMac = configurationService.getDefaultGatewayMacAddress(null);
            if (this.externalRouterMac == null) {
                this.externalRouterMac = DEFAULT_EXT_RTR_MAC;
            }
            this.enabled = true;
            LOG.info("OVSDB L3 forwarding is enabled");
        } else {
            LOG.debug("OVSDB L3 forwarding is disabled");
        }
        this.portCleanupCache = new HashMap<>();
        this.networkCleanupCache = new HashMap<>();
    }

    //
    // Callbacks from AbstractHandler
    //
    @Override
    public void processEvent(AbstractEvent abstractEvent) {
        if (!(abstractEvent instanceof NeutronL3AdapterEvent)) {
            LOG.error("Unable to process abstract event " + abstractEvent);
            return;
        }
        if (!this.enabled) {
            return;
        }

        NeutronL3AdapterEvent ev = (NeutronL3AdapterEvent) abstractEvent;
        switch (ev.getAction()) {
            case UPDATE:
                if (ev.getSubType() == NeutronL3AdapterEvent.SubType.SUBTYPE_EXTERNAL_MAC_UPDATE) {
                    updateExternalRouterMac( ev.getMacAddress().getValue() );
                } else {
                    LOG.warn("Received update for an unexpected event " + ev);
                }
                break;
            case ADD:
                // fall through...
                // break;
            case DELETE:
                // fall through...
                // break;
            default:
                LOG.warn("Unable to process event " + ev);
                break;
        }
    }

    //
    // Callbacks from GatewayMacResolverListener
    //

    @Override
    public void gatewayMacResolved(Long externalNetworkBridgeDpid, IpAddress gatewayIpAddress, MacAddress macAddress) {
        LOG.info("got gatewayMacResolved callback for ip {} on dpid {} to mac {}",
                gatewayIpAddress, externalNetworkBridgeDpid, macAddress);
        if (!this.enabled) {
            return;
        }

        if (macAddress == null || macAddress.getValue() == null) {
            // TODO: handle cases when mac is null
            return;
        }

        //
        // Enqueue event so update is handled by adapter's thread
        //
        enqueueEvent( new NeutronL3AdapterEvent(externalNetworkBridgeDpid, gatewayIpAddress, macAddress) );
    }

    private void populateL3ForwardingCaches() {
        if (!this.enabled) {
            return;
        }
        if(this.isCachePopulationDone || this.neutronFloatingIpCache == null
                || this.neutronPortCache == null ||this.neutronNetworkCache == null) {
            return;
        }
        this.isCachePopulationDone = true;
        LOG.debug("Populating NetVirt L3 caches from data store configuration");
        Routers routers = this.neutronModelsDataStoreHelper.readAllNeutronRouters();
        Ports ports = this.neutronModelsDataStoreHelper.readAllNeutronPorts();
        if(routers != null && routers.getRouter() != null && ports != null) {
            LOG.debug("L3 Cache Population : {} Neutron router present in data store",routers.getRouter().size());
            for( Router router : routers.getRouter()) {
                LOG.debug("L3 Cache Population : Populate caches for router {}",router);
                if(!ports.getPort().isEmpty()) {
                    for( Port port : ports.getPort()) {
                        if (port.getDeviceId().equals(router.getUuid().getValue()) &&
                                port.getDeviceOwner().equals(OWNER_ROUTER_INTERFACE)) {
                            LOG.debug("L3 Cache Population : Router interface {} found.",port);
                            networkIdToRouterMacCache.put(port.getNetworkId().getValue()
                                    , port.getMacAddress());

                            networkIdToRouterIpListCache.put(port.getNetworkId().getValue(),
                                    NeutronIAwareUtil.convertMDSalIpToNeutronIp(port.getFixedIps()));
                            subnetIdToRouterInterfaceCache.put(port.getFixedIps().get(0).getSubnetId().getValue(),
                                    NeutronIAwareUtil.convertMDSalInterfaceToNeutronRouterInterface(port));
                        }
                    }
                }else {
                    LOG.warn("L3 Cache Population :Did not find any port information " +
                            "in config Data Store for router {}",router);
                }
            }
        }
        LOG.debug("NetVirt L3 caches population is done");
    }

    private Pair<Long, Uuid> getDpIdOfNeutronPort(String neutronTenantPortUuid) {
        if(neutronPortToDpIdCache.get(neutronTenantPortUuid) == null) {
            List<Node> bridges = this.southbound.readOvsdbTopologyBridgeNodes();
            LOG.debug("getDpIdOfNeutronPort : {} bridges present in ovsdb topology",bridges.size());
            for(Node bridge : bridges) {
                List<OvsdbTerminationPointAugmentation> interfaces
                        = southbound.extractTerminationPointAugmentations(bridge);
                if(interfaces != null && !interfaces.isEmpty()) {
                    LOG.debug("getDpIdOfNeutronPort : {} termination point present on bridge {}",
                            interfaces.size(), bridge.getNodeId());
                    for (OvsdbTerminationPointAugmentation intf : interfaces) {
                        NeutronPort neutronPort = tenantNetworkManager.getTenantPort(intf);
                        if(neutronPort != null && neutronPort.getID().equals(neutronTenantPortUuid)) {
                            Long dpId = getDpidForIntegrationBridge(bridge);
                            Uuid interfaceUuid = intf.getInterfaceUuid();
                            LOG.debug("getDpIdOfNeutronPort : Found bridge {} and interface {} for the tenant neutron" +
                                    " port {}",dpId,interfaceUuid,neutronTenantPortUuid);
                            handleInterfaceEventAdd(neutronPort.getPortUUID(), dpId, interfaceUuid);
                            break;
                        }
                    }
                }
            }
        }
        return neutronPortToDpIdCache.get(neutronTenantPortUuid);
    }

    private Collection<FloatIpData> getAllFloatingIPsWithMetadata() {
        LOG.debug("getAllFloatingIPsWithMetadata : Fechting all floating Ips and it's metadata");
        List<NeutronFloatingIP> neutronFloatingIps = neutronFloatingIpCache.getAllFloatingIPs();
        if(neutronFloatingIps != null && !neutronFloatingIps.isEmpty()) {
            for (NeutronFloatingIP neutronFloatingIP : neutronFloatingIps) {
                if(!floatIpDataMapCache.containsKey(neutronFloatingIP.getID())){
                    LOG.debug("Metadata for floating ip {} is not present in the cache. " +
                            "Fetching from data store.",neutronFloatingIP.getID());
                    this.getFloatingIPWithMetadata(neutronFloatingIP.getID());
                }
            }
        }
        LOG.debug("getAllFloatingIPsWithMetadata : {} floating points found in data store",floatIpDataMapCache.size());
        return floatIpDataMapCache.values();
    }
    private FloatIpData getFloatingIPWithMetadata(String neutronFloatingId) {
        LOG.debug("getFloatingIPWithMetadata : Get Floating ip and it's meta data for neutron " +
                "floating id {} ",neutronFloatingId);
        if(floatIpDataMapCache.get(neutronFloatingId) == null) {
            NeutronFloatingIP neutronFloatingIP = neutronFloatingIpCache.getFloatingIP(neutronFloatingId);
            if (neutronFloatingIP == null) {
                LOG.error("getFloatingIPWithMetadata : Floating ip {} is missing from data store, that should not happen",neutronFloatingId);
                return null;
            }
            List<NeutronPort> neutronPorts = neutronPortCache.getAllPorts();
            NeutronPort neutronPortForFloatIp = null;
            for (NeutronPort neutronPort : neutronPorts) {
                if (neutronPort.getDeviceOwner().equals(OWNER_FLOATING_IP) &&
                        neutronPort.getDeviceID().equals(neutronFloatingIP.getID())) {
                    neutronPortForFloatIp = neutronPort;
                    break;
                }
            }

            String neutronTenantPortUuid = neutronFloatingIP.getPortUUID();
            if(neutronTenantPortUuid == null) {
                return null;
            }
            Pair<Long, Uuid> nodeIfPair = this.getDpIdOfNeutronPort(neutronTenantPortUuid);
            String floatingIpMac = neutronPortForFloatIp == null ? null : neutronPortForFloatIp.getMacAddress();
            String fixedIpAddress = neutronFloatingIP.getFixedIPAddress();
            String floatingIpAddress = neutronFloatingIP.getFloatingIPAddress();

            NeutronPort tenantNeutronPort = neutronPortCache.getPort(neutronTenantPortUuid);
            NeutronNetwork tenantNeutronNetwork = tenantNeutronPort != null ?
                    neutronNetworkCache.getNetwork(tenantNeutronPort.getNetworkUUID()) : null;
            String providerSegmentationId = tenantNeutronNetwork != null ?
                    tenantNeutronNetwork.getProviderSegmentationID() : null;
            String neutronRouterMac = tenantNeutronNetwork != null ?
                    networkIdToRouterMacCache.get(tenantNeutronNetwork.getID()) : null;

            if (nodeIfPair == null || neutronTenantPortUuid == null ||
                    providerSegmentationId == null || providerSegmentationId.isEmpty() ||
                    floatingIpMac == null || floatingIpMac.isEmpty() ||
                    neutronRouterMac == null || neutronRouterMac.isEmpty()) {
                LOG.debug("getFloatingIPWithMetadata :Floating IP {}<->{}, incomplete floatPort {} tenantPortUuid {} " +
                                "seg {} mac {} rtrMac {}",
                        fixedIpAddress,
                        floatingIpAddress,
                        neutronPortForFloatIp,
                        neutronTenantPortUuid,
                        providerSegmentationId,
                        floatingIpMac,
                        neutronRouterMac);

                return null;
            }

            // get ofport for patch port in br-int
            final Long dpId = nodeIfPair.getLeft();
            final Long ofPort = findOFPortForExtPatch(dpId);
            if (ofPort == null) {
                LOG.warn("getFloatingIPWithMetadata : Unable to locate OF port of patch port " +
                                "to connect floating ip to external bridge. dpid {}",
                        dpId);
                return null;
            }

            final FloatIpData floatIpData = new FloatIpData(dpId, ofPort, providerSegmentationId, floatingIpMac,
                    floatingIpAddress, fixedIpAddress, neutronRouterMac);
            floatIpDataMapCache.put(neutronFloatingIP.getID(), floatIpData);

        }
        return floatIpDataMapCache.get(neutronFloatingId);
    }
    /**
     * Invoked to configure the mac address for the external gateway in br-ex. ovsdb netvirt needs help in getting
     * mac for given ip in br-ex (bug 3378). For example, since ovsdb has no real arp, it needs a service in can
     * subscribe so that the mac address associated to the gateway ip address is available.
     *
     * @param externalRouterMacUpdate  The mac address to be associated to the gateway.
     */
    public void updateExternalRouterMac(final String externalRouterMacUpdate) {
        Preconditions.checkNotNull(externalRouterMacUpdate);

        flushExistingIpRewrite();
        this.externalRouterMac = externalRouterMacUpdate;
        rebuildExistingIpRewrite();
    }

    /**
     * Process the event.
     *
     * @param action the {@link org.opendaylight.ovsdb.openstack.netvirt.api.Action} action to be handled.
     * @param subnet An instance of NeutronSubnet object.
     */
    public void handleNeutronSubnetEvent(final NeutronSubnet subnet, Action action) {
        LOG.debug("Neutron subnet {} event : {}", action, subnet.toString());
        if (action == Action.ADD) {
            this.storeNetworkInCleanupCache(neutronNetworkCache.getNetwork(subnet.getNetworkUUID()));
        }
    }

    /**
     * Process the port event as a router interface event.
     * For a not delete action, since a port is only create when the tennat uses the subnet, it is required to
     * verify if all routers across all nodes have the interface for the port's subnet(s) configured.
     *
     * @param action the {@link org.opendaylight.ovsdb.openstack.netvirt.api.Action} action to be handled.
     * @param neutronPort An instance of NeutronPort object.
     */
    public void handleNeutronPortEvent(final NeutronPort neutronPort, Action action) {
        LOG.debug("Neutron port {} event : {}", action, neutronPort.toString());

        if (action == Action.UPDATE) {
            // FIXME: Bug 4971 Move cleanup cache to SG Impl
            this.updatePortInCleanupCache(neutronPort, neutronPort.getOriginalPort());
            this.processSecurityGroupUpdate(neutronPort);
        }

        if (!this.enabled) {
            return;
        }

        final boolean isDelete = action == Action.DELETE;

        if (action == Action.DELETE) {
            // Bug 5164: Cleanup Floating IP OpenFlow Rules when port is deleted.
            this.cleanupFloatingIPRules(neutronPort);
        }
        else if (action == Action.UPDATE){
            // Bug 5353: VM restart cause floatingIp flows to be removed
            this.updateFloatingIPRules(neutronPort);
        }

        if (neutronPort.getDeviceOwner().equalsIgnoreCase(OWNER_ROUTER_GATEWAY)){
            if (!isDelete) {
                LOG.info("Port {} is network router gateway interface, "
                        + "triggering gateway resolution for the attached external network", neutronPort);
                this.triggerGatewayMacResolver(neutronPort);
            }else{
                NeutronNetwork externalNetwork = neutronNetworkCache.getNetwork(neutronPort.getNetworkUUID());
                if (null == externalNetwork) {
                    externalNetwork = this.getNetworkFromCleanupCache(neutronPort.getNetworkUUID());
                }

                if (externalNetwork != null && externalNetwork.isRouterExternal()) {
                    final NeutronSubnet externalSubnet = getExternalNetworkSubnet(neutronPort);
                    // TODO support IPv6
                    if (externalSubnet != null &&
                            externalSubnet.getIpVersion() == 4) {
                        gatewayMacResolver.stopPeriodicRefresh(new Ipv4Address(externalSubnet.getGatewayIP()));
                    }
                }
            }
        }

        // Treat the port event as a router interface event if the port belongs to router. This is a
        // helper for handling cases when handleNeutronRouterInterfaceEvent is not available
        //
        if (neutronPort.getDeviceOwner().equalsIgnoreCase(OWNER_ROUTER_INTERFACE) ||
            neutronPort.getDeviceOwner().equalsIgnoreCase(OWNER_ROUTER_INTERFACE_DISTRIBUTED)) {

            if (neutronPort.getFixedIPs() != null) {
                for (Neutron_IPs neutronIP : neutronPort.getFixedIPs()) {
                    NeutronRouter_Interface neutronRouterInterface =
                        new NeutronRouter_Interface(neutronIP.getSubnetUUID(), neutronPort.getPortUUID());
                    // id of router interface to be same as subnet
                    neutronRouterInterface.setID(neutronIP.getSubnetUUID());
                    neutronRouterInterface.setTenantID(neutronPort.getTenantID());

                    this.handleNeutronRouterInterfaceEvent(null /*neutronRouter*/, neutronRouterInterface, action);
                }
            }
        } else {
            // We made it here, port is not used as a router interface. If this is not a delete action, make sure that
            // all nodes that are supposed to have a router interface for the port's subnet(s), have it configured. We
            // need to do this check here because a router interface is not added to a node until tenant becomes needed
            // there.
            //
            if (!isDelete && neutronPort.getFixedIPs() != null) {
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

    /**
     * Process the event.
     *
     * @param action the {@link org.opendaylight.ovsdb.openstack.netvirt.api.Action} action to be handled.
     * @param neutronRouter An instance of NeutronRouter object.
     */
    public void handleNeutronRouterEvent(final NeutronRouter neutronRouter, Action action) {
        LOG.debug("Neutron router {} event : {}", action, neutronRouter.toString());
    }

    /**
     * Process the event enforcing actions and verifying dependencies between all router's interface. For example,
     * delete the ports on the same subnet.
     *
     * @param action the {@link org.opendaylight.ovsdb.openstack.netvirt.api.Action} action to be handled.
     * @param neutronRouter An instance of NeutronRouter object.
     * @param neutronRouterInterface An instance of NeutronRouter_Interface object.
     */
    public void handleNeutronRouterInterfaceEvent(final NeutronRouter neutronRouter,
                                                  final NeutronRouter_Interface neutronRouterInterface,
                                                  Action action) {
        LOG.debug("Router interface {} got event {}. Subnet {}",
                     neutronRouterInterface.getPortUUID(),
                     action,
                     neutronRouterInterface.getSubnetUUID());
        if (!this.enabled) {
            return;
        }

        final boolean isDelete = action == Action.DELETE;

        this.programFlowsForNeutronRouterInterface(neutronRouterInterface, isDelete);

        // As neutron router interface is added/removed, we need to iterate through all the neutron ports and
        // see if they are affected by l3
        //
        for (NeutronPort neutronPort : neutronPortCache.getAllPorts()) {
            boolean currPortShouldBeDeleted = false;
            // Note: delete in this case only applies to 1)router interface delete and 2)ports on the same subnet
            if (isDelete) {
                if (neutronPort.getFixedIPs() != null) {
                    for (Neutron_IPs neutronIP : neutronPort.getFixedIPs()) {
                        if (neutronRouterInterface.getSubnetUUID().equalsIgnoreCase(neutronIP.getSubnetUUID())) {
                            currPortShouldBeDeleted = true;
                            break;
                        }
                    }
                }
            }
            this.updateL3ForNeutronPort(neutronPort, currPortShouldBeDeleted);
        }

    }

    /**
     * Invoked when a neutron message regarding the floating ip association is sent to odl via ml2. If the action is
     * a creation, it will first add ARP rules for the given floating ip and then configure the DNAT (rewrite the
     * packets from the floating IP address to the internal fixed ip) rules on OpenFlow Table 30 and SNAT rules (other
     * way around) on OpenFlow Table 100.
     *
     * @param actionIn the {@link org.opendaylight.ovsdb.openstack.netvirt.api.Action} action to be handled.
     * @param neutronFloatingIP An {@link org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronFloatingIP} instance of NeutronFloatingIP object.
     */
    public void handleNeutronFloatingIPEvent(final NeutronFloatingIP neutronFloatingIP,
                                             Action actionIn) {
        Preconditions.checkNotNull(neutronFloatingIP);

        LOG.debug(" Floating IP {} {}<->{}, network uuid {}", actionIn,
                neutronFloatingIP.getFixedIPAddress(),
                neutronFloatingIP.getFloatingIPAddress(),
                neutronFloatingIP.getFloatingNetworkUUID());
        if (!this.enabled) {
            return;
        }

        Action action;

        // Consider action to be delete if getFixedIPAddress is null
        //
        if (neutronFloatingIP.getFixedIPAddress() == null) {
            action = Action.DELETE;
        } else {
            action = actionIn;
        }

        // this.programFlowsForFloatingIP(neutronFloatingIP, action == Action.DELETE);

        if (action != Action.DELETE) {
            // must be first, as it updates floatIpDataMapCache
            programFlowsForFloatingIPArpAdd(neutronFloatingIP);

            programFlowsForFloatingIPInbound(neutronFloatingIP, Action.ADD);
            programFlowsForFloatingIPOutbound(neutronFloatingIP, Action.ADD);
        } else {
            programFlowsForFloatingIPOutbound(neutronFloatingIP, Action.DELETE);
            programFlowsForFloatingIPInbound(neutronFloatingIP, Action.DELETE);

            // must be last, as it updates floatIpDataMapCache
            programFlowsForFloatingIPArpDelete(neutronFloatingIP.getID());
        }
    }

    /**
     * This method performs creation or deletion of in-bound rules into Table 30 for a existing available floating
     * ip, otherwise for newer one.
     */
    private void programFlowsForFloatingIPInbound(final NeutronFloatingIP neutronFloatingIP, final Action action) {
        Preconditions.checkNotNull(neutronFloatingIP);

        final FloatIpData fid = getFloatingIPWithMetadata(neutronFloatingIP.getID());
        if (fid == null) {
            LOG.trace("programFlowsForFloatingIPInboundAdd {} for {} uuid {} not in local cache",
                    action, neutronFloatingIP.getFloatingIPAddress(), neutronFloatingIP.getID());
            return;
        }
        programInboundIpRewriteStage1(fid.dpid, fid.ofPort, fid.segId, fid.floatingIpAddress, fid.fixedIpAddress,
                                      action);
    }

    /**
     * This method performs creation or deletion of out-bound rules into Table 100 for a existing available floating
     * ip, otherwise for newer one.
     */
    private void programFlowsForFloatingIPOutbound(final NeutronFloatingIP neutronFloatingIP, final Action action) {
        Preconditions.checkNotNull(neutronFloatingIP);

        final FloatIpData fid = getFloatingIPWithMetadata(neutronFloatingIP.getID());
        if (fid == null) {
            LOG.trace("programFlowsForFloatingIPOutbound {} for {} uuid {} not in local cache",
                    action, neutronFloatingIP.getFloatingIPAddress(), neutronFloatingIP.getID());
            return;
        }
        programOutboundIpRewriteStage1(fid, action);
    }

    private void flushExistingIpRewrite() {
        for (FloatIpData fid : getAllFloatingIPsWithMetadata()) {
            programOutboundIpRewriteStage1(fid, Action.DELETE);
        }
    }

    private void rebuildExistingIpRewrite() {
        for (FloatIpData fid : getAllFloatingIPsWithMetadata()) {
            programOutboundIpRewriteStage1(fid, Action.ADD);
        }
    }

    /**
     * This method creates ARP response rules into OpenFlow Table 30 for a given floating ip. In order to connect
     * to br-ex from br-int, a patch-port is used. Thus, the patch-port will be responsible to respond the ARP
     * requests.
     */
    private void programFlowsForFloatingIPArpAdd(final NeutronFloatingIP neutronFloatingIP) {
        Preconditions.checkNotNull(neutronFloatingIP);
        Preconditions.checkNotNull(neutronFloatingIP.getFixedIPAddress());
        Preconditions.checkNotNull(neutronFloatingIP.getFloatingIPAddress());

        // find bridge Node where floating ip is configured by looking up cache for its port
        final NeutronPort neutronPortForFloatIp = findNeutronPortForFloatingIp(neutronFloatingIP.getID());
        final String neutronTenantPortUuid = neutronFloatingIP.getPortUUID();
        final Pair<Long, Uuid> nodeIfPair = this.getDpIdOfNeutronPort(neutronTenantPortUuid);
        final String floatingIpMac = neutronPortForFloatIp == null ? null : neutronPortForFloatIp.getMacAddress();
        final String fixedIpAddress = neutronFloatingIP.getFixedIPAddress();
        final String floatingIpAddress = neutronFloatingIP.getFloatingIPAddress();

        final NeutronPort tenantNeutronPort = neutronPortCache.getPort(neutronTenantPortUuid);
        final NeutronNetwork tenantNeutronNetwork = tenantNeutronPort != null ?
                neutronNetworkCache.getNetwork(tenantNeutronPort.getNetworkUUID()) : null;
        final String providerSegmentationId = tenantNeutronNetwork != null ?
                tenantNeutronNetwork.getProviderSegmentationID() : null;
        final String neutronRouterMac = tenantNeutronNetwork != null ?
                networkIdToRouterMacCache.get(tenantNeutronNetwork.getID()) : null;

        if (nodeIfPair == null || neutronTenantPortUuid == null ||
                providerSegmentationId == null || providerSegmentationId.isEmpty() ||
                floatingIpMac == null || floatingIpMac.isEmpty() ||
                neutronRouterMac == null || neutronRouterMac.isEmpty()) {
            LOG.trace("Floating IP {}<->{}, incomplete floatPort {} tenantPortUuid {} seg {} mac {} rtrMac {}",
                    fixedIpAddress,
                    floatingIpAddress,
                    neutronPortForFloatIp,
                    neutronTenantPortUuid,
                    providerSegmentationId,
                    floatingIpMac,
                    neutronRouterMac);
            return;
        }

        // get ofport for patch port in br-int
        final Long dpId = nodeIfPair.getLeft();
        final Long ofPort = findOFPortForExtPatch(dpId);
        if (ofPort == null) {
            LOG.warn("Unable to locate OF port of patch port to connect floating ip to external bridge. dpid {}",
                    dpId);
            return;
        }

        // Respond to ARPs for the floating ip address by default, via the patch port that connects br-int to br-ex
        //
        if (distributedArpService.programStaticRuleStage1(dpId, encodeExcplicitOFPort(ofPort), floatingIpMac, floatingIpAddress,
                Action.ADD)) {
            final FloatIpData floatIpData = new FloatIpData(dpId, ofPort, providerSegmentationId, floatingIpMac,
                    floatingIpAddress, fixedIpAddress, neutronRouterMac);
            floatIpDataMapCache.put(neutronFloatingIP.getID(), floatIpData);
            LOG.info("Floating IP {}<->{} programmed ARP mac {} on OFport {} seg {} dpid {}",
                    neutronFloatingIP.getFixedIPAddress(), neutronFloatingIP.getFloatingIPAddress(),
                    floatingIpMac, ofPort, providerSegmentationId, dpId);
        }
    }

    private void programFlowsForFloatingIPArpDelete(final String neutronFloatingIPUuid) {
        final FloatIpData floatIpData = getFloatingIPWithMetadata(neutronFloatingIPUuid);
        if (floatIpData == null) {
            LOG.trace("programFlowsForFloatingIPArpDelete for uuid {} is not needed", neutronFloatingIPUuid);
            return;
        }

        if (distributedArpService.programStaticRuleStage1(floatIpData.dpid, encodeExcplicitOFPort(floatIpData.ofPort), floatIpData.macAddress,
                floatIpData.floatingIpAddress, Action.DELETE)) {
            floatIpDataMapCache.remove(neutronFloatingIPUuid);
            LOG.info("Floating IP {} un-programmed ARP mac {} on {} dpid {}",
                    floatIpData.floatingIpAddress, floatIpData.macAddress, floatIpData.ofPort, floatIpData.dpid);
        }
    }

    private NeutronPort findNeutronPortForFloatingIp(final String floatingIpUuid) {
        for (NeutronPort neutronPort : neutronPortCache.getAllPorts()) {
            if (neutronPort.getDeviceOwner().equals(OWNER_FLOATING_IP) &&
                    neutronPort.getDeviceID().equals(floatingIpUuid)) {
                return neutronPort;
            }
        }
        return null;
    }

    private Long findOFPortForExtPatch(Long dpId) {
        final String brInt = configurationService.getIntegrationBridgeName();
        final String brExt = configurationService.getExternalBridgeName();
        final String portNameInt = configurationService.getPatchPortName(new ImmutablePair<>(brInt, brExt));

        Preconditions.checkNotNull(dpId);
        Preconditions.checkNotNull(portNameInt);

        final long dpidPrimitive = dpId;
        for (Node node : nodeCacheManager.getBridgeNodes()) {
            if (dpidPrimitive == southbound.getDataPathId(node)) {
                final OvsdbTerminationPointAugmentation terminationPointOfBridge =
                        southbound.getTerminationPointOfBridge(node, portNameInt);
                return terminationPointOfBridge == null ? null : terminationPointOfBridge.getOfport();
            }
        }
        return null;
    }

    /**
     * Process the event.
     *
     * @param action the {@link org.opendaylight.ovsdb.openstack.netvirt.api.Action} action to be handled.
     * @param neutronNetwork An {@link org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronNetwork} instance of NeutronFloatingIP object.
     */
    public void handleNeutronNetworkEvent(final NeutronNetwork neutronNetwork, Action action) {
        LOG.debug("neutronNetwork {}: network: {}", action, neutronNetwork);
        if (action == Action.UPDATE) {
            this.updateNetworkInCleanupCache(neutronNetwork);
        }
    }

    //
    // Callbacks from OVSDB's southbound handler
    //
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
    public void handleInterfaceEvent(final Node bridgeNode, final OvsdbTerminationPointAugmentation intf,
                                     final NeutronNetwork neutronNetwork, Action action) {
        LOG.debug("southbound interface {} node:{} interface:{}, neutronNetwork:{}",
                     action, bridgeNode.getNodeId().getValue(), intf.getName(), neutronNetwork);

        final NeutronPort neutronPort = tenantNetworkManager.getTenantPort(intf);
        if (action != Action.DELETE && neutronPort != null) {
            // FIXME: Bug 4971 Move cleanup cache to SG Impl
            storePortInCleanupCache(neutronPort);
        }

        if (!this.enabled) {
            return;
        }

        final Long dpId = getDpidForIntegrationBridge(bridgeNode);
        final Uuid interfaceUuid = intf.getInterfaceUuid();

        LOG.trace("southbound interface {} node:{} interface:{}, neutronNetwork:{} port:{} dpid:{} intfUuid:{}",
                action, bridgeNode.getNodeId().getValue(), intf.getName(), neutronNetwork, neutronPort, dpId, interfaceUuid);

        if (neutronPort != null) {
            final String neutronPortUuid = neutronPort.getPortUUID();

            if (action != Action.DELETE && dpId != null && interfaceUuid != null) {
                handleInterfaceEventAdd(neutronPortUuid, dpId, interfaceUuid);
            }

            handleNeutronPortEvent(neutronPort, action);
        }

        if (action == Action.DELETE && interfaceUuid != null) {
            handleInterfaceEventDelete(intf, dpId);
        }
    }

    private void handleInterfaceEventAdd(final String neutronPortUuid, Long dpId, final Uuid interfaceUuid) {
        neutronPortToDpIdCache.put(neutronPortUuid, new ImmutablePair<>(dpId, interfaceUuid));
        LOG.debug("handleInterfaceEvent add cache entry NeutronPortUuid {} : dpid {}, ifUuid {}",
                neutronPortUuid, dpId, interfaceUuid.getValue());
    }

    private void handleInterfaceEventDelete(final OvsdbTerminationPointAugmentation intf, final Long dpId) {
        // Remove entry from neutronPortToDpIdCache based on interface uuid
        for (Map.Entry<String, Pair<Long, Uuid>> entry : neutronPortToDpIdCache.entrySet()) {
            final String currPortUuid = entry.getKey();
            if (intf.getInterfaceUuid().equals(entry.getValue().getRight())) {
                LOG.debug("handleInterfaceEventDelete remove cache entry NeutronPortUuid {} : dpid {}, ifUuid {}",
                        currPortUuid, dpId, intf.getInterfaceUuid().getValue());
                neutronPortToDpIdCache.remove(currPortUuid);
                break;
            }
        }
    }

    //
    // Internal helpers
    //
    private void updateL3ForNeutronPort(final NeutronPort neutronPort, final boolean isDelete) {

        final String networkUUID = neutronPort.getNetworkUUID();
        final String routerMacAddress = networkIdToRouterMacCache.get(networkUUID);

        if(!isDelete) {
            // If there is no router interface handling the networkUUID, we are done
            if (routerMacAddress == null || routerMacAddress.isEmpty()) {
                return;
            }

            // If this is the neutron port for the router interface itself, ignore it as well. Ports that represent the
            // router interface are handled via handleNeutronRouterInterfaceEvent.
            if (routerMacAddress.equalsIgnoreCase(neutronPort.getMacAddress())) {
                return;
            }
        }

        final NeutronNetwork neutronNetwork = neutronNetworkCache.getNetwork(networkUUID);
        final String providerSegmentationId = neutronNetwork != null ?
                                              neutronNetwork.getProviderSegmentationID() : null;
        final String tenantMac = neutronPort.getMacAddress();

        if (providerSegmentationId == null || providerSegmentationId.isEmpty() ||
            tenantMac == null || tenantMac.isEmpty()) {
            // done: go no further w/out all the info needed...
            return;
        }

        final Action action = isDelete ? Action.DELETE : Action.ADD;
        List<Node> nodes = nodeCacheManager.getBridgeNodes();
        if (nodes.isEmpty()) {
            LOG.trace("updateL3ForNeutronPort has no nodes to work with");
        }
        for (Node node : nodes) {
            final Long dpid = getDpidForIntegrationBridge(node);
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

                // Configure L3 fwd. We do that regardless of tenant network present, because these rules are
                // still needed when routing to subnets non-local to node (bug 2076).
                programL3ForwardingStage1(node, dpid, providerSegmentationId, tenantMac, tenantIpStr, action);
            }
        }
    }

    private void processSecurityGroupUpdate(NeutronPort neutronPort) {
        LOG.trace("processSecurityGroupUpdate:" + neutronPort);
        /**
         * Get updated data and original data for the the changed. Identify the security groups that got
         * added and removed and call the appropriate providers for updating the flows.
         */
        try {
            NeutronPort originalPort = neutronPort.getOriginalPort();
            List<NeutronSecurityGroup> addedGroup = getsecurityGroupChanged(neutronPort,
                                                                            neutronPort.getOriginalPort());
            List<NeutronSecurityGroup> deletedGroup = getsecurityGroupChanged(neutronPort.getOriginalPort(),
                                                                              neutronPort);

            if (null != addedGroup && !addedGroup.isEmpty()) {
                securityServicesManager.syncSecurityGroup(neutronPort,addedGroup,true);
            }
            if (null != deletedGroup && !deletedGroup.isEmpty()) {
                securityServicesManager.syncSecurityGroup(neutronPort,deletedGroup,false);
            }

        } catch (Exception e) {
            LOG.error("Exception in processSecurityGroupUpdate", e);
        }
    }

    private List<NeutronSecurityGroup> getsecurityGroupChanged(NeutronPort port1, NeutronPort port2) {
        LOG.trace("getsecurityGroupChanged:" + "Port1:" + port1 + "Port2" + port2);
        if (port1 == null) {
            return null;
        }
        List<NeutronSecurityGroup> list1 = new ArrayList<>(port1.getSecurityGroups());
        if (port2 == null) {
            return list1;
        }
        List<NeutronSecurityGroup> list2 = new ArrayList<>(port2.getSecurityGroups());
        for (Iterator<NeutronSecurityGroup> iterator = list1.iterator(); iterator.hasNext();) {
            NeutronSecurityGroup securityGroup1 = iterator.next();
            for (NeutronSecurityGroup securityGroup2 :list2) {
                if (securityGroup1.getID().equals(securityGroup2.getID())) {
                    iterator.remove();
                }
            }
        }
        return list1;
    }

    private void programL3ForwardingStage1(Node node, Long dpid, String providerSegmentationId,
                                           String macAddress, String ipStr,
                                           Action actionForNode) {
        if (actionForNode == Action.DELETE) {
            LOG.trace("Deleting Flow : programL3ForwardingStage1 for node {} providerId {} mac {} ip {} action {}",
                         node.getNodeId().getValue(), providerSegmentationId, macAddress, ipStr, actionForNode);
        }
        if (actionForNode == Action.ADD) {
            LOG.trace("Adding Flow : programL3ForwardingStage1 for node {} providerId {} mac {} ip {} action {}",
                    node.getNodeId().getValue(), providerSegmentationId, macAddress, ipStr, actionForNode);
        }

        this.programL3ForwardingStage2(node, dpid, providerSegmentationId,
                                                       macAddress, ipStr, actionForNode);
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
                     l3ForwardingProvider.programForwardingTableEntry(dpid, providerSegmentationId,
                                                                      inetAddress, macAddress, actionForNode);
        } catch (UnknownHostException e) {
            status = new Status(StatusCode.BADREQUEST);
        }

        if (status.isSuccess()) {
            LOG.debug("ProgramL3Forwarding {} for mac:{} addr:{} node:{} action:{}",
                         l3ForwardingProvider == null ? "skipped" : "programmed",
                         macAddress, address, node.getNodeId().getValue(), actionForNode);
        } else {
            LOG.error("ProgramL3Forwarding failed for mac:{} addr:{} node:{} action:{} status:{}",
                         macAddress, address, node.getNodeId().getValue(), actionForNode, status);
        }
        return status;
    }

    // --

    private void programFlowsForNeutronRouterInterface(final NeutronRouter_Interface destNeutronRouterInterface,
                                                       Boolean isDelete) {
        Preconditions.checkNotNull(destNeutronRouterInterface);

        final NeutronPort neutronPort = neutronPortCache.getPort(destNeutronRouterInterface.getPortUUID());
        String macAddress = neutronPort != null ? neutronPort.getMacAddress() : null;
        List<Neutron_IPs> ipList = neutronPort != null ? neutronPort.getFixedIPs() : null;
        final NeutronSubnet subnet = neutronSubnetCache.getSubnet(destNeutronRouterInterface.getSubnetUUID());
        final NeutronNetwork neutronNetwork = subnet != null ?
                                              neutronNetworkCache.getNetwork(subnet.getNetworkUUID()) : null;
        final String destinationSegmentationId = neutronNetwork != null ?
                                                 neutronNetwork.getProviderSegmentationID() : null;
        final Boolean isExternal = neutronNetwork != null ? neutronNetwork.getRouterExternal() : Boolean.TRUE;
        final String cidr = subnet != null ? subnet.getCidr() : null;
        final int mask = getMaskLenFromCidr(cidr);

        LOG.trace("programFlowsForNeutronRouterInterface called for interface {} isDelete {}",
                     destNeutronRouterInterface, isDelete);

        // in delete path, mac address as well as ip address are not provided. Being so, let's find them from
        // the local cache
        if (neutronNetwork != null) {
            if (macAddress == null || macAddress.isEmpty()) {
                macAddress = networkIdToRouterMacCache.get(neutronNetwork.getNetworkUUID());
            }
            if (ipList == null || ipList.isEmpty()) {
                ipList = networkIdToRouterIpListCache.get(neutronNetwork.getNetworkUUID());
            }
        }

        if (destinationSegmentationId == null || destinationSegmentationId.isEmpty() ||
            cidr == null || cidr.isEmpty() ||
            macAddress == null || macAddress.isEmpty() ||
            ipList == null || ipList.isEmpty()) {
            LOG.debug("programFlowsForNeutronRouterInterface is bailing seg:{} cidr:{} mac:{}  ip:{}",
                         destinationSegmentationId, cidr, macAddress, ipList);
            // done: go no further w/out all the info needed...
            return;
        }

        final Action actionForNode = isDelete ? Action.DELETE : Action.ADD;

        // Keep cache for finding router's mac from network uuid -- add
        //
        if (! isDelete) {
            networkIdToRouterMacCache.put(neutronNetwork.getNetworkUUID(), macAddress);
            networkIdToRouterIpListCache.put(neutronNetwork.getNetworkUUID(), new ArrayList<>(ipList));
            subnetIdToRouterInterfaceCache.put(subnet.getSubnetUUID(), destNeutronRouterInterface);
        }

        List<Node> nodes = nodeCacheManager.getBridgeNodes();
        if (nodes.isEmpty()) {
            LOG.trace("programFlowsForNeutronRouterInterface has no nodes to work with");
        }
        for (Node node : nodes) {
            final Long dpid = getDpidForIntegrationBridge(node);
            if (dpid == null) {
                continue;
            }

            for (Neutron_IPs neutronIP : ipList) {
                final String ipStr = neutronIP.getIpAddress();
                if (ipStr.isEmpty()) {
                    LOG.debug("programFlowsForNeutronRouterInterface is skipping node {} ip {}",
                            node.getNodeId().getValue(), ipStr);
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

                if (! isExternal) {
                    programFlowForNetworkFromExternal(node, dpid, destinationSegmentationId, macAddress, ipStr, mask,
                            actionForNode);
                }
                // Enable ARP responder by default, because router interface needs to be responded always.
                distributedArpService.programStaticRuleStage1(dpid, destinationSegmentationId, macAddress, ipStr, actionForNode);
                programIcmpEcho(dpid, destinationSegmentationId, macAddress, ipStr, actionForNode);
            }

            // Compute action to be programmed. In the case of rewrite exclusions, we must never program rules
            // for the external neutron networks.
            //
            {
                final Action actionForRewriteExclusion = isExternal ? Action.DELETE : actionForNode;
                programIpRewriteExclusionStage1(node, dpid, destinationSegmentationId, cidr, actionForRewriteExclusion);
            }
        }

        if (isDelete) {
            networkIdToRouterMacCache.remove(neutronNetwork.getNetworkUUID());
            networkIdToRouterIpListCache.remove(neutronNetwork.getNetworkUUID());
            subnetIdToRouterInterfaceCache.remove(subnet.getSubnetUUID());
        }
    }

    private void programFlowForNetworkFromExternal(final Node node,
                                                   final Long dpid,
                                                   final String destinationSegmentationId,
                                                   final String dstMacAddress,
                                                   final String destIpStr,
                                                   final int destMask,
                                                   final Action actionForNode) {
        programRouterInterfaceStage1(node, dpid, Constants.EXTERNAL_NETWORK, destinationSegmentationId,
                dstMacAddress, destIpStr, destMask, actionForNode);
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
            LOG.error("Could not get provider Subnet ID from router interface {}",
                         srcNeutronRouterInterface.getID());
            return;
        }

        final NeutronSubnet sourceSubnet = neutronSubnetCache.getSubnet(sourceSubnetId);
        final String sourceNetworkId = sourceSubnet == null ? null : sourceSubnet.getNetworkUUID();
        if (sourceNetworkId == null) {
            LOG.error("Could not get provider Network ID from subnet {}", sourceSubnetId);
            return;
        }

        final NeutronNetwork sourceNetwork = neutronNetworkCache.getNetwork(sourceNetworkId);
        if (sourceNetwork == null) {
            LOG.error("Could not get provider Network for Network ID {}", sourceNetworkId);
            return;
        }

        if (! sourceNetwork.getTenantID().equals(dstNeutronNetwork.getTenantID())) {
            // Isolate subnets from different tenants within the same router
            return;
        }
        final String sourceSegmentationId = sourceNetwork.getProviderSegmentationID();
        if (sourceSegmentationId == null) {
            LOG.error("Could not get provider Segmentation ID for Subnet {}", sourceSubnetId);
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
                LOG.trace("programFlowsForNeutronRouterInterfacePair reflexive is bailing seg:{} cidr:{} mac:{} ip:{}",
                             sourceSegmentationId, cidr2, macAddress2, ipList2);
                // done: go no further w/out all the info needed...
                return;
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
        if (actionForNode == Action.DELETE) {
            LOG.trace("Deleting Flow : programRouterInterfaceStage1 for node {} sourceSegId {} destSegId {} mac {} ip {} mask {}" +
                         " action {}",
                         node.getNodeId().getValue(), sourceSegmentationId, destinationSegmentationId,
                         macAddress, ipStr, mask, actionForNode);
        }
        if (actionForNode == Action.ADD) {
            LOG.trace("Adding Flow : programRouterInterfaceStage1 for node {} sourceSegId {} destSegId {} mac {} ip {} mask {}" +
                         " action {}",
                         node.getNodeId().getValue(), sourceSegmentationId, destinationSegmentationId,
                         macAddress, ipStr, mask, actionForNode);
        }

        this.programRouterInterfaceStage2(node, dpid, sourceSegmentationId, destinationSegmentationId,
                                                          macAddress, ipStr, mask, actionForNode);
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
                     routingProvider.programRouterInterface(dpid, sourceSegmentationId, destinationSegmentationId,
                                                            macAddress, inetAddress, mask, actionForNode);
        } catch (UnknownHostException e) {
            status = new Status(StatusCode.BADREQUEST);
        }

        if (status.isSuccess()) {
            LOG.debug("programRouterInterfaceStage2 {} for mac:{} addr:{}/{} node:{} srcTunId:{} destTunId:{} action:{}",
                         routingProvider == null ? "skipped" : "programmed",
                         macAddress, address, mask, node.getNodeId().getValue(), sourceSegmentationId, destinationSegmentationId,
                         actionForNode);
        } else {
            LOG.error("programRouterInterfaceStage2 failed for mac:{} addr:{}/{} node:{} srcTunId:{} destTunId:{} action:{} status:{}",
                         macAddress, address, mask, node.getNodeId().getValue(), sourceSegmentationId, destinationSegmentationId,
                         actionForNode, status);
        }
        return status;
    }

    private boolean programIcmpEcho(Long dpid, String segOrOfPort,
                                           String macAddress, String ipStr,
                                           Action action) {
        if (action == Action.DELETE ) {
            LOG.trace("Deleting Flow : programIcmpEcho dpid {} segOrOfPort {} mac {} ip {} action {}",
                    dpid, segOrOfPort, macAddress, ipStr, action);
        }
        if (action == Action.ADD) {
            LOG.trace("Adding Flow : programIcmpEcho dpid {} segOrOfPort {} mac {} ip {} action {}",
                    dpid, segOrOfPort, macAddress, ipStr, action);
        }

        Status status = new Status(StatusCode.UNSUPPORTED);
        if (icmpEchoProvider != null){
            try {
                InetAddress inetAddress = InetAddress.getByName(ipStr);
                status = icmpEchoProvider.programIcmpEchoEntry(dpid, segOrOfPort,
                                                macAddress, inetAddress, action);
            } catch (UnknownHostException e) {
                status = new Status(StatusCode.BADREQUEST);
            }
        }

        if (status.isSuccess()) {
            LOG.debug("programIcmpEcho {} for mac:{} addr:{} dpid:{} segOrOfPort:{} action:{}",
                    icmpEchoProvider == null ? "skipped" : "programmed",
                    macAddress, ipStr, dpid, segOrOfPort, action);
        } else {
            LOG.error("programIcmpEcho failed for mac:{} addr:{} dpid:{} segOrOfPort:{} action:{} status:{}",
                    macAddress, ipStr, dpid, segOrOfPort, action, status);
        }

        return status.isSuccess();
    }

    private boolean programInboundIpRewriteStage1(Long dpid, Long inboundOFPort, String providerSegmentationId,
                                                  String matchAddress, String rewriteAddress,
                                                  Action action) {
        if (action == Action.DELETE ) {
            LOG.trace("Deleting Flow : programInboundIpRewriteStage1 dpid {} OFPort {} seg {} matchAddress {} rewriteAddress {}" +
                    " action {}",
                    dpid, inboundOFPort, providerSegmentationId, matchAddress, rewriteAddress, action);
        }
        if (action == Action.ADD ) {
            LOG.trace("Adding Flow : programInboundIpRewriteStage1 dpid {} OFPort {} seg {} matchAddress {} rewriteAddress {}" +
                    " action {}",
                    dpid, inboundOFPort, providerSegmentationId, matchAddress, rewriteAddress, action);
        }

        Status status = programInboundIpRewriteStage2(dpid, inboundOFPort, providerSegmentationId, matchAddress,
                rewriteAddress, action);
        return status.isSuccess();
    }

    private Status programInboundIpRewriteStage2(Long dpid, Long inboundOFPort, String providerSegmentationId,
                                                 String matchAddress, String rewriteAddress,
                                                 Action action) {
        Status status;
        try {
            InetAddress inetMatchAddress = InetAddress.getByName(matchAddress);
            InetAddress inetRewriteAddress = InetAddress.getByName(rewriteAddress);
            status = inboundNatProvider == null ?
                    new Status(StatusCode.SUCCESS) :
                    inboundNatProvider.programIpRewriteRule(dpid, inboundOFPort, providerSegmentationId,
                            inetMatchAddress, inetRewriteAddress,
                            action);
        } catch (UnknownHostException e) {
            status = new Status(StatusCode.BADREQUEST);
        }

        if (status.isSuccess()) {
            final boolean isSkipped = inboundNatProvider == null;
            LOG.debug("programInboundIpRewriteStage2 {} for dpid:{} ofPort:{} seg:{} match:{} rewrite:{} action:{}",
                    isSkipped ? "skipped" : "programmed",
                    dpid, inboundOFPort, providerSegmentationId, matchAddress, rewriteAddress, action);
        } else {
            LOG.error("programInboundIpRewriteStage2 failed for dpid:{} ofPort:{} seg:{} match:{} rewrite:{} action:{}" +
                         " status:{}",
                    dpid, inboundOFPort, providerSegmentationId, matchAddress, rewriteAddress, action,
                    status);
        }
        return status;
    }

    private void programIpRewriteExclusionStage1(Node node, Long dpid, String providerSegmentationId, String cidr,
                                                 Action actionForRewriteExclusion) {
        if (actionForRewriteExclusion == Action.DELETE ) {
            LOG.trace("Deleting Flow : programIpRewriteExclusionStage1 node {} providerId {} cidr {} action {}",
                         node.getNodeId().getValue(), providerSegmentationId, cidr, actionForRewriteExclusion);
        }
        if (actionForRewriteExclusion == Action.ADD) {
            LOG.trace("Adding Flow : programIpRewriteExclusionStage1 node {} providerId {} cidr {} action {}",
                         node.getNodeId().getValue(), providerSegmentationId, cidr, actionForRewriteExclusion);
        }

        this.programIpRewriteExclusionStage2(node, dpid, providerSegmentationId, cidr,actionForRewriteExclusion);
    }

    private Status programIpRewriteExclusionStage2(Node node, Long dpid, String providerSegmentationId, String cidr,
                                                   Action actionForNode) {
        final Status status = outboundNatProvider == null ? new Status(StatusCode.SUCCESS) :
                outboundNatProvider.programIpRewriteExclusion(dpid, providerSegmentationId, cidr, actionForNode);

        if (status.isSuccess()) {
            final boolean isSkipped = outboundNatProvider == null;
            LOG.debug("IpRewriteExclusion {} for cidr:{} node:{} action:{}",
                         isSkipped ? "skipped" : "programmed",
                         cidr, node.getNodeId().getValue(), actionForNode);
        } else {
            LOG.error("IpRewriteExclusion failed for cidr:{} node:{} action:{} status:{}",
                         cidr, node.getNodeId().getValue(), actionForNode, status);
        }
        return status;
    }

    private void programOutboundIpRewriteStage1(FloatIpData fid, Action action) {

        if (action == Action.DELETE) {
            LOG.trace("Deleting Flow : programOutboundIpRewriteStage1 dpid {} seg {} fixedIpAddress {} floatIp {} action {} ",
                    fid.dpid, fid.segId, fid.fixedIpAddress, fid.floatingIpAddress, action);
        }
        if (action == Action.ADD) {
            LOG.trace("Adding Flow : programOutboundIpRewriteStage1 dpid {} seg {} fixedIpAddress {} floatIp {} action {} " ,
                    fid.dpid, fid.segId, fid.fixedIpAddress, fid.floatingIpAddress, action);
        }

        this.programOutboundIpRewriteStage2(fid, action);
    }

    private Status programOutboundIpRewriteStage2(FloatIpData fid, Action action) {
        Status status;
        try {
            InetAddress matchSrcAddress = InetAddress.getByName(fid.fixedIpAddress);
            InetAddress rewriteSrcAddress = InetAddress.getByName(fid.floatingIpAddress);
            status = outboundNatProvider == null ?
                    new Status(StatusCode.SUCCESS) :
                    outboundNatProvider.programIpRewriteRule(
                            fid.dpid, fid.segId, fid.neutronRouterMac, matchSrcAddress, fid.macAddress,
                            this.externalRouterMac, rewriteSrcAddress, fid.ofPort, action);
        } catch (UnknownHostException e) {
            status = new Status(StatusCode.BADREQUEST);
        }

        if (status.isSuccess()) {
            final boolean isSkipped = outboundNatProvider == null;
            LOG.debug("programOutboundIpRewriteStage2 {} for dpid {} seg {} fixedIpAddress {} floatIp {}" +
                            " action {}",
                         isSkipped ? "skipped" : "programmed",
                         fid.dpid, fid.segId, fid.fixedIpAddress, fid.floatingIpAddress, action);
        } else {
            LOG.error("programOutboundIpRewriteStage2 failed for dpid {} seg {} fixedIpAddress {} floatIp {}" +
                         " action {} status:{}",
                         fid.dpid, fid.segId, fid.fixedIpAddress, fid.floatingIpAddress, action, status);
        }
        return status;
    }

    private int getMaskLenFromCidr(String cidr) {
        if (cidr == null) {
            return 0;
        }
        String[] splits = cidr.split("/");
        if (splits.length != 2) {
            return 0;
        }

        int result;
        try {
            result = Integer.parseInt(splits[1].trim());
        } catch (NumberFormatException nfe) {
            result = 0;
        }
        return result;
    }

    private Long getDpidForIntegrationBridge(Node node) {
        // Check if node is integration bridge; and only then return its dpid
        if (southbound.getBridge(node, configurationService.getIntegrationBridgeName()) != null) {
            return southbound.getDataPathId(node);
        }
        return null;
    }

    private Long getDpidForExternalBridge(Node node) {
        // Check if node is external bridge; and only then return its dpid
        if (southbound.getBridge(node, configurationService.getExternalBridgeName()) != null) {
            return southbound.getDataPathId(node);
        }
        return null;
    }

    private Node getExternalBridgeNode(){
        //Pickup the first node that has external bridge (br-ex).
        //NOTE: We are assuming that all the br-ex are serving one external network and gateway ip of
        //the external network is reachable from every br-ex
        // TODO: Consider other deployment scenario, and thing of better solution.
        List<Node> allBridges = nodeCacheManager.getBridgeNodes();
        for(Node node : allBridges){
            if (southbound.getBridge(node, configurationService.getExternalBridgeName()) != null) {
                return node;
            }
        }
        return null;
    }

    private NeutronSubnet getExternalNetworkSubnet(NeutronPort gatewayPort){
        if (gatewayPort.getFixedIPs() == null) {
            return null;
        }
        for (Neutron_IPs neutronIPs : gatewayPort.getFixedIPs()) {
            String subnetUUID = neutronIPs.getSubnetUUID();
            NeutronSubnet extSubnet = neutronSubnetCache.getSubnet(subnetUUID);
            if (extSubnet != null && extSubnet.getGatewayIP() != null) {
                return extSubnet;
            }
            if (extSubnet == null) {
                // TODO: when subnet is created, try again.
                LOG.debug("subnet {} in not found", subnetUUID);
             }
        }
        return null;
    }

    private void cleanupFloatingIPRules(final NeutronPort neutronPort) {

        List<NeutronFloatingIP> neutronFloatingIps = neutronFloatingIpCache.getAllFloatingIPs();
        if (neutronFloatingIps != null && !neutronFloatingIps.isEmpty()) {
            for (NeutronFloatingIP neutronFloatingIP : neutronFloatingIps) {
                if (neutronFloatingIP.getPortUUID().equals(neutronPort.getPortUUID())) {
                    handleNeutronFloatingIPEvent(neutronFloatingIP, Action.DELETE);
                }
            }
        }
    }

    private void updateFloatingIPRules(final NeutronPort neutronPort) {
        List<NeutronFloatingIP> neutronFloatingIps = neutronFloatingIpCache.getAllFloatingIPs();
        if (neutronFloatingIps != null) {
            for (NeutronFloatingIP neutronFloatingIP : neutronFloatingIps) {
                if (neutronFloatingIP.getPortUUID().equals(neutronPort.getPortUUID())) {
                    handleNeutronFloatingIPEvent(neutronFloatingIP, Action.UPDATE);
                }
            }
        }
    }


    private void triggerGatewayMacResolver(final NeutronPort gatewayPort){

        Preconditions.checkNotNull(gatewayPort);
        NeutronNetwork externalNetwork = neutronNetworkCache.getNetwork(gatewayPort.getNetworkUUID());

        if(externalNetwork != null){
            if(externalNetwork.isRouterExternal()){
                final NeutronSubnet externalSubnet = getExternalNetworkSubnet(gatewayPort);

                // TODO: address IPv6 case.
                if (externalSubnet != null &&
                        externalSubnet.getIpVersion() == 4 &&
                        gatewayPort.getFixedIPs() != null) {
                    LOG.info("Trigger MAC resolution for gateway ip {}", externalSubnet.getGatewayIP());
                    Neutron_IPs neutronIP = null;
                    for (Neutron_IPs nIP : gatewayPort.getFixedIPs()) {
                        InetAddress ipAddress;
                        try {
                            ipAddress = InetAddress.getByAddress(nIP.getIpAddress().getBytes());
                        } catch (UnknownHostException e) {
                            LOG.warn("unknown host exception {}", e);
                            continue;
                        }
                        if (ipAddress instanceof Inet4Address) {
                            neutronIP = nIP;
                            break;
                        }
                    }
                    if (neutronIP == null) {
                        // TODO IPv6 neighbor discovery
                        LOG.debug("Ignoring gateway ports with IPv6 only fixed ip {}",
                                  gatewayPort.getFixedIPs());
                    } else {
                        gatewayMacResolver.resolveMacAddress(
                            this, /* gatewayMacResolverListener */
                            null, /* externalNetworkBridgeDpid */
                            true, /* refreshExternalNetworkBridgeDpidIfNeeded */
                            new Ipv4Address(externalSubnet.getGatewayIP()),
                            new Ipv4Address(neutronIP.getIpAddress()),
                            new MacAddress(gatewayPort.getMacAddress()),
                            true /* periodicRefresh */);
                    }
                } else {
                    LOG.warn("No gateway IP address found for external network {}", externalNetwork);
                }
            }
        }else{
            LOG.warn("Neutron network not found for router interface {}", gatewayPort);
        }
    }


    private void storePortInCleanupCache(NeutronPort port) {
        this.portCleanupCache.put(port.getPortUUID(),port);
    }


    private void updatePortInCleanupCache(NeutronPort updatedPort,NeutronPort originalPort) {
        removePortFromCleanupCache(originalPort);
        storePortInCleanupCache(updatedPort);
    }

    public void removePortFromCleanupCache(NeutronPort port) {
        if(port != null) {
            this.portCleanupCache.remove(port.getPortUUID());
        }
    }

    public Map<String, NeutronPort> getPortCleanupCache() {
        return this.portCleanupCache;
    }

    public NeutronPort getPortFromCleanupCache(String portid) {
        for (String neutronPortUuid : this.portCleanupCache.keySet()) {
            if (neutronPortUuid.equals(portid)) {
                LOG.info("getPortFromCleanupCache: Matching NeutronPort found {}", portid);
                return this.portCleanupCache.get(neutronPortUuid);
            }
        }
        return null;
    }

    private void storeNetworkInCleanupCache(NeutronNetwork network) {
        this.networkCleanupCache.put(network.getNetworkUUID(), network);
    }


    private void updateNetworkInCleanupCache(NeutronNetwork network) {
        for (String neutronNetworkUuid:this.networkCleanupCache.keySet()) {
            if (neutronNetworkUuid.equals(network.getNetworkUUID())) {
                this.networkCleanupCache.remove(neutronNetworkUuid);
            }
        }
        this.networkCleanupCache.put(network.getNetworkUUID(), network);
    }

    public void removeNetworkFromCleanupCache(String networkid) {
        NeutronNetwork network = null;
        for (String neutronNetworkUuid:this.networkCleanupCache.keySet()) {
            if (neutronNetworkUuid.equals(networkid)) {
                network = networkCleanupCache.get(neutronNetworkUuid);
                break;
            }
        }
        if (network != null) {
            for (String neutronPortUuid:this.portCleanupCache.keySet()) {
                if (this.portCleanupCache.get(neutronPortUuid).getNetworkUUID().equals(network.getNetworkUUID())) {
                    LOG.info("This network is used by another port", network);
                    return;
                }
            }
            this.networkCleanupCache.remove(network.getNetworkUUID());
        }
    }

    public Map<String, NeutronNetwork> getNetworkCleanupCache() {
        return this.networkCleanupCache;
    }

    public NeutronNetwork getNetworkFromCleanupCache(String networkid) {
        for (String neutronNetworkUuid:this.networkCleanupCache.keySet()) {
            if (neutronNetworkUuid.equals(networkid)) {
                LOG.info("getPortFromCleanupCache: Matching NeutronPort found {}", networkid);
                return networkCleanupCache.get(neutronNetworkUuid);
            }
        }
        return null;
    }
    /**
     * Return String that represents OF port with marker explicitly provided (reverse of MatchUtils:parseExplicitOFPort)
     *
     * @param ofPort the OF port number
     * @return the string with encoded OF port (example format "OFPort|999")
     */
    public static String encodeExcplicitOFPort(Long ofPort) {
        return "OFPort|" + ofPort.toString();
    }
    private void initNetworkCleanUpCache() {
        if (this.neutronNetworkCache != null) {
            for (NeutronNetwork neutronNetwork : neutronNetworkCache.getAllNetworks()) {
                networkCleanupCache.put(neutronNetwork.getNetworkUUID(), neutronNetwork);
            }
        }
    }
    private void initPortCleanUpCache() {
        if (this.neutronPortCache != null) {
            for (NeutronPort neutronPort : neutronPortCache.getAllPorts()) {
                portCleanupCache.put(neutronPort.getPortUUID(), neutronPort);
            }
        }
    }
    @Override
    public void setDependencies(ServiceReference serviceReference) {
        eventDispatcher =
                (EventDispatcher) ServiceHelper.getGlobalInstance(EventDispatcher.class, this);
        eventDispatcher.eventHandlerAdded(serviceReference, this);
        tenantNetworkManager =
                (TenantNetworkManager) ServiceHelper.getGlobalInstance(TenantNetworkManager.class, this);
        configurationService =
                (ConfigurationService) ServiceHelper.getGlobalInstance(ConfigurationService.class, this);
        arpProvider =
                (ArpProvider) ServiceHelper.getGlobalInstance(ArpProvider.class, this);
        inboundNatProvider =
                (InboundNatProvider) ServiceHelper.getGlobalInstance(InboundNatProvider.class, this);
        outboundNatProvider =
                (OutboundNatProvider) ServiceHelper.getGlobalInstance(OutboundNatProvider.class, this);
        routingProvider =
                (RoutingProvider) ServiceHelper.getGlobalInstance(RoutingProvider.class, this);
        l3ForwardingProvider =
                (L3ForwardingProvider) ServiceHelper.getGlobalInstance(L3ForwardingProvider.class, this);
        distributedArpService =
                 (DistributedArpService) ServiceHelper.getGlobalInstance(DistributedArpService.class, this);
        nodeCacheManager =
                (NodeCacheManager) ServiceHelper.getGlobalInstance(NodeCacheManager.class, this);
        southbound =
                (Southbound) ServiceHelper.getGlobalInstance(Southbound.class, this);
        gatewayMacResolver =
                (GatewayMacResolver) ServiceHelper.getGlobalInstance(GatewayMacResolver.class, this);
        securityServicesManager =
                (SecurityServicesManager) ServiceHelper.getGlobalInstance(SecurityServicesManager.class, this);

        initL3AdapterMembers();
    }

    @Override
    public void setDependencies(Object impl) {
        if (impl instanceof INeutronNetworkCRUD) {
            neutronNetworkCache = (INeutronNetworkCRUD)impl;
            initNetworkCleanUpCache();
        } else if (impl instanceof INeutronPortCRUD) {
            neutronPortCache = (INeutronPortCRUD)impl;
            initPortCleanUpCache();
        } else if (impl instanceof INeutronSubnetCRUD) {
            neutronSubnetCache = (INeutronSubnetCRUD)impl;
        } else if (impl instanceof INeutronFloatingIPCRUD) {
            neutronFloatingIpCache = (INeutronFloatingIPCRUD)impl;
        } else if (impl instanceof ArpProvider) {
            arpProvider = (ArpProvider)impl;
        } else if (impl instanceof InboundNatProvider) {
            inboundNatProvider = (InboundNatProvider)impl;
        } else if (impl instanceof OutboundNatProvider) {
            outboundNatProvider = (OutboundNatProvider)impl;
        } else if (impl instanceof RoutingProvider) {
            routingProvider = (RoutingProvider)impl;
        } else if (impl instanceof L3ForwardingProvider) {
            l3ForwardingProvider = (L3ForwardingProvider)impl;
        }else if (impl instanceof GatewayMacResolver) {
            gatewayMacResolver = (GatewayMacResolver)impl;
        }else if (impl instanceof IcmpEchoProvider) {
            icmpEchoProvider = (IcmpEchoProvider)impl;
        }

        populateL3ForwardingCaches();
    }
}
