/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.reconciliation.connection;

import org.opendaylight.ovsdb.southbound.reconciliation.ReconciliationTask;
import org.opendaylight.ovsdb.southbound.reconciliation.TaskManager;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/**
 * Created by Anil Vishnoi (avishnoi@Brocade.com) on 3/15/16.
 */
public class ConnectionReconciliationTaskManager implements TaskManager{
    private static final Logger LOG = LoggerFactory.getLogger(ConnectionReconciliationTaskManager.class);

    private final ConcurrentHashMap<InstanceIdentifier<?>,Future<?>> connectionTaskCache
            = new ConcurrentHashMap();
    @Override
    public boolean isTaskAlreadyQueued(InstanceIdentifier<?> iid) {
        return connectionTaskCache.containsKey(iid);
    }

    @Override
    public boolean cancleTask(InstanceIdentifier<?> iid) {
        if(connectionTaskCache.containsKey(iid)){
            Future<?> taskFuture = connectionTaskCache.remove(iid);
            if( !taskFuture.isCancelled() && !taskFuture.isDone()) {
                LOG.info("Connection reconciliation task is cancelled for {}",iid);
                return taskFuture.cancel(true);
            }
        }
        return false;
    }

    @Override
    public void cacheTask(InstanceIdentifier<?> iid, Future<?> taskFuture) {
        connectionTaskCache.put(iid,taskFuture);
    }
}
