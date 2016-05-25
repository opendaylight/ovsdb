/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.reconciliation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/**
 * Copied from org.opendaylight.ovsdb.southbound.reconciliation.ReconciliationTaskManager
 * 
 * TODO: A common ReconciliatonTaskManager for all plugins
 */
class ReconciliationTaskManager {
    private static final Logger LOG = LoggerFactory.getLogger(ReconciliationTaskManager.class);

    private final ConcurrentHashMap<ReconciliationTask,Future<?>> reconciliationTaskCache
            = new ConcurrentHashMap();

    public boolean isTaskQueued(ReconciliationTask task) {
        return reconciliationTaskCache.containsKey(task);
    }
    public boolean cancelTask(ReconciliationTask task) {
        if(reconciliationTaskCache.containsKey(task)){
            Future<?> taskFuture = reconciliationTaskCache.remove(task);
            if( !taskFuture.isDone() && !taskFuture.isCancelled() ) {
                LOG.info("Reconciliation task is cancelled : {}",task);
                return taskFuture.cancel(true);
            }
        }
        return false;

    }
    public void cacheTask(ReconciliationTask task, Future<?> taskFuture) {
        reconciliationTaskCache.put(task,taskFuture);
    }
}
