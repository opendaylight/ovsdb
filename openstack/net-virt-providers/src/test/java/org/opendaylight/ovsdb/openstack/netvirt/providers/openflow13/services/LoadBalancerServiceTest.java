/*
 * Copyright (c) 2015 Inocybe and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.services;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.ovsdb.openstack.netvirt.NetworkHandler;
import org.opendaylight.ovsdb.openstack.netvirt.api.Action;
import org.opendaylight.ovsdb.openstack.netvirt.api.LoadBalancerConfiguration;
import org.opendaylight.ovsdb.openstack.netvirt.api.LoadBalancerConfiguration.LoadBalancerPoolMember;
import org.opendaylight.ovsdb.openstack.netvirt.api.Status;
import org.opendaylight.ovsdb.openstack.netvirt.api.StatusCode;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.PipelineOrchestrator;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.Service;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.util.concurrent.CheckedFuture;

/**
 * Unit test fort {@link LoadBalancerService}
 */
@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("unchecked")
public class LoadBalancerServiceTest {

    @InjectMocks private LoadBalancerService loadBalancerService = new LoadBalancerService(Service.ARP_RESPONDER);

    @Mock private DataBroker dataBroker;
    @Mock private PipelineOrchestrator orchestrator;

    @Mock private WriteTransaction writeTransaction;
    @Mock private CheckedFuture<Void, TransactionCommitFailedException> commitFuture;

    @Mock private LoadBalancerConfiguration lbConfig;
    @Mock private LoadBalancerPoolMember member;
    @Mock private Node node;

    private static final String SEGMENTATION_ID = "2";
    private static final String HOST_ADDRESS = "127.0.0.1";
    private static final String MAC_ADDRESS = "87:1D:5E:02:40:B7";

    @Before
    public void setUp() throws Exception {
        when(writeTransaction.submit()).thenReturn(commitFuture);

        when(dataBroker.newWriteOnlyTransaction()).thenReturn(writeTransaction);

        when(orchestrator.getNextServiceInPipeline(any(Service.class))).thenReturn(Service.ARP_RESPONDER);

        Map<String, LoadBalancerPoolMember> members = new HashMap<String, LoadBalancerPoolMember>();
        members.put("key", member);

        when(lbConfig.isValid()).thenReturn(true);
        when(lbConfig.getVip()).thenReturn(HOST_ADDRESS);
        when(lbConfig.getMembers()).thenReturn(members);
        when(lbConfig.getProviderNetworkType()).thenReturn(NetworkHandler.NETWORK_TYPE_VXLAN, NetworkHandler.NETWORK_TYPE_VLAN);
        when(lbConfig.getProviderSegmentationId()).thenReturn(SEGMENTATION_ID);

        when(member.getIP()).thenReturn(HOST_ADDRESS);
        when(member.getIndex()).thenReturn(1);
        when(member.getMAC()).thenReturn(MAC_ADDRESS);

        NodeId nodeId = mock(NodeId.class);
        when(nodeId.getValue()).thenReturn("id");

        when(node.getNodeId()).thenReturn(nodeId);
    }
    /**
     * Test method {@link LoadBalancerService#programLoadBalancerPoolMemberRules(Node, LoadBalancerConfiguration, LoadBalancerPoolMember, Action)}
     */
    @Test
    public void testprogramLoadBalancerPoolMemberRules() throws Exception {
        assertEquals("Error, did not return the expected StatusCode", new Status(StatusCode.BADREQUEST), loadBalancerService.programLoadBalancerPoolMemberRules(node, null, null, Action.ADD));

        assertEquals("Error, did not return the expected StatusCode", new Status(StatusCode.NOTIMPLEMENTED), loadBalancerService.programLoadBalancerPoolMemberRules(node, lbConfig, member, Action.DELETE));
        verify(writeTransaction, times(2)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), anyBoolean());
        verify(writeTransaction, times(1)).submit();
        verify(commitFuture, times(1)).get();

        assertEquals("Error, did not return the expected StatusCode", new Status(StatusCode.SUCCESS), loadBalancerService.programLoadBalancerPoolMemberRules(node, lbConfig, member, Action.ADD));
        verify(writeTransaction, times(8)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), anyBoolean());
        verify(writeTransaction, times(4)).submit();
        verify(commitFuture, times(4)).get();
    }

    /**
     * Test method {@link LoadBalancerService#programLoadBalancerRules(Node, LoadBalancerConfiguration, Action)}
     */
    @Test
    public void testProgramLoadBalancerRules() throws Exception {
        assertEquals("Error, did not return the expected StatusCode", new Status(StatusCode.BADREQUEST), loadBalancerService.programLoadBalancerRules(node, null, Action.ADD));

        assertEquals("Error, did not return the expected StatusCode", new Status(StatusCode.NOTIMPLEMENTED), loadBalancerService.programLoadBalancerRules(node, lbConfig, Action.UPDATE));

        assertEquals("Error, did not return the expected StatusCode", new Status(StatusCode.SUCCESS), loadBalancerService.programLoadBalancerRules(node, lbConfig, Action.ADD));
        verify(writeTransaction, times(6)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), anyBoolean());
        verify(writeTransaction, times(3)).submit();
        verify(commitFuture, times(3)).get();

        assertEquals("Error, did not return the expected StatusCode", new Status(StatusCode.SUCCESS), loadBalancerService.programLoadBalancerRules(node, lbConfig, Action.DELETE));
        verify(writeTransaction, times(3)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(writeTransaction, times(6)).submit();
        verify(commitFuture, times(6)).get(); // 3 + 3 before
    }
}
