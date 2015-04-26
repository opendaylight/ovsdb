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
import org.opendaylight.ovsdb.lib.notation.OvsdbSet;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.openstack.netvirt.api.Constants;
import org.opendaylight.ovsdb.openstack.netvirt.api.MdsalConsumer;
import org.opendaylight.ovsdb.openstack.netvirt.api.OvsdbConfigurationService;
import org.opendaylight.ovsdb.openstack.netvirt.api.OvsdbConnectionService;
import org.opendaylight.ovsdb.openstack.netvirt.api.TenantNetworkManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.VlanConfigurationCache;
import org.opendaylight.ovsdb.schema.openvswitch.Interface;
import org.opendaylight.ovsdb.schema.openvswitch.Port;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceExternalIds;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class TenantNetworkManagerImpl implements TenantNetworkManager {
    static final Logger logger = LoggerFactory.getLogger(TenantNetworkManagerImpl.class);

    // The implementation for each of these services is resolved by the OSGi Service Manager
    private volatile OvsdbConfigurationService ovsdbConfigurationService;
    private volatile OvsdbConnectionService connectionService;
    private volatile MdsalConsumer mdsalConsumer;
    private volatile INeutronNetworkCRUD neutronNetworkCache;
    private volatile INeutronPortCRUD neutronPortCache;
    private volatile VlanConfigurationCache vlanConfigurationCache;

    public TenantNetworkManagerImpl() {
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
    public void programInternalVlan(Node node, String portUUID, NeutronNetwork network) {
        /* TODO SB_MIGRATION */
        Preconditions.checkNotNull(ovsdbConfigurationService);

        int vlan = vlanConfigurationCache.getInternalVlan(node, network.getID());
        logger.debug("Programming Vlan {} on {}", vlan, portUUID);
        if (vlan <= 0) {
            logger.debug("Unable to get an internalVlan for Network {}", network);
            return;
        }

        Port port = ovsdbConfigurationService.createTypedRow(node, Port.class);
        OvsdbSet<Long> tags = new OvsdbSet<>();
        tags.add((long) vlan);
        port.setTag(tags);
        ovsdbConfigurationService.updateRow(node, port.getSchema().getName(), null, portUUID, port.getRow());
    }

    @Override
    public boolean isTenantNetworkPresentInNode(Node node, String segmentationId) {
        /* TODO SB_MIGRATION */
        Preconditions.checkNotNull(ovsdbConfigurationService);

        String networkId = this.getNetworkId(segmentationId);
        if (networkId == null) {
            logger.debug("Tenant Network not found with Segmenation-id {}",segmentationId);
            return false;
        }

        try {
            /* TODO SB_MIGRATION this code was already commented out
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
             */ //TODO SB_MIGRATION this code was already commented out
            // External-id based more accurate VM Location identification
            Map<String, Row> ifTable = ovsdbConfigurationService.getRows(node, ovsdbConfigurationService.getTableName(node, Interface.class));
            if (ifTable == null) {
                logger.debug("Interface table is null for Node {} ", node);
                return false;
            }

            for (Row row : ifTable.values()) {
                Interface intf = ovsdbConfigurationService.getTypedRow(node, Interface.class, row);
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
    public NeutronNetwork getTenantNetwork(OvsdbTerminationPointAugmentation terminationPointAugmentation) {
        NeutronNetwork neutronNetwork = null;
        logger.trace("getTenantNetwork for {}", terminationPointAugmentation.getName());
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
            logger.warn("getTenantPort did not find network for {}", terminationPointAugmentation.getName());
        }
        return neutronNetwork;
    }

    @Override
    public NeutronPort getTenantPort(OvsdbTerminationPointAugmentation terminationPointAugmentation) {
        NeutronPort neutronPort = null;
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
        //ToDo: Delete? This method does nothing how we dropped container support...
    }

    private boolean isInterfacePresentInTenantNetwork (String portId, String networkId) {
        NeutronPort neutronPort = neutronPortCache.getPort(portId);
        return neutronPort != null && neutronPort.getNetworkUUID().equalsIgnoreCase(networkId);
    }

}
