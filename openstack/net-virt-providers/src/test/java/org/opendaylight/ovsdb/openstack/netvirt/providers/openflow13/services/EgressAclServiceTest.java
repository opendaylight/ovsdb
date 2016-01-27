/*
 * Copyright (c) 2015 Inocybe and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.services;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronSecurityGroup;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronSecurityRule;
import org.opendaylight.ovsdb.openstack.netvirt.translator.Neutron_IPs;
import org.opendaylight.ovsdb.openstack.netvirt.api.SecurityGroupCacheManger;
import org.opendaylight.ovsdb.openstack.netvirt.api.SecurityServicesManager;
import org.opendaylight.ovsdb.openstack.netvirt.providers.NetvirtProvidersProvider;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.PipelineOrchestrator;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.Service;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Icmpv4Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Icmpv6Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.TcpMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.UdpMatch;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberModifier;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.util.concurrent.CheckedFuture;
/**
 * Unit test for {@link EgressAclService}
 */
@RunWith(PowerMockRunner.class)
@SuppressWarnings("unchecked")
public class EgressAclServiceTest {

    @InjectMocks private EgressAclService egressAclService = new EgressAclService();
    private EgressAclService egressAclServiceSpy;

    @Mock private DataBroker dataBroker;
    @Mock private PipelineOrchestrator orchestrator;

    @Mock private WriteTransaction writeTransaction;
    @Mock private CheckedFuture<Void, TransactionCommitFailedException> commitFuture;

    @Mock private NeutronSecurityGroup securityGroup;
    @Mock private NeutronSecurityRule portSecurityRule;
    @Mock private NeutronSecurityGroup securityGroupIpv6;
    @Mock private NeutronSecurityRule portSecurityIpv6Rule;

    @Mock private SecurityServicesManager securityServices;
    @Mock private SecurityGroupCacheManger securityGroupCacheManger;

    private Neutron_IPs neutron_ip_src;
    private Neutron_IPs neutron_ip_dest_1;
    private Neutron_IPs neutron_ip_dest_2;
    private Neutron_IPs neutron_ipv6_dest_1;
    private Neutron_IPs neutron_ipv6_dest_2;
    private List<Neutron_IPs> neutronSrcIpList = new ArrayList<>();
    private List<Neutron_IPs> neutronDestIpList = new ArrayList<>();
    private static final String MAC_ADDRESS = "87:1D:5E:02:40:B7";
    private static final String SRC_IP = "192.168.0.1";
    private static final String DEST_IP_1 = "192.169.0.1";
    private static final String DEST_IP_2 = "192.169.0.2";
    private static final String IPV6_DEST_IP_1 = "2001:db8:2::200";
    private static final String IPV6_DEST_IP_2 = "2001:db8:2::201";
    private static final String SECURITY_GROUP_UUID = "85cc3048-abc3-43cc-89b3-377341426ac5";
    private static final String PORT_UUID = "95cc3048-abc3-43cc-89b3-377341426ac5";
    private static final Long IPV6_ETHER_TYPE = (long) 0x86DD;
    private static final Long IPV4_ETHER_TYPE = (long) 0x0800;
    private static final String SEGMENT_ID = "2";
    private static final Long DP_ID_LONG = (long) 1554;
    private static final Long LOCAL_PORT = (long) 124;
    private static final int PORT_RANGE_MIN = 1;
    private static final int PORT_RANGE_MAX = 65535;
    private static FlowBuilder flowBuilder;
    private static NodeBuilder nodeBuilder;

    private static Answer<Object> answer() {
        return new Answer<Object>() {
            @Override
            public CheckedFuture<Void, TransactionCommitFailedException> answer(InvocationOnMock invocation)
                    throws Throwable {
                flowBuilder = (FlowBuilder) invocation.getArguments()[0];
                nodeBuilder = (NodeBuilder) invocation.getArguments()[1];
                return null;
            }
        };
    }

    @Before
    public void setUp() throws IllegalArgumentException, IllegalAccessException {
        egressAclServiceSpy = PowerMockito.spy(egressAclService);

        when(writeTransaction.submit()).thenReturn(commitFuture);

        when(dataBroker.newWriteOnlyTransaction()).thenReturn(writeTransaction);

        when(orchestrator.getNextServiceInPipeline(any(Service.class))).thenReturn(Service.ARP_RESPONDER);

        portSecurityRule = mock(NeutronSecurityRule.class);
        portSecurityIpv6Rule = mock(NeutronSecurityRule.class);

        when(portSecurityRule.getSecurityRuleEthertype()).thenReturn("IPv4");
        when(portSecurityRule.getSecurityRuleDirection()).thenReturn("egress");
        when(portSecurityIpv6Rule.getSecurityRuleEthertype()).thenReturn("IPv6");
        when(portSecurityIpv6Rule.getSecurityRuleDirection()).thenReturn("egress");

        List<NeutronSecurityRule> portSecurityList = new ArrayList<>();
        portSecurityList.add(portSecurityRule);

        neutron_ip_src = new Neutron_IPs();
        neutron_ip_src.setIpAddress(SRC_IP);
        neutronSrcIpList.add(neutron_ip_src);

        neutron_ip_dest_1 = new Neutron_IPs();
        neutron_ip_dest_1.setIpAddress(DEST_IP_1);
        neutronDestIpList.add(neutron_ip_dest_1);

        neutron_ip_dest_2 = new Neutron_IPs();
        neutron_ip_dest_2.setIpAddress(DEST_IP_2);
        neutronDestIpList.add(neutron_ip_dest_2);

        List<NeutronSecurityRule> portSecurityIpv6List = new ArrayList<>();
        portSecurityIpv6List.add(portSecurityIpv6Rule);
        when(securityGroupIpv6.getSecurityRules()).thenReturn(portSecurityIpv6List);

        neutron_ipv6_dest_1 = new Neutron_IPs();
        neutron_ipv6_dest_1.setIpAddress(IPV6_DEST_IP_1);
        neutronDestIpList.add(neutron_ipv6_dest_1);

        neutron_ipv6_dest_2 = new Neutron_IPs();
        neutron_ipv6_dest_2.setIpAddress(IPV6_DEST_IP_2);
        neutronDestIpList.add(neutron_ipv6_dest_2);

        when(securityGroup.getSecurityRules()).thenReturn(portSecurityList);
        when(securityServices.getVmListForSecurityGroup(PORT_UUID, SECURITY_GROUP_UUID)).thenReturn(neutronDestIpList);

        NetvirtProvidersProvider netvirtProvider = mock(NetvirtProvidersProvider.class);
        MemberModifier.field(NetvirtProvidersProvider.class, "hasProviderEntityOwnership").set(netvirtProvider, new AtomicBoolean(true));

    }

    /**
     * Test method {@link EgressAclService#programPortSecurityGroup(java.lang.Long, java.lang.String,
     * java.lang.String, long, org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronSecurityGroup,
     * java.lang.String, boolean)} when portSecurityRule is incomplete
     */
    @Test
    public void testProgramPortSecurityGroupWithIncompleteRule() throws Exception {
        NeutronSecurityRule portSecurityRule1 = mock(NeutronSecurityRule.class);
        when(portSecurityRule1.getSecurityRuleEthertype()).thenReturn("IPv4");
        when(portSecurityRule1.getSecurityRuleDirection()).thenReturn("not_egress");  // other direction

        NeutronSecurityRule portSecurityRule2 = mock(NeutronSecurityRule.class);
        when(portSecurityRule2.getSecurityRuleEthertype()).thenReturn(null);
        when(portSecurityRule2.getSecurityRuleDirection()).thenReturn("egress");

        NeutronSecurityRule portSecurityRule3 = mock(NeutronSecurityRule.class);
        when(portSecurityRule3.getSecurityRuleEthertype()).thenReturn("IPv4");
        when(portSecurityRule3.getSecurityRuleDirection()).thenReturn(null);

        NeutronSecurityRule portSecurityRule4 = mock(NeutronSecurityRule.class);
        when(portSecurityRule4.getSecurityRuleEthertype()).thenReturn(null);
        when(portSecurityRule4.getSecurityRuleDirection()).thenReturn(null);

        List<NeutronSecurityRule> portSecurityList = new ArrayList<>();
        portSecurityList.add(null);
        portSecurityList.add(portSecurityRule1);
        portSecurityList.add(portSecurityRule2);
        portSecurityList.add(portSecurityRule3);
        portSecurityList.add(portSecurityRule4);

        NeutronSecurityGroup localSecurityGroup = mock(NeutronSecurityGroup.class);
        when(localSecurityGroup.getSecurityRules()).thenReturn(portSecurityList);

        egressAclServiceSpy.programPortSecurityGroup(DP_ID_LONG, SEGMENT_ID, MAC_ADDRESS, LOCAL_PORT,
                localSecurityGroup, PORT_UUID, true);
    }

    /**
     *  Test IPv4 add test case.
     */
    @Test
    public void testProgramPortSecurityACLRuleAddIpv4() throws Exception {
        when(portSecurityRule.getSecurityRuleProtocol()).thenReturn(null);
        when(portSecurityRule.getSecurityRulePortMax()).thenReturn(null);
        when(portSecurityRule.getSecurityRulePortMin()).thenReturn(null);
        when(portSecurityRule.getSecurityRuleRemoteIpPrefix()).thenReturn(null);

        egressAclServiceSpy.programPortSecurityGroup(Long.valueOf(1554), "2", MAC_ADDRESS, 124, securityGroup,PORT_UUID,true);

        verify(writeTransaction, times(1)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), eq(true));
        verify(writeTransaction, times(1)).submit();
        verify(commitFuture, times(1)).checkedGet();
    }

    /**
     *  Test IPv6 add test case.
     */
    @Test
    public void testProgramPortSecurityACLRuleAddIpv6() throws Exception {
        when(portSecurityIpv6Rule.getSecurityRuleProtocol()).thenReturn(null);
        when(portSecurityIpv6Rule.getSecurityRulePortMax()).thenReturn(null);
        when(portSecurityIpv6Rule.getSecurityRulePortMin()).thenReturn(null);
        when(portSecurityIpv6Rule.getSecurityRuleRemoteIpPrefix()).thenReturn(null);

        egressAclServiceSpy.programPortSecurityGroup(Long.valueOf(1554), "2", MAC_ADDRESS, 124, securityGroupIpv6, PORT_UUID, true);

        verify(writeTransaction, times(1)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), eq(true));
        verify(writeTransaction, times(1)).submit();
        verify(commitFuture, times(1)).checkedGet();
    }

    /**
     *  Test IPv4 remove test case.
     */
    @Test
    public void testProgramPortSecurityACLRuleRemoveIpv4() throws Exception {
        when(portSecurityRule.getSecurityRuleProtocol()).thenReturn(null);
        when(portSecurityRule.getSecurityRulePortMax()).thenReturn(null);
        when(portSecurityRule.getSecurityRulePortMin()).thenReturn(null);
        when(portSecurityRule.getSecurityRuleRemoteIpPrefix()).thenReturn(null);

        egressAclServiceSpy.programPortSecurityGroup(Long.valueOf(1554), "2", MAC_ADDRESS, 124, securityGroup,PORT_UUID,false);
        verify(writeTransaction, times(1)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(writeTransaction, times(1)).submit();
        verify(commitFuture, times(1)).get();
    }

    /**
     *  Test IPv6 remove test case.
     */
    @Test
    public void testProgramPortSecurityACLRuleRemoveIpv6() throws Exception {
        when(portSecurityIpv6Rule.getSecurityRuleProtocol()).thenReturn(null);
        when(portSecurityIpv6Rule.getSecurityRulePortMax()).thenReturn(null);
        when(portSecurityIpv6Rule.getSecurityRulePortMin()).thenReturn(null);
        when(portSecurityIpv6Rule.getSecurityRuleRemoteIpPrefix()).thenReturn(null);

        egressAclServiceSpy.programPortSecurityGroup(Long.valueOf(1554), "2", MAC_ADDRESS, 124, securityGroupIpv6, PORT_UUID, false);
        verify(writeTransaction, times(1)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(writeTransaction, times(1)).submit();
        verify(commitFuture, times(1)).get();
    }

    /**
     *  Test IPv4 TCP add with port no and CIDR selected.
     */
    @Test
    public void testProgramPortSecurityACLRuleAddTcp1() throws Exception {
        when(portSecurityRule.getSecurityRuleProtocol()).thenReturn("tcp");
        when(portSecurityRule.getSecurityRulePortMax()).thenReturn(20);
        when(portSecurityRule.getSecurityRulePortMin()).thenReturn(20);
        when(portSecurityRule.getSecurityRuleRemoteIpPrefix()).thenReturn("0.0.0.0/24");
        PowerMockito.doAnswer(answer()).when(egressAclServiceSpy, "writeFlow", any(FlowBuilder.class),
                                             any(NodeBuilder.class));
        egressAclServiceSpy.programPortSecurityGroup(DP_ID_LONG, SEGMENT_ID, MAC_ADDRESS, LOCAL_PORT, securityGroup,
                                                     PORT_UUID, true);

        Match match = flowBuilder.getMatch();
        EthernetMatch ethMatch = match.getEthernetMatch();
        Assert.assertEquals(MAC_ADDRESS, ethMatch.getEthernetSource().getAddress().getValue());
        Assert.assertEquals((long) IPV4_ETHER_TYPE, (long) ethMatch.getEthernetType().getType().getValue());

        Assert.assertTrue(match.getLayer4Match() instanceof TcpMatch);
        TcpMatch layer4Match=(TcpMatch) match.getLayer4Match();
        Assert.assertEquals(20, layer4Match.getTcpDestinationPort().getValue().intValue());
        int port=portSecurityRule.getSecurityRulePortMin();
        Assert.assertEquals("Egress_TCP_" + SEGMENT_ID + "_" + MAC_ADDRESS +
                            "_" + port + "_0.0.0.0/24_Permit", flowBuilder.getFlowName());
    }

    /**
     *  Test IPv6 TCP add with port no and CIDR selected.
     */
    @Test
    public void testProgramPortSecurityACLRuleIpv6AddTcp1() throws Exception {
        when(portSecurityIpv6Rule.getSecurityRuleProtocol()).thenReturn("tcp");
        when(portSecurityIpv6Rule.getSecurityRulePortMax()).thenReturn(20);
        when(portSecurityIpv6Rule.getSecurityRulePortMin()).thenReturn(20);
        when(portSecurityIpv6Rule.getSecurityRuleRemoteIpPrefix()).thenReturn("::/64");
        PowerMockito.doAnswer(answer()).when(egressAclServiceSpy, "writeFlow", any(FlowBuilder.class),
                any(NodeBuilder.class));
        egressAclServiceSpy.programPortSecurityGroup(DP_ID_LONG, SEGMENT_ID, MAC_ADDRESS, LOCAL_PORT, securityGroupIpv6,
                PORT_UUID, true);

        Match match = flowBuilder.getMatch();
        EthernetMatch ethMatch = match.getEthernetMatch();
        Assert.assertEquals(MAC_ADDRESS, ethMatch.getEthernetSource().getAddress().getValue());
        Assert.assertEquals((long) IPV6_ETHER_TYPE, (long) ethMatch.getEthernetType().getType().getValue());

        Assert.assertTrue(match.getLayer4Match() instanceof TcpMatch);
        TcpMatch layer4Match=(TcpMatch) match.getLayer4Match();
        Assert.assertEquals(20, layer4Match.getTcpDestinationPort().getValue().intValue());
        int port=portSecurityIpv6Rule.getSecurityRulePortMin();
        Assert.assertEquals("Egress_TCP_" + SEGMENT_ID + "_" + MAC_ADDRESS +
                "_" + port + "_::/64_Permit", flowBuilder.getFlowName());
    }

    /**
     *  Test IPv4 TCP remove with port no and CIDR selected.
     */
    @Test
    public void testProgramPortSecurityACLRuleRemoveTcp1() throws Exception {
        when(portSecurityRule.getSecurityRuleProtocol()).thenReturn("tcp");
        when(portSecurityRule.getSecurityRulePortMax()).thenReturn(30);
        when(portSecurityRule.getSecurityRulePortMin()).thenReturn(30);
        when(portSecurityRule.getSecurityRuleRemoteIpPrefix()).thenReturn("0.0.0.0/24");
        PowerMockito.doAnswer(answer()).when(egressAclServiceSpy, "removeFlow", any(FlowBuilder.class),
                                             any(NodeBuilder.class));

        egressAclServiceSpy.programPortSecurityGroup(DP_ID_LONG, SEGMENT_ID, MAC_ADDRESS, LOCAL_PORT, securityGroup,
                                                     PORT_UUID, false);

        Match match = flowBuilder.getMatch();
        EthernetMatch ethMatch = match.getEthernetMatch();
        Assert.assertEquals(MAC_ADDRESS, ethMatch.getEthernetSource().getAddress().getValue());
        Assert.assertEquals((long) IPV4_ETHER_TYPE, (long) ethMatch.getEthernetType().getType().getValue());

        Assert.assertTrue(match.getLayer4Match() instanceof TcpMatch);
        TcpMatch layer4Match = (TcpMatch) match.getLayer4Match();
        Assert.assertEquals(30, layer4Match.getTcpDestinationPort().getValue().intValue());
        int port=portSecurityRule.getSecurityRulePortMin();
        Assert.assertEquals("Egress_TCP_" + SEGMENT_ID + "_" + MAC_ADDRESS +
                            "_" + port + "_0.0.0.0/24_Permit", flowBuilder.getFlowName());
    }

    /**
     *  Test IPv6 TCP remove with port no and CIDR selected.
     */
    @Test
    public void testProgramPortSecurityACLRuleIpv6RemoveTcp1() throws Exception {
        when(portSecurityIpv6Rule.getSecurityRuleProtocol()).thenReturn("tcp");
        when(portSecurityIpv6Rule.getSecurityRulePortMax()).thenReturn(30);
        when(portSecurityIpv6Rule.getSecurityRulePortMin()).thenReturn(30);
        when(portSecurityIpv6Rule.getSecurityRuleRemoteIpPrefix()).thenReturn("::/64");
        PowerMockito.doAnswer(answer()).when(egressAclServiceSpy, "removeFlow", any(FlowBuilder.class),
                any(NodeBuilder.class));

        egressAclServiceSpy.programPortSecurityGroup(DP_ID_LONG, SEGMENT_ID, MAC_ADDRESS, LOCAL_PORT, securityGroupIpv6,
                PORT_UUID, false);

        Match match = flowBuilder.getMatch();
        EthernetMatch ethMatch = match.getEthernetMatch();
        Assert.assertEquals(MAC_ADDRESS, ethMatch.getEthernetSource().getAddress().getValue());
        Assert.assertEquals((long) IPV6_ETHER_TYPE, (long) ethMatch.getEthernetType().getType().getValue());

        Assert.assertTrue(match.getLayer4Match() instanceof TcpMatch);
        TcpMatch layer4Match = (TcpMatch) match.getLayer4Match();
        Assert.assertEquals(30, layer4Match.getTcpDestinationPort().getValue().intValue());
        int port=portSecurityIpv6Rule.getSecurityRulePortMin();
        Assert.assertEquals("Egress_TCP_" + SEGMENT_ID + "_" + MAC_ADDRESS +
                "_" + port + "_::/64_Permit", flowBuilder.getFlowName());
    }

    /**
     *  Test IPv4 TCP add with port no and remote SG selected.
     */
    @Test
    public void testProgramPortSecurityACLRuleAddTcp2() throws Exception {
        when(portSecurityRule.getSecurityRuleProtocol()).thenReturn("tcp");
        when(portSecurityRule.getSecurityRulePortMax()).thenReturn(40);
        when(portSecurityRule.getSecurityRulePortMin()).thenReturn(40);
        when(portSecurityRule.getSecurityRemoteGroupID()).thenReturn("85cc3048-abc3-43cc-89b3-377341426ac5");
        PowerMockito.doAnswer(answer()).when(egressAclServiceSpy, "writeFlow", any(FlowBuilder.class),
                                             any(NodeBuilder.class));
        egressAclServiceSpy.programPortSecurityGroup(DP_ID_LONG, SEGMENT_ID, MAC_ADDRESS, LOCAL_PORT, securityGroup,
                                                     PORT_UUID, true);

        Match match = flowBuilder.getMatch();
        EthernetMatch ethMatch = match.getEthernetMatch();
        Assert.assertEquals(MAC_ADDRESS, ethMatch.getEthernetSource().getAddress().getValue());
        Assert.assertEquals((long) IPV4_ETHER_TYPE, (long) ethMatch.getEthernetType().getType().getValue());

        Assert.assertTrue(match.getLayer4Match() instanceof TcpMatch);
        TcpMatch layer4Match=(TcpMatch) match.getLayer4Match();
        int port=portSecurityRule.getSecurityRulePortMin();
        String expectedFlowId1 = "Egress_TCP_" + SEGMENT_ID + "_" + MAC_ADDRESS + "_" + port + "_" + DEST_IP_1 +
                "_Permit";
        String expectedFlowId2 = "Egress_TCP_" + SEGMENT_ID + "_" + MAC_ADDRESS + "_" + port + "_" + DEST_IP_2 +
                "_Permit";
        String actualFlowId = flowBuilder.getFlowName();
        if(actualFlowId.equals(expectedFlowId1) || actualFlowId.equals(expectedFlowId2)) {
            Assert.assertTrue(true);
        } else {
            Assert.assertTrue(false);
        }
    }

    /**
     *  Test IPv6 TCP add with port no and remote SG selected.
     */
    @Test
    public void testProgramPortSecurityACLRuleIpv6AddTcp2() throws Exception {
        when(portSecurityIpv6Rule.getSecurityRuleProtocol()).thenReturn("tcp");
        when(portSecurityIpv6Rule.getSecurityRulePortMax()).thenReturn(40);
        when(portSecurityIpv6Rule.getSecurityRulePortMin()).thenReturn(40);
        when(portSecurityIpv6Rule.getSecurityRemoteGroupID()).thenReturn("85cc3048-abc3-43cc-89b3-377341426ac5");
        PowerMockito.doAnswer(answer()).when(egressAclServiceSpy, "writeFlow", any(FlowBuilder.class),
                any(NodeBuilder.class));
        egressAclServiceSpy.programPortSecurityGroup(DP_ID_LONG, SEGMENT_ID, MAC_ADDRESS, LOCAL_PORT, securityGroupIpv6,
                PORT_UUID, true);

        Match match = flowBuilder.getMatch();
        EthernetMatch ethMatch = match.getEthernetMatch();
        Assert.assertEquals(MAC_ADDRESS, ethMatch.getEthernetSource().getAddress().getValue());
        Assert.assertEquals((long) IPV6_ETHER_TYPE, (long) ethMatch.getEthernetType().getType().getValue());

        Assert.assertTrue(match.getLayer4Match() instanceof TcpMatch);
        TcpMatch layer4Match=(TcpMatch) match.getLayer4Match();
        int port=portSecurityIpv6Rule.getSecurityRulePortMin();
        String expectedFlowId1 = "Egress_TCP_" + SEGMENT_ID + "_" + MAC_ADDRESS + "_" + port + "_" + IPV6_DEST_IP_1 +
                "_Permit";
        String expectedFlowId2 = "Egress_TCP_" + SEGMENT_ID + "_" + MAC_ADDRESS + "_" + port + "_" + IPV6_DEST_IP_2 +
                "_Permit";
        String actualFlowId = flowBuilder.getFlowName();
        if(actualFlowId.equals(expectedFlowId1) || actualFlowId.equals(expectedFlowId2)) {
            Assert.assertTrue(true);
        } else {
            Assert.assertTrue(false);
        }
    }

    /**
     *  Test IPv4 TCP remove with port no and remote SG selected.
     */
    @Test
    public void testProgramPortSecurityACLRuleRemoveTcp2() throws Exception {
        when(portSecurityRule.getSecurityRuleProtocol()).thenReturn("tcp");
        when(portSecurityRule.getSecurityRulePortMax()).thenReturn(50);
        when(portSecurityRule.getSecurityRulePortMin()).thenReturn(50);
        when(portSecurityRule.getSecurityRemoteGroupID()).thenReturn("85cc3048-abc3-43cc-89b3-377341426ac5");
        PowerMockito.doAnswer(answer()).when(egressAclServiceSpy, "removeFlow", any(FlowBuilder.class),
                                             any(NodeBuilder.class));

        egressAclServiceSpy.programPortSecurityGroup(DP_ID_LONG, SEGMENT_ID, MAC_ADDRESS, LOCAL_PORT, securityGroup,
                                                     PORT_UUID, false);

        Match match = flowBuilder.getMatch();
        EthernetMatch ethMatch = match.getEthernetMatch();
        Assert.assertEquals(MAC_ADDRESS, ethMatch.getEthernetSource().getAddress().getValue());
        Assert.assertEquals((long) IPV4_ETHER_TYPE, (long) ethMatch.getEthernetType().getType().getValue());

        Assert.assertTrue(match.getLayer4Match() instanceof TcpMatch);
        TcpMatch layer4Match=(TcpMatch) match.getLayer4Match();
        int port=portSecurityRule.getSecurityRulePortMin();
        String expectedFlowId1 = "Egress_TCP_" + SEGMENT_ID + "_" + MAC_ADDRESS +"_" + port + "_" + DEST_IP_1 +
                "_Permit";
        String expectedFlowId2 = "Egress_TCP_" + SEGMENT_ID + "_" + MAC_ADDRESS +"_" + port + "_" + DEST_IP_2 +
                "_Permit";
        String actualFlowId = flowBuilder.getFlowName();
        if(actualFlowId.equals(expectedFlowId1) || actualFlowId.equals(expectedFlowId2)) {
            Assert.assertTrue(true);
        } else {
            Assert.assertTrue(false);
        }
    }

    /**
     *  Test IPv6 TCP remove with port no and remote SG selected.
     */
    @Test
    public void testProgramPortSecurityACLRuleIpv6RemoveTcp2() throws Exception {
        when(portSecurityIpv6Rule.getSecurityRuleProtocol()).thenReturn("tcp");
        when(portSecurityIpv6Rule.getSecurityRulePortMax()).thenReturn(50);
        when(portSecurityIpv6Rule.getSecurityRulePortMin()).thenReturn(50);
        when(portSecurityIpv6Rule.getSecurityRemoteGroupID()).thenReturn("85cc3048-abc3-43cc-89b3-377341426ac5");
        PowerMockito.doAnswer(answer()).when(egressAclServiceSpy, "removeFlow", any(FlowBuilder.class),
                any(NodeBuilder.class));

        egressAclServiceSpy.programPortSecurityGroup(DP_ID_LONG, SEGMENT_ID, MAC_ADDRESS, LOCAL_PORT, securityGroupIpv6,
                PORT_UUID, false);

        Match match = flowBuilder.getMatch();
        EthernetMatch ethMatch = match.getEthernetMatch();
        Assert.assertEquals(MAC_ADDRESS, ethMatch.getEthernetSource().getAddress().getValue());
        Assert.assertEquals((long) IPV6_ETHER_TYPE, (long) ethMatch.getEthernetType().getType().getValue());

        Assert.assertTrue(match.getLayer4Match() instanceof TcpMatch);
        TcpMatch layer4Match=(TcpMatch) match.getLayer4Match();
        int port=portSecurityIpv6Rule.getSecurityRulePortMin();
        String expectedFlowId1 = "Egress_TCP_" + SEGMENT_ID + "_" + MAC_ADDRESS +"_" + port + "_" + IPV6_DEST_IP_1 +
                "_Permit";
        String expectedFlowId2 = "Egress_TCP_" + SEGMENT_ID + "_" + MAC_ADDRESS +"_" + port + "_" + IPV6_DEST_IP_2 +
                "_Permit";
        String actualFlowId = flowBuilder.getFlowName();
        if(actualFlowId.equals(expectedFlowId1) || actualFlowId.equals(expectedFlowId2)) {
            Assert.assertTrue(true);
        } else {
            Assert.assertTrue(false);
        }
    }

    /**
     *  Test IPv4 TCP add with port range (All TCP) and CIDR selected.
     */
    @Test
    public void testProgramPortSecurityACLRuleAddTcpAll1() throws Exception {
        when(portSecurityRule.getSecurityRuleProtocol()).thenReturn("tcp");
        when(portSecurityRule.getSecurityRulePortMax()).thenReturn(PORT_RANGE_MAX);
        when(portSecurityRule.getSecurityRulePortMin()).thenReturn(PORT_RANGE_MIN);
        when(portSecurityRule.getSecurityRuleRemoteIpPrefix()).thenReturn("0.0.0.0/24");
        PowerMockito.doAnswer(answer()).when(egressAclServiceSpy, "writeFlow", any(FlowBuilder.class),
                                             any(NodeBuilder.class));
        egressAclServiceSpy.programPortSecurityGroup(DP_ID_LONG, SEGMENT_ID, MAC_ADDRESS, LOCAL_PORT, securityGroup,
                                                     PORT_UUID, true);

        Match match = flowBuilder.getMatch();
        Assert.assertTrue(match.getLayer4Match() instanceof TcpMatch);
        TcpMatch layer4Match=(TcpMatch) match.getLayer4Match();
        EthernetMatch ethMatch = match.getEthernetMatch();
        Assert.assertEquals(MAC_ADDRESS, ethMatch.getEthernetSource().getAddress().getValue());
        Assert.assertEquals((long) IPV4_ETHER_TYPE, (long) ethMatch.getEthernetType().getType().getValue());
    }

    /**
     *  Test IPv6 TCP add with port range (All TCP) and CIDR selected.
     */
    @Test
    public void testProgramPortSecurityACLRuleIpv6AddTcpAll1() throws Exception {
        when(portSecurityIpv6Rule.getSecurityRuleProtocol()).thenReturn("tcp");
        when(portSecurityIpv6Rule.getSecurityRulePortMax()).thenReturn(PORT_RANGE_MAX);
        when(portSecurityIpv6Rule.getSecurityRulePortMin()).thenReturn(PORT_RANGE_MIN);
        when(portSecurityIpv6Rule.getSecurityRuleRemoteIpPrefix()).thenReturn("::/64");
        PowerMockito.doAnswer(answer()).when(egressAclServiceSpy, "writeFlow", any(FlowBuilder.class),
                any(NodeBuilder.class));
        egressAclServiceSpy.programPortSecurityGroup(DP_ID_LONG, SEGMENT_ID, MAC_ADDRESS, LOCAL_PORT, securityGroupIpv6,
                PORT_UUID, true);

        Match match = flowBuilder.getMatch();
        Assert.assertTrue(match.getLayer4Match() instanceof TcpMatch);
        TcpMatch layer4Match=(TcpMatch) match.getLayer4Match();
        EthernetMatch ethMatch = match.getEthernetMatch();
        Assert.assertEquals(MAC_ADDRESS, ethMatch.getEthernetSource().getAddress().getValue());
        Assert.assertEquals((long) IPV6_ETHER_TYPE, (long) ethMatch.getEthernetType().getType().getValue());
    }

    /**
     *  Test IPv4 TCP remove with port range (All TCP) and CIDR selected.
     */
    @Test
    public void testProgramPortSecurityACLRuleRemoveTcpAll1() throws Exception {
        when(portSecurityRule.getSecurityRuleProtocol()).thenReturn("tcp");
        when(portSecurityRule.getSecurityRulePortMax()).thenReturn(PORT_RANGE_MAX);
        when(portSecurityRule.getSecurityRulePortMin()).thenReturn(PORT_RANGE_MIN);
        when(portSecurityRule.getSecurityRuleRemoteIpPrefix()).thenReturn("0.0.0.0/24");
        PowerMockito.doAnswer(answer()).when(egressAclServiceSpy, "removeFlow", any(FlowBuilder.class),
                                             any(NodeBuilder.class));

        egressAclServiceSpy.programPortSecurityGroup(DP_ID_LONG, SEGMENT_ID, MAC_ADDRESS, LOCAL_PORT, securityGroup,
                                                     PORT_UUID, false);

        Match match = flowBuilder.getMatch();
        EthernetMatch ethMatch = match.getEthernetMatch();
        Assert.assertEquals(MAC_ADDRESS, ethMatch.getEthernetSource().getAddress().getValue());
        Assert.assertEquals((long) IPV4_ETHER_TYPE, (long) ethMatch.getEthernetType().getType().getValue());
        Assert.assertTrue(match.getLayer4Match() instanceof TcpMatch);
        Assert.assertEquals("Egress_TCP_" + SEGMENT_ID + "_" + MAC_ADDRESS + "_" + PORT_RANGE_MIN + "_" +
                            PORT_RANGE_MAX + "_0.0.0.0/24_Permit", flowBuilder.getFlowName());
    }

    /**
     *  Test IPv6 TCP remove with port range (All TCP) and CIDR selected.
     */
    @Test
    public void testProgramPortSecurityACLRuleIpv6RemoveTcpAll1() throws Exception {
        when(portSecurityIpv6Rule.getSecurityRuleProtocol()).thenReturn("tcp");
        when(portSecurityIpv6Rule.getSecurityRulePortMax()).thenReturn(PORT_RANGE_MAX);
        when(portSecurityIpv6Rule.getSecurityRulePortMin()).thenReturn(PORT_RANGE_MIN);
        when(portSecurityIpv6Rule.getSecurityRuleRemoteIpPrefix()).thenReturn("::/64");
        PowerMockito.doAnswer(answer()).when(egressAclServiceSpy, "removeFlow", any(FlowBuilder.class),
                any(NodeBuilder.class));

        egressAclServiceSpy.programPortSecurityGroup(DP_ID_LONG, SEGMENT_ID, MAC_ADDRESS, LOCAL_PORT, securityGroupIpv6,
                PORT_UUID, false);

        Match match = flowBuilder.getMatch();
        EthernetMatch ethMatch = match.getEthernetMatch();
        Assert.assertEquals(MAC_ADDRESS, ethMatch.getEthernetSource().getAddress().getValue());
        Assert.assertEquals((long) IPV6_ETHER_TYPE, (long) ethMatch.getEthernetType().getType().getValue());
        Assert.assertTrue(match.getLayer4Match() instanceof TcpMatch);
        Assert.assertEquals("Egress_TCP_" + SEGMENT_ID + "_" + MAC_ADDRESS + "_" + PORT_RANGE_MIN + "_" +
                PORT_RANGE_MAX + "_::/64_Permit", flowBuilder.getFlowName());
    }

    /**
     *  Test IPv4 TCP add with port range (All TCP) and remote SG selected.
     */
    @Test
    public void testProgramPortSecurityACLRuleAddTcpAll2() throws Exception {
        when(portSecurityRule.getSecurityRuleProtocol()).thenReturn("tcp");
        when(portSecurityRule.getSecurityRulePortMax()).thenReturn(PORT_RANGE_MAX);
        when(portSecurityRule.getSecurityRulePortMin()).thenReturn(PORT_RANGE_MIN);
        when(portSecurityRule.getSecurityRemoteGroupID()).thenReturn("85cc3048-abc3-43cc-89b3-377341426ac5");
        PowerMockito.doAnswer(answer()).when(egressAclServiceSpy, "writeFlow", any(FlowBuilder.class),
                                             any(NodeBuilder.class));
        egressAclServiceSpy.programPortSecurityGroup(DP_ID_LONG, SEGMENT_ID, MAC_ADDRESS, LOCAL_PORT, securityGroup,
                                                     PORT_UUID, true);

        Match match = flowBuilder.getMatch();
        EthernetMatch ethMatch = match.getEthernetMatch();
        Assert.assertEquals(MAC_ADDRESS, ethMatch.getEthernetSource().getAddress().getValue());
        Assert.assertEquals((long) IPV4_ETHER_TYPE, (long) ethMatch.getEthernetType().getType().getValue());

        Assert.assertTrue(match.getLayer4Match() instanceof TcpMatch);
        String expectedFlowId1 = "Egress_TCP_" + SEGMENT_ID + "_" + MAC_ADDRESS + "_" + PORT_RANGE_MIN + "_" +
                                PORT_RANGE_MAX + "_" + DEST_IP_1 + "_Permit";
        String expectedFlowId2 = "Egress_TCP_" + SEGMENT_ID + "_" + MAC_ADDRESS + "_" + PORT_RANGE_MIN + "_" +
                                PORT_RANGE_MAX + "_" + DEST_IP_2 + "_Permit";
        String actualFlowId = flowBuilder.getFlowName();
        if(actualFlowId.equals(expectedFlowId1) || actualFlowId.equals(expectedFlowId2)) {
            Assert.assertTrue(true);
        } else {
            Assert.assertTrue(false);
        }
    }

    /**
     *  Test IPv6 TCP add with port range (All TCP) and remote SG selected.
     */
    @Test
    public void testProgramPortSecurityACLRuleIpv6AddTcpAll2() throws Exception {
        when(portSecurityIpv6Rule.getSecurityRuleProtocol()).thenReturn("tcp");
        when(portSecurityIpv6Rule.getSecurityRulePortMax()).thenReturn(PORT_RANGE_MAX);
        when(portSecurityIpv6Rule.getSecurityRulePortMin()).thenReturn(PORT_RANGE_MIN);
        when(portSecurityIpv6Rule.getSecurityRemoteGroupID()).thenReturn("85cc3048-abc3-43cc-89b3-377341426ac5");
        PowerMockito.doAnswer(answer()).when(egressAclServiceSpy, "writeFlow", any(FlowBuilder.class),
                any(NodeBuilder.class));
        egressAclServiceSpy.programPortSecurityGroup(DP_ID_LONG, SEGMENT_ID, MAC_ADDRESS, LOCAL_PORT, securityGroupIpv6,
                PORT_UUID, true);

        Match match = flowBuilder.getMatch();
        EthernetMatch ethMatch = match.getEthernetMatch();
        Assert.assertEquals(MAC_ADDRESS, ethMatch.getEthernetSource().getAddress().getValue());
        Assert.assertEquals((long) IPV6_ETHER_TYPE, (long) ethMatch.getEthernetType().getType().getValue());

        Assert.assertTrue(match.getLayer4Match() instanceof TcpMatch);
        String expectedFlowId1 = "Egress_TCP_" + SEGMENT_ID + "_" + MAC_ADDRESS + "_" + PORT_RANGE_MIN + "_" +
                PORT_RANGE_MAX + "_" + IPV6_DEST_IP_1 + "_Permit";
        String expectedFlowId2 = "Egress_TCP_" + SEGMENT_ID + "_" + MAC_ADDRESS + "_" + PORT_RANGE_MIN + "_" +
                PORT_RANGE_MAX + "_" + IPV6_DEST_IP_2 + "_Permit";
        String actualFlowId = flowBuilder.getFlowName();
        if(actualFlowId.equals(expectedFlowId1) || actualFlowId.equals(expectedFlowId2)) {
            Assert.assertTrue(true);
        } else {
            Assert.assertTrue(false);
        }
    }

    /**
     *  Test IPv4 TCP remove with port range (All TCP) and remote SG selected.
     */
    @Test
    public void testProgramPortSecurityACLRuleRemoveTcpAll2() throws Exception {
        when(portSecurityRule.getSecurityRuleProtocol()).thenReturn("tcp");
        when(portSecurityRule.getSecurityRulePortMax()).thenReturn(PORT_RANGE_MAX);
        when(portSecurityRule.getSecurityRulePortMin()).thenReturn(PORT_RANGE_MIN);
        when(portSecurityRule.getSecurityRemoteGroupID()).thenReturn("85cc3048-abc3-43cc-89b3-377341426ac5");
        PowerMockito.doAnswer(answer()).when(egressAclServiceSpy, "removeFlow", any(FlowBuilder.class),
                                             any(NodeBuilder.class));

        egressAclServiceSpy.programPortSecurityGroup(DP_ID_LONG, SEGMENT_ID, MAC_ADDRESS, LOCAL_PORT, securityGroup,
                                                     PORT_UUID, false);

        Match match = flowBuilder.getMatch();
        EthernetMatch ethMatch = match.getEthernetMatch();
        Assert.assertEquals(MAC_ADDRESS, ethMatch.getEthernetSource().getAddress().getValue());
        Assert.assertEquals((long) IPV4_ETHER_TYPE, (long) ethMatch.getEthernetType().getType().getValue());

        Assert.assertTrue(match.getLayer4Match() instanceof TcpMatch);
        String expectedFlowId1 = "Egress_TCP_" + SEGMENT_ID + "_" + MAC_ADDRESS + "_" + PORT_RANGE_MIN + "_" +
                                PORT_RANGE_MAX + "_" + DEST_IP_1 + "_Permit";
        String expectedFlowId2 = "Egress_TCP_" + SEGMENT_ID + "_" + MAC_ADDRESS + "_" + PORT_RANGE_MIN + "_" +
                                PORT_RANGE_MAX + "_" + DEST_IP_2 + "_Permit";
        String actualFlowId = flowBuilder.getFlowName();
        if(actualFlowId.equals(expectedFlowId1) || actualFlowId.equals(expectedFlowId2)) {
            Assert.assertTrue(true);
        } else {
            Assert.assertTrue(false);
        }
    }

    /**
     *  Test IPv6 TCP remove with port range (All TCP) and remote SG selected.
     */
    @Test
    public void testProgramPortSecurityACLRuleIpv6RemoveTcpAll2() throws Exception {
        when(portSecurityIpv6Rule.getSecurityRuleProtocol()).thenReturn("tcp");
        when(portSecurityIpv6Rule.getSecurityRulePortMax()).thenReturn(PORT_RANGE_MAX);
        when(portSecurityIpv6Rule.getSecurityRulePortMin()).thenReturn(PORT_RANGE_MIN);
        when(portSecurityIpv6Rule.getSecurityRemoteGroupID()).thenReturn("85cc3048-abc3-43cc-89b3-377341426ac5");
        PowerMockito.doAnswer(answer()).when(egressAclServiceSpy, "removeFlow", any(FlowBuilder.class),
                any(NodeBuilder.class));

        egressAclServiceSpy.programPortSecurityGroup(DP_ID_LONG, SEGMENT_ID, MAC_ADDRESS, LOCAL_PORT, securityGroupIpv6,
                PORT_UUID, false);

        Match match = flowBuilder.getMatch();
        EthernetMatch ethMatch = match.getEthernetMatch();
        Assert.assertEquals(MAC_ADDRESS, ethMatch.getEthernetSource().getAddress().getValue());
        Assert.assertEquals((long) IPV6_ETHER_TYPE, (long) ethMatch.getEthernetType().getType().getValue());

        Assert.assertTrue(match.getLayer4Match() instanceof TcpMatch);
        String expectedFlowId1 = "Egress_TCP_" + SEGMENT_ID + "_" + MAC_ADDRESS + "_" + PORT_RANGE_MIN + "_" +
                PORT_RANGE_MAX + "_" + IPV6_DEST_IP_1 + "_Permit";
        String expectedFlowId2 = "Egress_TCP_" + SEGMENT_ID + "_" + MAC_ADDRESS + "_" + PORT_RANGE_MIN + "_" +
                PORT_RANGE_MAX + "_" + IPV6_DEST_IP_2 + "_Permit";
        String actualFlowId = flowBuilder.getFlowName();
        if(actualFlowId.equals(expectedFlowId1) || actualFlowId.equals(expectedFlowId2)) {
            Assert.assertTrue(true);
        } else {
            Assert.assertTrue(false);
        }
    }

    /**
     *  Test IPv4 UDP add with port no and CIDR selected.
     */
    @Test
    public void testProgramPortSecurityACLRuleAddUdp1() throws Exception {
        when(portSecurityRule.getSecurityRuleProtocol()).thenReturn("udp");
        when(portSecurityRule.getSecurityRulePortMax()).thenReturn(50);
        when(portSecurityRule.getSecurityRulePortMin()).thenReturn(50);
        when(portSecurityRule.getSecurityRuleRemoteIpPrefix()).thenReturn("0.0.0.0/24");
        PowerMockito.doAnswer(answer()).when(egressAclServiceSpy, "writeFlow", any(FlowBuilder.class),
                                             any(NodeBuilder.class));
        egressAclServiceSpy.programPortSecurityGroup(DP_ID_LONG, SEGMENT_ID, MAC_ADDRESS, LOCAL_PORT, securityGroup,
                                                     PORT_UUID, true);

        Match match = flowBuilder.getMatch();
        EthernetMatch ethMatch = match.getEthernetMatch();
        Assert.assertEquals(MAC_ADDRESS, ethMatch.getEthernetSource().getAddress().getValue());
        Assert.assertEquals((long) IPV4_ETHER_TYPE, (long) ethMatch.getEthernetType().getType().getValue());

        Assert.assertTrue(match.getLayer4Match() instanceof UdpMatch);
        UdpMatch layer4Match = (UdpMatch) match.getLayer4Match();
        Assert.assertEquals(50, layer4Match.getUdpDestinationPort().getValue().intValue());
        int port = portSecurityRule.getSecurityRulePortMin();
        Assert.assertEquals("Egress_UDP_" + SEGMENT_ID + "_" + MAC_ADDRESS +
                            "_" + port + "_0.0.0.0/24_Permit", flowBuilder.getFlowName());
    }

    /**
     *  Test IPv6 UDP add with port no and CIDR selected.
     */
    @Test
    public void testProgramPortSecurityACLRuleIpv6AddUdp1() throws Exception {
        when(portSecurityIpv6Rule.getSecurityRuleProtocol()).thenReturn("udp");
        when(portSecurityIpv6Rule.getSecurityRulePortMax()).thenReturn(50);
        when(portSecurityIpv6Rule.getSecurityRulePortMin()).thenReturn(50);
        when(portSecurityIpv6Rule.getSecurityRuleRemoteIpPrefix()).thenReturn("::/64");
        PowerMockito.doAnswer(answer()).when(egressAclServiceSpy, "writeFlow", any(FlowBuilder.class),
                any(NodeBuilder.class));
        egressAclServiceSpy.programPortSecurityGroup(DP_ID_LONG, SEGMENT_ID, MAC_ADDRESS, LOCAL_PORT, securityGroupIpv6,
                PORT_UUID, true);

        Match match = flowBuilder.getMatch();
        EthernetMatch ethMatch = match.getEthernetMatch();
        Assert.assertEquals(MAC_ADDRESS, ethMatch.getEthernetSource().getAddress().getValue());
        Assert.assertEquals((long) IPV6_ETHER_TYPE, (long) ethMatch.getEthernetType().getType().getValue());

        Assert.assertTrue(match.getLayer4Match() instanceof UdpMatch);
        UdpMatch layer4Match = (UdpMatch) match.getLayer4Match();
        Assert.assertEquals(50, layer4Match.getUdpDestinationPort().getValue().intValue());
        int port = portSecurityIpv6Rule.getSecurityRulePortMin();
        Assert.assertEquals("Egress_UDP_" + SEGMENT_ID + "_" + MAC_ADDRESS +
                "_" + port + "_::/64_Permit", flowBuilder.getFlowName());
    }

    /**
     *  Test IPv4 UDP remove with port no and CIDR selected.
     */
    @Test
    public void testProgramPortSecurityACLRuleRemoveUdp1() throws Exception {
        when(portSecurityRule.getSecurityRuleProtocol()).thenReturn("udp");
        when(portSecurityRule.getSecurityRulePortMax()).thenReturn(50);
        when(portSecurityRule.getSecurityRulePortMin()).thenReturn(50);
        when(portSecurityRule.getSecurityRuleRemoteIpPrefix()).thenReturn("0.0.0.0/24");
        PowerMockito.doAnswer(answer()).when(egressAclServiceSpy, "removeFlow", any(FlowBuilder.class),
                                             any(NodeBuilder.class));
        egressAclServiceSpy.programPortSecurityGroup(DP_ID_LONG, SEGMENT_ID, MAC_ADDRESS, LOCAL_PORT, securityGroup,
                                                     PORT_UUID, false);

        Match match = flowBuilder.getMatch();
        EthernetMatch ethMatch = match.getEthernetMatch();
        Assert.assertEquals(MAC_ADDRESS, ethMatch.getEthernetSource().getAddress().getValue());
        Assert.assertEquals((long) IPV4_ETHER_TYPE, (long) ethMatch.getEthernetType().getType().getValue());

        Assert.assertTrue(match.getLayer4Match() instanceof UdpMatch);
        UdpMatch layer4Match = (UdpMatch) match.getLayer4Match();
        Assert.assertEquals(50, layer4Match.getUdpDestinationPort().getValue().intValue());
        int port = portSecurityRule.getSecurityRulePortMin();
        Assert.assertEquals("Egress_UDP_" + SEGMENT_ID + "_" + MAC_ADDRESS +
                            "_" + port + "_0.0.0.0/24_Permit", flowBuilder.getFlowName());
    }

    /**
     *  Test IPv6 UDP remove with port no and CIDR selected.
     */
    @Test
    public void testProgramPortSecurityACLRuleIpv6RemoveUdp1() throws Exception {
        when(portSecurityIpv6Rule.getSecurityRuleProtocol()).thenReturn("udp");
        when(portSecurityIpv6Rule.getSecurityRulePortMax()).thenReturn(50);
        when(portSecurityIpv6Rule.getSecurityRulePortMin()).thenReturn(50);
        when(portSecurityIpv6Rule.getSecurityRuleRemoteIpPrefix()).thenReturn("::/64");
        PowerMockito.doAnswer(answer()).when(egressAclServiceSpy, "removeFlow", any(FlowBuilder.class),
                any(NodeBuilder.class));
        egressAclServiceSpy.programPortSecurityGroup(DP_ID_LONG, SEGMENT_ID, MAC_ADDRESS, LOCAL_PORT, securityGroupIpv6,
                PORT_UUID, false);

        Match match = flowBuilder.getMatch();
        EthernetMatch ethMatch = match.getEthernetMatch();
        Assert.assertEquals(MAC_ADDRESS, ethMatch.getEthernetSource().getAddress().getValue());
        Assert.assertEquals((long) IPV6_ETHER_TYPE, (long) ethMatch.getEthernetType().getType().getValue());

        Assert.assertTrue(match.getLayer4Match() instanceof UdpMatch);
        UdpMatch layer4Match = (UdpMatch) match.getLayer4Match();
        Assert.assertEquals(50, layer4Match.getUdpDestinationPort().getValue().intValue());
        int port = portSecurityIpv6Rule.getSecurityRulePortMin();
        Assert.assertEquals("Egress_UDP_" + SEGMENT_ID + "_" + MAC_ADDRESS +
                "_" + port + "_::/64_Permit", flowBuilder.getFlowName());
    }

    /**
     *  Test IPv4 UDP add with port no and remote SG selected.
     */
    @Test
    public void testProgramPortSecurityACLRuleAddUdp2() throws Exception {
        when(portSecurityRule.getSecurityRuleProtocol()).thenReturn("udp");
        when(portSecurityRule.getSecurityRulePortMax()).thenReturn(50);
        when(portSecurityRule.getSecurityRulePortMin()).thenReturn(50);
        when(portSecurityRule.getSecurityRuleRemoteIpPrefix()).thenReturn("0.0.0.0/24");
        when(portSecurityRule.getSecurityRemoteGroupID()).thenReturn("85cc3048-abc3-43cc-89b3-377341426ac5");
        PowerMockito.doAnswer(answer()).when(egressAclServiceSpy, "writeFlow", any(FlowBuilder.class),
                                             any(NodeBuilder.class));
        egressAclServiceSpy.programPortSecurityGroup(DP_ID_LONG, SEGMENT_ID, MAC_ADDRESS, LOCAL_PORT, securityGroup,
                                                     PORT_UUID, true);

        Match match = flowBuilder.getMatch();
        EthernetMatch ethMatch = match.getEthernetMatch();
        Assert.assertEquals(MAC_ADDRESS, ethMatch.getEthernetSource().getAddress().getValue());
        Assert.assertEquals((long) IPV4_ETHER_TYPE, (long) ethMatch.getEthernetType().getType().getValue());

        Assert.assertTrue(match.getLayer4Match() instanceof UdpMatch);
        UdpMatch layer4Match = (UdpMatch) match.getLayer4Match();
        Assert.assertEquals(50, layer4Match.getUdpDestinationPort().getValue().intValue());
        int port = portSecurityRule.getSecurityRulePortMin();
        String expectedFlowId1 = "Egress_UDP_" + SEGMENT_ID + "_" + MAC_ADDRESS + "_" + port + "_" + DEST_IP_1 +
                "_Permit";
        String expectedFlowId2 = "Egress_UDP_" + SEGMENT_ID + "_" + MAC_ADDRESS + "_" + port + "_" + DEST_IP_2 +
                "_Permit";
        String actualFlowId = flowBuilder.getFlowName();
        if(actualFlowId.equals(expectedFlowId1) || actualFlowId.equals(expectedFlowId2)) {
            Assert.assertTrue(true);
        } else {
            Assert.assertTrue(false);
        }
    }

    /**
     *  Test IPv6 UDP add with port no and remote SG selected.
     */
    @Test
    public void testProgramPortSecurityACLRuleIpv6AddUdp2() throws Exception {
        when(portSecurityIpv6Rule.getSecurityRuleProtocol()).thenReturn("udp");
        when(portSecurityIpv6Rule.getSecurityRulePortMax()).thenReturn(50);
        when(portSecurityIpv6Rule.getSecurityRulePortMin()).thenReturn(50);
        when(portSecurityIpv6Rule.getSecurityRuleRemoteIpPrefix()).thenReturn("::/64");
        when(portSecurityIpv6Rule.getSecurityRemoteGroupID()).thenReturn("85cc3048-abc3-43cc-89b3-377341426ac5");
        PowerMockito.doAnswer(answer()).when(egressAclServiceSpy, "writeFlow", any(FlowBuilder.class),
                any(NodeBuilder.class));
        egressAclServiceSpy.programPortSecurityGroup(DP_ID_LONG, SEGMENT_ID, MAC_ADDRESS, LOCAL_PORT, securityGroupIpv6,
                PORT_UUID, true);

        Match match = flowBuilder.getMatch();
        EthernetMatch ethMatch = match.getEthernetMatch();
        Assert.assertEquals(MAC_ADDRESS, ethMatch.getEthernetSource().getAddress().getValue());
        Assert.assertEquals((long) IPV6_ETHER_TYPE, (long) ethMatch.getEthernetType().getType().getValue());

        Assert.assertTrue(match.getLayer4Match() instanceof UdpMatch);
        UdpMatch layer4Match = (UdpMatch) match.getLayer4Match();
        Assert.assertEquals(50, layer4Match.getUdpDestinationPort().getValue().intValue());
        int port = portSecurityIpv6Rule.getSecurityRulePortMin();
        String expectedFlowId1 = "Egress_UDP_" + SEGMENT_ID + "_" + MAC_ADDRESS + "_" + port + "_" + IPV6_DEST_IP_1 +
                "_Permit";
        String expectedFlowId2 = "Egress_UDP_" + SEGMENT_ID + "_" + MAC_ADDRESS + "_" + port + "_" + IPV6_DEST_IP_2 +
                "_Permit";
        String actualFlowId = flowBuilder.getFlowName();
        if(actualFlowId.equals(expectedFlowId1) || actualFlowId.equals(expectedFlowId2)) {
            Assert.assertTrue(true);
        } else {
            Assert.assertTrue(false);
        }
    }

    /**
     *  Test IPv4 UDP remove with port no and remote SG selected.
     */
    @Test
    public void testProgramPortSecurityACLRuleRemoveUdp2() throws Exception {
        when(portSecurityRule.getSecurityRuleProtocol()).thenReturn("udp");
        when(portSecurityRule.getSecurityRulePortMax()).thenReturn(50);
        when(portSecurityRule.getSecurityRulePortMin()).thenReturn(50);
        when(portSecurityRule.getSecurityRuleRemoteIpPrefix()).thenReturn("0.0.0.0/24");
        when(portSecurityRule.getSecurityRemoteGroupID()).thenReturn("85cc3048-abc3-43cc-89b3-377341426ac5");
        PowerMockito.doAnswer(answer()).when(egressAclServiceSpy, "removeFlow", any(FlowBuilder.class),
                                             any(NodeBuilder.class));
        egressAclServiceSpy.programPortSecurityGroup(DP_ID_LONG, SEGMENT_ID, MAC_ADDRESS, LOCAL_PORT, securityGroup,
                                                     PORT_UUID, false);

        Match match = flowBuilder.getMatch();
        EthernetMatch ethMatch = match.getEthernetMatch();
        Assert.assertEquals(MAC_ADDRESS, ethMatch.getEthernetSource().getAddress().getValue());
        Assert.assertEquals((long) IPV4_ETHER_TYPE, (long) ethMatch.getEthernetType().getType().getValue());

        Assert.assertTrue(match.getLayer4Match() instanceof UdpMatch);
        UdpMatch layer4Match = (UdpMatch) match.getLayer4Match();
        Assert.assertEquals(50, layer4Match.getUdpDestinationPort().getValue().intValue());
        int port = portSecurityRule.getSecurityRulePortMin();
        String expectedFlowId1 = "Egress_UDP_" + SEGMENT_ID + "_" + MAC_ADDRESS + "_" + port + "_" + DEST_IP_1 +
                "_Permit";
        String expectedFlowId2 = "Egress_UDP_" + SEGMENT_ID + "_" + MAC_ADDRESS + "_" + port + "_" + DEST_IP_2 +
                "_Permit";
        String actualFlowId = flowBuilder.getFlowName();
        if(actualFlowId.equals(expectedFlowId1) || actualFlowId.equals(expectedFlowId2)) {
            Assert.assertTrue(true);
        } else {
            Assert.assertTrue(false);
        }
    }

    /**
     *  Test IPv6 UDP remove with port no and remote SG selected.
     */
    @Test
    public void testProgramPortSecurityACLRuleIpv6RemoveUdp2() throws Exception {
        when(portSecurityIpv6Rule.getSecurityRuleProtocol()).thenReturn("udp");
        when(portSecurityIpv6Rule.getSecurityRulePortMax()).thenReturn(50);
        when(portSecurityIpv6Rule.getSecurityRulePortMin()).thenReturn(50);
        when(portSecurityIpv6Rule.getSecurityRuleRemoteIpPrefix()).thenReturn("::/64");
        when(portSecurityIpv6Rule.getSecurityRemoteGroupID()).thenReturn("85cc3048-abc3-43cc-89b3-377341426ac5");
        PowerMockito.doAnswer(answer()).when(egressAclServiceSpy, "removeFlow", any(FlowBuilder.class),
                any(NodeBuilder.class));
        egressAclServiceSpy.programPortSecurityGroup(DP_ID_LONG, SEGMENT_ID, MAC_ADDRESS, LOCAL_PORT, securityGroupIpv6,
                PORT_UUID, false);

        Match match = flowBuilder.getMatch();
        EthernetMatch ethMatch = match.getEthernetMatch();
        Assert.assertEquals(MAC_ADDRESS, ethMatch.getEthernetSource().getAddress().getValue());
        Assert.assertEquals((long) IPV6_ETHER_TYPE, (long) ethMatch.getEthernetType().getType().getValue());

        Assert.assertTrue(match.getLayer4Match() instanceof UdpMatch);
        UdpMatch layer4Match = (UdpMatch) match.getLayer4Match();
        Assert.assertEquals(50, layer4Match.getUdpDestinationPort().getValue().intValue());
        int port = portSecurityIpv6Rule.getSecurityRulePortMin();
        String expectedFlowId1 = "Egress_UDP_" + SEGMENT_ID + "_" + MAC_ADDRESS + "_" + port + "_" + IPV6_DEST_IP_1 +
                "_Permit";
        String expectedFlowId2 = "Egress_UDP_" + SEGMENT_ID + "_" + MAC_ADDRESS + "_" + port + "_" + IPV6_DEST_IP_2 +
                "_Permit";
        String actualFlowId = flowBuilder.getFlowName();
        if(actualFlowId.equals(expectedFlowId1) || actualFlowId.equals(expectedFlowId2)) {
            Assert.assertTrue(true);
        } else {
            Assert.assertTrue(false);
        }
    }

    /**
     *  Test IPv4 UDP add with port (All UDP) and CIDR selected.
     */
    @Test
    public void testProgramPortSecurityACLRuleAddUdpAll1() throws Exception {
        when(portSecurityRule.getSecurityRuleProtocol()).thenReturn("udp");
        when(portSecurityRule.getSecurityRulePortMax()).thenReturn(PORT_RANGE_MAX);
        when(portSecurityRule.getSecurityRulePortMin()).thenReturn(PORT_RANGE_MIN);
        when(portSecurityRule.getSecurityRuleRemoteIpPrefix()).thenReturn("0.0.0.0/24");
        PowerMockito.doAnswer(answer()).when(egressAclServiceSpy, "writeFlow", any(FlowBuilder.class),
                                             any(NodeBuilder.class));
        egressAclServiceSpy.programPortSecurityGroup(DP_ID_LONG, SEGMENT_ID, MAC_ADDRESS, LOCAL_PORT, securityGroup,
                                                     PORT_UUID, true);

        Match match = flowBuilder.getMatch();
        EthernetMatch ethMatch = match.getEthernetMatch();
        Assert.assertEquals(MAC_ADDRESS, ethMatch.getEthernetSource().getAddress().getValue());
        Assert.assertEquals((long) IPV4_ETHER_TYPE, (long) ethMatch.getEthernetType().getType().getValue());

        Assert.assertTrue(match.getLayer4Match() instanceof UdpMatch);
        Assert.assertEquals("Egress_UDP_" + SEGMENT_ID + "_" + MAC_ADDRESS + "_" + PORT_RANGE_MIN + "_" +
                            PORT_RANGE_MAX + "_0.0.0.0/24_Permit", flowBuilder.getFlowName());
    }

    /**
     *  Test IPv6 UDP add with port (All UDP) and CIDR selected.
     */
    @Test
    public void testProgramPortSecurityACLRuleIpv6AddUdpAll1() throws Exception {
        when(portSecurityIpv6Rule.getSecurityRuleProtocol()).thenReturn("udp");
        when(portSecurityIpv6Rule.getSecurityRulePortMax()).thenReturn(PORT_RANGE_MAX);
        when(portSecurityIpv6Rule.getSecurityRulePortMin()).thenReturn(PORT_RANGE_MIN);
        when(portSecurityIpv6Rule.getSecurityRuleRemoteIpPrefix()).thenReturn("::/64");
        PowerMockito.doAnswer(answer()).when(egressAclServiceSpy, "writeFlow", any(FlowBuilder.class),
                any(NodeBuilder.class));
        egressAclServiceSpy.programPortSecurityGroup(DP_ID_LONG, SEGMENT_ID, MAC_ADDRESS, LOCAL_PORT, securityGroupIpv6,
                PORT_UUID, true);

        Match match = flowBuilder.getMatch();
        EthernetMatch ethMatch = match.getEthernetMatch();
        Assert.assertEquals(MAC_ADDRESS, ethMatch.getEthernetSource().getAddress().getValue());
        Assert.assertEquals((long) IPV6_ETHER_TYPE, (long) ethMatch.getEthernetType().getType().getValue());

        Assert.assertTrue(match.getLayer4Match() instanceof UdpMatch);
        Assert.assertEquals("Egress_UDP_" + SEGMENT_ID + "_" + MAC_ADDRESS + "_" + PORT_RANGE_MIN + "_" +
                PORT_RANGE_MAX + "_::/64_Permit", flowBuilder.getFlowName());
    }

    /**
     *  Test IPv4 UDP remove with port (All UDP) and CIDR selected.
     */
    @Test
    public void testProgramPortSecurityACLRuleRemoveUdpAll1() throws Exception {
        when(portSecurityRule.getSecurityRuleProtocol()).thenReturn("udp");
        when(portSecurityRule.getSecurityRulePortMax()).thenReturn(PORT_RANGE_MAX);
        when(portSecurityRule.getSecurityRulePortMin()).thenReturn(PORT_RANGE_MIN);
        when(portSecurityRule.getSecurityRuleRemoteIpPrefix()).thenReturn("0.0.0.0/24");
        PowerMockito.doAnswer(answer()).when(egressAclServiceSpy, "removeFlow", any(FlowBuilder.class),
                                             any(NodeBuilder.class));
        egressAclServiceSpy.programPortSecurityGroup(DP_ID_LONG, SEGMENT_ID, MAC_ADDRESS, LOCAL_PORT, securityGroup,
                                                     PORT_UUID, false);

        Match match = flowBuilder.getMatch();
        EthernetMatch ethMatch = match.getEthernetMatch();
        Assert.assertEquals(MAC_ADDRESS, ethMatch.getEthernetSource().getAddress().getValue());
        Assert.assertEquals((long) IPV4_ETHER_TYPE, (long) ethMatch.getEthernetType().getType().getValue());

        Assert.assertTrue(match.getLayer4Match() instanceof UdpMatch);
        Assert.assertEquals("Egress_UDP_" + SEGMENT_ID + "_" + MAC_ADDRESS + "_" + PORT_RANGE_MIN + "_" +
                            PORT_RANGE_MAX + "_0.0.0.0/24_Permit", flowBuilder.getFlowName());
    }

    /**
     *  Test IPv6 UDP remove with port (All UDP) and CIDR selected.
     */
    @Test
    public void testProgramPortSecurityACLRuleIpv6RemoveUdpAll1() throws Exception {
        when(portSecurityIpv6Rule.getSecurityRuleProtocol()).thenReturn("udp");
        when(portSecurityIpv6Rule.getSecurityRulePortMax()).thenReturn(PORT_RANGE_MAX);
        when(portSecurityIpv6Rule.getSecurityRulePortMin()).thenReturn(PORT_RANGE_MIN);
        when(portSecurityIpv6Rule.getSecurityRuleRemoteIpPrefix()).thenReturn("::/64");
        PowerMockito.doAnswer(answer()).when(egressAclServiceSpy, "removeFlow", any(FlowBuilder.class),
                any(NodeBuilder.class));
        egressAclServiceSpy.programPortSecurityGroup(DP_ID_LONG, SEGMENT_ID, MAC_ADDRESS, LOCAL_PORT, securityGroupIpv6,
                PORT_UUID, false);

        Match match = flowBuilder.getMatch();
        EthernetMatch ethMatch = match.getEthernetMatch();
        Assert.assertEquals(MAC_ADDRESS, ethMatch.getEthernetSource().getAddress().getValue());
        Assert.assertEquals((long) IPV6_ETHER_TYPE, (long) ethMatch.getEthernetType().getType().getValue());

        Assert.assertTrue(match.getLayer4Match() instanceof UdpMatch);
        Assert.assertEquals("Egress_UDP_" + SEGMENT_ID + "_" + MAC_ADDRESS + "_" + PORT_RANGE_MIN + "_" +
                PORT_RANGE_MAX + "_::/64_Permit", flowBuilder.getFlowName());
    }

    /**
     *  Test IPv4 UDP add with port (All UDP) and remote SG selected.
     */
    @Test
    public void testProgramPortSecurityACLRuleAddUdpAll2() throws Exception {
        when(portSecurityRule.getSecurityRuleProtocol()).thenReturn("udp");
        when(portSecurityRule.getSecurityRulePortMax()).thenReturn(PORT_RANGE_MAX);
        when(portSecurityRule.getSecurityRulePortMin()).thenReturn(PORT_RANGE_MIN);
        when(portSecurityRule.getSecurityRemoteGroupID()).thenReturn("85cc3048-abc3-43cc-89b3-377341426ac5");
        PowerMockito.doAnswer(answer()).when(egressAclServiceSpy, "writeFlow", any(FlowBuilder.class),
                                             any(NodeBuilder.class));
        egressAclServiceSpy.programPortSecurityGroup(DP_ID_LONG, SEGMENT_ID, MAC_ADDRESS, LOCAL_PORT, securityGroup,
                                                     PORT_UUID, true);

        Match match = flowBuilder.getMatch();
        EthernetMatch ethMatch = match.getEthernetMatch();
        Assert.assertEquals(MAC_ADDRESS, ethMatch.getEthernetSource().getAddress().getValue());
        Assert.assertEquals((long) IPV4_ETHER_TYPE, (long) ethMatch.getEthernetType().getType().getValue());

        Assert.assertTrue(match.getLayer4Match() instanceof UdpMatch);
        String expectedFlowId1 = "Egress_UDP_" + SEGMENT_ID + "_" + MAC_ADDRESS + "_" + PORT_RANGE_MIN + "_" +
                                PORT_RANGE_MAX + "_" + DEST_IP_1 + "_Permit";
        String expectedFlowId2 = "Egress_UDP_" + SEGMENT_ID + "_" + MAC_ADDRESS + "_" + PORT_RANGE_MIN + "_" +
                                PORT_RANGE_MAX + "_" + DEST_IP_2 + "_Permit";
        String actualFlowId = flowBuilder.getFlowName();
        if(actualFlowId.equals(expectedFlowId1) || actualFlowId.equals(expectedFlowId2)) {
            Assert.assertTrue(true);
        } else {
            Assert.assertTrue(false);
        }
    }

    /**
     *  Test IPv6 UDP add with port (All UDP) and remote SG selected.
     */
    @Test
    public void testProgramPortSecurityACLRuleIpv6AddUdpAll2() throws Exception {
        when(portSecurityIpv6Rule.getSecurityRuleProtocol()).thenReturn("udp");
        when(portSecurityIpv6Rule.getSecurityRulePortMax()).thenReturn(PORT_RANGE_MAX);
        when(portSecurityIpv6Rule.getSecurityRulePortMin()).thenReturn(PORT_RANGE_MIN);
        when(portSecurityIpv6Rule.getSecurityRemoteGroupID()).thenReturn("85cc3048-abc3-43cc-89b3-377341426ac5");
        PowerMockito.doAnswer(answer()).when(egressAclServiceSpy, "writeFlow", any(FlowBuilder.class),
                any(NodeBuilder.class));
        egressAclServiceSpy.programPortSecurityGroup(DP_ID_LONG, SEGMENT_ID, MAC_ADDRESS, LOCAL_PORT, securityGroupIpv6,
                PORT_UUID, true);

        Match match = flowBuilder.getMatch();
        EthernetMatch ethMatch = match.getEthernetMatch();
        Assert.assertEquals(MAC_ADDRESS, ethMatch.getEthernetSource().getAddress().getValue());
        Assert.assertEquals((long) IPV6_ETHER_TYPE, (long) ethMatch.getEthernetType().getType().getValue());

        Assert.assertTrue(match.getLayer4Match() instanceof UdpMatch);
        String expectedFlowId1 = "Egress_UDP_" + SEGMENT_ID + "_" + MAC_ADDRESS + "_" + PORT_RANGE_MIN + "_" +
                PORT_RANGE_MAX + "_" + IPV6_DEST_IP_1 + "_Permit";
        String expectedFlowId2 = "Egress_UDP_" + SEGMENT_ID + "_" + MAC_ADDRESS + "_" + PORT_RANGE_MIN + "_" +
                PORT_RANGE_MAX + "_" + IPV6_DEST_IP_2 + "_Permit";
        String actualFlowId = flowBuilder.getFlowName();
        if(actualFlowId.equals(expectedFlowId1) || actualFlowId.equals(expectedFlowId2)) {
            Assert.assertTrue(true);
        } else {
            Assert.assertTrue(false);
        }
    }

    /**
     *  Test IPv4 UDP remove with port (All UDP) and remote SG selected.
     */
    @Test
    public void testProgramPortSecurityACLRuleRemoveUdpAll2() throws Exception {
        when(portSecurityRule.getSecurityRuleProtocol()).thenReturn("udp");
        when(portSecurityRule.getSecurityRulePortMax()).thenReturn(PORT_RANGE_MAX);
        when(portSecurityRule.getSecurityRulePortMin()).thenReturn(PORT_RANGE_MIN);
        when(portSecurityRule.getSecurityRemoteGroupID()).thenReturn("85cc3048-abc3-43cc-89b3-377341426ac5");
        PowerMockito.doAnswer(answer()).when(egressAclServiceSpy, "removeFlow", any(FlowBuilder.class),
                                             any(NodeBuilder.class));
        egressAclServiceSpy.programPortSecurityGroup(DP_ID_LONG, SEGMENT_ID, MAC_ADDRESS, LOCAL_PORT, securityGroup,
                                                     PORT_UUID, false);

        Match match = flowBuilder.getMatch();
        EthernetMatch ethMatch = match.getEthernetMatch();
        Assert.assertEquals(MAC_ADDRESS, ethMatch.getEthernetSource().getAddress().getValue());
        Assert.assertEquals((long) IPV4_ETHER_TYPE, (long) ethMatch.getEthernetType().getType().getValue());

        Assert.assertTrue(match.getLayer4Match() instanceof UdpMatch);
        String expectedFlowId1 = "Egress_UDP_" + SEGMENT_ID + "_" + MAC_ADDRESS + "_" + PORT_RANGE_MIN + "_" +
                PORT_RANGE_MAX + "_" + DEST_IP_1 + "_Permit";
        String expectedFlowId2 = "Egress_UDP_" + SEGMENT_ID + "_" + MAC_ADDRESS + "_" + PORT_RANGE_MIN + "_" +
                PORT_RANGE_MAX + "_" + DEST_IP_2 + "_Permit";
        String actualFlowId = flowBuilder.getFlowName();
        if(actualFlowId.equals(expectedFlowId1) || actualFlowId.equals(expectedFlowId2)) {
            Assert.assertTrue(true);
        } else {
            Assert.assertTrue(false);
        }
    }

    /**
     *  Test IPv6 UDP remove with port (All UDP) and remote SG selected.
     */
    @Test
    public void testProgramPortSecurityACLRuleIpv6RemoveUdpAll2() throws Exception {
        when(portSecurityIpv6Rule.getSecurityRuleProtocol()).thenReturn("udp");
        when(portSecurityIpv6Rule.getSecurityRulePortMax()).thenReturn(PORT_RANGE_MAX);
        when(portSecurityIpv6Rule.getSecurityRulePortMin()).thenReturn(PORT_RANGE_MIN);
        when(portSecurityIpv6Rule.getSecurityRemoteGroupID()).thenReturn("85cc3048-abc3-43cc-89b3-377341426ac5");
        PowerMockito.doAnswer(answer()).when(egressAclServiceSpy, "removeFlow", any(FlowBuilder.class),
                any(NodeBuilder.class));
        egressAclServiceSpy.programPortSecurityGroup(DP_ID_LONG, SEGMENT_ID, MAC_ADDRESS, LOCAL_PORT, securityGroupIpv6,
                PORT_UUID, false);

        Match match = flowBuilder.getMatch();
        EthernetMatch ethMatch = match.getEthernetMatch();
        Assert.assertEquals(MAC_ADDRESS, ethMatch.getEthernetSource().getAddress().getValue());
        Assert.assertEquals((long) IPV6_ETHER_TYPE, (long) ethMatch.getEthernetType().getType().getValue());

        Assert.assertTrue(match.getLayer4Match() instanceof UdpMatch);
        String expectedFlowId1 = "Egress_UDP_" + SEGMENT_ID + "_" + MAC_ADDRESS + "_" + PORT_RANGE_MIN + "_" +
                PORT_RANGE_MAX + "_" + IPV6_DEST_IP_1 + "_Permit";
        String expectedFlowId2 = "Egress_UDP_" + SEGMENT_ID + "_" + MAC_ADDRESS + "_" + PORT_RANGE_MIN + "_" +
                PORT_RANGE_MAX + "_" + IPV6_DEST_IP_2 + "_Permit";
        String actualFlowId = flowBuilder.getFlowName();
        if(actualFlowId.equals(expectedFlowId1) || actualFlowId.equals(expectedFlowId2)) {
            Assert.assertTrue(true);
        } else {
            Assert.assertTrue(false);
        }
    }

    /**
     *  Test ICMP add with code, type and CIDR selected.
     */
    @Test
    public void testProgramPortSecurityACLRuleAddIcmp1() throws Exception {
        when(portSecurityRule.getSecurityRuleProtocol()).thenReturn("icmp");
        when(portSecurityRule.getSecurityRulePortMax()).thenReturn(10);
        when(portSecurityRule.getSecurityRulePortMin()).thenReturn(10);
        when(portSecurityRule.getSecurityRuleRemoteIpPrefix()).thenReturn("0.0.0.0/24");
        PowerMockito.doAnswer(answer()).when(egressAclServiceSpy, "writeFlow", any(FlowBuilder.class),
                                             any(NodeBuilder.class));

        egressAclServiceSpy.programPortSecurityGroup(DP_ID_LONG, SEGMENT_ID, MAC_ADDRESS, LOCAL_PORT, securityGroup,
                                                     PORT_UUID, true);

        Match match = flowBuilder.getMatch();
        Icmpv4Match icmpv4Match = match.getIcmpv4Match();
        Assert.assertEquals(10, icmpv4Match.getIcmpv4Type().shortValue());
        Assert.assertEquals(10, icmpv4Match.getIcmpv4Code().shortValue());
        EthernetMatch ethMatch = match.getEthernetMatch();
        Assert.assertEquals(MAC_ADDRESS, ethMatch.getEthernetSource().getAddress().getValue());
        Assert.assertEquals((long) IPV4_ETHER_TYPE, (long) ethMatch.getEthernetType().getType().getValue());
        Short type = portSecurityRule.getSecurityRulePortMin().shortValue();
        Short code = portSecurityRule.getSecurityRulePortMax().shortValue();
        Assert.assertEquals("Egress_ICMP_" + SEGMENT_ID + "_" + MAC_ADDRESS +
                            "_" + type + "_" + code + "_0.0.0.0/24_Permit", flowBuilder.getFlowName());
    }

    /**
     *  Test ICMPv6 add with code, type and CIDR selected.
     */
    @Test
    public void testProgramPortSecurityACLRuleIpv6AddIcmp1() throws Exception {
        when(portSecurityIpv6Rule.getSecurityRuleProtocol()).thenReturn("icmpv6");
        when(portSecurityIpv6Rule.getSecurityRulePortMax()).thenReturn(10);
        when(portSecurityIpv6Rule.getSecurityRulePortMin()).thenReturn(10);
        when(portSecurityIpv6Rule.getSecurityRuleRemoteIpPrefix()).thenReturn("::/64");
        PowerMockito.doAnswer(answer()).when(egressAclServiceSpy, "writeFlow", any(FlowBuilder.class),
                any(NodeBuilder.class));

        egressAclServiceSpy.programPortSecurityGroup(DP_ID_LONG, SEGMENT_ID, MAC_ADDRESS, LOCAL_PORT, securityGroupIpv6,
                PORT_UUID, true);

        Match match = flowBuilder.getMatch();
        Icmpv6Match icmpv6Match = match.getIcmpv6Match();
        Assert.assertEquals(10, icmpv6Match.getIcmpv6Type().shortValue());
        Assert.assertEquals(10, icmpv6Match.getIcmpv6Code().shortValue());
        EthernetMatch ethMatch = match.getEthernetMatch();
        Assert.assertEquals(MAC_ADDRESS, ethMatch.getEthernetSource().getAddress().getValue());
        Assert.assertEquals((long) IPV6_ETHER_TYPE, (long) ethMatch.getEthernetType().getType().getValue());
        Short type = portSecurityIpv6Rule.getSecurityRulePortMin().shortValue();
        Short code = portSecurityIpv6Rule.getSecurityRulePortMax().shortValue();
        Assert.assertEquals("Egress_ICMP_" + SEGMENT_ID + "_" + MAC_ADDRESS +
                "_" + type + "_" + code + "_::/64_Permit", flowBuilder.getFlowName());
    }

    /**
     *  Test ICMP remove with code, type and CIDR selected.
     */
    @Test
    public void testProgramPortSecurityACLRuleRemoveIcmp1() throws Exception {
        when(portSecurityRule.getSecurityRuleProtocol()).thenReturn("icmp");
        when(portSecurityRule.getSecurityRulePortMax()).thenReturn(20);
        when(portSecurityRule.getSecurityRulePortMin()).thenReturn(20);
        when(portSecurityRule.getSecurityRuleRemoteIpPrefix()).thenReturn("0.0.0.0/24");
        PowerMockito.doAnswer(answer()).when(egressAclServiceSpy, "removeFlow", any(FlowBuilder.class),
                                             any(NodeBuilder.class));

        egressAclServiceSpy.programPortSecurityGroup(DP_ID_LONG, SEGMENT_ID, MAC_ADDRESS, LOCAL_PORT, securityGroup,
                                                     PORT_UUID, false);

        Match match = flowBuilder.getMatch();
        Icmpv4Match icmpv4Match = match.getIcmpv4Match();
        Assert.assertEquals(20, icmpv4Match.getIcmpv4Type().shortValue());
        Assert.assertEquals(20, icmpv4Match.getIcmpv4Code().shortValue());
        EthernetMatch ethMatch = match.getEthernetMatch();
        Assert.assertEquals(MAC_ADDRESS, ethMatch.getEthernetSource().getAddress().getValue());
        Assert.assertEquals((long) IPV4_ETHER_TYPE, (long) ethMatch.getEthernetType().getType().getValue());
        Short type = portSecurityRule.getSecurityRulePortMin().shortValue();
        Short code = portSecurityRule.getSecurityRulePortMax().shortValue();
        Assert.assertEquals("Egress_ICMP_" + SEGMENT_ID + "_" + MAC_ADDRESS +
                            "_" + type + "_" + code + "_0.0.0.0/24_Permit", flowBuilder.getFlowName());
    }

    /**
     *  Test ICMPv6 remove with code, type and CIDR selected.
     */
    @Test
    public void testProgramPortSecurityACLRuleIpv6RemoveIcmp1() throws Exception {
        when(portSecurityIpv6Rule.getSecurityRuleProtocol()).thenReturn("icmpv6");
        when(portSecurityIpv6Rule.getSecurityRulePortMax()).thenReturn(20);
        when(portSecurityIpv6Rule.getSecurityRulePortMin()).thenReturn(20);
        when(portSecurityIpv6Rule.getSecurityRuleRemoteIpPrefix()).thenReturn("::/64");
        PowerMockito.doAnswer(answer()).when(egressAclServiceSpy, "removeFlow", any(FlowBuilder.class),
                any(NodeBuilder.class));

        egressAclServiceSpy.programPortSecurityGroup(DP_ID_LONG, SEGMENT_ID, MAC_ADDRESS, LOCAL_PORT, securityGroupIpv6,
                PORT_UUID, false);

        Match match = flowBuilder.getMatch();
        Icmpv6Match icmpv6Match = match.getIcmpv6Match();
        Assert.assertEquals(20, icmpv6Match.getIcmpv6Type().shortValue());
        Assert.assertEquals(20, icmpv6Match.getIcmpv6Code().shortValue());
        EthernetMatch ethMatch = match.getEthernetMatch();
        Assert.assertEquals(MAC_ADDRESS, ethMatch.getEthernetSource().getAddress().getValue());
        Assert.assertEquals((long) IPV6_ETHER_TYPE, (long) ethMatch.getEthernetType().getType().getValue());
        Short type = portSecurityIpv6Rule.getSecurityRulePortMin().shortValue();
        Short code = portSecurityIpv6Rule.getSecurityRulePortMax().shortValue();
        Assert.assertEquals("Egress_ICMP_" + SEGMENT_ID + "_" + MAC_ADDRESS +
                "_" + type + "_" + code + "_::/64_Permit", flowBuilder.getFlowName());
    }

    /**
     *  Test ICMP add with code, type and remote SG selected.
     */
    @Test
    public void testProgramPortSecurityACLRuleAddIcmp2() throws Exception {
        when(portSecurityRule.getSecurityRuleProtocol()).thenReturn("icmp");
        when(portSecurityRule.getSecurityRulePortMax()).thenReturn(30);
        when(portSecurityRule.getSecurityRulePortMin()).thenReturn(30);
        when(portSecurityRule.getSecurityRuleRemoteIpPrefix()).thenReturn("0.0.0.0/24");
        when(portSecurityRule.getSecurityRemoteGroupID()).thenReturn("85cc3048-abc3-43cc-89b3-377341426ac5");
        PowerMockito.doAnswer(answer()).when(egressAclServiceSpy, "writeFlow", any(FlowBuilder.class),
                                             any(NodeBuilder.class));

        egressAclServiceSpy.programPortSecurityGroup(DP_ID_LONG, SEGMENT_ID, MAC_ADDRESS, LOCAL_PORT, securityGroup,
                                                     PORT_UUID, true);

        Match match = flowBuilder.getMatch();
        Icmpv4Match icmpv4Match = match.getIcmpv4Match();
        Assert.assertEquals(30, icmpv4Match.getIcmpv4Type().shortValue());
        Assert.assertEquals(30, icmpv4Match.getIcmpv4Code().shortValue());
        EthernetMatch ethMatch = match.getEthernetMatch();
        Assert.assertEquals(MAC_ADDRESS, ethMatch.getEthernetSource().getAddress().getValue());
        Assert.assertEquals((long) IPV4_ETHER_TYPE, (long) ethMatch.getEthernetType().getType().getValue());
        Short type = portSecurityRule.getSecurityRulePortMin().shortValue();
        Short code = portSecurityRule.getSecurityRulePortMax().shortValue();
        String expectedFlowId1 = "Egress_ICMP_" + SEGMENT_ID + "_" + MAC_ADDRESS + "_" + type + "_" + code + "_"
                                + DEST_IP_1 + "_Permit";
        String expectedFlowId2 = "Egress_ICMP_" + SEGMENT_ID + "_" + MAC_ADDRESS + "_" + type + "_" + code + "_"
                                + DEST_IP_2 + "_Permit";
        String actualFlowId = flowBuilder.getFlowName();
        if(actualFlowId.equals(expectedFlowId1) || actualFlowId.equals(expectedFlowId2)) {
            Assert.assertTrue(true);
        } else {
            Assert.assertTrue(false);
        }
    }

    /**
     *  Test ICMPv6 add with code, type and remote SG selected.
     */
    @Test
    public void testProgramPortSecurityACLRuleIpv6AddIcmp2() throws Exception {
        when(portSecurityIpv6Rule.getSecurityRuleProtocol()).thenReturn("icmpv6");
        when(portSecurityIpv6Rule.getSecurityRulePortMax()).thenReturn(30);
        when(portSecurityIpv6Rule.getSecurityRulePortMin()).thenReturn(30);
        when(portSecurityIpv6Rule.getSecurityRuleRemoteIpPrefix()).thenReturn("::/64");
        when(portSecurityIpv6Rule.getSecurityRemoteGroupID()).thenReturn("85cc3048-abc3-43cc-89b3-377341426ac5");
        PowerMockito.doAnswer(answer()).when(egressAclServiceSpy, "writeFlow", any(FlowBuilder.class),
                any(NodeBuilder.class));

        egressAclServiceSpy.programPortSecurityGroup(DP_ID_LONG, SEGMENT_ID, MAC_ADDRESS, LOCAL_PORT, securityGroupIpv6,
                PORT_UUID, true);

        Match match = flowBuilder.getMatch();
        Icmpv6Match icmpv6Match = match.getIcmpv6Match();
        Assert.assertEquals(30, icmpv6Match.getIcmpv6Type().shortValue());
        Assert.assertEquals(30, icmpv6Match.getIcmpv6Code().shortValue());
        EthernetMatch ethMatch = match.getEthernetMatch();
        Assert.assertEquals(MAC_ADDRESS, ethMatch.getEthernetSource().getAddress().getValue());
        Assert.assertEquals((long) IPV6_ETHER_TYPE, (long) ethMatch.getEthernetType().getType().getValue());
        Short type = portSecurityIpv6Rule.getSecurityRulePortMin().shortValue();
        Short code = portSecurityIpv6Rule.getSecurityRulePortMax().shortValue();
        String expectedFlowId1 = "Egress_ICMP_" + SEGMENT_ID + "_" + MAC_ADDRESS + "_" + type + "_" + code + "_"
                + IPV6_DEST_IP_1 + "_Permit";
        String expectedFlowId2 = "Egress_ICMP_" + SEGMENT_ID + "_" + MAC_ADDRESS + "_" + type + "_" + code + "_"
                + IPV6_DEST_IP_2 + "_Permit";
        String actualFlowId = flowBuilder.getFlowName();
        if(actualFlowId.equals(expectedFlowId1) || actualFlowId.equals(expectedFlowId2)) {
            Assert.assertTrue(true);
        } else {
            Assert.assertTrue(false);
        }
    }

    /**
     *  Test ICMP remove with code, type and remote SG selected.
     */
    @Test
    public void testProgramPortSecurityACLRuleRemoveIcmp2() throws Exception {
        when(portSecurityRule.getSecurityRuleProtocol()).thenReturn("icmp");
        when(portSecurityRule.getSecurityRulePortMax()).thenReturn(40);
        when(portSecurityRule.getSecurityRulePortMin()).thenReturn(40);
        when(portSecurityRule.getSecurityRemoteGroupID()).thenReturn("85cc3048-abc3-43cc-89b3-377341426ac5");
        PowerMockito.doAnswer(answer()).when(egressAclServiceSpy, "removeFlow", any(FlowBuilder.class),
                                             any(NodeBuilder.class));

        egressAclServiceSpy.programPortSecurityGroup(DP_ID_LONG, SEGMENT_ID, MAC_ADDRESS, LOCAL_PORT, securityGroup,
                                                     PORT_UUID, false);

        Match match = flowBuilder.getMatch();
        Icmpv4Match icmpv4Match = match.getIcmpv4Match();
        Assert.assertEquals(40, icmpv4Match.getIcmpv4Type().shortValue());
        Assert.assertEquals(40, icmpv4Match.getIcmpv4Code().shortValue());
        EthernetMatch ethMatch = match.getEthernetMatch();
        Assert.assertEquals(MAC_ADDRESS, ethMatch.getEthernetSource().getAddress().getValue());
        Assert.assertEquals((long) IPV4_ETHER_TYPE, (long) ethMatch.getEthernetType().getType().getValue());
        Short type = portSecurityRule.getSecurityRulePortMin().shortValue();
        Short code = portSecurityRule.getSecurityRulePortMax().shortValue();
        String expectedFlowId1 = "Egress_ICMP_" + SEGMENT_ID + "_" + MAC_ADDRESS + "_" + type + "_" + code + "_"
                                + DEST_IP_1 + "_Permit";
        String expectedFlowId2 = "Egress_ICMP_" + SEGMENT_ID + "_" + MAC_ADDRESS + "_" + type + "_" + code + "_"
                                + DEST_IP_2 + "_Permit";
        String actualFlowId = flowBuilder.getFlowName();
        if(actualFlowId.equals(expectedFlowId1) || actualFlowId.equals(expectedFlowId2)) {
            Assert.assertTrue(true);
        } else {
            Assert.assertTrue(false);
        }
    }

    /**
     *  Test ICMPv6 remove with code, type and remote SG selected.
     */
    @Test
    public void testProgramPortSecurityACLRuleIpv6RemoveIcmp2() throws Exception {
        when(portSecurityIpv6Rule.getSecurityRuleProtocol()).thenReturn("icmpv6");
        when(portSecurityIpv6Rule.getSecurityRulePortMax()).thenReturn(40);
        when(portSecurityIpv6Rule.getSecurityRulePortMin()).thenReturn(40);
        when(portSecurityIpv6Rule.getSecurityRemoteGroupID()).thenReturn("85cc3048-abc3-43cc-89b3-377341426ac5");
        PowerMockito.doAnswer(answer()).when(egressAclServiceSpy, "removeFlow", any(FlowBuilder.class),
                any(NodeBuilder.class));

        egressAclServiceSpy.programPortSecurityGroup(DP_ID_LONG, SEGMENT_ID, MAC_ADDRESS, LOCAL_PORT, securityGroupIpv6,
                PORT_UUID, false);

        Match match = flowBuilder.getMatch();
        Icmpv6Match icmpv6Match = match.getIcmpv6Match();
        Assert.assertEquals(40, icmpv6Match.getIcmpv6Type().shortValue());
        Assert.assertEquals(40, icmpv6Match.getIcmpv6Code().shortValue());
        EthernetMatch ethMatch = match.getEthernetMatch();
        Assert.assertEquals(MAC_ADDRESS, ethMatch.getEthernetSource().getAddress().getValue());
        Assert.assertEquals((long) IPV6_ETHER_TYPE, (long) ethMatch.getEthernetType().getType().getValue());
        Short type = portSecurityIpv6Rule.getSecurityRulePortMin().shortValue();
        Short code = portSecurityIpv6Rule.getSecurityRulePortMax().shortValue();
        String expectedFlowId1 = "Egress_ICMP_" + SEGMENT_ID + "_" + MAC_ADDRESS + "_" + type + "_" + code + "_"
                + IPV6_DEST_IP_1 + "_Permit";
        String expectedFlowId2 = "Egress_ICMP_" + SEGMENT_ID + "_" + MAC_ADDRESS + "_" + type + "_" + code + "_"
                + IPV6_DEST_IP_2 + "_Permit";
        String actualFlowId = flowBuilder.getFlowName();
        if(actualFlowId.equals(expectedFlowId1) || actualFlowId.equals(expectedFlowId2)) {
            Assert.assertTrue(true);
        } else {
            Assert.assertTrue(false);
        }
    }

    /**
     *  Test IPv4 invalid ether type test case.
     */
    @Test
    public void testProgramPortSecurityACLRuleInvalidEther() throws Exception {
        when(portSecurityRule.getSecurityRuleEthertype()).thenReturn("IP");

        egressAclServiceSpy.programPortSecurityGroup(Long.valueOf(1554), "2", MAC_ADDRESS, 124, securityGroup,PORT_UUID,false);

        verify(writeTransaction, times(0)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(writeTransaction, times(0)).submit();
        verify(commitFuture, times(0)).get();
    }

    /**
     *  Test IPv4 invalid direction type test case.
     */
    @Test
    public void testProgramPortSecurityACLRuleInvalidDirection() throws Exception {
        when(portSecurityRule.getSecurityRuleDirection()).thenReturn("ingress");

        egressAclServiceSpy.programPortSecurityGroup(Long.valueOf(1554), "2", MAC_ADDRESS, 124, securityGroup,PORT_UUID,false);

        verify(writeTransaction, times(0)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(writeTransaction, times(0)).submit();
        verify(commitFuture, times(0)).get();
    }

    /**
     *  Test With isConntrackEnabled false isComputeNode false
     */
    @Test
    public void testProgramFixedSecurityACLAdd1() throws Exception {
        when(securityServices.isConntrackEnabled()).thenReturn(false);

        egressAclServiceSpy.programFixedSecurityGroup(Long.valueOf(1554), "2", MAC_ADDRESS, 1, neutronDestIpList, false, false, true);

        verify(writeTransaction, times(2)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), eq(true));
        verify(writeTransaction, times(2)).submit();
        verify(commitFuture, times(2)).checkedGet();
    }
    /**
     *  Test With isConntrackEnabled false isComputeNode false
     */
    @Test
    public void testProgramFixedSecurityACLRemove1() throws Exception {
        when(securityServices.isConntrackEnabled()).thenReturn(false);

        egressAclServiceSpy.programFixedSecurityGroup(Long.valueOf(1554), "2", MAC_ADDRESS, 1, neutronDestIpList, false, false, false);

        verify(writeTransaction, times(2)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(writeTransaction, times(2)).submit();
        verify(commitFuture, times(2)).get();
    }

    /**
     *  Test With isConntrackEnabled false isComputeNode true
     */
    @Test
    public void testProgramFixedSecurityACLAdd2() throws Exception {
        when(securityServices.isConntrackEnabled()).thenReturn(false);

        egressAclServiceSpy.programFixedSecurityGroup(Long.valueOf(1554), "2", MAC_ADDRESS, 1, neutronDestIpList, false, true, true);

        verify(writeTransaction, times(9)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), eq(true));
        verify(writeTransaction, times(9)).submit();
        verify(commitFuture, times(9)).checkedGet();
    }

    /**
     *  Test With isConntrackEnabled false isComputeNode true
     */
    @Test
    public void testProgramFixedSecurityACLRemove2() throws Exception {
        when(securityServices.isConntrackEnabled()).thenReturn(false);

        egressAclServiceSpy.programFixedSecurityGroup(Long.valueOf(1554), "2", MAC_ADDRESS, 1, neutronDestIpList, false, true, false);

        verify(writeTransaction, times(9)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(writeTransaction, times(9)).submit();
        verify(commitFuture, times(9)).get();
    }

    /**
     *  Test With isConntrackEnabled true isComputeNode false
     */
    @Test
    public void testProgramFixedSecurityACLAdd3() throws Exception {
        when(securityServices.isConntrackEnabled()).thenReturn(true);

        egressAclServiceSpy.programFixedSecurityGroup(Long.valueOf(1554), "2", MAC_ADDRESS, 1, neutronDestIpList, false, false, true);

        verify(writeTransaction, times(2)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), eq(true));
        verify(writeTransaction, times(2)).submit();
        verify(commitFuture, times(2)).checkedGet();
    }

    /**
     *  Test With isConntrackEnabled true isComputeNode false
     */
    @Test
    public void testProgramFixedSecurityACLRemove3() throws Exception {
        when(securityServices.isConntrackEnabled()).thenReturn(true);

        egressAclServiceSpy.programFixedSecurityGroup(Long.valueOf(1554), "2", MAC_ADDRESS, 1, neutronDestIpList, false, false, false);

        verify(writeTransaction, times(2)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(writeTransaction, times(2)).submit();
        verify(commitFuture, times(2)).get();
    }

    /**
     *  Test With isConntrackEnabled true isComputeNode true
     */
    @Test
    public void testProgramFixedSecurityACLAdd4() throws Exception {
        when(securityServices.isConntrackEnabled()).thenReturn(true);

        egressAclServiceSpy.programFixedSecurityGroup(Long.valueOf(1554), "2", MAC_ADDRESS, 1, neutronDestIpList, false, true, true);

        verify(writeTransaction, times(12)).put(any(LogicalDatastoreType.class),
                                               any(InstanceIdentifier.class), any(Node.class), eq(true));
        verify(writeTransaction, times(12)).submit();
        verify(commitFuture, times(12)).checkedGet();
    }

    /**
     *  Test With isConntrackEnabled true isComputeNode true
     */
    @Test
    public void testProgramFixedSecurityACLRemove4() throws Exception {
        when(securityServices.isConntrackEnabled()).thenReturn(true);

        egressAclServiceSpy.programFixedSecurityGroup(Long.valueOf(1554), "2", MAC_ADDRESS, 1, neutronDestIpList, false, true, false);

        verify(writeTransaction, times(12)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(writeTransaction, times(12)).submit();
        verify(commitFuture, times(12)).get();
    }

}
