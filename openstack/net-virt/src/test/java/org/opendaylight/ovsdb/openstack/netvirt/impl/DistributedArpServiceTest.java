/*
 * Copyright (c) 2016 NEC Corporation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronNetwork;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronPort;
import org.opendaylight.ovsdb.openstack.netvirt.translator.Neutron_IPs;
import org.opendaylight.ovsdb.openstack.netvirt.translator.crud.INeutronNetworkCRUD;
import org.opendaylight.ovsdb.openstack.netvirt.translator.crud.INeutronPortCRUD;
import org.opendaylight.ovsdb.openstack.netvirt.api.Action;
import org.opendaylight.ovsdb.openstack.netvirt.api.ArpProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.ConfigurationService;
import org.opendaylight.ovsdb.openstack.netvirt.api.NodeCacheManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.Southbound;
import org.opendaylight.ovsdb.openstack.netvirt.api.Status;
import org.opendaylight.ovsdb.openstack.netvirt.api.StatusCode;
import org.opendaylight.ovsdb.openstack.netvirt.api.TenantNetworkManager;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

import org.osgi.framework.ServiceReference;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberMatcher;
import org.powermock.api.support.membermodification.MemberModifier;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * Unit test for {@link DistributedArpService}
 */
@PrepareForTest({ServiceHelper.class, InetAddress.class, DistributedArpService.class})
@RunWith(PowerMockRunner.class)
public class DistributedArpServiceTest {

    @Mock private DistributedArpService distributedArpService;
    /**
     * ID used for testing different scenarios.
     */
    private static final String ID = "45";
    /**
     * IP used for testing different scenarios.
     */
    private static final String IP = "127.0.0.1";
    /**
     * MALFORM_IP used for testing different scenarios.
     */
    private static final String MALFORM_IP = "127.0.0.1.5";
    /**
     * INTF_NAME used for testing different scenarios.
     */
    private static final String INTF_NAME = "br-int";
    /**
     * UUID used for testing different scenarios.
     */
    private static final String UUID = "7da709ff-397f-4778-a0e8-994811272fdb";
    /**
     * FIXED_IP_ADDRESS used for testing different scenarios.
     */
    private static final String FIXED_IP_ADDRESS = "192.168.1.0";
    /**
     * MAC_ADDRESS used for testing different scenarios.
     */
    private static final String MAC_ADDRESS = "00:00:5E:00:02:01";
    /**
     * MAC_ADDRESS_2 used for testing different scenarios.
     */
    private static final String MAC_ADDRESS_2 = "00:00:5E:00:02:02";
    /**
     * PORT_INT used for testing different scenarios.
     */
    private static final String PORT_INT = "port_int";

    @Before
    public void setUp() throws Exception{
        distributedArpService = PowerMockito.spy(new DistributedArpService());
    }

    /**
     * Test that checks if @{DistributedArpService#handlePortEvent} is called
     * and then checks that the port event process to write arp rules for neutron ports based on action.
     */
    @Test
    public void testHandlePortEvent() throws Exception {
        NeutronPort neutronPortOne = PowerMockito.mock(NeutronPort.class);
        NeutronPort neutronPortTwo = PowerMockito.mock(NeutronPort.class);
        List<NeutronPort> list_neutronPort = new ArrayList<>();
        list_neutronPort.add(neutronPortOne);
        list_neutronPort.add(neutronPortTwo);
        INeutronPortCRUD neutronPortCache = PowerMockito.mock(INeutronPortCRUD.class);
        MemberModifier.field(DistributedArpService.class, "neutronPortCache").set(distributedArpService, neutronPortCache);
        PowerMockito.when(neutronPortCache, "getAllPorts").thenReturn(list_neutronPort);

        // Suppress the called to these functions.
        MemberModifier.suppress(MemberMatcher.method(DistributedArpService.class, "handleNeutronPortForArp", NeutronPort.class, Action.class));

        //Case 1: Delete Action.
        Whitebox.invokeMethod(distributedArpService, "handlePortEvent", neutronPortOne, Action.DELETE);
        PowerMockito.verifyPrivate(distributedArpService, times(1)).invoke("handleNeutronPortForArp", any(NeutronPort.class), eq(Action.DELETE));

        //Case 2: Add Action.
        Whitebox.invokeMethod(distributedArpService, "handlePortEvent", neutronPortOne, Action.ADD);
        PowerMockito.verifyPrivate(distributedArpService, times(2)).invoke("handleNeutronPortForArp", any(NeutronPort.class), eq(Action.ADD));
    }

    /**
     * Test that checks if @{DistributedArpService#programStaticRuleStage1} is called
     * and then checks that the arp rules are added/removed based on neutron port event.
     */
    @Test
    public void testProgramStaticRuleStage1() throws Exception {
        MemberModifier.suppress(MemberMatcher.method(DistributedArpService.class, "programStaticRuleStage2", Long.class, String.class, String.class, String.class, Action.class));
        PowerMockito.when(distributedArpService, "programStaticRuleStage2", anyLong(), anyString(), anyString(), anyString(), any(Action.class)).thenReturn(new Status(StatusCode.SUCCESS));

        //Case 1: Add Action.
        Whitebox.invokeMethod(distributedArpService, "programStaticRuleStage1", Long.valueOf(12), PORT_INT, MAC_ADDRESS, IP, Action.ADD);
        PowerMockito.verifyPrivate(distributedArpService, times(1)).invoke("programStaticRuleStage2", anyLong(), anyString(), anyString(), anyString(), eq(Action.ADD));

        //Case 2: Delete Action.
        Whitebox.invokeMethod(distributedArpService, "programStaticRuleStage1", Long.valueOf(12), PORT_INT, MAC_ADDRESS, IP, Action.DELETE);
        PowerMockito.verifyPrivate(distributedArpService, times(1)).invoke("programStaticRuleStage2", anyLong(), anyString(), anyString(), anyString(), eq(Action.DELETE));
    }

    /**
     * Test that checks if @{DistributedArpService#programStaticRuleStage2} is called
     * and then checks that the arp rules are programmed by invoke arpProvider.
     */
    @Test
    public void testProgramStaticRuleStage2() throws Exception {
        //Case 1: StatusCode BADREQUEST.
        assertEquals("Error, this not return the correct status code", new Status(StatusCode.BADREQUEST), Whitebox.invokeMethod(distributedArpService, "programStaticRuleStage2", Long.valueOf(45), PORT_INT, MAC_ADDRESS, MALFORM_IP, Action.ADD));
        PowerMockito.mockStatic(InetAddress.class);
        InetAddress inetAddress = mock(InetAddress.class);
        PowerMockito.when(InetAddress.getByName(anyString())).thenReturn(inetAddress);

        //Case 2: StatusCode SUCCESS.
        assertEquals("Error, this not return the correct status code", new Status(StatusCode.SUCCESS), Whitebox.invokeMethod(distributedArpService, "programStaticRuleStage2", Long.valueOf(45), PORT_INT, MAC_ADDRESS, IP, Action.DELETE));
    }

    /**
     * Test that checks if @{DistributedArpService#handleNeutornPortForArp} is called
     * and then checks that the arp rules are written based on event for neutron port.
     */
    @Test
    public void testHandleNeutornPortForArp() throws Exception {
        Neutron_IPs neutronIp = mock(Neutron_IPs.class);
        when(neutronIp.getIpAddress()).thenReturn(FIXED_IP_ADDRESS);
        List<Neutron_IPs> neutronIps = new ArrayList<>();
        neutronIps.add(neutronIp);
        NeutronPort neutronPort = mock(NeutronPort.class);
        when(neutronPort.getNetworkUUID()).thenReturn(UUID);
        when(neutronPort.getMacAddress()).thenReturn(MAC_ADDRESS_2);
        when(neutronPort.getFixedIPs()).thenReturn(neutronIps);
        NeutronNetwork neutronNetwork = mock(NeutronNetwork.class);
        when(neutronNetwork.getProviderSegmentationID()).thenReturn(ID);
        List<Node> nodes = new ArrayList<>();
        nodes.add(mock(Node.class));
        TenantNetworkManager tenantNetworkManager = mock(TenantNetworkManager.class);
        MemberModifier.field(DistributedArpService.class, "tenantNetworkManager").set(distributedArpService, tenantNetworkManager);
        when(tenantNetworkManager.isTenantNetworkPresentInNode(any(Node.class), eq(ID))).thenReturn(true);
        PowerMockito.doReturn(15L).when(distributedArpService, "getDatapathIdIntegrationBridge", any(Node.class));
        INeutronNetworkCRUD neutronNetworkCache = mock(INeutronNetworkCRUD.class);
        when(neutronNetworkCache.getNetwork(anyString())).thenReturn(neutronNetwork);
        MemberModifier.field(DistributedArpService.class, "neutronNetworkCache").set(distributedArpService, neutronNetworkCache);
        NodeCacheManager nodeCacheManager = mock(NodeCacheManager.class);
        when(nodeCacheManager.getBridgeNodes()).thenReturn(nodes);
        MemberModifier.field(DistributedArpService.class, "nodeCacheManager").set(distributedArpService, nodeCacheManager);
        MemberModifier.field(DistributedArpService.class, "flgDistributedARPEnabled").set(distributedArpService, true);

        // Suppress the called to these functions.
        MemberModifier.suppress(MemberMatcher.method(DistributedArpService.class, "programStaticRuleStage1", Long.class, String.class, String.class, String.class, Action.class));

        //Case 1: Add Action.
        Whitebox.invokeMethod(distributedArpService, "handleNeutronPortForArp", neutronPort, Action.ADD);
        PowerMockito.verifyPrivate(distributedArpService, times(1)).invoke("getDatapathIdIntegrationBridge", any(Node.class));
        Mockito.verify(distributedArpService, times(1)).programStaticRuleStage1(anyLong(), anyString(), anyString(), anyString(), eq(Action.ADD));

        //Case 2: Delete Action.
        Whitebox.invokeMethod(distributedArpService, "handleNeutronPortForArp", neutronPort, Action.DELETE);
        PowerMockito.verifyPrivate(distributedArpService, times(2)).invoke("getDatapathIdIntegrationBridge", any(Node.class));
        Mockito.verify(distributedArpService, times(1)).programStaticRuleStage1(anyLong(), anyString(), anyString(), anyString(), eq(Action.DELETE));
    }

    /**
     * Test that checks if @{DistributedArpService#getDatapathIdIntegrationBridge} is called
     * and then checks the node integration bridge, then return its datapathID.
     */
    @Test
    public void testGetDatapathIdIntegrationBridge() throws Exception {
        Southbound southbound = mock(Southbound.class);
        ConfigurationService configurationService = mock(ConfigurationService.class);

        MemberModifier.field(DistributedArpService.class, "southbound").set(distributedArpService, southbound);
        MemberModifier.field(DistributedArpService.class, "configurationService").set(distributedArpService, configurationService);

        PowerMockito.when(southbound.getBridge(any(Node.class), anyString())).thenReturn(mock(OvsdbBridgeAugmentation.class));
        PowerMockito.when(configurationService.getIntegrationBridgeName()).thenReturn("");
        PowerMockito.when(southbound.getDataPathId(any(Node.class))).thenReturn(45L);

        //Assert check for correct Dp Id.
        assertEquals("Error, did not return the correct Dpid", 45, (long)Whitebox.invokeMethod(distributedArpService, "getDatapathIdIntegrationBridge", mock(Node.class)));
    }

    /**
     * Test that checks if @{DistributedArpService#processInterfaceEvent} is called
     * and then checks that the event is processing.
     */
    @Test
    public void testProcessInterfaceEvent() throws Exception {
        NeutronPort neutronPort = mock(NeutronPort.class);
        NeutronNetwork neutronNetwork = mock(NeutronNetwork.class);
        PowerMockito.doNothing().when(distributedArpService).handlePortEvent(any(NeutronPort.class), any(Action.class));
        // init instance variables.
        TenantNetworkManager tenantNetworkManager = mock(TenantNetworkManager.class);
        MemberModifier.field(DistributedArpService.class, "tenantNetworkManager").set(distributedArpService , tenantNetworkManager);

        // Mock variables
        NodeId nodeId = mock(NodeId.class);
        when(nodeId.getValue()).thenReturn(ID);
        Node node = mock(Node.class);
        when(node.getNodeId()).thenReturn(nodeId);

        OvsdbTerminationPointAugmentation intf = mock(OvsdbTerminationPointAugmentation.class);
        when(intf.getName()).thenReturn(INTF_NAME);

        when(tenantNetworkManager.getTenantPort(intf)).thenReturn(neutronPort);

        //Case 1: Add Action.
        distributedArpService.processInterfaceEvent(node, intf, neutronNetwork, Action.ADD);
        Mockito.verify(distributedArpService, times(1)).handlePortEvent(neutronPort, Action.ADD);

        //Case 2: Delete Action.
        distributedArpService.processInterfaceEvent(node, intf, neutronNetwork, Action.DELETE);
        Mockito.verify(distributedArpService, times(1)).handlePortEvent(neutronPort, Action.DELETE);
    }

    /**
     * Test that checks if @{DistributedArpService#setDependencies} is called
     * and then checks the object instances.
     */
    @Test
    public void testSetDependencies() throws Exception {
        TenantNetworkManager tenantNetworkManager = mock(TenantNetworkManager.class);
        ConfigurationService configurationService = mock(ConfigurationService.class);
        ArpProvider arpProvider = mock(ArpProvider.class);
        NodeCacheManager nodeCacheManager = mock(NodeCacheManager.class);
        Southbound southbound = mock(Southbound.class);

        ServiceHelper.overrideGlobalInstance(TenantNetworkManager.class, tenantNetworkManager);
        ServiceHelper.overrideGlobalInstance(ConfigurationService.class, configurationService);
        ServiceHelper.overrideGlobalInstance(ArpProvider.class, arpProvider);
        ServiceHelper.overrideGlobalInstance(NodeCacheManager.class, nodeCacheManager);
        ServiceHelper.overrideGlobalInstance(Southbound.class, southbound);

        distributedArpService.setDependencies(mock(ServiceReference.class));

        assertEquals("Error, did not return the correct object", getField("tenantNetworkManager"), tenantNetworkManager);
        assertEquals("Error, did not return the correct object", getField("configurationService"), configurationService);
        assertEquals("Error, did not return the correct object", getField("arpProvider"), arpProvider);
        assertEquals("Error, did not return the correct object", getField("nodeCacheManager"), nodeCacheManager);
        assertEquals("Error, did not return the correct object", getField("southbound"), southbound);
    }

    /**
     * Test that checks if @{DistributedArpService#setDependencies} is called
     * and then checks the object instances.
     */
    @Test
    public void testSetDependenciesObject() throws Exception{
        INeutronNetworkCRUD iNeutronNetworkCRUD = mock(INeutronNetworkCRUD.class);
        distributedArpService.setDependencies(iNeutronNetworkCRUD);
        assertEquals("Error, did not return the correct object", getField("neutronNetworkCache"), iNeutronNetworkCRUD);

        INeutronPortCRUD iNeutronPortCRUD = mock(INeutronPortCRUD.class);
        distributedArpService.setDependencies(iNeutronPortCRUD);
        assertEquals("Error, did not return the correct object", getField("neutronPortCache"), iNeutronPortCRUD);

        ArpProvider arpProvider = mock(ArpProvider.class);
        distributedArpService.setDependencies(arpProvider);
        assertEquals("Error, did not return the correct object", getField("arpProvider"), arpProvider);
    }

    private Object getField(String fieldName) throws Exception {
        Field field = DistributedArpService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(distributedArpService);
    }
}