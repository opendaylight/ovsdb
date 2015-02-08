/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Brent Salisbury
 */
package org.opendaylight.ovsdb.plugin.internal;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.dm.Component;
import org.opendaylight.controller.sal.core.ComponentActivatorAbstractBase;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.ovsdb.lib.OvsdbConnection;
import org.opendaylight.ovsdb.lib.OvsdbConnectionListener;
import org.opendaylight.ovsdb.plugin.api.OvsdbConfigurationService;
import org.opendaylight.ovsdb.plugin.api.OvsdbConnectionService;
import org.opendaylight.ovsdb.plugin.api.OvsdbInventoryListener;
import org.opendaylight.ovsdb.plugin.api.OvsdbInventoryService;
import org.opendaylight.ovsdb.plugin.impl.ConfigurationServiceImpl;
import org.opendaylight.ovsdb.plugin.impl.ConnectionServiceImpl;
import org.opendaylight.ovsdb.plugin.impl.InventoryServiceImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OVSDB protocol plugin Activator
 *
 *
 */
public class Activator extends ComponentActivatorAbstractBase {
    protected static final Logger logger = LoggerFactory
            .getLogger(Activator.class);

    /**
     * Function called when the activator starts just after some initializations
     * are done by the ComponentActivatorAbstractBase.
     * Here it registers the node Type
     *
     */
    @Override
    public void init() {
        Node.NodeIDType.registerIDType("OVS", String.class);
        NodeConnector.NodeConnectorIDType.registerIDType("OVS", String.class, "OVS");
    }

    /**
     * Function called when the activator stops just before the cleanup done by
     * ComponentActivatorAbstractBase
     *
     */
    @Override
    public void destroy() {
        Node.NodeIDType.unRegisterIDType("OVS");
        NodeConnector.NodeConnectorIDType.unRegisterIDType("OVS");
    }
    @Override
    public Object[] getGlobalImplementations() {
        Object[] res = { ConnectionServiceImpl.class, ConfigurationServiceImpl.class, InventoryServiceImpl.class };
        return res;
    }

    @Override
    public void configureGlobalInstance(Component c, Object imp){
        if (imp.equals(ConfigurationServiceImpl.class)) {
            // export the service to be used by SAL
            Dictionary<String, Object> props = new Hashtable<String, Object>();
            c.setInterface(new String[] { OvsdbConfigurationService.class.getName()}, props);

            c.add(createServiceDependency()
                    .setService(OvsdbConnectionService.class)
                    .setRequired(true));
            c.add(createServiceDependency()
                    .setService(OvsdbInventoryService.class)
                    .setRequired(true));
        }

        if (imp.equals(ConnectionServiceImpl.class)) {
            // export the service to be used by SAL
            Dictionary<String, Object> props = new Hashtable<String, Object>();
            c.setInterface(
                    new String[] {OvsdbConnectionService.class.getName(),
                                  OvsdbConnectionListener.class.getName()}, props);
            c.add(createServiceDependency()
                    .setService(OvsdbInventoryService.class)
                    .setRequired(true));
            c.add(createServiceDependency()
                    .setService(OvsdbConnection.class)
                    .setRequired(true));
        }

        if (imp.equals(InventoryServiceImpl.class)) {
            Dictionary<String, Object> props = new Hashtable<>();
            c.setInterface(
                    new String[]{OvsdbInventoryService.class.getName()}, props);
            c.add(createServiceDependency()
                    .setService(OvsdbInventoryListener.class)
                    .setCallbacks("listenerAdded", "listenerRemoved"));
            c.add(createServiceDependency()
                    .setService(OvsdbConfigurationService.class)
                    .setRequired(false));
        }

    }
}
