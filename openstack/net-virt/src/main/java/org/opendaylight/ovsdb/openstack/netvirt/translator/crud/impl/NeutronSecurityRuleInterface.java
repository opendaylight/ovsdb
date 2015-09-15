/*
 * Copyright (c) 2014, 2015 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.translator.crud.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronSecurityGroup;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronSecurityRule;
import org.opendaylight.ovsdb.openstack.netvirt.translator.crud.INeutronSecurityGroupCRUD;
import org.opendaylight.ovsdb.openstack.netvirt.translator.crud.INeutronSecurityRuleCRUD;
import org.opendaylight.ovsdb.openstack.netvirt.translator.crud.NeutronCRUDInterfaces;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev160807.DirectionBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev160807.DirectionEgress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev160807.DirectionIngress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev160807.EthertypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev160807.EthertypeV4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev160807.EthertypeV6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev160807.ProtocolBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev160807.ProtocolHttp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev160807.ProtocolHttps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev160807.ProtocolIcmp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev160807.ProtocolTcp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150325.Neutron;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev141002.security.rules.attributes.SecurityRules;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev141002.security.rules.attributes.security.rules.SecurityRule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev141002.security.rules.attributes.security.rules.SecurityRuleBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableBiMap;


public class NeutronSecurityRuleInterface extends AbstractNeutronInterface<SecurityRule, NeutronSecurityRule> implements INeutronSecurityRuleCRUD {

    private static final Logger LOGGER = LoggerFactory.getLogger(NeutronSecurityRuleInterface.class);

    private static final ImmutableBiMap<Class<? extends DirectionBase>,String> DIRECTION_MAP
            = new ImmutableBiMap.Builder<Class<? extends DirectionBase>,String>()
            .put(DirectionEgress.class,"egress")
            .put(DirectionIngress.class,"ingress")
            .build();
    private static final ImmutableBiMap<Class<? extends ProtocolBase>,String> PROTOCOL_MAP
            = new ImmutableBiMap.Builder<Class<? extends ProtocolBase>,String>()
            .put(ProtocolHttp.class,"HTTP")
            .put(ProtocolHttps.class,"HTTPS")
            .put(ProtocolIcmp.class,"ICMP")
            .put(ProtocolTcp.class,"TCP")
            .build();
    private static final ImmutableBiMap<Class<? extends EthertypeBase>,String> ETHERTYPE_MAP
            = new ImmutableBiMap.Builder<Class<? extends EthertypeBase>,String>()
            .put(EthertypeV4.class,"v4")
            .put(EthertypeV6.class,"v6")
            .build();

    NeutronSecurityRuleInterface(ProviderContext providerContext) {
        super(providerContext);
    }

    private void updateSecGroupRuleInSecurityGroup(NeutronSecurityRule input) {
        NeutronCRUDInterfaces interfaces = new NeutronCRUDInterfaces()
            .fetchINeutronSecurityGroupCRUD(this);
        INeutronSecurityGroupCRUD sgCrud = interfaces.getSecurityGroupInterface();
        NeutronSecurityGroup sg = sgCrud.getNeutronSecurityGroup(input.getSecurityRuleGroupID());
        if(sg != null && sg.getSecurityRules() != null) {
            for(NeutronSecurityRule sgr :sg.getSecurityRules()) {
                if(sgr != null && sgr.getID() != null && sgr.getID().equals(input.getID())) {
                    int index = sg.getSecurityRules().indexOf(sgr);
                    sg.getSecurityRules().set(index, input);
                }
            }
        }
        if (sg != null) {
            sg.getSecurityRules().add(input);
        }
    }

    private void removeSecGroupRuleFromSecurityGroup(NeutronSecurityRule input) {
        NeutronCRUDInterfaces interfaces = new NeutronCRUDInterfaces()
            .fetchINeutronSecurityGroupCRUD(this);
        INeutronSecurityGroupCRUD sgCrud = interfaces.getSecurityGroupInterface();
        NeutronSecurityGroup sg = sgCrud.getNeutronSecurityGroup(input.getSecurityRuleGroupID());
        if(sg != null && sg.getSecurityRules() != null) {
            List<NeutronSecurityRule> toRemove = new ArrayList<NeutronSecurityRule>();
            for(NeutronSecurityRule sgr :sg.getSecurityRules()) {
                if(sgr.getID() != null && sgr.getID().equals(input.getID())) {
                    toRemove.add(sgr);
                }
            }
            sg.getSecurityRules().removeAll(toRemove);
        }
    }

    @Override
    public boolean neutronSecurityRuleExists(String uuid) {
        SecurityRule rule = readMd(createInstanceIdentifier(toMd(uuid)));
        if (rule == null) {
            return false;
        }
        return true;
    }

    @Override
    public NeutronSecurityRule getNeutronSecurityRule(String uuid) {
        SecurityRule rule = readMd(createInstanceIdentifier(toMd(uuid)));
        if (rule == null) {
            return null;
        }
        return fromMd(rule);
    }

    @Override
    public List<NeutronSecurityRule> getAllNeutronSecurityRules() {
        Set<NeutronSecurityRule> allSecurityRules = new HashSet<NeutronSecurityRule>();
        SecurityRules rules = readMd(createInstanceIdentifier());
        if (rules != null) {
            for (SecurityRule rule: rules.getSecurityRule()) {
                allSecurityRules.add(fromMd(rule));
            }
        }
        LOGGER.debug("Exiting getSecurityRule, Found {} OpenStackSecurityRule", allSecurityRules.size());
        List<NeutronSecurityRule> ans = new ArrayList<NeutronSecurityRule>();
        ans.addAll(allSecurityRules);
        return ans;
    }

    @Override
    public boolean addNeutronSecurityRule(NeutronSecurityRule input) {
        if (neutronSecurityRuleExists(input.getID())) {
            return false;
        }
        updateSecGroupRuleInSecurityGroup(input);
        addMd(input);
        return true;
    }

    @Override
    public boolean removeNeutronSecurityRule(String uuid) {
        if (!neutronSecurityRuleExists(uuid)) {
            return false;
        }
        removeSecGroupRuleFromSecurityGroup(getNeutronSecurityRule(uuid));
        removeMd(toMd(uuid));
        return true;
    }

    @Override
    public boolean updateNeutronSecurityRule(String uuid, NeutronSecurityRule delta) {
        if (!neutronSecurityRuleExists(uuid)) {
            return false;
        }
        updateSecGroupRuleInSecurityGroup(delta);
        updateMd(delta);
        return true;
    }

    @Override
    public boolean neutronSecurityRuleInUse(String securityRuleUUID) {
        return !neutronSecurityRuleExists(securityRuleUUID);
    }

    protected NeutronSecurityRule fromMd(SecurityRule rule) {
        NeutronSecurityRule answer = new NeutronSecurityRule();
        if (rule.getTenantId() != null) {
            answer.setSecurityRuleTenantID(rule.getTenantId().getValue().replace("-",""));
        }
        if (rule.getDirection() != null) {
            answer.setSecurityRuleDirection(DIRECTION_MAP.get(rule.getDirection()));
        }
        if (rule.getSecurityGroupId() != null) {
            answer.setSecurityRuleGroupID(rule.getSecurityGroupId().getValue());
        }
        if (rule.getRemoteGroupId() != null) {
            answer.setSecurityRemoteGroupID(rule.getRemoteGroupId().getValue());
        }
        if (rule.getRemoteIpPrefix() != null) {
            answer.setSecurityRuleRemoteIpPrefix(rule.getRemoteIpPrefix());
        }
        if (rule.getProtocol() != null) {
            answer.setSecurityRuleProtocol(PROTOCOL_MAP.get(rule.getProtocol()));
        }
        if (rule.getEthertype() != null) {
            answer.setSecurityRuleEthertype(ETHERTYPE_MAP.get(rule.getEthertype()));
        }
        if (rule.getPortRangeMin() != null) {
            answer.setSecurityRulePortMin(Integer.valueOf(rule.getPortRangeMin()));
        }
        if (rule.getPortRangeMax() != null) {
            answer.setSecurityRulePortMax(Integer.valueOf(rule.getPortRangeMax()));
        }
        if (rule.getId() != null) {
            answer.setID(rule.getId().getValue());
        }
        return answer;
    }

    @Override
    protected SecurityRule toMd(NeutronSecurityRule securityRule) {
        SecurityRuleBuilder securityRuleBuilder = new SecurityRuleBuilder();

        if (securityRule.getSecurityRuleTenantID() != null) {
            securityRuleBuilder.setTenantId(toUuid(securityRule.getSecurityRuleTenantID()));
        }
        if (securityRule.getSecurityRuleDirection() != null) {
            ImmutableBiMap<String, Class<? extends DirectionBase>> mapper =
                    DIRECTION_MAP.inverse();
            securityRuleBuilder.setDirection((Class<? extends DirectionBase>) mapper.get(securityRule.getSecurityRuleDirection()));
        }
        if (securityRule.getSecurityRuleGroupID() != null) {
            securityRuleBuilder.setSecurityGroupId(toUuid(securityRule.getSecurityRuleGroupID()));
        }
        if (securityRule.getSecurityRemoteGroupID() != null) {
            securityRuleBuilder.setRemoteGroupId(toUuid(securityRule.getSecurityRemoteGroupID()));
        }
        if (securityRule.getSecurityRuleRemoteIpPrefix() != null) {
            securityRuleBuilder.setRemoteIpPrefix(securityRule.getSecurityRuleRemoteIpPrefix());
        }
        if (securityRule.getSecurityRuleProtocol() != null) {
            ImmutableBiMap<String, Class<? extends ProtocolBase>> mapper =
                    PROTOCOL_MAP.inverse();
            securityRuleBuilder.setProtocol((Class<? extends ProtocolBase>) mapper.get(securityRule.getSecurityRuleProtocol()));
        }
        if (securityRule.getSecurityRuleEthertype() != null) {
            ImmutableBiMap<String, Class<? extends EthertypeBase>> mapper =
                    ETHERTYPE_MAP.inverse();
            securityRuleBuilder.setEthertype((Class<? extends EthertypeBase>) mapper.get(securityRule.getSecurityRuleEthertype()));
        }
        if (securityRule.getSecurityRulePortMin() != null) {
            securityRuleBuilder.setPortRangeMin(Integer.valueOf(securityRule.getSecurityRulePortMin()));
        }
        if (securityRule.getSecurityRulePortMax() != null) {
            securityRuleBuilder.setPortRangeMax(Integer.valueOf(securityRule.getSecurityRulePortMax()));
        }
        if (securityRule.getID() != null) {
            securityRuleBuilder.setId(toUuid(securityRule.getID()));
        } else {
            LOGGER.warn("Attempting to write neutron securityRule without UUID");
        }
        return securityRuleBuilder.build();
    }

    @Override
    protected InstanceIdentifier<SecurityRule> createInstanceIdentifier(SecurityRule securityRule) {
        return InstanceIdentifier.create(Neutron.class)
            .child(SecurityRules.class).child(SecurityRule.class,
                                              securityRule.getKey());
    }

    protected InstanceIdentifier<SecurityRules> createInstanceIdentifier() {
        return InstanceIdentifier.create(Neutron.class)
            .child(SecurityRules.class);
    }

    @Override
    protected SecurityRule toMd(String uuid) {
        SecurityRuleBuilder securityRuleBuilder = new SecurityRuleBuilder();
        securityRuleBuilder.setId(toUuid(uuid));
        return securityRuleBuilder.build();
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
