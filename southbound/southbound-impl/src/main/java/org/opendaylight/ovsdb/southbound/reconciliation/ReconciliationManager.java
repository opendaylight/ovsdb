/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.reconciliation;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionManager;
import org.opendaylight.ovsdb.southbound.reconciliation.connection.ConnectionReconciliation;
import org.opendaylight.yangtools.util.ExecutorServiceUtil;
import org.opendaylight.yangtools.util.concurrent.SpecialExecutors;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.*;

/**
 * Created by Anil Vishnoi (avishnoi@Brocade.com) on 3/9/16.
 */
public class ReconciliationManager implements AutoCloseable{
    private static final Logger LOG = LoggerFactory.getLogger(ReconciliationManager.class);

    private final ExecutorService reconcilers;
    private final ScheduledExecutorService taskTriager;


    public ReconciliationManager() {
        reconcilers = SpecialExecutors.newBoundedCachedThreadPool(10, 5000, "ovsdb-reconciler");

        ThreadFactory threadFact = new ThreadFactoryBuilder().setNameFormat("ovsdb-task-triager-%d").build();
        taskTriager = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors(), threadFact);
    }

    public void enqueue(final ReconciliationTask task) {
        LOG.trace("Reconciliation task submitted for execution {}",task);
        reconcilers.submit(task);
    }

    public void enqueueForRetry(final ReconciliationTask task) {
        LOG.trace("Reconciliation task re-queued for re-execution {}",task);
        taskTriager.schedule(new Runnable() {
            @Override
            public void run() {
                if(task.isTaskReadyForRetry()) {
                    //If task is ready for retry again, submit it for immediate execution
                    reconcilers.submit(task);
                } else {
                    //If task is not ready for retry at this time, submit it for delayed execution
                    // Single thread will periodically check if there is any reconciliation task
                    // that is ready for execution.
                    taskTriager.schedule(task, task.retryDelayInMills(), TimeUnit.MILLISECONDS);
                }
            }
        }, task.retryDelayInMills(), TimeUnit.MILLISECONDS);
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
}
