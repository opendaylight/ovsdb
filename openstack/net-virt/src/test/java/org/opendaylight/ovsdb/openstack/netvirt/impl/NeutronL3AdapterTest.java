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
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.junit.Before;
import org.junit.Ignore;
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
import org.opendaylight.ovsdb.openstack.netvirt.api.ConfigurationService;
import org.opendaylight.ovsdb.openstack.netvirt.api.Status;
import org.opendaylight.ovsdb.openstack.netvirt.api.TenantNetworkManager;
import org.opendaylight.ovsdb.utils.config.ConfigProperties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Unit test for {@link NeutronL3Adapter}
 */
/* TODO SB_MIGRATION */ @Ignore
@PrepareForTest(ConfigProperties.class)
@RunWith(PowerMockRunner.class)
public class NeutronL3AdapterTest {

    @InjectMocks NeutronL3Adapter neutronL3Adapter;

    @Mock private ConfigurationService configurationService;
    @Mock private TenantNetworkManager tenantNetworkManager;
    @Mock private INeutronNetworkCRUD neutronNetworkCache;
    @Mock private INeutronSubnetCRUD neutronSubnetCache;
    @Mock private INeutronPortCRUD neutronPortCache;
    @Mock NeutronPort neutronPort;

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
    private String enabledARP;

    private static final String HOST_ADDRESS = "127.0.0.1";

    @Before
    public void setUp() throws Exception{
        PowerMockito.mockStatic(ConfigProperties.class);
        PowerMockito.when(ConfigProperties.getProperty(neutronL3Adapter.getClass(), "ovsdb.l3.fwd.enabled")).thenReturn("yes");
        PowerMockito.when(ConfigProperties.getProperty(neutronL3Adapter.getClass(), "ovsdb.arp.responder.enabled")).thenReturn("yes");

        //neutronL3Adapter.init();

        this.getNeutronL3AdapterFields();
        this.setUpVar();
    }

    private void getNeutronL3AdapterFields() throws Exception{
        inboundIpRewriteCache = (Set<String>) getNeutronL3AdapterField("inboundIpRewriteCache");
        outboundIpRewriteCache = (Set<String>) getNeutronL3AdapterField("outboundIpRewriteCache");
        inboundIpRewriteExclusionCache = (Set<String>) getNeutronL3AdapterField("inboundIpRewriteExclusionCache");
        outboundIpRewriteExclusionCache = (Set<String>) getNeutronL3AdapterField("outboundIpRewriteExclusionCache");
        routerInterfacesCache = (Set<String>) getNeutronL3AdapterField("routerInterfacesCache");
        staticArpEntryCache = (Set<String>) getNeutronL3AdapterField("staticArpEntryCache");
        l3ForwardingCache = (Set<String>) getNeutronL3AdapterField("l3ForwardingCache");
        defaultRouteCache = (Set<String>) getNeutronL3AdapterField("defaultRouteCache");
        networkIdToRouterMacCache = (Map<String, String>) getNeutronL3AdapterField("networkIdToRouterMacCache");
        subnetIdToRouterInterfaceCache = (Map<String, NeutronRouter_Interface>) getNeutronL3AdapterField("subnetIdToRouterInterfaceCache");
        /* enabledARP = getNeutronL3AdapterField("enabledARP"); */ 
    }

    private Object getNeutronL3AdapterField(String fieldName) throws Exception {
        Field fieldObject = NeutronL3Adapter.class.getDeclaredField(fieldName);
        fieldObject.setAccessible(true);
        return fieldObject.get(neutronL3Adapter);
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
    }


    /**
     * Test method {@link NeutronL3Adapter#handleNeutronPortEvent(NeutronPort, Action)}
     * Device owner = network:router_interface
     */
    @Test
    public void testHandleNeutronPortEvent1() {
        when(neutronPort.getDeviceOwner()).thenReturn("network:router_interface");

        neutronL3Adapter.handleNeutronPortEvent(neutronPort, Action.ADD);
        // Affected by the add
        assertEquals("Error, did not return the correct networkIdToRouterMacCache size", 1, networkIdToRouterMacCache.size());
        assertEquals("Error, did not return the correct subnetIdToRouterInterfaceCache size", 1, subnetIdToRouterInterfaceCache.size());
        /* TODO SB_MIGRATION */
        //assertEquals("Error, did not return the correct routerInterfacesCache size", 2, routerInterfacesCache.size());
        assertEquals("Error, did not return the correct staticArpEntryCache size", 2, staticArpEntryCache.size());
        assertEquals("Error, did not return the correct inboundIpRewriteExclusionCache size", 1, inboundIpRewriteExclusionCache.size());
        assertEquals("Error, did not return the correct outboundIpRewriteExclusionCache size", 1, outboundIpRewriteExclusionCache.size());
        assertEquals("Error, did not return the correct l3ForwardingCache size", 1, l3ForwardingCache.size());
        // Unchanged
        assertEquals("Error, did not return the correct inboundIpRewriteCache size", 0, inboundIpRewriteCache.size());
        assertEquals("Error, did not return the correct outboundIpRewriteCache size", 0, outboundIpRewriteCache.size());
        assertEquals("Error, did not return the correct defaultRouteCache size", 0, defaultRouteCache.size());

        neutronL3Adapter.handleNeutronPortEvent(neutronPort, Action.DELETE);
        // Affected by the delete
        assertEquals("Error, did not return the correct networkIdToRouterMacCache size", 0, networkIdToRouterMacCache.size());
        assertEquals("Error, did not return the correct subnetIdToRouterInterfaceCache size", 0, subnetIdToRouterInterfaceCache.size());
        assertEquals("Error, did not return the correct staticArpEntryCache size", 1, staticArpEntryCache.size());
        // Unchanged
        assertEquals("Error, did not return the correct routerInterfacesCache size", 2, routerInterfacesCache.size());
        assertEquals("Error, did not return the correct inboundIpRewriteExclusionCache size", 1, inboundIpRewriteExclusionCache.size());
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
    @Test
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
        assertEquals("Error, did not return the correct inboundIpRewriteExclusionCache size", 1, inboundIpRewriteExclusionCache.size());
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
        assertEquals("Error, did not return the correct inboundIpRewriteExclusionCache size", 1, inboundIpRewriteExclusionCache.size());
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
    @Test
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
        assertEquals("Error, did not return the correct inboundIpRewriteExclusionCache size", 0, inboundIpRewriteExclusionCache.size());
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
        assertEquals("Error, did not return the correct inboundIpRewriteExclusionCache size", 0, inboundIpRewriteExclusionCache.size());
        assertEquals("Error, did not return the correct outboundIpRewriteExclusionCache size", 0, outboundIpRewriteExclusionCache.size());
        assertEquals("Error, did not return the correct subnetIdToRouterInterfaceCache size", 0, subnetIdToRouterInterfaceCache.size());
        assertEquals("Error, did not return the correct l3ForwardingCache size", 0, l3ForwardingCache.size());
        assertEquals("Error, did not return the correct defaultRouteCache size", 0, defaultRouteCache.size());
    }

}
