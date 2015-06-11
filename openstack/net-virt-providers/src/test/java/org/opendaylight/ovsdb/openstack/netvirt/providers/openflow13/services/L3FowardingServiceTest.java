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

import java.net.InetAddress;

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
import org.opendaylight.ovsdb.openstack.netvirt.api.Action;
import org.opendaylight.ovsdb.openstack.netvirt.api.Status;
import org.opendaylight.ovsdb.openstack.netvirt.api.StatusCode;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.PipelineOrchestrator;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.Service;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.util.concurrent.CheckedFuture;

/**
 * Unit test fort {@link L3ForwardingService}
 */
@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("unchecked")
public class L3FowardingServiceTest {

    @InjectMocks private L3ForwardingService l3ForwardingService = new L3ForwardingService();

    @Mock private DataBroker dataBroker;
    @Mock private PipelineOrchestrator orchestrator;

    @Mock private WriteTransaction writeTransaction;
    @Mock private CheckedFuture<Void, TransactionCommitFailedException> commitFuture;

    private static final String SEGMENTATION_ID = "2";
    private static final String HOST_ADDRESS = "127.0.0.1";
    private static final String MAC_ADDRESS = "87:1D:5E:02:40:B7";

    @Before
    public void setUp() throws Exception {
        when(writeTransaction.submit()).thenReturn(commitFuture);

        when(dataBroker.newWriteOnlyTransaction()).thenReturn(writeTransaction);

        when(orchestrator.getNextServiceInPipeline(any(Service.class))).thenReturn(Service.ARP_RESPONDER);
    }

    /**
     * Test method {@link L3ForwardingService#programForwardingTableEntry(Long, String, InetAddress, String, Action)}
     */
    @Test
    public void testProgramForwardingTableEntry() throws Exception {
        InetAddress address = mock(InetAddress.class);
        when(address.getHostAddress()).thenReturn(HOST_ADDRESS);

        assertEquals("Error, did not return the expected StatusCode",
                new Status(StatusCode.SUCCESS),
                l3ForwardingService.programForwardingTableEntry(Long.valueOf(123),
                        SEGMENTATION_ID, address, MAC_ADDRESS, Action.ADD));
        verify(writeTransaction, times(2)).put(any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class), any(Node.class), anyBoolean());
        verify(writeTransaction, times(1)).submit();
        verify(commitFuture, times(1)).get();

        assertEquals("Error, did not return the expected StatusCode",
                new Status(StatusCode.SUCCESS),
                l3ForwardingService.programForwardingTableEntry(Long.valueOf(123),
                        SEGMENTATION_ID, address, MAC_ADDRESS, Action.DELETE));
        verify(writeTransaction, times(1)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(writeTransaction, times(2)).submit();
        verify(commitFuture, times(2)).get(); // 1 + 1 above
    }

}
