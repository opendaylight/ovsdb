/*
 * Copyright (C) 2014 Red Hat, Inc.
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
import org.opendaylight.ovsdb.openstack.netvirt.ConfigInterface;
import org.opendaylight.ovsdb.openstack.netvirt.api.Constants;
import org.opendaylight.ovsdb.openstack.netvirt.api.SecurityServicesManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.Southbound;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.*;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecurityServicesImpl implements ConfigInterface, SecurityServicesManager {
    static final Logger logger = LoggerFactory.getLogger(TenantNetworkManagerImpl.class);
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
            logger.error("neutron port is null");
            return false;
        }
        logger.trace("isPortSecurityReady for {}", terminationPointAugmentation.getName());
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
            logger.debug("Port {} is not a compute host, it is a: {}", neutronPortId, deviceOwner);
        }
        logger.debug("isPortSecurityReady() is a {} ", deviceOwner);
        List<NeutronSecurityGroup> securityGroups = neutronPort.getSecurityGroups();
        if (securityGroups.isEmpty()) {
            logger.debug("Check for device: {} does not contain a Security Group for port: {}", deviceOwner,
                    neutronPortId);
            return false;
        }
        String vmPort = southbound.getInterfaceExternalIdsValue(terminationPointAugmentation,
                Constants.EXTERNAL_ID_VM_MAC);
        logger.debug("Security Group Check {} DOES contain a Neutron Security Group", neutronPortId);
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
            logger.error("neutron port is null");
            return null;
        }
        logger.trace("isPortSecurityReady for {}", terminationPointAugmentation.getName());
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
            NeutronSecurityGroup neutronSecurityGroup = (NeutronSecurityGroup) neutronSecurityGroups.toArray()[0];
            return neutronSecurityGroup;
        } else {
            return null;
        }
    }

    @Override
	public NeutronPort getDHCPServerPort(
			OvsdbTerminationPointAugmentation terminationPointAugmentation) {
		if (neutronPortCache == null) {
			logger.error("neutron port is null");
			return null;
		}
		logger.trace("getDHCPServerPort for {}",
				terminationPointAugmentation.getName());
		String neutronPortId = southbound.getInterfaceExternalIdsValue(
				terminationPointAugmentation,
				Constants.EXTERNAL_ID_INTERFACE_ID);
		if (neutronPortId == null) {
			return null;
		}
		NeutronPort neutronPort = neutronPortCache.getPort(neutronPortId);
		// ToDO
		String subnetUUID = neutronPort.getFixedIPs().get(0).getSubnetUUID();
		NeutronSubnet neutronSubnet = neutronSubnetCache.getSubnet(subnetUUID);
		List<NeutronPort> ports = neutronSubnet.getPortsInSubnet();
		for (NeutronPort port : ports) {
			if (port.getDeviceOwner().contains("dhcp")) {
				return port;
			}
		}

		return null;

	}

	@Override
	public boolean isDHCPServerPort(
			OvsdbTerminationPointAugmentation terminationPointAugmentation) {
		if (neutronPortCache == null) {
			logger.error("neutron port is null");
			return false;
		}
		logger.trace("isDHCPServerPort for {}",
				terminationPointAugmentation.getName());
		String neutronPortId = southbound.getInterfaceExternalIdsValue(
				terminationPointAugmentation,
				Constants.EXTERNAL_ID_INTERFACE_ID);
		if (neutronPortId == null) {
			return false;
		}
		NeutronPort neutronPort = neutronPortCache.getPort(neutronPortId);
		if (neutronPort == null) {
			return false;
		}
		String deviceOwner = neutronPort.getDeviceOwner();
		if (!deviceOwner.contains("dhcp")) {
			logger.debug("Port {} is not a compute host, it is a: {}",
					neutronPortId, deviceOwner);
			return false;
		}
		return true;
	}

	@Override
	public boolean isLastPort(
			OvsdbTerminationPointAugmentation terminationPointAugmentation) {
		if (neutronPortCache == null) {
			logger.error("neutron port is null");
			return false;
		}
		logger.trace("islonePort for {}",
				terminationPointAugmentation.getName());
		String neutronPortId = southbound.getInterfaceExternalIdsValue(
				terminationPointAugmentation,
				Constants.EXTERNAL_ID_INTERFACE_ID);
		if (neutronPortId == null) {
			return false;
		}
		NeutronPort neutronPort = neutronPortCache.getPort(neutronPortId);
		String subnetUUID = neutronPort.getFixedIPs().get(0).getSubnetUUID();
		NeutronSubnet neutronSubnet = neutronSubnetCache.getSubnet(subnetUUID);
		List<NeutronPort> ports = neutronSubnet.getPortsInSubnet();
		for (NeutronPort port : ports) {
			if (!port.getID().equals(neutronPort.getID())
					&& port.getDeviceOwner().equals(
							neutronPort.getDeviceOwner())) {
				return false;
			}
		}

		return true;
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
