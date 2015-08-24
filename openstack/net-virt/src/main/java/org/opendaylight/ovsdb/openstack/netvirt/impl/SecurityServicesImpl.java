/*
 * Copyright (c) 2014, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.impl;

import java.util.List;

import org.opendaylight.neutron.spi.INeutronPortCRUD;
import org.opendaylight.neutron.spi.INeutronSubnetCRUD;
import org.opendaylight.neutron.spi.NeutronPort;
import org.opendaylight.neutron.spi.NeutronSecurityGroup;
import org.opendaylight.neutron.spi.NeutronSubnet;
import org.opendaylight.neutron.spi.Neutron_IPs;
import org.opendaylight.ovsdb.openstack.netvirt.ConfigInterface;
import org.opendaylight.ovsdb.openstack.netvirt.api.Constants;
import org.opendaylight.ovsdb.openstack.netvirt.api.SecurityServicesManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.Southbound;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.*;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecurityServicesImpl implements ConfigInterface, SecurityServicesManager {
    private static final Logger LOG = LoggerFactory.getLogger(TenantNetworkManagerImpl.class);
    private volatile INeutronPortCRUD neutronPortCache;
    private volatile INeutronSubnetCRUD neutronSubnetCache;
    private volatile Southbound southbound;

    /**
     * Is security group ready.
     *
     * @param terminationPointAugmentation the intf
     * @return the boolean
     */
    public boolean isPortSecurityReady(OvsdbTerminationPointAugmentation terminationPointAugmentation) {
        if (neutronPortCache == null) {
            LOG.error("neutron port is null");
            return false;
        }
        LOG.trace("isPortSecurityReady for {}", terminationPointAugmentation.getName());
        String neutronPortId = southbound.getInterfaceExternalIdsValue(terminationPointAugmentation,
                Constants.EXTERNAL_ID_INTERFACE_ID);
        if (neutronPortId == null) {
            return false;
        }
        NeutronPort neutronPort = neutronPortCache.getPort(neutronPortId);
        if (neutronPort == null) {
            return false;
        }
        String deviceOwner = neutronPort.getDeviceOwner();
        if (!deviceOwner.contains("compute")) {
            LOG.debug("Port {} is not a compute host, it is a: {}", neutronPortId, deviceOwner);
        }
        LOG.debug("isPortSecurityReady() is a {} ", deviceOwner);
        List<NeutronSecurityGroup> securityGroups = neutronPort.getSecurityGroups();
        if (securityGroups.isEmpty()) {
            LOG.debug("Check for device: {} does not contain a Security Group for port: {}", deviceOwner,
                    neutronPortId);
            return false;
        }
        LOG.debug("Security Group Check {} DOES contain a Neutron Security Group", neutronPortId);
        return true;
    }

    /**
     * Gets security group in port.
     *
     * @param terminationPointAugmentation the intf
     * @return the security group in port
     */
    public NeutronSecurityGroup getSecurityGroupInPort(OvsdbTerminationPointAugmentation terminationPointAugmentation) {
        if (neutronPortCache == null) {
            LOG.error("neutron port is null");
            return null;
        }
        LOG.trace("isPortSecurityReady for {}", terminationPointAugmentation.getName());
        String neutronPortId = southbound.getInterfaceExternalIdsValue(terminationPointAugmentation,
                Constants.EXTERNAL_ID_INTERFACE_ID);
        if (neutronPortId == null) {
            return null;
        }
        NeutronPort neutronPort = neutronPortCache.getPort(neutronPortId);
        if (neutronPort == null) {
            return null;
        }

        List<NeutronSecurityGroup> neutronSecurityGroups = neutronPort.getSecurityGroups();
        if (neutronSecurityGroups != null) {
            return (NeutronSecurityGroup) neutronSecurityGroups.toArray()[0];
        } else {
            return null;
        }
    }

    @Override
    public NeutronPort getDHCPServerPort(
            OvsdbTerminationPointAugmentation terminationPointAugmentation) {
        if (neutronPortCache == null) {
            LOG.error("getDHCPServerPort: neutron port is null");
            return null;
        }
        LOG.trace("getDHCPServerPort for {}",
                terminationPointAugmentation.getName());
        String neutronPortId = southbound.getInterfaceExternalIdsValue(
                terminationPointAugmentation,
                Constants.EXTERNAL_ID_INTERFACE_ID);
        if (neutronPortId == null) {
            return null;
        }
        NeutronPort neutronPort = neutronPortCache.getPort(neutronPortId);
        if (neutronPort == null) {
            LOG.error("getDHCPServerPort: neutron port of {} is not found", neutronPortId);
            return null;
        }
        //Since all the fixed ip assigned to a port should be from the same network, first port is sufficient.
        List<Neutron_IPs> fixedIps = neutronPort.getFixedIPs();
        if(null==fixedIps || 0 == fixedIps.size() )
        {
            LOG.error("getDHCPServerPort: No fixed ip is assigned");
            return null;
        }

        String networkUUID = neutronPort.getNetworkUUID();
        for (NeutronPort port : neutronPortCache.getAllPorts()) {
            if (port.getNetworkUUID() == networkUUID && port.getDeviceOwner().contains("dhcp")) {
                return port;
            }
        }

        return null;

    }

    @Override
    public boolean isComputePort(OvsdbTerminationPointAugmentation terminationPointAugmentation) {
        if (neutronPortCache == null) {
            LOG.error("neutron port is null");
            return false;
        }
        LOG.trace("isComputePort for {}", terminationPointAugmentation.getName());
        String neutronPortId = southbound.getInterfaceExternalIdsValue(terminationPointAugmentation,
                Constants.EXTERNAL_ID_INTERFACE_ID);
        if (neutronPortId == null) {
            return false;
        }
        NeutronPort neutronPort = neutronPortCache.getPort(neutronPortId);
        if (neutronPort == null) {
            return false;
        }
        String deviceOwner = neutronPort.getDeviceOwner();
        if (!deviceOwner.contains("compute")) {
            LOG.debug("isComputePort : Port {} is not a DHCP server port", neutronPortId, deviceOwner);
            return false;
        }
        return true;
    }

    @Override
    public boolean isLastPortinSubnet(Node node, OvsdbTerminationPointAugmentation terminationPointAugmentation) {
        if (neutronPortCache == null) {
            LOG.error("isLastPortinSubnet: neutron port is null");
            return false;
        }
        LOG.trace("isLastPortinSubnet: for {}", terminationPointAugmentation.getName());
        String neutronPortId = southbound.getInterfaceExternalIdsValue(terminationPointAugmentation,
                                                                       Constants.EXTERNAL_ID_INTERFACE_ID);
        if (neutronPortId == null) {
            return false;
        }
        NeutronPort neutronPort = neutronPortCache.getPort(neutronPortId);
        if (neutronPort == null) {
            LOG.error("isLastPortinSubnet: neutron port of {} is not found", neutronPortId);
            return false;
        }
        List<Neutron_IPs> neutronPortFixedIp = neutronPort.getFixedIPs();
        if(null == neutronPortFixedIp || neutronPortFixedIp.isEmpty()) {
            return false;
        }
        List<TerminationPoint> terminationPoints = node.getTerminationPoint();
        if(terminationPoints != null && !terminationPoints.isEmpty()) {
            for(TerminationPoint tp : terminationPoints) {
                OvsdbTerminationPointAugmentation ovsdbTerminationPointAugmentation =
                        tp.getAugmentation(OvsdbTerminationPointAugmentation.class);
                if (ovsdbTerminationPointAugmentation != null && !ovsdbTerminationPointAugmentation.
                        getName().equals(Constants.INTEGRATION_BRIDGE)) {
                    String portId = southbound.getInterfaceExternalIdsValue(ovsdbTerminationPointAugmentation,
                                                                            Constants.EXTERNAL_ID_INTERFACE_ID);
                    if(null!=portId) {
                        NeutronPort port = neutronPortCache.getPort(portId);
                        if(null!=port) {
                            if(!(port.getID().equals(neutronPort.getID())) && port.getDeviceOwner().contains("compute")) {
                                List<Neutron_IPs> portFixedIp = port.getFixedIPs();
                                if(null == portFixedIp || portFixedIp.isEmpty()) {
                                    return false;
                                }
                                if(portFixedIp.iterator().next().getSubnetUUID().equals
                                        (neutronPort.getFixedIPs().iterator().next().getSubnetUUID())) {
                                    return false;
                                }
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    @Override
    public boolean isLastPortinBridge(Node node, OvsdbTerminationPointAugmentation terminationPointAugmentation) {
        LOG.trace("isLastPortinBridge: for {}", terminationPointAugmentation.getName());
        List<TerminationPoint> terminationPoints = node.getTerminationPoint();
        if(terminationPoints != null && !terminationPoints.isEmpty()){
            for(TerminationPoint tp : terminationPoints){
                OvsdbTerminationPointAugmentation ovsdbTerminationPointAugmentation =
                        tp.getAugmentation(OvsdbTerminationPointAugmentation.class);
                if(null!=ovsdbTerminationPointAugmentation)
                {
                    if(!(ovsdbTerminationPointAugmentation.getName().equals(Constants.INTEGRATION_BRIDGE))
                            && !(terminationPointAugmentation.getInterfaceUuid().equals
                                    (ovsdbTerminationPointAugmentation.getInterfaceUuid()))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @Override
    public List<Neutron_IPs> getIpAddress(Node node,
                                OvsdbTerminationPointAugmentation terminationPointAugmentation) {
        if (neutronPortCache == null) {
            LOG.error("getIpAddress: neutron port is null");
            return null;
        }
        LOG.trace("getIpAddress: for {}", terminationPointAugmentation.getName());
        String neutronPortId = southbound.getInterfaceExternalIdsValue(terminationPointAugmentation,
                Constants.EXTERNAL_ID_INTERFACE_ID);
        if (neutronPortId == null) {
            return null;
        }
        NeutronPort neutronPort = neutronPortCache.getPort(neutronPortId);
        if (neutronPort == null) {
            LOG.error("getIpAddress: neutron port of {} is not found", neutronPortId);
            return null;
        }
        return neutronPort.getFixedIPs();
    }

    @Override
    public void setDependencies(BundleContext bundleContext, ServiceReference serviceReference) {
        southbound =
                (Southbound) ServiceHelper.getGlobalInstance(Southbound.class, this);
    }

    @Override
    public void setDependencies(Object impl) {
        if (impl instanceof INeutronPortCRUD) {
            neutronPortCache = (INeutronPortCRUD)impl;
        }
        else if (impl instanceof INeutronSubnetCRUD) {
            neutronSubnetCache = (INeutronSubnetCRUD) impl;
        }
    }
}
