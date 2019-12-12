/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.transactions.md;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactionInvokerImpl implements TransactionInvoker,TransactionChainListener, Runnable, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(TransactionInvokerImpl.class);
    private static final int QUEUE_SIZE = 10000;

    private final DataBroker db;
    private final BlockingQueue<TransactionCommand> inputQueue = new LinkedBlockingQueue<>(QUEUE_SIZE);
    private final BlockingQueue<AsyncTransaction<?, ?>> failedTransactionQueue = new LinkedBlockingQueue<>(QUEUE_SIZE);
    private final ExecutorService executor;

    private final AtomicBoolean runTask = new AtomicBoolean(true);

    @GuardedBy("this")
    private final Map<ReadWriteTransaction, TransactionCommand> transactionToCommand = new HashMap<>();
    @GuardedBy("this")
    private final List<ReadWriteTransaction> pendingTransactions = new ArrayList<>();

    private BindingTransactionChain chain;

    public TransactionInvokerImpl(final DataBroker db) {
        this.db = db;
        this.chain = db.createTransactionChain(this);
        ThreadFactory threadFact = new ThreadFactoryBuilder().setNameFormat("transaction-invoker-impl-%d").build();
        executor = Executors.newSingleThreadExecutor(threadFact);
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
    public void onTransactionChainFailed(final TransactionChain<?, ?> chainArg,
            final AsyncTransaction<?, ?> transaction, final Throwable cause) {
        LOG.error("Failed to write operational topology", cause);
        offerFailedTransaction(transaction);
    }

    @Override
    public void onTransactionChainSuccessful(final TransactionChain<?, ?> chainArg) {
        // NO OP
    }

    @Override
    public void run() {
        while (runTask.get()) {
            final List<TransactionCommand> commands;
            try {
                commands = extractCommands();
            } catch (InterruptedException e) {
                LOG.warn("Extracting commands was interrupted.", e);
                continue;
            }

            ReadWriteTransaction transactionInFlight = null;
            try {
                for (TransactionCommand command: commands) {
                    synchronized (this) {
                        final ReadWriteTransaction transaction = chain.newReadWriteTransaction();
                        transactionInFlight = transaction;
                        recordPendingTransaction(command, transaction);
                        command.execute(transaction);
                        Futures.addCallback(transaction.submit(), new FutureCallback<Void>() {
                            @Override
                            public void onSuccess(final Void result) {
                                forgetSuccessfulTransaction(transaction);
                                command.onSuccess();
                            }

                            @Override
                            public void onFailure(final Throwable throwable) {
                                command.onFailure(throwable);
                                // NOOP - handled by failure of transaction chain
                            }
                        }, MoreExecutors.directExecutor());
                    }
                }
            } catch (IllegalStateException e) {
                if (transactionInFlight != null) {
                    // TODO: This method should distinguish exceptions on which the command should be
                    // retried from exceptions on which the command should NOT be retried.
                    // Then it should retry only the commands which should be retried, otherwise
                    // this method will retry commands which will never be successful forever.
                    offerFailedTransaction(transactionInFlight);
                }
                LOG.warn("Failed to process an update notification from OVS.", e);
            }
        }
    }

    private void offerFailedTransaction(final AsyncTransaction<?, ?> transaction) {
        if (!failedTransactionQueue.offer(transaction)) {
            LOG.warn("failedTransactionQueue is full (size: {})", failedTransactionQueue.size());
        }
    }

    @VisibleForTesting
    synchronized List<TransactionCommand> extractResubmitCommands() {
        AsyncTransaction<?, ?> transaction = failedTransactionQueue.poll();
        List<TransactionCommand> commands = new ArrayList<>();
        if (transaction != null) {
            int index = pendingTransactions.lastIndexOf(transaction);
            List<ReadWriteTransaction> transactions =
                    pendingTransactions.subList(index, pendingTransactions.size() - 1);
            for (ReadWriteTransaction tx: transactions) {
                commands.add(transactionToCommand.get(tx));
            }
            resetTransactionQueue();
        }
        return commands;
    }

    @VisibleForTesting
    synchronized void resetTransactionQueue() {
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
        commands.addAll(extractCommandsFromQueue());
        return commands;
    }

    @VisibleForTesting
    List<TransactionCommand> extractCommandsFromQueue() throws InterruptedException {
        List<TransactionCommand> result = new ArrayList<>();
        TransactionCommand command = inputQueue.take();
        result.add(command);
        inputQueue.drainTo(result);
        return result;
    }

    @Override
    public void close() throws InterruptedException {
        this.chain.close();
        this.executor.shutdown();
        if (!this.executor.awaitTermination(1, TimeUnit.SECONDS)) {
            runTask.set(false);
            this.executor.shutdownNow();
        }
    }
}
