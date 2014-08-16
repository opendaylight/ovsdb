/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Brent Salisbury, Dave Tucker
 */

package org.opendaylight.ovsdb.loadbalancer;

import org.apache.felix.dm.Component;
import org.opendaylight.controller.containermanager.IContainerManager;
import org.opendaylight.controller.forwardingrulesmanager.IForwardingRulesManager;
import org.opendaylight.controller.networkconfig.neutron.INeutronNetworkCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronPortCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronSecurityGroupCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronSecurityRuleCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronSubnetCRUD;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.core.ComponentActivatorAbstractBase;
import org.opendaylight.ovsdb.plugin.IConnectionServiceInternal;
import org.opendaylight.ovsdb.plugin.OVSDBConfigService;

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
        Object[] res = {LoadBalancer.class};
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
        if (imp.equals(LoadBalancer.class)) {
            c.setInterface(LoadBalancer.class.getName(), null);
        }


        //ToDo: DT: We don't need these dependencies for every implementation...
        //ToDo: DT: Callbacks are only required when behaviour is more complex than simple set/unset operation
        c.add(createServiceDependency().
                setService(OVSDBConfigService.class).
                setCallbacks("setOVSDBConfigService", "unsetOVSDBConfigService").
                setRequired(true));

        c.add(createServiceDependency().
                setService(IConnectionServiceInternal.class).
                setCallbacks("setConnectionService", "unsetConnectionService").
                setRequired(true));

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
        c.add(createServiceDependency().
            setService(INeutronSecurityRuleCRUD.class).
            setCallbacks("setNeutronSecurityRuleCRUD", "unsetNeutronSecurityRuleCRUD").
            setRequired(true));
        c.add(createServiceDependency().
            setService(INeutronSecurityGroupCRUD.class).
            setCallbacks("setNeutronSecurityGroupCRUD", "unsetNeutronSecurityGroupCRUD").
            setRequired(true));
    }
}
