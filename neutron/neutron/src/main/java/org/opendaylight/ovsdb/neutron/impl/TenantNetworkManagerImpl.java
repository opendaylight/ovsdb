/*
 * Copyright (C) 2013 Red Hat, Inc. and others...
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Brent Salisbury, Dave Tucker
 */
package org.opendaylight.ovsdb.neutron.impl;

import org.opendaylight.controller.networkconfig.neutron.INeutronNetworkCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronPortCRUD;
import org.opendaylight.controller.networkconfig.neutron.NeutronNetwork;
import org.opendaylight.controller.networkconfig.neutron.NeutronPort;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.ovsdb.lib.notation.OvsdbSet;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.neutron.Constants;
import org.opendaylight.ovsdb.neutron.api.NetworkingProviderManager;
import org.opendaylight.ovsdb.neutron.api.TenantNetworkManager;
import org.opendaylight.ovsdb.neutron.api.VlanConfigurationCache;
import org.opendaylight.ovsdb.plugin.IConnectionServiceInternal;
import org.opendaylight.ovsdb.plugin.OvsdbConfigService;
import org.opendaylight.ovsdb.schema.openvswitch.Interface;
import org.opendaylight.ovsdb.schema.openvswitch.Port;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class TenantNetworkManagerImpl implements TenantNetworkManager {
    static final Logger logger = LoggerFactory.getLogger(TenantNetworkManagerImpl.class);

    // The implementation for each of these services is resolved by the OSGi Service Manager
    private volatile NetworkingProviderManager networkingProviderManager;
    private volatile OvsdbConfigService ovsdbConfigService;
    private volatile IConnectionServiceInternal connectionService;
    private volatile INeutronNetworkCRUD neutronNetworkCache;
    private volatile INeutronPortCRUD neutronPortCache;
    private volatile VlanConfigurationCache vlanConfigurationCache;

    public TenantNetworkManagerImpl() {
    }

    @Override
    public int getVlan(Node node, String networkId) {
        Integer vlan = vlanConfigurationCache.getInternalVlan(node, networkId);
        if (vlan == null) return 0;
        return vlan;
    }

    @Override
    public void reclaimVlan(Node node, String portUUID, NeutronNetwork network) {
        int vlan = vlanConfigurationCache.reclaimInternalVlan(node, network.getID());
        if (vlan <= 0) {
            logger.error("Unable to get an internalVlan for Network {}", network);
            return;
        }
        logger.debug("Removed Vlan {} on {}", vlan, portUUID);
    }

    @Override
    public void programVlan(Node node, String portUUID, NeutronNetwork network) {
        Preconditions.checkNotNull(ovsdbConfigService);

        int vlan = vlanConfigurationCache.getInternalVlan(node, network.getID());
        logger.debug("Programming Vlan {} on {}", vlan, portUUID);
        if (vlan <= 0) {
            logger.error("Unable to get an internalVlan for Network {}", network);
            return;
        }

        Port port = ovsdbConfigService.createTypedRow(node, Port.class);
        OvsdbSet<Long> tags = new OvsdbSet<>();
        tags.add((long) vlan);
        port.setTag(tags);
        ovsdbConfigService.updateRow(node, port.getSchema().getName(), null, portUUID, port.getRow());
    }

    @Override
    public boolean isTenantNetworkPresentInNode(Node node, String segmentationId) {
        Preconditions.checkNotNull(ovsdbConfigService);

        String networkId = this.getNetworkId(segmentationId);
        if (networkId == null) {
            logger.debug("Tenant Network not found with Segmenation-id {}",segmentationId);
            return false;
        }
        if (networkingProviderManager.getProvider(node).hasPerTenantTunneling()) {
            int internalVlan = vlanConfigurationCache.getInternalVlan(node, networkId);
            if (internalVlan == 0) {
                logger.debug("No InternalVlan provisioned for Tenant Network {}",networkId);
                return false;
            }
        }

        try {
            /*
            // Vlan Tag based identification
            Map<String, Row> portTable = ovsdbConfigService.getRows(node, Port.NAME.getName());
            if (portTable == null) {
                logger.debug("Port table is null for Node {} ", node);
                return false;
            }

            for (Row row : portTable.values()) {
                Port port = (Port)row;
                Set<BigInteger> tags = port.getTag();
                if (tags.contains(internalVlan)) {
                    logger.debug("Tenant Network {} with Segmenation-id {} is present in Node {} / Port {}",
                                  networkId, segmentationId, node, port);
                    return true;
                }
            }
             */
            // External-id based more accurate VM Location identification
            Map<String, Row> ifTable = ovsdbConfigService.getRows(node, ovsdbConfigService.getTableName(node, Interface.class));
            if (ifTable == null) {
                logger.debug("Interface table is null for Node {} ", node);
                return false;
            }

            for (Row row : ifTable.values()) {
                Interface intf = ovsdbConfigService.getTypedRow(node, Interface.class, row);
                Map<String, String> externalIds = intf.getExternalIdsColumn().getData();
                if (externalIds != null && externalIds.get(Constants.EXTERNAL_ID_INTERFACE_ID) != null) {
                    if (this.isInterfacePresentInTenantNetwork(externalIds.get(Constants.EXTERNAL_ID_INTERFACE_ID), networkId)) {
                        logger.debug("Tenant Network {} with Segmentation-id {} is present in Node {} / Interface {}",
                                      networkId, segmentationId, node, intf);
                        return true;
                    }
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
    public NeutronNetwork getTenantNetwork(Interface intf) {
        logger.trace("getTenantNetwork for {}", intf);
        if (intf == null) return null;
        Map<String, String> externalIds = intf.getExternalIdsColumn().getData();
        logger.trace("externalIds {}", externalIds);
        if (externalIds == null) return null;
        String neutronPortId = externalIds.get(Constants.EXTERNAL_ID_INTERFACE_ID);
        if (neutronPortId == null) return null;
        NeutronPort neutronPort = neutronPortCache.getPort(neutronPortId);
        logger.trace("neutronPort {}", neutronPort);
        if (neutronPort == null) return null;
        NeutronNetwork neutronNetwork = neutronNetworkCache.getNetwork(neutronPort.getNetworkUUID());
        logger.debug("{} mapped to {}", intf, neutronNetwork);
        return neutronNetwork;
    }

    @Override
    public void networkCreated (String networkId) {
        List<Node> nodes = connectionService.getNodes();

        for (Node node : nodes) {
            this.networkCreated(node, networkId);
        }

    }

    @Override
    public int networkCreated (Node node, String networkId) {
        return vlanConfigurationCache.assignInternalVlan(node, networkId);
    }

    @Override
    public void networkDeleted(String id) {
        //ToDo: Delete? This method does nothing how we dropped container support...
    }

    private boolean isInterfacePresentInTenantNetwork (String portId, String networkId) {
        NeutronPort neutronPort = neutronPortCache.getPort(portId);
        return neutronPort != null && neutronPort.getNetworkUUID().equalsIgnoreCase(networkId);
    }

}
