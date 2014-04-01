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

import org.apache.felix.dm.Component;
import org.opendaylight.controller.containermanager.IContainerManager;
import org.opendaylight.controller.forwardingrulesmanager.IForwardingRulesManager;
import org.opendaylight.controller.networkconfig.neutron.INeutronNetworkAware;
import org.opendaylight.controller.networkconfig.neutron.INeutronNetworkCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronPortAware;
import org.opendaylight.controller.networkconfig.neutron.INeutronPortCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronSubnetAware;
import org.opendaylight.controller.networkconfig.neutron.INeutronSubnetCRUD;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.core.ComponentActivatorAbstractBase;
import org.opendaylight.controller.switchmanager.IInventoryListener;
import org.opendaylight.ovsdb.neutron.provider.IProviderNetworkManager;
import org.opendaylight.ovsdb.neutron.provider.ProviderNetworkManager;
import org.opendaylight.ovsdb.plugin.IConnectionServiceInternal;
import org.opendaylight.ovsdb.plugin.OVSDBConfigService;
import org.opendaylight.ovsdb.plugin.OVSDBInventoryListener;

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
        Object[] res = {AdminConfigManager.class,
                        InternalNetworkManager.class,
                        TenantNetworkManager.class,
                        NetworkHandler.class,
                        SubnetHandler.class,
                        PortHandler.class,
                        SouthboundHandler.class,
                        MDSALConsumer.class,
                        ProviderNetworkManager.class};
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
        if (imp.equals(AdminConfigManager.class)) {
            c.setInterface(IAdminConfigManager.class.getName(), null);
        }

        if (imp.equals(InternalNetworkManager.class)) {
            c.setInterface(IInternalNetworkManager.class.getName(), null);
            c.add(createServiceDependency().setService(IAdminConfigManager.class).setRequired(true));
            c.add(createServiceDependency()
                    .setService(IProviderNetworkManager.class)
                    .setCallbacks("setProviderNetworkManager", "unsetProviderNetworkManager"));
        }

        if (imp.equals(TenantNetworkManager.class)) {
            c.setInterface(ITenantNetworkManager.class.getName(), null);
            c.add(createServiceDependency()
                    .setService(IProviderNetworkManager.class)
                    .setCallbacks("setProviderNetworkManager", "unsetProviderNetworkManager"));
        }

        if (imp.equals(NetworkHandler.class)) {
            c.setInterface(INeutronNetworkAware.class.getName(), null);
            c.add(createServiceDependency().setService(ITenantNetworkManager.class).setRequired(true));
        }

        if (imp.equals(SubnetHandler.class)) {
            c.setInterface(INeutronSubnetAware.class.getName(), null);
        }

        if (imp.equals(PortHandler.class)) {
            c.setInterface(INeutronPortAware.class.getName(), null);
        }

        if (imp.equals(SouthboundHandler.class)) {
            c.setInterface(new String[] {OVSDBInventoryListener.class.getName(), IInventoryListener.class.getName()}, null);
            c.add(createServiceDependency().setService(IAdminConfigManager.class).setRequired(true));
            c.add(createServiceDependency().setService(IInternalNetworkManager.class).setRequired(true));
            c.add(createServiceDependency().setService(ITenantNetworkManager.class).setRequired(true));
            c.add(createServiceDependency().setService(IProviderNetworkManager.class).setRequired(true));
        }

        if (imp.equals(MDSALConsumer.class)) {
            c.setInterface(IMDSALConsumer.class.getName(), null);
        }

        if (imp.equals(ProviderNetworkManager.class)) {
            c.setInterface(IProviderNetworkManager.class.getName(), null);
            c.add(createServiceDependency()
                    .setService(IAdminConfigManager.class)
                    .setRequired(true));
            c.add(createServiceDependency()
                    .setService(IInternalNetworkManager.class)
                    .setRequired(true));
            c.add(createServiceDependency()
                    .setService(ITenantNetworkManager.class)
                    .setRequired(true));
        }

        c.add(createServiceDependency().
                setService(OVSDBConfigService.class).
                setCallbacks("setOVSDBConfigService", "unsetOVSDBConfigService").
                setRequired(true));

        c.add(createServiceDependency().
                setService(IConnectionServiceInternal.class).
                setCallbacks("setConnectionService", "unsetConnectionService").
                setRequired(true));

        // Create service dependencies.
        c.add(createServiceDependency().
              setService(IContainerManager.class).
              setCallbacks("setContainerManager", "unsetContainerManager").
              setRequired(true));

        c.add(createServiceDependency().
                setService(IForwardingRulesManager.class).
                setCallbacks("setForwardingRulesManager", "unsetForwardingRulesManager").
                setRequired(true));

        c.add(createServiceDependency()
                .setService(BindingAwareBroker.class)
                .setCallbacks("setBindingAwareBroker", "unsetBindingAwareBroker")
                .setRequired(true));

        c.add(createServiceDependency().
                setService(INeutronNetworkCRUD.class).
                setCallbacks("setNeutronNetworkCRUD", "unsetNeutronNetworkCRUD").
                setRequired(true));
        c.add(createServiceDependency().
                setService(INeutronSubnetCRUD.class).
                setCallbacks("setNeutronSubnetCRUD", "unsetNeutronSubnetCRUD").
                setRequired(true));
        c.add(createServiceDependency().
                setService(INeutronPortCRUD.class).
                setCallbacks("setNeutronPortCRUD", "unsetNeutronPortCRUD").
                setRequired(true));
    }
}
