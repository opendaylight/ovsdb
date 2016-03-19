/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.reconciliation.connection;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.DataStoreUnavailableException;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
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

import javax.annotation.Nullable;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Anil Vishnoi (avishnoi@Brocade.com) on 3/9/16.
 */
public class ConnectionReconciliationTask extends ReconciliationTask{

    private static final Logger LOG = LoggerFactory.getLogger(ConnectionReconciliationTask.class);

    private static final int RETRY_INTERVAL_FACTOR = 10000;
    private final int MAX_ATTEMPT = 10;
    private AtomicInteger connectionAttempt = new AtomicInteger(0);

    public ConnectionReconciliationTask(ReconciliationManager reconciliationManager, OvsdbConnectionManager
            connectionManager, InstanceIdentifier<?> nodeIid, DataObject configData) {
        super(reconciliationManager, connectionManager, nodeIid, configData);

    }

    @Override
    public boolean reconcileConfiguration(OvsdbConnectionManager connectionManager) {
        boolean result = false;
        connectionAttempt.incrementAndGet();
        InstanceIdentifier<Node> nIid = (InstanceIdentifier<Node>) nodeIid;
        OvsdbNodeAugmentation ovsdbNode = (OvsdbNodeAugmentation)configData;

        LOG.info("Retry({}) connection to Ovsdb Node {} ", connectionAttempt.get(), ovsdbNode.getConnectionInfo());
        OvsdbClient client = null;
        try {
            client = connectionManager.connect(nIid, ovsdbNode);
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
    public void doRetry() {

        if( connectionAttempt.get() <= MAX_ATTEMPT ) {
            reconciliationManager.enqueueForRetry(ConnectionReconciliationTask.this);
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
