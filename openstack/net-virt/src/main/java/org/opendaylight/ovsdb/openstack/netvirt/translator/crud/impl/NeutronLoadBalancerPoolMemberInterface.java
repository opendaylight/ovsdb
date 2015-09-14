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
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronLoadBalancerPoolMember;
import org.opendaylight.ovsdb.openstack.netvirt.translator.crud.INeutronLoadBalancerPoolMemberCRUD;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.lbaasv2.rev141002.lbaas.attributes.pools.pool.Members;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class NeutronLoadBalancerPoolMemberInterface extends
        AbstractNeutronInterface<Members, NeutronLoadBalancerPoolMember> implements INeutronLoadBalancerPoolMemberCRUD {

    NeutronLoadBalancerPoolMemberInterface(ProviderContext providerContext) {
        super(providerContext);
    }

    @Override
    public boolean neutronLoadBalancerPoolMemberExists(String uuid) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public NeutronLoadBalancerPoolMember getNeutronLoadBalancerPoolMember(
            String uuid) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<NeutronLoadBalancerPoolMember> getAllNeutronLoadBalancerPoolMembers() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean addNeutronLoadBalancerPoolMember(
            NeutronLoadBalancerPoolMember input) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean removeNeutronLoadBalancerPoolMember(String uuid) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean updateNeutronLoadBalancerPoolMember(String uuid,
            NeutronLoadBalancerPoolMember delta) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean neutronLoadBalancerPoolMemberInUse(String uuid) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected InstanceIdentifier<Members> createInstanceIdentifier(Members item) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected Members toMd(NeutronLoadBalancerPoolMember neutronObject) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected Members toMd(String uuid) {
        // TODO Auto-generated method stub
        return null;
    }

    public static void registerNewInterface(BundleContext context,
                                            ProviderContext providerContext,
                                            List<ServiceRegistration<?>> registrations) {
        NeutronLoadBalancerPoolMemberInterface neutronLoadBalancerPoolMemberInterface = new NeutronLoadBalancerPoolMemberInterface(providerContext);
        ServiceRegistration<INeutronLoadBalancerPoolMemberCRUD> neutronLoadBalancerPoolMemberInterfaceRegistration = context.registerService(INeutronLoadBalancerPoolMemberCRUD.class, neutronLoadBalancerPoolMemberInterface, null);
        if(neutronLoadBalancerPoolMemberInterfaceRegistration != null) {
            registrations.add(neutronLoadBalancerPoolMemberInterfaceRegistration);
        }
    }
}
