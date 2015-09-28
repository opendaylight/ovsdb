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
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.internal.matchers.Equality;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.neutron.spi.NeutronSecurityGroup;
import org.opendaylight.neutron.spi.NeutronSecurityRule;
import org.opendaylight.neutron.spi.Neutron_IPs;
import org.opendaylight.ovsdb.openstack.netvirt.api.Constants;
import org.opendaylight.ovsdb.openstack.netvirt.api.SecurityServicesManager;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.PipelineOrchestrator;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.Service;
import org.opendaylight.ovsdb.utils.mdsal.openflow.MatchUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.util.concurrent.CheckedFuture;
/**
 * Unit test for {@link EgressAclService}
 */
@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("unchecked")
public class EgressAclServiceTest {

    @InjectMocks private EgressAclService egressAclService = new EgressAclService();
    @Spy private EgressAclService egressAclServiceSpy;
    @Spy private MatchUtils matchUtil;

    @Mock private DataBroker dataBroker;
    @Mock private PipelineOrchestrator orchestrator;

    @Mock private WriteTransaction writeTransaction;
    @Mock private CheckedFuture<Void, TransactionCommitFailedException> commitFuture;

    @Mock private NeutronSecurityGroup securityGroup;
    @Mock private NeutronSecurityRule portSecurityRule;

    @Mock private SecurityServicesManager securityServices;

    private Neutron_IPs neutron_ip_src;
    private Neutron_IPs neutron_ip_dest_1;
    private Neutron_IPs neutron_ip_dest_2;
    private List<Neutron_IPs> neutronSrcIpList = new ArrayList<Neutron_IPs>();
    private List<Neutron_IPs> neutronDestIpList = new ArrayList<Neutron_IPs>();
    private static final String HOST_ADDRESS = "127.0.0.1/32";
    private static final String MAC_ADDRESS = "87:1D:5E:02:40:B7";
    private static final String SRC_IP = "192.168.0.1";
    private static final String DEST_IP_1 = "192.169.0.1";
    private static final String DEST_IP_2 = "192.169.0.2";
    private static final String DEST_IP_1_WITH_MASK = "192.169.0.1/32";
    private static final String DEST_IP_2_WITH_MASK = "192.169.0.2/32";
    private static final String SECURITY_GROUP_UUID = "85cc3048-abc3-43cc-89b3-377341426ac5";

    @Before
    public void setUp() {
        egressAclServiceSpy = Mockito.spy(egressAclService);

        when(writeTransaction.submit()).thenReturn(commitFuture);

        when(dataBroker.newWriteOnlyTransaction()).thenReturn(writeTransaction);

        when(orchestrator.getNextServiceInPipeline(any(Service.class))).thenReturn(Service.ARP_RESPONDER);

        portSecurityRule = mock(NeutronSecurityRule.class);

        when(portSecurityRule.getSecurityRuleEthertype()).thenReturn("IPv4");
        when(portSecurityRule.getSecurityRuleDirection()).thenReturn("egress");

        List<NeutronSecurityRule> portSecurityList = new ArrayList<NeutronSecurityRule>();
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
        when(securityServices.getVmListForSecurityGroup(neutronSrcIpList, SECURITY_GROUP_UUID)).thenReturn(neutronDestIpList);
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
     * Test method {@link EgressAclService#egressACLDefaultTcpDrop(Long, String, String, int, boolean)}
     */
    @Test
    public void testEgressACLDefaultTcpDrop() throws Exception {
        egressAclService.egressACLDefaultTcpDrop(Long.valueOf(123), "2", MAC_ADDRESS, 1, true);
        verify(writeTransaction, times(2)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), anyBoolean());
        verify(writeTransaction, times(1)).submit();
        verify(commitFuture, times(1)).get();

        egressAclService.egressACLDefaultTcpDrop(Long.valueOf(123), "2", MAC_ADDRESS, 1, false);
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

        egressAclServiceSpy.programPortSecurityAcl(Long.valueOf(1554), "2", MAC_ADDRESS, 124, securityGroup,neutronSrcIpList,true);

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

        egressAclServiceSpy.programPortSecurityAcl(Long.valueOf(1554), "2", MAC_ADDRESS, 124, securityGroup,neutronSrcIpList,false);
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
        when(portSecurityRule.getSecurityRulePortMax()).thenReturn(50);
        when(portSecurityRule.getSecurityRulePortMin()).thenReturn(50);
        when(portSecurityRule.getSecurityRuleRemoteIpPrefix()).thenReturn("0.0.0.0/24");

        egressAclServiceSpy.programPortSecurityAcl(Long.valueOf(1554), "2", MAC_ADDRESS, 124, securityGroup,neutronSrcIpList,true);

        verify(writeTransaction, times(2)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), eq(true));
        verify(writeTransaction, times(1)).submit();
        verify(commitFuture, times(1)).get();
    }

    /**
     *  Test TCP remove with port no and CIDR selected.
     */
    @Test
    public void testProgramPortSecurityACLRuleRemoveTcp1() throws Exception {
        when(portSecurityRule.getSecurityRuleProtocol()).thenReturn("tcp");
        when(portSecurityRule.getSecurityRulePortMax()).thenReturn(50);
        when(portSecurityRule.getSecurityRulePortMin()).thenReturn(50);
        when(portSecurityRule.getSecurityRuleRemoteIpPrefix()).thenReturn("0.0.0.0/24");

        egressAclServiceSpy.programPortSecurityAcl(Long.valueOf(1554), "2", MAC_ADDRESS, 124, securityGroup,neutronSrcIpList,false);

        verify(writeTransaction, times(1)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(writeTransaction, times(1)).submit();
        verify(commitFuture, times(1)).get();
    }

    /**
     *  Test TCP add with port no and remote SG selected.
     */
    @Test
    public void testProgramPortSecurityACLRuleAddTcp2() throws Exception {
        when(portSecurityRule.getSecurityRuleProtocol()).thenReturn("tcp");
        when(portSecurityRule.getSecurityRulePortMax()).thenReturn(50);
        when(portSecurityRule.getSecurityRulePortMin()).thenReturn(50);
        when(portSecurityRule.getSecurityRuleRemoteIpPrefix()).thenReturn("0.0.0.0/24");
        when(portSecurityRule.getSecurityRemoteGroupID()).thenReturn("85cc3048-abc3-43cc-89b3-377341426ac5");

        egressAclServiceSpy.programPortSecurityAcl(Long.valueOf(1554), "2", MAC_ADDRESS, 124, securityGroup,neutronSrcIpList,true);

        verify(writeTransaction, times(4)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), eq(true));
        verify(writeTransaction, times(2)).submit();
        verify(commitFuture, times(2)).get();
    }

    /**
     *  Test TCP remove with port no and remote SG selected.
     */
    @Test
    public void testProgramPortSecurityACLRuleRemoveTcp2() throws Exception {
        when(portSecurityRule.getSecurityRuleProtocol()).thenReturn("tcp");
        when(portSecurityRule.getSecurityRulePortMax()).thenReturn(50);
        when(portSecurityRule.getSecurityRulePortMin()).thenReturn(50);
        when(portSecurityRule.getSecurityRuleRemoteIpPrefix()).thenReturn("0.0.0.0/24");
        when(portSecurityRule.getSecurityRemoteGroupID()).thenReturn("85cc3048-abc3-43cc-89b3-377341426ac5");

        egressAclServiceSpy.programPortSecurityAcl(Long.valueOf(1554), "2", MAC_ADDRESS, 124, securityGroup,neutronSrcIpList,false);

        verify(writeTransaction, times(2)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(writeTransaction, times(2)).submit();
        verify(commitFuture, times(2)).get();
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

        egressAclServiceSpy.programPortSecurityAcl(Long.valueOf(1554), "2", MAC_ADDRESS, 124, securityGroup,neutronSrcIpList,true);

        verify(writeTransaction, times(2)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), eq(true));
        verify(writeTransaction, times(1)).submit();
        verify(commitFuture, times(1)).get();
    }

    /**
     *  Test UDP add with port no and CIDR selected.
     */
    @Test
    public void testProgramPortSecurityACLRuleRemoveUdp1() throws Exception {
        when(portSecurityRule.getSecurityRuleProtocol()).thenReturn("udp");
        when(portSecurityRule.getSecurityRulePortMax()).thenReturn(50);
        when(portSecurityRule.getSecurityRulePortMin()).thenReturn(50);
        when(portSecurityRule.getSecurityRuleRemoteIpPrefix()).thenReturn("0.0.0.0/24");

        egressAclServiceSpy.programPortSecurityAcl(Long.valueOf(1554), "2", MAC_ADDRESS, 124, securityGroup,neutronSrcIpList,false);

        verify(writeTransaction, times(1)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(writeTransaction, times(1)).submit();
        verify(commitFuture, times(1)).get();
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

        egressAclServiceSpy.programPortSecurityAcl(Long.valueOf(1554), "2", MAC_ADDRESS, 124, securityGroup,neutronSrcIpList,true);

        verify(writeTransaction, times(4)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), eq(true));
        verify(writeTransaction, times(2)).submit();
        verify(commitFuture, times(2)).get();
    }

    /**
     *  Test UDP add with port no and remote SG selected.
     */
    @Test
    public void testProgramPortSecurityACLRuleRemoveUdp2() throws Exception {
        when(portSecurityRule.getSecurityRuleProtocol()).thenReturn("udp");
        when(portSecurityRule.getSecurityRulePortMax()).thenReturn(50);
        when(portSecurityRule.getSecurityRulePortMin()).thenReturn(50);
        when(portSecurityRule.getSecurityRuleRemoteIpPrefix()).thenReturn("0.0.0.0/24");
        when(portSecurityRule.getSecurityRemoteGroupID()).thenReturn("85cc3048-abc3-43cc-89b3-377341426ac5");

        egressAclServiceSpy.programPortSecurityAcl(Long.valueOf(1554), "2", MAC_ADDRESS, 124, securityGroup,neutronSrcIpList,false);

        verify(writeTransaction, times(2)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(writeTransaction, times(2)).submit();
        verify(commitFuture, times(2)).get();
    }

    /**
     *  Test ICMP add with code, type and CIDR selected.
     */
    @Test
    public void testProgramPortSecurityACLRuleAddIcmp1() throws Exception {
        when(portSecurityRule.getSecurityRuleProtocol()).thenReturn("icmp");
        when(portSecurityRule.getSecurityRulePortMax()).thenReturn(50);
        when(portSecurityRule.getSecurityRulePortMin()).thenReturn(50);
        when(portSecurityRule.getSecurityRuleRemoteIpPrefix()).thenReturn("0.0.0.0/24");

        egressAclServiceSpy.programPortSecurityAcl(Long.valueOf(1554), "2", MAC_ADDRESS, 124, securityGroup,neutronSrcIpList,true);

        verify(writeTransaction, times(2)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), eq(true));
        verify(writeTransaction, times(1)).submit();
        verify(commitFuture, times(1)).get();
    }

    /**
     *  Test ICMP remove with code, type and CIDR selected.
     */
    @Test
    public void testProgramPortSecurityACLRuleRemoveIcmp1() throws Exception {
        when(portSecurityRule.getSecurityRuleProtocol()).thenReturn("icmp");
        when(portSecurityRule.getSecurityRulePortMax()).thenReturn(50);
        when(portSecurityRule.getSecurityRulePortMin()).thenReturn(50);
        when(portSecurityRule.getSecurityRuleRemoteIpPrefix()).thenReturn("0.0.0.0/24");

        egressAclServiceSpy.programPortSecurityAcl(Long.valueOf(1554), "2", MAC_ADDRESS, 124, securityGroup,neutronSrcIpList,false);

        verify(writeTransaction, times(1)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(writeTransaction, times(1)).submit();
        verify(commitFuture, times(1)).get();
    }

    /**
     *  Test ICMP add with code, type and remote SG selected.
     */
    @Test
    public void testProgramPortSecurityACLRuleAddIcmp2() throws Exception {
        when(portSecurityRule.getSecurityRuleProtocol()).thenReturn("icmp");
        when(portSecurityRule.getSecurityRulePortMax()).thenReturn(50);
        when(portSecurityRule.getSecurityRulePortMin()).thenReturn(50);
        when(portSecurityRule.getSecurityRuleRemoteIpPrefix()).thenReturn("0.0.0.0/24");
        when(portSecurityRule.getSecurityRemoteGroupID()).thenReturn("85cc3048-abc3-43cc-89b3-377341426ac5");

        egressAclServiceSpy.programPortSecurityAcl(Long.valueOf(1554), "2", MAC_ADDRESS, 124, securityGroup,neutronSrcIpList,true);

        verify(writeTransaction, times(4)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), eq(true));
        verify(writeTransaction, times(2)).submit();
        verify(commitFuture, times(2)).get();
    }

    /**
     *  Test ICMP remove with code, type and remote SG selected.
     */
    @Test
    public void testProgramPortSecurityACLRuleRemoveIcmp2() throws Exception {
        when(portSecurityRule.getSecurityRuleProtocol()).thenReturn("icmp");
        when(portSecurityRule.getSecurityRulePortMax()).thenReturn(50);
        when(portSecurityRule.getSecurityRulePortMin()).thenReturn(50);
        when(portSecurityRule.getSecurityRuleRemoteIpPrefix()).thenReturn("0.0.0.0/24");
        when(portSecurityRule.getSecurityRemoteGroupID()).thenReturn("85cc3048-abc3-43cc-89b3-377341426ac5");

        egressAclServiceSpy.programPortSecurityAcl(Long.valueOf(1554), "2", MAC_ADDRESS, 124, securityGroup,neutronSrcIpList,false);

        verify(writeTransaction, times(2)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(writeTransaction, times(2)).submit();
        verify(commitFuture, times(2)).get();
    }

    /**
     *  Test IPv4 invalid ether type test case.
     */
    @Test
    public void testProgramPortSecurityACLRuleInvalidEther() throws Exception {
        when(portSecurityRule.getSecurityRuleEthertype()).thenReturn("IPV6");

        egressAclServiceSpy.programPortSecurityAcl(Long.valueOf(1554), "2", MAC_ADDRESS, 124, securityGroup,neutronSrcIpList,false);

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

        egressAclServiceSpy.programPortSecurityAcl(Long.valueOf(1554), "2", MAC_ADDRESS, 124, securityGroup,neutronSrcIpList,false);

        verify(writeTransaction, times(0)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(writeTransaction, times(0)).submit();
        verify(commitFuture, times(0)).get();
    }

    /**
     *  Test With isLastPortInBridge false isComputeNode false
     */
    @Test
    public void testProgramFixedSecurityACLAdd1() throws Exception {
        egressAclServiceSpy.programFixedSecurityAcl(Long.valueOf(1554), "2", MAC_ADDRESS, 1, neutronDestIpList, false, false, true);

        verify(writeTransaction, times(0)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), eq(true));
        verify(writeTransaction, times(0)).submit();
        verify(commitFuture, times(0)).get();
    }
    /**
     *  Test With isLastPortInBridge false isComputeNode false
     */
    @Test
    public void testProgramFixedSecurityACLRemove1() throws Exception {

        egressAclServiceSpy.programFixedSecurityAcl(Long.valueOf(1554), "2", MAC_ADDRESS, 1, neutronDestIpList, false, false, false);

        verify(writeTransaction, times(0)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(writeTransaction, times(0)).submit();
        verify(commitFuture, times(0)).get();
    }

    /**
     *  Test With isLastPortInBridge false isComputeNode true
     */
    @Test
    public void testProgramFixedSecurityACLAdd2() throws Exception {

        egressAclServiceSpy.programFixedSecurityAcl(Long.valueOf(1554), "2", MAC_ADDRESS, 1, neutronDestIpList, false, true, true);

        verify(writeTransaction, times(6)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), eq(true));
        verify(writeTransaction, times(3)).submit();
        verify(commitFuture, times(3)).get();
    }

    /**
     *  Test With isLastPortInBridge false isComputeNode true
     */
    @Test
    public void testProgramFixedSecurityACLRemove2() throws Exception {

        egressAclServiceSpy.programFixedSecurityAcl(Long.valueOf(1554), "2", MAC_ADDRESS, 1, neutronDestIpList, false, true, false);

        verify(writeTransaction, times(3)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(writeTransaction, times(3)).submit();
        verify(commitFuture, times(3)).get();
    }

    /**
     *  Test With isLastPortInBridge true isComputeNode false
     */
    @Test
    public void testProgramFixedSecurityACLAdd3() throws Exception {

        egressAclServiceSpy.programFixedSecurityAcl(Long.valueOf(1554), "2", MAC_ADDRESS, 1, neutronDestIpList, true, false, true);

        verify(writeTransaction, times(2)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), eq(true));
        verify(writeTransaction, times(1)).submit();
        verify(commitFuture, times(1)).get();
    }

    /**
     *  Test With isLastPortInBridge true isComputeNode false
     */
    @Test
    public void testProgramFixedSecurityACLRemove3() throws Exception {

        egressAclServiceSpy.programFixedSecurityAcl(Long.valueOf(1554), "2", MAC_ADDRESS, 1, neutronDestIpList, true, false, false);

        verify(writeTransaction, times(1)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(writeTransaction, times(1)).submit();
        verify(commitFuture, times(1)).get();
    }

    /**
     *  Test With isLastPortInBridge true isComputeNode true
     */
    @Test
    public void testProgramFixedSecurityACLAdd4() throws Exception {

        egressAclServiceSpy.programFixedSecurityAcl(Long.valueOf(1554), "2", MAC_ADDRESS, 1, neutronDestIpList, true, true, true);

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

        egressAclServiceSpy.programFixedSecurityAcl(Long.valueOf(1554), "2", MAC_ADDRESS, 1, neutronDestIpList, true, true, false);

        verify(writeTransaction, times(4)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(writeTransaction, times(4)).submit();
        verify(commitFuture, times(4)).get();
    }

    /**
     * Test method {@link EgressAclService#egressACLTcpPortWithPrefix(Long, String, String, boolean, Integer, String, Integer)}
     */
    @Test
    public void testEgressACLTcpPortWithPrefix() throws Exception {
        egressAclService.egressACLTcpPortWithPrefix(Long.valueOf(123), "2", MAC_ADDRESS, true, 1, HOST_ADDRESS, 1);
        verify(writeTransaction, times(2)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), anyBoolean());
        verify(writeTransaction, times(1)).submit();
        verify(commitFuture, times(1)).get();

        egressAclService.egressACLTcpPortWithPrefix(Long.valueOf(123), "2", MAC_ADDRESS, false, 1, HOST_ADDRESS, 1);
        verify(writeTransaction, times(1)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(writeTransaction, times(2)).submit();
        verify(commitFuture, times(2)).get(); // 1 + 1 above
    }

    /**
     * Test method {@link EgressAclService#egressAllowProto(Long, String, String, boolean, String, Integer)}
     */
    @Test
    public void testEgressAllowProto() throws Exception {
        egressAclService.egressAllowProto(Long.valueOf(123), "2", MAC_ADDRESS, true, HOST_ADDRESS, 1);
        verify(writeTransaction, times(2)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), anyBoolean());
        verify(writeTransaction, times(1)).submit();
        verify(commitFuture, times(1)).get();

        egressAclService.egressAllowProto(Long.valueOf(123), "2", MAC_ADDRESS, false, HOST_ADDRESS, 1);
        verify(writeTransaction, times(1)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(writeTransaction, times(2)).submit();
        verify(commitFuture, times(2)).get(); // 1 + 1 above
    }

    /**
     * Test method {@link EgressAclService#egressACLPermitAllProto(Long, String, String, boolean, String, Integer)}
     */
    @Test
    public void testEgressACLPermitAllProto() throws Exception {
        egressAclService.egressACLPermitAllProto(Long.valueOf(123), "2", MAC_ADDRESS, true, HOST_ADDRESS, 1);
        verify(writeTransaction, times(2)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), anyBoolean());
        verify(writeTransaction, times(1)).submit();
        verify(commitFuture, times(1)).get();

        egressAclService.egressACLPermitAllProto(Long.valueOf(123), "2", MAC_ADDRESS, false, HOST_ADDRESS, 1);
        verify(writeTransaction, times(1)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(writeTransaction, times(2)).submit();
        verify(commitFuture, times(2)).get(); // 1 + 1 above
    }

    /**
     * Test method {@link EgressAclService#egressACLTcpSyn(Long, String, String, boolean, Integer, Integer)}
     */
    @Test
    public void testEgressACLTcpSyn() throws Exception {
        egressAclService.egressACLTcpSyn(Long.valueOf(123), "2", MAC_ADDRESS, true, 1, 1);
        verify(writeTransaction, times(2)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), anyBoolean());
        verify(writeTransaction, times(1)).submit();
        verify(commitFuture, times(1)).get();

        egressAclService.egressACLTcpSyn(Long.valueOf(123), "2", MAC_ADDRESS, false, 1, 1);
        verify(writeTransaction, times(1)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(writeTransaction, times(2)).submit();
        verify(commitFuture, times(2)).get(); // 1 + 1 above
    }
}
