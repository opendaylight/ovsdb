/*
 * Copyright (c) 2014, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.impl;

import org.opendaylight.ovsdb.openstack.netvirt.ConfigInterface;
import org.opendaylight.ovsdb.openstack.netvirt.api.ConfigurationService;
import org.opendaylight.ovsdb.openstack.netvirt.api.Constants;
import org.opendaylight.ovsdb.openstack.netvirt.api.EgressAclProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.IngressAclProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.SecurityServicesManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.Southbound;
import org.opendaylight.neutron.spi.INeutronNetworkCRUD;
import org.opendaylight.neutron.spi.INeutronPortCRUD;
import org.opendaylight.neutron.spi.INeutronSubnetCRUD;
import org.opendaylight.neutron.spi.NeutronNetwork;
import org.opendaylight.neutron.spi.NeutronPort;
import org.opendaylight.neutron.spi.NeutronSecurityGroup;
import org.opendaylight.neutron.spi.NeutronSecurityRule;
import org.opendaylight.neutron.spi.NeutronSubnet;
import org.opendaylight.neutron.spi.Neutron_IPs;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class SecurityServicesImpl implements ConfigInterface, SecurityServicesManager {
    private static final Logger LOG = LoggerFactory.getLogger(TenantNetworkManagerImpl.class);
    private volatile INeutronPortCRUD neutronPortCache;
    private volatile INeutronSubnetCRUD neutronSubnetCache;
    private volatile Southbound southbound;
    private volatile INeutronNetworkCRUD neutronNetworkCache;
    private volatile ConfigurationService configurationService;
    private volatile IngressAclProvider ingressAclProvider;
    private volatile EgressAclProvider egressAclProvider;

    @Override
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
        LOG.debug("Security Group Check {} does contain a Neutron Security Group", neutronPortId);
        return true;
    }

    @Override
    public List<NeutronSecurityGroup> getSecurityGroupInPortList(OvsdbTerminationPointAugmentation
                                                             terminationPointAugmentation) {
        List<NeutronSecurityGroup> neutronSecurityGroups = new ArrayList<>();
        if (neutronPortCache == null) {
            LOG.error("neutron port is null");
            return neutronSecurityGroups;
        }
        LOG.trace("isPortSecurityReady for {}", terminationPointAugmentation.getName());
        String neutronPortId = southbound.getInterfaceExternalIdsValue(terminationPointAugmentation,
                                                                       Constants.EXTERNAL_ID_INTERFACE_ID);
        if (neutronPortId == null) {
            return neutronSecurityGroups;
        }
        NeutronPort neutronPort = neutronPortCache.getPort(neutronPortId);
        if (neutronPort == null) {
            return neutronSecurityGroups;
        }
        neutronSecurityGroups = neutronPort.getSecurityGroups();
        return neutronSecurityGroups;

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
            if (neutronPort == null) {
                LOG.error("getDHCPServerPort: neutron port of {} is not found", neutronPortId);
                return null;
            }
            /* if the current port is a DHCP port, return the same*/
            if (neutronPort.getDeviceOwner().contains("dhcp")) {
                return neutronPort;
            }
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
            if (neutronSubnet == null) {
                LOG.error("getDHCPServerPort: No subnet is found for " + subnetUuid);
                return null;
            }
            List<NeutronPort> ports = neutronSubnet.getPortsInSubnet();
            for (NeutronPort port : ports) {
                if (port.getDeviceOwner().contains("dhcp")) {
                    return port;
                }
            }
        } catch (Exception e) {
            LOG.error("getDHCPServerPort:getDHCPServerPort failed due to ", e);
            return null;
        }
        return null;
    }

    @Override
    public NeutronPort getNeutronPortFromDhcpIntf(
            OvsdbTerminationPointAugmentation terminationPointAugmentation) {
        if (neutronPortCache == null) {
            LOG.error("getNeutronPortFromDhcpIntf: neutron port is null");
            return null;
        }
        String neutronPortId = southbound.getInterfaceExternalIdsValue(
                terminationPointAugmentation,
                Constants.EXTERNAL_ID_INTERFACE_ID);
        if (neutronPortId == null) {
            return null;
        }
        NeutronPort neutronPort = neutronPortCache.getPort(neutronPortId);
        if (neutronPort == null) {
            LOG.error("getNeutronPortFromDhcpIntf: neutron port of {} is not found", neutronPortId);
            return null;
        }
        /* if the current port is a DHCP port, return true*/
        if (neutronPort.getDeviceOwner().contains("dhcp")) {
            LOG.trace("getNeutronPortFromDhcpIntf: neutronPort is a dhcp port", neutronPort );
            return neutronPort;
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
            LOG.debug("isComputePort : Port {} is not a DHCP server port for device owner {}",
                      neutronPortId,deviceOwner);
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
            if (neutronPort == null) {
                LOG.error("isLastPortinSubnet: neutron port of {} is not found", neutronPortId);
                return false;
            }
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
                            if (null != port && !(port.getID().equals(neutronPort.getID()))
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
        } catch (Exception e) {
            LOG.error("isLastPortinSubnet: isLastPortinSubnet failed due to ", e);
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
                if (null != ovsdbTerminationPointAugmentation
                        && !(ovsdbTerminationPointAugmentation.getName().equals(Constants.INTEGRATION_BRIDGE))
                        && !(terminationPointAugmentation.getInterfaceUuid()
                        .equals(ovsdbTerminationPointAugmentation.getInterfaceUuid()))) {
                    LOG.debug("isLastPortinBridge: it the last port in bridge {}",
                            terminationPointAugmentation.getName());
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public List<Neutron_IPs> getIpAddressList(OvsdbTerminationPointAugmentation terminationPointAugmentation) {
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
    public List<Neutron_IPs> getVmListForSecurityGroup(String portUuid, String securityGroupUuid) {
        List<Neutron_IPs> vmListForSecurityGroup = new ArrayList<>();
        /*For every port check whether security grouplist contains the current
         * security group.*/
        try {
            for (NeutronPort neutronPort:neutronPortCache.getAllPorts()) {
                if (!neutronPort.getDeviceOwner().contains("compute")) {
                    LOG.debug("getVMListForSecurityGroup : the port {} is not "
                            + "compute port belongs to {}", neutronPort.getID(), neutronPort.getDeviceOwner());
                    continue;
                }
                if (portUuid.equals(neutronPort.getID())) {
                    continue;
                }
                List<NeutronSecurityGroup> securityGroups = neutronPort.getSecurityGroups();
                if (null != securityGroups) {
                    for (NeutronSecurityGroup securityGroup:securityGroups) {
                        if (securityGroup.getSecurityGroupUUID().equals(securityGroupUuid)) {
                            LOG.debug("getVMListForSecurityGroup : adding ports with ips {} "
                                    + "compute port", neutronPort.getFixedIPs());
                            vmListForSecurityGroup.addAll(neutronPort.getFixedIPs());
                        }
                    }
                }

            }
        } catch (Exception e) {
            LOG.error("getVMListForSecurityGroup: getVMListForSecurityGroup"
                    + " failed due to ", e);
            return null;
        }
        return vmListForSecurityGroup;

    }

    @Override
    public void syncSecurityGroup(NeutronPort port, List<NeutronSecurityGroup> securityGroupList, boolean write) {
        LOG.trace("syncSecurityGroup:" + securityGroupList + " Write:" + write);
        if (null != port && null != port.getSecurityGroups()) {
            Node node = getNode(port);
            if (node == null) {
                return;
            }
            NeutronNetwork neutronNetwork = neutronNetworkCache.getNetwork(port.getNetworkUUID());
            if (neutronNetwork == null) {
                return;
            }
            String segmentationId = neutronNetwork.getProviderSegmentationID();
            OvsdbTerminationPointAugmentation intf = getInterface(node, port);
            if (intf == null) {
                return;
            }
            long localPort = southbound.getOFPort(intf);
            String attachedMac = southbound.getInterfaceExternalIdsValue(intf, Constants.EXTERNAL_ID_VM_MAC);
            if (attachedMac == null) {
                LOG.debug("programVlanRules: No AttachedMac seen in {}", intf);
                return;
            }
            long dpid = getDpidOfIntegrationBridge(node);
            if (dpid == 0L) {
                return;
            }
            String neutronPortId = southbound.getInterfaceExternalIdsValue(intf,
                                                                           Constants.EXTERNAL_ID_INTERFACE_ID);
            if (neutronPortId == null) {
                LOG.debug("syncSecurityGroup: No neutronPortId seen in {}", intf);
                return;
            }
            for (NeutronSecurityGroup securityGroupInPort:securityGroupList) {
                ingressAclProvider.programPortSecurityGroup(dpid, segmentationId, attachedMac, localPort,
                                                          securityGroupInPort, neutronPortId, write);
                egressAclProvider.programPortSecurityGroup(dpid, segmentationId, attachedMac, localPort,
                                                         securityGroupInPort, neutronPortId, write);
            }
        }
    }

    @Override
    public void syncSecurityRule(NeutronPort port, NeutronSecurityRule securityRule,Neutron_IPs vmIp, boolean write) {
        LOG.trace("syncSecurityGroup:" + securityRule + " Write:" + write);
        if (null != port && null != port.getSecurityGroups()) {
            Node node = getNode(port);
            if (node == null) {
                return;
            }
            NeutronNetwork neutronNetwork = neutronNetworkCache.getNetwork(port.getNetworkUUID());
            if (neutronNetwork == null) {
                return;
            }
            String segmentationId = neutronNetwork.getProviderSegmentationID();
            OvsdbTerminationPointAugmentation intf = getInterface(node, port);
            if (intf == null) {
                return;
            }
            long localPort = southbound.getOFPort(intf);
            String attachedMac = southbound.getInterfaceExternalIdsValue(intf, Constants.EXTERNAL_ID_VM_MAC);
            if (attachedMac == null) {
                LOG.debug("programVlanRules: No AttachedMac seen in {}", intf);
                return;
            }
            long dpid = getDpidOfIntegrationBridge(node);
            if (dpid == 0L) {
                return;
            }
            if ("IPv4".equals(securityRule.getSecurityRuleEthertype())
                    && "ingress".equals(securityRule.getSecurityRuleDirection())) {

                ingressAclProvider.programPortSecurityRule(dpid, segmentationId, attachedMac, localPort,
                                                           securityRule, vmIp, write);
            } else if (securityRule.getSecurityRuleEthertype().equals("IPv4")
                    && securityRule.getSecurityRuleDirection().equals("egress")) {
                egressAclProvider.programPortSecurityRule(dpid, segmentationId, attachedMac, localPort,
                                                          securityRule, vmIp, write);
            }
        }
    }

    private long getDpidOfIntegrationBridge(Node node) {
        LOG.trace("getDpidOfIntegrationBridge:" + node);
        long dpid = 0L;
        if (southbound.getBridgeName(node).equals(configurationService.getIntegrationBridgeName())) {
            dpid = getDpid(node);
        }
        if (dpid == 0L) {
            LOG.warn("getDpidOfIntegerationBridge: dpid not found: {}", node);
        }
        return dpid;
    }

    private long getDpid(Node node) {
        LOG.trace("getDpid" + node);
        long dpid = southbound.getDataPathId(node);
        if (dpid == 0) {
            LOG.warn("getDpid: dpid not found: {}", node);
        }
        return dpid;
    }

    private Node getNode(NeutronPort port) {
        LOG.trace("getNode:Port" + port);
        List<Node> toplogyNodes = southbound.readOvsdbTopologyNodes();

        for (Node topologyNode : toplogyNodes) {
            try {
                Node node = southbound.getBridgeNode(topologyNode,Constants.INTEGRATION_BRIDGE);
                List<OvsdbTerminationPointAugmentation> ovsdbPorts = southbound.getTerminationPointsOfBridge(node);
                for (OvsdbTerminationPointAugmentation ovsdbPort : ovsdbPorts) {
                    String uuid = southbound.getInterfaceExternalIdsValue(ovsdbPort,
                                                            Constants.EXTERNAL_ID_INTERFACE_ID);
                    if (null != uuid && uuid.equals(port.getID())) {
                        return node;
                    }
                }
            } catch (Exception e) {
                LOG.error("Exception during handlingNeutron network delete", e);
            }
        }
        LOG.info("no node found for port:" + port);
        return null;
    }

    private OvsdbTerminationPointAugmentation getInterface(Node node, NeutronPort port) {
        LOG.trace("getInterface:Node:" + node + " Port:" + port);
        try {
            List<OvsdbTerminationPointAugmentation> ovsdbPorts = southbound.getTerminationPointsOfBridge(node);
            for (OvsdbTerminationPointAugmentation ovsdbPort : ovsdbPorts) {
                String uuid = southbound.getInterfaceExternalIdsValue(ovsdbPort,
                                                                      Constants.EXTERNAL_ID_INTERFACE_ID);
                if (null != uuid && uuid.equals(port.getID())) {
                    return ovsdbPort;
                }
            }
        } catch (Exception e) {
            LOG.error("Exception during handlingNeutron network delete", e);
        }
        LOG.info("no interface found for node: " + node + " port:" + port);
        return null;
    }

    @Override
    public void setDependencies(BundleContext bundleContext, ServiceReference serviceReference) {
        southbound =
                (Southbound) ServiceHelper.getGlobalInstance(Southbound.class, this);
        neutronNetworkCache =
                (INeutronNetworkCRUD) ServiceHelper.getGlobalInstance(INeutronNetworkCRUD.class, this);
        configurationService =
                (ConfigurationService) ServiceHelper.getGlobalInstance(ConfigurationService.class, this);

    }

    @Override
    public void setDependencies(Object impl) {
        if (impl instanceof INeutronPortCRUD) {
            neutronPortCache = (INeutronPortCRUD)impl;
        } else if (impl instanceof INeutronSubnetCRUD) {
            neutronSubnetCache = (INeutronSubnetCRUD) impl;
        } else if (impl instanceof IngressAclProvider) {
            ingressAclProvider = (IngressAclProvider) impl;
        } else if (impl instanceof EgressAclProvider) {
            egressAclProvider = (EgressAclProvider) impl;
        }
    }
}
