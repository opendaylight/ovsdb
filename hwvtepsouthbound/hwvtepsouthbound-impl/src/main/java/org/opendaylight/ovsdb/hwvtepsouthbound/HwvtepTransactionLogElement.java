/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
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
import org.opendaylight.ovsdb.utils.mdsal.utils.TransactionElement;
import org.opendaylight.ovsdb.utils.mdsal.utils.TransactionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HwvtepTransactionLogElement extends TransactionElement {

    private final boolean isDeviceLog;

    public HwvtepTransactionLogElement(TransactionType transactionType,
                                       Object data,
                                       boolean isDeviceLog) {
        super(transactionType, data);
        this.isDeviceLog = isDeviceLog;
    }

    public HwvtepTransactionLogElement(TransactionElement element,
                                       boolean isDeviceLog) {
        super(element.getTransactionType(), element.getData());
        this.isDeviceLog = isDeviceLog;
    }

    @Override
    public String toString() {
        return "{" +
                "isDeviceLog=" + isDeviceLog +
                super.toString() +
                '}';
    }
}
