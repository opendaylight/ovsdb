/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.loadbalancer.internal;

import org.apache.felix.dm.Component;
import org.opendaylight.controller.forwardingrulesmanager.IForwardingRulesManager;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.core.ComponentActivatorAbstractBase;

/**
 * OSGi bundle activator for the OVSDB LoadBalancer Interface.
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
        Object[] res = {LoadBalancerService.class,
                        MDSALConsumer.class };
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


        if (imp.equals(MDSALConsumer.class)) {
            c.setInterface(IMDSALConsumer.class.getName(), null);
        }
        c.add(createServiceDependency().
                setService(IForwardingRulesManager.class).
                setCallbacks("setForwardingRulesManager", "unsetForwardingRulesManager").
                setRequired(true));

        c.add(createServiceDependency()
                .setService(BindingAwareBroker.class)
                .setCallbacks("setBindingAwareBroker", "unsetBindingAwareBroker")
                .setRequired(true));

    }
}

