/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.reconciliation.connection;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicInteger;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepConnectionManager;
import org.opendaylight.ovsdb.hwvtepsouthbound.reconciliation.ReconciliationManager;
import org.opendaylight.ovsdb.hwvtepsouthbound.reconciliation.ReconciliationTask;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Copied from org.opendaylight.ovsdb.southbound.reconciliation.connection.ConnectionReconciliationTask.
 *
 * <p>
 * Created by Anil Vishnoi (avishnoi@Brocade.com) on 3/9/16.
 */
public class ConnectionReconciliationTask extends ReconciliationTask {

    private static final Logger LOG = LoggerFactory.getLogger(ConnectionReconciliationTask.class);

    private static final long RETRY_INTERVAL_FACTOR = 10000L;
    private static final int MAX_ATTEMPT = 10;

    private final AtomicInteger connectionAttempt = new AtomicInteger(0);

    public ConnectionReconciliationTask(ReconciliationManager reconciliationManager, HwvtepConnectionManager
            connectionManager, InstanceIdentifier<?> nodeIid, DataObject configData) {
        super(reconciliationManager, connectionManager, nodeIid, configData);

    }

    @Override
    public boolean reconcileConfiguration(HwvtepConnectionManager connectionManager) {
        boolean result = false;
        connectionAttempt.incrementAndGet();
        InstanceIdentifier<Node> nodeId = (InstanceIdentifier<Node>) nodeIid;
        HwvtepGlobalAugmentation hwvtepNode = (HwvtepGlobalAugmentation) configData;

        LOG.info("Retry({}) connection to Ovsdb Node {} ", connectionAttempt.get(), hwvtepNode.getConnectionInfo());
        OvsdbClient client = null;
        try {
            client = connectionManager.connect(nodeId, hwvtepNode);
            if (client != null) {
                LOG.info("Successfully connected to Hwvtep Node {} ", hwvtepNode.getConnectionInfo());
                result = true;
            } else {
                LOG.warn("Connection retry({}) failed for {}.",
                        connectionAttempt.get(), hwvtepNode.getConnectionInfo());
            }
        } catch (UnknownHostException | ConnectException e) {
            LOG.warn("Connection retry({}) failed with exception. ", connectionAttempt.get(), e);
        }
        return result;
    }

    @Override
    public void doRetry(boolean wasLastAttemptSuccessful) {

        if (!wasLastAttemptSuccessful && connectionAttempt.get() <= MAX_ATTEMPT) {
            reconciliationManager.enqueueForRetry(ConnectionReconciliationTask.this);
        } else {
            reconciliationManager.dequeue(this);
        }
    }

    @Override
    public void checkReadinessAndProcess() {
        reconciliationManager.enqueue(this);
    }

    @Override
    public long retryDelayInMills() {
        return connectionAttempt.get() * RETRY_INTERVAL_FACTOR;
    }
}
