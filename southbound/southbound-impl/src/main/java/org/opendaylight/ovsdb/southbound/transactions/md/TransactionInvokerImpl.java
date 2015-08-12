/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.southbound.transactions.md;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;

import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ThreadFactoryBuilder;


public class TransactionInvokerImpl implements TransactionInvoker,TransactionChainListener, Runnable, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(TransactionInvokerImpl.class);
    private static final int QUEUE_SIZE = 10000;
    private BindingTransactionChain chain;
    private DataBroker db;
    private BlockingQueue<TransactionCommand> inputQueue = new LinkedBlockingQueue<TransactionCommand>(QUEUE_SIZE);
    private BlockingQueue<ReadWriteTransaction> successfulTransactionQueue
        = new LinkedBlockingQueue<ReadWriteTransaction>(QUEUE_SIZE);
    private BlockingQueue<AsyncTransaction<?, ?>> failedTransactionQueue
        = new LinkedBlockingQueue<AsyncTransaction<?, ?>>(QUEUE_SIZE);
    private ExecutorService executor;
    private Map<ReadWriteTransaction,TransactionCommand> transactionToCommand
        = new HashMap<ReadWriteTransaction,TransactionCommand>();
    private List<ReadWriteTransaction> pendingTransactions = new ArrayList<ReadWriteTransaction>();

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
    public void onTransactionChainFailed(TransactionChain<?, ?> chain,
            AsyncTransaction<?, ?> transaction, Throwable cause) {
        failedTransactionQueue.offer(transaction);
    }

    @Override
    public void onTransactionChainSuccessful(TransactionChain<?, ?> chain) {
        // NO OP

    }

    @Override
    public void run() {
        while (true) {
            forgetSuccessfulTransactions();
            try {
                List<TransactionCommand> commands = extractCommands();
                for (TransactionCommand command: commands) {
                    final ReadWriteTransaction transaction = chain.newReadWriteTransaction();
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
            } catch (Exception e) {
                LOG.warn("Exception invoking Transaction: ", e);
            }
        }
    }

    private List<TransactionCommand> extractResubmitCommands() {
        AsyncTransaction<?, ?> transaction = failedTransactionQueue.poll();
        List<TransactionCommand> commands = new ArrayList<TransactionCommand>();
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
        pendingTransactions = new ArrayList<ReadWriteTransaction>();
        transactionToCommand = new HashMap<ReadWriteTransaction,TransactionCommand>();
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
        List<TransactionCommand> result = new ArrayList<TransactionCommand>();
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
    }
}
