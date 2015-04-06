/*
 * Copyright (c) 2015 Inocybe and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.northbound;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.ovsdb.northbound.transactions.TransactionInvoker;
import org.opendaylight.ovsdb.northbound.transactions.TransactionInvokerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NorthboundProvider implements BindingAwareProvider, AutoCloseable  {

    private static final Logger LOG = LoggerFactory.getLogger(NorthboundProvider.class);
    private DataBroker db;
    private TransactionInvoker txInvoker;

    @Override
    public void close() throws Exception {
        LOG.info("NorthboundProvider Closed.");
    }

    @Override
    public void onSessionInitiated(ProviderContext session) {
        LOG.info("NorthboundProvider Session Initated.");
        db = session.getSALService(DataBroker.class);
        this.txInvoker = new TransactionInvokerImpl(db);
    }

}
