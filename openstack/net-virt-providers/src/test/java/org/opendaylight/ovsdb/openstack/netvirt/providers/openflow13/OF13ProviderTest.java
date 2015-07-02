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

import java.net.InetAddress;
import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendaylight.ovsdb.openstack.netvirt.api.BridgeConfigurationManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.ConfigurationService;
import org.opendaylight.ovsdb.openstack.netvirt.api.Southbound;
import org.opendaylight.ovsdb.openstack.netvirt.api.Status;
import org.opendaylight.ovsdb.openstack.netvirt.api.StatusCode;
import org.opendaylight.ovsdb.openstack.netvirt.api.TenantNetworkManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberMatcher;
import org.powermock.api.support.membermodification.MemberModifier;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
//import org.opendaylight.ovsdb.plugin.api.OvsdbConnectionService;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.powermock.reflect.Whitebox;

/**
 * Unit test for {@link OF13Provider}
 */
@PrepareForTest({OF13Provider.class, InetAddress.class})
@RunWith(PowerMockRunner.class)
public class OF13ProviderTest {

    @Mock private OF13Provider of13Provider;

    private static final String TUNNEL_TYPE = "gre";
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

        String ret = TUNNEL_TYPE + "-" + inetAddress.getHostAddress();
        assertEquals("Error, did not return the correct status code", ret, Whitebox.invokeMethod(of13Provider, "getTunnelName", TUNNEL_TYPE, inetAddress));
    }

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

        assertTrue("Error, did not add the port", (boolean) Whitebox.invokeMethod(of13Provider, "addTunnelPort", node, TUNNEL_TYPE, mock(InetAddress.class), mock(InetAddress.class)));

        when(southbound.extractTerminationPointAugmentation(any(Node.class), anyString())).thenReturn(null);
        when(southbound.addTunnelTerminationPoint(any(Node.class), anyString(), anyString(), anyString(), any(HashMap.class))).thenReturn(false);

        assertFalse("Error, did add the port", (boolean) Whitebox.invokeMethod(of13Provider, "addTunnelPort", node, TUNNEL_TYPE, mock(InetAddress.class), mock(InetAddress.class)));

        when(southbound.addTunnelTerminationPoint(any(Node.class), anyString(), anyString(), anyString(), any(HashMap.class))).thenReturn(true);

        assertTrue("Error, did not add the port", (boolean) Whitebox.invokeMethod(of13Provider, "addTunnelPort", node, TUNNEL_TYPE, mock(InetAddress.class), mock(InetAddress.class)));
        PowerMockito.verifyPrivate(of13Provider, times(3)).invoke("getTunnelName", anyString(), any(InetAddress.class));
    }

    @Test
    public void testDeletePort() throws Exception {
        Southbound southbound = mock(Southbound.class);
        when(southbound.deleteTerminationPoint(any(Node.class), anyString())).thenReturn(false, true);
        MemberModifier.field(OF13Provider.class, "southbound").set(of13Provider , southbound);

        assertFalse("Error, did delete the port", (boolean) Whitebox.invokeMethod(of13Provider, "deletePort", mock(Node.class), TUNNEL_TYPE, PORT));
        assertTrue("Error, did not delete the port", (boolean) Whitebox.invokeMethod(of13Provider, "deletePort", mock(Node.class), TUNNEL_TYPE, PORT));
    }

    @Test
    public void testDeleteTunnelPort() throws Exception {
        ConfigurationService configurationService = mock(ConfigurationService.class);
        when(configurationService.getIntegrationBridgeName()).thenReturn(BR_INT);

        MemberModifier.field(OF13Provider.class, "configurationService").set(of13Provider , configurationService);

        MemberModifier.suppress(MemberMatcher.method(OF13Provider.class, "getTunnelName", String.class, InetAddress.class));
        MemberModifier.suppress(MemberMatcher.method(OF13Provider.class, "deletePort", Node.class, String.class, String.class));

        PowerMockito.when(of13Provider, "deletePort", any(Node.class), anyString(), anyString()).thenReturn(true);

        assertTrue("Error, did not delete the tunnel", (boolean) Whitebox.invokeMethod(of13Provider, "deleteTunnelPort", mock(Node.class), TUNNEL_TYPE, mock(InetAddress.class), mock(InetAddress.class)));

        PowerMockito.verifyPrivate(of13Provider, times(1)).invoke("getTunnelName", anyString(), any(InetAddress.class));
        PowerMockito.verifyPrivate(of13Provider, times(1)).invoke("deletePort", any(Node.class), anyString(), anyString());

        PowerMockito.when(of13Provider, "deletePort", any(Node.class), anyString(), anyString()).thenReturn(false);

        assertFalse("Error, did delete the tunnel", (boolean) Whitebox.invokeMethod(of13Provider, "deleteTunnelPort", mock(Node.class), TUNNEL_TYPE, mock(InetAddress.class), mock(InetAddress.class)));

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

//    /**
//     * Tests for defaults
//     *      getName()
//     *      supportsServices()
//     *      hasPerTenantTunneling()
//     */
//    @Test
//    public void verifyObjectDefaultSettings(){
//        assertEquals("Error, getName() - Default provider name is invalid","OF13Provider",of13Provider.getName());
//        assertEquals("Error, supportsServices() - Support services is disabled", true, of13Provider.supportsServices());
//        assertEquals("Error, hasPerTenantTunneling() - Support for per tenant tunnelling is enabled", false, of13Provider.hasPerTenantTunneling());
//    }
//
//    /**
//     * Test method
//     * {@link OF13Provider#notifyFlowCapableNodeEventTest(Long, Action)}
//     */
//    @Test
//    public void notifyFlowCapableNodeEventTest(){
//
//        long flowId = 100;
//        Action action = Action.ADD;
//
//        //of13Provider.notifyFlowCapableNodeEvent(flowId, action);
//        //verify(mdsalConsumer, times(1)).notifyFlowCapableNodeCreateEvent(Constants.OPENFLOW_NODE_PREFIX + flowId, action);
//    }
//
//    /**
//     * Test method
//     * {@link OF13Provider#initializeFlowRules(Node)}
//     */
//    @Test
//    public void initializeFlowRulesTest(){
//
//        //Row row = Mockito.mock(Row.class);
//        //when(ovsdbConfigurationService.getTypedRow(node, Interface.class, row)).thenReturn(intf);
//
//        //ConcurrentHashMap<String, Row> intfs = new ConcurrentHashMap();
//        //intfs.put("intf1", row);
//
//        NeutronNetwork network = Mockito.mock(NeutronNetwork.class);
//        when(network.getProviderNetworkType()).thenReturn(NetworkHandler.NETWORK_TYPE_VLAN);
//        //when(tenantNetworkManager.getTenantNetwork(intf)).thenReturn(network);
//        //when(ovsdbConfigurationService.getRows(node, ovsdbConfigurationService.getTableName(node, Interface.class))).thenReturn(intfs);
//
////        of13Provider.initializeFlowRules(node);
//
//        /**
//         * The final phase of the initialization process is to call triggerInterfaceUpdates(node)
//         * This must call tenantNetworkManager.getTenantNetwork(Interface) for each interface.
//         * Verify that this is called once since we are initializing flow rules for only one interface.
//         */
//        /* TODO SB_MIGRATION */
//        //verify(tenantNetworkManager, times(1)).getTenantNetwork(intf);
//    }
//
//    /**
//     * Test method
//     * {@link OF13Provider#initializeOFFlowRulesTest(Node)}
//     */
//    @Test
//    public void initializeOFFlowRulesTest(){
//        /* TODO SB_MIGRATION */
//        //of13Provider.initializeOFFlowRules(openflowNode);
//        //verify(connectionService, times(1)).getBridgeNodes();
//    }
//
//    /**
//     * Test method
//     * {@link OF13Provider#handleInterfaceUpdateTest(NeutronNetwork, Node, Interface)}
//     */
//    @Test
//    public void handleInterfaceUpdateTest(){
//        NeutronNetwork network = Mockito.mock(NeutronNetwork.class);
//
//        /**
//         * For Vlan network type, test ensures that all parameter validations
//         * passed by ensuring that ovsdbConfigurationService.getRows(node,"interface_table_name))
//         * is called at least once.
//         */
//
//        when(network.getProviderNetworkType()).thenReturn(NetworkHandler.NETWORK_TYPE_VLAN);
//        /* TODO SB_MIGRATION */
//        //this.of13Provider.handleInterfaceUpdate(network, node, intf);
//        //verify(ovsdbConfigurationService, times(1)).getRows(node, ovsdbConfigurationService.getTableName(node, Interface.class));
//
//        /**
//         * Ideally we want to verify that the right rule tables are constructed for
//         * each type of network (vlan, gre, vxlan). However, to simplify things, we just
//         * verify that configurationService.getTunnelEndPoint() is called twice for the appropriate
//         * network types and for each of the two remaining nodes.
//         */
//
//        when(network.getProviderNetworkType()).thenReturn(NetworkHandler.NETWORK_TYPE_GRE);
//        /* TODO SB_MIGRATION */
//        //this.of13Provider.handleInterfaceUpdate(network, node, intf);this.of13Provider.handleInterfaceUpdate(network, node, intf);
//        /* TODO SB_MIGRATION */
//        //verify(configurationService, times(4)).getTunnelEndPoint(node);
//
//        when(network.getProviderNetworkType()).thenReturn(NetworkHandler.NETWORK_TYPE_VXLAN);
//        /* TODO SB_MIGRATION */
//        //this.of13Provider.handleInterfaceUpdate(network, node, intf);this.of13Provider.handleInterfaceUpdate(network, node, intf);
//        //verify(configurationService, times(8)).getTunnelEndPoint(node);
//
//        //assertEquals("Error, handleInterfaceUpdate(String, String) - is returning a non NULL value.", null, this.of13Provider.handleInterfaceUpdate("",""));
//    }
//
//    /**
//     * Test method
//     * {@link OF13Provider#handleInterfaceDelete(String, NeutronNetwork, Node, Interface, boolean)}
//     */
//    @Test
//    public void handleInterfaceDeleteTest(){
//        NeutronNetwork network = Mockito.mock(NeutronNetwork.class);
//
//
//        when(network.getProviderNetworkType()).thenReturn(NetworkHandler.NETWORK_TYPE_VLAN);
////        when(bridgeConfigurationManager.getAllPhysicalInterfaceNames(node)).thenReturn(Arrays.asList(new String[] { "eth0", "eth1" ,"eth2"}));
//
//        //Column<GenericTableSchema, String> typeColumn = Mockito.mock(Column.class);
//        //when(typeColumn.getData()).thenReturn(NetworkHandler.NETWORK_TYPE_VXLAN);
//        //when(intf.getTypeColumn()).thenReturn(typeColumn);
//
//        Map<String, String> options = new HashMap();
//        options.put("local_ip", "192.168.0.1");
//        options.put("remote_ip", "10.0.12.0");
//
//        //Column<GenericTableSchema, Map<String, String>> optionColumns = Mockito.mock(Column.class);
//        //when(intf.getOptionsColumn()).thenReturn(optionColumns);
//
//        /* TODO SB_MIGRATION */
//        Status status = null;//this.of13Provider.handleInterfaceDelete("tunnel1", network, node, intf, true);
//
//        assertEquals("Error, handleInterfaceDelete(String, NeutronNetwor, Node, Interface, boolean) - returned the wrong status.", new Status(StatusCode.SUCCESS), status);
//    }
//
//
//    /**
//     * Test method
//     * {@link OF13Provider#createNodeBuilderTest(String)}
//     */
//    @Test
//    public void createNodeBuilderTest(){
//        final String nodeId="node1";
//
//        NodeBuilder builder = new NodeBuilder();
//        builder.setId(new org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId(nodeId));
//        builder.setKey(new org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey(builder.getId()));
//
//        NodeBuilder builderStatic = OF13Provider.createNodeBuilder(nodeId);
//
//        assertEquals("Error, createNodeBuilder() returned an invalid Node Builder Id", builderStatic.getId(), builder.getId());
//        assertEquals("Error, createNodeBuilder() returned an invalid Node Builder key", builderStatic.getKey(), builder.getKey());
//    }
//
//    /**
//     * Seeds mock dependencies into the of13Provider object
//     * @throws Exception
//     */
//    private void SeedMockDependencies() throws Exception{
//
////        SeedClassFieldValue(of13Provider, "configurationService", configurationService);
////        SeedClassFieldValue(of13Provider, "bridgeConfigurationManager", bridgeConfigurationManager);
////        SeedClassFieldValue(of13Provider, "tenantNetworkManager", tenantNetworkManager);
////        /* TODO SB_MIGRATION */
////        //SeedClassFieldValue(of13Provider, "ovsdbConfigurationService", ovsdbConfigurationService);
////        //SeedClassFieldValue(of13Provider, "connectionService", connectionService);
////        //SeedClassFieldValue(of13Provider, "mdsalConsumer", mdsalConsumer);
////        SeedClassFieldValue(of13Provider, "securityServicesManager", securityServicesManager);
////        SeedClassFieldValue(of13Provider, "ingressAclProvider", ingressAclProvider);
////        SeedClassFieldValue(of13Provider, "egressAclProvider", egressAclProvider);
////        SeedClassFieldValue(of13Provider, "classifierProvider", classifierProvider);
////        SeedClassFieldValue(of13Provider, "l2ForwardingProvider", l2ForwardingProvider);
////        SeedClassFieldValue(of13Provider, "dataBroker", dataBroker);
//    }
//
//    /**
//     * Get the specified field from OF13Provider using reflection
//     * @param instance - the class instance
//     * @param fieldName - the field to retrieve
//     *
//     * @return the desired field
//     */
//    private Object getClassField(OF13Provider instance, String fieldName) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException{
//        Field field = OF13Provider.class.getDeclaredField(fieldName);
//        field.setAccessible(true);
//        return field.get(instance);
//    }
//
//    /**
//     * Sets the internal value of a field from OF13Provider using reflection
//     * @param instance
//     * @param fieldName
//     * @param value
//     * @throws NoSuchFieldException
//     * @throws SecurityException
//     * @throws IllegalArgumentException
//     * @throws IllegalAccessException
//     */
//    private void SeedClassFieldValue(OF13Provider instance, String fieldName, Object value)throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException{
//
//        Field field = OF13Provider.class.getDeclaredField(fieldName);
//        field.setAccessible(true);
//        field.set(instance, value);
//    }
}
