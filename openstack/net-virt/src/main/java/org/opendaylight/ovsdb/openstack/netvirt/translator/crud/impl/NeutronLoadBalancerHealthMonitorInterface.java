/*
 * Copyright (c) 2014, 2015 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.translator.crud.impl;

import java.util.List;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronLoadBalancerHealthMonitor;
import org.opendaylight.ovsdb.openstack.netvirt.translator.crud.INeutronLoadBalancerHealthMonitorCRUD;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.lbaasv2.rev141002.lbaas.attributes.Healthmonitors;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class NeutronLoadBalancerHealthMonitorInterface extends AbstractNeutronInterface<Healthmonitors, NeutronLoadBalancerHealthMonitor> implements INeutronLoadBalancerHealthMonitorCRUD {

    NeutronLoadBalancerHealthMonitorInterface(ProviderContext providerContext) {
        super(providerContext);
    }

    @Override
    public boolean neutronLoadBalancerHealthMonitorExists(String uuid) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public NeutronLoadBalancerHealthMonitor getNeutronLoadBalancerHealthMonitor(
            String uuid) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<NeutronLoadBalancerHealthMonitor> getAllNeutronLoadBalancerHealthMonitors() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean addNeutronLoadBalancerHealthMonitor(
            NeutronLoadBalancerHealthMonitor input) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean removeNeutronLoadBalancerHealthMonitor(String uuid) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean updateNeutronLoadBalancerHealthMonitor(String uuid,
            NeutronLoadBalancerHealthMonitor delta) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean neutronLoadBalancerHealthMonitorInUse(String uuid) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected InstanceIdentifier<Healthmonitors> createInstanceIdentifier(
            Healthmonitors item) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected Healthmonitors toMd(NeutronLoadBalancerHealthMonitor neutronObject) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected Healthmonitors toMd(String uuid) {
        // TODO Auto-generated method stub
        return null;
    }

    public static void registerNewInterface(BundleContext context,
                                            ProviderContext providerContext,
                                            List<ServiceRegistration<?>> registrations) {
        NeutronLoadBalancerHealthMonitorInterface neutronLoadBalancerHealthMonitorInterface = new NeutronLoadBalancerHealthMonitorInterface(providerContext);
        ServiceRegistration<INeutronLoadBalancerHealthMonitorCRUD> neutronLoadBalancerHealthMonitorInterfaceRegistration = context.registerService(INeutronLoadBalancerHealthMonitorCRUD.class, neutronLoadBalancerHealthMonitorInterface, null);
        if(neutronLoadBalancerHealthMonitorInterfaceRegistration != null) {
            registrations.add(neutronLoadBalancerHealthMonitorInterfaceRegistration);
        }
    }
}
