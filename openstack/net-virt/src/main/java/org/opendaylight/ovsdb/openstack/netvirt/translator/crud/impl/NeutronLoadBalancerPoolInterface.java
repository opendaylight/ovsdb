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
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronLoadBalancerPool;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronLoadBalancer_SessionPersistence;
import org.opendaylight.ovsdb.openstack.netvirt.translator.Neutron_ID;
import org.opendaylight.ovsdb.openstack.netvirt.translator.crud.INeutronLoadBalancerPoolCRUD;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev160807.ProtocolBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev160807.ProtocolHttp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev160807.ProtocolHttps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev160807.ProtocolTcp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.lbaasv2.rev141002.lbaas.attributes.Pools;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.lbaasv2.rev141002.lbaas.attributes.pools.Pool;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.lbaasv2.rev141002.lbaas.attributes.pools.PoolBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.lbaasv2.rev141002.pool.attributes.SessionPersistenceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150325.Neutron;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableBiMap;

/**
 * TODO: Migrate this to consume the MD-SAL data store, so that it can read all the data from data store.
 * No need to worry about the write/update related methods here. OVSDB net-virt will use these CRUD Interface
 * only for reading. We will cleanup these interface/methods later.
 */

public class NeutronLoadBalancerPoolInterface extends AbstractNeutronInterface<Pool, NeutronLoadBalancerPool> implements INeutronLoadBalancerPoolCRUD {
    private static final Logger LOGGER = LoggerFactory.getLogger(NeutronLoadBalancerPoolInterface.class);
    private ConcurrentMap<String, NeutronLoadBalancerPool> loadBalancerPoolDB = new ConcurrentHashMap<String, NeutronLoadBalancerPool>();

    private static final ImmutableBiMap<Class<? extends ProtocolBase>,String> PROTOCOL_MAP
            = new ImmutableBiMap.Builder<Class<? extends ProtocolBase>,String>()
            .put(ProtocolHttp.class,"HTTP")
            .put(ProtocolHttps.class,"HTTPS")
            .put(ProtocolTcp.class,"TCP")
            .build();

    NeutronLoadBalancerPoolInterface(ProviderContext providerContext) {
        super(providerContext);
    }

    @Override
    public boolean neutronLoadBalancerPoolExists(String uuid) {
        return loadBalancerPoolDB.containsKey(uuid);
    }

    @Override
    public NeutronLoadBalancerPool getNeutronLoadBalancerPool(String uuid) {
        if (!neutronLoadBalancerPoolExists(uuid)) {
            LOGGER.debug("No LoadBalancerPool has Been Defined");
            return null;
        }
        return loadBalancerPoolDB.get(uuid);
    }

    @Override
    public List<NeutronLoadBalancerPool> getAllNeutronLoadBalancerPools() {
        Set<NeutronLoadBalancerPool> allLoadBalancerPools = new HashSet<NeutronLoadBalancerPool>();
        for (Entry<String, NeutronLoadBalancerPool> entry : loadBalancerPoolDB.entrySet()) {
            NeutronLoadBalancerPool loadBalancerPool = entry.getValue();
            allLoadBalancerPools.add(loadBalancerPool);
        }
        LOGGER.debug("Exiting getLoadBalancerPools, Found {} OpenStackLoadBalancerPool", allLoadBalancerPools.size());
        List<NeutronLoadBalancerPool> ans = new ArrayList<NeutronLoadBalancerPool>();
        ans.addAll(allLoadBalancerPools);
        return ans;
    }

    @Override
    public boolean addNeutronLoadBalancerPool(NeutronLoadBalancerPool input) {
        if (neutronLoadBalancerPoolExists(input.getID())) {
            return false;
        }
        loadBalancerPoolDB.putIfAbsent(input.getID(), input);
        //TODO: add code to find INeutronLoadBalancerPoolAware services and call newtorkCreated on them
        return true;
    }

    @Override
    public boolean removeNeutronLoadBalancerPool(String uuid) {
        if (!neutronLoadBalancerPoolExists(uuid)) {
            return false;
        }
        loadBalancerPoolDB.remove(uuid);
        //TODO: add code to find INeutronLoadBalancerPoolAware services and call newtorkDeleted on them
        return true;
    }

    @Override
    public boolean updateNeutronLoadBalancerPool(String uuid, NeutronLoadBalancerPool delta) {
        if (!neutronLoadBalancerPoolExists(uuid)) {
            return false;
        }
        NeutronLoadBalancerPool target = loadBalancerPoolDB.get(uuid);
        return overwrite(target, delta);
    }

    @Override
    public boolean neutronLoadBalancerPoolInUse(String loadBalancerPoolUUID) {
        return !neutronLoadBalancerPoolExists(loadBalancerPoolUUID);
    }

    @Override
    protected Pool toMd(String uuid) {
        PoolBuilder poolsBuilder = new PoolBuilder();
        poolsBuilder.setUuid(toUuid(uuid));
        return poolsBuilder.build();
    }

    @Override
    protected InstanceIdentifier<Pool> createInstanceIdentifier(Pool pools) {
        return InstanceIdentifier.create(Neutron.class)
                .child(Pools.class)
                .child(Pool.class, pools.getKey());
    }

    @Override
    protected Pool toMd(NeutronLoadBalancerPool pool) {
        PoolBuilder poolBuilder = new PoolBuilder();
        poolBuilder.setAdminStateUp(pool.getLoadBalancerPoolAdminIsStateIsUp());
        if (pool.getLoadBalancerPoolDescription() != null) {
            poolBuilder.setDescr(pool.getLoadBalancerPoolDescription());
        }
        if (pool.getNeutronLoadBalancerPoolHealthMonitorID() != null) {
            poolBuilder.setHealthmonitorId(toUuid(pool.getNeutronLoadBalancerPoolHealthMonitorID()));
        }
        if (pool.getLoadBalancerPoolLbAlgorithm() != null) {
            poolBuilder.setLbAlgorithm(pool.getLoadBalancerPoolLbAlgorithm());
        }
        if (pool.getLoadBalancerPoolListeners() != null) {
            List<Uuid> listListener = new ArrayList<Uuid>();
            for (Neutron_ID neutron_id : pool.getLoadBalancerPoolListeners()) {
                listListener.add(toUuid(neutron_id.getID()));
            }
            poolBuilder.setListeners(listListener);
        }
        // because members are another container, we don't want to copy
        // it over, so just skip it here
        if (pool.getLoadBalancerPoolName() != null) {
            poolBuilder.setName(pool.getLoadBalancerPoolName());
        }
        if (pool.getLoadBalancerPoolProtocol() != null) {
            ImmutableBiMap<String, Class<? extends ProtocolBase>> mapper =
                PROTOCOL_MAP.inverse();
            poolBuilder.setProtocol((Class<? extends ProtocolBase>) mapper.get(pool.getLoadBalancerPoolProtocol()));
        }
        if (pool.getLoadBalancerPoolSessionPersistence() != null) {
            NeutronLoadBalancer_SessionPersistence sessionPersistence = pool.getLoadBalancerPoolSessionPersistence();
            SessionPersistenceBuilder sessionPersistenceBuilder = new SessionPersistenceBuilder();
            sessionPersistenceBuilder.setCookieName(sessionPersistence.getCookieName());
            sessionPersistenceBuilder.setType(sessionPersistence.getType());
            poolBuilder.setSessionPersistence(sessionPersistenceBuilder.build());
        }
        if (pool.getLoadBalancerPoolTenantID() != null) {
            poolBuilder.setTenantId(toUuid(pool.getLoadBalancerPoolTenantID()));
        }
        if (pool.getID() != null) {
            poolBuilder.setUuid(toUuid(pool.getID()));
        } else {
            LOGGER.warn("Attempting to write neutron load balancer pool without UUID");
        }
        return poolBuilder.build();
    }

    public static void registerNewInterface(BundleContext context,
                                            ProviderContext providerContext,
                                            List<ServiceRegistration<?>> registrations) {
        NeutronLoadBalancerPoolInterface neutronLoadBalancerPoolInterface = new NeutronLoadBalancerPoolInterface(providerContext);
        ServiceRegistration<INeutronLoadBalancerPoolCRUD> neutronLoadBalancerPoolInterfaceRegistration = context.registerService(INeutronLoadBalancerPoolCRUD.class, neutronLoadBalancerPoolInterface, null);
        if(neutronLoadBalancerPoolInterfaceRegistration != null) {
            registrations.add(neutronLoadBalancerPoolInterfaceRegistration);
        }
    }
}
