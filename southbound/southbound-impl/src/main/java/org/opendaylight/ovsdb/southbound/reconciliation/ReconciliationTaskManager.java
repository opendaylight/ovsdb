/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.reconciliation;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is a task cache manager that provides a cache to store
 * the task that is queued for the reconciliation. Whenever new task
 * is submitted to the reconciliation manager, it will be cached in
 * the cache. If the reconciliation is successful or it's done with
 * all the attempt of reconciliation,
 * <p>
 * Caching of the task is required, because reconciliation task might
 * require longer duration to reconcile and there is a possibility that
 * meanwhile user can change the configuration in config data store while
 * task is queued for the reconciliation. In that scenario, reconciliation
 * manager should not attempt any further reconciliation attempt for that
 * task. ReconciliationManager will call cancelTask() to cancel the task.
 * </p>
 * Created by Anil Vishnoi (avishnoi@Brocade.com) on 3/15/16.
 */
class ReconciliationTaskManager {
    private static final Logger LOG = LoggerFactory.getLogger(ReconciliationTaskManager.class);

    private final ConcurrentHashMap<ReconciliationTask, Future<?>> reconciliationTaskCache
            = new ConcurrentHashMap<>();

    public boolean isTaskQueued(ReconciliationTask task) {
        return reconciliationTaskCache.containsKey(task);
    }

    public boolean cancelTask(ReconciliationTask task) {
        if (reconciliationTaskCache.containsKey(task)) {
            Future<?> taskFuture = reconciliationTaskCache.remove(task);
            if (!taskFuture.isDone() && !taskFuture.isCancelled()) {
                LOG.info("Reconciliation task is cancelled : {}", task);
                return taskFuture.cancel(true);
            }
        }
        return false;

    }

    public void cacheTask(ReconciliationTask task, Future<?> taskFuture) {
        reconciliationTaskCache.put(task,taskFuture);
    }
}
