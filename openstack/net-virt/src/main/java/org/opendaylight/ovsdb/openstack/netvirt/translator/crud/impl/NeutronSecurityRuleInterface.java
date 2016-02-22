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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.DirectionBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.DirectionEgress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.DirectionIngress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.EthertypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.EthertypeV4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.EthertypeV6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.ProtocolBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.ProtocolIcmp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.ProtocolIcmpV6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.ProtocolTcp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev150712.ProtocolUdp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150712.Neutron;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev150712.security.rules.attributes.SecurityRules;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev150712.security.rules.attributes.security.rules.SecurityRule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev150712.security.rules.attributes.security.rules.SecurityRuleBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableBiMap;


public class NeutronSecurityRuleInterface extends AbstractNeutronInterface<SecurityRule, NeutronSecurityRule> implements INeutronSecurityRuleCRUD {

    private static final Logger LOGGER = LoggerFactory.getLogger(NeutronSecurityRuleInterface.class);

    private static final ImmutableBiMap<Class<? extends DirectionBase>, String> DIRECTION_MAP = ImmutableBiMap.of(
            DirectionEgress.class, NeutronSecurityRule.DIRECTION_EGRESS,
            DirectionIngress.class, NeutronSecurityRule.DIRECTION_INGRESS);
    private static final ImmutableBiMap<Class<? extends ProtocolBase>, String> PROTOCOL_MAP = ImmutableBiMap.of(
            ProtocolIcmp.class, NeutronSecurityRule.PROTOCOL_ICMP,
            ProtocolTcp.class, NeutronSecurityRule.PROTOCOL_TCP,
            ProtocolUdp.class, NeutronSecurityRule.PROTOCOL_UDP,
            ProtocolIcmpV6.class, NeutronSecurityRule.PROTOCOL_ICMPV6);
    private static final ImmutableBiMap<Class<? extends EthertypeBase>,String> ETHERTYPE_MAP = ImmutableBiMap.of(
            EthertypeV4.class, NeutronSecurityRule.ETHERTYPE_IPV4,
            EthertypeV6.class, NeutronSecurityRule.ETHERTYPE_IPV6);

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
            List<NeutronSecurityRule> toRemove = new ArrayList<>();
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
        return rule != null;
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
        Set<NeutronSecurityRule> allSecurityRules = new HashSet<>();
        SecurityRules rules = readMd(createInstanceIdentifier());
        if (rules != null) {
            for (SecurityRule rule: rules.getSecurityRule()) {
                allSecurityRules.add(fromMd(rule));
            }
        }
        LOGGER.debug("Exiting getSecurityRule, Found {} OpenStackSecurityRule", allSecurityRules.size());
        return new ArrayList<>(allSecurityRules);
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
            answer.setSecurityRuleRemoteIpPrefix(rule.getRemoteIpPrefix().getIpv4Prefix() != null?
                    rule.getRemoteIpPrefix().getIpv4Prefix().getValue():rule.getRemoteIpPrefix().getIpv6Prefix().getValue());
        }
        if (rule.getProtocol() != null) {
            answer.setSecurityRuleProtocol(PROTOCOL_MAP.get(rule.getProtocol()));
        }
        if (rule.getEthertype() != null) {
            answer.setSecurityRuleEthertype(ETHERTYPE_MAP.get(rule.getEthertype()));
        }
        if (rule.getPortRangeMin() != null) {
            answer.setSecurityRulePortMin(rule.getPortRangeMin());
        }
        if (rule.getPortRangeMax() != null) {
            answer.setSecurityRulePortMax(rule.getPortRangeMax());
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
            securityRuleBuilder.setDirection(mapper.get(securityRule.getSecurityRuleDirection()));
        }
        if (securityRule.getSecurityRuleGroupID() != null) {
            securityRuleBuilder.setSecurityGroupId(toUuid(securityRule.getSecurityRuleGroupID()));
        }
        if (securityRule.getSecurityRemoteGroupID() != null) {
            securityRuleBuilder.setRemoteGroupId(toUuid(securityRule.getSecurityRemoteGroupID()));
        }
        if (securityRule.getSecurityRuleRemoteIpPrefix() != null) {
            securityRuleBuilder.setRemoteIpPrefix(new IpPrefix(securityRule.getSecurityRuleRemoteIpPrefix().toCharArray()));
        }
        if (securityRule.getSecurityRuleProtocol() != null) {
            ImmutableBiMap<String, Class<? extends ProtocolBase>> mapper =
                    PROTOCOL_MAP.inverse();
            securityRuleBuilder.setProtocol(mapper.get(securityRule.getSecurityRuleProtocol()));
        }
        if (securityRule.getSecurityRuleEthertype() != null) {
            ImmutableBiMap<String, Class<? extends EthertypeBase>> mapper =
                    ETHERTYPE_MAP.inverse();
            securityRuleBuilder.setEthertype(mapper.get(securityRule.getSecurityRuleEthertype()));
        }
        if (securityRule.getSecurityRulePortMin() != null) {
            securityRuleBuilder.setPortRangeMin(securityRule.getSecurityRulePortMin());
        }
        if (securityRule.getSecurityRulePortMax() != null) {
            securityRuleBuilder.setPortRangeMax(securityRule.getSecurityRulePortMax());
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
