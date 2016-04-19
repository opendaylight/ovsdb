/*
 * Copyright (c) 2016 Inocybe and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.utils.mdsal.utils;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@SuppressWarnings("unchecked")
@RunWith(MockitoJUnitRunner.class)
public class MdsalUtilsAsyncTest {

    private MdsalUtilsAsync brokerHelper;

    @Mock
    private DataBroker databroker;
    @Mock
    private WriteTransaction writeTransaction;
    @Mock
    private ReadOnlyTransaction readTransaction;
    @Mock
    private DataObject data;

    abstract class Tests implements DataObject {}

    @SuppressWarnings("rawtypes")
    private static final InstanceIdentifier TEST_IID = InstanceIdentifier.create(Tests.class);

    @Before
    public void setUp() {
        setupDataBroker();
        setupTransactions();
        brokerHelper = Mockito.spy(new MdsalUtilsAsync(databroker));
    }

    private void setupDataBroker() {
        Mockito.doReturn(writeTransaction).when(databroker).newWriteOnlyTransaction();
        Mockito.doReturn(readTransaction).when(databroker).newReadOnlyTransaction();
    }

    private void setupTransactions() {
        // Read transaction
        Mockito.doReturn("readOperation").when(readTransaction).getIdentifier();
        final CheckedFuture<Optional<DataObject>, ReadFailedException> readFuture = Mockito.mock(CheckedFuture.class);
        Mockito.doReturn(readFuture).when(readTransaction).read(Matchers.any(LogicalDatastoreType.class), Matchers.eq(TEST_IID));

        // Write transaction
        Mockito.doReturn("writeOperation").when(writeTransaction).getIdentifier();
        final CheckedFuture<Void, TransactionCommitFailedException> writeFuture = Mockito.mock(CheckedFuture.class);
        Mockito.doReturn(writeFuture).when(writeTransaction).submit();
    }

    @Test
    public void testDelete() {
        final String operationDesc = "testDelete";
        brokerHelper.delete(LogicalDatastoreType.CONFIGURATION, TEST_IID, operationDesc);

        Mockito.verify(writeTransaction, Mockito.times(1)).delete(LogicalDatastoreType.CONFIGURATION, TEST_IID);
        Mockito.verify(brokerHelper, Mockito.times(1)).commitTransaction(writeTransaction, operationDesc);
    }

    @Test
    public void testPut() {
        final String operationDesc = "testPut";
        brokerHelper.put(LogicalDatastoreType.CONFIGURATION, TEST_IID, data, operationDesc);

        Mockito.verify(writeTransaction, Mockito.times(1)).put(LogicalDatastoreType.CONFIGURATION, TEST_IID, data, true);
        Mockito.verify(brokerHelper, Mockito.times(1)).commitTransaction(writeTransaction, operationDesc);
    }

    @Test
    public void testMerge() {
        final String operationDesc = "testMerge";
        final boolean withParent = false;
        brokerHelper.merge(LogicalDatastoreType.CONFIGURATION, TEST_IID, data, operationDesc, withParent);

        Mockito.verify(writeTransaction, Mockito.times(1)).merge(LogicalDatastoreType.CONFIGURATION, TEST_IID, data, withParent);
        Mockito.verify(brokerHelper, Mockito.times(1)).commitTransaction(writeTransaction, operationDesc);
    }

    @Test
    public void testRead() {
        brokerHelper.read(LogicalDatastoreType.CONFIGURATION, TEST_IID);

        Mockito.verify(readTransaction, Mockito.times(1)).read(LogicalDatastoreType.CONFIGURATION, TEST_IID);
        // TODO I still need to figure out how to verify readTransaction.close()
        // is correctly called.
        // Mockito.verify(readTransaction, Mockito.times(1)).close();
    }
}
