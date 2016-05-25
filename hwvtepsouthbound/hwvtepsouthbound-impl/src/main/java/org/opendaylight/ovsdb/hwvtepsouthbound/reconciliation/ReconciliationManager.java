/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.reconciliation;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yangtools.util.concurrent.SpecialExecutors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

/**
 * Copied from org.opendaylight.ovsdb.southbound.reconciliation.ReconciliationManager
 * 
 * TODO: A common reconciliatonManager for all plugins
 */
public class ReconciliationManager implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(ReconciliationManager.class);

    private static final int NO_OF_RECONCILER = 10;
    private static final int RECON_TASK_QUEUE_SIZE = 5000;

    private final DataBroker db;
    private final ExecutorService reconcilers;
    private final ScheduledExecutorService taskTriager;

    private final ReconciliationTaskManager reconTaskManager = new ReconciliationTaskManager();

    public ReconciliationManager(final DataBroker db) {
        this.db = db;
        reconcilers = SpecialExecutors.newBoundedCachedThreadPool(NO_OF_RECONCILER, RECON_TASK_QUEUE_SIZE, "ovsdb-reconciler");

        ThreadFactory threadFact = new ThreadFactoryBuilder()
                .setNameFormat("ovsdb-recon-task-triager-%d").build();
        taskTriager = Executors.newSingleThreadScheduledExecutor(threadFact);
    }

    public boolean isEnqueued(final ReconciliationTask task) {
        return reconTaskManager.isTaskQueued(task);
    }

    public void enqueue(final ReconciliationTask task) {
        LOG.trace("Reconciliation task submitted for execution {}",task);
        reconTaskManager.cacheTask(task, reconcilers.submit(task));
    }

    public void enqueueForRetry(final ReconciliationTask task) {
        LOG.trace("Reconciliation task re-queued for re-execution {}",task);
        reconTaskManager.cacheTask(task, taskTriager.schedule(
                new Runnable() {
                    @Override
                    public void run() {
                        task.checkReadinessAndProcess();
                    }
                }, task.retryDelayInMills(), TimeUnit.MILLISECONDS
            )
        );
    }

    public void dequeue(final ReconciliationTask task) {
        reconTaskManager.cancelTask(task);
    }

    public DataBroker getDb() {
        return db;
    }

    @Override
    public void close() throws Exception {
        if (this.reconcilers != null) {
            this.reconcilers.shutdownNow();
        }

        if (this.taskTriager != null) {
            this.taskTriager.shutdownNow();
        }
    }
}
