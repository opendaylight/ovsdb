/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.ovsdb.openstack.netvirt.api;

import org.opendaylight.neutron.spi.NeutronSecurityGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.*;

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

}