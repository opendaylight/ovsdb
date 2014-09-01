/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.impl;

import org.opendaylight.controller.networkconfig.neutron.INeutronPortCRUD;
import org.opendaylight.controller.networkconfig.neutron.NeutronPort;
import org.opendaylight.controller.networkconfig.neutron.NeutronSecurityGroup;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.ovsdb.openstack.netvirt.api.Constants;
import org.opendaylight.ovsdb.openstack.netvirt.api.SecurityServicesManager;

import org.opendaylight.ovsdb.schema.openvswitch.Interface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class SecurityServicesImpl implements SecurityServicesManager {

    static final Logger logger = LoggerFactory.getLogger(TenantNetworkManagerImpl.class);

    public SecurityServicesImpl() {
    }

    /**
     * Is security group ready.
     *
     * @param intf the intf
     * @return the boolean
     */
    public boolean isPortSecurityReady(Interface intf) {
        logger.trace("getTenantNetworkForInterface for {}", intf);
        if (intf == null) return false;
        Map<String, String> externalIds = intf.getExternalIdsColumn().getData();
        logger.trace("externalIds {}", externalIds);
        if (externalIds == null) return false;
        String neutronPortId = externalIds.get(Constants.EXTERNAL_ID_INTERFACE_ID);
        if (neutronPortId == null) return false;
        INeutronPortCRUD neutronPortService = (INeutronPortCRUD) ServiceHelper.getGlobalInstance(INeutronPortCRUD.class,
                this);
        NeutronPort neutronPort = neutronPortService.getPort(neutronPortId);
        String deviceOwner = neutronPort.getDeviceOwner();
        if (!deviceOwner.contains("compute")) {
            logger.debug("Port {} is not a compute host, it is a: {}", neutronPortId, deviceOwner);
        }
        logger.debug("isPortSecurityReady() is a {} ", deviceOwner);
        List<NeutronSecurityGroup> securityGroups = neutronPort.getSecurityGroups();
        if (securityGroups.isEmpty()) {
            logger.debug("Check for device: {} does not contain a Security Group for port: {}", deviceOwner,
                    neutronPortId);
            return false;
        }
        try {
            String vmPort = externalIds.get("attached-mac");
        } catch(Exception e) {
            logger.debug("Error VMID did *NOT* work");
        }
        logger.debug("Security Group Check {} DOES contain a Neutron Security Group", neutronPortId);
        return true;
    }

    /**
     * Gets security group in port.
     *
     * @param intf the intf
     * @return the security group in port
     */
    public NeutronSecurityGroup getSecurityGroupInPort(Interface intf) {
        logger.trace("getTenantNetworkForInterface for {}", intf);
        if (intf == null) return null;
        Map<String, String> externalIds = intf.getExternalIdsColumn().getData();
        logger.trace("externalIds {}", externalIds);
        if (externalIds == null) return null;
        String neutronPortId = externalIds.get(Constants.EXTERNAL_ID_INTERFACE_ID);
        if (neutronPortId == null) return null;
        INeutronPortCRUD neutronPortService = (INeutronPortCRUD)
                ServiceHelper.getGlobalInstance(INeutronPortCRUD.class, this);
        NeutronPort neutronPort = neutronPortService.getPort(neutronPortId);
        List<NeutronSecurityGroup> neutronSecurityGroups = neutronPort.getSecurityGroups();
        NeutronSecurityGroup neutronSecurityGroup = (NeutronSecurityGroup) neutronSecurityGroups.toArray()[0];
        return neutronSecurityGroup;
    }
}
