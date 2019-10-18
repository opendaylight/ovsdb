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
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.binding.api.Transaction;
import org.opendaylight.mdsal.binding.api.TransactionChain;
import org.opendaylight.mdsal.binding.api.TransactionChainListener;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TransactionInvokerImpl implements TransactionInvoker, TransactionChainListener, Runnable, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(TransactionInvokerImpl.class);
    private static final int QUEUE_SIZE = 10000;
    private TransactionChain chain;
    private final DataBroker db;
    private final BlockingQueue<TransactionCommand> inputQueue = new LinkedBlockingQueue<>(QUEUE_SIZE);
    private final BlockingQueue<ReadWriteTransaction> successfulTransactionQueue
        = new LinkedBlockingQueue<>(QUEUE_SIZE);
    private final BlockingQueue<Transaction> failedTransactionQueue
        = new LinkedBlockingQueue<>(QUEUE_SIZE);
    private final ExecutorService executor;
    private Map<ReadWriteTransaction,TransactionCommand> transactionToCommand
        = new HashMap<>();
    private List<ReadWriteTransaction> pendingTransactions = new ArrayList<>();
    private final AtomicBoolean runTask = new AtomicBoolean(true);

    public TransactionInvokerImpl(DataBroker db) {
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
    public void onTransactionChainFailed(@NonNull TransactionChain transactionChain, @NonNull Transaction transaction,
            @NonNull Throwable cause) {
        LOG.error("Failed to write operational topology", cause);
        offerFailedTransaction(transaction);
    }

    @Override
    public void onTransactionChainSuccessful(TransactionChain chainArg) {
        // NO OP
    }

    @Override
    public void run() {
        while (runTask.get()) {
            forgetSuccessfulTransactions();

            List<TransactionCommand> commands = null;
            try {
                commands = extractCommands();
            } catch (InterruptedException e) {
                LOG.warn("Extracting commands was interrupted.", e);
                continue;
            }

            ReadWriteTransaction transactionInFlight = null;
            try {
                for (TransactionCommand command: commands) {
                    final ReadWriteTransaction transaction = chain.newReadWriteTransaction();
                    transactionInFlight = transaction;
                    recordPendingTransaction(command, transaction);
                    command.execute(transaction);
                    Futures.addCallback(transaction.commit(), new FutureCallback<CommitInfo>() {
                        @Override
                        public void onSuccess(final CommitInfo result) {
                            if (!successfulTransactionQueue.offer(transaction)) {
                                LOG.error("successfulTransactionQueue is full (size: {}) - could not offer {}",
                                        successfulTransactionQueue.size(), transaction);
                            }
                            command.onSuccess();
                        }

                        @Override
                        public void onFailure(final Throwable throwable) {
                            command.onFailure(throwable);
                            // NOOP - handled by failure of transaction chain
                        }
                    }, MoreExecutors.directExecutor());
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

    private void offerFailedTransaction(Transaction transaction) {
        if (!failedTransactionQueue.offer(transaction)) {
            LOG.warn("failedTransactionQueue is full (size: {})", failedTransactionQueue.size());
        }
    }

    @VisibleForTesting
    List<TransactionCommand> extractResubmitCommands() {
        Transaction transaction = failedTransactionQueue.poll();
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
    void resetTransactionQueue() {
        chain.close();
        chain = db.createTransactionChain(this);
        pendingTransactions = new ArrayList<>();
        transactionToCommand = new HashMap<>();
        failedTransactionQueue.clear();
        successfulTransactionQueue.clear();
    }

    private void recordPendingTransaction(TransactionCommand command,
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
        while (command != null) {
            result.add(command);
            command = inputQueue.poll();
        }
        return result;
    }

    private void forgetSuccessfulTransactions() {
        ReadWriteTransaction transaction = successfulTransactionQueue.poll();
        while (transaction != null) {
            pendingTransactions.remove(transaction);
            transactionToCommand.remove(transaction);
            transaction = successfulTransactionQueue.poll();
        }
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
