/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.reconciliation;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.ovsdb.southbound.reconciliation.connection.ConnectionReconciliationTask;
import org.opendaylight.ovsdb.southbound.reconciliation.connection.ConnectionReconciliationTaskManager;
import org.opendaylight.yangtools.util.concurrent.SpecialExecutors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

/**
 * Created by Anil Vishnoi (avishnoi@Brocade.com) on 3/9/16.
 */
public class ReconciliationManager implements AutoCloseable{
    private static final Logger LOG = LoggerFactory.getLogger(ReconciliationManager.class);

    private final DataBroker db;
    private final ExecutorService reconcilers;
    private final ScheduledExecutorService taskTriager;

    private final TaskManager connectionTaskManager = new ConnectionReconciliationTaskManager();

    public ReconciliationManager(final DataBroker db) {
        this.db = db;
        reconcilers = SpecialExecutors.newBoundedCachedThreadPool(10, 5000, "ovsdb-reconciler");

        ThreadFactory threadFact = new ThreadFactoryBuilder()
                .setNameFormat("ovsdb-reconciliation-task-triager-%d").build();
        taskTriager = Executors.newSingleThreadScheduledExecutor(threadFact);
    }

    public boolean isEnqueued(final ReconciliationTask task) {
        return getTaskManager(task).isTaskAlreadyQueued(task.nodeIid);
    }

    public void enqueue(final ReconciliationTask task) {
        LOG.trace("Reconciliation task submitted for execution {}",task);
        getTaskManager(task).cacheTask(
                task.nodeIid,
                reconcilers.submit(task));
    }

    public void enqueueForRetry(final ReconciliationTask task) {
        LOG.trace("Reconciliation task re-queued for re-execution {}",task);
        getTaskManager(task).cacheTask(
                task.nodeIid,
                taskTriager.schedule(new Runnable() {
                    @Override
                    public void run() {
                        task.checkReadinessAndProcess();
                    }
                }, task.retryDelayInMills(), TimeUnit.MILLISECONDS)
        );
    }

    public void dequeue(final ReconciliationTask task) {
        getTaskManager(task).cancelTask(task.nodeIid);
    }

    public DataBroker getDataBroker() {
        return this.db;
    }

    @Override
    public void close() throws Exception {
        if(this.reconcilers != null)  {
            this.reconcilers.shutdownNow();
        }

        if(this.taskTriager != null) {
            this.taskTriager.shutdownNow();
        }
    }

    private TaskManager getTaskManager(ReconciliationTask task) {
        if(task instanceof ConnectionReconciliationTask) {
            return this.connectionTaskManager;
        }
        return null;
    }
}
