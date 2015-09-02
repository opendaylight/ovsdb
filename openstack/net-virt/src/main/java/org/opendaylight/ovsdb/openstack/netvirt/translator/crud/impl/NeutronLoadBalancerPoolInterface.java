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
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronLoadBalancerPoolMember;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronLoadBalancer_SessionPersistence;
import org.opendaylight.ovsdb.openstack.netvirt.translator.Neutron_ID;
import org.opendaylight.ovsdb.openstack.netvirt.translator.crud.INeutronLoadBalancerPoolCRUD;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev160807.ProtocolBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev160807.ProtocolHttp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev160807.ProtocolHttps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev160807.ProtocolTcp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.lbaasv2.rev141002.lbaas.attributes.Pool;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.lbaasv2.rev141002.lbaas.attributes.pool.Pools;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.lbaasv2.rev141002.lbaas.attributes.pool.PoolsBuilder;
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

public class NeutronLoadBalancerPoolInterface extends AbstractNeutronInterface<Pools, NeutronLoadBalancerPool> implements INeutronLoadBalancerPoolCRUD {
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
    protected Pools toMd(String uuid) {
        PoolsBuilder poolsBuilder = new PoolsBuilder();
        poolsBuilder.setUuid(toUuid(uuid));
        return poolsBuilder.build();
    }

    @Override
    protected InstanceIdentifier<Pools> createInstanceIdentifier(Pools pools) {
        return InstanceIdentifier.create(Neutron.class)
                .child(Pool.class)
                .child(Pools.class, pools.getKey());
    }

    @Override
    protected Pools toMd(NeutronLoadBalancerPool pools) {
        PoolsBuilder poolsBuilder = new PoolsBuilder();
        poolsBuilder.setAdminStateUp(pools.getLoadBalancerPoolAdminIsStateIsUp());
        if (pools.getLoadBalancerPoolDescription() != null) {
            poolsBuilder.setDescr(pools.getLoadBalancerPoolDescription());
        }
        if (pools.getNeutronLoadBalancerPoolHealthMonitorID() != null) {
            List<Uuid> listHealthMonitor = new ArrayList<Uuid>();
            listHealthMonitor.add(toUuid(pools.getNeutronLoadBalancerPoolHealthMonitorID()));
            poolsBuilder.setHealthmonitorIds(listHealthMonitor);
        }
        if (pools.getLoadBalancerPoolLbAlgorithm() != null) {
            poolsBuilder.setLbAlgorithm(pools.getLoadBalancerPoolLbAlgorithm());
        }
        if (pools.getLoadBalancerPoolListeners() != null) {
            List<Uuid> listListener = new ArrayList<Uuid>();
            for (Neutron_ID neutron_id : pools.getLoadBalancerPoolListeners()) {
                listListener.add(toUuid(neutron_id.getID()));
            }
            poolsBuilder.setListeners(listListener);
        }
        if (pools.getLoadBalancerPoolMembers() != null) {
            List<Uuid> listMember = new ArrayList<Uuid>();
            for (NeutronLoadBalancerPoolMember loadBalancerPoolMember : pools.getLoadBalancerPoolMembers()) {
                listMember.add(toUuid(loadBalancerPoolMember.getID()));
            }
            poolsBuilder.setMembers(listMember);
        }
        if (pools.getLoadBalancerPoolName() != null) {
            poolsBuilder.setName(pools.getLoadBalancerPoolName());
        }
        if (pools.getLoadBalancerPoolProtocol() != null) {
            ImmutableBiMap<String, Class<? extends ProtocolBase>> mapper =
                PROTOCOL_MAP.inverse();
            poolsBuilder.setProtocol((Class<? extends ProtocolBase>) mapper.get(pools.getLoadBalancerPoolProtocol()));
        }
        if (pools.getLoadBalancerPoolSessionPersistence() != null) {
            NeutronLoadBalancer_SessionPersistence sessionPersistence = pools.getLoadBalancerPoolSessionPersistence();
            SessionPersistenceBuilder sessionPersistenceBuilder = new SessionPersistenceBuilder();
            sessionPersistenceBuilder.setCookieName(sessionPersistence.getCookieName());
            sessionPersistenceBuilder.setType(sessionPersistence.getType());
            poolsBuilder.setSessionPersistence(sessionPersistenceBuilder.build());
        }
        if (pools.getLoadBalancerPoolTenantID() != null) {
            poolsBuilder.setTenantId(toUuid(pools.getLoadBalancerPoolTenantID()));
        }
        if (pools.getID() != null) {
            poolsBuilder.setUuid(toUuid(pools.getID()));
        } else {
            LOGGER.warn("Attempting to write neutron load balancer pool without UUID");
        }
        return poolsBuilder.build();
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
