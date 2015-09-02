/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.openstack.netvirt.translator.iaware.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronPort;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronSubnet;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronSubnetIPAllocationPool;
import org.opendaylight.ovsdb.openstack.netvirt.translator.Neutron_IPs;
import org.opendaylight.ovsdb.openstack.netvirt.translator.crud.INeutronPortCRUD;
import org.opendaylight.ovsdb.openstack.netvirt.translator.crud.NeutronCRUDInterfaces;
import org.opendaylight.ovsdb.openstack.netvirt.translator.iaware.INeutronSubnetAware;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev160807.Dhcpv6Base;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev160807.Dhcpv6Off;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev160807.Dhcpv6Slaac;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev160807.Dhcpv6Stateful;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev160807.Dhcpv6Stateless;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev160807.IpVersionBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev160807.IpVersionV4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev160807.IpVersionV6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150325.Neutron;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev141002.subnet.attributes.AllocationPools;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev141002.subnets.attributes.Subnets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev141002.subnets.attributes.subnets.Subnet;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableBiMap;

public class NeutronSubnetChangeListener implements DataChangeListener, AutoCloseable{
    private static final Logger LOG = LoggerFactory.getLogger(NeutronSubnetChangeListener.class);

    private static final ImmutableBiMap<Class<? extends IpVersionBase>,Integer> IPV_MAP
    = new ImmutableBiMap.Builder<Class<? extends IpVersionBase>,Integer>()
    .put(IpVersionV4.class,Integer.valueOf(4))
    .put(IpVersionV6.class,Integer.valueOf(6))
    .build();

    private static final ImmutableBiMap<Class<? extends Dhcpv6Base>,String> DHCPV6_MAP
    = new ImmutableBiMap.Builder<Class<? extends Dhcpv6Base>,String>()
    .put(Dhcpv6Off.class,"off")
    .put(Dhcpv6Stateful.class,"dhcpv6-stateful")
    .put(Dhcpv6Slaac.class,"slaac")
    .put(Dhcpv6Stateless.class,"dhcpv6-stateless")
    .build();

    private ListenerRegistration<DataChangeListener> registration;
    private DataBroker db;

    public NeutronSubnetChangeListener(DataBroker db){
        this.db = db;
        InstanceIdentifier<Subnet> path = InstanceIdentifier
                .create(Neutron.class)
                .child(Subnets.class)
                .child(Subnet.class);
        LOG.debug("Register listener for Neutron Subnet model data changes");
        registration =
                this.db.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION, path, this, DataChangeScope.ONE);

    }

    @Override
    public void onDataChanged(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        LOG.trace("Data changes : {}",changes);

        Object[] subscribers = NeutronIAwareUtil.getInstances(INeutronSubnetAware.class, this);
        createSubnet(changes, subscribers);
        updateSubnet(changes, subscribers);
        deleteSubnet(changes, subscribers);
    }

    private void createSubnet(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes,
            Object[] subscribers) {
        for (Entry<InstanceIdentifier<?>, DataObject> newSubnet : changes.getCreatedData().entrySet()) {
            NeutronSubnet subnet = fromMd((Subnet)newSubnet.getValue());
            for(Object entry: subscribers){
                INeutronSubnetAware subscriber = (INeutronSubnetAware)entry;
                subscriber.neutronSubnetCreated(subnet);
            }
        }

    }

    private void updateSubnet(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes,
            Object[] subscribers) {
        for (Entry<InstanceIdentifier<?>, DataObject> updateSubnet : changes.getUpdatedData().entrySet()) {
            NeutronSubnet subnet = fromMd((Subnet)updateSubnet.getValue());
            for(Object entry: subscribers){
                INeutronSubnetAware subscriber = (INeutronSubnetAware)entry;
                subscriber.neutronSubnetUpdated(subnet);
            }
        }
    }

    private void deleteSubnet(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes,
            Object[] subscribers) {
        for (InstanceIdentifier<?> deletedSubnetPath : changes.getRemovedPaths()) {
            NeutronSubnet subnet = fromMd((Subnet)changes.getOriginalData().get(deletedSubnetPath));
            for(Object entry: subscribers){
                INeutronSubnetAware subscriber = (INeutronSubnetAware)entry;
                subscriber.neutronSubnetDeleted(subnet);
            }
        }
    }

    /*
     * This method is borrowed from NeutronSubnetInterface.java class of Neutron Northbound class.
     * We will be utilizing similar code from other classes from the same package of neutron project.
     */
    private NeutronSubnet fromMd(Subnet subnet) {
        NeutronSubnet result = new NeutronSubnet();
        result.setName(subnet.getName());
        result.setTenantID(String.valueOf(subnet.getTenantId().getValue()).replace("-",""));
        result.setNetworkUUID(subnet.getNetworkId().getValue());
        result.setIpVersion(IPV_MAP.get(subnet.getIpVersion()));
        result.setCidr(subnet.getCidr());
        result.setGatewayIP(String.valueOf(subnet.getGatewayIp().getValue()));
        result.setIpV6RaMode(DHCPV6_MAP.get(subnet.getIpv6RaMode()));
        result.setIpV6AddressMode(DHCPV6_MAP.get(subnet.getIpv6AddressMode()));
        result.setEnableDHCP(subnet.isEnableDhcp());
        if (subnet.getAllocationPools() != null) {
            List<NeutronSubnetIPAllocationPool> allocationPools = new ArrayList<NeutronSubnetIPAllocationPool>();
            for (AllocationPools allocationPool : subnet.getAllocationPools()) {
                NeutronSubnetIPAllocationPool pool = new NeutronSubnetIPAllocationPool();
                pool.setPoolStart(allocationPool.getStart());
                pool.setPoolEnd(allocationPool.getEnd());
                allocationPools.add(pool);
            }
            result.setAllocationPools(allocationPools);
        }
        if (subnet.getDnsNameservers() != null) {
            List<String> dnsNameServers = new ArrayList<String>();
            for (IpAddress dnsNameServer : subnet.getDnsNameservers()) {
                dnsNameServers.add(String.valueOf(dnsNameServer.getValue()));
            }
            result.setDnsNameservers(dnsNameServers);
        }
        result.setID(subnet.getUuid().getValue());

        // read through the ports and put the ones in this subnet into the internal
        // myPorts object.
       Set<NeutronPort> allPorts = new HashSet<NeutronPort>();
        NeutronCRUDInterfaces interfaces = new NeutronCRUDInterfaces()
            .fetchINeutronPortCRUD(this);
        INeutronPortCRUD portIf = interfaces.getPortInterface();
        for (NeutronPort port : portIf.getAllPorts()) {
            if (port.getFixedIPs() != null) {
                for (Neutron_IPs ip : port.getFixedIPs()) {
                    if (ip.getSubnetUUID().equals(result.getID())) {
                        allPorts.add(port);
                    }
                }
            }
        }
        List<NeutronPort> ports = new ArrayList<NeutronPort>();
        ports.addAll(allPorts);
        result.setPorts(ports);
        return result;
    }

    @Override
    public void close() throws Exception {
        registration.close();
    }

}
