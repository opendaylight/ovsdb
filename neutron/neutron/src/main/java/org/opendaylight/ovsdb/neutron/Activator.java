/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Brent Salisbury, Dave Tucker
 */

package org.opendaylight.ovsdb.neutron;

import org.opendaylight.controller.networkconfig.neutron.INeutronNetworkAware;
import org.opendaylight.controller.networkconfig.neutron.INeutronNetworkCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronPortAware;
import org.opendaylight.controller.networkconfig.neutron.INeutronPortCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronSecurityGroupAware;
import org.opendaylight.controller.networkconfig.neutron.INeutronSecurityRuleAware;
import org.opendaylight.controller.networkconfig.neutron.INeutronSubnetAware;
import org.opendaylight.controller.sal.core.ComponentActivatorAbstractBase;
import org.opendaylight.controller.switchmanager.IInventoryListener;
import org.opendaylight.ovsdb.neutron.api.BridgeConfigurationManager;
import org.opendaylight.ovsdb.neutron.api.ConfigurationService;
import org.opendaylight.ovsdb.neutron.api.NetworkingProvider;
import org.opendaylight.ovsdb.neutron.api.NetworkingProviderManager;
import org.opendaylight.ovsdb.neutron.api.TenantNetworkManager;
import org.opendaylight.ovsdb.neutron.api.VlanConfigurationCache;
import org.opendaylight.ovsdb.neutron.impl.BridgeConfigurationManagerImpl;
import org.opendaylight.ovsdb.neutron.impl.ConfigurationServiceImpl;
import org.opendaylight.ovsdb.neutron.impl.TenantNetworkManagerImpl;
import org.opendaylight.ovsdb.neutron.impl.VlanConfigurationCacheImpl;
import org.opendaylight.ovsdb.neutron.impl.ProviderNetworkManagerImpl;
import org.opendaylight.ovsdb.plugin.IConnectionServiceInternal;
import org.opendaylight.ovsdb.plugin.OvsdbConfigService;
import org.opendaylight.ovsdb.plugin.OvsdbInventoryListener;

import org.apache.felix.dm.Component;

/**
 * OSGi bundle activator for the OVSDB Neutron Interface.
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
        Object[] res = {ConfigurationServiceImpl.class,
                        BridgeConfigurationManagerImpl.class,
                        TenantNetworkManagerImpl.class,
                        VlanConfigurationCacheImpl.class,
                        NetworkHandler.class,
                        SubnetHandler.class,
                        PortHandler.class,
                        SouthboundHandler.class,
                        PortSecurityHandler.class,
                        ProviderNetworkManagerImpl.class};
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
        if (imp.equals(ConfigurationServiceImpl.class)) {
            c.setInterface(ConfigurationService.class.getName(), null);
            c.add(createServiceDependency().setService(OvsdbConfigService.class));
        }

        if (imp.equals(BridgeConfigurationManagerImpl.class)) {
            c.setInterface(BridgeConfigurationManager.class.getName(), null);
            c.add(createServiceDependency().setService(ConfigurationService.class).setRequired(true));
            c.add(createServiceDependency().setService(NetworkingProviderManager.class));
            c.add(createServiceDependency().setService(OvsdbConfigService.class));
        }

        if (imp.equals(TenantNetworkManagerImpl.class)) {
            c.setInterface(TenantNetworkManager.class.getName(), null);
            c.add(createServiceDependency().setService(NetworkingProviderManager.class));
            c.add(createServiceDependency().setService(OvsdbConfigService.class));
            c.add(createServiceDependency().setService(IConnectionServiceInternal.class));
            c.add(createServiceDependency().
                    setService(INeutronNetworkCRUD.class).
                    setRequired(true));
            c.add(createServiceDependency().
                    setService(INeutronPortCRUD.class).
                    setRequired(true));
            c.add(createServiceDependency().setService(VlanConfigurationCache.class));
        }

        if (imp.equals(VlanConfigurationCacheImpl.class)) {
            c.setInterface(VlanConfigurationCache.class.getName(), null);
            c.add(createServiceDependency().setService(OvsdbConfigService.class));
            c.add(createServiceDependency().setService(TenantNetworkManager.class));
        }

        if (imp.equals(NetworkHandler.class)) {
            c.setInterface(INeutronNetworkAware.class.getName(), null);
            c.add(createServiceDependency().setService(TenantNetworkManager.class).setRequired(true));
            c.add(createServiceDependency().setService(BridgeConfigurationManager.class).setRequired(true));
            c.add(createServiceDependency().setService(ConfigurationService.class).setRequired(true));
            c.add(createServiceDependency().setService(OvsdbConfigService.class).setRequired(true));
        }

        if (imp.equals(SubnetHandler.class)) {
            c.setInterface(INeutronSubnetAware.class.getName(), null);
        }

        if (imp.equals(PortHandler.class)) {
            c.setInterface(INeutronPortAware.class.getName(), null);
            c.add(createServiceDependency().setService(OvsdbConfigService.class).setRequired(true));
        }

        if (imp.equals(SouthboundHandler.class)) {
            c.setInterface(new String[] {OvsdbInventoryListener.class.getName(),
                                         IInventoryListener.class.getName()}, null);
            c.add(createServiceDependency().setService(ConfigurationService.class).setRequired(true));
            c.add(createServiceDependency().setService(BridgeConfigurationManager.class).setRequired(true));
            c.add(createServiceDependency().setService(TenantNetworkManager.class).setRequired(true));
            c.add(createServiceDependency().setService(NetworkingProviderManager.class).setRequired(true));
            c.add(createServiceDependency().setService(OvsdbConfigService.class).setRequired(true));
            c.add(createServiceDependency().setService(IConnectionServiceInternal.class).setRequired(true));
        }

        if (imp.equals(PortSecurityHandler.class)) {
            c.setInterface(new String[] {INeutronSecurityRuleAware.class.getName(),
                                         INeutronSecurityGroupAware.class.getName()}, null);
        }

        if (imp.equals(ProviderNetworkManagerImpl.class)) {
            c.setInterface(NetworkingProviderManager.class.getName(), null);
            c.add(createServiceDependency()
                    .setService(ConfigurationService.class)
                    .setRequired(true));
            c.add(createServiceDependency()
                    .setService(NetworkingProvider.class)
                    .setCallbacks("providerAdded", "providerRemoved"));
        }
    }
}
