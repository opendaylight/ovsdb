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
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.powermock.reflect.Whitebox.getInternalState;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;

@RunWith(MockitoJUnitRunner.class)
public class TransactionInvokerImplTest {
    @Mock
    private BindingTransactionChain chain;
    @Mock
    private DataBroker db;

    @Before
    public void setUp() {
        doReturn(chain).when(db).createTransactionChain(any(TransactionChainListener.class));
        doNothing().when(chain).close();
    }

    @Test
    public void testConstructor() throws InterruptedException {
        try (TransactionInvokerImpl invoker = new TransactionInvokerImpl(db)) {
            verify(db).createTransactionChain(any(TransactionChainListener.class));
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

        final AsyncTransaction<?, ?> transaction = mock(AsyncTransaction.class);
        invoker.onTransactionChainFailed(chain, transaction, new Throwable());

        final Queue<?> failedQueue = getInternalState(invoker, "failedTransactionQueue");
        assertEquals(1, failedQueue.size());
        assertTrue(failedQueue.contains(transaction));
    }

    @Test
    public void testExtractResubmitCommands() {
        final ReadWriteTransaction transaction = mock(ReadWriteTransaction.class);
        final ReadWriteTransaction tx1 = mock(ReadWriteTransaction.class);
        final ReadWriteTransaction tx2 = mock(ReadWriteTransaction.class);

        final List<ReadWriteTransaction> pendingTransactions = new ArrayList<>();
        pendingTransactions.add(tx1);
        pendingTransactions.add(transaction);
        pendingTransactions.add(tx2);

        final Map<ReadWriteTransaction,TransactionCommand> transactionToCommand = new HashMap<>();
        final TransactionCommand txCommand = mock(TransactionCommand.class);
        transactionToCommand.put(tx1, txCommand);
        transactionToCommand.put(tx2, txCommand);
        transactionToCommand.put(transaction, txCommand);

        final TransactionInvokerImpl invoker = new TransactionInvokerImpl(db, pendingTransactions,
            Collections.singletonList(transaction), transactionToCommand);

        assertEquals(ImmutableList.of(txCommand, txCommand), invoker.extractResubmitCommands());
    }

    @Test
    public void testResetTransactionQueue() {
        final TransactionInvokerImpl invoker = new TransactionInvokerImpl(db, Collections.emptyList(),
            Collections.singletonList(mock(ReadWriteTransaction.class)), Collections.emptyMap());

        invoker.resetTransactionQueue();

        assertNotNull(getInternalState(invoker, "pendingTransactions"));
        assertNotNull(getInternalState(invoker, "transactionToCommand"));
        final Queue<?> failedTransactionQueue = getInternalState(invoker, "failedTransactionQueue");
        assertEquals(0, failedTransactionQueue.size());
    }

    @Test
    public void testRecordPendingTransaction() {
        final TransactionInvokerImpl invoker = new TransactionInvokerImpl(db, Collections.emptyList());

        final TransactionCommand command = mock(TransactionCommand.class);
        final ReadWriteTransaction transaction = mock(ReadWriteTransaction.class);
        invoker.recordPendingTransaction(command, transaction);

        List<ReadWriteTransaction> testPendingTransactions = getInternalState(invoker, "pendingTransactions");
        assertEquals(1, testPendingTransactions.size());
        assertTrue(testPendingTransactions.contains(transaction));

        assertEquals(Collections.singletonMap(transaction, command), getInternalState(invoker, "transactionToCommand"));
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
