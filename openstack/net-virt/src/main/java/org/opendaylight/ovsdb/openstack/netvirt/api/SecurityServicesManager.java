/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.ovsdb.openstack.netvirt.api;

import java.util.List;

import org.opendaylight.neutron.spi.NeutronPort;
import org.opendaylight.neutron.spi.NeutronSecurityGroup;
import org.opendaylight.neutron.spi.Neutron_IPs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.*;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

/**
 * Open vSwitch isolates Tenant Networks using VLANs on the Integration Bridge
 * This class manages the provisioning of these VLANs
 */
public interface SecurityServicesManager {
    /**
     * Is port security ready.
     *
     * @param intf the intf
     * @return the boolean
     */
    public boolean isPortSecurityReady(OvsdbTerminationPointAugmentation intf);
    /**
     * Gets security group in port.
     *
     * @param intf the intf
     * @return the security group in port
     */
    public NeutronSecurityGroup getSecurityGroupInPort(OvsdbTerminationPointAugmentation intf);
     /**
     * Gets the DHCP server port corresponding to a network.
     *
     * @param intf the intf
     * @return the security group in port
     */
    public NeutronPort getDHCPServerPort(OvsdbTerminationPointAugmentation intf);

    /**
     * Is the port a compute port.
     *
     * @param intf the intf
     * @return the security group in port
     */
    public boolean isComputePort(OvsdbTerminationPointAugmentation intf);

    /**
     * Is this the last port in the subnet to which interface belongs to.
     *
     * @param intf the intf
     * @return the security group in port
     */
    public boolean isLastPortinSubnet(Node node, OvsdbTerminationPointAugmentation intf);

    /**
     * Is this the last port in the bridge to which interface belongs to.
     *
     * @param intf the intf
     * @return the security group in port
     */
    public boolean isLastPortinBridge(Node node, OvsdbTerminationPointAugmentation intf);
    /**
     * Returns the  list of ip adddress assigned to the interface.
     *
     * @param intf the intf
     * @return the security group in port
     */
    public List<Neutron_IPs> getIpAddress(Node node, OvsdbTerminationPointAugmentation intf);
}