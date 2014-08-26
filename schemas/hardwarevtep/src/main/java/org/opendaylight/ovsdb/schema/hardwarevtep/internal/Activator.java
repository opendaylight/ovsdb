/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Brent Salisbury
 */
package org.opendaylight.ovsdb.schema.hardwarevtep.internal;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.opendaylight.ovsdb.lib.schema.typed.SchemaService;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
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
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(TyperUtils.DATABASE_NAME.toString(), "hardware_vtep");

        manager.add(createComponent()
            .setInterface(SchemaService.class.getName(), props)
            .setImplementation(HardwareVtepSchemaService.class)
        );
    }

    @Override
    public void destroy(BundleContext context, DependencyManager manager) throws Exception {}
}
