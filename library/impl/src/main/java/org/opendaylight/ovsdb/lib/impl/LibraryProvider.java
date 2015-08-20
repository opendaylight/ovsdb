/*
 * Copyright © 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.ovsdb.lib.ConfigActivator;
import org.opendaylight.ovsdb.lib.OvsdbConnection;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LibraryProvider implements BindingAwareProvider, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(LibraryProvider.class);
    private final BundleContext bundleContext;
    private DataBroker dataBroker;
    private ConfigActivator activator;

    public LibraryProvider(BundleContext bundleContext) {
        LOG.info("LibraryProvider: bundleContext: {}", bundleContext);
        this.bundleContext = bundleContext;
    }

    @Override
    public void onSessionInitiated(ProviderContext providerContext) {
        LOG.info("LibraryProvider Session Initiated");
        dataBroker = providerContext.getSALService(DataBroker.class);
        LOG.info("OvsdbLibraryProvider: onSessionInitiated dataBroker: {}", dataBroker);
        this.activator = new ConfigActivator(providerContext);
        try {
            activator.start(bundleContext);
        } catch (Exception e) {
            LOG.warn("Failed to start OvsdbLibraryProvider: ", e);
        }
    }

    @Override
    public void close() throws Exception {
        LOG.info("LibraryProvider Closed");
        if (activator != null) {
            activator.stop(bundleContext);
        }
    }

}
