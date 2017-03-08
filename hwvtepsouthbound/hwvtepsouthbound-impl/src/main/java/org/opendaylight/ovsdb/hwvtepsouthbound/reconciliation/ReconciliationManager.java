/*
 * Copyright © 2016, 2017 Brocade Communications Systems, Inc. and others.  All rights reserved.
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
 * This class provides the implementation of ovsdb southbound plugins
 * configuration reconciliation engine. This engine provide interfaces
 * to enqueue (one time retry)/ enqueueForRetry(periodic retry)/ dequeue
 * (remove from retry queue) reconciliation task. Reconciliation task can
 * be a connection reconciliation or configuration reconciliation of any
 * ovsdb managed resource like bridge, termination point etc. This engine
 * execute all the reconciliation task through a fixed size thread pool.
 * If submitted task need to be retry after a periodic interval they are
 * submitted to a single thread executor to periodically wake up and check
 * if task is ready for execution.
 * Ideally, addition of any type of reconciliation task should not require
 * any change in this reconciliation manager execution engine.
 *
 * 3-Node Cluster:
 * Reconciliation manager is agnostic of whether it's running in single
 * node cluster or 3-node cluster. It's a responsibility of the task
 * submitter to make sure that it submit the task for reconciliation only
 * if it's an owner of that device EXCEPT controller initiated Connection.
 * Reconciliation of controller initiated connection should be done by all
 * the 3-nodes in the cluster, because connection to individual controller
 * can be interrupted for various reason.
 *
 * Created by Anil Vishnoi (avishnoi@Brocade.com) on 3/9/16.
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
                () -> task.checkReadinessAndProcess(), task.retryDelayInMills(), TimeUnit.MILLISECONDS
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
