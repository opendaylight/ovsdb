/*
 * Copyright (C) 2015 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.translator.iaware.impl;

import com.google.common.collect.ImmutableBiMap;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.openstack.netvirt.translator.*;
import org.opendaylight.ovsdb.openstack.netvirt.translator.iaware.INeutronLoadBalancerPoolAware;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev160807.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.lbaasv2.rev141002.lbaas.attributes.Pool;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.lbaasv2.rev141002.lbaas.attributes.pool.Pools;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150325.Neutron;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

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
        InstanceIdentifier<Pools> path = InstanceIdentifier
                .create(Neutron.class)
                .child(Pool.class)
                .child(Pools.class);
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
            LOG.trace("Data changes : {}",changes);

        LOG.trace("Data changes : {}",changes);

        Object[] subscribers = NeutronIAwareUtil.getInstances(INeutronLoadBalancerPoolAware.class, this);
        createPool(changes, subscribers);
        updatePool(changes, subscribers);
        deletePool(changes, subscribers);
    }

    private void createPool(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes,
            Object[] subscribers) {
        for (Entry<InstanceIdentifier<?>, DataObject> newPool : changes.getCreatedData().entrySet()) {
            NeutronLoadBalancerPool loadBalancerPool = fromMd((Pools) newPool.getValue());
            for (Object entry : subscribers) {
                INeutronLoadBalancerPoolAware subscriber = (INeutronLoadBalancerPoolAware) entry;
                subscriber.neutronLoadBalancerPoolCreated(loadBalancerPool);
            }
        }
    }
    private void updatePool(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes,
            Object[] subscribers) {
        for (Entry<InstanceIdentifier<?>, DataObject> updatePool : changes.getUpdatedData().entrySet()) {
            NeutronLoadBalancerPool loadBalancerPool = fromMd((Pools)updatePool.getValue());
            for(Object entry: subscribers){
                INeutronLoadBalancerPoolAware subscriber = (INeutronLoadBalancerPoolAware)entry;
                subscriber.neutronLoadBalancerPoolUpdated(loadBalancerPool);
            }
        }

    }
    private void deletePool(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes,
            Object[] subscribers) {
        for (InstanceIdentifier<?> deletedPoolPath : changes.getRemovedPaths()) {
            NeutronLoadBalancerPool loadBalancerPool = fromMd((Pools)changes.getOriginalData().get(deletedPoolPath));
            for(Object entry: subscribers){
                INeutronLoadBalancerPoolAware subscriber = (INeutronLoadBalancerPoolAware)entry;
                subscriber.neutronLoadBalancerPoolDeleted(loadBalancerPool);
            }
        }
    }

    /*
     * This method is borrowed from NeutronLoadBalancerPool.java class of Neutron Northbound class.
     * in the original location, this method is called extractFields.
     * We will be utilizing similar code from other classes from the same package of neutron project.
     */
    private NeutronLoadBalancerPool fromMd(Pools pool) {
        NeutronLoadBalancerPool result = new NeutronLoadBalancerPool();

        result.setID(pool.getUuid().getValue());
        result.setLoadBalancerPoolTenantID(pool.getTenantId().getValue());
        result.setLoadBalancerPoolName(pool.getName());
        result.setLoadBalancerPoolDescription(pool.getDescr());
        result.setLoadBalancerPoolProtocol(PROTOCOL_MAP.get(pool.getProtocol()));
        result.setLoadBalancerPoolLbAlgorithm(pool.getLbAlgorithm());

        // TODO: setNeutronLoadBalancerPoolHealthMonitorID is a list? Fill in, when its needed.
        if (pool.getHealthmonitorIds() != null) {
            for (Uuid monitorId : pool.getHealthmonitorIds()) {
                result.setNeutronLoadBalancerPoolHealthMonitorID(monitorId.getValue());
                break;
            }
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
            for (Uuid memberUuid : pool.getMembers()) {
                NeutronLoadBalancerPoolMember member = new NeutronLoadBalancerPoolMember();

                member.setPoolID(pool.getUuid().getValue());
                member.setPoolMemberID(memberUuid.getValue());

                // TODO: locate and populate remainder attributes, when its needed
                // member.setPoolMemberAddress(xxx);
                // member.setPoolMemberAdminStateIsUp(xxx);
                // member.setPoolMemberProtoPort(xxx);
                // member.setPoolMemberSubnetID(xxx);
                // member.setPoolMemberTenantID(xxx);
                // member.setPoolMemberWeight(xxx);

                loadBalancerPoolMembers.add(member);
            }
        }
        result.setLoadBalancerPoolMembers(loadBalancerPoolMembers);

        return result;
    }
}
