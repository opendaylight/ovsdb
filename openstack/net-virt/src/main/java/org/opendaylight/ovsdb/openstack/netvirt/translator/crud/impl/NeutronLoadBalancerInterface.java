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
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronLoadBalancer;
import org.opendaylight.ovsdb.openstack.netvirt.translator.crud.INeutronLoadBalancerCRUD;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.lbaasv2.rev141002.lbaas.attributes.Loadbalancers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.lbaasv2.rev141002.lbaas.attributes.loadbalancers.Loadbalancer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.lbaasv2.rev141002.lbaas.attributes.loadbalancers.LoadbalancerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150325.Neutron;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: Migrate this to consume the MD-SAL data store, so that it can read all the data from data store.
 * No need to worry about the write/update related methods here. OVSDB net-virt will use these CRUD Interface
 * only for reading. We will cleanup these interface/methods later.
 */
public class NeutronLoadBalancerInterface extends AbstractNeutronInterface<Loadbalancer, NeutronLoadBalancer> implements INeutronLoadBalancerCRUD {
    private static final Logger LOGGER = LoggerFactory.getLogger(NeutronLoadBalancerInterface.class);
    private ConcurrentMap<String, NeutronLoadBalancer> loadBalancerDB  = new ConcurrentHashMap<String, NeutronLoadBalancer>();


    NeutronLoadBalancerInterface(ProviderContext providerContext) {
        super(providerContext);
    }

    @Override
    public boolean neutronLoadBalancerExists(String uuid) {
        return loadBalancerDB.containsKey(uuid);
    }

    @Override
    public NeutronLoadBalancer getNeutronLoadBalancer(String uuid) {
        if (!neutronLoadBalancerExists(uuid)) {
            LOGGER.debug("No LoadBalancer Have Been Defined");
            return null;
        }
        return loadBalancerDB.get(uuid);
    }

    @Override
    public List<NeutronLoadBalancer> getAllNeutronLoadBalancers() {
        Set<NeutronLoadBalancer> allLoadBalancers = new HashSet<NeutronLoadBalancer>();
        for (Entry<String, NeutronLoadBalancer> entry : loadBalancerDB.entrySet()) {
            NeutronLoadBalancer loadBalancer = entry.getValue();
            allLoadBalancers.add(loadBalancer);
        }
        LOGGER.debug("Exiting getLoadBalancers, Found {} OpenStackLoadBalancer", allLoadBalancers.size());
        List<NeutronLoadBalancer> ans = new ArrayList<NeutronLoadBalancer>();
        ans.addAll(allLoadBalancers);
        return ans;
    }

    @Override
    public boolean addNeutronLoadBalancer(NeutronLoadBalancer input) {
        if (neutronLoadBalancerExists(input.getID())) {
            return false;
        }
        loadBalancerDB.putIfAbsent(input.getID(), input);
        //TODO: add code to find INeutronLoadBalancerAware services and call newtorkCreated on them
        return true;
    }

    @Override
    public boolean removeNeutronLoadBalancer(String uuid) {
        if (!neutronLoadBalancerExists(uuid)) {
            return false;
        }
        loadBalancerDB.remove(uuid);
        //TODO: add code to find INeutronLoadBalancerAware services and call newtorkDeleted on them
        return true;
    }

    @Override
    public boolean updateNeutronLoadBalancer(String uuid, NeutronLoadBalancer delta) {
        if (!neutronLoadBalancerExists(uuid)) {
            return false;
        }
        NeutronLoadBalancer target = loadBalancerDB.get(uuid);
        return overwrite(target, delta);
    }

    @Override
    public boolean neutronLoadBalancerInUse(String loadBalancerUUID) {
        return !neutronLoadBalancerExists(loadBalancerUUID);
    }

    @Override
    protected Loadbalancer toMd(String uuid) {
        LoadbalancerBuilder loadBalancersBuilder = new LoadbalancerBuilder();
        loadBalancersBuilder.setUuid(toUuid(uuid));
        return loadBalancersBuilder.build();
    }

    @Override
    protected InstanceIdentifier<Loadbalancer> createInstanceIdentifier(
            Loadbalancer loadBalancer) {
        return InstanceIdentifier.create(Neutron.class)
                .child(Loadbalancers.class)
                .child(Loadbalancer.class, loadBalancer.getKey());
    }

    @Override
    protected Loadbalancer toMd(NeutronLoadBalancer loadBalancer) {
        LoadbalancerBuilder loadBalancersBuilder = new LoadbalancerBuilder();
        loadBalancersBuilder.setAdminStateUp(loadBalancer.getLoadBalancerAdminStateUp());
        if (loadBalancer.getLoadBalancerDescription() != null) {
            loadBalancersBuilder.setDescr(loadBalancer.getLoadBalancerDescription());
        }
        if (loadBalancer.getLoadBalancerName() != null) {
            loadBalancersBuilder.setName(loadBalancer.getLoadBalancerName());
        }
        if (loadBalancer.getLoadBalancerStatus() != null) {
            loadBalancersBuilder.setStatus(loadBalancer.getLoadBalancerStatus());
        }
        if (loadBalancer.getLoadBalancerTenantID() != null) {
            loadBalancersBuilder.setTenantId(toUuid(loadBalancer.getLoadBalancerTenantID()));
        }
        if (loadBalancer.getLoadBalancerVipAddress() != null) {
            loadBalancersBuilder.setVipAddress(new IpAddress(loadBalancer.getLoadBalancerVipAddress().toCharArray()));
        }
        if (loadBalancer.getLoadBalancerVipSubnetID() != null) {
            loadBalancersBuilder.setVipSubnetId(toUuid(loadBalancer.getLoadBalancerVipSubnetID()));
        }
        if (loadBalancer.getID() != null) {
            loadBalancersBuilder.setUuid(toUuid(loadBalancer.getID()));
        } else {
            LOGGER.warn("Attempting to write neutron load balancer without UUID");
        }
        return loadBalancersBuilder.build();
    }

    public static void registerNewInterface(BundleContext context,
                                            ProviderContext providerContext,
                                            List<ServiceRegistration<?>> registrations) {
        NeutronLoadBalancerInterface neutronLoadBalancerInterface = new NeutronLoadBalancerInterface(providerContext);
        ServiceRegistration<INeutronLoadBalancerCRUD> neutronLoadBalancerInterfaceRegistration = context.registerService(INeutronLoadBalancerCRUD.class, neutronLoadBalancerInterface, null);
        if(neutronLoadBalancerInterfaceRegistration != null) {
            registrations.add(neutronLoadBalancerInterfaceRegistration);
        }
    }
}
