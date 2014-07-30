/*
 * Copyright (c) 2013 Hewlett-Packard Development Company, L.P.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors: Dave Tucker
*/

package org.opendaylight.ovsdb.openstack.netvirt.impl;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.openstack.netvirt.NodeConfiguration;
import org.opendaylight.ovsdb.openstack.netvirt.api.TenantNetworkManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.VlanConfigurationCache;
import org.opendaylight.ovsdb.plugin.api.OvsdbConfigurationService;
import org.opendaylight.ovsdb.schema.openvswitch.Interface;
import org.opendaylight.ovsdb.schema.openvswitch.OpenVSwitch;
import org.opendaylight.ovsdb.schema.openvswitch.Port;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

public class VlanConfigurationCacheImpl implements VlanConfigurationCache {
    static final Logger logger = LoggerFactory.getLogger(VlanConfigurationCacheImpl.class);

    private Map<String, NodeConfiguration> configurationCache = Maps.newConcurrentMap();

    private volatile TenantNetworkManager tenantNetworkManager;
    private volatile OvsdbConfigurationService ovsdbConfigurationService;

    private NodeConfiguration getNodeConfiguration(Node node){
        String nodeUuid = getNodeUUID(node);
        if (configurationCache.get(nodeUuid) != null) {
            return configurationCache.get(nodeUuid);
        }

        // Cache miss
        initializeNodeConfiguration(nodeUuid, node);

        return configurationCache.get(nodeUuid);
    }

    private String getNodeUUID(Node node) {
        Preconditions.checkNotNull(ovsdbConfigurationService);
        String nodeUuid = new String();
        try {
            Map<String, Row> ovsTable = ovsdbConfigurationService.getRows(node, ovsdbConfigurationService.getTableName(node, OpenVSwitch.class));
            nodeUuid = (String)ovsTable.keySet().toArray()[0];
        }
        catch (Exception e) {
            logger.error("Unable to get the Open_vSwitch table for Node {}: {}", node, e);
        }

        return nodeUuid;
    }

    private void initializeNodeConfiguration(String nodeUuid, Node node) {

        NodeConfiguration nodeConfiguration = new NodeConfiguration();
        Integer vlan;
        String networkId = null;

        try {
            Map<String, Row> portRows = ovsdbConfigurationService.getRows(node, ovsdbConfigurationService.getTableName(node, Port.class));

            if (portRows == null){
                logger.debug("Port table is null for Node {}", node);
                return;
            }

            for (Row row : portRows.values()) {
                Port port = ovsdbConfigurationService.getTypedRow(node, Port.class, row);

                if (port.getTagColumn() == null) continue;
                Set<Long> tags = port.getTagColumn().getData();
                if (tags.size() == 1)
                {
                    //There is only one tag here
                    vlan = tags.iterator().next().intValue();
                }
                else {
                   logger.debug("This port ({}) has {} tags", port.getName(), tags.size());
                   continue;
                }

                for (UUID ifaceId : port.getInterfacesColumn().getData()) {
                    Row ifaceRow = ovsdbConfigurationService
                            .getRow(node, ovsdbConfigurationService.getTableName(node, Interface.class),
                                    ifaceId.toString());
                    Interface iface = ovsdbConfigurationService.getTypedRow(node, Interface.class, ifaceRow);

                    if (iface == null) {
                        logger.debug("Interface table is null");
                        continue;
                    }

                    networkId = tenantNetworkManager.getTenantNetwork(iface).getNetworkUUID();

                    if (networkId != null) break;
                }

                if (vlan != 0 && networkId != null) {

                    this.internalVlanInUse(nodeConfiguration, vlan);
                    nodeConfiguration.getTenantVlanMap().put(networkId, vlan);

                } else {
                    logger.debug("Node: {} initialized without a vlan", node);
                }
            }

            configurationCache.put(nodeUuid, nodeConfiguration);
        }
        catch (Exception e) {
            logger.debug("Error getting Port table for Node {}: {}", node, e);
        }
    }

    /*
     * Return the currently mapped internal vlan or get the next
     * free internal vlan from the available pool and map it to the networkId.
     */
    @Override
    public Integer assignInternalVlan (Node node, String networkId) {
        NodeConfiguration nodeConfiguration = getNodeConfiguration(node);
        Integer mappedVlan = nodeConfiguration.getTenantVlanMap().get(networkId);
        if (mappedVlan != null) {
            return mappedVlan;
        }
        mappedVlan = nodeConfiguration.getInternalVlans().poll();
        if (mappedVlan != null) {
            nodeConfiguration.getTenantVlanMap().put(networkId, mappedVlan);
        }
        return mappedVlan;
    }

    /*
     * Return the mapped internal vlan to the available pool.
     */
    @Override
    public Integer reclaimInternalVlan (Node node, String networkId) {
        NodeConfiguration nodeConfiguration = getNodeConfiguration(node);
        Integer mappedVlan = nodeConfiguration.getTenantVlanMap().get(networkId);
        if (mappedVlan != null) {
            nodeConfiguration.getTenantVlanMap().remove(networkId);
            nodeConfiguration.getInternalVlans().add(mappedVlan);
            return mappedVlan;
        }
        return 0;
    }

    private void internalVlanInUse (NodeConfiguration nodeConfiguration, Integer vlan) {
        nodeConfiguration.getInternalVlans().remove(vlan);
    }

    @Override
    public Integer getInternalVlan (Node node, String networkId) {
        NodeConfiguration nodeConfiguration = getNodeConfiguration(node);
        Integer vlan = nodeConfiguration.getTenantVlanMap().get(networkId);
        if (vlan == null) return 0;
        return vlan;
    }

}
