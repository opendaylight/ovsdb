/*
 * Copyright (c) 2014, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.impl;

import java.util.ArrayList;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
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

    @Override
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

    @Override
    /**
     * Gets security group in port.
     *
     * @param terminationPointAugmentation the intf
     * @return the security group list in port
     */
    public List<NeutronSecurityGroup> getSecurityGroupInPort(OvsdbTerminationPointAugmentation
                                                             terminationPointAugmentation) {
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
            return neutronSecurityGroups;
        }

        return null;

    }

    @Override
    public NeutronPort getDhcpServerPort(OvsdbTerminationPointAugmentation terminationPointAugmentation) {
        if (neutronPortCache == null) {
            LOG.error("getDHCPServerPort: neutron port is null");
            return null;
        }
        LOG.trace("getDHCPServerPort for {}",
                  terminationPointAugmentation.getName());
        try {
            String neutronPortId = southbound.getInterfaceExternalIdsValue(terminationPointAugmentation,
                                                                           Constants.EXTERNAL_ID_INTERFACE_ID);
            if (neutronPortId == null) {
                return null;
            }
            NeutronPort neutronPort = neutronPortCache.getPort(neutronPortId);
            /*Since all the fixed ip assigned to a port should be
             *from the same network, first port is sufficient.*/
            List<Neutron_IPs> fixedIps = neutronPort.getFixedIPs();
            if (null == fixedIps || 0 == fixedIps.size() ) {
                LOG.error("getDHCPServerPort: No fixed ip is assigned");
                return null;
            }
            /* Get all the ports in the subnet and identify the dhcp port*/
            String subnetUuid = fixedIps.iterator().next().getSubnetUUID();
            NeutronSubnet neutronSubnet = neutronSubnetCache.getSubnet(subnetUuid);
            List<NeutronPort> ports = neutronSubnet.getPortsInSubnet();
            for (NeutronPort port : ports) {
                if (port.getDeviceOwner().contains("dhcp")) {
                    return port;
                }
            }
        } catch (Exception e) {
            LOG.error("getDHCPServerPort:getDHCPServerPort failed due to " +  e);
            return null;
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
        /*Check the device owner and if it contains compute to identify
         * whether it is a compute port.*/
        String deviceOwner = neutronPort.getDeviceOwner();
        if (!deviceOwner.contains("compute")) {
            LOG.debug("isComputePort : Port {} is not a DHCP server port", neutronPortId,deviceOwner);
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
        try {
            LOG.trace("isLastPortinSubnet: for {}", terminationPointAugmentation.getName());
            String neutronPortId = southbound.getInterfaceExternalIdsValue(terminationPointAugmentation,
                                                                           Constants.EXTERNAL_ID_INTERFACE_ID);
            if (neutronPortId == null) {
                return false;
            }
            NeutronPort neutronPort = neutronPortCache.getPort(neutronPortId);
            List<Neutron_IPs> neutronPortFixedIp = neutronPort.getFixedIPs();
            if (null == neutronPortFixedIp || neutronPortFixedIp.isEmpty()) {
                return false;
            }
            /*Get all the ports in the current node and check whether there
             * is any port belonging to the same subnet of the input
             */
            List<TerminationPoint> terminationPoints = node.getTerminationPoint();
            if (terminationPoints != null && !terminationPoints.isEmpty()) {
                for (TerminationPoint tp : terminationPoints) {
                    OvsdbTerminationPointAugmentation ovsdbTerminationPointAugmentation =
                            tp.getAugmentation(OvsdbTerminationPointAugmentation.class);
                    if (ovsdbTerminationPointAugmentation != null && !ovsdbTerminationPointAugmentation
                            .getName().equals(Constants.INTEGRATION_BRIDGE)) {
                        String portId = southbound.getInterfaceExternalIdsValue(ovsdbTerminationPointAugmentation,
                                                                                Constants.EXTERNAL_ID_INTERFACE_ID);
                        if (null != portId) {
                            NeutronPort port = neutronPortCache.getPort(portId);
                            if (null != port) {
                                if (!(port.getID().equals(neutronPort.getID()))
                                        && port.getDeviceOwner().contains("compute")) {
                                    List<Neutron_IPs> portFixedIp = port.getFixedIPs();
                                    if (null == portFixedIp || portFixedIp.isEmpty()) {
                                        return false;
                                    }
                                    if (portFixedIp.iterator().next().getSubnetUUID()
                                            .equals(neutronPort.getFixedIPs().iterator().next().getSubnetUUID())) {
                                        LOG.trace("isLastPortinSubnet: Port is not the only port.");
                                        return false;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("isLastPortinSubnet: isLastPortinSubnet failed due to " +  e);
            return false;
        }
        return true;
    }

    @Override
    public boolean isLastPortinBridge(Node node, OvsdbTerminationPointAugmentation terminationPointAugmentation) {
        LOG.trace("isLastPortinBridge: for {}", terminationPointAugmentation.getName());
        List<TerminationPoint> terminationPoints = node.getTerminationPoint();
        /*Check whether the node has any port other than br-int*/
        if (terminationPoints != null && !terminationPoints.isEmpty()) {
            for (TerminationPoint tp : terminationPoints) {
                OvsdbTerminationPointAugmentation ovsdbTerminationPointAugmentation =
                        tp.getAugmentation(OvsdbTerminationPointAugmentation.class);
                if (null != ovsdbTerminationPointAugmentation) {
                    if (!(ovsdbTerminationPointAugmentation.getName().equals(Constants.INTEGRATION_BRIDGE))
                            && !(terminationPointAugmentation.getInterfaceUuid()
                                    .equals(ovsdbTerminationPointAugmentation.getInterfaceUuid()))) {
                        LOG.debug("isLastPortinBridge: it the last port in bridge",
                                  terminationPointAugmentation.getName());
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
        return neutronPort.getFixedIPs();
    }

    @Override
    public List<Neutron_IPs> getVmListForSecurityGroup(List<Neutron_IPs> srcAddressList, String securityGroupUuid) {
        List<Neutron_IPs> vmListForSecurityGroup = new ArrayList<Neutron_IPs>();
        /*For every port check whether security grouplist contains the current
         * security group.*/
        try {
            for (NeutronPort neutronPort:neutronPortCache.getAllPorts()) {
                if (!neutronPort.getDeviceOwner().contains("compute")) {
                    LOG.debug("getVMListForSecurityGroup : the port is not "
                            + "compute port", neutronPort.getID(), neutronPort.getDeviceOwner());
                    continue;
                }
                List<NeutronSecurityGroup> securityGroups = neutronPort.getSecurityGroups();
                if (null != securityGroups) {
                    for (NeutronSecurityGroup securityGroup:securityGroups) {
                        if (securityGroup.getSecurityGroupUUID().equals(securityGroupUuid)
                                && !neutronPort.getFixedIPs().containsAll(srcAddressList)) {
                            LOG.debug("getVMListForSecurityGroup : adding ports with ips "
                                    + "compute port", neutronPort.getFixedIPs());
                            vmListForSecurityGroup.addAll(neutronPort.getFixedIPs());
                        }
                    }
                }

            }
        } catch (Exception e) {
            LOG.error("getVMListForSecurityGroup: getVMListForSecurityGroup"
                    + " failed due to " +  e);
            return null;
        }
        return vmListForSecurityGroup;

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
        } else if (impl instanceof INeutronSubnetCRUD) {
            neutronSubnetCache = (INeutronSubnetCRUD) impl;
        }
    }
}
