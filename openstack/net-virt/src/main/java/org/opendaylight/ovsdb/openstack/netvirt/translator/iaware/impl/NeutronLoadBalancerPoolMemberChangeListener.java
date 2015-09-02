/*
 * Copyright (C) 2015 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.translator.iaware.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronLoadBalancerPoolMember;
import org.opendaylight.ovsdb.openstack.netvirt.translator.iaware.INeutronLoadBalancerPoolMemberAware;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.lbaasv2.rev141002.lbaas.attributes.Pool;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.lbaasv2.rev141002.lbaas.attributes.pool.Pools;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.lbaasv2.rev141002.lbaas.attributes.pool.PoolsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.lbaasv2.rev141002.lbaas.attributes.pool.pools.Member;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.lbaasv2.rev141002.lbaas.attributes.pool.pools.member.Members;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150325.Neutron;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

public class NeutronLoadBalancerPoolMemberChangeListener implements DataChangeListener, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(NeutronLoadBalancerPoolMemberChangeListener.class);

    private ListenerRegistration<DataChangeListener> registration;
    private DataBroker db;

    public NeutronLoadBalancerPoolMemberChangeListener(DataBroker db){
        this.db = db;
        InstanceIdentifier<Members> path = InstanceIdentifier
                .create(Neutron.class)
                .child(Pool.class)
                .child(Pools.class)
                .child(Member.class)
                .child(Members.class);
        LOG.debug("Register listener for Neutron Load Balancer Pool Member model data changes");
        registration =
                this.db.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION, path, this,
                        AsyncDataBroker.DataChangeScope.ONE);
    }

    @Override
    public void close() throws Exception {
        registration.close();
    }

    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        LOG.trace("Data changes : {}", changes);

        Object[] subscribers = NeutronIAwareUtil.getInstances(INeutronLoadBalancerPoolMemberAware.class, this);
        createPoolMember(changes, subscribers);
        updatePoolMember(changes, subscribers);
        deletePoolMember(changes, subscribers);
    }

    private void createPoolMember(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes,
            Object[] subscribers) {
        for (Entry<InstanceIdentifier<?>, DataObject> newPoolMember : changes.getCreatedData().entrySet()) {
            NeutronLoadBalancerPoolMember neutronLBPoolMember = fromMd(newPoolMember.getKey(), (Members) newPoolMember.getValue());
            for (Object entry : subscribers) {
                INeutronLoadBalancerPoolMemberAware subscriber = (INeutronLoadBalancerPoolMemberAware) entry;
                subscriber.neutronLoadBalancerPoolMemberCreated(neutronLBPoolMember);
            }
        }
    }
    private void updatePoolMember(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes,
            Object[] subscribers) {
        for (Entry<InstanceIdentifier<?>, DataObject> updatePoolMember : changes.getUpdatedData().entrySet()) {
            NeutronLoadBalancerPoolMember neutronLBPoolMember =
                    fromMd(updatePoolMember.getKey(), (Members) updatePoolMember.getValue());
            for(Object entry: subscribers){
                INeutronLoadBalancerPoolMemberAware subscriber = (INeutronLoadBalancerPoolMemberAware) entry;
                subscriber.neutronLoadBalancerPoolMemberUpdated(neutronLBPoolMember);
            }
        }
    }
    private void deletePoolMember(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes,
            Object[] subscribers) {
        for (InstanceIdentifier<?> deletedPoolMemberPath : changes.getRemovedPaths()) {
            NeutronLoadBalancerPoolMember neutronLBPoolMember =
                    fromMd(deletedPoolMemberPath, (Members) changes.getOriginalData().get(deletedPoolMemberPath));
            for(Object entry: subscribers){
                INeutronLoadBalancerPoolMemberAware subscriber = (INeutronLoadBalancerPoolMemberAware) entry;
                subscriber.neutronLoadBalancerPoolMemberDeleted(neutronLBPoolMember);
            }
        }
    }

    /*
     * This method is borrowed from NeutronLoadBalancerPoolMember.java class of Neutron Northbound class.
     * in the original location, this method is called extractFields.
     * We will be utilizing similar code from other classes from the same package of neutron project.
     */
    private NeutronLoadBalancerPoolMember fromMd(InstanceIdentifier<?> iid, Members members) {
        NeutronLoadBalancerPoolMember result = new NeutronLoadBalancerPoolMember();

        final PoolsKey poolsKey = iid.firstKeyOf(Pools.class, PoolsKey.class);
        if (poolsKey != null) {
            result.setPoolID(poolsKey.getUuid().getValue());
        }

        result.setID(members.getUuid().getValue());
        result.setPoolMemberAdminStateIsUp(members.isAdminStateUp());

        final IpAddress memberIpAddress = members.getAddress();
        if (memberIpAddress != null) {
            if (memberIpAddress.getIpv4Address() != null) {
                result.setPoolMemberAddress(memberIpAddress.getIpv4Address().getValue());
            } else if (memberIpAddress.getIpv6Address() != null) {
                result.setPoolMemberAddress(memberIpAddress.getIpv6Address().getValue());
            }
        }

        result.setPoolMemberProtoPort(members.getProtocolPort());
        result.setPoolMemberSubnetID(members.getSubnetId().getValue());
        result.setPoolMemberTenantID(members.getTenantId().getValue());
        result.setPoolMemberWeight(members.getWeight());

        return result;
    }
}

