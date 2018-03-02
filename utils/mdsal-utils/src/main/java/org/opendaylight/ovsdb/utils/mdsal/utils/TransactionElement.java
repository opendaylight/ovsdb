/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.utils.mdsal.utils;

public class TransactionElement {
    private final long date;
    private final TransactionType transactionType;
    private final Object data;

    public TransactionElement(TransactionType transactionType, Object data) {
        this.data = data;
        this.transactionType = transactionType;
        this.date = System.currentTimeMillis();
    }

    public Object getData() {
        return data;
    }

    public long getDate() {
        return date;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    @Override
    public String toString() {
        return "TransactionElement{" + "date=" + date + ", transactionType=" + transactionType + ", data=" + data + '}';
    }
}
