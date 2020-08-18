/*
 * Copyright (c) 2015, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.transactions.md;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.binding.api.Transaction;
import org.opendaylight.mdsal.binding.api.TransactionChain;
import org.opendaylight.mdsal.binding.api.TransactionChainListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*  TODO:
 * Copied over as-is from southbound plugin. Good candidate to be common
 * when refactoring code.
 */
public class TransactionInvokerImpl implements TransactionInvoker,TransactionChainListener, Runnable, AutoCloseable,
        Thread.UncaughtExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(TransactionInvokerImpl.class);
    private static final int QUEUE_SIZE = 10000;

    private final DataBroker db;
    private final BlockingQueue<TransactionCommand> inputQueue = new LinkedBlockingQueue<>(QUEUE_SIZE);
    private final BlockingQueue<Transaction> failedTransactionQueue = new LinkedBlockingQueue<>(QUEUE_SIZE);
    private final ExecutorService executor;

    @GuardedBy("this")
    private final Map<ReadWriteTransaction,TransactionCommand> transactionToCommand = new HashMap<>();
    @GuardedBy("this")
    private final List<ReadWriteTransaction> pendingTransactions = new ArrayList<>();

    private TransactionChain chain;
    //This is made volatile as it is accessed from uncaught exception handler thread also
    private volatile ReadWriteTransaction transactionInFlight = null;
    private Iterator<TransactionCommand> commandIterator = null;

    public TransactionInvokerImpl(final DataBroker db) {
        this.db = db;
        this.chain = db.createTransactionChain(this);
        ThreadFactory threadFact = new ThreadFactoryBuilder().setNameFormat("transaction-invoker-impl-%d")
                .setUncaughtExceptionHandler(this).build();
        executor = Executors.newSingleThreadExecutor(threadFact);
        //Using the execute method here so that un caught exception handler gets triggered upon exception.
        //The other way to do it is using submit method and wait on the future to catch any exceptions
        executor.execute(this);
    }

    @Override
    public void invoke(final TransactionCommand command) {
        // TODO what do we do if queue is full?
        if (!inputQueue.offer(command)) {
            LOG.error("inputQueue is full (size: {}) - could not offer {}", inputQueue.size(), command);
        }
    }

    @Override
    public void onTransactionChainFailed(final TransactionChain txChain,
            final Transaction transaction, final Throwable cause) {
        offerFailedTransaction(transaction);
    }

    @Override
    public void onTransactionChainSuccessful(final TransactionChain txChain) {
        // NO OP
    }

    @Override
    public void run() {
        while (true) {
            final List<TransactionCommand> commands;
            try {
                commands = extractCommands();
            } catch (InterruptedException e) {
                LOG.warn("Extracting commands was interrupted.", e);
                continue;
            }
            commandIterator = commands.iterator();
            try {
                while (commandIterator.hasNext()) {
                    executeCommand(commandIterator.next());
                }
                transactionInFlight = null;
            } catch (IllegalStateException e) {
                if (transactionInFlight != null) {
                    // TODO: This method should distinguish exceptions on which the command should be
                    // retried from exceptions on which the command should NOT be retried.
                    // Then it should retry only the commands which should be retried, otherwise
                    // this method will retry commands which will never be successful forever.
                    offerFailedTransaction(transactionInFlight);
                }
                transactionInFlight = null;
                LOG.warn("Failed to process an update notification from OVS.", e);
            }
        }
    }

    private synchronized void executeCommand(final TransactionCommand command) {
        final ReadWriteTransaction transaction = chain.newReadWriteTransaction();
        transactionInFlight = transaction;
        recordPendingTransaction(command, transaction);
        command.execute(transaction);
        FluentFuture<?> ft = transaction.commit();
        command.setTransactionResultFuture(ft);
        ft.addCallback(new FutureCallback<Object>() {
            @Override
            public void onSuccess(final Object result) {
                forgetSuccessfulTransaction(transaction);
                command.onSuccess();
            }

            @Override
            public void onFailure(final Throwable throwable) {
                // NOOP - handled by failure of transaction chain
                command.onFailure();
            }
        }, MoreExecutors.directExecutor());
    }

    private void offerFailedTransaction(final Transaction transaction) {
        if (!failedTransactionQueue.offer(transaction)) {
            LOG.warn("failedTransactionQueue is full (size: {})", failedTransactionQueue.size());
        }
    }

    private List<TransactionCommand> extractResubmitCommands() {
        List<TransactionCommand> commands = new ArrayList<>();
        synchronized (this) {
            Transaction transaction = failedTransactionQueue.poll();
            if (transaction != null) {
                int index = pendingTransactions.lastIndexOf(transaction);
                //This logic needs to be revisited. Is it ok to resubmit these things again ?
                //are these operations idempotent ?
                //Does the transaction chain execute n+1th if nth one threw error ?
                List<ReadWriteTransaction> transactions =
                        pendingTransactions.subList(index, pendingTransactions.size() - 1);
                for (ReadWriteTransaction tx: transactions) {
                    commands.add(transactionToCommand.get(tx));
                }
                resetTransactionQueue();
            }
        }
        if (commandIterator != null) {
            while (commandIterator.hasNext()) {
                commands.add(commandIterator.next());
            }
        }
        return commands;
    }

    private void resetTransactionQueue() {
        chain.close();
        chain = db.createTransactionChain(this);
        pendingTransactions.clear();
        transactionToCommand.clear();
        failedTransactionQueue.clear();
    }

    synchronized void forgetSuccessfulTransaction(final ReadWriteTransaction transaction) {
        pendingTransactions.remove(transaction);
        transactionToCommand.remove(transaction);
    }

    private synchronized void recordPendingTransaction(final TransactionCommand command,
            final ReadWriteTransaction transaction) {
        transactionToCommand.put(transaction, command);
        pendingTransactions.add(transaction);
    }

    private List<TransactionCommand> extractCommands() throws InterruptedException {
        List<TransactionCommand> commands = extractResubmitCommands();
        if (!commands.isEmpty() && inputQueue.isEmpty()) {
            //we got some commands to be executed let us not sit and wait on empty queue
            return commands;
        }
        //pull commands from queue if not empty , otherwise wait for commands to be placed in queue.
        commands.addAll(extractCommandsFromQueue());
        return commands;
    }

    private List<TransactionCommand> extractCommandsFromQueue() throws InterruptedException {
        List<TransactionCommand> result = new ArrayList<>();
        TransactionCommand command = inputQueue.take();
        result.add(command);
        inputQueue.drainTo(result);
        return result;
    }

    @Override
    public void close() throws Exception {
        this.chain.close();
        this.executor.shutdown();
    }

    @Override
    public void uncaughtException(final Thread thread, final Throwable ex) {
        LOG.error("Failed to execute hwvtep transact command, re-submitting the transaction again", ex);
        if (transactionInFlight != null) {
            offerFailedTransaction(transactionInFlight);
        }
        transactionInFlight = null;
        executor.execute(this);
    }
}
