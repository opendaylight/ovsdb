/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Brent Salisbury
 */
package org.opendaylight.ovsdb.neutron;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.ovsdb.lib.table.Open_vSwitch;
import org.opendaylight.ovsdb.lib.table.internal.Table;
import org.opendaylight.ovsdb.plugin.OVSDBConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdminConfigManager {
    static final Logger logger = LoggerFactory.getLogger(AdminConfigManager.class);

    private String integrationBridgeName;
    private String tunnelBridgeName;
    private String externalBridgeName;
    private String tunnelEndpointConfigName;
    private String patchToIntegration;
    private String patchToTunnel;
    private Map<Node, InetAddress> tunnelEndpoints;

    // Refer to /etc/quantum/plugins/openvswitch/ovs_quantum_plugin.ini
    private static String DEFAULT_TUNNEL_ENDPOINT_CONFIG_STRING = "local_ip";
    private static String DEFAULT_INTEGRATION_BRIDGENAME = "br-int";
    private static String DEFAULT_TUNNEL_BRIDGENAME = "br-tun";
    private static String DEFAULT_EXTERNAL_BRIDGENAME = "br-ex";
    private static String DEFAULT_PATCH_TO_INTEGRATION = "patch-int";
    private static String DEFAULT_PATCH_TO_TUNNEL = "patch-tun";
    private static String CONFIG_TUNNEL_ENDPOINT_CONFIG = "tunnel_endpoint_config_string";
    private static String CONFIG_INTEGRATION_BRIDGENAME = "integration_bridge";
    private static String CONFIG_TUNNEL_BRIDGENAME = "tunnel_bridge";
    private static String CONFIG_EXTERNAL_BRIDGENAME = "external_bridge";
    private static String CONFIG_PATCH_TO_INTEGRATION = "patch-int";
    private static String CONFIG_PATCH_TO_TUNNEL = "patch-tun";

    private static AdminConfigManager adminConfiguration = new AdminConfigManager();

    private AdminConfigManager() {
        tunnelEndpoints = new HashMap<Node, InetAddress>();
        tunnelEndpointConfigName = System.getProperty(CONFIG_TUNNEL_ENDPOINT_CONFIG);
        integrationBridgeName = System.getProperty(CONFIG_INTEGRATION_BRIDGENAME);
        tunnelBridgeName = System.getProperty(CONFIG_TUNNEL_BRIDGENAME);
        externalBridgeName = System.getProperty(CONFIG_EXTERNAL_BRIDGENAME);
        patchToIntegration = System.getProperty(CONFIG_PATCH_TO_INTEGRATION);
        patchToTunnel = System.getProperty(CONFIG_PATCH_TO_TUNNEL);

        if (tunnelEndpointConfigName == null) tunnelEndpointConfigName = DEFAULT_TUNNEL_ENDPOINT_CONFIG_STRING;
        if (integrationBridgeName == null) integrationBridgeName = DEFAULT_INTEGRATION_BRIDGENAME;
        if (tunnelBridgeName == null) tunnelBridgeName = DEFAULT_TUNNEL_BRIDGENAME;
        if (externalBridgeName == null) externalBridgeName = DEFAULT_EXTERNAL_BRIDGENAME;
        if (patchToIntegration == null) patchToIntegration = DEFAULT_PATCH_TO_INTEGRATION;
        if (patchToTunnel == null) patchToTunnel = DEFAULT_PATCH_TO_TUNNEL;
    }

    public static AdminConfigManager getManager() {
        return adminConfiguration;
    }

    public String getIntegrationBridgeName() {
        return integrationBridgeName;
    }

    public void setIntegrationBridgeName(String integrationBridgeName) {
        this.integrationBridgeName = integrationBridgeName;
    }

    public String getTunnelBridgeName() {
        return tunnelBridgeName;
    }

    public void setTunnelBridgeName(String tunnelBridgeName) {
        this.tunnelBridgeName = tunnelBridgeName;
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

    public String getPatchToTunnel() {
        return patchToTunnel;
    }

    public void setPatchToTunnel(String patchToTunnel) {
        this.patchToTunnel = patchToTunnel;
    }

    public InetAddress getTunnelEndPoint(Node node) {
        InetAddress address = null;
        OVSDBConfigService ovsdbConfig = (OVSDBConfigService)ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);
        try {
            Open_vSwitch ovsTable = (Open_vSwitch)ovsdbConfig.getRows(node, Open_vSwitch.NAME.getName());
            Map<String, String> configs = ovsTable.getOther_config();

            if (configs == null) {
                logger.debug("Open_vSwitch table is null for Node {} ", node);
                return null;
            }

            String tunnelEndpoint = configs.get(tunnelEndpointConfigName);
            address = InetAddress.getByName(tunnelEndpoint);
            logger.debug("Tunnel Endpoint for Node {} {}", node, address.getHostAddress());

        } catch (Exception e) {
            logger.error("Error populating Tunnel Endpoint for Node {} ", node, e);
        }
        return address;
    }

    public boolean isInterested (String tableName) {
        return tableName.equalsIgnoreCase(Open_vSwitch.NAME.getName());
    }

    private void populateTunnelEndpoint (Node node, Open_vSwitch row) {
        Map<String, String> configs = row.getOther_config();
        if (configs != null) {
            String tunnelEndpoint = configs.get(tunnelEndpointConfigName);
            if (tunnelEndpoint != null) {
                try {
                    InetAddress address = InetAddress.getByName(tunnelEndpoint);
                    logger.debug("Tunnel Endpoint for Node {} {}", node, address.getHostAddress());
                } catch (UnknownHostException e) {
                    logger.error("Unable to add tunnel endpoint for node " + node, e);
                }
            }
        }
    }

    public void populateTunnelEndpoint (Node node) {
        OVSDBConfigService ovsdbTable = (OVSDBConfigService)ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);
        try {
            Map<String, Table<?>> openvswitchTable = ovsdbTable.getRows(node, Open_vSwitch.NAME.getName());
            if (openvswitchTable == null) {
                logger.debug("Open_vSwitch table is null for Node {} ", node);
                return;
            }

            for (Table<?> row : openvswitchTable.values()) {
                populateTunnelEndpoint(node, (Open_vSwitch)row);
            }
        } catch (Exception e) {
            logger.error("Error populating Tunnel Endpoint for Node {} ", node, e);
        }
    }

    // Use this later if there is a need to update the tunnel-endpoint dynamically
    public void populateTunnelEndpoint (Node node, String tableName, Table<?> row) {
        OVSDBConfigService ovsdbTable = (OVSDBConfigService)ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);
        try {
            if (isInterested(tableName)) {
                populateTunnelEndpoint(node, (Open_vSwitch)row);
            }
        } catch (Exception e) {
            logger.error("Error populating Tunnel Endpoint for Node {} ", node, e);
        }
    }
}
