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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberModifier;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@PrepareForTest({TransactionInvokerImpl.class})
@RunWith(PowerMockRunner.class)
public class TransactionInvokerImplTest {
    private static final int QUEUE_SIZE = 10000;
    @Mock private BindingTransactionChain chain;
    @Mock private DataBroker db;
    private BlockingQueue<TransactionCommand> inputQueue = new LinkedBlockingQueue<>(QUEUE_SIZE);
    private BlockingQueue<ReadWriteTransaction> successfulTransactionQueue
        = new LinkedBlockingQueue<>(QUEUE_SIZE);
    private BlockingQueue<AsyncTransaction<?, ?>> failedTransactionQueue
        = new LinkedBlockingQueue<>(QUEUE_SIZE);
    @Mock private ExecutorService executor;
    @Mock private AtomicBoolean runTask;
    private Map<ReadWriteTransaction,TransactionCommand> transactionToCommand
        = new HashMap<>();
    private List<ReadWriteTransaction> pendingTransactions = new ArrayList<>();
    private TransactionInvokerImpl transactionInvokerImpl;

    @Before
    public void setUp() throws Exception {
        transactionInvokerImpl = PowerMockito.mock(TransactionInvokerImpl.class, Mockito.CALLS_REAL_METHODS);
        MemberModifier.field(TransactionInvokerImpl.class, "chain").set(transactionInvokerImpl, chain);
        MemberModifier.field(TransactionInvokerImpl.class, "db").set(transactionInvokerImpl, db);
    }

    @Test
    public void testTransactionInvokerImpl() throws Exception {
        MemberModifier.field(TransactionInvokerImpl.class, "inputQueue").set(transactionInvokerImpl, inputQueue);
        when(db.createTransactionChain(any(TransactionChainListener.class))).thenReturn(mock(BindingTransactionChain.class));
        TransactionInvokerImpl transactionInvokerImpl1 = new TransactionInvokerImpl(db);
        verify(db).createTransactionChain(any(TransactionChainListener.class));
        assertNotNull(Whitebox.getInternalState(transactionInvokerImpl1, "executor"));
    }

    @Test
    public void testInvoke() throws Exception {
        MemberModifier.field(TransactionInvokerImpl.class, "inputQueue").set(transactionInvokerImpl, inputQueue);
        TransactionCommand command = mock(TransactionCommand.class);
        transactionInvokerImpl.invoke(command);
        BlockingQueue<TransactionCommand> testInputQueue = Whitebox.getInternalState(transactionInvokerImpl, "inputQueue");
        assertTrue(testInputQueue.contains(command));
    }

    @Test
    public void testOnTransactionChainFailed() throws Exception {
        MemberModifier.field(TransactionInvokerImpl.class, "failedTransactionQueue").set(transactionInvokerImpl, failedTransactionQueue);
        TransactionChain<?, ?> chain = mock(TransactionChain.class);
        AsyncTransaction<?, ?> transaction = mock(AsyncTransaction.class);
        Throwable cause = mock(Throwable.class);
        transactionInvokerImpl.onTransactionChainFailed(chain, transaction, cause);
        BlockingQueue<AsyncTransaction<?, ?>> testFailedTransactionQueue = Whitebox.getInternalState(transactionInvokerImpl, "failedTransactionQueue");
        assertTrue(testFailedTransactionQueue.contains(transaction));
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testExtractResubmitCommands() throws Exception {
        AsyncTransaction<?, ?> transaction = mock(ReadWriteTransaction.class);
        failedTransactionQueue.put(transaction);
        MemberModifier.field(TransactionInvokerImpl.class, "failedTransactionQueue").set(transactionInvokerImpl, failedTransactionQueue);

        AsyncTransaction tx1 = mock(ReadWriteTransaction.class);
        AsyncTransaction tx2 = mock(ReadWriteTransaction.class);
        pendingTransactions.add((ReadWriteTransaction) tx1);
        pendingTransactions.add((ReadWriteTransaction) transaction);
        pendingTransactions.add((ReadWriteTransaction) tx2);
        MemberModifier.field(TransactionInvokerImpl.class, "pendingTransactions").set(transactionInvokerImpl, pendingTransactions);

        List<ReadWriteTransaction> transactions= new ArrayList<>();
        transactions.add((ReadWriteTransaction) tx1);

        TransactionCommand txCommand = mock(TransactionCommand.class);
        transactionToCommand.put((ReadWriteTransaction) tx1, txCommand);
        transactionToCommand.put((ReadWriteTransaction) tx2, txCommand);
        transactionToCommand.put((ReadWriteTransaction) transaction, txCommand);
        MemberModifier.field(TransactionInvokerImpl.class, "transactionToCommand").set(transactionInvokerImpl, transactionToCommand);
        PowerMockito.suppress(MemberModifier.method(TransactionInvokerImpl.class, "resetTransactionQueue"));

        List<TransactionCommand> testCommands = new ArrayList<>();
        testCommands.add(txCommand);

        assertEquals(testCommands, Whitebox.invokeMethod(transactionInvokerImpl, "extractResubmitCommands"));
    }

    @Test
    public void testResetTransactionQueue() throws Exception {
        doNothing().when(chain).close();
        when(db.createTransactionChain(any(TransactionInvokerImpl.class))).thenReturn(chain);

        failedTransactionQueue.add(mock(AsyncTransaction.class));
        MemberModifier.field(TransactionInvokerImpl.class, "pendingTransactions").set(transactionInvokerImpl, pendingTransactions);
        MemberModifier.field(TransactionInvokerImpl.class, "transactionToCommand").set(transactionInvokerImpl, transactionToCommand);
        MemberModifier.field(TransactionInvokerImpl.class, "failedTransactionQueue").set(transactionInvokerImpl, failedTransactionQueue);
        MemberModifier.field(TransactionInvokerImpl.class, "successfulTransactionQueue").set(transactionInvokerImpl, successfulTransactionQueue);

        Whitebox.invokeMethod(transactionInvokerImpl, "resetTransactionQueue");
        assertNotNull(Whitebox.getInternalState(transactionInvokerImpl, "pendingTransactions"));
        assertNotNull(Whitebox.getInternalState(transactionInvokerImpl, "transactionToCommand"));
        BlockingQueue<AsyncTransaction<?, ?>> testFailedTransactionQueue = Whitebox.getInternalState(transactionInvokerImpl, "failedTransactionQueue");
        assertEquals(0, testFailedTransactionQueue.size());
    }

    @Test
    public void testRecordPendingTransaction() throws Exception {
        TransactionCommand command = mock(TransactionCommand.class);
        ReadWriteTransaction transaction= mock(ReadWriteTransaction.class);
        MemberModifier.field(TransactionInvokerImpl.class, "pendingTransactions").set(transactionInvokerImpl, pendingTransactions);
        MemberModifier.field(TransactionInvokerImpl.class, "transactionToCommand").set(transactionInvokerImpl, transactionToCommand);
        Whitebox.invokeMethod(transactionInvokerImpl, "recordPendingTransaction", command, transaction);

        List<ReadWriteTransaction> testPendingTransactions = Whitebox.getInternalState(transactionInvokerImpl, "pendingTransactions");
        assertEquals(1, testPendingTransactions.size());

        Map<ReadWriteTransaction, TransactionCommand> testTransactionToCommand = Whitebox.getInternalState(transactionInvokerImpl, "transactionToCommand");
        assertEquals(1, testTransactionToCommand.size());
    }

    @Test
    public void testExtractCommands() throws Exception {
        List<TransactionCommand> commands = new ArrayList<>();
        PowerMockito.doReturn(commands).when(transactionInvokerImpl, "extractResubmitCommands");

        List<TransactionCommand> resubmitCommands = new ArrayList<>();
        resubmitCommands.add(mock(TransactionCommand.class));
        PowerMockito.doReturn(resubmitCommands).when(transactionInvokerImpl, "extractCommandsFromQueue");

        List<TransactionCommand> testCommands = new ArrayList<>();
        testCommands.addAll(resubmitCommands);

        assertEquals(testCommands, Whitebox.invokeMethod(transactionInvokerImpl, "extractCommands"));
    }

    @Test
    public void testExtractCommandsFromQueue() throws Exception {
        TransactionCommand command = mock(TransactionCommand.class);
        inputQueue.add(command);
        MemberModifier.field(TransactionInvokerImpl.class, "inputQueue").set(transactionInvokerImpl, inputQueue);
        List<TransactionCommand> testResult = new ArrayList<>();
        testResult.add(command);
        assertEquals(testResult, Whitebox.invokeMethod(transactionInvokerImpl, "extractCommandsFromQueue"));
    }

    @Test
    public void testForgetSuccessfulTransactions() throws Exception {
        ReadWriteTransaction transaction = mock(ReadWriteTransaction.class);
        successfulTransactionQueue.add(transaction);
        pendingTransactions.add(transaction);
        transactionToCommand.put(transaction, mock(TransactionCommand.class));
        MemberModifier.field(TransactionInvokerImpl.class, "successfulTransactionQueue").set(transactionInvokerImpl, successfulTransactionQueue);
        MemberModifier.field(TransactionInvokerImpl.class, "pendingTransactions").set(transactionInvokerImpl, pendingTransactions);
        MemberModifier.field(TransactionInvokerImpl.class, "transactionToCommand").set(transactionInvokerImpl, transactionToCommand);

        Whitebox.invokeMethod(transactionInvokerImpl, "forgetSuccessfulTransactions");

        List<ReadWriteTransaction> testPendingTransactions = Whitebox.getInternalState(transactionInvokerImpl, "pendingTransactions");
        Map<ReadWriteTransaction, TransactionCommand> testTransactionToCommand = Whitebox.getInternalState(transactionInvokerImpl, "transactionToCommand");
        assertTrue(testPendingTransactions.isEmpty());
        assertTrue(testTransactionToCommand.isEmpty());
    }

    @Test
    public void testClose() throws Exception {
        MemberModifier.field(TransactionInvokerImpl.class, "executor").set(transactionInvokerImpl, executor);
        MemberModifier.field(TransactionInvokerImpl.class, "runTask").set(transactionInvokerImpl, runTask);
        doNothing().when(executor).shutdown();
        transactionInvokerImpl.close();
        verify(executor).shutdown();
    }
}
