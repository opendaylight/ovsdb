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
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronSecurityGroup;
import org.opendaylight.ovsdb.openstack.netvirt.translator.crud.INeutronSecurityGroupCRUD;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev141002.security.groups.attributes.security.groups.SecurityGroup;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;


public class NeutronSecurityGroupInterface extends AbstractNeutronInterface<SecurityGroup,NeutronSecurityGroup> implements INeutronSecurityGroupCRUD {

    NeutronSecurityGroupInterface(ProviderContext providerContext) {
        super(providerContext);
    }

    @Override
    public boolean neutronSecurityGroupExists(String uuid) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public NeutronSecurityGroup getNeutronSecurityGroup(String uuid) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<NeutronSecurityGroup> getAllNeutronSecurityGroups() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean addNeutronSecurityGroup(NeutronSecurityGroup input) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean removeNeutronSecurityGroup(String uuid) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean updateNeutronSecurityGroup(String uuid,
            NeutronSecurityGroup delta) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean neutronSecurityGroupInUse(String uuid) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected InstanceIdentifier<SecurityGroup> createInstanceIdentifier(
            SecurityGroup item) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected SecurityGroup toMd(NeutronSecurityGroup neutronObject) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected SecurityGroup toMd(String uuid) {
        // TODO Auto-generated method stub
        return null;
    }

    public static void registerNewInterface(BundleContext context,
                                            ProviderContext providerContext,
                                            List<ServiceRegistration<?>> registrations) {
        NeutronSecurityGroupInterface neutronSecurityGroupInterface = new NeutronSecurityGroupInterface(providerContext);
        ServiceRegistration<INeutronSecurityGroupCRUD> neutronSecurityGroupInterfaceRegistration = context.registerService(INeutronSecurityGroupCRUD.class, neutronSecurityGroupInterface, null);
        if(neutronSecurityGroupInterfaceRegistration != null) {
            registrations.add(neutronSecurityGroupInterfaceRegistration);
        }
    }
}
