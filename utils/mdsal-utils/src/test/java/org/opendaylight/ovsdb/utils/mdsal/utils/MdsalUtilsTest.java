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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopologyBuilder;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;

/**
 * Unit test for class {@link MdsalUtils}.
 */
@RunWith(MockitoJUnitRunner.class)
public class MdsalUtilsTest {
    private static final DataObjectIdentifier<NetworkTopology> DOI =
        DataObjectIdentifier.builder(NetworkTopology.class).build();
    private static final NetworkTopology NT = new NetworkTopologyBuilder().build();

    @Mock
    private DataBroker dataBroker;
    @Mock
    private ReadTransaction readTransaction;
    @Mock
    private WriteTransaction writeTransaction;

    private MdsalUtils mdsalUtils;

    @Before
    public void before() {
        mdsalUtils = new MdsalUtils(dataBroker);
    }

    @Test
    public void testDelete() {
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(writeTransaction);
        doReturn(CommitInfo.emptyFluentFuture()).when(writeTransaction).commit();

        boolean result = mdsalUtils.delete(LogicalDatastoreType.CONFIGURATION, DOI);

        verify(writeTransaction, times(1)).delete(LogicalDatastoreType.CONFIGURATION, DOI);
        verify(writeTransaction, times(1)).commit();

        assertTrue("Error, the delete transaction failed", result);
    }

    @Test
    public void testMerge() {
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(writeTransaction);
        doReturn(CommitInfo.emptyFluentFuture()).when(writeTransaction).commit();

        boolean result = mdsalUtils.merge(LogicalDatastoreType.CONFIGURATION, DOI, NT);

        verify(writeTransaction, times(1)).mergeParentStructureMerge(LogicalDatastoreType.CONFIGURATION, DOI, NT);
        verify(writeTransaction, times(1)).commit();

        assertTrue("Error, the merge transaction failed", result);
    }

    @Test
    public void testPut() {
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(writeTransaction);
        doReturn(CommitInfo.emptyFluentFuture()).when(writeTransaction).commit();

        boolean result = mdsalUtils.put(LogicalDatastoreType.CONFIGURATION, DOI, NT);

        verify(writeTransaction, times(1)).mergeParentStructurePut(LogicalDatastoreType.CONFIGURATION, DOI, NT);
        verify(writeTransaction, times(1)).commit();

        assertTrue("Error, the put transaction failed", result);
    }

    @Test
    public void testRead() {
        doReturn(readTransaction).when(dataBroker).newReadOnlyTransaction();
        doReturn(FluentFutures.immediateFluentFuture(Optional.of(NT))).when(readTransaction).read(
            LogicalDatastoreType.CONFIGURATION, DOI);
        final var result = mdsalUtils.read(LogicalDatastoreType.CONFIGURATION, DOI);

        verify(readTransaction, times(1)).read(LogicalDatastoreType.CONFIGURATION, DOI);
        verify(readTransaction, times(1)).close();

        assertEquals("Error, the read transaction failed", NT, result);
    }
}
