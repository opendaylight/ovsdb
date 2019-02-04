/*
 * Copyright (c) 2015 Inocybe and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.utils.mdsal.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Unit test for class {@link ControllerMdsalUtils}.
 */
@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({ "unchecked", "rawtypes" })
public class ControllerMdsalUtilsTest {

    @InjectMocks private ControllerMdsalUtils mdsalUtils;

    @Mock private DataBroker databroker;

    @Test
    public void testDelete() {
        WriteTransaction writeTransaction = mock(WriteTransaction.class);
        when(databroker.newWriteOnlyTransaction()).thenReturn(writeTransaction);
        CheckedFuture<Void, TransactionCommitFailedException> future = mock(CheckedFuture.class);
        when(writeTransaction.submit()).thenReturn(future);

        boolean result = mdsalUtils.delete(LogicalDatastoreType.CONFIGURATION, mock(InstanceIdentifier.class));

        verify(writeTransaction, times(1)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(writeTransaction, times(1)).submit();

        assertTrue("Error, the delete transaction failed", result);
    }

    @Test
    public void testMerge() {
        WriteTransaction writeTransaction = mock(WriteTransaction.class);
        when(databroker.newWriteOnlyTransaction()).thenReturn(writeTransaction);
        CheckedFuture<Void, TransactionCommitFailedException> future = mock(CheckedFuture.class);
        when(writeTransaction.submit()).thenReturn(future);

        boolean result = mdsalUtils.merge(LogicalDatastoreType.CONFIGURATION,
                mock(InstanceIdentifier.class), mock(DataObject.class));

        verify(writeTransaction, times(1)).merge(any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class), any(DataObject.class), anyBoolean());
        verify(writeTransaction, times(1)).submit();

        assertTrue("Error, the merge transaction failed", result);
    }

    @Test
    public void testPut() {
        WriteTransaction writeTransaction = mock(WriteTransaction.class);
        when(databroker.newWriteOnlyTransaction()).thenReturn(writeTransaction);
        CheckedFuture<Void, TransactionCommitFailedException> future = mock(CheckedFuture.class);
        when(writeTransaction.submit()).thenReturn(future);

        boolean result = mdsalUtils.put(LogicalDatastoreType.CONFIGURATION,
                mock(InstanceIdentifier.class), mock(DataObject.class));

        verify(writeTransaction, times(1)).put(any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class), any(DataObject.class), anyBoolean());
        verify(writeTransaction, times(1)).submit();

        assertTrue("Error, the put transaction failed", result);
    }

    @Test
    public void testRead() throws ReadFailedException {
        ReadOnlyTransaction readOnlyTransaction = mock(ReadOnlyTransaction.class);
        when(databroker.newReadOnlyTransaction()).thenReturn(readOnlyTransaction);
        CheckedFuture<Optional, ReadFailedException> future = mock(CheckedFuture.class);
        DataObject obj = mock(DataObject.class);
        Optional opt = Optional.of(obj);
        when(future.checkedGet()).thenReturn(opt);
        when(readOnlyTransaction.read(any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class))).thenReturn(future);

        DataObject result = mdsalUtils.read(LogicalDatastoreType.CONFIGURATION, mock(InstanceIdentifier.class));

        verify(readOnlyTransaction, times(1)).read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(readOnlyTransaction, times(1)).close();

        assertEquals("Error, the read transaction failed", obj, result);
    }
}
