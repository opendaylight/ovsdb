/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Brent Salisbury, Dave Tucker
 */

package org.opendaylight.ovsdb.neutron.providers;

import org.opendaylight.controller.forwardingrulesmanager.IForwardingRulesManager;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.core.ComponentActivatorAbstractBase;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.ovsdb.neutron.api.BridgeConfigurationManager;
import org.opendaylight.ovsdb.neutron.api.ConfigurationService;
import org.opendaylight.ovsdb.neutron.api.Constants;
import org.opendaylight.ovsdb.neutron.api.NetworkingProvider;
import org.opendaylight.ovsdb.neutron.api.TenantNetworkManager;
import org.opendaylight.ovsdb.plugin.IConnectionServiceInternal;
import org.opendaylight.ovsdb.plugin.OvsdbConfigService;

import org.apache.felix.dm.Component;

import java.util.Properties;

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
        Object[] res = {MdsalConsumerImpl.class,
                        OF10Provider.class,
                        OF13Provider.class};
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

        if (imp.equals(MdsalConsumerImpl.class)) {
            c.setInterface(MdsalConsumer.class.getName(), null);
            c.add(createServiceDependency().setService(BindingAwareBroker.class).setRequired(true));
        }

        if (imp.equals(OF10Provider.class)) {
            Properties of10Properties = new Properties();
            of10Properties.put(Constants.SOUTHBOUND_PROTOCOL_PROPERTY, "ovsdb");
            of10Properties.put(Constants.OPENFLOW_VERSION_PROPERTY, Constants.OPENFLOW10);

            c.setInterface(NetworkingProvider.class.getName(), of10Properties);
            c.add(createServiceDependency()
                          .setService(ConfigurationService.class)
                          .setRequired(true));
            c.add(createServiceDependency()
                          .setService(BridgeConfigurationManager.class)
                          .setRequired(true));
            c.add(createServiceDependency()
                          .setService(TenantNetworkManager.class)
                          .setRequired(true));
            c.add(createServiceDependency().setService(OvsdbConfigService.class).setRequired(true));
            c.add(createServiceDependency().setService(IConnectionServiceInternal.class).setRequired(true));
            c.add(createServiceDependency().
                    setService(IForwardingRulesManager.class).
                    setRequired(true));
            c.add(createServiceDependency().
                    setService(ISwitchManager.class).
                    setRequired(true));
        }

        if (imp.equals(OF13Provider.class)) {
            Properties of13Properties = new Properties();
            of13Properties.put(Constants.SOUTHBOUND_PROTOCOL_PROPERTY, "ovsdb");
            of13Properties.put(Constants.OPENFLOW_VERSION_PROPERTY, Constants.OPENFLOW13);

            c.setInterface(NetworkingProvider.class.getName(), of13Properties);
            c.add(createServiceDependency()
                          .setService(ConfigurationService.class)
                          .setRequired(true));
            c.add(createServiceDependency()
                          .setService(BridgeConfigurationManager.class)
                          .setRequired(true));
            c.add(createServiceDependency()
                          .setService(TenantNetworkManager.class)
                          .setRequired(true));
            c.add(createServiceDependency().setService(OvsdbConfigService.class).setRequired(true));
            c.add(createServiceDependency().setService(IConnectionServiceInternal.class).setRequired(true));
            c.add(createServiceDependency().setService(MdsalConsumer.class).setRequired(true));
        }
    }
}
