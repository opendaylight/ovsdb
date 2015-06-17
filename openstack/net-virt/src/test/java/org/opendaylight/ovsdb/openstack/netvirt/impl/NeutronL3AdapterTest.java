/*
 * Copyright (c) 2015 Inocybe and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.opendaylight.neutron.spi.INeutronNetworkCRUD;
import org.opendaylight.neutron.spi.INeutronPortCRUD;
import org.opendaylight.neutron.spi.INeutronSubnetCRUD;
import org.opendaylight.neutron.spi.NeutronFloatingIP;
import org.opendaylight.neutron.spi.NeutronNetwork;
import org.opendaylight.neutron.spi.NeutronPort;
import org.opendaylight.neutron.spi.NeutronRouter;
import org.opendaylight.neutron.spi.NeutronRouter_Interface;
import org.opendaylight.neutron.spi.NeutronSubnet;
import org.opendaylight.neutron.spi.Neutron_IPs;
import org.opendaylight.ovsdb.openstack.netvirt.api.Action;
import org.opendaylight.ovsdb.openstack.netvirt.api.ArpProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.ConfigurationService;
import org.opendaylight.ovsdb.openstack.netvirt.api.InboundNatProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.L3ForwardingProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.NodeCacheManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.OutboundNatProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.RoutingProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.Southbound;
import org.opendaylight.ovsdb.openstack.netvirt.api.Status;
import org.opendaylight.ovsdb.openstack.netvirt.api.TenantNetworkManager;
import org.opendaylight.ovsdb.utils.config.ConfigProperties;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Unit test for {@link NeutronL3Adapter}
 */
@PrepareForTest({ConfigProperties.class, ServiceHelper.class})
@RunWith(PowerMockRunner.class)
public class NeutronL3AdapterTest {

    @InjectMocks private NeutronL3Adapter neutronL3Adapter;

    @Mock private ConfigurationService configurationService;
    @Mock private TenantNetworkManager tenantNetworkManager;
    @Mock private INeutronNetworkCRUD neutronNetworkCache;
    @Mock private INeutronSubnetCRUD neutronSubnetCache;
    @Mock private INeutronPortCRUD neutronPortCache;
    @Mock private NodeCacheManager nodeCacheManager;
    @Mock private Southbound southbound;

    @Mock private NeutronPort neutronPort;

    private Set<String> inboundIpRewriteCache;
    private Set<String> outboundIpRewriteCache;
    private Set<String> outboundIpRewriteExclusionCache;
    private Set<String> routerInterfacesCache;
    private Set<String> staticArpEntryCache;
    private Set<String> l3ForwardingCache;
    private Set<String> defaultRouteCache;
    private Map<String, String> networkIdToRouterMacCache;
    private Map<String, NeutronRouter_Interface> subnetIdToRouterInterfaceCache;

    private static final String HOST_ADDRESS = "127.0.0.1";

    @Before
    public void setUp() throws Exception{
        PowerMockito.mockStatic(ConfigProperties.class);
        PowerMockito.when(ConfigProperties.getProperty(neutronL3Adapter.getClass(), "ovsdb.l3.fwd.enabled")).thenReturn("yes");

        when(configurationService.isL3ForwardingEnabled()).thenReturn(true);

        this.getMethod("initL3AdapterMembers").invoke(neutronL3Adapter);

        this.getNeutronL3AdapterFields();
        this.setUpVar();
    }

    @Test
    public void test() {

    }

    private void getNeutronL3AdapterFields() throws Exception{
        inboundIpRewriteCache = (Set<String>) getField("inboundIpRewriteCache");
        outboundIpRewriteCache = (Set<String>) getField("outboundIpRewriteCache");
        outboundIpRewriteExclusionCache = (Set<String>) getField("outboundIpRewriteExclusionCache");
        routerInterfacesCache = (Set<String>) getField("routerInterfacesCache");
        staticArpEntryCache = (Set<String>) getField("staticArpEntryCache");
        l3ForwardingCache = (Set<String>) getField("l3ForwardingCache");
        defaultRouteCache = (Set<String>) getField("defaultRouteCache");
        networkIdToRouterMacCache = (Map<String, String>) getField("networkIdToRouterMacCache");
        subnetIdToRouterInterfaceCache = (Map<String, NeutronRouter_Interface>) getField("subnetIdToRouterInterfaceCache");
    }

    private void setUpVar(){
        Neutron_IPs neutronIP = mock(Neutron_IPs.class);
        NeutronRouter neutronRouter = mock(NeutronRouter.class);

        NeutronSubnet neutronSubnet = mock(NeutronSubnet.class);
        NeutronNetwork neutronNetwork = mock(NeutronNetwork.class);
        Node node = mock(Node.class);
        NodeId nodeID = mock(NodeId.class);
        // TODO SB_MIGRATION
        //Row row = mock(Row.class);
        //Bridge bridge = mock(Bridge.class);
        Status status = mock(Status.class);

        List<Neutron_IPs> list_neutronIP = new ArrayList<Neutron_IPs>();
        list_neutronIP.add(neutronIP);

        List<NeutronPort> list_neutronPort = new ArrayList<>();
        list_neutronPort.add(neutronPort);

        List<Node> list_nodes = new ArrayList<Node>();
        list_nodes.add(node);

        //ConcurrentMap<String, Row> rowMap = mock(ConcurrentMap.class);
        //rowMap.put("key", row);

        //Column<GenericTableSchema, Set<String>> bridgeColumnIds = mock(Column.class);
        Set<String> dpids = new HashSet();
        dpids.add("11111");

        when(neutronPort.getFixedIPs()).thenReturn(list_neutronIP);
        when(neutronPort.getPortUUID()).thenReturn("portUUID");
        when(neutronPort.getTenantID()).thenReturn("tenantID");
        when(neutronPort.getNetworkUUID()).thenReturn("networkUUID");
        when(neutronPort.getMacAddress()).thenReturn("macAddress1").thenReturn("macAddress2");

        when(neutronIP.getSubnetUUID()).thenReturn("subnetUUID");
        when(neutronIP.getIpAddress()).thenReturn(HOST_ADDRESS);

        when(neutronPortCache.getAllPorts()).thenReturn(list_neutronPort);
        when(neutronPortCache.getPort(anyString())).thenReturn(neutronPort);

        when(neutronSubnetCache.getSubnet(anyString())).thenReturn(neutronSubnet);

        when(neutronSubnet.getNetworkUUID()).thenReturn("networkUUID");

        when(neutronNetworkCache.getNetwork(anyString())).thenReturn(neutronNetwork);

        when(neutronNetwork.getRouterExternal()).thenReturn(false); // default true
        when(neutronNetwork.getProviderSegmentationID()).thenReturn("providerSegmentationId1","providerSegmentationId2", "providerSegmentationId3");
        when(neutronNetwork.getTenantID()).thenReturn("tenantId");
        when(neutronNetwork.getNetworkUUID()).thenReturn("networkUUID");

        when(neutronSubnet.getGatewayIP()).thenReturn("gatewayIp");
        when(neutronSubnet.getCidr()).thenReturn(HOST_ADDRESS + "/32");
        when(neutronSubnet.getSubnetUUID()).thenReturn("subnetUUID");

        when(tenantNetworkManager.isTenantNetworkPresentInNode(any(Node.class), anyString())).thenReturn(false);
        when(tenantNetworkManager.isTenantNetworkPresentInNode(any(Node.class), anyString())).thenReturn(true);

        when(node.getNodeId()).thenReturn(nodeID);

        when(nodeID.getValue()).thenReturn("nodeId");

        when(status.isSuccess()).thenReturn(true);

        /* TODO SB_MIGRATION */
        //when(connectionService.getBridgeNodes()).thenReturn(list_nodes);

        when(configurationService.getDefaultGatewayMacAddress(any(Node.class))).thenReturn("defaultGatewayMacAddress");
        when(configurationService.getIntegrationBridgeName()).thenReturn("brName");

        //when(ovsdbConfigurationService.getRows(any(Node.class), anyString())).thenReturn(rowMap);
        //when(ovsdbConfigurationService.getTypedRow(any(Node.class), same(Bridge.class), any(Row.class))).thenReturn(bridge);
        //when(ovsdbConfigurationService.getRow(any(Node.class), anyString(), anyString())).thenReturn(row);

        //when(bridge.getName()).thenReturn("brName");
        //when(bridge.getDatapathIdColumn()).thenReturn(bridgeColumnIds);

        //when(bridgeColumnIds.getData()).thenReturn(dpids);

        when(nodeCacheManager.getBridgeNodes()).thenReturn(list_nodes);
    }


    /**
     * Test method {@link NeutronL3Adapter#handleNeutronPortEvent(NeutronPort, Action)}
     * Device owner = network:router_interface
     */
//    @Test
    public void testHandleNeutronPortEvent1() {
        when(neutronPort.getDeviceOwner()).thenReturn("network:router_interface");

        neutronL3Adapter.handleNeutronPortEvent(neutronPort, Action.ADD);
        // Affected by the add
        assertEquals("Error, did not return the correct networkIdToRouterMacCache size", 1, networkIdToRouterMacCache.size());
        assertEquals("Error, did not return the correct subnetIdToRouterInterfaceCache size", 1, subnetIdToRouterInterfaceCache.size());
        /* TODO SB_MIGRATION */
        //assertEquals("Error, did not return the correct routerInterfacesCache size", 2, routerInterfacesCache.size());
//        assertEquals("Error, did not return the correct staticArpEntryCache size", 2, staticArpEntryCache.size());
//        assertEquals("Error, did not return the correct outboundIpRewriteExclusionCache size", 1, outboundIpRewriteExclusionCache.size());
//        assertEquals("Error, did not return the correct l3ForwardingCache size", 1, l3ForwardingCache.size());
        // Unchanged
        assertEquals("Error, did not return the correct inboundIpRewriteCache size", 0, inboundIpRewriteCache.size());
        assertEquals("Error, did not return the correct outboundIpRewriteCache size", 0, outboundIpRewriteCache.size());
        assertEquals("Error, did not return the correct defaultRouteCache size", 0, defaultRouteCache.size());

        neutronL3Adapter.handleNeutronPortEvent(neutronPort, Action.DELETE);
        // Affected by the delete
        assertEquals("Error, did not return the correct networkIdToRouterMacCache size", 0, networkIdToRouterMacCache.size());
        assertEquals("Error, did not return the correct subnetIdToRouterInterfaceCache size", 0, subnetIdToRouterInterfaceCache.size());
//        assertEquals("Error, did not return the correct staticArpEntryCache size", 1, staticArpEntryCache.size());
        // Unchanged
        assertEquals("Error, did not return the correct routerInterfacesCache size", 2, routerInterfacesCache.size());
        assertEquals("Error, did not return the correct outboundIpRewriteExclusionCache size", 1, outboundIpRewriteExclusionCache.size());
        assertEquals("Error, did not return the correct l3ForwardingCache size", 1, l3ForwardingCache.size());
        assertEquals("Error, did not return the correct inboundIpRewriteCache size", 0, inboundIpRewriteCache.size());
        assertEquals("Error, did not return the correct outboundIpRewriteCache size", 0, outboundIpRewriteCache.size());
        assertEquals("Error, did not return the correct defaultRouteCache size", 0, defaultRouteCache.size());
    }

    /**
     * Test method {@link NeutronL3Adapter#handleNeutronPortEvent(NeutronPort, Action)}
     * Device owner = ""
     */
//    @Test
    public void testHandleNeutronPortEvent2() {
        when(neutronPort.getDeviceOwner()).thenReturn("");

        // populate subnetIdToRouterInterfaceCache to pass the
        // if (neutronRouterInterface != null)
        NeutronRouter_Interface neutronRouterInterface = mock(NeutronRouter_Interface.class);
        when(neutronRouterInterface.getPortUUID()).thenReturn("portUUID");
        when(neutronRouterInterface.getSubnetUUID()).thenReturn("subnetUUID");
        subnetIdToRouterInterfaceCache.put("subnetUUID", neutronRouterInterface);

        /* device owner = "" */
        neutronL3Adapter.handleNeutronPortEvent(neutronPort, Action.ADD);
        // Affected by the add
        /* TODO SB_MIGRATION */
        //assertEquals("Error, did not return the correct routerInterfacesCache size", 2, routerInterfacesCache.size());
        assertEquals("Error, did not return the correct staticArpEntryCache size", 2, staticArpEntryCache.size());
        assertEquals("Error, did not return the correct outboundIpRewriteExclusionCache size", 1, outboundIpRewriteExclusionCache.size());
        assertEquals("Error, did not return the correct l3ForwardingCache size", 1, l3ForwardingCache.size());
        assertEquals("Error, did not return the correct networkIdToRouterMacCache size", 1, networkIdToRouterMacCache.size());
        // Added above
        assertEquals("Error, did not return the correct subnetIdToRouterInterfaceCache size", 1, subnetIdToRouterInterfaceCache.size());
        // Unchanged
        assertEquals("Error, did not return the correct inboundIpRewriteCache size", 0, inboundIpRewriteCache.size());
        assertEquals("Error, did not return the correct outboundIpRewriteCache size", 0, outboundIpRewriteCache.size());
        assertEquals("Error, did not return the correct defaultRouteCache size", 0, defaultRouteCache.size());

        neutronL3Adapter.handleNeutronPortEvent(neutronPort, Action.DELETE);
        // Affected by the delete
        assertEquals("Error, did not return the correct staticArpEntryCache size", 1, staticArpEntryCache.size());
        assertEquals("Error, did not return the correct l3ForwardingCache size", 0, l3ForwardingCache.size());
        // Unchanged
        assertEquals("Error, did not return the correct routerInterfacesCache size", 2, routerInterfacesCache.size());
        assertEquals("Error, did not return the correct outboundIpRewriteExclusionCache size", 1, outboundIpRewriteExclusionCache.size());
        assertEquals("Error, did not return the correct networkIdToRouterMacCache size", 1, networkIdToRouterMacCache.size());
        assertEquals("Error, did not return the correct inboundIpRewriteCache size", 0, inboundIpRewriteCache.size());
        assertEquals("Error, did not return the correct outboundIpRewriteCache size", 0, outboundIpRewriteCache.size());
        assertEquals("Error, did not return the correct defaultRouteCache size", 0, defaultRouteCache.size());
        // Added above
        assertEquals("Error, did not return the correct subnetIdToRouterInterfaceCache size", 1, subnetIdToRouterInterfaceCache.size());
    }

    /**
     * Test method {@link NeutronL3Adapter#handleNeutronFloatingIPEvent(NeutronFloatingIP, Action)}
     */
//    @Test
    public void testandleNeutronFloatingIPEvent() throws Exception{
        NeutronFloatingIP neutronFloatingIP = mock(NeutronFloatingIP.class);
        when(neutronFloatingIP.getFixedIPAddress()).thenReturn(HOST_ADDRESS);
        when(neutronFloatingIP.getFloatingIPAddress()).thenReturn(HOST_ADDRESS);
        when(neutronFloatingIP.getFloatingNetworkUUID()).thenReturn("floatingNetworkUUID");

        networkIdToRouterMacCache.put("floatingNetworkUUID", "routerMacAddress");

        neutronL3Adapter.handleNeutronFloatingIPEvent(neutronFloatingIP, Action.ADD);
        // Added above
        assertEquals("Error, did not return the correct networkIdToRouterMacCache size", 1, networkIdToRouterMacCache.size());
        // Affected by the add
        /* TODO SB_MIGRATION */
        //assertEquals("Error, did not return the correct inboundIpRewriteCache size", 1, inboundIpRewriteCache.size());
        assertEquals("Error, did not return the correct outboundIpRewriteCache size", 1, outboundIpRewriteCache.size());
        assertEquals("Error, did not return the correct staticArpEntryCache size", 1, staticArpEntryCache.size());
        // Unchanged
        assertEquals("Error, did not return the correct routerInterfacesCache size", 0, routerInterfacesCache.size());
        assertEquals("Error, did not return the correct outboundIpRewriteExclusionCache size", 0, outboundIpRewriteExclusionCache.size());
        assertEquals("Error, did not return the correct subnetIdToRouterInterfaceCache size", 0, subnetIdToRouterInterfaceCache.size());
        assertEquals("Error, did not return the correct l3ForwardingCache size", 0, l3ForwardingCache.size());
        assertEquals("Error, did not return the correct defaultRouteCache size", 0, defaultRouteCache.size());

        neutronL3Adapter.handleNeutronFloatingIPEvent(neutronFloatingIP, Action.DELETE);
        // Unchanged
        assertEquals("Error, did not return the correct networkIdToRouterMacCache size", 1, networkIdToRouterMacCache.size());
        assertEquals("Error, did not return the correct inboundIpRewriteCache size", 1, inboundIpRewriteCache.size());
        assertEquals("Error, did not return the correct outboundIpRewriteCache size", 1, outboundIpRewriteCache.size());
        assertEquals("Error, did not return the correct staticArpEntryCache size", 1, staticArpEntryCache.size());
        assertEquals("Error, did not return the correct routerInterfacesCache size", 0, routerInterfacesCache.size());
        assertEquals("Error, did not return the correct outboundIpRewriteExclusionCache size", 0, outboundIpRewriteExclusionCache.size());
        assertEquals("Error, did not return the correct subnetIdToRouterInterfaceCache size", 0, subnetIdToRouterInterfaceCache.size());
        assertEquals("Error, did not return the correct l3ForwardingCache size", 0, l3ForwardingCache.size());
        assertEquals("Error, did not return the correct defaultRouteCache size", 0, defaultRouteCache.size());
    }

    @Test
    public void testSetDependencies() throws Exception {
        TenantNetworkManager tenantNetworkManager = mock(TenantNetworkManager.class);
        ConfigurationService configurationService = mock(ConfigurationService.class);
        ArpProvider arpProvider = mock(ArpProvider.class);
        InboundNatProvider inboundNatProvider = mock(InboundNatProvider.class);
        OutboundNatProvider outboundNatProvider = mock(OutboundNatProvider.class);
        RoutingProvider routingProvider = mock(RoutingProvider.class);
        L3ForwardingProvider l3ForwardingProvider = mock(L3ForwardingProvider.class);
        NodeCacheManager nodeCacheManager = mock(NodeCacheManager.class);
        Southbound southbound = mock(Southbound.class);

        PowerMockito.mockStatic(ServiceHelper.class);
        PowerMockito.when(ServiceHelper.getGlobalInstance(TenantNetworkManager.class, neutronL3Adapter)).thenReturn(tenantNetworkManager);
        PowerMockito.when(ServiceHelper.getGlobalInstance(ConfigurationService.class, neutronL3Adapter)).thenReturn(configurationService);
        PowerMockito.when(ServiceHelper.getGlobalInstance(ArpProvider.class, neutronL3Adapter)).thenReturn(arpProvider);
        PowerMockito.when(ServiceHelper.getGlobalInstance(InboundNatProvider.class, neutronL3Adapter)).thenReturn(inboundNatProvider);
        PowerMockito.when(ServiceHelper.getGlobalInstance(OutboundNatProvider.class, neutronL3Adapter)).thenReturn(outboundNatProvider);
        PowerMockito.when(ServiceHelper.getGlobalInstance(RoutingProvider.class, neutronL3Adapter)).thenReturn(routingProvider);
        PowerMockito.when(ServiceHelper.getGlobalInstance(L3ForwardingProvider.class, neutronL3Adapter)).thenReturn(l3ForwardingProvider);
        PowerMockito.when(ServiceHelper.getGlobalInstance(NodeCacheManager.class, neutronL3Adapter)).thenReturn(nodeCacheManager);
        PowerMockito.when(ServiceHelper.getGlobalInstance(Southbound.class, neutronL3Adapter)).thenReturn(southbound);

        neutronL3Adapter.setDependencies(mock(BundleContext.class), mock(ServiceReference.class));

        assertEquals("Error, did not return the correct object", getField("tenantNetworkManager"), tenantNetworkManager);
        assertEquals("Error, did not return the correct object", getField("configurationService"), configurationService);
        assertEquals("Error, did not return the correct object", getField("arpProvider"), arpProvider);
        assertEquals("Error, did not return the correct object", getField("inboundNatProvider"), inboundNatProvider);
        assertEquals("Error, did not return the correct object", getField("outboundNatProvider"), outboundNatProvider);
        assertEquals("Error, did not return the correct object", getField("routingProvider"), routingProvider);
        assertEquals("Error, did not return the correct object", getField("l3ForwardingProvider"), l3ForwardingProvider);
        assertEquals("Error, did not return the correct object", getField("nodeCacheManager"), nodeCacheManager);
        assertEquals("Error, did not return the correct object", getField("southbound"), southbound);
    }

    @Test
    public void testSetDependenciesObject() throws Exception{
        INeutronNetworkCRUD iNeutronNetworkCRUD = mock(INeutronNetworkCRUD.class);
        neutronL3Adapter.setDependencies(iNeutronNetworkCRUD);
        assertEquals("Error, did not return the correct object", getField("neutronNetworkCache"), iNeutronNetworkCRUD);

        INeutronPortCRUD iNeutronPortCRUD = mock(INeutronPortCRUD.class);
        neutronL3Adapter.setDependencies(iNeutronPortCRUD);
        assertEquals("Error, did not return the correct object", getField("neutronPortCache"), iNeutronPortCRUD);

        INeutronSubnetCRUD iNeutronSubnetCRUD = mock(INeutronSubnetCRUD.class);
        neutronL3Adapter.setDependencies(iNeutronSubnetCRUD);
        assertEquals("Error, did not return the correct object", getField("neutronSubnetCache"), iNeutronSubnetCRUD);

        ArpProvider arpProvider = mock(ArpProvider.class);
        neutronL3Adapter.setDependencies(arpProvider);
        assertEquals("Error, did not return the correct object", getField("arpProvider"), arpProvider);

        InboundNatProvider inboundNatProvider = mock(InboundNatProvider.class);
        neutronL3Adapter.setDependencies(inboundNatProvider);
        assertEquals("Error, did not return the correct object", getField("inboundNatProvider"), inboundNatProvider);

        OutboundNatProvider outboundNatProvider = mock(OutboundNatProvider.class);
        neutronL3Adapter.setDependencies(outboundNatProvider);
        assertEquals("Error, did not return the correct object", getField("outboundNatProvider"), outboundNatProvider);

        RoutingProvider routingProvider = mock(RoutingProvider.class);
        neutronL3Adapter.setDependencies(routingProvider);
        assertEquals("Error, did not return the correct object", getField("routingProvider"), routingProvider);

        L3ForwardingProvider l3ForwardingProvider = mock(L3ForwardingProvider.class);
        neutronL3Adapter.setDependencies(l3ForwardingProvider);
        assertEquals("Error, did not return the correct object", getField("l3ForwardingProvider"), l3ForwardingProvider);
    }

    private Object getField(String fieldName) throws Exception {
        Field field = NeutronL3Adapter.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(neutronL3Adapter);
    }

    private Method getMethod(String methodName) throws Exception {
        Method method = neutronL3Adapter.getClass().getDeclaredMethod(methodName, new Class[] {});
        method.setAccessible(true);
        return method;
    }
}
