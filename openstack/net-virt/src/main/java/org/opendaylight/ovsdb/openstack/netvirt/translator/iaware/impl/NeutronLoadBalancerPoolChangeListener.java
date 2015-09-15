/*
 * Copyright (C) 2015 Red Hat, Inc. and others.  All rights reserved.
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
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronLoadBalancerPool;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronLoadBalancerPoolMember;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronLoadBalancer_SessionPersistence;
import org.opendaylight.ovsdb.openstack.netvirt.translator.Neutron_ID;
import org.opendaylight.ovsdb.openstack.netvirt.translator.iaware.INeutronLoadBalancerPoolAware;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev160807.ProtocolBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev160807.ProtocolHttp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev160807.ProtocolHttps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev160807.ProtocolIcmp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev160807.ProtocolTcp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.lbaasv2.rev141002.lbaas.attributes.Pools;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.lbaasv2.rev141002.lbaas.attributes.pools.Pool;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.lbaasv2.rev141002.lbaas.attributes.pools.pool.members.Member;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150325.Neutron;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableBiMap;

public class NeutronLoadBalancerPoolChangeListener implements DataChangeListener, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(NeutronLoadBalancerPoolChangeListener.class);

    private static final ImmutableBiMap<Class<? extends ProtocolBase>,String> PROTOCOL_MAP
            = new ImmutableBiMap.Builder<Class<? extends ProtocolBase>,String>()
            .put(ProtocolHttp.class, "HTTP")
            .put(ProtocolHttps.class, "HTTPS")
            .put(ProtocolIcmp.class, "ICMP")
            .put(ProtocolTcp.class,"TCP")
            .build();

    private ListenerRegistration<DataChangeListener> registration;
    private DataBroker db;

    public NeutronLoadBalancerPoolChangeListener(DataBroker db){
        this.db = db;
        InstanceIdentifier<Pool> path = InstanceIdentifier
                .create(Neutron.class)
                .child(Pools.class)
                .child(Pool.class);
        LOG.debug("Register listener for Neutron Load Balancer Pool model data changes");
        registration =
                this.db.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION, path, this, AsyncDataBroker.DataChangeScope.ONE);
    }

    @Override
    public void close() throws Exception {
        registration.close();
    }

    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        LOG.trace("Data changes : {}", changes);

        Object[] subscribers = NeutronIAwareUtil.getInstances(INeutronLoadBalancerPoolAware.class, this);
        createPool(changes, subscribers);
        updatePool(changes, subscribers);
        deletePool(changes, subscribers);
    }

    private void createPool(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes,
            Object[] subscribers) {
        for (Entry<InstanceIdentifier<?>, DataObject> newPool : changes.getCreatedData().entrySet()) {
        	if(newPool.getValue() instanceof Pool){
                NeutronLoadBalancerPool loadBalancerPool = fromMd((Pool) newPool.getValue());
                for (Object entry : subscribers) {
                    INeutronLoadBalancerPoolAware subscriber = (INeutronLoadBalancerPoolAware) entry;
                    subscriber.neutronLoadBalancerPoolCreated(loadBalancerPool);
                }
        	}
        }
    }
    private void updatePool(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes,
            Object[] subscribers) {
        for (Entry<InstanceIdentifier<?>, DataObject> updatePool : changes.getUpdatedData().entrySet()) {
        	if(updatePool.getValue() instanceof Pool){
                NeutronLoadBalancerPool loadBalancerPool = fromMd((Pool)updatePool.getValue());
                for(Object entry: subscribers){
                    INeutronLoadBalancerPoolAware subscriber = (INeutronLoadBalancerPoolAware) entry;
                    subscriber.neutronLoadBalancerPoolUpdated(loadBalancerPool);
                }
        	}
        }
    }
    private void deletePool(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes,
            Object[] subscribers) {
        for (InstanceIdentifier<?> deletedPoolPath : changes.getRemovedPaths()) {
        	if(deletedPoolPath.getTargetType().equals(Pool.class)){
                NeutronLoadBalancerPool loadBalancerPool = fromMd((Pool)changes.getOriginalData().get(deletedPoolPath));
                for(Object entry: subscribers){
                    INeutronLoadBalancerPoolAware subscriber = (INeutronLoadBalancerPoolAware) entry;
                    subscriber.neutronLoadBalancerPoolDeleted(loadBalancerPool);
                }
        	}
        }
    }

    /*
     * This method is borrowed from NeutronLoadBalancerPool.java class of Neutron Northbound class.
     * in the original location, this method is called extractFields.
     * We will be utilizing similar code from other classes from the same package of neutron project.
     */
    private NeutronLoadBalancerPool fromMd(Pool pool) {
        NeutronLoadBalancerPool result = new NeutronLoadBalancerPool();

        result.setID(pool.getUuid().getValue());
        result.setLoadBalancerPoolTenantID(pool.getTenantId().getValue());
        result.setLoadBalancerPoolName(pool.getName());
        result.setLoadBalancerPoolDescription(pool.getDescr());
        result.setLoadBalancerPoolProtocol(PROTOCOL_MAP.get(pool.getProtocol()));
        result.setLoadBalancerPoolLbAlgorithm(pool.getLbAlgorithm());

        // TODO: setNeutronLoadBalancerPoolHealthMonitorID is a list? Fill in, when its needed.
        if (pool.getHealthmonitorId() != null) {
        	result.setNeutronLoadBalancerPoolHealthMonitorID(pool.getHealthmonitorId().getValue());
        }

        result.setLoadBalancerPoolAdminStateIsUp(pool.isAdminStateUp());

        List<Neutron_ID> listeners = new ArrayList();
        if (pool.getListeners() != null) {
            for (Uuid listenerUuid : pool.getListeners()) {
                listeners.add(new Neutron_ID(listenerUuid.getValue()));
            }
        }
        result.setLoadBalancerPoolListeners(listeners);

        if (pool.getSessionPersistence() != null) {
            NeutronLoadBalancer_SessionPersistence sessionPersistence = new NeutronLoadBalancer_SessionPersistence();
            sessionPersistence.setCookieName(pool.getSessionPersistence().getCookieName());
            sessionPersistence.setType(pool.getSessionPersistence().getType());
            result.setLoadBalancerSessionPersistence(sessionPersistence);
        }

        List<NeutronLoadBalancerPoolMember> loadBalancerPoolMembers = new ArrayList();
        if (pool.getMembers() != null) {
            for (Member member : pool.getMembers().getMember()) {
                NeutronLoadBalancerPoolMember neutronMember = new NeutronLoadBalancerPoolMember();

                neutronMember.setPoolID(pool.getUuid().getValue());
                neutronMember.setPoolMemberID(member.getUuid().getValue());

                // TODO: locate and populate remainder attributes, when its needed
                // member.setPoolMemberAddress(xxx);
                // member.setPoolMemberAdminStateIsUp(xxx);
                // member.setPoolMemberProtoPort(xxx);
                // member.setPoolMemberSubnetID(xxx);
                // member.setPoolMemberTenantID(xxx);
                // member.setPoolMemberWeight(xxx);

                loadBalancerPoolMembers.add(neutronMember);
            }
        }
        result.setLoadBalancerPoolMembers(loadBalancerPoolMembers);

        return result;
    }
}
