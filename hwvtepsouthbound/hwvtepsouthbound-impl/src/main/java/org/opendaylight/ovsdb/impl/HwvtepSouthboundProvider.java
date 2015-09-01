/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.impl;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HwvtepSouthboundProvider implements BindingAwareProvider, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(HwvtepSouthboundProvider.class);

    @Override
    public void onSessionInitiated(ProviderContext session) {
        LOG.info("HwvtepSouthboundProvider Session Initiated");
    }

    @Override
    public void close() throws Exception {
        LOG.info("HwvtepSouthboundProvider Closed");
    }

}
