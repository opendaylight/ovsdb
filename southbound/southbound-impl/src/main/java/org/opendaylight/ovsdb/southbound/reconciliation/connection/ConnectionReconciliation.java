/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.reconciliation.connection;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionManager;
import org.opendaylight.ovsdb.southbound.reconciliation.ReconciliationManager;
import org.opendaylight.ovsdb.southbound.reconciliation.ReconciliationTask;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by Anil Vishnoi (avishnoi@Brocade.com) on 3/9/16.
 */
public class ConnectionReconciliation extends ReconciliationTask{

    private static final Logger LOG = LoggerFactory.getLogger(ConnectionReconciliation.class);

    private static final int RETRY_INTERVAL_FACTOR = 10000;
    private final int MAX_ATTEMPT = 10;
    private AtomicInteger connectionAttempt = new AtomicInteger(0);

    public ConnectionReconciliation(ReconciliationManager reconciliationManager, OvsdbConnectionManager
            connectionManager, InstanceIdentifier<Node> nodeIid, DataObject configData) {
        super(reconciliationManager, connectionManager, nodeIid, configData);

    }

    @Override
    public boolean reconcileConfiguration(OvsdbConnectionManager connectionManager) {
        boolean result = false;
        connectionAttempt.incrementAndGet();
        InstanceIdentifier<Node> nodeIid = (InstanceIdentifier<Node>) getNodeIid();
        OvsdbNodeAugmentation ovsdbNode = (OvsdbNodeAugmentation)getConfigData();

        LOG.info("Retry({}) connection to Ovsdb Node {} ", connectionAttempt.get(), ovsdbNode.getConnectionInfo());
        OvsdbClient client = null;
        try {
            client = connectionManager.connect(nodeIid, ovsdbNode);
            if (client != null) {
                LOG.info("Successfully connected to Ovsdb Node {} ", ovsdbNode.getConnectionInfo());
                result = true;
            } else {
                LOG.warn("Connection retry({}) failed for {}.",
                        connectionAttempt.get(), ovsdbNode.getConnectionInfo());
            }
        } catch (UnknownHostException e) {
            LOG.warn("Connection retry({}) failed with exception. ",connectionAttempt.get(), e);
        }
        return result;
    }

    @Override
    public boolean doRetry() {
        return connectionAttempt.get() <= MAX_ATTEMPT;
    }

    @Override
    public boolean isTaskReadyForRetry() {
        return true;
    }

    @Override
    public long retryDelayInMills() {
        return connectionAttempt.get() * RETRY_INTERVAL_FACTOR;
    }
}
