/*
 * Copyright (c) 2015 Inocybe Technologies.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyShort;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendaylight.neutron.spi.NeutronNetwork;
import org.opendaylight.ovsdb.openstack.netvirt.MdsalHelper;
import org.opendaylight.ovsdb.openstack.netvirt.NetworkHandler;
import org.opendaylight.ovsdb.openstack.netvirt.api.BridgeConfigurationManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.ClassifierProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.ConfigurationService;
import org.opendaylight.ovsdb.openstack.netvirt.api.EgressAclProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.IngressAclProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.L2ForwardingProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.NetworkingProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.NetworkingProviderManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.NodeCacheManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.SecurityServicesManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.Southbound;
import org.opendaylight.ovsdb.openstack.netvirt.api.Status;
import org.opendaylight.ovsdb.openstack.netvirt.api.StatusCode;
import org.opendaylight.ovsdb.openstack.netvirt.api.TenantNetworkManager;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
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
 * Unit test for {@link OF13Provider}
 */
@PrepareForTest({OF13Provider.class, InetAddress.class, MdsalHelper.class, ServiceHelper.class})
@RunWith(PowerMockRunner.class)
public class OF13ProviderTest {

    @Mock private OF13Provider of13Provider;

    private static final String TYPE = "gre";
    private static final String IP = "127.0.0.1";
    private static final String BR_INT = "br-int";
    private static final String ID = "4";
    private static final String PORT = "port-int";
    private static final String SEG_ID = "5";
    private static final String MAC_ADDRESS = "mac-address";
    private static final long LOCAL_PORT = 3;

    @Before
    public void setUp() throws Exception{
        of13Provider = PowerMockito.mock(OF13Provider.class, Mockito.CALLS_REAL_METHODS);
    }

    @Test
    public void testGetName() {
        assertEquals("Error, did not return the correct name",  OF13Provider.NAME, of13Provider.getName());
    }

    @Test
    public void testSupportsServices() {
        assertTrue("Error, did not return the correct boolean", of13Provider.supportsServices());
    }

    @Test
    public void testHasPerTenantTunneling() {
        assertFalse("Error, did not return the correct boolean", of13Provider.hasPerTenantTunneling());
    }

    @Test
    public void testGetTunnelReadinessStatus() throws Exception {
        ConfigurationService configurationService = mock(ConfigurationService.class);
        BridgeConfigurationManager bridgeConfigurationManager = mock(BridgeConfigurationManager.class);
        TenantNetworkManager tenantNetworkManager = mock(TenantNetworkManager.class);

        MemberModifier.field(OF13Provider.class, "configurationService").set(of13Provider , configurationService);
        MemberModifier.field(OF13Provider.class, "bridgeConfigurationManager").set(of13Provider , bridgeConfigurationManager);
        MemberModifier.field(OF13Provider.class, "tenantNetworkManager").set(of13Provider , tenantNetworkManager);

        assertEquals("Error, did not return the correct status code", new Status(StatusCode.NOTFOUND), Whitebox.invokeMethod(of13Provider, "getTunnelReadinessStatus", mock(Node.class), ""));

        when(configurationService.getTunnelEndPoint(any(Node.class))).thenReturn(mock(InetAddress.class));
        when(bridgeConfigurationManager.isNodeNeutronReady(any(Node.class))).thenReturn(false, true);

        assertEquals("Error, did not return the correct status code", new Status(StatusCode.NOTACCEPTABLE), Whitebox.invokeMethod(of13Provider, "getTunnelReadinessStatus", mock(Node.class), ""));

        when(tenantNetworkManager.isTenantNetworkPresentInNode(any(Node.class), anyString())).thenReturn(false, true);

        assertEquals("Error, did not return the correct status code", new Status(StatusCode.NOTACCEPTABLE), Whitebox.invokeMethod(of13Provider, "getTunnelReadinessStatus", mock(Node.class), ""));
        assertEquals("Error, did not return the correct status code", new Status(StatusCode.SUCCESS), Whitebox.invokeMethod(of13Provider, "getTunnelReadinessStatus", mock(Node.class), ""));
    }

    @Test
    public void testGetTunnelName() throws Exception {
        PowerMockito.mockStatic(InetAddress.class);
        InetAddress inetAddress = mock(InetAddress.class);
        PowerMockito.when(inetAddress.getHostAddress()).thenReturn(IP);

        String ret = TYPE + "-" + inetAddress.getHostAddress();
        assertEquals("Error, did not return the correct status code", ret, Whitebox.invokeMethod(of13Provider, "getTunnelName", TYPE, inetAddress));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAddTunnelPort() throws Exception {
        ConfigurationService configurationService = mock(ConfigurationService.class);
        when(configurationService.getIntegrationBridgeName()).thenReturn(BR_INT);
        Southbound southbound = mock(Southbound.class);
        when(southbound.extractTerminationPointAugmentation(any(Node.class), anyString())).thenReturn(mock(OvsdbTerminationPointAugmentation.class));
        NodeId nodeId = mock(NodeId.class);
        when(nodeId.getValue()).thenReturn(ID);
        Node node = mock(Node.class);
        when(node.getNodeId()).thenReturn(nodeId);

        MemberModifier.suppress(MemberMatcher.method(OF13Provider.class, "getTunnelName", String.class, InetAddress.class));

        MemberModifier.field(OF13Provider.class, "configurationService").set(of13Provider , configurationService);
        MemberModifier.field(OF13Provider.class, "southbound").set(of13Provider , southbound);

        assertTrue("Error, did not add the port", (boolean) Whitebox.invokeMethod(of13Provider, "addTunnelPort", node, TYPE, mock(InetAddress.class), mock(InetAddress.class)));

        when(southbound.extractTerminationPointAugmentation(any(Node.class), anyString())).thenReturn(null);
        when(southbound.addTunnelTerminationPoint(any(Node.class), anyString(), anyString(), anyString(), any(HashMap.class))).thenReturn(false);

        assertFalse("Error, did add the port", (boolean) Whitebox.invokeMethod(of13Provider, "addTunnelPort", node, TYPE, mock(InetAddress.class), mock(InetAddress.class)));

        when(southbound.addTunnelTerminationPoint(any(Node.class), anyString(), anyString(), anyString(), any(HashMap.class))).thenReturn(true);

        assertTrue("Error, did not add the port", (boolean) Whitebox.invokeMethod(of13Provider, "addTunnelPort", node, TYPE, mock(InetAddress.class), mock(InetAddress.class)));
        PowerMockito.verifyPrivate(of13Provider, times(3)).invoke("getTunnelName", anyString(), any(InetAddress.class));
    }

    @Test
    public void testDeletePort() throws Exception {
        Southbound southbound = mock(Southbound.class);
        when(southbound.deleteTerminationPoint(any(Node.class), anyString())).thenReturn(false, true);
        MemberModifier.field(OF13Provider.class, "southbound").set(of13Provider , southbound);

        assertFalse("Error, did delete the port", (boolean) Whitebox.invokeMethod(of13Provider, "deletePort", mock(Node.class), TYPE, PORT));
        assertTrue("Error, did not delete the port", (boolean) Whitebox.invokeMethod(of13Provider, "deletePort", mock(Node.class), TYPE, PORT));
    }

    @Test
    public void testDeleteTunnelPort() throws Exception {
        ConfigurationService configurationService = mock(ConfigurationService.class);
        when(configurationService.getIntegrationBridgeName()).thenReturn(BR_INT);

        MemberModifier.field(OF13Provider.class, "configurationService").set(of13Provider , configurationService);

        MemberModifier.suppress(MemberMatcher.method(OF13Provider.class, "getTunnelName", String.class, InetAddress.class));
        MemberModifier.suppress(MemberMatcher.method(OF13Provider.class, "deletePort", Node.class, String.class, String.class));

        PowerMockito.when(of13Provider, "deletePort", any(Node.class), anyString(), anyString()).thenReturn(true);

        assertTrue("Error, did not delete the tunnel", (boolean) Whitebox.invokeMethod(of13Provider, "deleteTunnelPort", mock(Node.class), TYPE, mock(InetAddress.class), mock(InetAddress.class)));

        PowerMockito.verifyPrivate(of13Provider, times(1)).invoke("getTunnelName", anyString(), any(InetAddress.class));
        PowerMockito.verifyPrivate(of13Provider, times(1)).invoke("deletePort", any(Node.class), anyString(), anyString());

        PowerMockito.when(of13Provider, "deletePort", any(Node.class), anyString(), anyString()).thenReturn(false);

        assertFalse("Error, did delete the tunnel", (boolean) Whitebox.invokeMethod(of13Provider, "deleteTunnelPort", mock(Node.class), TYPE, mock(InetAddress.class), mock(InetAddress.class)));

        PowerMockito.verifyPrivate(of13Provider, times(2)).invoke("getTunnelName", anyString(), any(InetAddress.class));
        PowerMockito.verifyPrivate(of13Provider, times(2)).invoke("deletePort", any(Node.class), anyString(), anyString());
    }

    @Test
    public void testProgramLocalBridgeRules() throws Exception {
        MemberModifier.suppress(MemberMatcher.method(OF13Provider.class, "handleLocalInPort", Long.class, Short.class, Short.class, String.class, Long.class, String.class, boolean.class));
        MemberModifier.suppress(MemberMatcher.method(OF13Provider.class, "handleDropSrcIface", Long.class, Long.class, boolean.class));
        MemberModifier.suppress(MemberMatcher.method(OF13Provider.class, "handleLocalUcastOut", Long.class, Short.class, String.class, Long.class, String.class, boolean.class));
        MemberModifier.suppress(MemberMatcher.method(OF13Provider.class, "handleLocalBcastOut", Long.class, Short.class, String.class, Long.class, boolean.class));
        MemberModifier.suppress(MemberMatcher.method(OF13Provider.class, "handleTunnelFloodOut", Long.class, Short.class, Short.class, String.class, Long.class, boolean.class));
        MemberModifier.suppress(MemberMatcher.method(OF13Provider.class, "handleTunnelMiss", Long.class, Short.class, Short.class, String.class, boolean.class));
        MemberModifier.suppress(MemberMatcher.method(OF13Provider.class, "handleLocalTableMiss", Long.class, Short.class, String.class, boolean.class));


        Whitebox.invokeMethod(of13Provider, "programLocalBridgeRules", mock(Node.class), Long.valueOf("45"), SEG_ID, MAC_ADDRESS, LOCAL_PORT);

        PowerMockito.verifyPrivate(of13Provider, times(1)).invoke("handleLocalInPort", anyLong(), anyShort(), anyShort(), anyString(), anyLong(), anyString(), anyBoolean());
        PowerMockito.verifyPrivate(of13Provider, times(1)).invoke("handleDropSrcIface", anyLong(), anyLong(), anyBoolean());
        PowerMockito.verifyPrivate(of13Provider, times(1)).invoke("handleLocalUcastOut", anyLong(), anyShort(), anyString(), anyLong(), anyString(), anyBoolean());
        PowerMockito.verifyPrivate(of13Provider, times(1)).invoke("handleLocalBcastOut", anyLong(), anyShort(), anyString(), anyLong(), anyBoolean());
        PowerMockito.verifyPrivate(of13Provider, times(1)).invoke("handleTunnelFloodOut", anyLong(), anyShort(), anyShort(), anyString(), anyLong(), anyBoolean());
        PowerMockito.verifyPrivate(of13Provider, times(1)).invoke("handleTunnelMiss", anyLong(), anyShort(), anyShort(), anyString(), anyBoolean());
        PowerMockito.verifyPrivate(of13Provider, times(1)).invoke("handleLocalTableMiss", anyLong(), anyShort(), anyString(), anyBoolean());
    }

    @Test
    public void testRemoveLocalBridgeRules() throws Exception {
        MemberModifier.suppress(MemberMatcher.method(OF13Provider.class, "handleLocalInPort", Long.class, Short.class, Short.class, String.class, Long.class, String.class, boolean.class));
        MemberModifier.suppress(MemberMatcher.method(OF13Provider.class, "handleDropSrcIface", Long.class, Long.class, boolean.class));
        MemberModifier.suppress(MemberMatcher.method(OF13Provider.class, "handleLocalUcastOut", Long.class, Short.class, String.class, Long.class, String.class, boolean.class));
        MemberModifier.suppress(MemberMatcher.method(OF13Provider.class, "handleLocalBcastOut", Long.class, Short.class, String.class, Long.class, boolean.class));
        MemberModifier.suppress(MemberMatcher.method(OF13Provider.class, "handleTunnelFloodOut", Long.class, Short.class, Short.class, String.class, Long.class, boolean.class));


        Whitebox.invokeMethod(of13Provider, "removeLocalBridgeRules", mock(Node.class), Long.valueOf("45"), SEG_ID, MAC_ADDRESS, LOCAL_PORT);

        PowerMockito.verifyPrivate(of13Provider, times(1)).invoke("handleLocalInPort", anyLong(), anyShort(), anyShort(), anyString(), anyLong(), anyString(), anyBoolean());
        PowerMockito.verifyPrivate(of13Provider, times(1)).invoke("handleDropSrcIface", anyLong(), anyLong(), anyBoolean());
        PowerMockito.verifyPrivate(of13Provider, times(1)).invoke("handleLocalUcastOut", anyLong(), anyShort(), anyString(), anyLong(), anyString(), anyBoolean());
        PowerMockito.verifyPrivate(of13Provider, times(1)).invoke("handleLocalBcastOut", anyLong(), anyShort(), anyString(), anyLong(), anyBoolean());
        PowerMockito.verifyPrivate(of13Provider, times(1)).invoke("handleTunnelFloodOut", anyLong(), anyShort(), anyShort(), anyString(), anyLong(), anyBoolean());
    }

    @Test
    public void testProgramLocalIngressTunnelBridgeRules() throws Exception {
        MemberModifier.suppress(MemberMatcher.method(OF13Provider.class, "handleTunnelIn", Long.class, Short.class, Short.class, String.class, Long.class, boolean.class));
        MemberModifier.suppress(MemberMatcher.method(OF13Provider.class, "handleTunnelFloodOut", Long.class, Short.class, Short.class, String.class, Long.class, boolean.class));


        Whitebox.invokeMethod(of13Provider, "programLocalIngressTunnelBridgeRules", mock(Node.class), Long.valueOf("45"), SEG_ID, MAC_ADDRESS, LOCAL_PORT, LOCAL_PORT);

        PowerMockito.verifyPrivate(of13Provider, times(1)).invoke("handleTunnelIn", anyLong(), anyShort(), anyShort(), anyString(), anyLong(), anyBoolean());
        PowerMockito.verifyPrivate(of13Provider, times(1)).invoke("handleTunnelFloodOut", anyLong(), anyShort(), anyShort(), anyString(), anyLong(), anyBoolean());
    }

    @Test
    public void testProgramRemoteEgressTunnelBridgeRules() throws Exception {
        MemberModifier.suppress(MemberMatcher.method(OF13Provider.class, "handleTunnelOut", Long.class, Short.class, Short.class, String.class, Long.class, String.class, boolean.class));

        Whitebox.invokeMethod(of13Provider, "programRemoteEgressTunnelBridgeRules", mock(Node.class), Long.valueOf("45"), SEG_ID, MAC_ADDRESS, LOCAL_PORT, LOCAL_PORT);

        PowerMockito.verifyPrivate(of13Provider, times(1)).invoke("handleTunnelOut", anyLong(), anyShort(), anyShort(), anyString(), anyLong(), anyString(), anyBoolean());
    }

    @Test
    public void testRemovePerTunnelRules() throws Exception {
        MemberModifier.suppress(MemberMatcher.method(OF13Provider.class, "handleTunnelIn", Long.class, Short.class, Short.class, String.class, Long.class, boolean.class));
        MemberModifier.suppress(MemberMatcher.method(OF13Provider.class, "handleTunnelFloodOut", Long.class, Short.class, Short.class, String.class, Long.class, boolean.class));
        MemberModifier.suppress(MemberMatcher.method(OF13Provider.class, "handleTunnelMiss", Long.class, Short.class, Short.class, String.class, boolean.class));
        MemberModifier.suppress(MemberMatcher.method(OF13Provider.class, "handleLocalTableMiss", Long.class, Short.class, String.class, boolean.class));

        Whitebox.invokeMethod(of13Provider, "removePerTunnelRules", mock(Node.class), Long.valueOf("45"), SEG_ID, LOCAL_PORT);

        PowerMockito.verifyPrivate(of13Provider, times(1)).invoke("handleTunnelIn", anyLong(), anyShort(), anyShort(), anyString(), anyLong(), anyBoolean());
        PowerMockito.verifyPrivate(of13Provider, times(1)).invoke("handleTunnelFloodOut", anyLong(), anyShort(), anyShort(), anyString(), anyLong(), anyBoolean());
        PowerMockito.verifyPrivate(of13Provider, times(1)).invoke("handleTunnelMiss", anyLong(), anyShort(), anyShort(), anyString(), anyBoolean());
        PowerMockito.verifyPrivate(of13Provider, times(1)).invoke("handleLocalTableMiss", anyLong(), anyShort(), anyString(), anyBoolean());
    }

    @Test
    public void testProgramLocalVlanRules() throws Exception {
        MemberModifier.suppress(MemberMatcher.method(OF13Provider.class, "handleLocalInPortSetVlan", Long.class, Short.class, Short.class, String.class, Long.class, String.class, boolean.class));
        MemberModifier.suppress(MemberMatcher.method(OF13Provider.class, "handleDropSrcIface", Long.class, Long.class, boolean.class));
        MemberModifier.suppress(MemberMatcher.method(OF13Provider.class, "handleLocalVlanUcastOut", Long.class, Short.class, String.class, Long.class, String.class, boolean.class));

        Whitebox.invokeMethod(of13Provider, "programLocalVlanRules", mock(Node.class), Long.valueOf("45"), SEG_ID, MAC_ADDRESS, LOCAL_PORT);

        PowerMockito.verifyPrivate(of13Provider, times(1)).invoke("handleLocalInPortSetVlan", anyLong(), anyShort(), anyShort(), anyString(), anyLong(), anyString(), anyBoolean());
        PowerMockito.verifyPrivate(of13Provider, times(1)).invoke("handleDropSrcIface", anyLong(), anyLong(), anyBoolean());
        PowerMockito.verifyPrivate(of13Provider, times(1)).invoke("handleLocalVlanUcastOut", anyLong(), anyShort(), anyString(), anyLong(), anyString(), anyBoolean());
    }

    @Test
    public void testRemoveLocalVlanRules() throws Exception {
        MemberModifier.suppress(MemberMatcher.method(OF13Provider.class, "handleLocalInPortSetVlan", Long.class, Short.class, Short.class, String.class, Long.class, String.class, boolean.class));
        MemberModifier.suppress(MemberMatcher.method(OF13Provider.class, "handleDropSrcIface", Long.class, Long.class, boolean.class));
        MemberModifier.suppress(MemberMatcher.method(OF13Provider.class, "handleLocalVlanUcastOut", Long.class, Short.class, String.class, Long.class, String.class, boolean.class));

        Whitebox.invokeMethod(of13Provider, "programLocalVlanRules", mock(Node.class), Long.valueOf("45"), SEG_ID, MAC_ADDRESS, LOCAL_PORT);

        PowerMockito.verifyPrivate(of13Provider, times(1)).invoke("handleLocalInPortSetVlan", anyLong(), anyShort(), anyShort(), anyString(), anyLong(), anyString(), anyBoolean());
        PowerMockito.verifyPrivate(of13Provider, times(1)).invoke("handleDropSrcIface", anyLong(), anyLong(), anyBoolean());
        PowerMockito.verifyPrivate(of13Provider, times(1)).invoke("handleLocalVlanUcastOut", anyLong(), anyShort(), anyString(), anyLong(), anyString(), anyBoolean());
    }


    @Test
    public void testProgramLocalIngressVlanRules() throws Exception {
        MemberModifier.suppress(MemberMatcher.method(OF13Provider.class, "handleVlanIn", Long.class, Short.class, Short.class, String.class, Long.class, boolean.class));
        MemberModifier.suppress(MemberMatcher.method(OF13Provider.class, "handleLocalVlanBcastOut", Long.class, Short.class, String.class, Long.class, Long.class, boolean.class));

        Whitebox.invokeMethod(of13Provider, "programLocalIngressVlanRules", mock(Node.class), Long.valueOf("45"), SEG_ID, MAC_ADDRESS, LOCAL_PORT, LOCAL_PORT);

        PowerMockito.verifyPrivate(of13Provider, times(1)).invoke("handleVlanIn", anyLong(), anyShort(), anyShort(), anyString(), anyLong(), anyBoolean());
        PowerMockito.verifyPrivate(of13Provider, times(1)).invoke("handleLocalVlanBcastOut", anyLong(), anyShort(), anyString(), anyLong(), anyLong(), anyBoolean());
    }

    @Test
    public void testProgramRemoteEgressVlanRules() throws Exception {
        MemberModifier.suppress(MemberMatcher.method(OF13Provider.class, "handleVlanMiss", Long.class, Short.class, Short.class, String.class, Long.class, boolean.class));

        Whitebox.invokeMethod(of13Provider, "programRemoteEgressVlanRules", mock(Node.class), Long.valueOf("45"), SEG_ID, MAC_ADDRESS, LOCAL_PORT);

        PowerMockito.verifyPrivate(of13Provider, times(1)).invoke("handleVlanMiss", anyLong(), anyShort(), anyShort(), anyString(), anyLong(), anyBoolean());
    }

    @Test
    public void testRemoveRemoteEgressVlanRules() throws Exception {
        MemberModifier.suppress(MemberMatcher.method(OF13Provider.class, "handleLocalVlanBcastOut", Long.class, Short.class, String.class, Long.class, Long.class, boolean.class));

        Whitebox.invokeMethod(of13Provider, "removeRemoteEgressVlanRules", mock(Node.class), Long.valueOf("45"), SEG_ID, MAC_ADDRESS, LOCAL_PORT, LOCAL_PORT);

        PowerMockito.verifyPrivate(of13Provider, times(1)).invoke("handleLocalVlanBcastOut", anyLong(), anyShort(), anyString(), anyLong(), anyLong(), anyBoolean());
    }

    @Test
    public void testRemovePerVlanRules() throws Exception {
        MemberModifier.suppress(MemberMatcher.method(OF13Provider.class, "handleVlanIn", Long.class, Short.class, Short.class, String.class, Long.class, boolean.class));
        MemberModifier.suppress(MemberMatcher.method(OF13Provider.class, "handleVlanMiss", Long.class, Short.class, Short.class, String.class, Long.class, boolean.class));

        Whitebox.invokeMethod(of13Provider, "removePerVlanRules", mock(Node.class), Long.valueOf("45"), SEG_ID, LOCAL_PORT, LOCAL_PORT);

        PowerMockito.verifyPrivate(of13Provider, times(1)).invoke("handleVlanIn", anyLong(), anyShort(), anyShort(), anyString(), anyLong(), anyBoolean());
        PowerMockito.verifyPrivate(of13Provider, times(1)).invoke("handleVlanMiss", anyLong(), anyShort(), anyShort(), anyString(), anyLong(), anyBoolean());
    }

    @Test
    public void testHandleInterfaceUpdate() throws Exception{
        NeutronNetwork neutronNetwork = mock(NeutronNetwork.class);
        when(neutronNetwork.getProviderNetworkType()).thenReturn(NetworkHandler.NETWORK_TYPE_VLAN, NetworkHandler.NETWORK_TYPE_GRE);
        NodeCacheManager nodeCacheManager = mock(NodeCacheManager.class);
        Map<NodeId, Node> nodes = new HashMap<NodeId, Node>();
        nodes.put(mock(NodeId.class), mock(Node.class));
        when(nodeCacheManager.getOvsdbNodes()).thenReturn(nodes);
        Southbound southbound = mock(Southbound.class);
        when(southbound.extractBridgeOvsdbNodeId(any(Node.class))).thenReturn(mock(NodeId.class));
        when(southbound.getBridgeNode(any(Node.class), anyString())).thenReturn(mock(Node.class));
        ConfigurationService configurationService = mock(ConfigurationService.class);
        when(configurationService.getIntegrationBridgeName()).thenReturn(BR_INT);

        MemberModifier.field(OF13Provider.class, "nodeCacheManager").set(of13Provider , nodeCacheManager);
        MemberModifier.field(OF13Provider.class, "southbound").set(of13Provider , southbound);
        MemberModifier.field(OF13Provider.class, "configurationService").set(of13Provider , configurationService);

        MemberModifier.suppress(MemberMatcher.method(OF13Provider.class, "programLocalRules", String.class, String.class, Node.class, OvsdbTerminationPointAugmentation.class));
        MemberModifier.suppress(MemberMatcher.method(OF13Provider.class, "programVlanRules", NeutronNetwork.class, Node.class, OvsdbTerminationPointAugmentation.class));

        assertTrue("Error, did not update the interface correclty", of13Provider.handleInterfaceUpdate(neutronNetwork, mock(Node.class), mock(OvsdbTerminationPointAugmentation.class)));
        PowerMockito.verifyPrivate(of13Provider, times(1)).invoke("programLocalRules", anyString(), anyString(), any(Node.class), any(OvsdbTerminationPointAugmentation.class));
        PowerMockito.verifyPrivate(of13Provider, times(1)).invoke("programVlanRules", any(NeutronNetwork.class), any(Node.class), any(OvsdbTerminationPointAugmentation.class));

        when(configurationService.getTunnelEndPoint(any(Node.class))).thenReturn(mock(InetAddress.class));
        MemberModifier.suppress(MemberMatcher.method(OF13Provider.class, "addTunnelPort", Node.class, String.class, InetAddress.class, InetAddress.class));
        PowerMockito.when(of13Provider, "addTunnelPort", any(Node.class), anyString(), any(InetAddress.class), any(InetAddress.class)).thenReturn(true);
        MemberModifier.suppress(MemberMatcher.method(OF13Provider.class, "programTunnelRules", String.class, String.class, InetAddress.class, Node.class, OvsdbTerminationPointAugmentation.class, boolean.class));

        assertTrue("Error, did not update the interface correclty", of13Provider.handleInterfaceUpdate(neutronNetwork, mock(Node.class), mock(OvsdbTerminationPointAugmentation.class)));
        PowerMockito.verifyPrivate(of13Provider, times(2)).invoke("addTunnelPort", any(Node.class), anyString(), any(InetAddress.class), any(InetAddress.class));
        PowerMockito.verifyPrivate(of13Provider, times(2)).invoke("programTunnelRules", anyString(), anyString(), any(InetAddress.class), any(Node.class), any(OvsdbTerminationPointAugmentation.class), anyBoolean());
    }

    private static final String INTF = "interface";

    @SuppressWarnings("unchecked")
    @Test
    public void testHandlerInterfaceDelete() throws Exception {
        NeutronNetwork neutronNetwork = mock(NeutronNetwork.class);
        NodeCacheManager nodeCacheManager = mock(NodeCacheManager.class);
        NodeId nodeId = mock(NodeId.class);
        when(nodeId.getValue()).thenReturn(ID);
        Node node = mock(Node.class);
        when(node.getNodeId()).thenReturn(nodeId);
        Map<NodeId, Node> nodes = new HashMap<NodeId, Node>();
        nodes.put(mock(NodeId.class), node);
        when(nodeCacheManager.getOvsdbNodes()).thenReturn(nodes);
        Southbound southbound = mock(Southbound.class);
        when(southbound.extractBridgeOvsdbNodeId(any(Node.class))).thenReturn(mock(NodeId.class));
        when(southbound.isTunnel(any(OvsdbTerminationPointAugmentation.class))).thenReturn(true);
        when(southbound.getOptionsValue(any(List.class), anyString())).thenReturn(IP);
        OvsdbTerminationPointAugmentation intf = mock(OvsdbTerminationPointAugmentation.class);
        when(intf.getName()).thenReturn(INTF);
        List<String> intfs = new ArrayList<String>();
        intfs.add(INTF);
        BridgeConfigurationManager bridgeConfigurationManager = mock(BridgeConfigurationManager.class);
        when(bridgeConfigurationManager.getAllPhysicalInterfaceNames(any(Node.class))).thenReturn(intfs);
        ConfigurationService configurationService = mock(ConfigurationService.class);
        when(configurationService.getTunnelEndPoint(any(Node.class))).thenReturn(mock(InetAddress.class));

        PowerMockito.mockStatic(InetAddress.class);
        PowerMockito.when(InetAddress.getByName(anyString())).thenReturn(mock(InetAddress.class));

        PowerMockito.mockStatic(MdsalHelper.class);
        PowerMockito.when(MdsalHelper.createOvsdbInterfaceType(any(Class.class))).thenReturn(INTF);

        MemberModifier.field(OF13Provider.class, "nodeCacheManager").set(of13Provider , nodeCacheManager);
        MemberModifier.field(OF13Provider.class, "southbound").set(of13Provider , southbound);
        MemberModifier.field(OF13Provider.class, "bridgeConfigurationManager").set(of13Provider , bridgeConfigurationManager);
        MemberModifier.field(OF13Provider.class, "configurationService").set(of13Provider , configurationService);

        MemberModifier.suppress(MemberMatcher.method(OF13Provider.class, "deleteTunnelPort", Node.class, String.class, InetAddress.class, InetAddress.class));

        assertTrue("Error, did not delete the interface correclty", of13Provider.handleInterfaceDelete(TYPE,  neutronNetwork, mock(Node.class), intf, false));
        PowerMockito.verifyPrivate(of13Provider, times(1)).invoke("deleteTunnelPort", any(Node.class), anyString(), any(InetAddress.class), any(InetAddress.class));

        when(southbound.isTunnel(any(OvsdbTerminationPointAugmentation.class))).thenReturn(false);
        MemberModifier.suppress(MemberMatcher.method(OF13Provider.class, "deletePhysicalPort", Node.class, String.class));

        assertTrue("Error, did not delete the interface correclty", of13Provider.handleInterfaceDelete(TYPE,  neutronNetwork, mock(Node.class), intf, false));
        PowerMockito.verifyPrivate(of13Provider, times(1)).invoke("deletePhysicalPort", any(Node.class), anyString());

        intfs.clear();
        MemberModifier.suppress(MemberMatcher.method(OF13Provider.class, "removeLocalRules", String.class, String.class, Node.class, OvsdbTerminationPointAugmentation.class));
        MemberModifier.suppress(MemberMatcher.method(OF13Provider.class, "removeVlanRules", NeutronNetwork.class, Node.class, OvsdbTerminationPointAugmentation.class, boolean.class));
        when(neutronNetwork.getProviderNetworkType()).thenReturn(NetworkHandler.NETWORK_TYPE_VLAN);

        assertTrue("Error, did not delete the interface correclty", of13Provider.handleInterfaceDelete(TYPE,  neutronNetwork, mock(Node.class), intf, false));
        PowerMockito.verifyPrivate(of13Provider, times(1)).invoke("removeLocalRules",  anyString(), anyString(), any(Node.class), any(OvsdbTerminationPointAugmentation.class));
        PowerMockito.verifyPrivate(of13Provider, times(1)).invoke("removeVlanRules",  any(NeutronNetwork.class), any(Node.class), any(OvsdbTerminationPointAugmentation.class), anyBoolean());

        when(neutronNetwork.getProviderNetworkType()).thenReturn(NetworkHandler.NETWORK_TYPE_GRE);
        when(southbound.getBridgeNode(any(Node.class), anyString())).thenReturn(node);
        MemberModifier.suppress(MemberMatcher.method(OF13Provider.class, "removeTunnelRules", String.class, String.class, InetAddress.class, Node.class, OvsdbTerminationPointAugmentation.class, boolean.class, boolean.class));

        assertTrue("Error, did not delete the interface correclty", of13Provider.handleInterfaceDelete(TYPE,  neutronNetwork, node, intf, false));
        PowerMockito.verifyPrivate(of13Provider, times(2)).invoke("removeTunnelRules", anyString(), anyString(), any(InetAddress.class), any(Node.class), any(OvsdbTerminationPointAugmentation.class), any(boolean.class), any(boolean.class));
    }

    // Problem with methods signatures: initializeFlowRules(Node) has the same signature than initializeFlowRules(Node, String)
//    @Test
//    public void testInitializeFlowRules() throws Exception {
//        MemberModifier.suppress(MemberMatcher.method(OF13Provider.class, "initializeFlowRules", Node.class, String.class));
//        MemberModifier.suppress(MemberMatcher.method(OF13Provider.class, "triggerInterfaceUpdates", Node.class));
//
//        of13Provider.initializeFlowRules(mock(Node.class));
//
//        PowerMockito.verifyPrivate(of13Provider, times(2)).invoke("initializeFlowRules", any(Node.class), anyString());
//        PowerMockito.verifyPrivate(of13Provider, times(1)).invoke("triggerInterfaceUpdates", any(Node.class));
//    }

    @Test
    public void testInitializeOFFlowRule() throws Exception{
        Southbound southbound = mock(Southbound.class);
        when(southbound.getBridgeName(any(Node.class))).thenReturn(BR_INT);
        ConfigurationService configurationService = mock(ConfigurationService.class);
        when(configurationService.getIntegrationBridgeName()).thenReturn(BR_INT);

        MemberModifier.field(OF13Provider.class, "southbound").set(of13Provider , southbound);
        MemberModifier.field(OF13Provider.class, "configurationService").set(of13Provider , configurationService);

        MemberModifier.suppress(MemberMatcher.method(OF13Provider.class, "initializeFlowRules", Node.class, String.class));
        MemberModifier.suppress(MemberMatcher.method(OF13Provider.class, "triggerInterfaceUpdates", Node.class));

        of13Provider.initializeOFFlowRules(mock(Node.class));

        PowerMockito.verifyPrivate(of13Provider, times(1)).invoke("initializeFlowRules", any(Node.class), anyString());
        PowerMockito.verifyPrivate(of13Provider, times(1)).invoke("triggerInterfaceUpdates", any(Node.class));
    }

    @Test
    public void testSetDependencies() throws Exception {
        ConfigurationService configurationService = mock(ConfigurationService.class);
        TenantNetworkManager tenantNetworkManager = mock(TenantNetworkManager.class);
        BridgeConfigurationManager bridgeConfigurationManager = mock(BridgeConfigurationManager.class);
        NodeCacheManager nodeCacheManager = mock(NodeCacheManager.class);
        ClassifierProvider classifierProvider = mock(ClassifierProvider.class);
        IngressAclProvider ingressAclProvider = mock(IngressAclProvider.class);
        EgressAclProvider egressAclProvider = mock(EgressAclProvider.class);
        L2ForwardingProvider l2ForwardingProvider = mock(L2ForwardingProvider.class);
        SecurityServicesManager securityServicesManager = mock(SecurityServicesManager.class);
        Southbound southbound = mock(Southbound.class);

        PowerMockito.mockStatic(ServiceHelper.class);
        PowerMockito.when(ServiceHelper.getGlobalInstance(ConfigurationService.class, of13Provider)).thenReturn(configurationService);
        PowerMockito.when(ServiceHelper.getGlobalInstance(TenantNetworkManager.class, of13Provider)).thenReturn(tenantNetworkManager);
        PowerMockito.when(ServiceHelper.getGlobalInstance(BridgeConfigurationManager.class, of13Provider)).thenReturn(bridgeConfigurationManager);
        PowerMockito.when(ServiceHelper.getGlobalInstance(NodeCacheManager.class, of13Provider)).thenReturn(nodeCacheManager);
        PowerMockito.when(ServiceHelper.getGlobalInstance(ClassifierProvider.class, of13Provider)).thenReturn(classifierProvider);
        PowerMockito.when(ServiceHelper.getGlobalInstance(IngressAclProvider.class, of13Provider)).thenReturn(ingressAclProvider);
        PowerMockito.when(ServiceHelper.getGlobalInstance(EgressAclProvider.class, of13Provider)).thenReturn(egressAclProvider);
        PowerMockito.when(ServiceHelper.getGlobalInstance(L2ForwardingProvider.class, of13Provider)).thenReturn(l2ForwardingProvider);
        PowerMockito.when(ServiceHelper.getGlobalInstance(SecurityServicesManager.class, of13Provider)).thenReturn(securityServicesManager);
        PowerMockito.when(ServiceHelper.getGlobalInstance(Southbound.class, of13Provider)).thenReturn(southbound);

        of13Provider.setDependencies(mock(BundleContext.class), mock(ServiceReference.class));

        assertEquals("Error, did not return the correct object", getField("configurationService"), configurationService);
        assertEquals("Error, did not return the correct object", getField("tenantNetworkManager"), tenantNetworkManager);
        assertEquals("Error, did not return the correct object", getField("bridgeConfigurationManager"), bridgeConfigurationManager);
        assertEquals("Error, did not return the correct object", getField("nodeCacheManager"), nodeCacheManager);
        assertEquals("Error, did not return the correct object", getField("classifierProvider"), classifierProvider);
        assertEquals("Error, did not return the correct object", getField("ingressAclProvider"), ingressAclProvider);
        assertEquals("Error, did not return the correct object", getField("egressAclProvider"), egressAclProvider);
        assertEquals("Error, did not return the correct object", getField("l2ForwardingProvider"), l2ForwardingProvider);
        assertEquals("Error, did not return the correct object", getField("securityServicesManager"), securityServicesManager);
        assertEquals("Error, did not return the correct object", getField("southbound"), southbound);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSetDependenciesObject() throws Exception{
        NetworkingProviderManager networkingProviderManager = mock(NetworkingProviderManager.class);
        BundleContext bundleContext = mock(BundleContext.class);
        when(bundleContext.getServiceReference(NetworkingProvider.class.getName())).thenReturn(mock(ServiceReference.class));

        MemberModifier.field(OF13Provider.class, "bundleContext").set(of13Provider , bundleContext);

        of13Provider.setDependencies(networkingProviderManager);
        assertEquals("Error, did not return the correct object", getField("networkingProviderManager"), networkingProviderManager);
}

    private Object getField(String fieldName) throws Exception {
        Field field = OF13Provider.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(of13Provider);
    }
}