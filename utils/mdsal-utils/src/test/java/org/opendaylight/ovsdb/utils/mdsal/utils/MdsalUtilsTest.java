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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Unit test for class {@link MdsalUtils}.
 */
@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("unchecked")
public class MdsalUtilsTest {

    @InjectMocks private MdsalUtils mdsalUtils;

    @Mock private DataBroker databroker;

    @Test
    public void testDelete() {
        WriteTransaction writeTransaction = mock(WriteTransaction.class);
        when(databroker.newWriteOnlyTransaction()).thenReturn(writeTransaction);
        doReturn(CommitInfo.emptyFluentFuture()).when(writeTransaction).commit();

        boolean result = mdsalUtils.delete(LogicalDatastoreType.CONFIGURATION, mock(InstanceIdentifier.class));

        verify(writeTransaction, times(1)).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(writeTransaction, times(1)).commit();

        assertTrue("Error, the delete transaction failed", result);
    }

    @Test
    public void testMerge() {
        WriteTransaction writeTransaction = mock(WriteTransaction.class);
        when(databroker.newWriteOnlyTransaction()).thenReturn(writeTransaction);
        doReturn(CommitInfo.emptyFluentFuture()).when(writeTransaction).commit();

        boolean result = mdsalUtils.merge(LogicalDatastoreType.CONFIGURATION,
                mock(InstanceIdentifier.class), mock(DataObject.class));

        verify(writeTransaction, times(1)).mergeParentStructureMerge(any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class), any(DataObject.class));
        verify(writeTransaction, times(1)).commit();

        assertTrue("Error, the merge transaction failed", result);
    }

    @Test
    public void testPut() {
        WriteTransaction writeTransaction = mock(WriteTransaction.class);
        when(databroker.newWriteOnlyTransaction()).thenReturn(writeTransaction);
        doReturn(CommitInfo.emptyFluentFuture()).when(writeTransaction).commit();

        boolean result = mdsalUtils.put(LogicalDatastoreType.CONFIGURATION,
                mock(InstanceIdentifier.class), mock(DataObject.class));

        verify(writeTransaction, times(1)).mergeParentStructurePut(any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class), any(DataObject.class));
        verify(writeTransaction, times(1)).commit();

        assertTrue("Error, the put transaction failed", result);
    }

    @Test
    public void testRead() {
        ReadTransaction readTransaction = mock(ReadTransaction.class);
        doReturn(readTransaction).when(databroker).newReadOnlyTransaction();
        DataObject obj = mock(DataObject.class);
        doReturn(FluentFutures.immediateFluentFuture(Optional.of(obj))).when(readTransaction).read(
            any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        DataObject result = mdsalUtils.read(LogicalDatastoreType.CONFIGURATION, mock(InstanceIdentifier.class));

        verify(readTransaction, times(1)).read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(readTransaction, times(1)).close();

        assertEquals("Error, the read transaction failed", obj, result);
    }
}
