/*
 * Copyright © 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.ovsdb.lib.impl.OvsdbConnectionService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * "Configuration" activator for the OVSDB library.
 */
public class ConfigActivator implements BundleActivator {
    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ConfigActivator.class);

    /**
     * Parent provider context.
     */
    private final BindingAwareBroker.ProviderContext providerContext;

    /**
     * Creates an instance of the activator.
     *
     * @param providerContext The parent provider context.
     */
    public ConfigActivator(BindingAwareBroker.ProviderContext providerContext) {
        LOG.info("OVSDB library ConfigActivator created.");
        this.providerContext = providerContext;
    }

    @Override
    public void start(BundleContext context) throws Exception {
        LOG.info("OVSDB library ConfigActivator starting.");
        context.registerService(OvsdbConnection.class, new OvsdbConnectionService(), null);
        // TODO Need to indicate that OvsdbConnectionListeners should register with the connection service
        // (if I've understood correctly, the old dependency manager would call registerConnectionListener()
        // whenever an instance of OvsdbConnection is retrieved, and unregisterConnectionListener() when it
        // is no longer used)
        // (All current users register manually...)
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        LOG.info("OVSDB library ConfigActivator stopping.");
    }
}
