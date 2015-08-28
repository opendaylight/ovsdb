/*
 * Copyright (c) 2013, 2015 IBM Corporation and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.translator.crud.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronNetwork;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronPort;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronSubnet;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronSubnetIPAllocationPool;
import org.opendaylight.ovsdb.openstack.netvirt.translator.Neutron_IPs;
import org.opendaylight.ovsdb.openstack.netvirt.translator.crud.INeutronNetworkCRUD;
import org.opendaylight.ovsdb.openstack.netvirt.translator.crud.INeutronPortCRUD;
import org.opendaylight.ovsdb.openstack.netvirt.translator.crud.INeutronSubnetCRUD;
import org.opendaylight.ovsdb.openstack.netvirt.translator.crud.NeutronCRUDInterfaces;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev141002.subnet.attributes.AllocationPoolsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev141002.subnets.attributes.Subnets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev141002.subnets.attributes.subnets.Subnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev141002.subnets.attributes.subnets.SubnetBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableBiMap;

public class NeutronSubnetInterface extends AbstractNeutronInterface<Subnet, NeutronSubnet> implements INeutronSubnetCRUD {
    private static final Logger LOGGER = LoggerFactory.getLogger(NeutronSubnetInterface.class);

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

    NeutronSubnetInterface(ProviderContext providerContext) {
        super(providerContext);
    }

    // IfNBSubnetCRUD methods

    @Override
    public boolean subnetExists(String uuid) {
        Subnet subnet = readMd(createInstanceIdentifier(toMd(uuid)));
        if (subnet == null) {
            return false;
        }
        return true;
    }

    @Override
    public NeutronSubnet getSubnet(String uuid) {
        Subnet subnet = readMd(createInstanceIdentifier(toMd(uuid)));
        if (subnet == null) {
            return null;
        }
        return fromMd(subnet);
    }

    @Override
    public List<NeutronSubnet> getAllSubnets() {
        Set<NeutronSubnet> allSubnets = new HashSet<NeutronSubnet>();
        Subnets subnets = readMd(createInstanceIdentifier());
        if (subnets != null) {
            for (Subnet subnet: subnets.getSubnet()) {
                allSubnets.add(fromMd(subnet));
            }
        }
        LOGGER.debug("Exiting getAllSubnets, Found {} OpenStackSubnets", allSubnets.size());
        List<NeutronSubnet> ans = new ArrayList<NeutronSubnet>();
        ans.addAll(allSubnets);
        return ans;
    }

    @Override
    public boolean addSubnet(NeutronSubnet input) {
        String id = input.getID();
        if (subnetExists(id)) {
            return false;
        }
        addMd(input);
        NeutronCRUDInterfaces interfaces = new NeutronCRUDInterfaces()
            .fetchINeutronNetworkCRUD(this);
        INeutronNetworkCRUD networkIf = interfaces.getNetworkInterface();

        NeutronNetwork targetNet = networkIf.getNetwork(input.getNetworkUUID());
        targetNet.addSubnet(id);
        return true;
    }

    @Override
    public boolean removeSubnet(String uuid) {
        NeutronSubnet target = getSubnet(uuid);
        if (target == null) {
            return false;
        }
        removeMd(toMd(uuid));
        NeutronCRUDInterfaces interfaces = new NeutronCRUDInterfaces()
            .fetchINeutronNetworkCRUD(this);
        INeutronNetworkCRUD networkIf = interfaces.getNetworkInterface();

        NeutronNetwork targetNet = networkIf.getNetwork(target.getNetworkUUID());
        targetNet.removeSubnet(uuid);
        return true;
    }

    @Override
    public boolean updateSubnet(String uuid, NeutronSubnet delta) {
        if (!subnetExists(uuid)) {
            return false;
        }
/* note: because what we get is *not* a delta but (at this point) the updated
 * object, this is much simpler - just replace the value and update the mdsal
 * with it */
        updateMd(delta);
        return true;
    }

// note: this is being set to false in preparation for deprecation and removal
    @Override
    public boolean subnetInUse(String subnetUUID) {
        return false;
    }

    protected NeutronSubnet fromMd(Subnet subnet) {
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
// @deprecated and will be removed in Boron
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

    protected Subnet toMd(NeutronSubnet subnet) {
        SubnetBuilder subnetBuilder = new SubnetBuilder();
        if (subnet.getName() != null) {
            subnetBuilder.setName(subnet.getName());
        }
        if (subnet.getTenantID() != null) {
            subnetBuilder.setTenantId(toUuid(subnet.getTenantID()));
        }
        if (subnet.getNetworkUUID() != null) {
            subnetBuilder.setNetworkId(toUuid(subnet.getNetworkUUID()));
        }
        if (subnet.getIpVersion() != null) {
            ImmutableBiMap<Integer, Class<? extends IpVersionBase>> mapper =
                    IPV_MAP.inverse();
            subnetBuilder.setIpVersion((Class<? extends IpVersionBase>) mapper.get(subnet
                    .getIpVersion()));
        }
        if (subnet.getCidr() != null) {
            subnetBuilder.setCidr(subnet.getCidr());
        }
        if (subnet.getGatewayIP() != null) {
            IpAddress ipAddress = new IpAddress(subnet.getGatewayIP()
                    .toCharArray());
            subnetBuilder.setGatewayIp(ipAddress);
        }
        if (subnet.getIpV6RaMode() != null) {
            ImmutableBiMap<String, Class<? extends Dhcpv6Base>> mapper =
                    DHCPV6_MAP.inverse();
            subnetBuilder.setIpv6RaMode((Class<? extends Dhcpv6Base>) mapper.get(subnet.getIpV6RaMode()));
        }
        if (subnet.getIpV6AddressMode() != null) {
            ImmutableBiMap<String, Class<? extends Dhcpv6Base>> mapper =
                    DHCPV6_MAP.inverse();
            subnetBuilder.setIpv6AddressMode((Class<? extends Dhcpv6Base>) mapper.get(subnet.getIpV6AddressMode()));
        }
        subnetBuilder.setEnableDhcp(subnet.getEnableDHCP());
        if (subnet.getAllocationPools() != null) {
            List<AllocationPools> allocationPools = new ArrayList<AllocationPools>();
            for (NeutronSubnetIPAllocationPool allocationPool : subnet
                    .getAllocationPools()) {
                AllocationPoolsBuilder builder = new AllocationPoolsBuilder();
                builder.setStart(allocationPool.getPoolStart());
                builder.setEnd(allocationPool.getPoolEnd());
                AllocationPools temp = builder.build();
                allocationPools.add(temp);
            }
            subnetBuilder.setAllocationPools(allocationPools);
        }
        if (subnet.getDnsNameservers() != null) {
            List<IpAddress> dnsNameServers = new ArrayList<IpAddress>();
            for (String dnsNameServer : subnet.getDnsNameservers()) {
                IpAddress ipAddress = new IpAddress(dnsNameServer.toCharArray());
                dnsNameServers.add(ipAddress);
            }
            subnetBuilder.setDnsNameservers(dnsNameServers);
        }
        if (subnet.getID() != null) {
            subnetBuilder.setUuid(toUuid(subnet.getID()));
        } else {
            LOGGER.warn("Attempting to write neutron subnet without UUID");
        }
        return subnetBuilder.build();
    }

    @Override
    protected InstanceIdentifier<Subnet> createInstanceIdentifier(Subnet subnet) {
        return InstanceIdentifier.create(Neutron.class).child(Subnets.class)
                .child(Subnet.class, subnet.getKey());
    }

    protected InstanceIdentifier<Subnets> createInstanceIdentifier() {
        return InstanceIdentifier.create(Neutron.class)
                .child(Subnets.class);
    }

    @Override
    protected Subnet toMd(String uuid) {
        SubnetBuilder subnetBuilder = new SubnetBuilder();
        subnetBuilder.setUuid(toUuid(uuid));
        return subnetBuilder.build();
    }

    public static void registerNewInterface(BundleContext context,
                                            ProviderContext providerContext,
                                            List<ServiceRegistration<?>> registrations) {
        NeutronSubnetInterface neutronSubnetInterface = new NeutronSubnetInterface(providerContext);
        ServiceRegistration<INeutronSubnetCRUD> neutronSubnetInterfaceRegistration = context.registerService(INeutronSubnetCRUD.class, neutronSubnetInterface, null);
        if(neutronSubnetInterfaceRegistration != null) {
            registrations.add(neutronSubnetInterfaceRegistration);
        }
    }
}
