/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound;

import java.util.concurrent.atomic.AtomicBoolean;
import org.opendaylight.ovsdb.lib.MonitorCallBack;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.southbound.transactions.md.OvsdbOperationalCommandAggregator;
import org.opendaylight.ovsdb.southbound.transactions.md.TransactionInvoker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsdbMonitorCallback implements MonitorCallBack {
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbMonitorCallback.class);

    private final InstanceIdentifierCodec instanceIdentifierCodec;
    private TransactionInvoker txInvoker;
    private OvsdbConnectionInstance key;
    private AtomicBoolean intialUpdate = new AtomicBoolean(false);

    OvsdbMonitorCallback(InstanceIdentifierCodec instanceIdentifierCodec, OvsdbConnectionInstance key,
            TransactionInvoker txInvoker) {
        this.instanceIdentifierCodec = instanceIdentifierCodec;
        this.txInvoker = txInvoker;
        this.key = key;
    }

    @Override
    public void update(TableUpdates result, DatabaseSchema dbSchema) {
        boolean isFirstUpdate = intialUpdate.compareAndSet(false, true);
        txInvoker.invoke(new OvsdbOperationalCommandAggregator(instanceIdentifierCodec, key, result,
                dbSchema, isFirstUpdate));
        LOG.trace("Updated dbSchema: {} and result: {}", dbSchema, result);
    }

    @Override
    public void exception(Throwable exception) {
        LOG.warn("exception", exception);
    }

}
