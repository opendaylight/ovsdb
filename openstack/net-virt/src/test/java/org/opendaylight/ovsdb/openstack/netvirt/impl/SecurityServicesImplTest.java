/*
 * Copyright (c) 2015 Inocybe and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.openstack.netvirt.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.opendaylight.neutron.spi.INeutronNetworkCRUD;
import org.opendaylight.neutron.spi.INeutronPortCRUD;
import org.opendaylight.neutron.spi.INeutronSubnetCRUD;
import org.opendaylight.neutron.spi.NeutronNetwork;
import org.opendaylight.neutron.spi.NeutronPort;
import org.opendaylight.neutron.spi.NeutronSecurityGroup;
import org.opendaylight.neutron.spi.NeutronSecurityRule;
import org.opendaylight.neutron.spi.NeutronSubnet;
import org.opendaylight.neutron.spi.Neutron_IPs;
import org.opendaylight.ovsdb.openstack.netvirt.api.ConfigurationService;
import org.opendaylight.ovsdb.openstack.netvirt.api.EgressAclProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.IngressAclProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.Southbound;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Unit test for {@link SecurityServicesImpl}
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(ServiceHelper.class)
public class SecurityServicesImplTest {

    @InjectMocks private SecurityServicesImpl securityServicesImpl;
    @Mock INeutronNetworkCRUD neutronNetworkCache;
    @Mock private INeutronPortCRUD neutronPortCache;
    @Mock private INeutronSubnetCRUD subNetCache;
    @Mock private Southbound southbound;
    @Mock private ConfigurationService configurationService;
    @Mock NeutronNetwork neutronNetwork;
    @Mock NeutronPort neutronPort_Vm1;
    @Mock NeutronPort neutronPort_Vm2;
    @Mock NeutronPort neutronPort_Vm3;
    @Mock NeutronSecurityGroup neutronSecurityGroup_1;
    @Mock NeutronSecurityGroup neutronSecurityGroup_2;
    @Mock NeutronSecurityGroup neutronSecurityGroup_3;
    @Mock NeutronSecurityRule neutronSecurityRule_1;
    @Mock NeutronSecurityRule neutronSecurityRule_2;
    @Mock NeutronSecurityRule neutronSecurityRule_3;
    @Mock  NeutronPort neutronPort_Dhcp;
    @Mock Neutron_IPs neutron_ip_1;
    @Mock Neutron_IPs neutron_ip_2;
    @Mock Neutron_IPs neutron_ip_3;
    @Mock NeutronSubnet subnet;
    @Mock Node node;
    @Mock OvsdbTerminationPointAugmentation tp;
    @Mock IngressAclProvider ingressAclService;
    @Mock EgressAclProvider egressAclService;
    @Mock NeutronL3Adapter neutronL3Adapter;

    private static final String NEUTRON_PORT_ID_VM_1 = "neutronID_VM_1";
    private static final String NEUTRON_PORT_ID_VM_2 = "neutronID_VM_2";
    private static final String NEUTRON_PORT_ID_VM_3 = "neutronID_VM_3";
    private static final String NEUTRON_PORT_ID_DHCP = "neutronID_VM_DHCP";
    private static final String SECURITY_GROUP_ID_1 = "securityGroupId_1";
    private static final String SECURITY_GROUP_ID_2 = "securityGroupId_2";
    private static final String SECURITY_GROUP_ID_3 = "securityGroupId_3";
    private static final String DEVICE_OWNER_VM = "compute";
    private static final String DEVICE_OWNER_DHCP = "dhcp";
    private static final String SUBNET_UUID = "subnet_uuid";
    private static final List<Neutron_IPs> neutron_IPs_1 = new ArrayList<>();
    private static final List<Neutron_IPs> neutron_IPs_2 = new ArrayList<>();
    private static final List<Neutron_IPs> neutron_IPs_3 = new ArrayList<>();

    @Before
    public void setUp(){
        List<NeutronSecurityGroup> securityGroups_1 = new ArrayList<>();
        securityGroups_1.add(neutronSecurityGroup_1);
        List<NeutronSecurityGroup> securityGroups_2 = new ArrayList<>();
        securityGroups_2.add(neutronSecurityGroup_2);
        List<NeutronSecurityGroup> securityGroups_3 = new ArrayList<>();
        securityGroups_3.add(neutronSecurityGroup_3);
        List<NeutronSecurityRule> securityRule_1 = new ArrayList<>();
        securityRule_1.add(neutronSecurityRule_1);
        List<NeutronSecurityRule> securityRule_2 = new ArrayList<>();
        securityRule_1.add(neutronSecurityRule_2);
        List<NeutronSecurityRule> securityRule_3 = new ArrayList<>();
        securityRule_1.add(neutronSecurityRule_3);

        neutron_IPs_1.add(neutron_ip_1);
        neutron_IPs_2.add(neutron_ip_2);
        neutron_IPs_3.add(neutron_ip_3);

        when(neutronPort_Vm1.getID()).thenReturn(NEUTRON_PORT_ID_VM_1);
        when(neutronPort_Vm2.getID()).thenReturn(NEUTRON_PORT_ID_VM_2);
        when(neutronPort_Vm3.getID()).thenReturn(NEUTRON_PORT_ID_VM_3);
        when(neutronPort_Vm1.getSecurityGroups()).thenReturn(securityGroups_1);
        when(neutronPort_Vm2.getSecurityGroups()).thenReturn(securityGroups_2);
        when(neutronPort_Vm3.getSecurityGroups()).thenReturn(securityGroups_3);
        when(neutronSecurityGroup_1.getSecurityRules()).thenReturn(securityRule_1);
        when(neutronSecurityGroup_2.getSecurityRules()).thenReturn(securityRule_2);
        when(neutronSecurityGroup_3.getSecurityRules()).thenReturn(securityRule_3);
        when(neutronSecurityGroup_1.getSecurityGroupUUID()).thenReturn(SECURITY_GROUP_ID_1);
        when(neutronSecurityGroup_2.getSecurityGroupUUID()).thenReturn(SECURITY_GROUP_ID_2);
        when(neutronSecurityGroup_3.getSecurityGroupUUID()).thenReturn(SECURITY_GROUP_ID_3);
        when(neutronPort_Vm1.getDeviceOwner()).thenReturn(DEVICE_OWNER_VM);
        when(neutronPort_Vm2.getDeviceOwner()).thenReturn(DEVICE_OWNER_VM);
        when(neutronPort_Vm3.getDeviceOwner()).thenReturn(DEVICE_OWNER_VM);
        when(neutronPort_Dhcp.getDeviceOwner()).thenReturn(DEVICE_OWNER_DHCP);
        when(neutronPort_Vm1.getFixedIPs()).thenReturn(neutron_IPs_1);
        when(neutronPort_Vm2.getFixedIPs()).thenReturn(neutron_IPs_2);
        when(neutronPort_Vm3.getFixedIPs()).thenReturn(neutron_IPs_3);
        when(neutron_ip_1.getSubnetUUID()).thenReturn(SUBNET_UUID);
        List<NeutronPort> portList = new ArrayList<>();
        portList.add(neutronPort_Vm1);
        portList.add(neutronPort_Dhcp);
        when(subnet.getPortsInSubnet()).thenReturn(portList);

        List<Node> nodeList = new ArrayList<>();
        nodeList.add(node);
        List<OvsdbTerminationPointAugmentation> tpList = new ArrayList<>();
        tpList.add(tp);
        when(southbound.getInterfaceExternalIdsValue(any(OvsdbTerminationPointAugmentation.class), eq("iface-id"))).thenReturn(NEUTRON_PORT_ID_VM_1);
        when(southbound.readOvsdbTopologyNodes()).thenReturn(nodeList);
        when(southbound.getBridgeNode(any(Node.class), anyString())).thenReturn(node);
        when(southbound.getTerminationPointsOfBridge(node)).thenReturn(tpList);
        when(southbound.getDataPathId(node)).thenReturn(1L);
        when(southbound.getBridgeName(node)).thenReturn("br-int");
        when(southbound.getOFPort(any(OvsdbTerminationPointAugmentation.class))).thenReturn(2L);
        when(southbound.getInterfaceExternalIdsValue(any(OvsdbTerminationPointAugmentation.class),eq("attached-mac"))).thenReturn("attached-mac");
        when(configurationService.getIntegrationBridgeName()).thenReturn("br-int");
        when(neutronNetworkCache.getNetwork(anyString())).thenReturn(neutronNetwork);
        when(neutronNetwork.getProviderSegmentationID()).thenReturn("1000");
        when(neutronPortCache.getPort(eq(NEUTRON_PORT_ID_VM_1))).thenReturn(neutronPort_Vm1);
        when(neutronPortCache.getPort(eq(NEUTRON_PORT_ID_DHCP))).thenReturn(neutronPort_Dhcp);
        when(neutronPortCache.getAllPorts()).thenReturn(portList);
        when(subNetCache.getSubnet(eq(SUBNET_UUID))).thenReturn(subnet);
    }

    /**
     * Test method {@link SecurityServicesImpl#isPortSecurityReady(Interface)}
     */
    @Test
    public void testIsPortSecurityReady(){
        assertTrue("Error, did not return expected boolean for isPortSecurityReady", securityServicesImpl.isPortSecurityReady(mock(OvsdbTerminationPointAugmentation.class)));
    }

    /**
     * Test method {@link SecurityServicesImpl#getSecurityGroupInPortList(Interface)}
     */
    @Test
    public void testSecurityGroupInPort(){
        assertEquals("Error, did not return the good neutronSecurityGroup of securityGroups",
                     neutronSecurityGroup_1, securityServicesImpl.getSecurityGroupInPortList(mock(OvsdbTerminationPointAugmentation.class)).get(0));
    }

    /**
     * Test getDhcpServerPort returning a valid port.
     */
    @Test
    public void testGetDhcpServerPort() {
        NeutronPort dhcpPort = securityServicesImpl.getDhcpServerPort(mock(OvsdbTerminationPointAugmentation.class));
        assertEquals(dhcpPort,neutronPort_Dhcp);
    }

    /**
     * Test getDhcpServerPort with null port id returned by the southbound.
     */
    @Test
    public void testGetDhcpServerPortWithNullPortId() {
        when(southbound.getInterfaceExternalIdsValue(any(OvsdbTerminationPointAugmentation.class), anyString())).thenReturn(null);
        NeutronPort dhcpPort = securityServicesImpl.getDhcpServerPort(mock(OvsdbTerminationPointAugmentation.class));
        assertEquals(dhcpPort,null);
    }

    /**
     * Test getDhcpServerPort with port not present in cache.
     */
    @Test
    public void testGetDhcpServerPortWithNullPort() {
        when(neutronPortCache.getPort(eq(NEUTRON_PORT_ID_VM_1))).thenReturn(null);
        NeutronPort dhcpPort = securityServicesImpl.getDhcpServerPort(mock(OvsdbTerminationPointAugmentation.class));
        assertEquals(dhcpPort,null);
    }

    /**
     * Test getDhcpServerPort with a dhcp port as the input port.
     */
    @Test
    public void testGetDhcpServerPortWithDhcpPort() {
        when(neutronPortCache.getPort(eq(NEUTRON_PORT_ID_VM_1))).thenReturn(neutronPort_Dhcp);
        NeutronPort dhcpPort = securityServicesImpl.getDhcpServerPort(mock(OvsdbTerminationPointAugmentation.class));
        assertEquals(dhcpPort,neutronPort_Dhcp);
    }

    /**
     * Test getDhcpServerPort with a dhcp port with fixed ip null
     * for the input port..
     */
    @Test
    public void testGetDhcpServerPortWithFixedIpNull() {
        when(neutronPort_Vm1.getFixedIPs()).thenReturn(null);
        NeutronPort dhcpPort = securityServicesImpl.getDhcpServerPort(mock(OvsdbTerminationPointAugmentation.class));
        assertEquals(dhcpPort,null);
    }

    /**
     * Test getDhcpServerPort with a dhcp port with fixed ip empty
     * for the input port.
     */
    @Test
    public void testGetDhcpServerPortWithFixedIpEmpty() {
        when(neutronPort_Vm1.getFixedIPs()).thenReturn(new ArrayList<Neutron_IPs>());
        NeutronPort dhcpPort = securityServicesImpl.getDhcpServerPort(mock(OvsdbTerminationPointAugmentation.class));
        assertEquals(dhcpPort,null);
    }

    /**
     * Test getDhcpServerPort with a dhcp port with no port in subnet.
     */
    @Test
    public void testGetDhcpServerPortWithNoPortinSubnet() {
        when(subnet.getPortsInSubnet()).thenReturn(new ArrayList<NeutronPort>());
        NeutronPort dhcpPort = securityServicesImpl.getDhcpServerPort(mock(OvsdbTerminationPointAugmentation.class));
        assertEquals(dhcpPort,null);
    }

    /**
     * Test getNeutronPortFromDhcpIntf with port not present in cache.
     */
    @Test
    public void testGetNeutronPortFromDhcpIntfWithNullPort() {
        when(neutronPortCache.getPort(eq(NEUTRON_PORT_ID_VM_1))).thenReturn(null);
        NeutronPort dhcpPort = securityServicesImpl.getNeutronPortFromDhcpIntf(mock(OvsdbTerminationPointAugmentation.class));
        assertEquals(dhcpPort,null);
    }

    /**
     * Test getNeutronPortFromDhcpIntf with port id returned null
     * from the southbound.
     */
    @Test
    public void testGetNeutronPortFromDhcpIntfWithNullPortId() {
        when(southbound.getInterfaceExternalIdsValue(any(OvsdbTerminationPointAugmentation.class), anyString())).thenReturn(null);
        NeutronPort dhcpPort = securityServicesImpl.getNeutronPortFromDhcpIntf(mock(OvsdbTerminationPointAugmentation.class));
        assertEquals(dhcpPort,null);
    }

    /**
     * Test getNeutronPortFromDhcpIntf valid
     */
    @Test
    public void testGetNeutronPortFromDhcpIntfWithDhcpPort() {
        when(neutronPortCache.getPort(eq(NEUTRON_PORT_ID_VM_1))).thenReturn(neutronPort_Dhcp);
        NeutronPort dhcpPort = securityServicesImpl.getNeutronPortFromDhcpIntf(mock(OvsdbTerminationPointAugmentation.class));
        assertEquals(dhcpPort,neutronPort_Dhcp);
    }

    /**
     * Test getNeutronPortFromDhcpIntf with the port passed
     * a vm port.
     */
    @Test
    public void testGetNeutronPortFromDhcpIntfWithVmPort() {
        NeutronPort dhcpPort = securityServicesImpl.getNeutronPortFromDhcpIntf(mock(OvsdbTerminationPointAugmentation.class));
        assertEquals(dhcpPort,null);
    }

    /**
     * Test isComputePort with the port passed a vm port.
     */
    @Test
    public void testIsComputePortWithComputePort() {
        boolean isComputePort = securityServicesImpl.isComputePort(mock(OvsdbTerminationPointAugmentation.class));
        assertEquals(isComputePort,true);
    }

    /**
     * Test isComputePort with the port passed a dhcp port.
     */
    @Test
    public void testIsComputePortWithDhcpPort() {
        when(neutronPortCache.getPort(eq(NEUTRON_PORT_ID_VM_1))).thenReturn(neutronPort_Dhcp);
        boolean isComputePort = securityServicesImpl.isComputePort(mock(OvsdbTerminationPointAugmentation.class));
        assertEquals(isComputePort,false);
    }

    /**
     * Test isComputePort with port id null from southbound.
     */
    @Test
    public void testIsComputePortWithNullPortId() {
        when(southbound.getInterfaceExternalIdsValue(any(OvsdbTerminationPointAugmentation.class), anyString())).thenReturn(null);
        boolean isComputePort = securityServicesImpl.isComputePort(mock(OvsdbTerminationPointAugmentation.class));
        assertEquals(isComputePort,false);
    }

    /**
     * Test isComputePort with port not present in cache.
     */
    @Test
    public void testIsComputePortWithNullPort() {
        when(neutronPortCache.getPort(eq(NEUTRON_PORT_ID_VM_1))).thenReturn(null);
        boolean isComputePort = securityServicesImpl.isComputePort(mock(OvsdbTerminationPointAugmentation.class));
        assertEquals(isComputePort,false);
    }

    /**
     * Test getIpAddressList valid.
     */
    @Test
    public void testGetIpAddressList() {
        List<Neutron_IPs> ipList = securityServicesImpl.getIpAddressList(mock(OvsdbTerminationPointAugmentation.class));
        assertEquals(ipList,neutron_IPs_1);
    }

    /**
     * Test getIpAddressList with port not present in cache..
     */
    @Test
    public void testGetIpAddressListWithNullPort() {
        when(neutronPortCache.getPort(eq(NEUTRON_PORT_ID_VM_1))).thenReturn(null);
        List<Neutron_IPs> ipList = securityServicesImpl.getIpAddressList(mock(OvsdbTerminationPointAugmentation.class));
        assertEquals(ipList,null);
    }


    /**
     * Test getIpAddressList  with port id null from southbound.
     */
    @Test
    public void testGetIpAddressListWithNullPortId() {
        when(southbound.getInterfaceExternalIdsValue(any(OvsdbTerminationPointAugmentation.class), anyString())).thenReturn(null);
        List<Neutron_IPs> ipList = securityServicesImpl.getIpAddressList(mock(OvsdbTerminationPointAugmentation.class));
        assertEquals(ipList,null);
    }

    /**
     * Test getVmListForSecurityGroup valid.
     */
    @Test
    public void testGetVmListForSecurityGroup() {
        List<NeutronPort> portList = new ArrayList<>();
        portList.add(neutronPort_Vm1);
        portList.add(neutronPort_Vm2);
        portList.add(neutronPort_Vm3);
        portList.add(neutronPort_Dhcp);
        when(neutronL3Adapter.getPortCleanupCache()).thenReturn(new HashSet<NeutronPort>(portList));
        List<Neutron_IPs> ipList = securityServicesImpl.getVmListForSecurityGroup(NEUTRON_PORT_ID_VM_1, SECURITY_GROUP_ID_2);
        assertEquals(ipList,neutron_IPs_2);
    }

    /**
     * Test getVmListForSecurityGroup with no vm with the
     * SG associated..
     */
    @Test
    public void testGetVmListForSecurityGroupWithNoVm() {
        List<NeutronPort> portList = new ArrayList<>();
        portList.add(neutronPort_Vm1);
        portList.add(neutronPort_Vm2);
        portList.add(neutronPort_Vm3);
        portList.add(neutronPort_Dhcp);
        when(neutronPortCache.getAllPorts()).thenReturn(portList);
        List<Neutron_IPs> ipList = securityServicesImpl.getVmListForSecurityGroup(NEUTRON_PORT_ID_VM_1, SECURITY_GROUP_ID_1);
        assert(ipList.isEmpty());
    }

    /**
     * Test syncSecurityGroup addition
     */
    @Test
    public void testSyncSecurityGroupAddition() {
        List<NeutronSecurityGroup> securityGroupsList = new ArrayList<>();
        securityGroupsList.add(neutronSecurityGroup_1);
        securityServicesImpl.syncSecurityGroup(neutronPort_Vm1, securityGroupsList, true);
        verify(ingressAclService, times(1)).programPortSecurityGroup(eq(new Long(1)), eq("1000"), eq("attached-mac"), eq(2L), eq(neutronSecurityGroup_1), eq(NEUTRON_PORT_ID_VM_1), eq(true));
        verify(egressAclService, times(1)).programPortSecurityGroup(eq(new Long(1)), eq("1000"), eq("attached-mac"), eq(2L), eq(neutronSecurityGroup_1), eq(NEUTRON_PORT_ID_VM_1), eq(true));
    }

    /**
     * Test syncSecurityGroup deletion
     */
    @Test
    public void testSyncSecurityGroupDeletion() {
        List<NeutronSecurityGroup> securityGroupsList = new ArrayList<>();
        securityGroupsList.add(neutronSecurityGroup_1);
        securityServicesImpl.syncSecurityGroup(neutronPort_Vm1, securityGroupsList, false);
        verify(ingressAclService, times(1)).programPortSecurityGroup(eq(new Long(1)), eq("1000"), eq("attached-mac"), eq(2L), eq(neutronSecurityGroup_1), eq(NEUTRON_PORT_ID_VM_1), eq(false));
        verify(egressAclService, times(1)).programPortSecurityGroup(eq(new Long(1)), eq("1000"), eq("attached-mac"), eq(2L), eq(neutronSecurityGroup_1), eq(NEUTRON_PORT_ID_VM_1), eq(false));
    }

    /**
     * Test syncSecurityGroup deletion with port null
     */
    @Test
    public void testSyncSecurityGroupPortNull() {
        List<NeutronSecurityGroup> securityGroupsList = new ArrayList<>();
        securityGroupsList.add(neutronSecurityGroup_1);
        securityServicesImpl.syncSecurityGroup(null, securityGroupsList, false);
        verify(ingressAclService, times(0)).programPortSecurityGroup(eq(new Long(1)), eq("1000"), eq("attached-mac"), eq(2L), eq(neutronSecurityGroup_1), eq(NEUTRON_PORT_ID_VM_1), eq(false));
        verify(egressAclService, times(0)).programPortSecurityGroup(eq(new Long(1)), eq("1000"), eq("attached-mac"), eq(2L), eq(neutronSecurityGroup_1), eq(NEUTRON_PORT_ID_VM_1), eq(false));
    }

    /**
     * Test syncSecurityGroup deletion with Sg null
     */
    @Test
    public void testSyncSecurityGroupSgNull() {
        List<NeutronSecurityGroup> securityGroupsList = new ArrayList<>();
        securityGroupsList.add(neutronSecurityGroup_1);
        when(neutronPort_Vm1.getSecurityGroups()).thenReturn(null);
        securityServicesImpl.syncSecurityGroup(neutronPort_Vm1, null, false);
        verify(ingressAclService, times(0)).programPortSecurityGroup(eq(new Long(1)), eq("1000"), eq("attached-mac"), eq(2L), eq(neutronSecurityGroup_1), eq(NEUTRON_PORT_ID_VM_1), eq(false));
        verify(egressAclService, times(0)).programPortSecurityGroup(eq(new Long(1)), eq("1000"), eq("attached-mac"), eq(2L), eq(neutronSecurityGroup_1), eq(NEUTRON_PORT_ID_VM_1), eq(false));
    }

    /**
     * Test syncSecurityGroup deletion with Mac null
     */
    @Test
    public void testSyncSecurityGroupAttachedMacNull() {
        List<NeutronSecurityGroup> securityGroupsList = new ArrayList<>();
        securityGroupsList.add(neutronSecurityGroup_1);
        when(southbound.getInterfaceExternalIdsValue(any(OvsdbTerminationPointAugmentation.class),eq("attached-mac"))).thenReturn(null);
        securityServicesImpl.syncSecurityGroup(neutronPort_Vm1, securityGroupsList, false);
        verify(ingressAclService, times(0)).programPortSecurityGroup(eq(new Long(1)), eq("1000"), eq("attached-mac"), eq(2L), eq(neutronSecurityGroup_1), eq(NEUTRON_PORT_ID_VM_1), eq(false));
        verify(egressAclService, times(0)).programPortSecurityGroup(eq(new Long(1)), eq("1000"), eq("attached-mac"), eq(2L), eq(neutronSecurityGroup_1), eq(NEUTRON_PORT_ID_VM_1), eq(false));
    }

    /**
     * Test syncSecurityRule addition of egress rule.
     */
    @Test
    public void testSyncSecurityRuleAdditionEgress() {
        List<NeutronSecurityRule> securityRuleList = new ArrayList<>();
        securityRuleList.add(neutronSecurityRule_1);
        when(neutronSecurityRule_1.getSecurityRuleDirection()).thenReturn("egress");
        when(neutronSecurityRule_1.getSecurityRuleEthertype()).thenReturn("IPv4");
        securityServicesImpl.syncSecurityRule(neutronPort_Vm1, neutronSecurityRule_1, neutron_ip_1, true);
        verify(egressAclService, times(1)).programPortSecurityRule(eq(new Long(1)), eq("1000"), eq("attached-mac"), eq(2L), eq(neutronSecurityRule_1), eq(neutron_ip_1), eq(true));
    }

    /**
     * Test syncSecurityRule addition of ingress rule.
     */
    @Test
    public void testSyncSecurityRuleAdditionIngress() {
        List<NeutronSecurityRule> securityRuleList = new ArrayList<>();
        securityRuleList.add(neutronSecurityRule_1);
        when(neutronSecurityRule_1.getSecurityRuleDirection()).thenReturn("ingress");
        when(neutronSecurityRule_1.getSecurityRuleEthertype()).thenReturn("IPv4");
        securityServicesImpl.syncSecurityRule(neutronPort_Vm1, neutronSecurityRule_1, neutron_ip_1, true);
        verify(ingressAclService, times(1)).programPortSecurityRule(eq(new Long(1)), eq("1000"), eq("attached-mac"), eq(2L), eq(neutronSecurityRule_1), eq(neutron_ip_1), eq(true));
    }

    /**
     * Test syncSecurityRule deletion of egress rule.
     */
    @Test
    public void testSyncSecurityRuleDeletionEgress() {
        List<NeutronSecurityRule> securityRuleList = new ArrayList<>();
        securityRuleList.add(neutronSecurityRule_1);
        when(neutronSecurityRule_1.getSecurityRuleDirection()).thenReturn("egress");
        when(neutronSecurityRule_1.getSecurityRuleEthertype()).thenReturn("IPv4");
        securityServicesImpl.syncSecurityRule(neutronPort_Vm1, neutronSecurityRule_1, neutron_ip_1, false);
        verify(egressAclService, times(1)).programPortSecurityRule(eq(new Long(1)), eq("1000"), eq("attached-mac"), eq(2L), eq(neutronSecurityRule_1), eq(neutron_ip_1), eq(false));
    }

    /**
     * Test syncSecurityRule deletion of ingress rule.
     */
    @Test
    public void testSyncSecurityRuleDeletionIngress() {
        List<NeutronSecurityRule> securityRuleList = new ArrayList<>();
        securityRuleList.add(neutronSecurityRule_1);
        when(neutronSecurityRule_1.getSecurityRuleDirection()).thenReturn("ingress");
        when(neutronSecurityRule_1.getSecurityRuleEthertype()).thenReturn("IPv4");
        securityServicesImpl.syncSecurityRule(neutronPort_Vm1, neutronSecurityRule_1, neutron_ip_1, false);
        verify(ingressAclService, times(1)).programPortSecurityRule(eq(new Long(1)), eq("1000"), eq("attached-mac"), eq(2L), eq(neutronSecurityRule_1), eq(neutron_ip_1), eq(false));
    }

    /**
     * Test syncSecurityRule deletion of ingress rule with port null.
     */
    @Test
    public void testSyncSecurityRuleDeletionIngressPortNull() {
        List<NeutronSecurityRule> securityRuleList = new ArrayList<>();
        securityRuleList.add(neutronSecurityRule_1);
        when(neutronSecurityRule_1.getSecurityRuleDirection()).thenReturn("ingress");
        when(neutronSecurityRule_1.getSecurityRuleEthertype()).thenReturn("IPv4");
        securityServicesImpl.syncSecurityRule(null, neutronSecurityRule_1, neutron_ip_1, false);
        verify(ingressAclService, times(0)).programPortSecurityRule(eq(new Long(1)), eq("1000"), eq("attached-mac"), eq(2L), eq(neutronSecurityRule_1), eq(neutron_ip_1), eq(false));
    }

    /**
     * Test syncSecurityRule deletion of ingress rule with sg null.
     */
    @Test
    public void testSyncSecurityRuleDeletionIngressSgNull() {
        List<NeutronSecurityRule> securityRuleList = new ArrayList<>();
        securityRuleList.add(neutronSecurityRule_1);
        when(neutronPort_Vm1.getSecurityGroups()).thenReturn(null);
        when(neutronSecurityRule_1.getSecurityRuleDirection()).thenReturn("ingress");
        when(neutronSecurityRule_1.getSecurityRuleEthertype()).thenReturn("IPv4");
        securityServicesImpl.syncSecurityRule(neutronPort_Vm1, neutronSecurityRule_1, neutron_ip_1, false);
        verify(ingressAclService, times(0)).programPortSecurityRule(eq(new Long(1)), eq("1000"), eq("attached-mac"), eq(2L), eq(neutronSecurityRule_1), eq(neutron_ip_1), eq(false));
    }

    /**
     * Test syncSecurityRule deletion of ingress rule with mac null.
     */
    @Test
    public void testSyncSecurityRuleDeletionIngressAttachedMacNull() {
        List<NeutronSecurityRule> securityRuleList = new ArrayList<>();
        securityRuleList.add(neutronSecurityRule_1);
        when(neutronSecurityRule_1.getSecurityRuleDirection()).thenReturn("ingress");
        when(neutronSecurityRule_1.getSecurityRuleEthertype()).thenReturn("IPv4");
        when(southbound.getInterfaceExternalIdsValue(any(OvsdbTerminationPointAugmentation.class),eq("attached-mac"))).thenReturn(null);
        securityServicesImpl.syncSecurityRule(neutronPort_Vm1, neutronSecurityRule_1, neutron_ip_1, false);
        verify(ingressAclService, times(0)).programPortSecurityRule(eq(new Long(1)), eq("1000"), eq("attached-mac"), eq(2L), eq(neutronSecurityRule_1), eq(neutron_ip_1), eq(false));
    }


    /**
     * Test syncSecurityRule deletion of ingress rule no ipv4 ether.
     */
    @Test
    public void testSyncSecurityRuleDeletionIngressNonIpV4() {
        List<NeutronSecurityRule> securityRuleList = new ArrayList<>();
        securityRuleList.add(neutronSecurityRule_1);
        when(neutronSecurityRule_1.getSecurityRuleDirection()).thenReturn("ingress");
        when(neutronSecurityRule_1.getSecurityRuleEthertype()).thenReturn("IPv6");
        securityServicesImpl.syncSecurityRule(neutronPort_Vm1, neutronSecurityRule_1, neutron_ip_1, false);
        verify(ingressAclService, times(0)).programPortSecurityRule(eq(new Long(1)), eq("1000"), eq("attached-mac"), eq(2L), eq(neutronSecurityRule_1), eq(neutron_ip_1), eq(false));
    }

    /**
     * Test syncSecurityRule deletion of ingress rule with invalid direction.
     */
    @Test
    public void testSyncSecurityRuleDeletionInvalidDirection() {
        List<NeutronSecurityRule> securityRuleList = new ArrayList<>();
        securityRuleList.add(neutronSecurityRule_1);
        when(neutronSecurityRule_1.getSecurityRuleDirection()).thenReturn("outgress");
        when(neutronSecurityRule_1.getSecurityRuleEthertype()).thenReturn("IPv4");
        securityServicesImpl.syncSecurityRule(neutronPort_Vm1, neutronSecurityRule_1, neutron_ip_1, false);
        verify(ingressAclService, times(0)).programPortSecurityRule(eq(new Long(1)), eq("1000"), eq("attached-mac"), eq(2L), eq(neutronSecurityRule_1), eq(neutron_ip_1), eq(false));
    }

    @Test
    public void testSetDependencies() throws Exception {
        Southbound southbound = mock(Southbound.class);

        PowerMockito.mockStatic(ServiceHelper.class);
        PowerMockito.when(ServiceHelper.getGlobalInstance(Southbound.class, securityServicesImpl)).thenReturn(southbound);

        securityServicesImpl.setDependencies(mock(BundleContext.class), mock(ServiceReference.class));

        assertEquals("Error, did not return the correct object", getField("southbound"), southbound);
    }

    @Test
    public void testSetDependenciesObject() throws Exception{
        INeutronPortCRUD neutronPortCache = mock(INeutronPortCRUD.class);
        securityServicesImpl.setDependencies(neutronPortCache);
        assertEquals("Error, did not return the correct object", getField("neutronPortCache"), neutronPortCache);
    }

    private Object getField(String fieldName) throws Exception {
        Field field = SecurityServicesImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(securityServicesImpl);
    }
}
