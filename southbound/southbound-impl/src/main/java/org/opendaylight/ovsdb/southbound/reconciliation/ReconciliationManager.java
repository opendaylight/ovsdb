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
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.*;

/**
 * Created by Anil Vishnoi <avishnoi@Brocade.com> on 3/9/16.
 */
public class ReconciliationManager implements Runnable{
    private static final Logger LOG = LoggerFactory.getLogger(ReconciliationManager.class);

    private final BlockingQueue<ReconciliationTask> reconciliationTasksQueue = new LinkedBlockingDeque<>();

    private final Queue<ConnectionReconciliation> retryConnectionTaskCache = new ConcurrentLinkedQueue();

    private final ExecutorService reconcilers;
    private final ScheduledExecutorService connectionTaskTriager;
    private final OvsdbConnectionManager connectionManager;
    private volatile boolean stop = false;


    public ReconciliationManager(OvsdbConnectionManager cm) {
        connectionManager = cm;
        ThreadFactory threadFact;
        threadFact = new ThreadFactoryBuilder().setNameFormat("ovsdb-reconsiler-%d").build();
        reconcilers = Executors.newCachedThreadPool(threadFact);
        reconcilers.execute(this);

        threadFact = new ThreadFactoryBuilder().setNameFormat("ovsdb-triager-%d").build();
        connectionTaskTriager = Executors.newSingleThreadScheduledExecutor(threadFact);
        connectionTaskTriager.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                int queueSize = retryConnectionTaskCache.size();
                while (queueSize-- != 0){
                    ConnectionReconciliation task = retryConnectionTaskCache.poll();
                    if(task.attemptConnectionAgain()) {
                        if(task.isAttemptTimeoutExpired()){
                            reconciliationTasksQueue.offer(task);
                        } else {
                            retryConnectionTaskCache.offer(task);
                        }
                    }
                }
            }
        },0,10,TimeUnit.SECONDS);
    }

    public void enqueue(final ReconciliationTask task) {
        final boolean success = reconciliationTasksQueue.offer(task);
        if ( ! success) {
            LOG.warn("Reconciliation task queue is full!. Following task enqueue failed {}",task);
        }
    }

    @Override
    public void run() {
        while(!stop) {
            try {
                ReconciliationTask task = reconciliationTasksQueue.take();

                if(task instanceof ConnectionReconciliation) {
                    if(!task.reconcileConfiguration(connectionManager)) {
                        retryConnectionTaskCache.offer((ConnectionReconciliation) task);
                    }
                }else {
                    task.reconcileConfiguration(connectionManager);
                }

            } catch (InterruptedException e) {
                LOG.warn("Reconciliation task queue interrupted!",e);
            }
        }
    }

}
