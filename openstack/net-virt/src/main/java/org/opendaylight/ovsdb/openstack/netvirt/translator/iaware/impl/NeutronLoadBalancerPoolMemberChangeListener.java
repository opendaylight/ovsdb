/*
 * Copyright (C) 2015 Red Hat, Inc. and others.  All rights reserved.
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
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronLoadBalancerPoolMember;
import org.opendaylight.ovsdb.openstack.netvirt.translator.iaware.INeutronLoadBalancerPoolMemberAware;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.lbaasv2.rev150712.lbaas.attributes.Pools;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.lbaasv2.rev150712.lbaas.attributes.pools.Pool;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.lbaasv2.rev150712.lbaas.attributes.pools.PoolKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.lbaasv2.rev150712.lbaas.attributes.pools.pool.Members;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.lbaasv2.rev150712.lbaas.attributes.pools.pool.members.Member;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150712.Neutron;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NeutronLoadBalancerPoolMemberChangeListener implements ClusteredDataChangeListener, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(NeutronLoadBalancerPoolMemberChangeListener.class);

    private ListenerRegistration<DataChangeListener> registration;
    private DataBroker db;

    public NeutronLoadBalancerPoolMemberChangeListener(DataBroker db){
        this.db = db;
        InstanceIdentifier<Member> path = InstanceIdentifier
                .create(Neutron.class)
                .child(Pools.class)
                .child(Pool.class)
                .child(Members.class)
                .child(Member.class);
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
        	if(newPoolMember.getValue() instanceof Member){
                NeutronLoadBalancerPoolMember neutronLBPoolMember = fromMd(newPoolMember.getKey(), (Member) newPoolMember.getValue());
                for (Object entry : subscribers) {
                    INeutronLoadBalancerPoolMemberAware subscriber = (INeutronLoadBalancerPoolMemberAware) entry;
                    subscriber.neutronLoadBalancerPoolMemberCreated(neutronLBPoolMember);
                }
        	}
        }
    }
    private void updatePoolMember(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes,
            Object[] subscribers) {
        for (Entry<InstanceIdentifier<?>, DataObject> updatePoolMember : changes.getUpdatedData().entrySet()) {
        	if(updatePoolMember.getValue() instanceof Member){
                NeutronLoadBalancerPoolMember neutronLBPoolMember =
                        fromMd(updatePoolMember.getKey(), (Member) updatePoolMember.getValue());
                for(Object entry: subscribers){
                    INeutronLoadBalancerPoolMemberAware subscriber = (INeutronLoadBalancerPoolMemberAware) entry;
                    subscriber.neutronLoadBalancerPoolMemberUpdated(neutronLBPoolMember);
                }
        	}
        }
    }
    private void deletePoolMember(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes,
            Object[] subscribers) {
        for (InstanceIdentifier<?> deletedPoolMemberPath : changes.getRemovedPaths()) {
        	if(deletedPoolMemberPath.getTargetType().equals(Member.class)){
                NeutronLoadBalancerPoolMember neutronLBPoolMember =
                        fromMd(deletedPoolMemberPath, (Member) changes.getOriginalData().get(deletedPoolMemberPath));
                for(Object entry: subscribers){
                    INeutronLoadBalancerPoolMemberAware subscriber = (INeutronLoadBalancerPoolMemberAware) entry;
                    subscriber.neutronLoadBalancerPoolMemberDeleted(neutronLBPoolMember);
                }
        	}
        }
    }

    /*
     * This method is borrowed from NeutronLoadBalancerPoolMember.java class of Neutron Northbound class.
     * in the original location, this method is called extractFields.
     * We will be utilizing similar code from other classes from the same package of neutron project.
     */
    private NeutronLoadBalancerPoolMember fromMd(InstanceIdentifier<?> iid, Member member) {
        NeutronLoadBalancerPoolMember result = new NeutronLoadBalancerPoolMember();

        final PoolKey poolsKey = iid.firstKeyOf(Pool.class, PoolKey.class);
        if (poolsKey != null) {
            result.setPoolID(poolsKey.getUuid().getValue());
        }

        result.setID(member.getUuid().getValue());
        result.setPoolMemberAdminStateIsUp(member.isAdminStateUp());

        final IpAddress memberIpAddress = member.getAddress();
        if (memberIpAddress != null) {
            if (memberIpAddress.getIpv4Address() != null) {
                result.setPoolMemberAddress(memberIpAddress.getIpv4Address().getValue());
            } else if (memberIpAddress.getIpv6Address() != null) {
                result.setPoolMemberAddress(memberIpAddress.getIpv6Address().getValue());
            }
        }

        result.setPoolMemberProtoPort(member.getProtocolPort());
        result.setPoolMemberSubnetID(member.getSubnetId().getValue());
        result.setPoolMemberTenantID(member.getTenantId().getValue());
        result.setPoolMemberWeight(member.getWeight());

        return result;
    }
}

