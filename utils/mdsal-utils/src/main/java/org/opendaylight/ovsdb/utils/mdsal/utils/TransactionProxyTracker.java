/*
 * Copyright (c) 2020 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.utils.mdsal.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;

public class TransactionProxyTracker {

    private Map<AsyncTransaction, TransactionProxy> proxyTransactions = new ConcurrentHashMap<>();

    public TransactionProxyTracker() {
    }

    public void putProxyFor(AsyncTransaction actual, TransactionProxy proxy) {
        proxyTransactions.put(actual, proxy);
    }

    public TransactionProxy getProxyFor(AsyncTransaction actual) {
        if (actual != null) {
            return proxyTransactions.get(actual);
        }
        return null;
    }

    public void clearProxyFor(AsyncTransaction actual) {
        if (actual != null) {
            proxyTransactions.remove(actual);
        }
    }

    public Map<AsyncTransaction, TransactionProxy> getTransactions() {
        return proxyTransactions;
    }

    public int getSize() {
        return proxyTransactions.size();
    }
}
