/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound;

import org.opendaylight.ovsdb.hwvtepsouthbound.transactions.md.HwvtepOperationalCommandAggregator;
import org.opendaylight.ovsdb.hwvtepsouthbound.transactions.md.TransactionInvoker;
import org.opendaylight.ovsdb.lib.MonitorCallBack;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HwvtepMonitorCallback implements MonitorCallBack {

    private static final Logger LOG = LoggerFactory.getLogger(HwvtepMonitorCallback.class);
    private HwvtepConnectionInstance key;
    private TransactionInvoker txInvoker;

    HwvtepMonitorCallback(HwvtepConnectionInstance key,TransactionInvoker txInvoker) {
        this.txInvoker = txInvoker;
        this.key = key;
    }

    @Override
    public void update(TableUpdates result, DatabaseSchema dbSchema) {
        LOG.trace("result: {} dbSchema: {}",result,dbSchema.getName());
        txInvoker.invoke(new HwvtepOperationalCommandAggregator(key, result, dbSchema));
        LOG.trace("update exit");
    }

    @Override
    public void exception(Throwable exception) {
        LOG.warn("exception {}", exception);
    }

}
