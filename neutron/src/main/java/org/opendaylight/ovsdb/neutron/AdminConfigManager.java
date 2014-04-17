/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Brent Salisbury, Sam Hague
 */
package org.opendaylight.ovsdb.neutron;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.ovsdb.lib.table.Open_vSwitch;
import org.opendaylight.ovsdb.lib.table.internal.Table;
import org.opendaylight.ovsdb.plugin.OVSDBConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdminConfigManager implements IAdminConfigManager{
    static final Logger logger = LoggerFactory.getLogger(AdminConfigManager.class);

    private String integrationBridgeName;
    private String networkBridgeName;
    private String externalBridgeName;
    private String tunnelEndpointConfigName;
    private String patchToIntegration;
    private String patchToNetwork;
    private String providerMappingsConfigName;
    private String providerMappings;

    // Refer to /etc/quantum/plugins/openvswitch/ovs_quantum_plugin.ini
    private static String DEFAULT_TUNNEL_ENDPOINT_CONFIG_STRING = "local_ip";
    private static String DEFAULT_INTEGRATION_BRIDGENAME = "br-int";
    private static String DEFAULT_NETWORK_BRIDGENAME = "br-net";
    private static String DEFAULT_EXTERNAL_BRIDGENAME = "br-ex";
    private static String DEFAULT_PATCH_TO_INTEGRATION = "patch-int";
    private static String DEFAULT_PATCH_TO_NETWORK = "patch-net";
    private static String CONFIG_TUNNEL_ENDPOINT_CONFIG = "tunnel_endpoint_config_string";
    private static String CONFIG_INTEGRATION_BRIDGENAME = "integration_bridge";
    private static String CONFIG_NETWORK_BRIDGENAME = "network_bridge";
    private static String CONFIG_EXTERNAL_BRIDGENAME = "external_bridge";
    private static String CONFIG_PATCH_TO_INTEGRATION = "patch-int";
    private static String CONFIG_PATCH_TO_NETWORK = "patch-net";
    private static String DEFAULT_PROVIDER_MAPPINGS_CONFIG_STRING = "provider_mappings";
    private static String CONFIG_PROVIDER_MAPPINGS_CONFIG = "provider_mappings_config_string";
    private static String CONFIG_PROVIDER_MAPPINGS = "provider_mappings";

    public AdminConfigManager() {
        tunnelEndpointConfigName = System.getProperty(CONFIG_TUNNEL_ENDPOINT_CONFIG);
        integrationBridgeName = System.getProperty(CONFIG_INTEGRATION_BRIDGENAME);
        networkBridgeName = System.getProperty(CONFIG_NETWORK_BRIDGENAME);
        externalBridgeName = System.getProperty(CONFIG_EXTERNAL_BRIDGENAME);
        patchToIntegration = System.getProperty(CONFIG_PATCH_TO_INTEGRATION);
        patchToNetwork = System.getProperty(CONFIG_PATCH_TO_NETWORK);
        providerMappingsConfigName = System.getProperty(CONFIG_PROVIDER_MAPPINGS_CONFIG);
        providerMappings = System.getProperty(CONFIG_PROVIDER_MAPPINGS);

        if (tunnelEndpointConfigName == null) tunnelEndpointConfigName = DEFAULT_TUNNEL_ENDPOINT_CONFIG_STRING;
        if (integrationBridgeName == null) integrationBridgeName = DEFAULT_INTEGRATION_BRIDGENAME;
        if (networkBridgeName == null) networkBridgeName = DEFAULT_NETWORK_BRIDGENAME;
        if (externalBridgeName == null) externalBridgeName = DEFAULT_EXTERNAL_BRIDGENAME;
        if (patchToIntegration == null) patchToIntegration = DEFAULT_PATCH_TO_INTEGRATION;
        if (patchToNetwork == null) patchToNetwork  = DEFAULT_PATCH_TO_NETWORK;
        if (providerMappingsConfigName == null) providerMappingsConfigName = DEFAULT_PROVIDER_MAPPINGS_CONFIG_STRING;
    }

    public String getIntegrationBridgeName() {
        return integrationBridgeName;
    }

    public void setIntegrationBridgeName(String integrationBridgeName) {
        this.integrationBridgeName = integrationBridgeName;
    }

    public String getNetworkBridgeName() { return networkBridgeName; }

    public void setNetworkBridgeName(String networkBridgeName) {
        this.networkBridgeName = networkBridgeName;
    }

    public String getExternalBridgeName() {
        return externalBridgeName;
    }

    public void setExternalBridgeName (String externalBridgeName) {
        this.externalBridgeName = externalBridgeName;
    }

    public String getPatchToIntegration() {
        return patchToIntegration;
    }

    public void setPatchToIntegration(String patchToIntegration) {
        this.patchToIntegration = patchToIntegration;
    }

    public String getPatchToNetwork() { return patchToNetwork; }

    public void setPatchToNetwork(String patchToNetwork) {
        this.patchToNetwork = patchToNetwork;
    }

    public InetAddress getTunnelEndPoint(Node node) {
        InetAddress address = null;
        OVSDBConfigService ovsdbConfig = (OVSDBConfigService)ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);
        try {
            Map<String, Table<?>> ovsTable = ovsdbConfig.getRows(node, Open_vSwitch.NAME.getName());

            if (ovsTable == null) {
                logger.error("Open_vSwitch table is null for Node {} ", node);
                return null;
            }

            // While there is only one entry in the HashMap, we can't access it by index...
            for (Table<?> row : ovsTable.values()) {
                Open_vSwitch ovsRow = (Open_vSwitch)row;
                Map<String, String> configs = ovsRow.getOther_config();

                if (configs == null) {
                    logger.debug("Open_vSwitch table is null for Node {} ", node);
                    continue;
                }

                String tunnelEndpoint = configs.get(tunnelEndpointConfigName);

                if (tunnelEndpoint == null) {
                    continue;
                }

                address = InetAddress.getByName(tunnelEndpoint);
                logger.debug("Tunnel Endpoint for Node {} {}", node, address.getHostAddress());
                break;
            }
        }
        catch (Exception e) {
            logger.error("Error populating Tunnel Endpoint for Node {} ", node, e);
        }

        return address;
    }


    /*
     * Return the physical interface mapped to the given neutron physical network.
     * Provider mappings will be of the following format:
     * provider_mappings=physnet1:eth1[,physnet2:eth2]
     */
      public String getPhysicalInterfaceName (Node node, String physicalNetwork) {
        String phyIf = null;

        try {
            OVSDBConfigService ovsdbConfig = (OVSDBConfigService) ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);
            Map<String, Table<?>> ovsTable = ovsdbConfig.getRows(node, Open_vSwitch.NAME.getName());

            if (ovsTable == null) {
                logger.error("Open_vSwitch table is null for Node {} ", node);
                return null;
            }

            // Loop through all the Open_vSwitch rows looking for the first occurrence of other_config.
            // The specification does not restrict the number of rows so we choose the first we find.
            for (Table<?> row : ovsTable.values()) {
                String providerMaps;
                Open_vSwitch ovsRow = (Open_vSwitch) row;
                Map<String, String> configs = ovsRow.getOther_config();

                if (configs == null) {
                    logger.debug("Open_vSwitch table is null for Node {} ", node);
                    continue;
                }

                providerMaps = configs.get(providerMappingsConfigName);
                if (providerMaps == null) {
                    providerMaps = providerMappings;
                }

                if (providerMaps != null) {
                    for (String map : providerMaps.split(",")) {
                        String[] pair = map.split(":");
                        if (pair[0].equals(physicalNetwork)) {
                            phyIf = pair[1];
                            break;
                        }
                    }
                }
                break;
            }
        } catch (Exception e) {
            logger.error("Unable to find physical interface for Node: {}, Network {}",
                    node, physicalNetwork, e);
        }

        if (phyIf == null) {
            logger.error("Physical interface not found for Node: {}, Network {}",
                    node, physicalNetwork);
        }

        return phyIf;
    }

    /* Return all physical interfaces configure in bridge mapping
     * Bridge mappings will be of the following format:
     * bridge_mappings=physnet1:eth1,physnet2:eth2
     * Method will return list = {eth1, eth2}
     */
    public List<String> getAllPhysicalInterfaceNames(Node node) {
        List<String> phyIfName = new ArrayList<String>();

        try {
            OVSDBConfigService ovsdbConfig = (OVSDBConfigService) ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);
            Map<String, Table<?>> ovsTable = ovsdbConfig.getRows(node, Open_vSwitch.NAME.getName());

            if (ovsTable == null) {
                logger.error("Open_vSwitch table is null for Node {} ", node);
                return null;
            }

            // While there is only one entry in the HashMap, we can't access it by index...
            for (Table<?> row : ovsTable.values()) {
                String bridgeMaps;
                Open_vSwitch ovsRow = (Open_vSwitch) row;
                Map<String, String> configs = ovsRow.getOther_config();

                if (configs == null) {
                    logger.debug("Open_vSwitch table is null for Node {} ", node);
                    continue;
                }

                bridgeMaps = configs.get(providerMappingsConfigName);
                if (bridgeMaps == null) {
                    bridgeMaps = providerMappings;
                }

                if (bridgeMaps != null) {
                    for (String map : bridgeMaps.split(",")) {
                        String[] pair = map.split(":");
                        phyIfName.add(pair[1]);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Unable to find physical interface for Node: {}",
                    node, e);
        }

        logger.debug("Physical interface for Node: {}, If: {}",
                node, phyIfName);

        return phyIfName;
    }

    public boolean isInterested (String tableName) {
        return tableName.equalsIgnoreCase(Open_vSwitch.NAME.getName());
    }

}
