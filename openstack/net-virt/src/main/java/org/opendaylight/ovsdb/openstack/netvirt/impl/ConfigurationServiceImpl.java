/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Brent Salisbury, Sam Hague, Dave Tucker
 */
package org.opendaylight.ovsdb.openstack.netvirt.impl;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.Version;
import org.opendaylight.ovsdb.openstack.netvirt.api.Constants;
import org.opendaylight.ovsdb.plugin.api.OvsdbConfigurationService;
import org.opendaylight.ovsdb.schema.openvswitch.OpenVSwitch;

import com.google.common.collect.Maps;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.Map;
import java.util.Set;

public class ConfigurationServiceImpl implements org.opendaylight.ovsdb.openstack.netvirt.api.ConfigurationService {
    static final Logger logger = LoggerFactory.getLogger(ConfigurationServiceImpl.class);

    private volatile OvsdbConfigurationService ovsdbConfigurationService;

    private String integrationBridgeName;
    private String networkBridgeName;
    private String externalBridgeName;
    private String tunnelEndpointKey;

    private Map<Pair<String, String>, String> patchPortNames = Maps.newHashMap();
    private String providerMappingsKey;
    private String providerMapping;

    public ConfigurationServiceImpl() {
        tunnelEndpointKey = Constants.TUNNEL_ENDPOINT_KEY;
        integrationBridgeName = Constants.INTEGRATION_BRIDGE;
        networkBridgeName = Constants.NETWORK_BRIDGE;
        externalBridgeName = Constants.EXTERNAL_BRIDGE;
        patchPortNames.put(new ImmutablePair<>(integrationBridgeName, networkBridgeName),
                           Constants.PATCH_PORT_TO_NETWORK_BRIDGE_NAME);
        patchPortNames.put(new ImmutablePair<>(networkBridgeName, integrationBridgeName),
                           Constants.PATCH_PORT_TO_INTEGRATION_BRIDGE_NAME);
        providerMappingsKey = Constants.PROVIDER_MAPPINGS_KEY;
        providerMapping = Constants.PROVIDER_MAPPING;
    }

    @Override
    public String getIntegrationBridgeName() {
        return integrationBridgeName;
    }

    @Override
    public void setIntegrationBridgeName(String integrationBridgeName) {
        this.integrationBridgeName = integrationBridgeName;
    }

    @Override
    public String getNetworkBridgeName() {
        return networkBridgeName;
    }

    @Override
    public void setNetworkBridgeName(String networkBridgeName) {
        this.networkBridgeName = networkBridgeName;
    }

    @Override
    public String getExternalBridgeName() {
        return externalBridgeName;
    }

    @Override
    public void setExternalBridgeName(String externalBridgeName) {
        this.externalBridgeName = externalBridgeName;
    }

    @Override
    public String getTunnelEndpointKey() {
        return tunnelEndpointKey;
    }

    @Override
    public void setTunnelEndpointKey(String tunnelEndpointKey) {
        this.tunnelEndpointKey = tunnelEndpointKey;
    }

    @Override
    public String getProviderMappingsKey() {
        return providerMappingsKey;
    }

    @Override
    public void setProviderMappingsKey(String providerMappingsKey) {
        this.providerMappingsKey = providerMappingsKey;
    }

    @Override
    public Map<Pair<String, String>, String> getPatchPortNames() {
        return patchPortNames;
    }

    @Override
    public void setPatchPortNames(Map<Pair<String, String>, String> patchPortNames) {
        this.patchPortNames = patchPortNames;
    }

    public String getPatchPortName(Pair portTuple){
        return this.patchPortNames.get(portTuple);
    }

    @Override
    public String getDefaultProviderMapping() {
        return providerMapping;
    }

    @Override
    public void setDefaultProviderMapping(String providerMapping) {
        this.providerMapping = providerMapping;
    }

    @Override
    public InetAddress getTunnelEndPoint(Node node) {
        InetAddress address = null;
        try {
            Map<String, Row> ovsTable = ovsdbConfigurationService.getRows(node,
                    ovsdbConfigurationService.getTableName(node, OpenVSwitch.class));

            if (ovsTable == null) {
                logger.error("OpenVSwitch table is null for Node {} ", node);
                return null;
            }

            // While there is only one entry in the HashMap, we can't access it by index...
            for (Row row : ovsTable.values()) {
                OpenVSwitch ovsRow = ovsdbConfigurationService.getTypedRow(node, OpenVSwitch.class, row);
                Map<String, String> configs = ovsRow.getOtherConfigColumn().getData();

                if (configs == null) {
                    logger.debug("OpenVSwitch table is null for Node {} ", node);
                    continue;
                }

                String tunnelEndpoint = configs.get(tunnelEndpointKey);

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

    @Override
    public String getOpenflowVersion(Node node) {

        String configuredVersion = System.getProperty("ovsdb.of.version");
        if (configuredVersion != null){
            switch (configuredVersion){
                case "1.0":
                    return Constants.OPENFLOW10;
                case "1.3":
                    //fall through
                default:
                    return Constants.OPENFLOW13;

            }
        }

        Map<String, Row> ovsRows = ovsdbConfigurationService.getRows(node,
                ovsdbConfigurationService.getTableName(node, OpenVSwitch.class));

        if (ovsRows == null) {
            logger.info("The OVS node {} has no Open_vSwitch rows", node.toString());
            return null;
        }

        Version ovsVersion = null;
        // While there is only one entry in the HashMap, we can't access it by index...
        for (Row row : ovsRows.values()) {
            OpenVSwitch ovsRow = ovsdbConfigurationService.getTypedRow(node, OpenVSwitch.class, row);
            Set<String> versionSet = ovsRow.getOvsVersionColumn().getData();
            if (versionSet != null && versionSet.iterator().hasNext()) {
                ovsVersion = Version.fromString(versionSet.iterator().next());
            }
        }

        if (ovsVersion == null || ovsVersion.compareTo(Constants.OPENFLOW13_SUPPORTED) < 0) {
            return Constants.OPENFLOW10;
        }

        return Constants.OPENFLOW13;
    }

    @Override
    public String getDefaultGatewayMacAddress(Node node) {
        final String l3gatewayForNode =
            node != null ? System.getProperty("ovsdb.l3gateway.mac." + node.getNodeIDString()) : null;
        return l3gatewayForNode != null ? l3gatewayForNode : System.getProperty("ovsdb.l3gateway.mac");
    }
}
