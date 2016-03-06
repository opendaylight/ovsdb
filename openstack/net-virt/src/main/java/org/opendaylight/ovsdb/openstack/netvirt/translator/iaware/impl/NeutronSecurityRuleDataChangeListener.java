/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.openstack.netvirt.translator.iaware.impl;

import java.util.Map.Entry;

import org.opendaylight.controller.md.sal.binding.api.ClusteredDataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronSecurityRule;
import org.opendaylight.ovsdb.openstack.netvirt.translator.iaware.INeutronSecurityRuleAware;
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
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableBiMap;

public class NeutronSecurityRuleDataChangeListener implements ClusteredDataChangeListener, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(NeutronSecurityRuleDataChangeListener.class);

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

    private ListenerRegistration<DataChangeListener> registration;

    public NeutronSecurityRuleDataChangeListener(DataBroker db) {
        InstanceIdentifier<SecurityRule> path = InstanceIdentifier
                .create(Neutron.class).child(SecurityRules.class)
                .child(SecurityRule.class);
        LOG.debug("Register listener for Neutron Secutiry rules model data changes");
        registration = db.registerDataChangeListener(
                LogicalDatastoreType.CONFIGURATION, path, this,
                DataChangeScope.ONE);

    }

    @Override
    public void onDataChanged(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        LOG.trace("Data changes : {}", changes);

        Object[] subscribers = NeutronIAwareUtil.getInstances(
                INeutronSecurityRuleAware.class, this);
        createSecurityRule(changes, subscribers);
        updateSecurityRule(changes, subscribers);
        deleteSecurityRule(changes, subscribers);
    }

    private void createSecurityRule(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes,
            Object[] subscribers) {
        for (Entry<InstanceIdentifier<?>, DataObject> newSecutiryRule : changes
                .getCreatedData().entrySet()) {
            if (newSecutiryRule.getValue() instanceof SecurityRule) {
                NeutronSecurityRule secutiryRule = fromMd((SecurityRule) newSecutiryRule
                        .getValue());
                for (Object entry : subscribers) {
                    INeutronSecurityRuleAware subscriber = (INeutronSecurityRuleAware) entry;
                    subscriber.neutronSecurityRuleCreated(secutiryRule);
                }
            }
        }

    }

    private void updateSecurityRule(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes,
            Object[] subscribers) {
        for (Entry<InstanceIdentifier<?>, DataObject> updateSecurityRule : changes
                .getUpdatedData().entrySet()) {
            if (updateSecurityRule.getValue() instanceof SecurityRule) {
                NeutronSecurityRule securityRule = fromMd((SecurityRule) updateSecurityRule
                        .getValue());
                for (Object entry : subscribers) {
                    INeutronSecurityRuleAware subscriber = (INeutronSecurityRuleAware) entry;
                    subscriber.neutronSecurityRuleUpdated(securityRule);
                }
            }
        }
    }

    private void deleteSecurityRule(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes,
            Object[] subscribers) {
        for (InstanceIdentifier<?> deletedSecurityRule : changes
                .getRemovedPaths()) {
            if (deletedSecurityRule.getTargetType().equals(SecurityRule.class)) {
                NeutronSecurityRule securityRule = fromMd((SecurityRule) changes
                        .getOriginalData().get(deletedSecurityRule));
                for (Object entry : subscribers) {
                    INeutronSecurityRuleAware subscriber = (INeutronSecurityRuleAware) entry;
                    subscriber.neutronSecurityRuleDeleted(securityRule);
                }
            }
        }
    }

    private NeutronSecurityRule fromMd(SecurityRule rule) {
        NeutronSecurityRule answer = new NeutronSecurityRule();
        if (rule.getTenantId() != null) {
            answer.setSecurityRuleTenantID(rule.getTenantId().getValue()
                    .replace("-", ""));
        }
        if (rule.getDirection() != null) {
            answer.setSecurityRuleDirection(DIRECTION_MAP.get(rule
                    .getDirection()));
        }
        if (rule.getSecurityGroupId() != null) {
            answer.setSecurityRuleGroupID(rule.getSecurityGroupId().getValue());
        }
        if (rule.getRemoteGroupId() != null) {
            answer.setSecurityRemoteGroupID(rule.getRemoteGroupId().getValue());
        }
        if (rule.getRemoteIpPrefix() != null) {
            answer.setSecurityRuleRemoteIpPrefix(rule.getRemoteIpPrefix().getIpv4Prefix()!= null?
                    rule.getRemoteIpPrefix().getIpv4Prefix().getValue():rule.getRemoteIpPrefix().getIpv6Prefix().getValue());
        }
        if (rule.getProtocol() != null) {
            answer.setSecurityRuleProtocol(PROTOCOL_MAP.get(rule.getProtocol()));
        }
        if (rule.getEthertype() != null) {
            answer.setSecurityRuleEthertype(ETHERTYPE_MAP.get(rule
                    .getEthertype()));
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
    public void close() throws Exception {
        registration.close();
    }
}
