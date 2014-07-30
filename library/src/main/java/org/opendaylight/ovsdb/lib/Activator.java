/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Brent Salisbury
 */
package org.opendaylight.ovsdb.lib;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.opendaylight.ovsdb.lib.impl.OvsdbConnectionService;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OVSDB Library OSGi Activator
 */
public class Activator extends DependencyActivatorBase {
    protected static final Logger logger = LoggerFactory
            .getLogger(Activator.class);

    @Override
    public void init(BundleContext context, DependencyManager manager) throws Exception {
        manager.add(createComponent()
            .setInterface(OvsdbConnection.class.getName(), null)
            .setImplementation(OvsdbConnectionService.class)
        );
        manager.createServiceDependency()
               .setService(OvsdbConnectionListener.class)
               .setCallbacks("registerForPassiveConnection", "unregisterForPassiveConnection")
               .setRequired(false);
    }

    @Override
    public void destroy(BundleContext context, DependencyManager manager) throws Exception {}
}
