/*
 * Copyright (c) 2014, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.api;

import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronPort;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronSecurityGroup;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronSecurityRule;
import org.opendaylight.ovsdb.openstack.netvirt.translator.Neutron_IPs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

import java.util.List;

/**
 * Open vSwitch isolates Tenant Networks using VLANs on the Integration Bridge.
 * This class manages the provisioning of these VLANs
 */
public interface SecurityServicesManager {
    /**
     * Is port security ready.
     *
     * @param intf the intf
     * @return the boolean
     */
    boolean isPortSecurityReady(OvsdbTerminationPointAugmentation intf);
    /**
     * Gets security group in port.
     *
     * @param intf the intf
     * @return the list of security group in port, returns empty list if no group associated.
     */
    List<NeutronSecurityGroup> getSecurityGroupInPortList(OvsdbTerminationPointAugmentation intf);
    /**
     * Gets the DHCP server port corresponding to a network.
     *
     * @param intf the intf
     * @return the dhcp server port
     */
    NeutronPort getDhcpServerPort(OvsdbTerminationPointAugmentation intf);
    /**
     * Gets the NeutronPort from the cleanup cache.
     *
     * @param intf the intf
     * @return the NeutronPort stored in the cleanupCache of NeutronL3Adapter
     */
    NeutronPort getNeutronPortFromCache(OvsdbTerminationPointAugmentation intf);

    /**
      * Check if the given interface corresponds to a DHCP server port.
      *
      * @param intf the intf
      * @return Return the DHCP neutron port
      */
    NeutronPort getNeutronPortFromDhcpIntf(OvsdbTerminationPointAugmentation intf);

    /**
     * Is the port a compute port.
     *
     * @param intf the intf
     * @return  whether it is a compute port or not
     */
    boolean isComputePort(OvsdbTerminationPointAugmentation intf);

    /**
     * Is this the last port in the subnet to which interface belongs to.
     * @param node The node to which the intf is connected.
     * @param intf the intf
     * @return whether last port in the subnet
     */
    boolean isLastPortinSubnet(Node node, OvsdbTerminationPointAugmentation intf);

    /**
     * Is this the last port in the bridge to which interface belongs to.
     * @param node The node to which the intf is connected.
     * @param intf the intf
     * @return whether last port in bridge
     */
    boolean isLastPortinBridge(Node node, OvsdbTerminationPointAugmentation intf);
    /**
     * Returns the  list of ip address assigned to the interface.
     * @param intf the intf
     * @return the list of ip address associated with the vm
     */
    List<Neutron_IPs> getIpAddressList(OvsdbTerminationPointAugmentation intf);
    /**
     * Get the list of vm belonging to a security group.
     * @param portUuid the uuid of the port.
     * @param securityGroupUuid the UUID of the remote security group.
     * @return the list of all vm belonging to the security group UUID passed.
     */
    List<Neutron_IPs> getVmListForSecurityGroup(String portUuid,
                                                String securityGroupUuid);
    /**
     * Add or remove the security groups  from the port.
     * @param port the neutron port.
     * @param securityGroup the security group associated with the port.
     * @param write whether to add/delete flow.
     */
    void syncSecurityGroup(NeutronPort port, List<NeutronSecurityGroup> securityGroup, boolean write);
    /**
     * Add or remove individual security  rules from the port.
     * @param port the neutron port.
     * @param securityRule the security group associated with the port.
     * @param vmIp The list of remote vm ips.
     * @param write whether to add/delete flow.
     */
    void syncSecurityRule(NeutronPort port, NeutronSecurityRule securityRule,Neutron_IPs vmIp, boolean write);
}