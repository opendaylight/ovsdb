/*
 * Copyright (c) 2013, 2015 Hewlett-Packard Development Company, L.P. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.impl;

import org.opendaylight.ovsdb.openstack.netvirt.ConfigInterface;
import org.opendaylight.ovsdb.openstack.netvirt.NodeConfiguration;
import org.opendaylight.ovsdb.openstack.netvirt.api.Southbound;
import org.opendaylight.ovsdb.openstack.netvirt.api.TenantNetworkManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.VlanConfigurationCache;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Dave Tucker
 * @author Sam Hague
 */
public class VlanConfigurationCacheImpl implements ConfigInterface, VlanConfigurationCache {
    private static final Logger LOG = LoggerFactory.getLogger(VlanConfigurationCacheImpl.class);
    private Map<String, NodeConfiguration> configurationCache = Maps.newConcurrentMap();
    private volatile TenantNetworkManager tenantNetworkManager;
    private volatile Southbound southbound;

    private NodeConfiguration getNodeConfiguration(Node node){
        String nodeUuid = getNodeUUID(node);
        if (configurationCache.get(nodeUuid) != null) {
            return configurationCache.get(nodeUuid);
        }

        // Cache miss
        initializeNodeConfiguration(node, nodeUuid);

        return configurationCache.get(nodeUuid);
    }

    private String getNodeUUID(Node node) {
        return southbound.getOvsdbNodeUUID(node);
    }

    private void initializeNodeConfiguration(Node node, String nodeUuid) {
        NodeConfiguration nodeConfiguration = new NodeConfiguration();
        List<OvsdbTerminationPointAugmentation> ports = southbound.getTerminationPointsOfBridge(node);
        for (OvsdbTerminationPointAugmentation port : ports) {
            Integer vlan = port.getVlanTag().getValue();
            String networkId = tenantNetworkManager.getTenantNetwork(port).getNetworkUUID();
            if (vlan != 0 && networkId != null) {
                internalVlanInUse(nodeConfiguration, vlan);
                nodeConfiguration.getTenantVlanMap().put(networkId, vlan);
            } else {
                LOG.debug("Node: {} initialized without a vlan", node);
            }
        }
        configurationCache.put(nodeUuid, nodeConfiguration);
    }

    /**
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

    /**
     * Return the mapped internal vlan to the available pool.
     */
    @Override
    public Integer reclaimInternalVlan(Node node, String networkId) {
        NodeConfiguration nodeConfiguration = getNodeConfiguration(node);
        Integer mappedVlan = nodeConfiguration.getTenantVlanMap().get(networkId);
        if (mappedVlan != null) {
            nodeConfiguration.getTenantVlanMap().remove(networkId);
            nodeConfiguration.getInternalVlans().add(mappedVlan);
            return mappedVlan;
        }
        return 0;
    }

    private void internalVlanInUse(NodeConfiguration nodeConfiguration, Integer vlan) {
        nodeConfiguration.getInternalVlans().remove(vlan);
    }

    @Override
    public Integer getInternalVlan(Node node, String networkId) {
        NodeConfiguration nodeConfiguration = getNodeConfiguration(node);
        Integer vlan = nodeConfiguration.getTenantVlanMap().get(networkId);
        return vlan == null ? 0 : vlan;
    }

    @Override
    public void setDependencies(BundleContext bundleContext, ServiceReference serviceReference) {
        tenantNetworkManager =
                (TenantNetworkManager) ServiceHelper.getGlobalInstance(TenantNetworkManager.class, this);
        southbound =
                (Southbound) ServiceHelper.getGlobalInstance(Southbound.class, this);
    }

    @Override
    public void setDependencies(Object impl) {

    }
}
