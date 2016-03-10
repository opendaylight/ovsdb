/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.reconciliation;

import org.opendaylight.ovsdb.southbound.OvsdbConnectionManager;
import org.opendaylight.ovsdb.southbound.reconciliation.connection.ConnectionReconciliation;
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
    private final OvsdbConnectionManager connectionManager;
    private final InstanceIdentifier<?> nodeIid;
    private final DataObject configData;

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
     * Do you want to retry the task reconciliation?
     * @return true if yes, else false
     */
    public abstract boolean doRetry();

    /**
     * Is task ready for re-trying the config reconciliation
     * @return true if yes, else false
     */
    public abstract boolean isTaskReadyForRetry();

    /**
     * Method returns retry time.
     * @return time
     */
    public abstract long retryDelayInMills();

    @Override
    public void run() {
        boolean status = this.reconcileConfiguration(connectionManager);
        if(!status && doRetry()){
            reconciliationManager.enqueueForRetry(this);
        }
    }

    public InstanceIdentifier<?> getNodeIid() {
        return nodeIid;
    }

    public DataObject getConfigData() {
        return configData;
    }
}
