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
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
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
import org.opendaylight.ovsdb.openstack.netvirt.api.StatusCode;
import org.opendaylight.ovsdb.openstack.netvirt.api.TenantNetworkManager;
import org.opendaylight.ovsdb.utils.config.ConfigProperties;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberMatcher;
import org.powermock.api.support.membermodification.MemberModifier;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * Unit test for {@link NeutronL3Adapter}
 */
@PrepareForTest({ConfigProperties.class, ServiceHelper.class, InetAddress.class, NeutronL3Adapter.class})
@RunWith(PowerMockRunner.class)
public class NeutronL3AdapterTest {

    @Mock private NeutronL3Adapter neutronL3Adapter;
//    private NeutronL3Adapter neutronL3AdapterSpy;
    @Mock private ConfigurationService configurationService;
    @Mock private TenantNetworkManager tenantNetworkManager;
    @Mock private INeutronNetworkCRUD neutronNetworkCache;
    @Mock private INeutronSubnetCRUD neutronSubnetCache;
    @Mock private INeutronPortCRUD neutronPortCache;
    @Mock private NodeCacheManager nodeCacheManager;
    @Mock private L3ForwardingProvider l3ForwardingProvider;
    @Mock private InboundNatProvider inboundNatProvider;
    @Mock private OutboundNatProvider outboundNatProvider;
    @Mock private ArpProvider arpProvider;
    @Mock private RoutingProvider routingProvider;
    @Mock private Southbound southbound;

    @Mock private NeutronPort neutronPort;

    private Set<String> inboundIpRewriteCache;
    private Set<String> outboundIpRewriteCache;
    private Set<String> outboundIpRewriteExclusionCache;
    private Set<String> routerInterfacesCache;
    private Set<String> staticArpEntryCache;
    private Set<String> l3ForwardingCache;
    private Map<String, String> networkIdToRouterMacCache;
    private Map<String, List<Neutron_IPs>> networkIdToRouterIpListCache;
    private Map<String, NeutronRouter_Interface> subnetIdToRouterInterfaceCache;
    private Map<String, Pair<Long, Uuid>> neutronPortToDpIdCache;

    private Map floatIpDataMapCache;

    private static final String ID = "45";
    private static final String IP = "ip";
    private static final String INTF_NAME = "intf-name";
    private static final String EXTERNAL_ROUTER_MAC_UPDATE = "mac";
    private static final String UUID = "port-uudi";
    private static final String FIXED_IP_ADDRESS = "ip";
    private static final String FLOATING_IP_ADDRESS = "ip";
    private static final String OWNER_ROUTER_INTERFACE = "network:router_interface";
    private static final String MAC_ADDRESS = "mac-address";
    private static final String MAC_ADDRESS_2 = "mac-address-2";
    private static final String OWNER_FLOATING_IP = "network:floatingip";
    private static final String PORT_INT = "port_int";
    private static final String SEG_ID = "seg-id";


    private Class floatingIpClass;
    private Object floatingIpObject;

    @Before
    public void setUp() throws Exception{
        neutronL3Adapter = PowerMockito.mock(NeutronL3Adapter.class, Mockito.CALLS_REAL_METHODS);

        // init instance variables
        MemberModifier.field(NeutronL3Adapter.class, "enabled").set(neutronL3Adapter, true);

        floatingIpClass = Whitebox.getInnerClassType(NeutronL3Adapter.class, "FloatIpData");
        floatingIpObject = createFloatingIpObject();

//        PowerMockito.mockStatic(ConfigProperties.class);
//        PowerMockito.when(ConfigProperties.getProperty(neutronL3Adapter.getClass(), "ovsdb.l3.fwd.enabled")).thenReturn("yes");
//        PowerMockito.when(ConfigProperties.getProperty(neutronL3Adapter.getClass(), "ovsdb.l3.arp.responder.disabled")).thenReturn("no");

//        when(configurationService.isL3ForwardingEnabled()).thenReturn(true);

        neutronPortToDpIdCache = new HashMap<String, Pair<Long,Uuid>>();
        subnetIdToRouterInterfaceCache = new HashMap<String, NeutronRouter_Interface>();
        floatIpDataMapCache = new HashMap();
        neutronPortToDpIdCache = new HashMap<String, Pair<Long,Uuid>>();
        networkIdToRouterMacCache = new HashMap<String, String>();
        l3ForwardingCache = new HashSet<String>();
        networkIdToRouterMacCache = new HashMap<String, String>();
        networkIdToRouterIpListCache = new HashMap<String, List<Neutron_IPs>>();
    }

    @Test
    public void testUpdateExternalRouterMac() throws Exception {
        // Suppress the called to these functions
        MemberModifier.suppress(MemberMatcher.method(NeutronL3Adapter.class, "flushExistingIpRewrite"));
        MemberModifier.suppress(MemberMatcher.method(NeutronL3Adapter.class, "rebuildExistingIpRewrite"));

        neutronL3Adapter.updateExternalRouterMac(EXTERNAL_ROUTER_MAC_UPDATE);

        PowerMockito.verifyPrivate(neutronL3Adapter, times(1)).invoke("flushExistingIpRewrite");
        PowerMockito.verifyPrivate(neutronL3Adapter, times(1)).invoke("rebuildExistingIpRewrite");
    }

    @Test
    public void testhandleNeutronSubnetEvent() throws Exception {
        // Nothing to be done here
        neutronL3Adapter.handleNeutronSubnetEvent(mock(NeutronSubnet.class), Action.ADD);
    }

    @Test
    public void testHandleNeutronPortEvent() throws Exception {
        // Mock variables
        Neutron_IPs neutronIP = mock(Neutron_IPs.class);
        when(neutronIP.getSubnetUUID()).thenReturn(UUID);
        List<Neutron_IPs> list_neutronIP = new ArrayList<Neutron_IPs>();
        list_neutronIP.add(neutronIP);
        NeutronPort neutronPort = mock(NeutronPort.class);
        when(neutronPort.getDeviceOwner()).thenReturn(OWNER_ROUTER_INTERFACE);
        when(neutronPort.getFixedIPs()).thenReturn(list_neutronIP);

        // init instance variables
        MemberModifier.field(NeutronL3Adapter.class, "neutronPortCache").set(neutronL3Adapter , neutronPortCache);
        subnetIdToRouterInterfaceCache.put(UUID, mock(NeutronRouter_Interface.class));
        MemberModifier.field(NeutronL3Adapter.class, "subnetIdToRouterInterfaceCache").set(neutronL3Adapter , subnetIdToRouterInterfaceCache);

        // Suppress the called to these functions
        MemberModifier.suppress(MemberMatcher.method(NeutronL3Adapter.class, "updateL3ForNeutronPort", NeutronPort.class, boolean.class));
        Mockito.doNothing().when(neutronL3Adapter).handleNeutronRouterInterfaceEvent(any(NeutronRouter.class), any(NeutronRouter_Interface.class), any(Action.class));

        neutronL3Adapter.handleNeutronPortEvent(neutronPort, Action.ADD);
        Mockito.verify(neutronL3Adapter).handleNeutronRouterInterfaceEvent(any(NeutronRouter.class), any(NeutronRouter_Interface.class), eq(Action.ADD));

        when(neutronPort.getDeviceOwner()).thenReturn("");
        neutronL3Adapter.handleNeutronPortEvent(neutronPort, Action.ADD);
        Mockito.verify(neutronL3Adapter, times(2)).handleNeutronRouterInterfaceEvent(any(NeutronRouter.class), any(NeutronRouter_Interface.class), eq(Action.ADD));
        PowerMockito.verifyPrivate(neutronL3Adapter, times(1)).invoke("updateL3ForNeutronPort", any(NeutronPort.class), eq(false));

        neutronL3Adapter.handleNeutronPortEvent(neutronPort, Action.DELETE);
        PowerMockito.verifyPrivate(neutronL3Adapter, times(1)).invoke("updateL3ForNeutronPort", any(NeutronPort.class), eq(true));
    }

    @Test
    public void testhandleNeutronRouterEvent() throws Exception {
        // Nothing to be done here
        neutronL3Adapter.handleNeutronRouterEvent(mock(NeutronRouter.class), Action.ADD);
    }

    @Test
    public void testHandleNeutronRouterInterfaceEvent() throws Exception {
        // Mock variables
        NeutronRouter_Interface neutronRouterInterface = mock(NeutronRouter_Interface.class);
        when(neutronRouterInterface.getPortUUID()).thenReturn(UUID);
        when(neutronRouterInterface.getSubnetUUID()).thenReturn(UUID);

        Neutron_IPs neutronIP = mock(Neutron_IPs.class);
        when(neutronIP.getSubnetUUID()).thenReturn(UUID);
        NeutronPort neutronPort = mock(NeutronPort.class);
        List<Neutron_IPs> list_neutronIP = new ArrayList<Neutron_IPs>();
        list_neutronIP.add(neutronIP);
        when(neutronPort.getFixedIPs()).thenReturn(list_neutronIP);
        List<NeutronPort> list_neutronPort = new ArrayList<>();
        list_neutronPort.add(neutronPort);
        when(neutronPortCache.getAllPorts()).thenReturn(list_neutronPort);

        // init instance variables
        MemberModifier.field(NeutronL3Adapter.class, "neutronPortCache").set(neutronL3Adapter , neutronPortCache);

        // Suppress the called to these functions
        MemberModifier.suppress(MemberMatcher.method(NeutronL3Adapter.class, "programFlowsForNeutronRouterInterface", NeutronRouter_Interface.class, Boolean.class));
        MemberModifier.suppress(MemberMatcher.method(NeutronL3Adapter.class, "updateL3ForNeutronPort", NeutronPort.class, boolean.class));

        neutronL3Adapter.handleNeutronRouterInterfaceEvent(mock(NeutronRouter.class), neutronRouterInterface, Action.ADD);
        PowerMockito.verifyPrivate(neutronL3Adapter, times(1)).invoke("programFlowsForNeutronRouterInterface", any(NeutronRouter_Interface.class), eq(false));
        PowerMockito.verifyPrivate(neutronL3Adapter, times(1)).invoke("updateL3ForNeutronPort", any(NeutronPort.class), eq(false));

        neutronL3Adapter.handleNeutronRouterInterfaceEvent(mock(NeutronRouter.class), neutronRouterInterface, Action.DELETE);
        PowerMockito.verifyPrivate(neutronL3Adapter, times(1)).invoke("programFlowsForNeutronRouterInterface", any(NeutronRouter_Interface.class), eq(true));
        PowerMockito.verifyPrivate(neutronL3Adapter, times(1)).invoke("updateL3ForNeutronPort", any(NeutronPort.class), eq(true));
    }

    @Test
    public void testHandleNeutronFloatingIPEvent() throws Exception {
        // Mock variables
        NeutronFloatingIP neutronFloatingIP = mock(NeutronFloatingIP.class);
        when(neutronFloatingIP.getFixedIPAddress()).thenReturn(FIXED_IP_ADDRESS);
        when(neutronFloatingIP.getFloatingIPAddress()).thenReturn(FLOATING_IP_ADDRESS);
        when(neutronFloatingIP.getFloatingIPUUID()).thenReturn(UUID);

        // Suppress the called to these functions
        MemberModifier.suppress(MemberMatcher.method(NeutronL3Adapter.class, "programFlowsForFloatingIPArpAdd", NeutronFloatingIP.class));
        MemberModifier.suppress(MemberMatcher.method(NeutronL3Adapter.class, "programFlowsForFloatingIPInbound", NeutronFloatingIP.class, Action.class));
        MemberModifier.suppress(MemberMatcher.method(NeutronL3Adapter.class, "programFlowsForFloatingIPOutbound", NeutronFloatingIP.class, Action.class));
        MemberModifier.suppress(MemberMatcher.method(NeutronL3Adapter.class, "programFlowsForFloatingIPArpDelete", String.class));

        neutronL3Adapter.handleNeutronFloatingIPEvent(neutronFloatingIP, Action.ADD);
        PowerMockito.verifyPrivate(neutronL3Adapter, times(1)).invoke("programFlowsForFloatingIPArpAdd", any(NeutronFloatingIP.class));
        PowerMockito.verifyPrivate(neutronL3Adapter, times(1)).invoke("programFlowsForFloatingIPInbound", any(NeutronFloatingIP.class), eq(Action.ADD));
        PowerMockito.verifyPrivate(neutronL3Adapter, times(1)).invoke("programFlowsForFloatingIPOutbound", any(NeutronFloatingIP.class), eq(Action.ADD));

        neutronL3Adapter.handleNeutronFloatingIPEvent(neutronFloatingIP, Action.DELETE);
        PowerMockito.verifyPrivate(neutronL3Adapter, times(1)).invoke("programFlowsForFloatingIPArpDelete", anyString());
        PowerMockito.verifyPrivate(neutronL3Adapter, times(1)).invoke("programFlowsForFloatingIPInbound", any(NeutronFloatingIP.class), eq(Action.DELETE));
        PowerMockito.verifyPrivate(neutronL3Adapter, times(1)).invoke("programFlowsForFloatingIPOutbound", any(NeutronFloatingIP.class), eq(Action.DELETE));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testProgramFlowsForFloatingIPInbound() throws Exception {
        NeutronFloatingIP neutronFloatingIp = mock(NeutronFloatingIP.class);
        when(neutronFloatingIp.getID()).thenReturn(ID);

        // init instance variables
        floatIpDataMapCache.put(ID, floatingIpObject);
        MemberModifier.field(NeutronL3Adapter.class, "floatIpDataMapCache").set(neutronL3Adapter , floatIpDataMapCache);

        // Suppress the called to these functions
        MemberModifier.suppress(MemberMatcher.method(NeutronL3Adapter.class, "programInboundIpRewriteStage1", Long.class, Long.class, String.class, String.class, String.class, Action.class));

        Whitebox.invokeMethod(neutronL3Adapter, "programFlowsForFloatingIPInbound", neutronFloatingIp, Action.ADD);
        PowerMockito.verifyPrivate(neutronL3Adapter, times(1)).invoke("programInboundIpRewriteStage1", anyLong(), anyLong(), anyString(), anyString(), anyString(), eq(Action.ADD));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testProgramFlowsForFloatingIPOutbound() throws Exception {
        NeutronFloatingIP neutronFloatingIp = mock(NeutronFloatingIP.class);
        when(neutronFloatingIp.getID()).thenReturn(ID);

        // init instance variables
        floatIpDataMapCache.put(ID, floatingIpObject);
        MemberModifier.field(NeutronL3Adapter.class, "floatIpDataMapCache").set(neutronL3Adapter , floatIpDataMapCache);

        // Suppress the called to these functions
        MemberModifier.suppress(MemberMatcher.method(NeutronL3Adapter.class, "programOutboundIpRewriteStage1", floatingIpClass, Action.class));

        Whitebox.invokeMethod(neutronL3Adapter, "programFlowsForFloatingIPOutbound", neutronFloatingIp, Action.ADD);
        PowerMockito.verifyPrivate(neutronL3Adapter, times(1)).invoke("programOutboundIpRewriteStage1", any(floatingIpClass), eq(Action.ADD));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testFlushExistingIpRewrite() throws Exception {
        // init instance variables
        floatIpDataMapCache.put(ID, floatingIpObject);
        MemberModifier.field(NeutronL3Adapter.class, "floatIpDataMapCache").set(neutronL3Adapter , floatIpDataMapCache);

        // Suppress the called to these functions
        MemberModifier.suppress(MemberMatcher.method(NeutronL3Adapter.class, "programOutboundIpRewriteStage1", floatingIpClass, Action.class));

        Whitebox.invokeMethod(neutronL3Adapter, "flushExistingIpRewrite");
        PowerMockito.verifyPrivate(neutronL3Adapter, times(1)).invoke("programOutboundIpRewriteStage1", any(floatingIpClass), eq(Action.DELETE));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRebuildExistingIpRewrite() throws Exception {
        // init instance variables
        floatIpDataMapCache.put(ID, floatingIpObject);
        MemberModifier.field(NeutronL3Adapter.class, "floatIpDataMapCache").set(neutronL3Adapter , floatIpDataMapCache);

        // Suppress the called to these functions
        MemberModifier.suppress(MemberMatcher.method(NeutronL3Adapter.class, "programOutboundIpRewriteStage1", floatingIpClass, Action.class));

        Whitebox.invokeMethod(neutronL3Adapter, "rebuildExistingIpRewrite");
        PowerMockito.verifyPrivate(neutronL3Adapter, times(1)).invoke("programOutboundIpRewriteStage1", any(floatingIpClass), eq(Action.ADD));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testProgramFlowsForFloatingIPArpAdd() throws Exception {
        NeutronFloatingIP neutronFloatingIP = mock(NeutronFloatingIP.class);
        when(neutronFloatingIP.getFixedIPAddress()).thenReturn(FIXED_IP_ADDRESS);
        when(neutronFloatingIP.getFloatingIPAddress()).thenReturn(FLOATING_IP_ADDRESS);
        when(neutronFloatingIP.getPortUUID()).thenReturn(UUID);
        NeutronPort neutronPort = mock(NeutronPort.class);
        when(neutronPort.getMacAddress()).thenReturn(MAC_ADDRESS);
        NeutronNetwork neutronNetwork = mock(NeutronNetwork.class);
        when(neutronNetwork.getProviderSegmentationID()).thenReturn(ID);
        when(neutronNetwork.getID()).thenReturn(ID);

        // init instance variables
        floatIpDataMapCache.put(ID, floatingIpObject);
        neutronPortToDpIdCache.put(UUID, mock(Pair.class));
        networkIdToRouterMacCache.put(ID, MAC_ADDRESS);
        when(neutronNetworkCache.getNetwork(anyString())).thenReturn(neutronNetwork);
        when(neutronPortCache.getPort(anyString())).thenReturn(neutronPort);
        PowerMockito.doReturn(neutronPort).when(neutronL3Adapter, "findNeutronPortForFloatingIp", anyString());
        PowerMockito.doReturn(Long.valueOf(15)).when(neutronL3Adapter, "findOFPortForExtPatch", anyLong());
        MemberModifier.field(NeutronL3Adapter.class, "floatIpDataMapCache").set(neutronL3Adapter , floatIpDataMapCache);
        MemberModifier.field(NeutronL3Adapter.class, "neutronPortToDpIdCache").set(neutronL3Adapter , neutronPortToDpIdCache);
        MemberModifier.field(NeutronL3Adapter.class, "neutronPortCache").set(neutronL3Adapter , neutronPortCache);
        MemberModifier.field(NeutronL3Adapter.class, "neutronNetworkCache").set(neutronL3Adapter , neutronNetworkCache);
        MemberModifier.field(NeutronL3Adapter.class, "networkIdToRouterMacCache").set(neutronL3Adapter , networkIdToRouterMacCache);

        // Suppress the called to these functions
        MemberModifier.suppress(MemberMatcher.method(NeutronL3Adapter.class, "findNeutronPortForFloatingIp", String.class));
        MemberModifier.suppress(MemberMatcher.method(NeutronL3Adapter.class, "findOFPortForExtPatch", Long.class));
        MemberModifier.suppress(MemberMatcher.method(NeutronL3Adapter.class, "programStaticArpStage1", Long.class, String.class, String.class, String.class, Action.class));

        Whitebox.invokeMethod(neutronL3Adapter, "programFlowsForFloatingIPArpAdd", neutronFloatingIP);
        PowerMockito.verifyPrivate(neutronL3Adapter, times(1)).invoke("findNeutronPortForFloatingIp", anyString());
        PowerMockito.verifyPrivate(neutronL3Adapter, times(1)).invoke("findOFPortForExtPatch", anyLong());
        PowerMockito.verifyPrivate(neutronL3Adapter, times(1)).invoke("programStaticArpStage1", anyLong(), anyString(), anyString(), anyString(), eq(Action.ADD));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testProgramFlowsForFloatingIPArpDelete() throws Exception {
     // init instance variables
        floatIpDataMapCache.put(ID, floatingIpObject);
        MemberModifier.field(NeutronL3Adapter.class, "floatIpDataMapCache").set(neutronL3Adapter , floatIpDataMapCache);

        MemberModifier.suppress(MemberMatcher.method(NeutronL3Adapter.class, "programStaticArpStage1", Long.class, String.class, String.class, String.class, Action.class));

        Whitebox.invokeMethod(neutronL3Adapter, "programFlowsForFloatingIPArpDelete", ID);
        PowerMockito.verifyPrivate(neutronL3Adapter, times(1)).invoke("programStaticArpStage1", anyLong(), anyString(), anyString(), anyString(), eq(Action.DELETE));
    }

    @Test
    public void testFindNeutronPortForFloatingIp() throws Exception {
        NeutronPort neutronPort = mock(NeutronPort.class);
        when(neutronPort.getDeviceOwner()).thenReturn(OWNER_FLOATING_IP);
        when(neutronPort.getDeviceID()).thenReturn(ID);
        List<NeutronPort> list_neutronPort = new ArrayList<>();
        list_neutronPort.add(neutronPort);
        when(neutronPortCache.getAllPorts()).thenReturn(list_neutronPort);

        MemberModifier.field(NeutronL3Adapter.class, "neutronPortCache").set(neutronL3Adapter , neutronPortCache);

        assertEquals("Error, did not return the correct NeutronPort", neutronPort, Whitebox.invokeMethod(neutronL3Adapter, "findNeutronPortForFloatingIp", ID));
    }

    @Test
    public void testFindOFPortForExtPatch() throws Exception {

        when(configurationService.getPatchPortName(any(Pair.class))).thenReturn(PORT_INT);
        MemberModifier.field(NeutronL3Adapter.class, "configurationService").set(neutronL3Adapter , configurationService);
        List<Node> nodes = new ArrayList<Node>();
        nodes.add(mock(Node.class));
        when(nodeCacheManager.getBridgeNodes()).thenReturn(nodes);
        MemberModifier.field(NeutronL3Adapter.class, "nodeCacheManager").set(neutronL3Adapter , nodeCacheManager);
        when(southbound.getDataPathId(any(Node.class))).thenReturn(Long.valueOf(ID));
        OvsdbTerminationPointAugmentation terminationPointOfBridge = mock(OvsdbTerminationPointAugmentation.class);
        when(terminationPointOfBridge.getOfport()).thenReturn(Long.valueOf(ID));
        when(southbound.getTerminationPointOfBridge(any(Node.class), anyString())).thenReturn(terminationPointOfBridge);
        MemberModifier.field(NeutronL3Adapter.class, "southbound").set(neutronL3Adapter , southbound);

        assertEquals("Error, did not return the correct NeutronPort", Long.valueOf(ID), Whitebox.invokeMethod(neutronL3Adapter, "findOFPortForExtPatch", Long.valueOf(ID)));
    }

    @Test
    public void testHandleNeutronNetworkEvent() throws Exception {
        // Nothing to be done here
        Whitebox.invokeMethod(neutronL3Adapter, "handleNeutronNetworkEvent", mock(NeutronNetwork.class), Action.ADD);
    }

    @Test
    public void testHandleInterfaceEvent() throws Exception {
        // init instance variables
        MemberModifier.field(NeutronL3Adapter.class, "tenantNetworkManager").set(neutronL3Adapter , tenantNetworkManager);
        MemberModifier.field(NeutronL3Adapter.class, "neutronPortToDpIdCache").set(neutronL3Adapter , neutronPortToDpIdCache);

        // Mock variables
        NodeId nodeId = mock(NodeId.class);
        when(nodeId.getValue()).thenReturn(ID);
        Node node = mock(Node.class);
        when(node.getNodeId()).thenReturn(nodeId);

        OvsdbTerminationPointAugmentation intf = mock(OvsdbTerminationPointAugmentation.class);
        when(intf.getInterfaceUuid()).thenReturn(mock(Uuid.class));
        when(intf.getName()).thenReturn(INTF_NAME);

        NeutronPort neutronPort = mock(NeutronPort.class);
        when(neutronPort.getPortUUID()).thenReturn(UUID);

        when(tenantNetworkManager.getTenantPort(intf)).thenReturn(neutronPort);

        // Suppress the called to these functions
        MemberModifier.suppress(MemberMatcher.method(NeutronL3Adapter.class, "getDpidForIntegrationBridge", Node.class));
        MemberModifier.suppress(MemberMatcher.method(NeutronL3Adapter.class, "handleInterfaceEventAdd", String.class, Long.class, Uuid.class));
        MemberModifier.suppress(MemberMatcher.method(NeutronL3Adapter.class, "handleInterfaceEventDelete", OvsdbTerminationPointAugmentation.class, Long.class));

        PowerMockito.when(neutronL3Adapter, "getDpidForIntegrationBridge", any(Node.class)).thenReturn(Long.valueOf(45));
        Mockito.doNothing().when(neutronL3Adapter).handleNeutronPortEvent(any(NeutronPort.class), any(Action.class));

        neutronL3Adapter.handleInterfaceEvent(node, intf, mock(NeutronNetwork.class), Action.ADD);
        PowerMockito.verifyPrivate(neutronL3Adapter, times(1)).invoke("getDpidForIntegrationBridge", any(Node.class));
        PowerMockito.verifyPrivate(neutronL3Adapter, times(1)).invoke("handleInterfaceEventAdd", any(String.class), anyLong(), any(Uuid.class));
        Mockito.verify(neutronL3Adapter).handleNeutronPortEvent(neutronPort, Action.ADD);

        neutronL3Adapter.handleInterfaceEvent(node, intf, mock(NeutronNetwork.class), Action.DELETE);
        PowerMockito.verifyPrivate(neutronL3Adapter, times(1)).invoke("handleInterfaceEventDelete", any(OvsdbTerminationPointAugmentation.class), anyLong());
        Mockito.verify(neutronL3Adapter).handleNeutronPortEvent(neutronPort, Action.DELETE);
    }

    @Test
    public void testHandleInterfaceEventAdd() throws Exception {
        // init instance variables
        MemberModifier.field(NeutronL3Adapter.class, "neutronPortToDpIdCache").set(neutronL3Adapter , neutronPortToDpIdCache);
        int temp = neutronPortToDpIdCache.size();

        Whitebox.invokeMethod(neutronL3Adapter, "handleInterfaceEventAdd", "", Long.valueOf(5), mock(Uuid.class));

        assertEquals("Error, did not add the port", temp+1, neutronPortToDpIdCache.size());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testHandleInterfaceEventDelete() throws Exception {
        OvsdbTerminationPointAugmentation intf = mock(OvsdbTerminationPointAugmentation.class);
        Uuid uuid = mock(Uuid.class);
        when(intf.getInterfaceUuid()).thenReturn(uuid );
        Pair<Long, Uuid> pair = mock(Pair.class);
        when(pair.getRight()).thenReturn(uuid);

        // init instance variables
        neutronPortToDpIdCache.put("key", pair);
        MemberModifier.field(NeutronL3Adapter.class, "neutronPortToDpIdCache").set(neutronL3Adapter , neutronPortToDpIdCache);
        int temp = neutronPortToDpIdCache.size();

        Whitebox.invokeMethod(neutronL3Adapter, "handleInterfaceEventDelete", intf, Long.valueOf(ID));

        assertEquals("Error, did not remove the port", temp-1, neutronPortToDpIdCache.size());
    }

    @Test
    public void testUpdateL3ForNeutronPort() throws Exception {
        Neutron_IPs neutronIp = mock(Neutron_IPs.class);
        when(neutronIp.getIpAddress()).thenReturn(FIXED_IP_ADDRESS);
        List<Neutron_IPs> neutronIps = new ArrayList<Neutron_IPs>();
        neutronIps.add(neutronIp);
        NeutronPort neutronPort = mock(NeutronPort.class);
        when(neutronPort.getNetworkUUID()).thenReturn(UUID);
        when(neutronPort.getMacAddress()).thenReturn(MAC_ADDRESS_2);
        when(neutronPort.getFixedIPs()).thenReturn(neutronIps);
        NeutronNetwork neutronNetwork = mock(NeutronNetwork.class);
        when(neutronNetwork.getProviderSegmentationID()).thenReturn(ID);
        List<Node> nodes = new ArrayList<Node>();
        nodes.add(mock(Node.class));
        PowerMockito.doReturn(Long.valueOf(15)).when(neutronL3Adapter, "getDpidForIntegrationBridge", any(Node.class));

        // init instance variables
        networkIdToRouterMacCache.put(UUID, MAC_ADDRESS);
        MemberModifier.field(NeutronL3Adapter.class, "networkIdToRouterMacCache").set(neutronL3Adapter , networkIdToRouterMacCache);
        when(neutronNetworkCache.getNetwork(anyString())).thenReturn(neutronNetwork);
        MemberModifier.field(NeutronL3Adapter.class, "neutronNetworkCache").set(neutronL3Adapter , neutronNetworkCache);
        when(nodeCacheManager.getBridgeNodes()).thenReturn(nodes);
        MemberModifier.field(NeutronL3Adapter.class, "nodeCacheManager").set(neutronL3Adapter , nodeCacheManager);
        MemberModifier.field(NeutronL3Adapter.class, "flgDistributedARPEnabled").set(neutronL3Adapter , true);

        // Suppress the called to these functions
        MemberModifier.suppress(MemberMatcher.method(NeutronL3Adapter.class, "getDpidForIntegrationBridge", Node.class));
        MemberModifier.suppress(MemberMatcher.method(NeutronL3Adapter.class, "programL3ForwardingStage1", Node.class, Long.class, String.class, String.class, String.class, Action.class));
        MemberModifier.suppress(MemberMatcher.method(NeutronL3Adapter.class, "programStaticArpStage1", Long.class, String.class, String.class, String.class, Action.class));

        Whitebox.invokeMethod(neutronL3Adapter, "updateL3ForNeutronPort", neutronPort, false);
        PowerMockito.verifyPrivate(neutronL3Adapter, times(1)).invoke("getDpidForIntegrationBridge", any(Node.class));
        PowerMockito.verifyPrivate(neutronL3Adapter, times(1)).invoke("programL3ForwardingStage1", any(Node.class), anyLong(), anyString(), anyString(), anyString(), eq(Action.ADD));
        PowerMockito.verifyPrivate(neutronL3Adapter, times(1)).invoke("programStaticArpStage1", anyLong(), anyString(), anyString(), anyString(), eq(Action.ADD));

        Whitebox.invokeMethod(neutronL3Adapter, "updateL3ForNeutronPort", neutronPort, true);
        PowerMockito.verifyPrivate(neutronL3Adapter, times(2)).invoke("getDpidForIntegrationBridge", any(Node.class));
        PowerMockito.verifyPrivate(neutronL3Adapter, times(1)).invoke("programL3ForwardingStage1", any(Node.class), anyLong(), anyString(), anyString(), anyString(), eq(Action.DELETE));
        PowerMockito.verifyPrivate(neutronL3Adapter, times(1)).invoke("programStaticArpStage1", anyLong(), anyString(), anyString(), anyString(), eq(Action.DELETE));
    }

    // either add or remove item in l3ForwardingCache
    @Test
    public void testProgramL3ForwardingStage1() throws Exception {
        NodeId nodeId = mock(NodeId.class);
        when(nodeId.getValue()).thenReturn(ID);
        Node node = mock(Node.class);
        when(node.getNodeId()).thenReturn(nodeId);

        // Suppress the called to these functions
        MemberModifier.suppress(MemberMatcher.method(NeutronL3Adapter.class, "programL3ForwardingStage2", Node.class, Long.class, String.class, String.class, String.class, Action.class));

        // init instance variables
        MemberModifier.field(NeutronL3Adapter.class, "l3ForwardingCache").set(neutronL3Adapter , l3ForwardingCache);
        PowerMockito.when(neutronL3Adapter, "programL3ForwardingStage2", any(Node.class), anyLong(), anyString(), anyString(), anyString(), eq(Action.ADD)).thenReturn(new Status(StatusCode.SUCCESS));
        PowerMockito.when(neutronL3Adapter, "programL3ForwardingStage2", any(Node.class), anyLong(), anyString(), anyString(), anyString(), eq(Action.DELETE)).thenReturn(new Status(StatusCode.SUCCESS));

        int temp = l3ForwardingCache.size();

        Whitebox.invokeMethod(neutronL3Adapter, "programL3ForwardingStage1", node, Long.valueOf(45), SEG_ID, MAC_ADDRESS, IP, Action.ADD);
        PowerMockito.verifyPrivate(neutronL3Adapter, times(1)).invoke("programL3ForwardingStage2", any(Node.class), anyLong(), anyString(), anyString(), anyString(), eq(Action.ADD));
        assertEquals("Error, did not add the value", temp, l3ForwardingCache.size()-1);

        l3ForwardingCache.add(node.getNodeId().getValue() + ":" + SEG_ID + ":" + IP);
        temp = l3ForwardingCache.size();

        Whitebox.invokeMethod(neutronL3Adapter, "programL3ForwardingStage1", node, Long.valueOf(45), SEG_ID, MAC_ADDRESS, IP, Action.DELETE);
        PowerMockito.verifyPrivate(neutronL3Adapter, times(1)).invoke("programL3ForwardingStage2", any(Node.class), anyLong(), anyString(), anyString(), anyString(), eq(Action.DELETE));
        assertEquals("Error, did not delete the value", temp, l3ForwardingCache.size()+1);
    }

    @Test
    public void testProgramL3ForwardingStage2() throws Exception {
        NodeId nodeId = mock(NodeId.class);
        when(nodeId.getValue()).thenReturn(ID);
        Node node = mock(Node.class);
        when(node.getNodeId()).thenReturn(nodeId);

        assertEquals("Error, this not return the correct status code", new Status(StatusCode.BADREQUEST), Whitebox.invokeMethod(neutronL3Adapter, "programL3ForwardingStage2", node, Long.valueOf(45), SEG_ID, MAC_ADDRESS, IP, Action.ADD));

        PowerMockito.mockStatic(InetAddress.class);
        InetAddress inetAddress = mock(InetAddress.class);
        PowerMockito.when(InetAddress.getByName(anyString())).thenReturn(inetAddress);

        assertEquals("Error, this not return the correct status code", new Status(StatusCode.SUCCESS), Whitebox.invokeMethod(neutronL3Adapter, "programL3ForwardingStage2", node, Long.valueOf(45), SEG_ID, MAC_ADDRESS, IP, Action.ADD));
    }

    @Test
    public void testProgramFlowsForNeutronRouterInterface() throws Exception {
        NeutronRouter_Interface intf = mock(NeutronRouter_Interface.class);
        when(intf.getPortUUID()).thenReturn(UUID);
        when(intf.getSubnetUUID()).thenReturn(UUID);
        Neutron_IPs neutronIp = mock(Neutron_IPs.class);
        when(neutronIp.getIpAddress()).thenReturn(FIXED_IP_ADDRESS);
        List<Neutron_IPs> ips = new ArrayList<Neutron_IPs>();
        ips.add(neutronIp);
        NeutronPort neutronPort = mock(NeutronPort.class);
        when(neutronPort.getMacAddress()).thenReturn(MAC_ADDRESS);
        when(neutronPort.getFixedIPs()).thenReturn(ips);
        NeutronSubnet neutronSubnet = mock(NeutronSubnet.class);
        when(neutronSubnet.getNetworkUUID()).thenReturn(UUID);
        when(neutronSubnet.getGatewayIP()).thenReturn(IP);
        when(neutronSubnet.getCidr()).thenReturn("cidr");
        NeutronNetwork neutronNetwork = mock(NeutronNetwork.class);
        when(neutronNetwork.getProviderSegmentationID()).thenReturn(ID);
        when(neutronNetwork.getRouterExternal()).thenReturn(false); //might change that to true
        when(neutronNetwork.getNetworkUUID()).thenReturn(UUID);

        Node node = mock(Node.class);
        List<Node> nodes = new ArrayList<Node>();
        nodes.add(node);

        MemberModifier.suppress(MemberMatcher.method(NeutronL3Adapter.class, "getDpidForIntegrationBridge", Node.class));
        MemberModifier.suppress(MemberMatcher.method(NeutronL3Adapter.class, "programFlowsForNeutronRouterInterfacePair", Node.class, Long.class, NeutronRouter_Interface.class, NeutronRouter_Interface.class, NeutronNetwork.class, String.class, String.class, String.class, int.class, Action.class, Boolean.class));
        MemberModifier.suppress(MemberMatcher.method(NeutronL3Adapter.class, "programFlowForNetworkFromExternal", Node.class, Long.class, String.class, String.class, String.class, int.class, Action.class));
        MemberModifier.suppress(MemberMatcher.method(NeutronL3Adapter.class, "programStaticArpStage1", Long.class, String.class, String.class, String.class, Action.class));
        MemberModifier.suppress(MemberMatcher.method(NeutronL3Adapter.class, "programIpRewriteExclusionStage1", Node.class, Long.class, String.class, String.class, Action.class));

        // init instance variables
        PowerMockito.when(neutronPortCache.getPort(anyString())).thenReturn(neutronPort);
        PowerMockito.when(neutronSubnetCache.getSubnet(anyString())).thenReturn(neutronSubnet);
        PowerMockito.when(neutronNetworkCache.getNetwork(anyString())).thenReturn(neutronNetwork);
        PowerMockito.when(nodeCacheManager.getBridgeNodes()).thenReturn(nodes);
        MemberModifier.field(NeutronL3Adapter.class, "networkIdToRouterMacCache").set(neutronL3Adapter , networkIdToRouterMacCache);
        MemberModifier.field(NeutronL3Adapter.class, "networkIdToRouterIpListCache").set(neutronL3Adapter , networkIdToRouterIpListCache);
        MemberModifier.field(NeutronL3Adapter.class, "subnetIdToRouterInterfaceCache").set(neutronL3Adapter , subnetIdToRouterInterfaceCache);
        MemberModifier.field(NeutronL3Adapter.class, "neutronPortCache").set(neutronL3Adapter , neutronPortCache);
        MemberModifier.field(NeutronL3Adapter.class, "neutronSubnetCache").set(neutronL3Adapter , neutronSubnetCache);
        MemberModifier.field(NeutronL3Adapter.class, "neutronNetworkCache").set(neutronL3Adapter , neutronNetworkCache);
        MemberModifier.field(NeutronL3Adapter.class, "nodeCacheManager").set(neutronL3Adapter , nodeCacheManager);
        PowerMockito.when(neutronL3Adapter, "getDpidForIntegrationBridge", any(Node.class)).thenReturn(Long.valueOf(45));

        int networkIdToRouterMacCacheSize, networkIdToRouterIpListCacheSize, subnetIdToRouterInterfaceCacheSize;
        networkIdToRouterMacCacheSize = networkIdToRouterMacCache.size();
        networkIdToRouterIpListCacheSize = networkIdToRouterIpListCache.size();
        subnetIdToRouterInterfaceCacheSize = subnetIdToRouterInterfaceCache.size();

        Whitebox.invokeMethod(neutronL3Adapter, "programFlowsForNeutronRouterInterface", intf, false);

        PowerMockito.verifyPrivate(neutronL3Adapter, times(1)).invoke("getDpidForIntegrationBridge", any(Node.class));
        PowerMockito.verifyPrivate(neutronL3Adapter, times(1)).invoke("programFlowsForNeutronRouterInterfacePair", any(Node.class), anyLong(), any(NeutronRouter_Interface.class), any(NeutronRouter_Interface.class), any(NeutronNetwork.class), anyString(), anyString(), anyString(), anyInt(), eq(Action.ADD), anyBoolean());
        PowerMockito.verifyPrivate(neutronL3Adapter, times(1)).invoke("programFlowForNetworkFromExternal", any(Node.class), anyLong(), anyString(), anyString(), anyString(), anyInt(), eq(Action.ADD));
        PowerMockito.verifyPrivate(neutronL3Adapter, times(1)).invoke("programStaticArpStage1", anyLong(), anyString(), anyString(), anyString(), eq(Action.ADD));
        PowerMockito.verifyPrivate(neutronL3Adapter, times(1)).invoke("programIpRewriteExclusionStage1", any(Node.class), anyLong(), anyString(), anyString(), eq(Action.ADD));
        assertEquals("Error, did not add the RouterMac", networkIdToRouterMacCacheSize, networkIdToRouterMacCache.size() -1);
        assertEquals("Error, did not add the RouterIP", networkIdToRouterIpListCacheSize, networkIdToRouterIpListCache.size() -1);
        assertEquals("Error, did not add the RouterInterface", subnetIdToRouterInterfaceCacheSize, subnetIdToRouterInterfaceCache.size() -1);

        networkIdToRouterMacCache.put(UUID, MAC_ADDRESS);
        networkIdToRouterIpListCache.put(UUID, ips);
        networkIdToRouterMacCacheSize = networkIdToRouterMacCache.size();
        networkIdToRouterIpListCacheSize = networkIdToRouterIpListCache.size();
        subnetIdToRouterInterfaceCacheSize = subnetIdToRouterInterfaceCache.size();

        Whitebox.invokeMethod(neutronL3Adapter, "programFlowsForNeutronRouterInterface", intf, true);

        PowerMockito.verifyPrivate(neutronL3Adapter, times(2)).invoke("getDpidForIntegrationBridge", any(Node.class));
        PowerMockito.verifyPrivate(neutronL3Adapter, times(1)).invoke("programFlowsForNeutronRouterInterfacePair", any(Node.class), anyLong(), any(NeutronRouter_Interface.class), any(NeutronRouter_Interface.class), any(NeutronNetwork.class), anyString(), anyString(), anyString(), anyInt(), eq(Action.DELETE), anyBoolean());
        PowerMockito.verifyPrivate(neutronL3Adapter, times(1)).invoke("programFlowForNetworkFromExternal", any(Node.class), anyLong(), anyString(), anyString(), anyString(), anyInt(), eq(Action.DELETE));
        PowerMockito.verifyPrivate(neutronL3Adapter, times(1)).invoke("programStaticArpStage1", anyLong(), anyString(), anyString(), anyString(), eq(Action.DELETE));
        PowerMockito.verifyPrivate(neutronL3Adapter, times(1)).invoke("programIpRewriteExclusionStage1", any(Node.class), anyLong(), anyString(), anyString(), eq(Action.DELETE));
        assertEquals("Error, did not remove the RouterMac", networkIdToRouterMacCacheSize, networkIdToRouterMacCache.size() +1);
        assertEquals("Error, did not remove the RouterIP", networkIdToRouterIpListCacheSize, networkIdToRouterIpListCache.size() +1);
        assertEquals("Error, did not remove the RouterInterface", subnetIdToRouterInterfaceCacheSize, subnetIdToRouterInterfaceCache.size() +1);
    }

    @Test
    public void testProgramFlowForNetworkFromExternal() throws Exception {
        MemberModifier.suppress(MemberMatcher.method(NeutronL3Adapter.class, "programRouterInterfaceStage1", Node.class, Long.class, String.class, String.class, String.class, String.class, int.class, Action.class));

        Whitebox.invokeMethod(neutronL3Adapter, "programFlowForNetworkFromExternal", mock(Node.class), Long.valueOf(12), "", "", "", 4, Action.ADD);

        PowerMockito.verifyPrivate(neutronL3Adapter, times(1)).invoke("programRouterInterfaceStage1", any(Node.class), anyLong(), anyString(), anyString(), anyString(), anyString(), anyInt(), any(Action.class));
    }

    @Test
    public void testProgramFlowsForNeutronRouterInterfacePair() throws Exception {

    }

    @Test
    public void testProgramRouterInterfaceStage1() throws Exception {

    }

    // either add or remove routerInterfacesCache
    @Test
    public void testProgramRouterInterfaceStage2() throws Exception {
//        PowerMockito.mockStatic(InetAddress.class);
//        InetAddress inetAddress = mock(InetAddress.class);
//        PowerMockito.when(InetAddress.getByName(anyString())).thenReturn(inetAddress);
    }

    //either add or remove staticArpEntryCache
    @Test
    public void testProgramStaticArpStage1() throws Exception {

    }

    @Test
    public void testProgramStaticArpStage2() throws Exception {
//        PowerMockito.mockStatic(InetAddress.class);
//        InetAddress inetAddress = mock(InetAddress.class);
//        PowerMockito.when(InetAddress.getByName(anyString())).thenReturn(inetAddress);
    }

    // either add or remove inboundIpRewriteCache
    @Test
    public void testProgramInboundIpRewriteStage1() throws Exception {

    }

    @Test
    public void testProgramInboundIpRewriteStage2() throws Exception {
//        PowerMockito.mockStatic(InetAddress.class);
//        InetAddress inetAddress = mock(InetAddress.class);
//        PowerMockito.when(InetAddress.getByName(anyString())).thenReturn(inetAddress);
    }

    // either add or remove outboundIpRewriteExclusionCache
    @Test
    public void testProgramIpRewriteExclusionStage1() {

    }

    @Test
    public void testProgramIpRewriteExclusionStage2() {

    }

    // either add or remove outboundIpRewriteCache
    @Test
    public void testProgramOutboundIpRewriteStage1() throws Exception{

    }

    @Test
    public void testPrepareProgramOutboundIpRewriteStage2() throws Exception {
        assertEquals("Error, did not return the correct status code", new Status(StatusCode.BADREQUEST), Whitebox.invokeMethod(neutronL3Adapter, "programOutboundIpRewriteStage2", floatingIpObject, Action.ADD));

        PowerMockito.mockStatic(InetAddress.class);
        InetAddress inetAddress = mock(InetAddress.class);
        PowerMockito.when(InetAddress.getByName(anyString())).thenReturn(inetAddress);

        assertEquals("Error, did not return the correct status code", new Status(StatusCode.SUCCESS), Whitebox.invokeMethod(neutronL3Adapter, "programOutboundIpRewriteStage2", floatingIpObject, Action.ADD));
    }

    @Test
    public void testGetMaskLenFromCidr() throws Exception {
        assertEquals("Error, did not return the correct mask", 32, Whitebox.invokeMethod(neutronL3Adapter, "getMaskLenFromCidr", "127.0.0.1/32"));
    }

    @Test
    public void testGetDpidForIntegrationBridge() throws Exception {
        MemberModifier.field(NeutronL3Adapter.class, "southbound").set(neutronL3Adapter , southbound);
        MemberModifier.field(NeutronL3Adapter.class, "configurationService").set(neutronL3Adapter , configurationService);

        PowerMockito.when(southbound.getBridge(any(Node.class), anyString())).thenReturn(mock(OvsdbBridgeAugmentation.class));
        PowerMockito.when(configurationService.getIntegrationBridgeName()).thenReturn("");
        PowerMockito.when(southbound.getDataPathId(any(Node.class))).thenReturn(Long.valueOf(45));

        assertEquals("Error, did not return the correct Dpid", Long.valueOf(45), Whitebox.invokeMethod(neutronL3Adapter, "getDpidForIntegrationBridge", mock(Node.class)));
    }

    @Test
    public void testencodeExcplicitOFPort() throws Exception {
        assertEquals("Error, did not correctly encode the port", "OFPort|45", NeutronL3Adapter.encodeExcplicitOFPort(Long.valueOf(45)));
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

    @SuppressWarnings("rawtypes")
    private Object createFloatingIpObject() throws Exception{
        Class clazz = Whitebox.getInnerClassType(NeutronL3Adapter.class, "FloatIpData");
        Constructor [] constructors = clazz.getConstructors();
        Constructor c  = constructors[0];
        return c.newInstance(new NeutronL3Adapter(), Long.valueOf(415), Long.valueOf(415), "a", "b", "c", "d", "e");
    }
}
