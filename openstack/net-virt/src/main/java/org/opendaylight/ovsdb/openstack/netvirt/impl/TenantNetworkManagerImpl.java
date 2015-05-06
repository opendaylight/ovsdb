/*
 * Copyright (C) 2013 Red Hat, Inc. and others...
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Brent Salisbury, Dave Tucker
 */
package org.opendaylight.ovsdb.openstack.netvirt.impl;

import org.opendaylight.neutron.spi.INeutronNetworkCRUD;
import org.opendaylight.neutron.spi.INeutronPortCRUD;
import org.opendaylight.neutron.spi.NeutronNetwork;
import org.opendaylight.neutron.spi.NeutronPort;
import org.opendaylight.ovsdb.openstack.netvirt.api.*;
import org.opendaylight.ovsdb.utils.servicehelper.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TenantNetworkManagerImpl implements TenantNetworkManager {
    static final Logger logger = LoggerFactory.getLogger(TenantNetworkManagerImpl.class);

    // The implementation for each of these services is resolved by the OSGi Service Manager
    private volatile INeutronNetworkCRUD neutronNetworkCache;
    private volatile INeutronPortCRUD neutronPortCache;
    private volatile VlanConfigurationCache vlanConfigurationCache;

    void init() {
        logger.info(">>>>>> init {}", this.getClass());
    }

    @Override
    public int getInternalVlan(Node node, String networkId) {
        Integer vlan = vlanConfigurationCache.getInternalVlan(node, networkId);
        if (vlan == null) return 0;
        return vlan;
    }

    @Override
    public void reclaimInternalVlan(Node node, NeutronNetwork network) {
        int vlan = vlanConfigurationCache.reclaimInternalVlan(node, network.getID());
        if (vlan <= 0) {
            logger.debug("Unable to get an internalVlan for Network {}", network);
            return;
        }
        logger.debug("Removed Vlan {} on {}", vlan);
    }

    @Override
    public void programInternalVlan(Node node, OvsdbTerminationPointAugmentation tp, NeutronNetwork network) {
        int vlan = vlanConfigurationCache.getInternalVlan(node, network.getID());
        logger.debug("Programming Vlan {} on {}", vlan, tp);
        if (vlan <= 0) {
            logger.debug("Unable to get an internalVlan for Network {}", network);
            return;
        }

        MdsalUtils.addVlanToTp(vlan);
    }

    @Override
    public boolean isTenantNetworkPresentInNode(Node node, String segmentationId) {
        String networkId = this.getNetworkId(segmentationId);
        if (networkId == null) {
            logger.debug("Tenant Network not found with Segmenation-id {}",segmentationId);
            return false;
        }

        try {
            List<OvsdbTerminationPointAugmentation> ports = MdsalUtils.getPorts(node);
            for (OvsdbTerminationPointAugmentation port : ports) {
                String ifaceId = MdsalUtils.getInterfaceExternalIdsValue(port, Constants.EXTERNAL_ID_INTERFACE_ID);
                if (ifaceId != null && isInterfacePresentInTenantNetwork(ifaceId, networkId)) {
                    logger.debug("Tenant Network {} with Segmentation-id {} is present in Node {} / Interface {}",
                            networkId, segmentationId, node, port);
                    return true;
                }
            }
        } catch (Exception e) {
            logger.error("Error while trying to determine if network is present on node", e);
            return false;
        }

        logger.debug("Tenant Network {} with Segmenation-id {} is NOT present in Node {}",
                networkId, segmentationId, node);

        return false;
    }

    @Override
    public String getNetworkId(String segmentationId) {
        List <NeutronNetwork> networks = neutronNetworkCache.getAllNetworks();
        for (NeutronNetwork network : networks) {
            if (network.getProviderSegmentationID().equalsIgnoreCase(segmentationId)) return network.getNetworkUUID();
        }
        return null;
    }

    @Override
    public NeutronNetwork getTenantNetwork(OvsdbTerminationPointAugmentation terminationPointAugmentation) {
        NeutronNetwork neutronNetwork = null;
        neutronPortCache = (INeutronPortCRUD) ServiceHelper.getGlobalInstance(INeutronPortCRUD.class, this);
        neutronNetworkCache = (INeutronNetworkCRUD) ServiceHelper.getGlobalInstance(INeutronNetworkCRUD.class, this);
        if (neutronPortCache == null || neutronNetworkCache == null) {
            logger.error("neutron interfaces are null");
            return neutronNetwork;
        }
        logger.debug("getTenantNetwork for {}", terminationPointAugmentation.getName());
        String neutronPortId = MdsalUtils.getInterfaceExternalIdsValue(terminationPointAugmentation,
                Constants.EXTERNAL_ID_INTERFACE_ID);
        if (neutronPortId != null) {
            NeutronPort neutronPort = neutronPortCache.getPort(neutronPortId);
            if (neutronPort != null) {
                neutronNetwork = neutronNetworkCache.getNetwork(neutronPort.getNetworkUUID());
                if (neutronNetwork != null) {
                    logger.debug("mapped to {}", neutronNetwork);
                }
            }
        }
        if (neutronNetwork != null) {
            logger.debug("mapped to {}", neutronNetwork);
        } else {
            logger.warn("getTenantNetwork did not find network for {}", terminationPointAugmentation.getName());
        }
        return neutronNetwork;
    }

    @Override
    public NeutronPort getTenantPort(OvsdbTerminationPointAugmentation terminationPointAugmentation) {
        NeutronPort neutronPort = null;
        neutronPortCache = (INeutronPortCRUD) ServiceHelper.getGlobalInstance(INeutronPortCRUD.class, this);
        if (neutronPortCache == null) {
            logger.error("neutron port is null");
            return neutronPort;
        }
        logger.trace("getTenantPort for {}", terminationPointAugmentation.getName());
        String neutronPortId = MdsalUtils.getInterfaceExternalIdsValue(terminationPointAugmentation,
            Constants.EXTERNAL_ID_INTERFACE_ID);
        if (neutronPortId != null) {
            neutronPort = neutronPortCache.getPort(neutronPortId);
        }
        if (neutronPort != null) {
            logger.debug("mapped to {}", neutronPort);
        } else {
            logger.warn("getTenantPort did not find port for {}", terminationPointAugmentation.getName());
        }

        return neutronPort;
    }

    @Override
    public int networkCreated (Node node, String networkId) {
        return vlanConfigurationCache.assignInternalVlan(node, networkId);
    }

    @Override
    public void networkDeleted(String id) {
        //ToDo: Delete? This method does nothing since container support was dropped...
    }

    private boolean isInterfacePresentInTenantNetwork (String portId, String networkId) {
        NeutronPort neutronPort = neutronPortCache.getPort(portId);
        return neutronPort != null && neutronPort.getNetworkUUID().equalsIgnoreCase(networkId);
    }
}
