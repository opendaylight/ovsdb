/*
 * Copyright (c) 2013, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.lib.osgi;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.opendaylight.ovsdb.lib.OvsdbConnection;
import org.opendaylight.ovsdb.lib.OvsdbConnectionListener;
import org.opendaylight.ovsdb.lib.impl.OvsdbConnectionService;
import org.osgi.framework.BundleContext;

/**
 * OVSDB Library OSGi Activator
 */
public class Activator extends DependencyActivatorBase {

    @Override
    public void init(BundleContext context, DependencyManager manager) throws Exception {
        manager.add(createComponent()
            .setInterface(OvsdbConnection.class.getName(), null)
            .setImplementation(OvsdbConnectionService.class)
            .add(createServiceDependency()
                            .setService(OvsdbConnectionListener.class)
                            .setCallbacks("registerConnectionListener", "unregisterConnectionListener")
                            .setRequired(false)
            )
        );
    }

    @Override
    public void destroy(BundleContext context, DependencyManager manager) throws Exception {}
}
