package org.opendaylight.ovsdb.neutron;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.ovsdb.lib.table.Open_vSwitch;
import org.opendaylight.ovsdb.lib.table.internal.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdminConfigManager {
    static final Logger logger = LoggerFactory.getLogger(AdminConfigManager.class);

    private String integrationBridgeName;
    private String tunnelBridgeName;
    private String externalBridgeName;
    private String tunnelEndpointConfigName;
    private Map<Node, InetAddress> tunnelEndpoints;

    // Refer to /etc/quantum/plugins/openvswitch/ovs_quantum_plugin.ini
    private static String DEFAULT_TUNNEL_ENDPOINT_CONFIG_STRING = "local_ip";
    private static String DEFAULT_INTEGRATION_BRIDGENAME = "br-int";
    private static String DEFAULT_TUNNEL_BRIDGENAME = "br-tun";
    private static String DEFAULT_EXTERNAL_BRIDGENAME = "br-ex";
    private static String CONFIG_TUNNEL_ENDPOINT_CONFIG = "tunnel_endpoint_config_string";
    private static String CONFIG_INTEGRATION_BRIDGENAME = "integration_bridge";
    private static String CONFIG_TUNNEL_BRIDGENAME = "tunnel_bridge";
    private static String CONFIG_EXTERNAL_BRIDGENAME = "external_bridge";

    private static AdminConfigManager adminConfiguration = new AdminConfigManager();

    private AdminConfigManager() {
        tunnelEndpoints = new HashMap<Node, InetAddress>();
        tunnelEndpointConfigName = System.getProperty(CONFIG_TUNNEL_ENDPOINT_CONFIG);
        integrationBridgeName = System.getProperty(CONFIG_INTEGRATION_BRIDGENAME);
        tunnelBridgeName = System.getProperty(CONFIG_TUNNEL_BRIDGENAME);
        externalBridgeName = System.getProperty(CONFIG_EXTERNAL_BRIDGENAME);

        if (tunnelEndpointConfigName == null) tunnelEndpointConfigName = DEFAULT_TUNNEL_ENDPOINT_CONFIG_STRING;
        if (integrationBridgeName == null) integrationBridgeName = DEFAULT_INTEGRATION_BRIDGENAME;
        if (tunnelBridgeName == null) tunnelBridgeName = DEFAULT_TUNNEL_BRIDGENAME;
        if (externalBridgeName == null) externalBridgeName = DEFAULT_EXTERNAL_BRIDGENAME;
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

    public InetAddress getTunnelEndPoint(Node node) {
        return tunnelEndpoints.get(node);
    }

    public void addTunnelEndpoint (Node node, InetAddress address) {
        tunnelEndpoints.put(node, address);
    }

    public String getTunnelEndpointConfigTable() {
        return "Open_vSwitch";
    }

    public void populateTunnelEndpoint (Node node, String tableName, Table<?> row) {
        try {
            if (tableName.equalsIgnoreCase(getTunnelEndpointConfigTable())) {
                Map<String, String> configs = ((Open_vSwitch) row).getOther_config();
                if (configs != null) {
                    String tunnelEndpoint = configs.get(tunnelEndpointConfigName);
                    if (tunnelEndpoint != null) {
                        try {
                            InetAddress address = InetAddress.getByName(tunnelEndpoint);
                            addTunnelEndpoint(node, address);
                            logger.debug("Tunnel Endpoint for Node {} {}", node, address.getHostAddress());
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error populating Tunnel Endpoint for Node {} ", node, e);
        }
    }
}