/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Brent Salisbury
 */
package org.opendaylight.ovsdb.lib.wrapper;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.dm.Component;
import org.opendaylight.controller.sal.core.ComponentActivatorAbstractBase;
import org.opendaylight.ovsdb.lib.OvsdbConnection;
import org.opendaylight.ovsdb.lib.OvsdbConnectionListener;
import org.opendaylight.ovsdb.lib.impl.OvsdbConnectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OVSDB protocol plugin Activator
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
    }

    /**
     * Function called when the activator stops just before the cleanup done by
     * ComponentActivatorAbstractBase
     *
     */
    @Override
    public void destroy() {
    }
    @Override
    public Object[] getGlobalImplementations() {
        Object[] res = { OvsdbConnectionService.class };
        return res;
    }

    @Override
    public void configureGlobalInstance(Component c, Object imp){
        if (imp.equals(OvsdbConnectionService.class)) {
            // export the service to be used by SAL
            Dictionary<String, Object> props = new Hashtable<String, Object>();
            c.setInterface(new String[] { OvsdbConnection.class.getName()}, null);

            c.add(createServiceDependency()
                    .setService(OvsdbConnectionListener.class)
                    .setCallbacks("registerForPassiveConnection", "unregisterFromPassiveConnection")
                    .setRequired(false));
        }
    }
}
