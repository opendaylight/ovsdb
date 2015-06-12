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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.PipelineOrchestrator;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.Service;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

/**
 * Unit test fort {@link L2ForwardingService}
 */
@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("unchecked")
public class L2FowardingServiceTest {

    @InjectMocks private L2ForwardingService l2ForwardingService = new L2ForwardingService(Service.ARP_RESPONDER);

    @Mock private DataBroker dataBroker;
    @Mock private PipelineOrchestrator orchestrator;

    @Mock private WriteTransaction writeTransaction;
    @Mock private ReadOnlyTransaction readOnlyTransaction;
    @Mock private CheckedFuture<Void, TransactionCommitFailedException> commitFuture;

    private static final String SEGMENTATION_ID = "2";
    private static final String MAC_ADDRESS = "87:1D:5E:02:40:B8";
    private static final Long DPID = Long.valueOf(122);
    private static final Long LOCAL_PORT = Long.valueOf(451);
    private static final Long ETH_PORT = Long.valueOf(564);
    private static final Long OF_PORT_OUT = Long.valueOf(5698);

    @Before
    public void setUp() throws Exception {
        when(writeTransaction.submit()).thenReturn(commitFuture);

        when(dataBroker.newWriteOnlyTransaction()).thenReturn(writeTransaction);
        when(dataBroker.newReadOnlyTransaction()).thenReturn(readOnlyTransaction);

        CheckedFuture future = mock(CheckedFuture.class);
        when(readOnlyTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(future);

        Optional<Flow> data = mock(Optional.class);
        when(future.get()).thenReturn(data);

        //when(mdsalConsumer.getDataBroker()).thenReturn(dataBroker);

        when(orchestrator.getNextServiceInPipeline(any(Service.class))).thenReturn(Service.ARP_RESPONDER);
    }

    /**
     * Test method {@link L2ForwardingService#programLocalUcastOut(Long, String, Long, String, boolean)}
     */
    @Test
    public void testProgramLoacalUcastOut() throws Exception {
        l2ForwardingService.programLocalUcastOut(DPID, SEGMENTATION_ID, LOCAL_PORT, MAC_ADDRESS, true);
        verify(writeTransaction, times(2)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), anyBoolean());
        verify(writeTransaction, times(1)).submit();
        verify(commitFuture, times(1)).get();

        l2ForwardingService.programLocalUcastOut(DPID, SEGMENTATION_ID, LOCAL_PORT, MAC_ADDRESS, false);
        verify(writeTransaction, times(1)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(writeTransaction, times(2)).submit();
        verify(commitFuture, times(2)).get(); // 1 + 1 above
    }

    /**
     * Test method {@link L2ForwardingService#programLocalVlanUcastOut(Long, String, Long, String, boolean)}
     */
    @Test
    public void testProgramLocalVlanUcastOut() throws Exception {
        l2ForwardingService.programLocalVlanUcastOut(DPID, SEGMENTATION_ID, Long.valueOf(124), MAC_ADDRESS, true);
        verify(writeTransaction, times(2)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), anyBoolean());
        verify(writeTransaction, times(1)).submit();
        verify(commitFuture, times(1)).get();

        l2ForwardingService.programLocalVlanUcastOut(DPID, SEGMENTATION_ID, LOCAL_PORT, MAC_ADDRESS, false);
        verify(writeTransaction, times(1)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(writeTransaction, times(2)).submit();
        verify(commitFuture, times(2)).get(); // 1 + 1 above
    }

    /**
     * Test method {@link L2ForwardingService#programLocalBcastOut(Long, String, Long, boolean)}
     */
    @Test
    public void testProgramLocalBcastOut() throws Exception {
        l2ForwardingService.programLocalBcastOut(DPID, SEGMENTATION_ID, LOCAL_PORT, true);
        verify(writeTransaction, times(2)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), anyBoolean());
        verify(writeTransaction, times(1)).submit();
        verify(commitFuture, times(1)).get();

        l2ForwardingService.programLocalBcastOut(DPID, SEGMENTATION_ID, LOCAL_PORT, false);
        verify(writeTransaction, times(1)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(writeTransaction, times(2)).submit();
        verify(commitFuture, times(2)).get(); // 1 + 1 above
    }

    /**--------------------------------- TODO go deeper in test
     * Test method {@link L2ForwardingService#programLocalVlanBcastOut(Long, String, Long, Long, boolean)}
     */
    @Test
    public void testProgramLocalVlanBcastOut() throws Exception {
        l2ForwardingService.programLocalVlanBcastOut(DPID, SEGMENTATION_ID, LOCAL_PORT, ETH_PORT, true);
        verify(writeTransaction, times(2)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), anyBoolean());
        verify(writeTransaction, times(1)).submit();
        verify(commitFuture, times(1)).get();

        l2ForwardingService.programLocalVlanBcastOut(DPID, SEGMENTATION_ID, LOCAL_PORT, ETH_PORT, false);
        verify(writeTransaction, times(1)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(writeTransaction, times(2)).submit();
        verify(commitFuture, times(2)).get(); // 1 + 1 above
    }

    /**
     * Test method {@link L2ForwardingService#programLocalTableMiss(Long, String, boolean)}
     */
    @Test
    public void testProgramLocalTableMiss() throws Exception {
        l2ForwardingService.programLocalTableMiss(DPID, SEGMENTATION_ID, true);
        verify(writeTransaction, times(2)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), anyBoolean());
        verify(writeTransaction, times(1)).submit();
        verify(commitFuture, times(1)).get();

        l2ForwardingService.programLocalTableMiss(DPID, SEGMENTATION_ID, false);
        verify(writeTransaction, times(1)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(writeTransaction, times(2)).submit();
        verify(commitFuture, times(2)).get(); // 1 + 1 above
    }

    /**
     * Test method {@link L2ForwardingService#programLocalVlanTableMiss(Long, String, boolean)}
     */
    @Test
    public void testProgramLocalVlanTableMiss() throws Exception {
        l2ForwardingService.programLocalVlanTableMiss(DPID, SEGMENTATION_ID, true);
        verify(writeTransaction, times(2)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), anyBoolean());
        verify(writeTransaction, times(1)).submit();
        verify(commitFuture, times(1)).get();

        l2ForwardingService.programLocalVlanTableMiss(DPID, SEGMENTATION_ID, false);
        verify(writeTransaction, times(1)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(writeTransaction, times(2)).submit();
        verify(commitFuture, times(2)).get(); // 1 + 1 above
    }

    /**
     * Test method {@link L2ForwardingService#programTunnelOut(Long, String, Long, String, boolean)}
     */
    @Test
    public void testProgramTunnelOut() throws Exception {
        l2ForwardingService.programTunnelOut(DPID, SEGMENTATION_ID, OF_PORT_OUT, MAC_ADDRESS, true);
        verify(writeTransaction, times(2)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), anyBoolean());
        verify(writeTransaction, times(1)).submit();
        verify(commitFuture, times(1)).get();

        l2ForwardingService.programTunnelOut(DPID, SEGMENTATION_ID, OF_PORT_OUT, MAC_ADDRESS, false);
        verify(writeTransaction, times(1)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(writeTransaction, times(2)).submit();
        verify(commitFuture, times(2)).get(); // 1 + 1 above
    }

    /**
     * Test method {@link L2ForwardingService#programVlanOut(Long, String, Long, String, boolean)}
     */
    @Test
    public void testProgramVlanOut() throws Exception {
        l2ForwardingService.programVlanOut(DPID, SEGMENTATION_ID, ETH_PORT, MAC_ADDRESS, true);
        verify(writeTransaction, times(2)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), anyBoolean());
        verify(writeTransaction, times(1)).submit();
        verify(commitFuture, times(1)).get();

        l2ForwardingService.programVlanOut(DPID, SEGMENTATION_ID, ETH_PORT, MAC_ADDRESS, false);
        verify(writeTransaction, times(1)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(writeTransaction, times(2)).submit();
        verify(commitFuture, times(2)).get(); // 1 + 1 above
    }

    /**--------------------------------- TODO go deeper in test
     * Test method {@link L2ForwardingService#programTunnelFloodOut(Long, String, Long, boolean)}
     */
    @Test
    public void testProgramTunnelFloodOut() throws Exception {
        l2ForwardingService.programTunnelFloodOut(DPID, SEGMENTATION_ID, OF_PORT_OUT, true);
        verify(writeTransaction, times(2)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), anyBoolean());
        verify(writeTransaction, times(1)).submit();
        verify(commitFuture, times(1)).get();

        l2ForwardingService.programTunnelFloodOut(DPID, SEGMENTATION_ID, OF_PORT_OUT, false);
        verify(writeTransaction, times(1)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(writeTransaction, times(2)).submit();
        verify(commitFuture, times(2)).get(); // 1 + 1 above
    }

    /**
     * Test method {@link L2ForwardingService#programVlanFloodOut(Long, String, Long, boolean)}
     */
    @Test
    public void testProgramVlanFloodOut() throws Exception {
        l2ForwardingService.programVlanFloodOut(DPID, SEGMENTATION_ID, ETH_PORT, true);
        verify(writeTransaction, times(2)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), anyBoolean());
        verify(writeTransaction, times(1)).submit();
        verify(commitFuture, times(1)).get();

        l2ForwardingService.programVlanFloodOut(DPID, SEGMENTATION_ID, ETH_PORT, false);
        verify(writeTransaction, times(1)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(writeTransaction, times(2)).submit();
        verify(commitFuture, times(2)).get(); // 1 + 1 above
    }

    /**
    * Test method {@link L2ForwardingService#programTunnelMiss(Long, String, boolean)}
    */
   @Test
   public void testProgramTunnelMiss() throws Exception {
       l2ForwardingService.programTunnelMiss(DPID, SEGMENTATION_ID, true);
       verify(writeTransaction, times(2)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), anyBoolean());
       verify(writeTransaction, times(1)).submit();
       verify(commitFuture, times(1)).get();

       l2ForwardingService.programTunnelMiss(DPID, SEGMENTATION_ID, false);
       verify(writeTransaction, times(1)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
       verify(writeTransaction, times(2)).submit();
       verify(commitFuture, times(2)).get(); // 1 + 1 above
   }

   /**
    * Test method {@link L2ForwardingService#programVlanMiss(Long, String, Long, boolean)}
    */
   @Test
   public void testProgramVlanMiss() throws Exception {
       l2ForwardingService.programTunnelMiss(DPID, SEGMENTATION_ID, true);
       verify(writeTransaction, times(2)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), anyBoolean());
       verify(writeTransaction, times(1)).submit();
       verify(commitFuture, times(1)).get();

       l2ForwardingService.programTunnelMiss(DPID, SEGMENTATION_ID, false);
       verify(writeTransaction, times(1)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
       verify(writeTransaction, times(2)).submit();
       verify(commitFuture, times(2)).get(); // 1 + 1 above
   }
}
