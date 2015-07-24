/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.compatibility.plugin.internal;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.dm.Component;
import org.opendaylight.controller.sal.core.ComponentActivatorAbstractBase;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.ovsdb.compatibility.plugin.api.OvsdbConfigurationService;
import org.opendaylight.ovsdb.compatibility.plugin.api.OvsdbConnectionService;
import org.opendaylight.ovsdb.compatibility.plugin.api.OvsdbInventoryListener;
import org.opendaylight.ovsdb.compatibility.plugin.api.OvsdbInventoryService;
import org.opendaylight.ovsdb.compatibility.plugin.impl.ConfigurationServiceImpl;
import org.opendaylight.ovsdb.compatibility.plugin.impl.ConnectionServiceImpl;
import org.opendaylight.ovsdb.compatibility.plugin.impl.InventoryServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Activator for ovsdb plugin compatibility layer
 * @author Anil Vishnoi (vishnoianil@gmail.com)
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
    }

    /**
     * Function called when the activator stops just before the cleanup done by
     * ComponentActivatorAbstractBase
     *
     */
    @Override
    public void destroy() {
        Node.NodeIDType.unRegisterIDType("OVS");
    }
    @Override
    public Object[] getGlobalImplementations() {
        return new Object[]{ ConnectionServiceImpl.class, ConfigurationServiceImpl.class, InventoryServiceImpl.class };
    }

    @Override
    public void configureGlobalInstance(Component c, Object imp){
        if (imp.equals(ConfigurationServiceImpl.class)) {
            // export the service to be used by SAL
            Dictionary<String, Object> props = new Hashtable<>();
            c.setInterface(new String[] { OvsdbConfigurationService.class.getName()}, props);
            c.add(createServiceDependency()
                    .setService(org.opendaylight.ovsdb.plugin.api.OvsdbConfigurationService.class)
                    .setCallbacks("setOvsdbConfigurationService", "unsetOvsdbConfigurationService")
                    .setRequired(true));
        }

        if (imp.equals(ConnectionServiceImpl.class)) {
            // export the service to be used by SAL
            Dictionary<String, Object> props = new Hashtable<>();
            c.setInterface(
                    new String[] {OvsdbConnectionService.class.getName()}, props);
            c.add(createServiceDependency()
                    .setService(org.opendaylight.ovsdb.plugin.api.OvsdbConnectionService.class)
                    .setCallbacks("setOvsdbConnectionService", "unsetOvsdbConnectionService")
                    .setRequired(true));
        }

        if (imp.equals(InventoryServiceImpl.class)) {
            Dictionary<String, Object> props = new Hashtable<>();
            c.setInterface(
                    new String[]{OvsdbInventoryService.class.getName(),
                            org.opendaylight.ovsdb.plugin.api.OvsdbInventoryListener.class.getName()}, props);
            c.add(createServiceDependency()
                    .setService(org.opendaylight.ovsdb.plugin.api.OvsdbInventoryService.class)
                    .setCallbacks("setOvsdbInventoryService", "unsetOvsdbInventoryService")
                    .setRequired(true));
            c.add(createServiceDependency()
                    .setService(OvsdbInventoryListener.class)
                    .setCallbacks("addOvsdbInventoryListener", "removeOvsdbInventoryListener")
                    .setRequired(true));
        }

    }
}
