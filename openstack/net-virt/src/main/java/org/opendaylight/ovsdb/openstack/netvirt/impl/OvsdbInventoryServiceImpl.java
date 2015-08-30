/*
 * Copyright (c) 2015 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.openstack.netvirt.impl;

import com.google.common.collect.Sets;

import java.util.Set;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.ovsdb.openstack.netvirt.ConfigInterface;
import org.opendaylight.ovsdb.openstack.netvirt.api.Constants;
import org.opendaylight.ovsdb.openstack.netvirt.api.OvsdbInventoryService;
import org.opendaylight.ovsdb.openstack.netvirt.api.OvsdbInventoryListener;
import org.opendaylight.ovsdb.openstack.netvirt.translator.iaware.impl.NeutronFloatingIPChangeListener;
import org.opendaylight.ovsdb.openstack.netvirt.translator.iaware.impl.NeutronNetworkChangeListener;
import org.opendaylight.ovsdb.openstack.netvirt.translator.iaware.impl.NeutronPortChangeListener;
import org.opendaylight.ovsdb.openstack.netvirt.translator.iaware.impl.NeutronRouterChangeListener;
import org.opendaylight.ovsdb.openstack.netvirt.translator.iaware.impl.NeutronSubnetChangeListener;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OvsdbInventoryServiceImpl is the implementation for {@link OvsdbInventoryService}
 *
 * @author Sam Hague (shague@redhat.com)
 */
public class OvsdbInventoryServiceImpl implements ConfigInterface, OvsdbInventoryService {
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbInventoryServiceImpl.class);
    private static DataBroker dataBroker = null;
    private static Set<OvsdbInventoryListener> ovsdbInventoryListeners = Sets.newCopyOnWriteArraySet();
    private OvsdbDataChangeListener ovsdbDataChangeListener = null;
    private static MdsalUtils mdsalUtils = null;

    public OvsdbInventoryServiceImpl(ProviderContext providerContext) {
        dataBroker = providerContext.getSALService(DataBroker.class);
        LOG.info("OvsdbInventoryServiceImpl initialized");
        ovsdbDataChangeListener = new OvsdbDataChangeListener(dataBroker);
        mdsalUtils = new MdsalUtils(dataBroker);
    }

    @Override
    public void listenerAdded(OvsdbInventoryListener listener) {
        ovsdbInventoryListeners.add(listener);
        LOG.info("listenerAdded: {}", listener);
    }

    @Override
    public void listenerRemoved(OvsdbInventoryListener listener) {
        ovsdbInventoryListeners.remove(listener);
        LOG.info("listenerRemoved: {}", listener);
    }

    @Override
    public void providersReady() {
        ovsdbDataChangeListener.start();
        initializeNeutronModelsDataChangeListeners(dataBroker);
        initializeNetvirtTopology();
    }

    public static Set<OvsdbInventoryListener> getOvsdbInventoryListeners() {
        return ovsdbInventoryListeners;
    }

    @Override
    public void setDependencies(BundleContext bundleContext, ServiceReference serviceReference) {}

    @Override
    public void setDependencies(Object impl) {}

    private void initializeNetvirtTopology() {
        final TopologyId topologyId = new TopologyId(new Uri(Constants.NETVIRT_TOPOLOGY_ID));
        InstanceIdentifier<Topology> path =
                InstanceIdentifier.create(NetworkTopology.class).child(Topology.class, new TopologyKey(topologyId));
        TopologyBuilder tpb = new TopologyBuilder();
        tpb.setTopologyId(topologyId);
        if (! mdsalUtils.put(LogicalDatastoreType.OPERATIONAL, path, tpb.build())) {
            LOG.error("Error initializing netvirt topology");
        }
    }

    private void initializeNeutronModelsDataChangeListeners(
            DataBroker db) {
        new NeutronNetworkChangeListener(db);
        new NeutronSubnetChangeListener(db);
        new NeutronPortChangeListener(db);
        new NeutronRouterChangeListener(db);
        new NeutronFloatingIPChangeListener(db);
    }

}
