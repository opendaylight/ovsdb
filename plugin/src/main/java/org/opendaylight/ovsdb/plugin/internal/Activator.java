/*
 * Copyright (c) 2013, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.plugin.internal;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.opendaylight.ovsdb.lib.OvsdbConnection;
import org.opendaylight.ovsdb.lib.OvsdbConnectionListener;
import org.opendaylight.ovsdb.plugin.api.OvsdbConfigurationService;
import org.opendaylight.ovsdb.plugin.api.OvsdbConnectionService;
import org.opendaylight.ovsdb.plugin.api.OvsdbInventoryListener;
import org.opendaylight.ovsdb.plugin.api.OvsdbInventoryService;
import org.opendaylight.ovsdb.plugin.impl.ConfigurationServiceImpl;
import org.opendaylight.ovsdb.plugin.impl.ConnectionServiceImpl;
import org.opendaylight.ovsdb.plugin.impl.InventoryServiceImpl;

import org.osgi.framework.BundleContext;

/**
 * OVSDB protocol plugin Activator
 *
 *
 */
public class Activator extends DependencyActivatorBase {

    @Override
    public void init(BundleContext context, DependencyManager manager) throws Exception {
        manager.add(createComponent()
                        .setInterface(OvsdbConfigurationService.class.getName(), null)
                        .setImplementation(ConfigurationServiceImpl.class)
                        .add(createServiceDependency()
                                        .setService(OvsdbConnectionService.class)
                                        .setRequired(true))
                        .add(createServiceDependency()
                                .setService(OvsdbInventoryService.class)
                                .setRequired(true)));

        manager.add(createComponent()
                        .setInterface(
                                new String[] {OvsdbConnectionService.class.getName(),
                                        OvsdbConnectionListener.class.getName()}, null)
                        .setImplementation(ConnectionServiceImpl.class)
                        .add(createServiceDependency()
                                .setService(OvsdbInventoryService.class)
                                .setRequired(true))
                        .add(createServiceDependency()
                                .setService(OvsdbConnection.class)
                                .setRequired(true))
        );

        manager.add(createComponent()
                        .setInterface(OvsdbInventoryService.class.getName(), null)
                        .setImplementation(InventoryServiceImpl.class)
                        .add(createServiceDependency()
                                .setService(OvsdbInventoryListener.class)
                                .setCallbacks("listenerAdded", "listenerRemoved"))
                        .add(createServiceDependency()
                                .setService(OvsdbConfigurationService.class)
                                .setRequired(false)));
    }

    @Override
    public void destroy(BundleContext context, DependencyManager manager) throws Exception {
    }
}
