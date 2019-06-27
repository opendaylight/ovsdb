/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound;

import org.opendaylight.ovsdb.utils.mdsal.utils.TransactionElement;

public class HwvtepTransactionLogElement extends TransactionElement {

    private final boolean isDeviceLog;
    private long date;

    public HwvtepTransactionLogElement(TransactionElement element,
                                       boolean isDeviceLog) {
        super(element.getTransactionType(), element.getData());
        this.isDeviceLog = isDeviceLog;
        this.date = element.getDate();
    }

    @Override
    public long getDate() {
        return date;
    }

    @Override
    public String toString() {
        return "HwvtepTransactionLogElement [isDeviceLog=" + isDeviceLog + "]";
    }
}
