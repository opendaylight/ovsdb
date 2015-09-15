/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.openstack.netvirt.translator.iaware.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronSecurityGroup;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronSecurityRule;
import org.opendaylight.ovsdb.openstack.netvirt.translator.crud.INeutronSecurityRuleCRUD;
import org.opendaylight.ovsdb.openstack.netvirt.translator.crud.NeutronCRUDInterfaces;
import org.opendaylight.ovsdb.openstack.netvirt.translator.iaware.INeutronSecurityGroupAware;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150325.Neutron;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev141002.security.groups.attributes.SecurityGroups;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.secgroups.rev141002.security.groups.attributes.security.groups.SecurityGroup;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NeutronSecurityGroupDataChangeListener implements
        DataChangeListener, AutoCloseable {
    private static final Logger LOG = LoggerFactory
            .getLogger(NeutronSecurityGroupDataChangeListener.class);

    private ListenerRegistration<DataChangeListener> registration;
    private DataBroker db;

    public NeutronSecurityGroupDataChangeListener(DataBroker db) {
        this.db = db;
        InstanceIdentifier<SecurityGroup> path = InstanceIdentifier
                .create(Neutron.class).child(SecurityGroups.class)
                .child(SecurityGroup.class);
        LOG.debug("Register listener for Neutron Secutiry group model data changes");
        registration = this.db.registerDataChangeListener(
                LogicalDatastoreType.CONFIGURATION, path, this,
                DataChangeScope.ONE);

    }

    @Override
    public void onDataChanged(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        LOG.trace("Data changes : {}", changes);

        Object[] subscribers = NeutronIAwareUtil.getInstances(
                INeutronSecurityGroupAware.class, this);
        createSecurityGroup(changes, subscribers);
        updateSecurityGroup(changes, subscribers);
        deleteSecurityGroup(changes, subscribers);
    }

    private void createSecurityGroup(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes,
            Object[] subscribers) {
        for (Entry<InstanceIdentifier<?>, DataObject> newSecutiryGroup : changes
                .getCreatedData().entrySet()) {
            if (newSecutiryGroup.getValue() instanceof SecurityGroup) {
                NeutronSecurityGroup secutiryGroup = fromMd((SecurityGroup) newSecutiryGroup
                        .getValue());
                for (Object entry : subscribers) {
                    INeutronSecurityGroupAware subscriber = (INeutronSecurityGroupAware) entry;
                    subscriber.neutronSecurityGroupCreated(secutiryGroup);
                }
            }
        }

    }

    private void updateSecurityGroup(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes,
            Object[] subscribers) {
        for (Entry<InstanceIdentifier<?>, DataObject> updateSecurityGroup : changes
                .getUpdatedData().entrySet()) {
            if (updateSecurityGroup.getValue() instanceof SecurityGroup) {
                NeutronSecurityGroup securityGroup = fromMd((SecurityGroup) updateSecurityGroup
                        .getValue());
                for (Object entry : subscribers) {
                    INeutronSecurityGroupAware subscriber = (INeutronSecurityGroupAware) entry;
                    subscriber.neutronSecurityGroupUpdated(securityGroup);
                }
            }
        }
    }

    private void deleteSecurityGroup(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes,
            Object[] subscribers) {
        for (InstanceIdentifier<?> deletedSecurityGroup : changes
                .getRemovedPaths()) {
            if (deletedSecurityGroup.getTargetType()
                    .equals(SecurityGroup.class)) {
                NeutronSecurityGroup securityGroup = fromMd((SecurityGroup) changes
                        .getOriginalData().get(deletedSecurityGroup));
                for (Object entry : subscribers) {
                    INeutronSecurityGroupAware subscriber = (INeutronSecurityGroupAware) entry;
                    subscriber.neutronSecurityGroupDeleted(securityGroup);
                }
            }
        }
    }

    private NeutronSecurityGroup fromMd(SecurityGroup group) {
        NeutronSecurityGroup answer = new NeutronSecurityGroup();
        if (group.getName() != null) {
            answer.setSecurityGroupName(group.getName());
        }
        if (group.getDescription() != null) {
            answer.setSecurityGroupDescription(group.getDescription());
        }
        if (group.getTenantId() != null) {
            answer.setSecurityGroupTenantID(group.getTenantId().getValue()
                    .replace("-", ""));
        }
        if (group.getSecurityRules() != null) {
            NeutronCRUDInterfaces interfaces = new NeutronCRUDInterfaces()
                    .fetchINeutronSecurityRuleCRUD(this);
            INeutronSecurityRuleCRUD srCrud = interfaces
                    .getSecurityRuleInterface();

            List<NeutronSecurityRule> rules = new ArrayList<NeutronSecurityRule>();
            for (Uuid uuid : group.getSecurityRules()) {
                rules.add(srCrud.getNeutronSecurityRule(uuid.getValue()));
            }
            answer.setSecurityRules(rules);
        }
        if (group.getUuid() != null) {
            answer.setID(group.getUuid().getValue());
        }
        return answer;
    }

    @Override
    public void close() throws Exception {
        registration.close();
    }
}
