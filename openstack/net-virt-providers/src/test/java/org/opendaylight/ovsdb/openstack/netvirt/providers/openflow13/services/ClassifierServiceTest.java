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
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.PipelineOrchestrator;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.Service;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.util.concurrent.CheckedFuture;

/**
 * Unit test for {@link ClassifierService}
 */
@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("unchecked")
public class ClassifierServiceTest {

    @InjectMocks private ClassifierService classifierService = new ClassifierService(Service.ARP_RESPONDER);

    @Mock private DataBroker dataBroker;

    private static final String MAC_ADDRESS = "87:1D:5E:02:40:B7";

    @Mock private PipelineOrchestrator orchestrator;

    @Mock private WriteTransaction writeTransaction;
    @Mock private CheckedFuture<Void, TransactionCommitFailedException> commitFuture;

    @Before
    public void setUp() {
        when(writeTransaction.submit()).thenReturn(commitFuture);
        when(writeTransaction.submit()).thenReturn(commitFuture);

        when(dataBroker.newWriteOnlyTransaction()).thenReturn(writeTransaction);

        when(orchestrator.getNextServiceInPipeline(any(Service.class))).thenReturn(Service.ARP_RESPONDER);
    }

    /**
     * Test method {@link ClassifierService#programLocalInPort(Long, String, Long, String, boolean)}
     */
    @Test
    public void testProgramLocalInPort() throws Exception {
        // write
        classifierService.programLocalInPort(Long.valueOf(1212), "2", Long.valueOf(455), MAC_ADDRESS, true);
        verify(writeTransaction, times(2)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), anyBoolean());
        verify(writeTransaction, times(1)).submit();
        verify(commitFuture, times(1)).get();

        // remove
        classifierService.programLocalInPort(Long.valueOf(1212), "2", Long.valueOf(455), MAC_ADDRESS, false);
        verify(writeTransaction, times(1)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(commitFuture, times(2)).get(); // 1 + 1 above
    }

    /**
     * Test method {@link ClassifierService#programLocalInPortSetVlan(Long, String, Long, String, boolean)}
     */
    @Test
    public void testProgramLocalInPortSetVlan() throws Exception {
        // write
        classifierService.programLocalInPortSetVlan(Long.valueOf(1212), "2", Long.valueOf(455), MAC_ADDRESS, true);
        verify(writeTransaction, times(2)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), anyBoolean());
        verify(writeTransaction, times(1)).submit();
        verify(commitFuture, times(1)).get();

        // remove
        classifierService.programLocalInPortSetVlan(Long.valueOf(1212), "2", Long.valueOf(455), MAC_ADDRESS, false);
        verify(writeTransaction, times(1)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(commitFuture, times(2)).get(); // 1 + 1 above
    }

    /**
     * Test method {@link ClassifierService#programDropSrcIface(Long, Long, boolean)}
     */
    @Test
    public void testProgramDropSrcIface() throws Exception {
        // write
        classifierService.programDropSrcIface(Long.valueOf(1212), Long.valueOf(455), true);
        verify(writeTransaction, times(2)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), anyBoolean());
        verify(writeTransaction, times(1)).submit();
        verify(commitFuture, times(1)).get();

        // remove
        classifierService.programDropSrcIface(Long.valueOf(1212), Long.valueOf(455), false);
        verify(writeTransaction, times(1)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(commitFuture, times(2)).get(); // 1 + 1 above
    }

    /**
     * Test method {@link ClassifierService#programTunnelIn(Long, String, Long, boolean)}
     */
    @Test
    public void testProgramTunnelIn() throws Exception {
        // write
        classifierService.programTunnelIn(Long.valueOf(1212), "2", Long.valueOf(455), true);
        verify(writeTransaction, times(2)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), anyBoolean());
        verify(writeTransaction, times(1)).submit();
        verify(commitFuture, times(1)).get();

        // remove
        classifierService.programTunnelIn(Long.valueOf(1212), "2", Long.valueOf(455), false);
        verify(writeTransaction, times(1)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(commitFuture, times(2)).get(); // 1 + 1 above
    }

    /**
     * Test method {@link ClassifierService#programVlanIn(Long, String, Long, boolean)}
     */
    @Test
    public void testProgramVlanIn() throws Exception {
        // write
        classifierService.programVlanIn(Long.valueOf(1212), "2", Long.valueOf(455), true);
        verify(writeTransaction, times(2)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), anyBoolean());
        verify(writeTransaction, times(1)).submit();
        verify(commitFuture, times(1)).get();

        // remove
        classifierService.programVlanIn(Long.valueOf(1212), "2", Long.valueOf(455), false);
        verify(writeTransaction, times(1)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(commitFuture, times(2)).get(); // 1 + 1 above
    }

    /**
     * Test method {@link ClassifierService#programLLDPPuntRule(Long)}
     */
    @Test
    public void testProgramLLDPPuntRule() throws Exception {
        // write
        classifierService.programLLDPPuntRule(Long.valueOf(1212));
        verify(writeTransaction, times(2)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), anyBoolean());
        verify(writeTransaction, times(1)).submit();
        verify(commitFuture, times(1)).get();
    }
}
