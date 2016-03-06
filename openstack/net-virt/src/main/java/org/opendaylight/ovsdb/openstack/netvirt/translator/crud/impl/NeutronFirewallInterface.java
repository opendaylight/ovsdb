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
import org.opendaylight.ovsdb.openstack.netvirt.translator.INeutronObject;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronFirewall;
import org.opendaylight.ovsdb.openstack.netvirt.translator.crud.INeutronFirewallCRUD;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class NeutronFirewallInterface extends AbstractNeutronInterface implements INeutronFirewallCRUD {

    NeutronFirewallInterface(ProviderContext providerContext) {
        super(providerContext);
    }

    public static void registerNewInterface(BundleContext context,
                                            ProviderContext providerContext,
                                            List<ServiceRegistration<?>> registrations) {
        NeutronFirewallInterface neutronFirewallInterface = new NeutronFirewallInterface(providerContext);
        ServiceRegistration<INeutronFirewallCRUD> neutronFirewallInterfaceRegistration = context.registerService(INeutronFirewallCRUD.class, neutronFirewallInterface, null);
        if(neutronFirewallInterfaceRegistration != null) {
            registrations.add(neutronFirewallInterfaceRegistration);
        }
    }

    @Override
    public boolean neutronFirewallExists(String uuid) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public NeutronFirewall getNeutronFirewall(String uuid) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<NeutronFirewall> getAllNeutronFirewalls() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean addNeutronFirewall(NeutronFirewall input) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean removeNeutronFirewall(String uuid) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean updateNeutronFirewall(String uuid, NeutronFirewall delta) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean neutronFirewallInUse(String uuid) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected InstanceIdentifier createInstanceIdentifier(DataObject item) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected DataObject toMd(INeutronObject neutronObject) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected DataObject toMd(String uuid) {
        // TODO Auto-generated method stub
        return null;
    }
}
