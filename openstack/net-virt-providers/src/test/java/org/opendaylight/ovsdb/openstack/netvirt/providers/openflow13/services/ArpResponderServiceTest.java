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
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.ovsdb.openstack.netvirt.api.Action;
import org.opendaylight.ovsdb.openstack.netvirt.api.Status;
import org.opendaylight.ovsdb.openstack.netvirt.api.StatusCode;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.MdsalConsumer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.util.concurrent.CheckedFuture;


/**
 * Unit test for {@link ArpResponderService}
 */
@RunWith(MockitoJUnitRunner.class)
public class ArpResponderServiceTest {

    @InjectMocks private ArpResponderService arpResponderService = new ArpResponderService();

    @Mock private MdsalConsumer mdsalConsumer;

    private static final String HOST_ADDRESS = "121.0.0.1";
    private static final String MAC_ADDRESS = "87:1D:5E:02:40:B7";

    @Test
    public void testProgramStaticArpEntry() throws Exception {
        InetAddress ipAddress = mock(InetAddress.class);
        when(ipAddress.getHostAddress()).thenReturn(HOST_ADDRESS);

        CheckedFuture<Void, TransactionCommitFailedException> commitFuture = mock(CheckedFuture.class);

        ReadWriteTransaction readWriteTransaction = mock(ReadWriteTransaction.class);
        when(readWriteTransaction.submit()).thenReturn(commitFuture);

        WriteTransaction writeTransaction = mock(WriteTransaction.class);
        when(writeTransaction.submit()).thenReturn(commitFuture);

        DataBroker dataBroker = mock(DataBroker.class);
        when(dataBroker.newReadWriteTransaction()).thenReturn(readWriteTransaction);
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(writeTransaction);

        when(mdsalConsumer.getDataBroker()).thenReturn(dataBroker);

        // test for Action.ADD
        assertEquals("Error, did not return the expected StatusCode", new Status(StatusCode.SUCCESS), arpResponderService.programStaticArpEntry(Long.valueOf(12), "2", MAC_ADDRESS, ipAddress, Action.ADD));

        verify(readWriteTransaction, times(2)).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(Node.class), anyBoolean());
        verify(readWriteTransaction, times(1)).submit();
        verify(commitFuture, times(1)).get();

        // test other Action, here Action.DELETE
        assertEquals("Error, did not return the expected StatusCode", new Status(StatusCode.SUCCESS), arpResponderService.programStaticArpEntry(Long.valueOf(12), "2", MAC_ADDRESS, ipAddress, Action.DELETE));

        verify(writeTransaction, times(1)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(commitFuture, times(2)).get(); // 1 + 1 above
    }

}
