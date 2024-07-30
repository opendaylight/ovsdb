/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.reconciliation;

import static java.util.Objects.requireNonNull;

import org.opendaylight.ovsdb.southbound.OvsdbConnectionManager;
import org.opendaylight.ovsdb.southbound.reconciliation.connection.ConnectionReconciliationTask;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Abstract implementation of a reconciliation task. Each new type of
 * resource configuration reconciliation task should extend this class
 * and implement the abstract methods.
 */
public abstract class ReconciliationTask implements Runnable {
    protected final ReconciliationManager reconciliationManager;
    protected final OvsdbConnectionManager connectionManager;
    protected final InstanceIdentifier<?> nodeIid;
    protected final DataObject configData;

    protected ReconciliationTask(final ReconciliationManager reconciliationManager,
                                 final OvsdbConnectionManager connectionManager,
                                 final InstanceIdentifier<?> nodeIid, final DataObject configData) {
        this.reconciliationManager = requireNonNull(reconciliationManager, "Reconciliation manager must not be null");
        this.connectionManager = requireNonNull(connectionManager, "Connection manager must not be null");
        this.nodeIid = requireNonNull(nodeIid, "Node Iid must not be null");
        this.configData = configData;
    }

    /**
     * Method contains task reconciliation logic. Please refer to
     * {@link ConnectionReconciliationTask#reconcileConfiguration(OvsdbConnectionManager)}
     * for example.
     * @param connectionManagerOfDevice Connection manager to get connection instance of the device
     * @return True if reconciliation was successful, else false
     */
    public abstract boolean reconcileConfiguration(OvsdbConnectionManager connectionManagerOfDevice);

    /**
     * Extended task should implement the logic that decides whether retry for the task
     * is required or not. If retry is required but it does not requires any delay, submit
     * the task immediately using {@link ReconciliationManager#enqueue(ReconciliationTask)}.
     * If retry requires delay, use {@link ReconciliationManager#enqueueForRetry(ReconciliationTask)}
     * and specify the delay using {@link #retryDelayInMills()}.
     * If data store operation is required to decide if the task need retry, please implement
     * it as an async operation and submit the task on the callback of the future.
     * <p>
     * Note:Please do not write blocking data store operations
     * {@link ConnectionReconciliationTask#doRetry(boolean)}
     * </p>
     * @param wasPreviousAttemptSuccessful Status of the previous attempt
     */
    public abstract void doRetry(boolean wasPreviousAttemptSuccessful);

    /**
     * Extended task should implement the logic that check the readiness of the task
     * for execution. If task is ready for the execution, submit it for immediate
     * execution using {@link ReconciliationManager#enqueue(ReconciliationTask)}.
     * If task is not ready for execution yet, enqueue it again for delayed execution
     * using {@link ReconciliationManager#enqueueForRetry(ReconciliationTask)}.
     * To check the readiness of the task, if the data store operation is required, please
     * implement it as an async operation and submit the task on the callback of the future.
     * <p>
     * Note:Please do not write blocking data store operations
     * {@link ConnectionReconciliationTask#doRetry(boolean)}
     * </p>
     */
    public abstract void checkReadinessAndProcess();

    /**
     * Method returns the time interval for retrying the failed task.
     * {@link ReconciliationTask#doRetry(boolean)}
     * @return time
     */
    public abstract long retryDelayInMills();

    @Override
    public void run() {
        boolean status = this.reconcileConfiguration(connectionManager);
        doRetry(status);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        final ReconciliationTask other = (ReconciliationTask) obj;
        return nodeIid.equals(other.nodeIid);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode() + nodeIid.hashCode();
    }

    @Override
    public String toString() {
        return "ReconciliationTask{ type=" + getClass().toString() + ", nodeIid=" + nodeIid + '}';
    }
}
