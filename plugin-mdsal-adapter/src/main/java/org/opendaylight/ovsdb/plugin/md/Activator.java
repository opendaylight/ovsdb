/*
 * Copyright (c) 2013, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.plugin.md;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.core.ComponentActivatorAbstractBase;
import org.opendaylight.ovsdb.plugin.api.OvsdbConfigurationService;
import org.opendaylight.ovsdb.plugin.api.OvsdbInventoryListener;

import org.apache.felix.dm.Component;

/**
 * OSGi Bundle Activator for the Neutron providers
 */
public class Activator extends ComponentActivatorAbstractBase {
    /**
     * Function called when the activator starts just after some
     * initializations are done by the
     * ComponentActivatorAbstractBase.
     */
    @Override
    public void init() {
    }

    /**
     * Function called when the activator stops just before the
     * cleanup done by ComponentActivatorAbstractBase.
     *
     */
    @Override
    public void destroy() {
    }

    /**
     * Function that is used to communicate to dependency manager the
     * list of known implementations for services inside a container.
     *
     * @return An array containing all the CLASS objects that will be
     * instantiated in order to get an fully working implementation
     * Object
     */
    @Override
    public Object[] getImplementations() {
        Object[] res = {OvsdbBindingAwareProviderImpl.class,
                        OvsdbInventoryManager.class };
        return res;
    }

    /**
     * Function that is called when configuration of the dependencies
     * is required.
     *
     * @param c dependency manager Component object, used for
     * configuring the dependencies exported and imported
     * @param imp Implementation class that is being configured,
     * needed as long as the same routine can configure multiple
     * implementations
     * @param containerName The containerName being configured, this allow
     * also optional per-container different behavior if needed, usually
     * should not be the case though.
     */
    @Override
    public void configureInstance(Component c, Object imp,
                                  String containerName) {

        if (imp.equals(OvsdbBindingAwareProviderImpl.class)) {
            c.setInterface(OvsdbBindingAwareProvider.class.getName(), null);
            c.add(createServiceDependency()
                          .setService(BindingAwareBroker.class)
                          .setRequired(true));
        }

        if (imp.equals(OvsdbInventoryManager.class)) {
            c.setInterface(OvsdbInventoryListener.class.getName(), null);
            c.add(createServiceDependency()
                          .setService(OvsdbBindingAwareProvider.class)
                          .setRequired(true));
            c.add(createServiceDependency()
                          .setService(OvsdbConfigurationService.class)
                          .setRequired(true));
        }
    }
}
