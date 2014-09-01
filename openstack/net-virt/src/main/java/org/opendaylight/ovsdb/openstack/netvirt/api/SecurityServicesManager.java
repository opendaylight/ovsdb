/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.ovsdb.openstack.netvirt.api;

import org.opendaylight.controller.networkconfig.neutron.NeutronSecurityGroup;
import org.opendaylight.ovsdb.schema.openvswitch.Interface;

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
    public boolean isPortSecurityReady(Interface intf);
    /**
     * Gets security group in port.
     *
     * @param intf the intf
     * @return the security group in port
     */
    public NeutronSecurityGroup getSecurityGroupInPort(Interface intf);

}