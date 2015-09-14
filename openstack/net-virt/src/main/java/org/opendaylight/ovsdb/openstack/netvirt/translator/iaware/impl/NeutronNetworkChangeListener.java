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
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronNetwork;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronNetwork_Segment;
import org.opendaylight.ovsdb.openstack.netvirt.translator.iaware.INeutronNetworkAware;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.l3.ext.rev141002.NetworkL3Extension;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.networks.rev141002.NetworkTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.networks.rev141002.NetworkTypeFlat;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.networks.rev141002.NetworkTypeGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.networks.rev141002.NetworkTypeVlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.networks.rev141002.NetworkTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.networks.rev141002.networks.attributes.Networks;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.networks.rev141002.networks.attributes.networks.Network;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.provider.ext.rev141002.NetworkProviderExtension;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.provider.ext.rev141002.neutron.networks.network.Segments;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150325.Neutron;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableBiMap;

public class NeutronNetworkChangeListener implements DataChangeListener, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(NeutronNetworkChangeListener.class);

    private static final ImmutableBiMap<Class<? extends NetworkTypeBase>,String> NETWORK_MAP
    = new ImmutableBiMap.Builder<Class<? extends NetworkTypeBase>,String>()
    .put(NetworkTypeFlat.class,"flat")
    .put(NetworkTypeGre.class,"gre")
    .put(NetworkTypeVlan.class,"vlan")
    .put(NetworkTypeVxlan.class,"vxlan")
    .build();

    private ListenerRegistration<DataChangeListener> registration;
    private DataBroker db;

    public NeutronNetworkChangeListener(DataBroker db){
        this.db = db;
        InstanceIdentifier<Network> path = InstanceIdentifier
                .create(Neutron.class)
                .child(Networks.class)
                .child(Network.class);
        LOG.debug("Register listener for Neutron Network model data changes");
        registration =
                this.db.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION, path, this, DataChangeScope.ONE);

    }

    @Override
    public void onDataChanged(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        LOG.trace("Data changes : {}",changes);

        Object[] subscribers = NeutronIAwareUtil.getInstances(INeutronNetworkAware.class, this);
        createNetwork(changes, subscribers);
        updateNetwork(changes, subscribers);
        deleteNetwork(changes, subscribers);
    }

    private void createNetwork(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes,
            Object[] subscribers) {
        for (Entry<InstanceIdentifier<?>, DataObject> newNetwork : changes.getCreatedData().entrySet()) {
        	if(newNetwork instanceof Network){
                NeutronNetwork network = fromMd((Network)newNetwork.getValue());
                for(Object entry: subscribers){
                    INeutronNetworkAware subscriber = (INeutronNetworkAware)entry;
                    subscriber.neutronNetworkCreated(network);
                }
        	}
        }
    }

    private void updateNetwork(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes,
            Object[] subscribers) {
        for (Entry<InstanceIdentifier<?>, DataObject> updateNetwork : changes.getUpdatedData().entrySet()) {
        	if(updateNetwork instanceof Network){
                NeutronNetwork network = fromMd((Network)updateNetwork.getValue());
                for(Object entry: subscribers){
                    INeutronNetworkAware subscriber = (INeutronNetworkAware)entry;
                    subscriber.neutronNetworkUpdated(network);
                }
        	}
        }
    }

    private void deleteNetwork(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes,
            Object[] subscribers) {
        for (InstanceIdentifier<?> deletedNetworkPath : changes.getRemovedPaths()) {
        	if(deletedNetworkPath.getTargetType().equals(Network.class)){
                NeutronNetwork network = fromMd((Network)changes.getOriginalData().get(deletedNetworkPath));
                for(Object entry: subscribers){
                    INeutronNetworkAware subscriber = (INeutronNetworkAware)entry;
                    subscriber.neutronNetworkDeleted(network);
                }
        	}
        }
    }

    /*
     * This method is borrowed from NeutronNetworkInterface.java class of Neutron Northbound class.
     * We will be utilizing similar code from other classes from the same package of neutron project.
     */
    private NeutronNetwork fromMd(Network network) {
        NeutronNetwork result = new NeutronNetwork();
        result.setAdminStateUp(network.isAdminStateUp());
        result.setNetworkName(network.getName());
        result.setShared(network.isShared());
        result.setStatus(network.getStatus());
        if (network.getSubnets() != null) {
            List<String> neutronSubnets = new ArrayList<String>();
            for( Uuid subnet : network.getSubnets()) {
               neutronSubnets.add(subnet.getValue());
            }
            result.setSubnets(neutronSubnets);
        }

        // todo remove '-' chars as tenant id doesn't use them
        result.setTenantID(network.getTenantId().getValue());
        result.setID(network.getUuid().getValue());

        NetworkL3Extension l3Extension = network.getAugmentation(NetworkL3Extension.class);
        result.setRouterExternal(l3Extension.isExternal());

        NetworkProviderExtension providerExtension = network.getAugmentation(NetworkProviderExtension.class);
        result.setProviderPhysicalNetwork(providerExtension.getPhysicalNetwork());
        result.setProviderSegmentationID(providerExtension.getSegmentationId());
        result.setProviderNetworkType(NETWORK_MAP.get(providerExtension.getNetworkType()));
        List<NeutronNetwork_Segment> segments = new ArrayList<NeutronNetwork_Segment>();
        if (providerExtension.getSegments() != null) {
            for (Segments segment: providerExtension.getSegments()) {
                NeutronNetwork_Segment neutronSegment = new NeutronNetwork_Segment();
                neutronSegment.setProviderPhysicalNetwork(segment.getPhysicalNetwork());
                neutronSegment.setProviderSegmentationID(segment.getSegmentationId());
                neutronSegment.setProviderNetworkType(NETWORK_MAP.get(segment.getNetworkType()));
                segments.add(neutronSegment);
            }
        }
        result.setSegments(segments);
        return result;
    }

    @Override
    public void close() throws Exception {
        registration.close();
    }
}
