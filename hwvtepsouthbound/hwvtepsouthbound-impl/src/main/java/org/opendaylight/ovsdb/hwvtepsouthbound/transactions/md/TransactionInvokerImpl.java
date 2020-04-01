/*
 * Copyright (c) 2015, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound.transactions.md;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.DataStoreUnavailableException;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepConnectionManager;
import org.opendaylight.ovsdb.utils.mdsal.utils.Scheduler;
import org.opendaylight.ovsdb.utils.mdsal.utils.TransactionProxy;
import org.opendaylight.ovsdb.utils.mdsal.utils.TransactionProxyTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/*  TODO:
 * Copied over as-is from southbound plugin. Good candidate to be common
 * when refactoring code.
 */
public class TransactionInvokerImpl implements TransactionInvoker, TransactionChainListener, Runnable, AutoCloseable,
        Thread.UncaughtExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger("HwvtepEventLogger");
    private static final String NL = System.getProperty("line.separator");
    private static String controllerExceptionsPkg = "org.opendaylight.controller.cluster.datastore.exceptions";
    private static final int QUEUE_SIZE = 10000;
    private static final Map<Long, String> LATENCY_COUNTER_NAMES = new ConcurrentHashMap<>();
    private static final AtomicInteger THRAED_ID_COUNTER = new AtomicInteger(0);

    private BindingTransactionChain chain;
    private DataBroker db;
    private LinkedBlockingQueue<TransactionCommand> inputQueue;
    private BlockingQueue<ReadWriteTransaction> successfulTransactionQueue
            = new LinkedBlockingQueue<>(QUEUE_SIZE);
    private BlockingQueue<TransactionProxy> failedTransactionQueue
            = new LinkedBlockingQueue<>(QUEUE_SIZE);
    private ExecutorService executor;
    private Map<ReadWriteTransaction, TransactionCommand> transactionToCommand
            = new HashMap<>();
    private List<ReadWriteTransaction> pendingTransactions = new ArrayList<>();
    private final AtomicBoolean runTask = new AtomicBoolean(true);
    private volatile TransactionProxy transactionInFlight = null;
    private volatile boolean cleanupExpiredProxies = false;
    private final TransactionProxyTracker transactionProxyTracker = new TransactionProxyTracker();
    private volatile boolean queueFilled = false;
    private Iterator<TransactionCommand> commandIterator = null;
    private TransactionCommand currentCmd = null;
    private TransactionInvokerProxy invokerProxy;
    //private final int id;
    private final Cache<TransactionProxy, FailedCmd> failedCmds = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .build();

    @Override
    @SuppressWarnings("checkstyle:ParameterName")
    public void uncaughtException(Thread t, Throwable e) {
    }

    @SuppressFBWarnings(value = "SIC_INNER_SHOULD_BE_STATIC_NEEDS_THIS")
    class FailedCmd {
        boolean askTimedout = false;
        boolean retryCurrentCmd = false;
        TransactionCommand cmd = null;
        boolean readFailed = false;
        Throwable throwable;

        FailedCmd(Throwable inThrowable, TransactionProxy transactionProxy) {
            this.throwable = inThrowable;
            cmd = transactionToCommand.getOrDefault(transactionProxy, currentCmd);
            if (cmd != null) {
                cmd.onFailure();
                //ErrorLog.addToLog(TransactionLog.ADD, cmd, getCustomStackTrace(throwable));
            }
            if (throwable instanceof IllegalStateException
                    || throwable.getCause() instanceof IllegalStateException) {
                retryCurrentCmd = true;
            }
            if (throwable instanceof IllegalArgumentException
                    || throwable.getCause() instanceof IllegalArgumentException) {
                retryCurrentCmd = true;
            }
            if (throwable.getCause() instanceof ReadFailedException) {
                readFailed = true;
                retryCurrentCmd = true;
            }
            if (clsName(throwable).contains("AskTime") || clsName(throwable.getCause()).contains("AskTime")) {
                askTimedout = true;
            }
            if (throwable instanceof DataStoreUnavailableException
                    || throwable.getCause() instanceof DataStoreUnavailableException) {
                askTimedout = true;
            }
            String pkgName = pkgName(throwable);
            String pkgName2 = pkgName(throwable.getCause());
            if (controllerExceptionsPkg.equals(pkgName) || controllerExceptionsPkg.equals(pkgName2)) {
                askTimedout = true;
            }
            if (askTimedout) {
                retryCurrentCmd = true;
                incrementStaticCounter("transaction.chain.failed.asktimedout");
            }
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    public TransactionInvokerImpl(DataBroker db, TransactionInvokerProxy transactionInvokerProxy, int queueSize) {
        this.db = db;
        //this.id = THRAED_ID_COUNTER.incrementAndGet();
        this.invokerProxy = transactionInvokerProxy;
        this.chain = db.createTransactionChain(this);
        this.inputQueue = new LinkedBlockingQueue<>(queueSize);
        ThreadFactory threadFact = new ThreadFactoryBuilder().setNameFormat("transaction-invoker-impl-%d").build();
        executor = Executors.newSingleThreadExecutor(threadFact);
        Scheduler.getScheduledExecutorService().scheduleAtFixedRate(() -> {
            cleanupExpiredProxies = true;
        }, 10, 240, TimeUnit.SECONDS);
        executor.execute(this);
    }

    private void incrementStaticCounter(String counter) {
        //Metrics.incrementStatic("hwvtep." + id + "." + counter);
    }

    @Override
    public void invoke(final TransactionCommand command) {
        boolean addedToQueue = inputQueue.offer(command);
        if (!addedToQueue) {
            queueFilled = true;
            incrementStaticCounter("failed.transaction.queue.full");
        }
    }

    @SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD",
            justification = "This is false reported in jdk 11.0.1")
    private String clsName(Throwable clz) {
        if (clz != null) {
            return clz.getClass().getSimpleName().toLowerCase(Locale.getDefault());
        }
        return "";
    }

    @SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD",
            justification = "This is false reported in jdk 11.0.1")
    private String pkgName(Throwable clz) {
        if (clz != null) {
            return clz.getClass().getPackage().getName();
        }
        return "";
    }

    @Override
    public synchronized void onTransactionChainFailed(TransactionChain<?, ?> transactionChain,
                                                      AsyncTransaction<?, ?> transaction, Throwable throwable) {
        TransactionProxy transactionProxy = transactionProxyTracker.getProxyFor(transaction);
        if (transactionProxy != null) {
            transactionProxyTracker.clearProxyFor(transaction);
        } else {
            transactionProxy = transactionInFlight;
            transactionInFlight = null;
            LOG.error("Could not find proxy for the transaction {} again", transaction);
        }
        if (transactionProxy != null) {
            boolean offered = failedTransactionQueue.offer(transactionProxy);
            failedCmds.put(transactionProxy, new FailedCmd(throwable, transactionProxy));
            incrementStaticCounter("transaction.chain.failed");
            LOG.error("Transaction chain failed transactionId:{} . Added to failedCmds {}",
                    transaction.getIdentifier(), offered, throwable);
        }
    }

    @Override
    public void onTransactionChainSuccessful(TransactionChain<?, ?> transactionChain) {
        // NO OP

    }

    public static String getCustomStackTrace(Throwable throwable) {
        //add the class name and any message passed to constructor
        StringBuilder result = new StringBuilder("");
        result.append(throwable.getMessage());
        result.append(NL);

        //add each element of the stack trace
        int idx = 0;
        for (StackTraceElement element : throwable.getStackTrace()) {
            if (idx++ == 8) {
                break;
            }
            result.append(element);
            result.append(NL);
        }
        return result.toString();
    }

    @Override
    @SuppressFBWarnings(value = "IS2_INCONSISTENT_SYNC", justification = "")
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void run() {
        TransactionProxy transactionProxy = null;
        while (runTask.get()) {
            try {
                if (cleanupExpiredProxies) {
                    cleanupExpiredProxies = false;
                    Map<AsyncTransaction, TransactionProxy> proxyMap = transactionProxyTracker.getTransactions();
                    proxyMap.keySet().stream()
                            .filter(tx -> !transactionToCommand.containsKey(tx))
                            .forEach(tx -> transactionProxyTracker.clearProxyFor(tx));
                }
                forgetSuccessfulTransactions();
                List<TransactionCommand> commands = null;
                try {
                    commands = extractCommands();
                } catch (InterruptedException e) {
                    LOG.warn("Extracting commands was interrupted.", e);
                    continue;
                }
                if (queueFilled) {
                    queueFilled = false;
                    inputQueue.clear();
                    LOG.error("Clearing queue ");
                    incrementStaticCounter("clear.queue.and.disconnect");
                    //Disconnect those connections which are processed by this invoker
                    HwvtepConnectionManager.getAllConnectedInstances().values().stream()
                            .filter(connection -> connection.getInstanceIdentifier() != null)
                            .filter(connection -> invokerProxy.getTransactionInvokerForNode(
                                    connection.getInstanceIdentifier()) == TransactionInvokerImpl.this)
                            .forEach(connection -> connection.disconnect());
                    continue;
                }
                commandIterator = commands.iterator();
                try {
                    while (commandIterator.hasNext()) {
                        TransactionCommand command = commandIterator.next();
                        if (command.getTransactionChainRetryCount() <= 0) {
                            continue;
                        }
                        currentCmd = command;
                        TransactionProxy transaction = new TransactionProxy(chain, command, transactionProxyTracker);
                        transactionInFlight = transaction;
                        recordPendingTransaction(command, transaction);
                        long startTime = System.currentTimeMillis();
                        command.execute(transaction);
                        incrementStaticCounter("transaction.submit");
                        CheckedFuture<Void, TransactionCommitFailedException>  submittedTransaction =
                                transaction.submit();
                        Futures.addCallback(submittedTransaction, new FutureCallback<Void>() {
                            @Override
                            public void onSuccess(final Void result) {
                                command.onSuccess();
                                incrementStaticCounter("transaction.submit.success");
                                incrementLatencyCounter(startTime);
                                boolean offered = successfulTransactionQueue.offer(transaction);
                                LOG.trace("transaction.submit.success offered the transaction {}", offered);
                                transactionProxyTracker.clearProxyFor(transaction.getBackingDelegate());
                            }

                            @Override
                            public void onFailure(final Throwable throwable) {
                                command.onFailure();
                                incrementStaticCounter("transaction.submit.failed");
                                incrementLatencyCounter(startTime);
                            }
                        }, MoreExecutors.directExecutor());
                    }
                } catch (IllegalStateException e) {
                    onTransactionChainFailed(chain, transactionInFlight, e);
                }
            } catch (Throwable e) {
                LOG.error("Failed to process an update notification from hwvtep.", e);
                onTransactionChainFailed(chain, transactionInFlight, e);
                incrementStaticCounter("transaction.invoker.exception");
            }
        }
    }

    @SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD",
            justification = "This is false reported in jdk 11.0.1")
    private void incrementLatencyCounter(long startTime) {
        long endTime = System.currentTimeMillis();
        long time = endTime - startTime;
        time = time / 1000;
        if (time > 10) {
            time = 10;
        }
        if (time == 0) {
            LATENCY_COUNTER_NAMES.computeIfAbsent(
                    time, timekey -> "hwvtep.transaction.latency.msecs." + 100 * ((endTime - startTime) / 100));
        } else {
            LATENCY_COUNTER_NAMES.computeIfAbsent(time, timekey -> "hwvtep.transaction.latency.secs." + timekey);
        }
        //Metrics.incrementStatic(LATENCY_COUNTER_NAMES.get(time));
    }


    @SuppressFBWarnings(value = "SWL_SLEEP_WITH_LOCK_HELD")
    private synchronized List<TransactionCommand> extractResubmitCommands() {
        TransactionProxy transaction = failedTransactionQueue.poll();
        boolean retryCurrentCmd = false;
        boolean askTimedout = false;
        boolean readFailed = false;
        List<TransactionCommand> commands = new ArrayList<>();
        if (transaction != null) {
            FailedCmd failedCmd = failedCmds.getIfPresent(transaction);
            if (failedCmd != null) {
                failedCmds.invalidate(transaction);
                retryCurrentCmd = failedCmd.retryCurrentCmd;
                askTimedout = failedCmd.askTimedout;
                readFailed = failedCmd.readFailed;
            }
            boolean skipCurrentCmd = !retryCurrentCmd;
            int index = pendingTransactions.lastIndexOf(transaction);
            if (!askTimedout && index > 0) {
                TransactionProxy transactionProxy = (TransactionProxy) pendingTransactions.get(index);
                transactionProxy.getTxHistory()
                        .forEach(tx -> LOG.error("Failed tx attempted {} {}", transaction.getIdentifier(), tx));
            }
            if (skipCurrentCmd) {
                transactionToCommand.remove(transaction);
                index = index + 1;
            }
            if (index >= 0 && index < pendingTransactions.size()) {
                List<ReadWriteTransaction> transactions =
                        pendingTransactions.subList(index, pendingTransactions.size());
                for (ReadWriteTransaction tx : transactions) {
                    LOG.error("Adding the pending command to the queue again");
                    commands.add(transactionToCommand.remove(tx));
                    transactionProxyTracker.clearProxyFor(((TransactionProxy) transaction).getBackingDelegate());
                }
            }
            resetTransactionQueue();
        }
        if (readFailed) {
            try {
                LOG.error("Prev command read failed sleeping for 1 sec");
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                LOG.warn("interrupted sleep");
            }
        }
        if (askTimedout) {
            try {
                LOG.error("Prev command ask timedout sleeping for 60 sec");
                Thread.sleep(60000L);
            } catch (InterruptedException e) {
                LOG.warn("interrupted sleep");
            }
        }
        if (commandIterator != null) {
            while (commandIterator.hasNext()) {
                commands.add(commandIterator.next());
            }
        }
        askTimedout = false;
        retryCurrentCmd = false;
        readFailed = false;
        return commands;
    }

    private void resetTransactionQueue() {
        incrementStaticCounter("transaction.chain.reset");
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
        if (commands.isEmpty()) {
            commands.addAll(extractCommandsFromQueue());
        }
        return commands;
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private List<TransactionCommand> extractCommandsFromQueue() throws InterruptedException {
        List<TransactionCommand> result = new ArrayList<>();
        TransactionCommand command = null;
        do {
            try {
                command = inputQueue.poll(1, TimeUnit.SECONDS);
                if (failedTransactionQueue.peek() != null) {
                    LOG.error("Got a failed command while processing input queue");
                    result.addAll(extractResubmitCommands());
                    break;
                }
            } catch (Exception e) {
                LOG.error("Failed to get element from queue ", e);
            }
        } while (command == null);

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
