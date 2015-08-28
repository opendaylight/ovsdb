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
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronLoadBalancerListener;
import org.opendaylight.ovsdb.openstack.netvirt.translator.crud.INeutronLoadBalancerListenerCRUD;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.lbaasv2.rev141002.lbaas.attributes.listener.Listeners;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class NeutronLoadBalancerListenerInterface extends AbstractNeutronInterface<Listeners, NeutronLoadBalancerListener> implements INeutronLoadBalancerListenerCRUD {

    NeutronLoadBalancerListenerInterface(ProviderContext providerContext) {
        super(providerContext);
    }

    @Override
    public boolean neutronLoadBalancerListenerExists(String uuid) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public NeutronLoadBalancerListener getNeutronLoadBalancerListener(
            String uuid) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<NeutronLoadBalancerListener> getAllNeutronLoadBalancerListeners() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean addNeutronLoadBalancerListener(
            NeutronLoadBalancerListener input) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean removeNeutronLoadBalancerListener(String uuid) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean updateNeutronLoadBalancerListener(String uuid,
            NeutronLoadBalancerListener delta) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean neutronLoadBalancerListenerInUse(String uuid) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected InstanceIdentifier<Listeners> createInstanceIdentifier(
            Listeners item) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected Listeners toMd(NeutronLoadBalancerListener neutronObject) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected Listeners toMd(String uuid) {
        // TODO Auto-generated method stub
        return null;
    }

    public static void registerNewInterface(BundleContext context,
                                            ProviderContext providerContext,
                                            List<ServiceRegistration<?>> registrations) {
        NeutronLoadBalancerListenerInterface neutronLoadBalancerListenerInterface = new NeutronLoadBalancerListenerInterface(providerContext);
        ServiceRegistration<INeutronLoadBalancerListenerCRUD> neutronLoadBalancerListenerInterfaceRegistration = context.registerService(INeutronLoadBalancerListenerCRUD.class, neutronLoadBalancerListenerInterface, null);
        if(neutronLoadBalancerListenerInterfaceRegistration != null) {
            registrations.add(neutronLoadBalancerListenerInterfaceRegistration);
        }
    }

}
