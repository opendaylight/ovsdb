/*
 * Copyright © 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib.impl;

import java.net.InetAddress;
import java.util.Collection;

import javax.net.ssl.SSLContext;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.OvsdbConnection;
import org.opendaylight.ovsdb.lib.OvsdbConnectionListener;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LibraryProvider implements BindingAwareProvider, AutoCloseable, OvsdbConnection {

    private static final Logger LOG = LoggerFactory.getLogger(LibraryProvider.class);

    public LibraryProvider(BundleContext bundleContext) {
        LOG.info("LibraryProvider: bundleContext: {}", bundleContext);
    }

    @Override
    public void onSessionInitiated(ProviderContext providerContext) {
        LOG.info("LibraryProvider Session Initiated");
    }

    @Override
    public void close() throws Exception {
        LOG.info("LibraryProvider Closed");
    }

    @Override
    public OvsdbClient connect(InetAddress address, int port) {
        return OvsdbConnectionService.getService().connect(address, port);
    }

    @Override
    public OvsdbClient connectWithSsl(
            InetAddress address, int port, SSLContext sslContext) {
        return OvsdbConnectionService.getService().connectWithSsl(address, port, sslContext);
    }

    @Override
    public void disconnect(OvsdbClient client) {
        OvsdbConnectionService.getService().disconnect(client);
    }

    @Override
    public boolean startOvsdbManager(int ovsdbListenPort) {
        return OvsdbConnectionService.getService().startOvsdbManager(ovsdbListenPort);
    }

    @Override
    public boolean startOvsdbManagerWithSsl(int ovsdbListenPort, SSLContext sslContext) {
        return OvsdbConnectionService.getService().startOvsdbManagerWithSsl(ovsdbListenPort, sslContext);
    }

    @Override
    public void registerConnectionListener(OvsdbConnectionListener listener) {
        OvsdbConnectionService.getService().registerConnectionListener(listener);
    }

    @Override
    public void unregisterConnectionListener(OvsdbConnectionListener listener) {
        OvsdbConnectionService.getService().unregisterConnectionListener(listener);
    }

    @Override
    public Collection<OvsdbClient> getConnections() {
        return OvsdbConnectionService.getService().getConnections();
    }
}
