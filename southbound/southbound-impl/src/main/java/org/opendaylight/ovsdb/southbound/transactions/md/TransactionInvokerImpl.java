/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.southbound.transactions.md;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
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
    private BindingTransactionChain chain;
    private DataBroker db;
    private BlockingQueue<TransactionCommand> inputQueue = new LinkedBlockingQueue<>(QUEUE_SIZE);
    private BlockingQueue<ReadWriteTransaction> successfulTransactionQueue
        = new LinkedBlockingQueue<>(QUEUE_SIZE);
    private BlockingQueue<AsyncTransaction<?, ?>> failedTransactionQueue
        = new LinkedBlockingQueue<>(QUEUE_SIZE);
    private ExecutorService executor;
    private Map<ReadWriteTransaction,TransactionCommand> transactionToCommand
        = new HashMap<>();
    private List<ReadWriteTransaction> pendingTransactions = new ArrayList<>();
    private final AtomicBoolean runTask = new AtomicBoolean(true);

    public TransactionInvokerImpl(DataBroker db) {
        this.db = db;
        this.chain = db.createTransactionChain(this);
        ThreadFactory threadFact = new ThreadFactoryBuilder().setNameFormat("transaction-invoker-impl-%d").build();
        executor = Executors.newSingleThreadExecutor(threadFact);
        executor.submit(this);
    }

    @Override
    public void invoke(final TransactionCommand command) {
        // TODO what do we do if queue is full?
        inputQueue.offer(command);
    }

    @Override
    public void onTransactionChainFailed(TransactionChain<?, ?> chainArg,
            AsyncTransaction<?, ?> transaction, Throwable cause) {
        failedTransactionQueue.offer(transaction);
    }

    @Override
    public void onTransactionChainSuccessful(TransactionChain<?, ?> chainArg) {
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
                    Futures.addCallback(transaction.submit(), new FutureCallback<Void>() {
                        @Override
                        public void onSuccess(final Void result) {
                            successfulTransactionQueue.offer(transaction);
                        }

                        @Override
                        public void onFailure(final Throwable throwable) {
                            // NOOP - handled by failure of transaction chain
                        }
                    });
                }
            } catch (IllegalStateException e) {
                if (transactionInFlight != null) {
                    // TODO: This method should distinguish exceptions on which the command should be
                    // retried from exceptions on which the command should NOT be retried.
                    // Then it should retry only the commands which should be retried, otherwise
                    // this method will retry commands which will never be successful forever.
                    failedTransactionQueue.offer(transactionInFlight);
                }
                LOG.warn("Failed to process an update notification from OVS.", e);
            }
        }
    }

    private List<TransactionCommand> extractResubmitCommands() {
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

    private void resetTransactionQueue() {
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

    private List<TransactionCommand> extractCommandsFromQueue() throws InterruptedException {
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
    public void close() throws Exception {
        this.executor.shutdown();
        if (!this.executor.awaitTermination(1, TimeUnit.SECONDS)) {
            runTask.set(false);
            this.executor.shutdownNow();
        }
    }
}
