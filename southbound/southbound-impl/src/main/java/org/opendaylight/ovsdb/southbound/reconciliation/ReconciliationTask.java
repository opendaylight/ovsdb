/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.reconciliation;

import org.opendaylight.ovsdb.southbound.OvsdbConnectionManager;
import org.opendaylight.ovsdb.southbound.reconciliation.connection.ConnectionReconciliationTask;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Anil Vishnoi (avishnoi@Brocade.com) on 3/9/16.
 */
public abstract class ReconciliationTask implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(ReconciliationTask.class);

    protected final ReconciliationManager reconciliationManager;
    protected final OvsdbConnectionManager connectionManager;
    protected final InstanceIdentifier<?> nodeIid;
    protected final DataObject configData;

    protected ReconciliationTask(ReconciliationManager reconciliationManager, OvsdbConnectionManager connectionManager,
                                 InstanceIdentifier<?> nodeIid,
                                 DataObject configData) {
        this.reconciliationManager = reconciliationManager;
        this.connectionManager = connectionManager;
        this.nodeIid = nodeIid;
        this.configData = configData;
    }

    /**
     * Method contains task reconciliation logic
     * @param connectionManager Connection manager to get connection instance of the device
     * @return True if reconciliation was successful, else false
     */
    public abstract boolean reconcileConfiguration(OvsdbConnectionManager connectionManager);

    /**
     * Extended task should implement the logic that decides whether retry for the task
     * is required or not. If retry does not requires any delay, submit the task
     * immediately using {@link ReconciliationManager#enqueue(ReconciliationTask)}.If retry
     * requires delay, use {@link ReconciliationManager#enqueueForRetry(ReconciliationTask)}
     * and specify the delay using {@link #retryDelayInMills()}.
     * If data store operation is required to decide if task need retry, please implement
     * it as an async operation and submit the task on the result of future.
     * <p>
     * Note:Please do not write blocking data store operations
     * {@link ConnectionReconciliationTask#doRetry()}
     * </p>
     */
    public abstract void doRetry();

    /**
     * Extended task should implement the logic that check the readiness of the task
     * for execution. If task is ready for the execution, submit it for immediate
     * execution using {@link ReconciliationManager#enqueue(ReconciliationTask)}.
     * If task is not ready for execution yet, enqueue it again for delayed execution
     * using {@link ReconciliationManager#enqueueForRetry(ReconciliationTask)}.
     * To check the readiness of the task, if data store operation is required, please
     * implement it as an async operation and submit the task on the result of future.
     * <p>
     * Note:Please do not write blocking data store operations
     * {@link ConnectionReconciliationTask#doRetry()}
     * </p>
     */
    public abstract void checkReadinessAndProcess();

    /**
     * Method returns the time interval for retrying the failed task.
     * {@link ReconciliationTask#doRetry()}
     * @return time
     */
    public abstract long retryDelayInMills();

    @Override
    public void run() {
        boolean status = this.reconcileConfiguration(connectionManager);
        if(!status ){
            doRetry();
        }else {
            this.reconciliationManager.dequeue(this);
        }
    }
}
