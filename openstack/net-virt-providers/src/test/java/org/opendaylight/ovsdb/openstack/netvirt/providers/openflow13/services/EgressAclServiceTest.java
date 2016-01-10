/*
 * Copyright (c) 2015, 2016 Inocybe and others.  All rights reserved.
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

    @Mock private SecurityServicesManager securityServices;
    @Mock private SecurityGroupCacheManger securityGroupCacheManger;

    private Neutron_IPs neutron_ip_src;
    private Neutron_IPs neutron_ip_dest_1;
    private Neutron_IPs neutron_ip_dest_2;
    private List<Neutron_IPs> neutronSrcIpList = new ArrayList<>();
    private List<Neutron_IPs> neutronDestIpList = new ArrayList<>();
    private static final String HOST_ADDRESS = "127.0.0.1/32";
    private static final String MAC_ADDRESS = "87:1D:5E:02:40:B7";
    private static final String SRC_IP = "192.168.0.1";
    private static final String DEST_IP_1 = "192.169.0.1";
    private static final String DEST_IP_2 = "192.169.0.2";
    private static final String DEST_IP_1_WITH_MASK = "192.169.0.1/32";
    private static final String DEST_IP_2_WITH_MASK = "192.169.0.2/32";
    private static final String SECURITY_GROUP_UUID = "85cc3048-abc3-43cc-89b3-377341426ac5";
    private static final String PORT_UUID = "95cc3048-abc3-43cc-89b3-377341426ac5";
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

        when(orchestrator.getNextServiceInPipeline(any(Service.class))).thenReturn(Service.RESPONDER);

        portSecurityRule = mock(NeutronSecurityRule.class);

        when(portSecurityRule.getSecurityRuleEthertype()).thenReturn("IPv4");
        when(portSecurityRule.getSecurityRuleDirection()).thenReturn("egress");

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

        when(securityGroup.getSecurityRules()).thenReturn(portSecurityList);
        when(securityServices.getVmListForSecurityGroup(PORT_UUID, SECURITY_GROUP_UUID)).thenReturn(neutronDestIpList);

        NetvirtProvidersProvider netvirtProvider = mock(NetvirtProvidersProvider.class);
        MemberModifier.field(NetvirtProvidersProvider.class, "hasProviderEntityOwnership").set(netvirtProvider, new AtomicBoolean(true));

    }

    /**
     * Rule 1: TCP Proto (True), TCP Port Minimum (True), TCP Port Max (True), IP Prefix (True)
     */
    /*@Test
    public void testProgramPortSecurityACLRule1() throws Exception {
        when(portSecurityRule.getSecurityRuleProtocol()).thenReturn("tcp");
        when(portSecurityRule.getSecurityRulePortMax()).thenReturn(1);
        when(portSecurityRule.getSecurityRulePortMin()).thenReturn(1);
        when(portSecurityRule.getSecurityRuleRemoteIpPrefix()).thenReturn(HOST_ADDRESS);

        egressAclServiceSpy.programPortSecurityACL(Long.valueOf(1554), "2", MAC_ADDRESS, 124, securityGroup);
        verify(egressAclServiceSpy, times(1)).egressACLDefaultTcpDrop(anyLong(), anyString(), anyString(), anyInt(), anyBoolean());
        verify(egressAclServiceSpy, times(1)).egressACLTcpPortWithPrefix(anyLong(), anyString(), anyString(), anyBoolean(), anyInt(), anyString(), anyInt());
        verify(writeTransaction, times(4)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), anyBoolean());
        verify(writeTransaction, times(2)).submit();
        verify(commitFuture, times(2)).get();
    }

    *//**
     * Rule 2: TCP Proto (True), TCP Port Minimum (True), TCP Port Max (False), IP Prefix (True)
     *//*
    @Test
    public void testProgramPortSecurityACLRule2() throws Exception {
        when(portSecurityRule.getSecurityRuleProtocol()).thenReturn("tcp");
        when(portSecurityRule.getSecurityRulePortMax()).thenReturn(null);
        when(portSecurityRule.getSecurityRulePortMin()).thenReturn(1);
        when(portSecurityRule.getSecurityRuleRemoteIpPrefix()).thenReturn(HOST_ADDRESS);

        egressAclServiceSpy.programPortSecurityACL(Long.valueOf(1554), "2", MAC_ADDRESS, 124, securityGroup);
        verify(egressAclServiceSpy, times(1)).egressACLDefaultTcpDrop(anyLong(), anyString(), anyString(), anyInt(), anyBoolean());
        verify(egressAclServiceSpy, times(1)).egressACLTcpPortWithPrefix(anyLong(), anyString(), anyString(), anyBoolean(), anyInt(), anyString(), anyInt());
        verify(writeTransaction, times(4)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), anyBoolean());
        verify(writeTransaction, times(2)).submit();
        verify(commitFuture, times(2)).get();
    }

    *//**
     * Rule 3: TCP Proto (True), TCP Port Minimum (False), TCP Port Max (False), IP Prefix (True)
     *//*
    @Test
    public void testProgramPortSecurityACLRule3() throws Exception {
        when(portSecurityRule.getSecurityRuleProtocol()).thenReturn("tcp");
        when(portSecurityRule.getSecurityRulePortMax()).thenReturn(null);
        when(portSecurityRule.getSecurityRulePortMin()).thenReturn(null);
        when(portSecurityRule.getSecurityRuleRemoteIpPrefix()).thenReturn(HOST_ADDRESS);

        egressAclServiceSpy.programPortSecurityACL(Long.valueOf(1554), "2", MAC_ADDRESS, 124, securityGroup);
        verify(egressAclServiceSpy, times(1)).egressACLDefaultTcpDrop(anyLong(), anyString(), anyString(), anyInt(), anyBoolean());
        verify(egressAclServiceSpy, times(1)).egressACLPermitAllProto(anyLong(), anyString(), anyString(), anyBoolean(), anyString(), anyInt());
        verify(writeTransaction, times(4)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), anyBoolean());
        verify(writeTransaction, times(2)).submit();
        verify(commitFuture, times(2)).get();
    }

    *//**
     * Rule 4: TCP Proto (False), TCP Port Minimum (False), TCP Port Max (False), IP Prefix (True)
     *//*
    @Test
    public void testProgramPortSecurityACLRule4() throws Exception {
        when(portSecurityRule.getSecurityRuleProtocol()).thenReturn(null);
        when(portSecurityRule.getSecurityRulePortMax()).thenReturn(null);
        when(portSecurityRule.getSecurityRulePortMin()).thenReturn(null);
        when(portSecurityRule.getSecurityRuleRemoteIpPrefix()).thenReturn(HOST_ADDRESS);

        egressAclServiceSpy.programPortSecurityACL(Long.valueOf(1554), "2", MAC_ADDRESS, 124, securityGroup);
        verify(egressAclServiceSpy, times(1)).egressACLDefaultTcpDrop(anyLong(), anyString(), anyString(), anyInt(), anyBoolean());
        verify(egressAclServiceSpy, times(1)).egressACLPermitAllProto(anyLong(), anyString(), anyString(), anyBoolean(), anyString(), anyInt());
        verify(writeTransaction, times(4)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), anyBoolean());
        verify(writeTransaction, times(2)).submit();
        verify(commitFuture, times(2)).get();
    }

    *//**
     * Rule 5: TCP Proto (True), TCP Port Minimum (True), TCP Port Max (True), IP Prefix (False)
     *//*
    @Test
    public void testProgramPortSecurityACLRule5() throws Exception {
        when(portSecurityRule.getSecurityRuleProtocol()).thenReturn("tcp");
        when(portSecurityRule.getSecurityRulePortMax()).thenReturn(1);
        when(portSecurityRule.getSecurityRulePortMin()).thenReturn(1);
        when(portSecurityRule.getSecurityRuleRemoteIpPrefix()).thenReturn(null);

        egressAclServiceSpy.programPortSecurityACL(Long.valueOf(1554), "2", MAC_ADDRESS, 124, securityGroup);
        verify(egressAclServiceSpy, times(1)).egressACLDefaultTcpDrop(anyLong(), anyString(), anyString(), anyInt(), anyBoolean());
        verify(egressAclServiceSpy, times(1)).egressACLTcpSyn(anyLong(), anyString(), anyString(), anyBoolean(), anyInt(), anyInt());
        verify(writeTransaction, times(4)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), anyBoolean());
        verify(writeTransaction, times(2)).submit();
        verify(commitFuture, times(2)).get();
    }

    *//**
     * Rule 6: TCP Proto (True), TCP Port Minimum (True), TCP Port Max (False), IP Prefix (False)
     *//*
    @Test
    public void testProgramPortSecurityACLRule6() throws Exception {
        when(portSecurityRule.getSecurityRuleProtocol()).thenReturn("tcp");
        when(portSecurityRule.getSecurityRulePortMax()).thenReturn(null);
        when(portSecurityRule.getSecurityRulePortMin()).thenReturn(1);
        when(portSecurityRule.getSecurityRuleRemoteIpPrefix()).thenReturn(null);

        egressAclServiceSpy.programPortSecurityACL(Long.valueOf(1554), "2", MAC_ADDRESS, 124, securityGroup);
        verify(egressAclServiceSpy, times(1)).egressACLDefaultTcpDrop(anyLong(), anyString(), anyString(), anyInt(), anyBoolean());
        verify(egressAclServiceSpy, times(1)).egressACLTcpSyn(anyLong(), anyString(), anyString(), anyBoolean(), anyInt(), anyInt());
        verify(writeTransaction, times(4)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), anyBoolean());
        verify(writeTransaction, times(2)).submit();
        verify(commitFuture, times(2)).get();
    }

    *//**
     * Rule 7: TCP Proto (True), TCP Port Minimum (False), TCP Port Max (False), IP Prefix (False or 0.0.0.0/0)
     *//*
    @Test
    public void testProgramPortSecurityACLRule7() throws Exception {
        when(portSecurityRule.getSecurityRuleProtocol()).thenReturn("tcp");
        when(portSecurityRule.getSecurityRulePortMax()).thenReturn(null);
        when(portSecurityRule.getSecurityRulePortMin()).thenReturn(null);
        when(portSecurityRule.getSecurityRuleRemoteIpPrefix()).thenReturn(null);

        egressAclServiceSpy.programPortSecurityACL(Long.valueOf(1554), "2", MAC_ADDRESS, 124, securityGroup);
        verify(egressAclServiceSpy, times(1)).egressAllowProto(anyLong(), anyString(), anyString(), anyBoolean(), anyString(), anyInt());
        verify(writeTransaction, times(2)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), anyBoolean());
        verify(writeTransaction, times(1)).submit();
        verify(commitFuture, times(1)).get();
    }
*/
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
     * Test method {@link EgressAclService#egressACLDefaultTcpDrop(Long, String, String, int, boolean)}
     */
    @Test
    public void testEgressACLDefaultTcpDrop() throws Exception {
        egressAclService.egressACLDefaultTcpDrop(123L, "2", MAC_ADDRESS, 1, true);
        verify(writeTransaction, times(2)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), anyBoolean());
        verify(writeTransaction, times(1)).submit();
        verify(commitFuture, times(1)).get();

        egressAclService.egressACLDefaultTcpDrop(123L, "2", MAC_ADDRESS, 1, false);
        verify(writeTransaction, times(1)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(writeTransaction, times(2)).submit();
        verify(commitFuture, times(2)).get(); // 1 + 1 above
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

        verify(writeTransaction, times(2)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), eq(true));
        verify(writeTransaction, times(1)).submit();
        verify(commitFuture, times(1)).get();
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
     *  Test TCP add with port no and CIDR selected.
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

        Assert.assertTrue(match.getLayer4Match() instanceof TcpMatch);
        TcpMatch layer4Match=(TcpMatch) match.getLayer4Match();
        Assert.assertEquals(20, layer4Match.getTcpDestinationPort().getValue().intValue());
        int port=portSecurityRule.getSecurityRulePortMin();
        Assert.assertEquals("Egress_TCP_" + SEGMENT_ID + "_" + MAC_ADDRESS +
                            "_" + port + "_0.0.0.0/24_Permit", flowBuilder.getFlowName());
    }

    /**
     *  Test TCP remove with port no and CIDR selected.
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

        Assert.assertTrue(match.getLayer4Match() instanceof TcpMatch);
        TcpMatch layer4Match = (TcpMatch) match.getLayer4Match();
        Assert.assertEquals(30, layer4Match.getTcpDestinationPort().getValue().intValue());
        int port=portSecurityRule.getSecurityRulePortMin();
        Assert.assertEquals("Egress_TCP_" + SEGMENT_ID + "_" + MAC_ADDRESS +
                            "_" + port + "_0.0.0.0/24_Permit", flowBuilder.getFlowName());
    }

    /**
     *  Test TCP add with port no and remote SG selected.
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
     *  Test TCP remove with port no and remote SG selected.
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
     *  Test TCP add with port range (All TCP) and CIDR selected.
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
    }

    /**
     *  Test TCP remove with port range (All TCP) and CIDR selected.
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

        Assert.assertTrue(match.getLayer4Match() instanceof TcpMatch);
        Assert.assertEquals("Egress_TCP_" + SEGMENT_ID + "_" + MAC_ADDRESS + "_" + PORT_RANGE_MIN + "_" +
                            PORT_RANGE_MAX + "_0.0.0.0/24_Permit", flowBuilder.getFlowName());
    }

    /**
     *  Test TCP add with port range (All TCP) and remote SG selected.
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
     *  Test TCP remove with port range (All TCP) and remote SG selected.
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
     *  Test UDP add with port no and CIDR selected.
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

        Assert.assertTrue(match.getLayer4Match() instanceof UdpMatch);
        UdpMatch layer4Match = (UdpMatch) match.getLayer4Match();
        Assert.assertEquals(50, layer4Match.getUdpDestinationPort().getValue().intValue());
        int port = portSecurityRule.getSecurityRulePortMin();
        Assert.assertEquals("Egress_UDP_" + SEGMENT_ID + "_" + MAC_ADDRESS +
                            "_" + port + "_0.0.0.0/24_Permit", flowBuilder.getFlowName());
    }

    /**
     *  Test UDP remove with port no and CIDR selected.
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

        Assert.assertTrue(match.getLayer4Match() instanceof UdpMatch);
        UdpMatch layer4Match = (UdpMatch) match.getLayer4Match();
        Assert.assertEquals(50, layer4Match.getUdpDestinationPort().getValue().intValue());
        int port = portSecurityRule.getSecurityRulePortMin();
        Assert.assertEquals("Egress_UDP_" + SEGMENT_ID + "_" + MAC_ADDRESS +
                            "_" + port + "_0.0.0.0/24_Permit", flowBuilder.getFlowName());
    }

    /**
     *  Test UDP add with port no and remote SG selected.
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
     *  Test UDP remove with port no and remote SG selected.
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
     *  Test UDP add with port (All UDP) and CIDR selected.
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

        Assert.assertTrue(match.getLayer4Match() instanceof UdpMatch);
        Assert.assertEquals("Egress_UDP_" + SEGMENT_ID + "_" + MAC_ADDRESS + "_" + PORT_RANGE_MIN + "_" +
                            PORT_RANGE_MAX + "_0.0.0.0/24_Permit", flowBuilder.getFlowName());
    }

    /**
     *  Test UDP remove with port (All UDP) and CIDR selected.
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

        Assert.assertTrue(match.getLayer4Match() instanceof UdpMatch);
        Assert.assertEquals("Egress_UDP_" + SEGMENT_ID + "_" + MAC_ADDRESS + "_" + PORT_RANGE_MIN + "_" +
                            PORT_RANGE_MAX + "_0.0.0.0/24_Permit", flowBuilder.getFlowName());
    }

    /**
     *  Test UDP add with port (All UDP) and remote SG selected.
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
     *  Test UDP remove with port (All UDP) and remote SG selected.
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
        Short type = portSecurityRule.getSecurityRulePortMin().shortValue();
        Short code = portSecurityRule.getSecurityRulePortMax().shortValue();
        Assert.assertEquals("Egress_ICMP_" + SEGMENT_ID + "_" + MAC_ADDRESS +
                            "_" + type + "_" + code + "_0.0.0.0/24_Permit", flowBuilder.getFlowName());
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
        Short type = portSecurityRule.getSecurityRulePortMin().shortValue();
        Short code = portSecurityRule.getSecurityRulePortMax().shortValue();
        Assert.assertEquals("Egress_ICMP_" + SEGMENT_ID + "_" + MAC_ADDRESS +
                            "_" + type + "_" + code + "_0.0.0.0/24_Permit", flowBuilder.getFlowName());
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
     *  Test IPv4 invalid ether type test case.
     */
    @Test
    public void testProgramPortSecurityACLRuleInvalidEther() throws Exception {
        when(portSecurityRule.getSecurityRuleEthertype()).thenReturn("IPV6");

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
     *  Test With isLastPortInBridge false isComputeNode false
     */
    @Test
    public void testProgramFixedSecurityACLAdd1() throws Exception {
        egressAclServiceSpy.programFixedSecurityGroup(Long.valueOf(1554), "2", MAC_ADDRESS, 1, neutronDestIpList, false, false, true);

        verify(writeTransaction, times(0)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), eq(true));
        verify(writeTransaction, times(0)).submit();
        verify(commitFuture, times(0)).get();
    }
    /**
     *  Test With isLastPortInBridge false isComputeNode false
     */
    @Test
    public void testProgramFixedSecurityACLRemove1() throws Exception {

        egressAclServiceSpy.programFixedSecurityGroup(Long.valueOf(1554), "2", MAC_ADDRESS, 1, neutronDestIpList, false, false, false);

        verify(writeTransaction, times(0)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(writeTransaction, times(0)).submit();
        verify(commitFuture, times(0)).get();
    }

    /**
     *  Test With isLastPortInBridge false isComputeNode true
     */
    @Test
    public void testProgramFixedSecurityACLAdd2() throws Exception {

        egressAclServiceSpy.programFixedSecurityGroup(Long.valueOf(1554), "2", MAC_ADDRESS, 1, neutronDestIpList, false, true, true);

        verify(writeTransaction, times(6)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), eq(true));
        verify(writeTransaction, times(3)).submit();
        verify(commitFuture, times(3)).get();
    }

    /**
     *  Test With isLastPortInBridge false isComputeNode true
     */
    @Test
    public void testProgramFixedSecurityACLRemove2() throws Exception {

        egressAclServiceSpy.programFixedSecurityGroup(Long.valueOf(1554), "2", MAC_ADDRESS, 1, neutronDestIpList, false, true, false);

        verify(writeTransaction, times(3)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(writeTransaction, times(3)).submit();
        verify(commitFuture, times(3)).get();
    }

    /**
     *  Test With isLastPortInBridge true isComputeNode false
     */
    @Test
    public void testProgramFixedSecurityACLAdd3() throws Exception {

        egressAclServiceSpy.programFixedSecurityGroup(Long.valueOf(1554), "2", MAC_ADDRESS, 1, neutronDestIpList, true, false, true);

        verify(writeTransaction, times(2)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), eq(true));
        verify(writeTransaction, times(1)).submit();
        verify(commitFuture, times(1)).get();
    }

    /**
     *  Test With isLastPortInBridge true isComputeNode false
     */
    @Test
    public void testProgramFixedSecurityACLRemove3() throws Exception {

        egressAclServiceSpy.programFixedSecurityGroup(Long.valueOf(1554), "2", MAC_ADDRESS, 1, neutronDestIpList, true, false, false);

        verify(writeTransaction, times(1)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(writeTransaction, times(1)).submit();
        verify(commitFuture, times(1)).get();
    }

    /**
     *  Test With isLastPortInBridge true isComputeNode true
     */
    @Test
    public void testProgramFixedSecurityACLAdd4() throws Exception {

        egressAclServiceSpy.programFixedSecurityGroup(Long.valueOf(1554), "2", MAC_ADDRESS, 1, neutronDestIpList, true, true, true);

        verify(writeTransaction, times(8)).put(any(LogicalDatastoreType.class),
                                               any(InstanceIdentifier.class), any(Node.class), eq(true));
        verify(writeTransaction, times(4)).submit();
        verify(commitFuture, times(4)).get();
    }

    /**
     *  Test With isLastPortInBridge true isComputeNode true
     */
    @Test
    public void testProgramFixedSecurityACLRemove4() throws Exception {

        egressAclServiceSpy.programFixedSecurityGroup(Long.valueOf(1554), "2", MAC_ADDRESS, 1, neutronDestIpList, true, true, false);

        verify(writeTransaction, times(4)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(writeTransaction, times(4)).submit();
        verify(commitFuture, times(4)).get();
    }

    /**
     * Test method {@link EgressAclService#egressACLTcpPortWithPrefix(Long, String, String, boolean, Integer, String, Integer)}
     */
    @Test
    public void testEgressACLTcpPortWithPrefix() throws Exception {
        egressAclService.egressACLTcpPortWithPrefix(123L, "2", MAC_ADDRESS, true, 1, HOST_ADDRESS, 1);
        verify(writeTransaction, times(2)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), anyBoolean());
        verify(writeTransaction, times(1)).submit();
        verify(commitFuture, times(1)).get();

        egressAclService.egressACLTcpPortWithPrefix(123L, "2", MAC_ADDRESS, false, 1, HOST_ADDRESS, 1);
        verify(writeTransaction, times(1)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(writeTransaction, times(2)).submit();
        verify(commitFuture, times(2)).get(); // 1 + 1 above
    }

    /**
     * Test method {@link EgressAclService#egressAllowProto(Long, String, String, boolean, String, Integer)}
     */
    @Test
    public void testEgressAllowProto() throws Exception {
        egressAclService.egressAllowProto(123L, "2", MAC_ADDRESS, true, HOST_ADDRESS, 1);
        verify(writeTransaction, times(2)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), anyBoolean());
        verify(writeTransaction, times(1)).submit();
        verify(commitFuture, times(1)).get();

        egressAclService.egressAllowProto(123L, "2", MAC_ADDRESS, false, HOST_ADDRESS, 1);
        verify(writeTransaction, times(1)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(writeTransaction, times(2)).submit();
        verify(commitFuture, times(2)).get(); // 1 + 1 above
    }

    /**
     * Test method {@link EgressAclService#egressACLPermitAllProto(Long, String, String, boolean, String, Integer)}
     */
    @Test
    public void testEgressACLPermitAllProto() throws Exception {
        egressAclService.egressACLPermitAllProto(123L, "2", MAC_ADDRESS, true, HOST_ADDRESS, 1);
        verify(writeTransaction, times(2)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), anyBoolean());
        verify(writeTransaction, times(1)).submit();
        verify(commitFuture, times(1)).get();

        egressAclService.egressACLPermitAllProto(123L, "2", MAC_ADDRESS, false, HOST_ADDRESS, 1);
        verify(writeTransaction, times(1)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(writeTransaction, times(2)).submit();
        verify(commitFuture, times(2)).get(); // 1 + 1 above
    }

    /**
     * Test method {@link EgressAclService#egressACLTcpSyn(Long, String, String, boolean, Integer, Integer)}
     */
    @Test
    public void testEgressACLTcpSyn() throws Exception {
        egressAclService.egressACLTcpSyn(123L, "2", MAC_ADDRESS, true, 1, 1);
        verify(writeTransaction, times(2)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), anyBoolean());
        verify(writeTransaction, times(1)).submit();
        verify(commitFuture, times(1)).get();

        egressAclService.egressACLTcpSyn(123L, "2", MAC_ADDRESS, false, 1, 1);
        verify(writeTransaction, times(1)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(writeTransaction, times(2)).submit();
        verify(commitFuture, times(2)).get(); // 1 + 1 above
    }
}
