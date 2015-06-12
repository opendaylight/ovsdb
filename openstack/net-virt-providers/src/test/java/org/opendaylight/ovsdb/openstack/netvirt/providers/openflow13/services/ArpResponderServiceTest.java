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
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.Service;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.util.concurrent.CheckedFuture;


/**
 * Unit test for {@link ArpResponderService}
 */
@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("unchecked")
public class ArpResponderServiceTest {

    @InjectMocks private ArpResponderService arpResponderService = new ArpResponderService();

    @Mock private DataBroker dataBroker;

    private static final String HOST_ADDRESS = "121.0.0.1";
    private static final String MAC_ADDRESS = "87:1D:5E:02:40:B7";

    @Test
    public void testProgramStaticArpEntry() throws Exception {
        arpResponderService.setService(Service.ARP_RESPONDER);

        InetAddress ipAddress = mock(InetAddress.class);
        when(ipAddress.getHostAddress()).thenReturn(HOST_ADDRESS);

        WriteTransaction transaction = mock(WriteTransaction.class);
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(transaction);
        CheckedFuture<Void, TransactionCommitFailedException> commitFuture = mock(CheckedFuture.class);
        when(transaction.submit()).thenReturn(commitFuture);

        NodeBuilder nodeBuilder = mock(NodeBuilder.class);
        when(nodeBuilder.getKey()).thenReturn(mock(NodeKey.class));

        FlowBuilder flowBuilder = mock(FlowBuilder.class);
        when(flowBuilder.getKey()).thenReturn(mock(FlowKey.class));

        // test Action.ADD
        assertEquals("Error, did not return the expected StatusCode", new Status(StatusCode.SUCCESS), arpResponderService.programStaticArpEntry(Long.valueOf(12), "2", MAC_ADDRESS, ipAddress, Action.ADD));

        verify(transaction, times(2)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), anyBoolean());
        verify(transaction, times(1)).submit();
        verify(commitFuture, times(1)).get();

        // test Action.DELETE
        assertEquals("Error, did not return the expected StatusCode", new Status(StatusCode.SUCCESS), arpResponderService.programStaticArpEntry(Long.valueOf(12), "2", MAC_ADDRESS, ipAddress, Action.DELETE));

        verify(transaction, times(1)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(commitFuture, times(2)).get(); // 1 + 1 above
    }
}
