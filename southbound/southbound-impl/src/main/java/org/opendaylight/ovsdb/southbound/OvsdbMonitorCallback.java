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

public class OvsdbMonitorCallback implements MonitorCallBack {

    private static final Logger LOG = LoggerFactory.getLogger(OvsdbMonitorCallback.class);
    private TransactionInvoker txInvoker;
    private OvsdbConnectionInstance key;

    OvsdbMonitorCallback(OvsdbConnectionInstance key,TransactionInvoker txInvoker) {
        this.txInvoker = txInvoker;
        this.key = key;
    }

    @Override
    public void update(TableUpdates result, DatabaseSchema dbSchema) {
        LOG.debug("result: {} dbSchema: {}",result,dbSchema);
        txInvoker.invoke(new OvsdbOperationalCommandAggregator(key, result, dbSchema));
        LOG.trace("update exit");
    }

    @Override
    public void exception(Throwable exception) {
        LOG.warn("exception {}", exception);
    }

}
