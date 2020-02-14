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

import com.google.common.util.concurrent.FluentFuture;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.eclipse.jdt.annotation.NonNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
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
        FluentFuture<? extends @NonNull CommitInfo> future = mock(FluentFuture.class);
        Mockito.doReturn(FluentFutures.immediateNullFluentFuture()).when(writeTransaction.commit());

        boolean result = mdsalUtils.delete(LogicalDatastoreType.CONFIGURATION, mock(InstanceIdentifier.class));

        verify(writeTransaction, times(1)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(writeTransaction, times(1)).commit();

        assertTrue("Error, the delete transaction failed", result);
    }

    @Test
    public void testMerge() {
        WriteTransaction writeTransaction = mock(WriteTransaction.class);
        when(databroker.newWriteOnlyTransaction()).thenReturn(writeTransaction);
        FluentFuture<? extends @NonNull CommitInfo> future = mock(FluentFuture.class);
        Mockito.doReturn(FluentFutures.immediateNullFluentFuture()).when(writeTransaction.commit());

        boolean result = mdsalUtils.merge(LogicalDatastoreType.CONFIGURATION,
                mock(InstanceIdentifier.class), mock(DataObject.class));

        verify(writeTransaction, times(1)).merge(any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class), any(DataObject.class), anyBoolean());
        verify(writeTransaction, times(1)).commit();

        assertTrue("Error, the merge transaction failed", result);
    }

    @Test
    public void testPut() {
        WriteTransaction writeTransaction = mock(WriteTransaction.class);
        when(databroker.newWriteOnlyTransaction()).thenReturn(writeTransaction);
        FluentFuture<? extends @NonNull CommitInfo> future = mock(FluentFuture.class);
        Mockito.doReturn(FluentFutures.immediateNullFluentFuture()).when(writeTransaction.commit());

        boolean result = mdsalUtils.put(LogicalDatastoreType.CONFIGURATION,
                mock(InstanceIdentifier.class), mock(DataObject.class));

        verify(writeTransaction, times(1)).put(any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class), any(DataObject.class), anyBoolean());
        verify(writeTransaction, times(1)).commit();

        assertTrue("Error, the put transaction failed", result);
    }

    @Test
    public void testRead() throws InterruptedException, ExecutionException {
        ReadTransaction readOnlyTransaction = mock(ReadTransaction.class);
        when(databroker.newReadOnlyTransaction()).thenReturn(readOnlyTransaction);
        FluentFuture<Optional> future = mock(FluentFuture.class);
        DataObject obj = mock(DataObject.class);
        Optional opt = Optional.of(obj);
        when(future.get()).thenReturn(opt);
        when(readOnlyTransaction.read(any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class))).thenReturn(future);

        DataObject result = mdsalUtils.read(LogicalDatastoreType.CONFIGURATION, mock(InstanceIdentifier.class));

        verify(readOnlyTransaction, times(1)).read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(readOnlyTransaction, times(1)).close();

        assertEquals("Error, the read transaction failed", obj, result);
    }
}
