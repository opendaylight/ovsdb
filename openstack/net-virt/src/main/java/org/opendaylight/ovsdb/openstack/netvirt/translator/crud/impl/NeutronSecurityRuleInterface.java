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
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronSecurityRule;
import org.opendaylight.ovsdb.openstack.netvirt.translator.crud.INeutronSecurityRuleCRUD;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev141002.security.rules.attributes.security.rules.SecurityRule;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;


public class NeutronSecurityRuleInterface extends AbstractNeutronInterface<SecurityRule, NeutronSecurityRule> implements INeutronSecurityRuleCRUD {

    NeutronSecurityRuleInterface(ProviderContext providerContext) {
        super(providerContext);
    }

    @Override
    public boolean neutronSecurityRuleExists(String uuid) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public NeutronSecurityRule getNeutronSecurityRule(String uuid) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<NeutronSecurityRule> getAllNeutronSecurityRules() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean addNeutronSecurityRule(NeutronSecurityRule input) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean removeNeutronSecurityRule(String uuid) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean updateNeutronSecurityRule(String uuid,
            NeutronSecurityRule delta) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean neutronSecurityRuleInUse(String uuid) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected InstanceIdentifier<SecurityRule> createInstanceIdentifier(
            SecurityRule item) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected SecurityRule toMd(NeutronSecurityRule neutronObject) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected SecurityRule toMd(String uuid) {
        // TODO Auto-generated method stub
        return null;
    }

    public static void registerNewInterface(BundleContext context,
                                            ProviderContext providerContext,
                                            List<ServiceRegistration<?>> registrations) {
        NeutronSecurityRuleInterface neutronSecurityRuleInterface = new NeutronSecurityRuleInterface(providerContext);
        ServiceRegistration<INeutronSecurityRuleCRUD> neutronSecurityRuleInterfaceRegistration = context.registerService(INeutronSecurityRuleCRUD.class, neutronSecurityRuleInterface, null);
        if(neutronSecurityRuleInterfaceRegistration != null) {
            registrations.add(neutronSecurityRuleInterfaceRegistration);
        }
    }
}
