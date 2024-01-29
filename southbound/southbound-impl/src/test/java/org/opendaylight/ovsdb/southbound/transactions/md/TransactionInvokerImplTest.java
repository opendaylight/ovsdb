/*
 * Copyright (c) 2015 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.transactions.md;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.powermock.reflect.Whitebox.getInternalState;

import com.google.common.collect.ImmutableList;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.binding.api.Transaction;
import org.opendaylight.mdsal.binding.api.TransactionChain;

@RunWith(MockitoJUnitRunner.class)
public class TransactionInvokerImplTest {
    @Mock
    private TransactionChain chain;
    @Mock
    private DataBroker db;

    @Before
    public void setUp() {
        doReturn(chain).when(db).createTransactionChain();
        doNothing().when(chain).close();
    }

    @Test
    public void testConstructor() throws InterruptedException {
        try (TransactionInvokerImpl invoker = new TransactionInvokerImpl(db)) {
            verify(db).createTransactionChain();
            assertNotNull(getInternalState(invoker, "executor"));
        }
    }

    @Test
    public void testInvoke() {
        final TransactionInvokerImpl invoker = new TransactionInvokerImpl(db, new ArrayList<>());
        final TransactionCommand command = mock(TransactionCommand.class);
        invoker.invoke(command);

        Queue<TransactionCommand> inputQueue = getInternalState(invoker, "inputQueue");
        assertEquals(1, inputQueue.size());
        assertTrue(inputQueue.contains(command));
    }

    @Test
    public void testOnTransactionChainFailed() {
        final TransactionInvokerImpl invoker = new TransactionInvokerImpl(db, new ArrayList<>());

        final Transaction transaction = mock(Transaction.class);
        invoker.onTransactionChainFailed(chain, transaction, new Throwable());

        final Queue<?> failedQueue = getInternalState(invoker, "failedTransactionQueue");
        assertEquals(1, failedQueue.size());
        assertTrue(failedQueue.contains(transaction));
    }

    @Test
    public void testExtractResubmitCommands() {
        final ReadWriteTransaction tx1 = mock(ReadWriteTransaction.class);
        final ReadWriteTransaction tx2 = mock(ReadWriteTransaction.class);
        final ReadWriteTransaction tx3 = mock(ReadWriteTransaction.class);
        final TransactionCommand cmd1 = mock(TransactionCommand.class);
        final TransactionCommand cmd2 = mock(TransactionCommand.class);
        final TransactionCommand cmd3 = mock(TransactionCommand.class);

        final TransactionInvokerImpl invoker = new TransactionInvokerImpl(db,
            // Given pending transaction order ...
            ImmutableList.of(entry(tx1, cmd1), entry(tx2, cmd2), entry(tx3, cmd3)),
            // .. if tx2 fails ...
            Collections.singletonList(tx2));

        // .. we want to replay tx2 and tx3
        assertEquals(ImmutableList.of(cmd2, cmd3), invoker.extractResubmitCommands());
    }

    private static <K, V> Entry<K, V> entry(final K key, final V value) {
        return new SimpleImmutableEntry<>(key, value);
    }

    @Test
    public void testResetTransactionQueue() {
        final TransactionInvokerImpl invoker = new TransactionInvokerImpl(db, Collections.emptyList(),
            Collections.singletonList(mock(ReadWriteTransaction.class)));

        invoker.resetTransactionQueue();

        assertEmpty(getInternalState(invoker, "pendingTransactions"));
        assertEmpty(getInternalState(invoker, "failedTransactionQueue"));
    }

    private static void assertEmpty(final Collection<?> collection) {
        assertNotNull(collection);
        assertEquals(0, collection.size());
    }

    @Test
    public void testRecordPendingTransaction() {
        final TransactionInvokerImpl invoker = new TransactionInvokerImpl(db, Collections.emptyList());

        final TransactionCommand command = mock(TransactionCommand.class);
        final ReadWriteTransaction transaction = mock(ReadWriteTransaction.class);
        invoker.recordPendingTransaction(command, transaction);

        Queue<Entry<?, ?>> endingTransactions = getInternalState(invoker, "pendingTransactions");
        assertEquals(1, endingTransactions.size());
        assertSame(transaction, endingTransactions.element().getKey());
        assertSame(command, endingTransactions.element().getValue());
    }

    @Test
    public void testExtractCommands() throws InterruptedException {
        final TransactionInvokerImpl invoker = new TransactionInvokerImpl(db, Collections.emptyList());

        final TransactionCommand command = mock(TransactionCommand.class);
        invoker.invoke(command);

        assertEquals(Collections.singletonList(command), invoker.extractCommands());
    }

    @Test
    public void testExtractCommandsFromQueue() throws InterruptedException {
        final TransactionInvokerImpl invoker = new TransactionInvokerImpl(db, Collections.emptyList());

        final TransactionCommand command = mock(TransactionCommand.class);
        invoker.invoke(command);

        assertEquals(Collections.singletonList(command), invoker.extractCommandsFromQueue());
    }

    @Test
    public void testClose() throws InterruptedException {
        final ExecutorService executor = mock(ExecutorService.class);
        doNothing().when(executor).shutdown();

        try (TransactionInvokerImpl invoker = new TransactionInvokerImpl(db, executor)) {
            // No-op, but invokes close
        }

        verify(executor).shutdown();
    }
}
