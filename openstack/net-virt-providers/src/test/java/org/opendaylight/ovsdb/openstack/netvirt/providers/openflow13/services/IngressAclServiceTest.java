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
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.neutron.spi.NeutronSecurityGroup;
import org.opendaylight.neutron.spi.NeutronSecurityRule;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.PipelineOrchestrator;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.Service;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.util.concurrent.CheckedFuture;

/**
 * Unit test fort {@link IngressAclService}
 */
@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("unchecked")
public class IngressAclServiceTest {

    @InjectMocks private IngressAclService ingressAclService = new IngressAclService();
    @Spy private IngressAclService ingressAclServiceSpy;

    @Mock private DataBroker dataBroker;
    @Mock private PipelineOrchestrator orchestrator;

    @Mock private WriteTransaction writeTransaction;
    @Mock private CheckedFuture<Void, TransactionCommitFailedException> commitFuture;

    @Mock private NeutronSecurityGroup securityGroup;
    @Mock private NeutronSecurityRule portSecurityRule;

    private static final String SEGMENTATION_ID = "2";
    private static final int PRIORITY = 1;
    private static final String HOST_ADDRESS = "127.0.0.1/32";
    private static final String MAC_ADDRESS = "87:1D:5E:02:40:B8";

    @Before
    public void setUp() {
        ingressAclServiceSpy = Mockito.spy(ingressAclService);

        when(writeTransaction.submit()).thenReturn(commitFuture);

        when(dataBroker.newWriteOnlyTransaction()).thenReturn(writeTransaction);

        when(orchestrator.getNextServiceInPipeline(any(Service.class))).thenReturn(Service.ARP_RESPONDER);

        portSecurityRule = mock(NeutronSecurityRule.class);
        when(portSecurityRule.getSecurityRuleEthertype()).thenReturn("IPv4");
        when(portSecurityRule.getSecurityRuleDirection()).thenReturn("ingress");

        List<NeutronSecurityRule> portSecurityList = new ArrayList<NeutronSecurityRule>();
        portSecurityList.add(portSecurityRule);

        when(securityGroup.getSecurityRules()).thenReturn(portSecurityList);
    }

    /**
     * Rule 1: TCP Proto (True), TCP Port Minimum (True), TCP Port Max (True), IP Prefix (True)
     */
    @Test
    public void testProgramPortSecurityACLRule1() throws Exception {
        when(portSecurityRule.getSecurityRuleProtocol()).thenReturn("tcp");
        when(portSecurityRule.getSecurityRulePortMax()).thenReturn(1);
        when(portSecurityRule.getSecurityRulePortMin()).thenReturn(1);
        when(portSecurityRule.getSecurityRuleRemoteIpPrefix()).thenReturn(HOST_ADDRESS);

        ingressAclServiceSpy.programPortSecurityACL(Long.valueOf(1554), SEGMENTATION_ID, MAC_ADDRESS, 124, securityGroup);
        verify(ingressAclServiceSpy, times(1)).ingressACLDefaultTcpDrop(anyLong(), anyString(), anyString(), anyInt(), anyBoolean());
        verify(ingressAclServiceSpy, times(1)).ingressACLTcpPortWithPrefix(anyLong(), anyString(), anyString(), anyBoolean(), anyInt(), anyString(), anyInt());
        verify(writeTransaction, times(4)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), anyBoolean());
        verify(writeTransaction, times(2)).submit();
        verify(commitFuture, times(2)).get();
    }


    /**
     * Rule 2: TCP Proto (True), TCP Port Minimum (True), TCP Port Max (False), IP Prefix (True)
     */
    @Test
    public void testProgramPortSecurityACLRule2() throws Exception {
        when(portSecurityRule.getSecurityRuleProtocol()).thenReturn("tcp");
        when(portSecurityRule.getSecurityRulePortMax()).thenReturn(null);
        when(portSecurityRule.getSecurityRulePortMin()).thenReturn(1);
        when(portSecurityRule.getSecurityRuleRemoteIpPrefix()).thenReturn(HOST_ADDRESS);

        ingressAclServiceSpy.programPortSecurityACL(Long.valueOf(1554), SEGMENTATION_ID, MAC_ADDRESS, 124, securityGroup);
        verify(ingressAclServiceSpy, times(1)).ingressACLDefaultTcpDrop(anyLong(), anyString(), anyString(), anyInt(), anyBoolean());
        verify(ingressAclServiceSpy, times(1)).ingressACLTcpPortWithPrefix(anyLong(), anyString(), anyString(), anyBoolean(), anyInt(), anyString(), anyInt());
        verify(writeTransaction, times(4)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), anyBoolean());
        verify(writeTransaction, times(2)).submit();
        verify(commitFuture, times(2)).get();
    }

    /**
     * Rule 3: TCP Proto (True), TCP Port Minimum (False), TCP Port Max (False), IP Prefix (True)
     */
    @Test
    public void testProgramPortSecurityACLRule3() throws Exception {
        when(portSecurityRule.getSecurityRuleProtocol()).thenReturn("tcp");
        when(portSecurityRule.getSecurityRulePortMax()).thenReturn(null);
        when(portSecurityRule.getSecurityRulePortMin()).thenReturn(null);
        when(portSecurityRule.getSecurityRuleRemoteIpPrefix()).thenReturn(HOST_ADDRESS);

        ingressAclServiceSpy.programPortSecurityACL(Long.valueOf(1554), SEGMENTATION_ID, MAC_ADDRESS, 124, securityGroup);
        verify(ingressAclServiceSpy, times(1)).ingressACLDefaultTcpDrop(anyLong(), anyString(), anyString(), anyInt(), anyBoolean());
        verify(ingressAclServiceSpy, times(1)).ingressACLPermitAllProto(anyLong(), anyString(), anyString(), anyBoolean(), anyString(), anyInt());
        verify(writeTransaction, times(4)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), anyBoolean());
        verify(writeTransaction, times(2)).submit();
        verify(commitFuture, times(2)).get();
    }

    /**
     * Rule 4: TCP Proto (False), TCP Port Minimum (False), TCP Port Max (False), IP Prefix (True)
     */
    @Test
    public void testProgramPortSecurityACLRule4() throws Exception {
        when(portSecurityRule.getSecurityRuleProtocol()).thenReturn(null);
        when(portSecurityRule.getSecurityRulePortMax()).thenReturn(null);
        when(portSecurityRule.getSecurityRulePortMin()).thenReturn(null);
        when(portSecurityRule.getSecurityRuleRemoteIpPrefix()).thenReturn(HOST_ADDRESS);

        ingressAclServiceSpy.programPortSecurityACL(Long.valueOf(1554), SEGMENTATION_ID, MAC_ADDRESS, 124, securityGroup);
        verify(ingressAclServiceSpy, times(1)).ingressACLDefaultTcpDrop(anyLong(), anyString(), anyString(), anyInt(), anyBoolean());
        verify(ingressAclServiceSpy, times(1)).ingressACLPermitAllProto(anyLong(), anyString(), anyString(), anyBoolean(), anyString(), anyInt());
        verify(writeTransaction, times(4)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), anyBoolean());
        verify(writeTransaction, times(2)).submit();
        verify(commitFuture, times(2)).get();
    }

    /**
     * Rule 5: TCP Proto (True), TCP Port Minimum (True), TCP Port Max (True), IP Prefix (False)
     */
    @Test
    public void testProgramPortSecurityACLRule5() throws Exception {
        when(portSecurityRule.getSecurityRuleProtocol()).thenReturn("tcp");
        when(portSecurityRule.getSecurityRulePortMax()).thenReturn(1);
        when(portSecurityRule.getSecurityRulePortMin()).thenReturn(1);
        when(portSecurityRule.getSecurityRuleRemoteIpPrefix()).thenReturn(null);

        ingressAclServiceSpy.programPortSecurityACL(Long.valueOf(1554), SEGMENTATION_ID, MAC_ADDRESS, 124, securityGroup);
        verify(ingressAclServiceSpy, times(1)).ingressACLDefaultTcpDrop(anyLong(), anyString(), anyString(), anyInt(), anyBoolean());
        verify(ingressAclServiceSpy, times(1)).ingressACLTcpSyn(anyLong(), anyString(), anyString(), anyBoolean(), anyInt(), anyInt());
        verify(writeTransaction, times(4)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), anyBoolean());
        verify(writeTransaction, times(2)).submit();
        verify(commitFuture, times(2)).get();
    }

    /**
     * Rule 6: TCP Proto (True), TCP Port Minimum (True), TCP Port Max (False), IP Prefix (False)
     */
    @Test
    public void testProgramPortSecurityACLRule6() throws Exception {
        when(portSecurityRule.getSecurityRuleProtocol()).thenReturn("tcp");
        when(portSecurityRule.getSecurityRulePortMax()).thenReturn(null);
        when(portSecurityRule.getSecurityRulePortMin()).thenReturn(1);
        when(portSecurityRule.getSecurityRuleRemoteIpPrefix()).thenReturn(null);

        ingressAclServiceSpy.programPortSecurityACL(Long.valueOf(1554), SEGMENTATION_ID, MAC_ADDRESS, 124, securityGroup);
        verify(ingressAclServiceSpy, times(1)).ingressACLDefaultTcpDrop(anyLong(), anyString(), anyString(), anyInt(), anyBoolean());
        verify(ingressAclServiceSpy, times(1)).ingressACLTcpSyn(anyLong(), anyString(), anyString(), anyBoolean(), anyInt(), anyInt());
        verify(writeTransaction, times(4)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), anyBoolean());
        verify(writeTransaction, times(2)).submit();
        verify(commitFuture, times(2)).get();
    }

    /**
     * Rule 7: TCP Proto (True), TCP Port Minimum (False), TCP Port Max (False), IP Prefix (False or 0.0.0.0/0)
     */
    @Test
    public void testProgramPortSecurityACLRule7() throws Exception {
        when(portSecurityRule.getSecurityRuleProtocol()).thenReturn("tcp");
        when(portSecurityRule.getSecurityRulePortMax()).thenReturn(null);
        when(portSecurityRule.getSecurityRulePortMin()).thenReturn(null);
        when(portSecurityRule.getSecurityRuleRemoteIpPrefix()).thenReturn(null);

        ingressAclServiceSpy.programPortSecurityACL(Long.valueOf(1554), SEGMENTATION_ID, MAC_ADDRESS, 124, securityGroup);
        verify(ingressAclServiceSpy, times(1)).handleIngressAllowProto(anyLong(), anyString(), anyString(), anyBoolean(), anyString(), anyInt());
        verify(writeTransaction, times(2)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), anyBoolean());
        verify(writeTransaction, times(1)).submit();
        verify(commitFuture, times(1)).get();
    }

    /**
     * Test method {@link IgressAclService#egressACLDefaultTcpDrop(Long, String, String, int, boolean)}
     */
    @Test
    public void testIgressACLDefaultTcpDrop() throws Exception {
        ingressAclService.ingressACLDefaultTcpDrop(Long.valueOf(123), SEGMENTATION_ID, MAC_ADDRESS, PRIORITY, true);
        verify(writeTransaction, times(2)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), anyBoolean());
        verify(writeTransaction, times(1)).submit();
        verify(commitFuture, times(1)).get();

        ingressAclService.ingressACLDefaultTcpDrop(Long.valueOf(123), SEGMENTATION_ID, MAC_ADDRESS, PRIORITY, false);
        verify(writeTransaction, times(1)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(writeTransaction, times(2)).submit();
        verify(commitFuture, times(2)).get(); // 1 + 1 above
    }

    /**
     * Test method {@link IgressAclService#ingressACLTcpPortWithPrefix(Long, String, String, boolean, Integer, String, Integer)}
     */
    @Test
    public void testIngressACLTcpPortWithPrefix() throws Exception {
        ingressAclService.ingressACLTcpPortWithPrefix(Long.valueOf(123), SEGMENTATION_ID, MAC_ADDRESS, true, 1, HOST_ADDRESS, PRIORITY);
        verify(writeTransaction, times(2)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), anyBoolean());
        verify(writeTransaction, times(1)).submit();
        verify(commitFuture, times(1)).get();

        ingressAclService.ingressACLTcpPortWithPrefix(Long.valueOf(123), SEGMENTATION_ID, MAC_ADDRESS, false, 1, HOST_ADDRESS, PRIORITY);
        verify(writeTransaction, times(1)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(writeTransaction, times(2)).submit();
        verify(commitFuture, times(2)).get(); // 1 + 1 above
    }

    /**
     * Test method {@link IgressAclService#handleIngressAllowProto(Long, String, String, boolean, String, Integer)}
     */
    @Test
    public void testIngressAllowProto() throws Exception {
        ingressAclService.handleIngressAllowProto(Long.valueOf(123), SEGMENTATION_ID, MAC_ADDRESS, true, HOST_ADDRESS, PRIORITY);
        verify(writeTransaction, times(2)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), anyBoolean());
        verify(writeTransaction, times(1)).submit();
        verify(commitFuture, times(1)).get();

        ingressAclService.handleIngressAllowProto(Long.valueOf(123), SEGMENTATION_ID, MAC_ADDRESS, false, HOST_ADDRESS, PRIORITY);
        verify(writeTransaction, times(1)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(writeTransaction, times(2)).submit();
        verify(commitFuture, times(2)).get(); // 1 + 1 above
    }

    /**
     * Test method {@link IgressAclService#ingressACLPermitAllProto(Long, String, String, boolean, String, Integer)}
     */
    @Test
    public void testIngressACLPermitAllProto() throws Exception {
        ingressAclService.ingressACLPermitAllProto(Long.valueOf(123), SEGMENTATION_ID, MAC_ADDRESS, true, HOST_ADDRESS, PRIORITY);
        verify(writeTransaction, times(2)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), anyBoolean());
        verify(writeTransaction, times(1)).submit();
        verify(commitFuture, times(1)).get();

        ingressAclService.ingressACLPermitAllProto(Long.valueOf(123), SEGMENTATION_ID, MAC_ADDRESS, false, HOST_ADDRESS, PRIORITY);
        verify(writeTransaction, times(1)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(writeTransaction, times(2)).submit();
        verify(commitFuture, times(2)).get(); // 1 + 1 above
    }

    /**
     * Test method {@link IgressAclService#ingressACLTcpSyn(Long, String, String, boolean, Integer, Integer)}
     */
    @Test
    public void testIngressACLTcpSyn() throws Exception {
        ingressAclService.ingressACLTcpSyn(Long.valueOf(123), SEGMENTATION_ID, MAC_ADDRESS, true, 1, PRIORITY);
        verify(writeTransaction, times(2)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), anyBoolean());
        verify(writeTransaction, times(1)).submit();
        verify(commitFuture, times(1)).get();

        ingressAclService.ingressACLTcpSyn(Long.valueOf(123), SEGMENTATION_ID, MAC_ADDRESS, false, 1, PRIORITY);
        verify(writeTransaction, times(1)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(writeTransaction, times(2)).submit();
        verify(commitFuture, times(2)).get(); // 1 + 1 above
    }
}
