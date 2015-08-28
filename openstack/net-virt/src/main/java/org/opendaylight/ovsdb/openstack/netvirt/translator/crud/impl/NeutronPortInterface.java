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
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronPort;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronPort_AllowedAddressPairs;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronPort_ExtraDHCPOption;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronPort_VIFDetail;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronSecurityGroup;
import org.opendaylight.ovsdb.openstack.netvirt.translator.Neutron_IPs;
import org.opendaylight.ovsdb.openstack.netvirt.translator.crud.INeutronPortCRUD;
import org.opendaylight.ovsdb.openstack.netvirt.translator.crud.INeutronSecurityGroupCRUD;
import org.opendaylight.ovsdb.openstack.netvirt.translator.crud.NeutronCRUDInterfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.binding.rev141002.PortBindingExtension;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.binding.rev141002.PortBindingExtensionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.binding.rev141002.binding.attributes.VifDetails;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.binding.rev141002.binding.attributes.VifDetailsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev141002.port.attributes.AllowedAddressPairs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev141002.port.attributes.AllowedAddressPairsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev141002.port.attributes.ExtraDhcpOpts;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev141002.port.attributes.ExtraDhcpOptsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev141002.port.attributes.FixedIps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev141002.port.attributes.FixedIpsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev141002.ports.attributes.Ports;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev141002.ports.attributes.ports.Port;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev141002.ports.attributes.ports.PortBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150325.Neutron;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NeutronPortInterface extends AbstractNeutronInterface<Port, NeutronPort> implements INeutronPortCRUD {
    private static final Logger LOGGER = LoggerFactory.getLogger(NeutronPortInterface.class);

    NeutronPortInterface(ProviderContext providerContext) {
        super(providerContext);
    }

    // IfNBPortCRUD methods

    @Override
    public boolean portExists(String uuid) {
        Port port = readMd(createInstanceIdentifier(toMd(uuid)));
        return port != null;
    }

    @Override
    public NeutronPort getPort(String uuid) {
        Port port = readMd(createInstanceIdentifier(toMd(uuid)));
        if (port == null) {
            return null;
        }
        return fromMd(port);
    }

    @Override
    public List<NeutronPort> getAllPorts() {
        Set<NeutronPort> allPorts = new HashSet<NeutronPort>();
        Ports ports = readMd(createInstanceIdentifier());
        if (ports != null) {
            for (Port port : ports.getPort()) {
                allPorts.add(fromMd(port));
            }
        }
        LOGGER.debug("Exiting getAllPorts, Found {} OpenStackPorts", allPorts.size());
        List<NeutronPort> ans = new ArrayList<NeutronPort>();
        ans.addAll(allPorts);
        return ans;
    }

    @Override
    public boolean addPort(NeutronPort input) {
        if (portExists(input.getID())) {
            return false;
        }
        addMd(input);
        return true;
    }

    @Override
    public boolean removePort(String uuid) {
        if (!portExists(uuid)) {
            return false;
        }
        return removeMd(toMd(uuid));
    }

    @Override
    public boolean updatePort(String uuid, NeutronPort delta) {
        if (!portExists(uuid)) {
            return false;
        }
        updateMd(delta);
        return true;
    }

    // @deprecated, will be removed in Boron
    @Override
    public boolean macInUse(String macAddress) {
        return false;
    }

    // @deprecated, will be removed in Boron
    @Override
    public NeutronPort getGatewayPort(String subnetUUID) {
        return null;
    }

    @Override
    protected InstanceIdentifier<Port> createInstanceIdentifier(Port port) {
        return InstanceIdentifier.create(Neutron.class)
                .child(Ports.class)
                .child(Port.class, port.getKey());
    }

    protected InstanceIdentifier<Ports> createInstanceIdentifier() {
        return InstanceIdentifier.create(Neutron.class)
                .child(Ports.class);
    }

    protected void addExtensions(Port port, NeutronPort result) {
        PortBindingExtension binding = port.getAugmentation(PortBindingExtension.class);
        result.setBindinghostID(binding.getHostId());
        if (binding.getVifDetails() != null) {
            List<NeutronPort_VIFDetail> details = new ArrayList<NeutronPort_VIFDetail>();
            for (VifDetails vifDetail : binding.getVifDetails()) {
                NeutronPort_VIFDetail detail = new NeutronPort_VIFDetail();
                detail.setPortFilter(vifDetail.isPortFilter());
                detail.setOvsHybridPlug(vifDetail.isOvsHybridPlug());
                details.add(detail);
            }
            result.setVIFDetail(details);
        }
        result.setBindingvifType(binding.getVifType());
        result.setBindingvnicType(binding.getVnicType());
    }

    protected NeutronPort fromMd(Port port) {
        NeutronPort result = new NeutronPort();
        result.setAdminStateUp(port.isAdminStateUp());
        if (port.getAllowedAddressPairs() != null) {
            List<NeutronPort_AllowedAddressPairs> pairs = new ArrayList<NeutronPort_AllowedAddressPairs>();
            for (AllowedAddressPairs mdPair : port.getAllowedAddressPairs()) {
                NeutronPort_AllowedAddressPairs pair = new NeutronPort_AllowedAddressPairs();
                pair.setIpAddress(mdPair.getIpAddress());
                pair.setMacAddress(mdPair.getMacAddress());
                pair.setPortID(mdPair.getPortId());
                pairs.add(pair);
            }
            result.setAllowedAddressPairs(pairs);
        }
        result.setDeviceID(port.getDeviceId());
        result.setDeviceOwner(port.getDeviceOwner());
        if (port.getExtraDhcpOpts() != null) {
            List<NeutronPort_ExtraDHCPOption> options = new ArrayList<NeutronPort_ExtraDHCPOption>();
            for (ExtraDhcpOpts opt : port.getExtraDhcpOpts()) {
                NeutronPort_ExtraDHCPOption arg = new NeutronPort_ExtraDHCPOption();
                arg.setName(opt.getOptName());
                arg.setValue(opt.getOptValue());
                options.add(arg);
            }
            result.setExtraDHCPOptions(options);
        }
        if (port.getFixedIps() != null) {
            List<Neutron_IPs> ips = new ArrayList<Neutron_IPs>();
            for (FixedIps mdIP : port.getFixedIps()) {
                Neutron_IPs ip = new Neutron_IPs();
                ip.setIpAddress(String.valueOf(mdIP.getIpAddress().getValue()));
                ip.setSubnetUUID(mdIP.getSubnetId().getValue());
                ips.add(ip);
            }
            result.setFixedIPs(ips);
        }
        result.setMacAddress(port.getMacAddress());
        result.setName(port.getName());
        result.setNetworkUUID(String.valueOf(port.getNetworkId().getValue()));
        if (port.getSecurityGroups() != null) {
            Set<NeutronSecurityGroup> allGroups = new HashSet<NeutronSecurityGroup>();
            NeutronCRUDInterfaces interfaces = new NeutronCRUDInterfaces().fetchINeutronSecurityGroupCRUD(this);
            INeutronSecurityGroupCRUD sgIf = interfaces.getSecurityGroupInterface();
            for (Uuid sgUuid : port.getSecurityGroups()) {
                allGroups.add(sgIf.getNeutronSecurityGroup(sgUuid.getValue()));
            }
            List<NeutronSecurityGroup> groups = new ArrayList<NeutronSecurityGroup>();
            groups.addAll(allGroups);
            result.setSecurityGroups(groups);
        }
        result.setStatus(port.getStatus());
        result.setTenantID(String.valueOf(port.getTenantId().getValue()).replace("-",""));
        result.setPortUUID(String.valueOf(port.getUuid().getValue()));
        addExtensions(port, result);
        return result;
    }

    @Override
    protected Port toMd(NeutronPort neutronPort) {
        PortBindingExtensionBuilder bindingBuilder = new PortBindingExtensionBuilder();
        if (neutronPort.getBindinghostID() != null) {
            bindingBuilder.setHostId(neutronPort.getBindinghostID());
        }
        if (neutronPort.getVIFDetail() != null) {
            List<VifDetails> listVifDetail = new ArrayList<VifDetails>();
            for (NeutronPort_VIFDetail detail: neutronPort.getVIFDetail()) {
                VifDetailsBuilder vifDetailsBuilder = new VifDetailsBuilder();
                if (detail.getPortFilter() != null) {
                    vifDetailsBuilder.setPortFilter(detail.getPortFilter());
                }
                if (detail.getOvsHybridPlug() != null) {
                    vifDetailsBuilder.setOvsHybridPlug(detail.getOvsHybridPlug());
                }
                listVifDetail.add(vifDetailsBuilder.build());
            }
            bindingBuilder.setVifDetails(listVifDetail);
        }
        if (neutronPort.getBindingvifType() != null) {
            bindingBuilder.setVifType(neutronPort.getBindingvifType());
        }
        if (neutronPort.getBindingvnicType() != null) {
            bindingBuilder.setVnicType(neutronPort.getBindingvnicType());
        }

        PortBuilder portBuilder = new PortBuilder();
        portBuilder.addAugmentation(PortBindingExtension.class,
                                    bindingBuilder.build());
        portBuilder.setAdminStateUp(neutronPort.isAdminStateUp());
        if(neutronPort.getAllowedAddressPairs() != null) {
            List<AllowedAddressPairs> listAllowedAddressPairs = new ArrayList<AllowedAddressPairs>();
            for (NeutronPort_AllowedAddressPairs allowedAddressPairs : neutronPort.getAllowedAddressPairs()) {
                    AllowedAddressPairsBuilder allowedAddressPairsBuilder = new AllowedAddressPairsBuilder();
                    allowedAddressPairsBuilder.setIpAddress(allowedAddressPairs.getIpAddress());
                    allowedAddressPairsBuilder.setMacAddress(allowedAddressPairs.getMacAddress());
                    allowedAddressPairsBuilder.setPortId(allowedAddressPairs.getPortID());
                    listAllowedAddressPairs.add(allowedAddressPairsBuilder.build());
            }
            portBuilder.setAllowedAddressPairs(listAllowedAddressPairs);
        }
        if (neutronPort.getDeviceID() != null) {
            portBuilder.setDeviceId(neutronPort.getDeviceID());
        }
        if (neutronPort.getDeviceOwner() != null) {
        portBuilder.setDeviceOwner(neutronPort.getDeviceOwner());
        }
        if (neutronPort.getExtraDHCPOptions() != null) {
            List<ExtraDhcpOpts> listExtraDHCPOptions = new ArrayList<ExtraDhcpOpts>();
            for (NeutronPort_ExtraDHCPOption extraDHCPOption : neutronPort.getExtraDHCPOptions()) {
                ExtraDhcpOptsBuilder extraDHCPOptsBuilder = new ExtraDhcpOptsBuilder();
                extraDHCPOptsBuilder.setOptName(extraDHCPOption.getName());
                extraDHCPOptsBuilder.setOptValue(extraDHCPOption.getValue());
                listExtraDHCPOptions.add(extraDHCPOptsBuilder.build());
            }
            portBuilder.setExtraDhcpOpts(listExtraDHCPOptions);
        }
        if (neutronPort.getFixedIPs() != null) {
            List<FixedIps> listNeutronIPs = new ArrayList<FixedIps>();
            for (Neutron_IPs neutron_IPs : neutronPort.getFixedIPs()) {
                FixedIpsBuilder fixedIpsBuilder = new FixedIpsBuilder();
                fixedIpsBuilder.setIpAddress(new IpAddress(neutron_IPs.getIpAddress().toCharArray()));
                fixedIpsBuilder.setSubnetId(toUuid(neutron_IPs.getSubnetUUID()));
                listNeutronIPs.add(fixedIpsBuilder.build());
            }
            portBuilder.setFixedIps(listNeutronIPs);
        }
        if (neutronPort.getMacAddress() != null) {
            portBuilder.setMacAddress(neutronPort.getMacAddress());
        }
        if (neutronPort.getName() != null) {
        portBuilder.setName(neutronPort.getName());
        }
        if (neutronPort.getNetworkUUID() != null) {
        portBuilder.setNetworkId(toUuid(neutronPort.getNetworkUUID()));
        }
        if (neutronPort.getSecurityGroups() != null) {
            List<Uuid> listSecurityGroups = new ArrayList<Uuid>();
            for (NeutronSecurityGroup neutronSecurityGroup : neutronPort.getSecurityGroups()) {
                listSecurityGroups.add(toUuid(neutronSecurityGroup.getID()));
            }
            portBuilder.setSecurityGroups(listSecurityGroups);
        }
        if (neutronPort.getStatus() != null) {
            portBuilder.setStatus(neutronPort.getStatus());
        }
        if (neutronPort.getTenantID() != null) {
            portBuilder.setTenantId(toUuid(neutronPort.getTenantID()));
        }
        if (neutronPort.getPortUUID() != null) {
            portBuilder.setUuid(toUuid(neutronPort.getPortUUID()));
        } else {
            LOGGER.warn("Attempting to write neutron port without UUID");
        }
        return portBuilder.build();
    }

    @Override
    protected Port toMd(String uuid) {
        PortBuilder portBuilder = new PortBuilder();
        portBuilder.setUuid(toUuid(uuid));
        return portBuilder.build();
    }

    public static void registerNewInterface(BundleContext context,
                                            ProviderContext providerContext,
                                            List<ServiceRegistration<?>> registrations) {
        NeutronPortInterface neutronPortInterface = new NeutronPortInterface(providerContext);
        ServiceRegistration<INeutronPortCRUD> neutronPortInterfaceRegistration = context.registerService(INeutronPortCRUD.class, neutronPortInterface, null);
        if(neutronPortInterfaceRegistration != null) {
            registrations.add(neutronPortInterfaceRegistration);
        }
    }
}
