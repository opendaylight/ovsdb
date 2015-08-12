/*
 * Copyright (c) 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sam Hague (shague@redhat.com)
 */
public class NetvirtProvider implements BindingAwareProvider, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(NetvirtProvider.class);
    private BundleContext bundleContext = null;
    private static DataBroker dataBroker = null;
    private ConfigActivator activator;

    public NetvirtProvider(BundleContext bundleContext) {
        LOG.info("NetvirtProvider: bundleContext: {}", bundleContext);
        this.bundleContext = bundleContext;
    }

    @Override
    public void close() throws Exception {
        activator.stop(bundleContext);
    }

    @Override
    public void onSessionInitiated(ProviderContext providerContext) {
        dataBroker = providerContext.getSALService(DataBroker.class);
        LOG.info("NetvirtProvider: onSessionInitiated dataBroker: {}", dataBroker);
        this.activator = new ConfigActivator(providerContext);
        try {
            activator.start(bundleContext);
        } catch (Exception e) {
            LOG.warn("Failed to start Netvirt: ", e);
        }
    }
}
