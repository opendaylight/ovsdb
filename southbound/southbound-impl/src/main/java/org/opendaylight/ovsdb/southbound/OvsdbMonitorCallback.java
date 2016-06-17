/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound;

import org.opendaylight.ovsdb.lib.MonitorCallBack;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.southbound.transactions.md.OvsdbOperationalCommandAggregator;
import org.opendaylight.ovsdb.southbound.transactions.md.TransactionInvoker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

public class OvsdbMonitorCallback implements MonitorCallBack {

    private static final Logger LOG = LoggerFactory.getLogger(OvsdbMonitorCallback.class);
    private TransactionInvoker txInvoker;
    private OvsdbConnectionInstance key;

    // latch to notify other tasks such as BridgeConfigReconciliationTask when the first schema
    // update is completed
    private CountDownLatch firstUpdateCompletionLatch = new CountDownLatch(1);

    OvsdbMonitorCallback(OvsdbConnectionInstance key,TransactionInvoker txInvoker) {
        this.txInvoker = txInvoker;
        this.key = key;
    }

    @Override
    public void update(TableUpdates result, DatabaseSchema dbSchema) {
        LOG.debug("result: {} dbSchema: {}",result,dbSchema);
        txInvoker.invoke(new OvsdbOperationalCommandAggregator(key, result, dbSchema, firstUpdateCompletionLatch));
        LOG.trace("update exit");
    }

    @Override
    public void exception(Throwable exception) {
        LOG.warn("exception {}", exception);
    }

    public CountDownLatch getFirstUpdateCompletionLatch() {
        return firstUpdateCompletionLatch;
    }

    public void setFirstUpdateCompletionLatch(CountDownLatch firstUpdateCompletionLatch) {
        this.firstUpdateCompletionLatch = firstUpdateCompletionLatch;
    }
}
