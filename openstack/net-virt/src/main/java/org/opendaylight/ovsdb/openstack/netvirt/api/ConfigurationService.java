/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 *  Authors : Dave Tucker
 */

package org.opendaylight.ovsdb.openstack.netvirt.api;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

import java.net.InetAddress;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;

/**
 * The ConfigurationService handles the configuration of the OpenStack Neutron Integration
 * It exposes a set of Configuration variables and helper functions for obtaining node-specific
 * configuration from the Open_vSwitch table of an OVS instance.
 */
public interface ConfigurationService {

    /**
     * Returns the name configured name of the Integration Bridge
     */
    String getIntegrationBridgeName();

    /**
     * Configures the name of the Integration Bridge
     */
    void setIntegrationBridgeName(String integrationBridgeName);

    /**
     * Returns the name configured name of the Network Bridge
     */
    String getNetworkBridgeName();

    /**
     * Configures the name of the Network Bridge
     */
    void setNetworkBridgeName(String networkBridgeName);

    /**
     * Returns the name configured name of the ExternalBridge
     */
    String getExternalBridgeName();

    /**
     * Configures the name of the External Bridge
     */
    void setExternalBridgeName(String externalBridgeName);

    /**
     * Returns the key used to access the Tunnel Endpoint configuration from Open vSwitch
     */
    String getTunnelEndpointKey();

    /**
     * Sets the key used to access the Tunnel Endpoint configuration from Open vSwitch
     */
    void setTunnelEndpointKey(String tunnelEndpointKey);

    /**
     * Returns a Map of patch port names where the key is a tuple of source bridge and destination bridge
     */
    Map<Pair<String, String>, String> getPatchPortNames();

    /**
     * Sets the Map of source/destination bridges to patch port name
     */
    void setPatchPortNames(Map<Pair<String, String>, String> patchPortNames);

    /**
     * Get the name of a patch port
     * @param portTuple a {@link org.apache.commons.lang3.tuple.Pair} where L
     *                  is the source bridge and R the destination bridge
     * @return the name of the patch port
     */
    String getPatchPortName(Pair portTuple);

    /**
     * Returns the key used to access the Tunnel Endpoint configuration from Open vSwitch
     */
    String getProviderMappingsKey();

    /**
     * Sets the key used to access the Tunnel Endpoint configuration from Open vSwitch
     */
    void setProviderMappingsKey(String providerMappingsKey);

    /**
     * Gets the default provider mapping
     */
    String getDefaultProviderMapping();

    /**
     * Sets the default provider mapping
     */
    void setDefaultProviderMapping(String providerMapping);

    /**
     * Gets the tunnel endpoint address for a given Node
     * @param node a {@link Node}
     * @return the tunnel endpoint
     * @see java.net.InetAddress
     */
    InetAddress getTunnelEndPoint(Node node);

    /**
     * Returns the OpenFlow version to be used by the {@link NetworkingProvider}
     * Default is OpenFlow 1.0. OVS versions greater than 1.10.0 will use OpenFlow 1.3
     * @param node the node to query
     * @return the OpenFlow version to use
     */
    String getOpenflowVersion(Node node);

    /**
     * Determine if L3 forwarding is enabled
     * @return true if ovsdb net-virt is configured to perform L3 forwarding
     */
    boolean isL3ForwardingEnabled();

    /**
     * Determine if Distributed ARP Responder is enabled
     * @return true if ovsdb net-virt is configured for distributed arp responder
     */
    boolean isDistributedArpDisabled();

    /**
     * Returns the MacAddress to be used for the default gateway by the {@link L3ForwardingProvider}
     * There is no default.
     * @param node the node to query
     * @return the MacAddress to use for the default gateway; or null if none is configured.
     */
    String getDefaultGatewayMacAddress(Node node);
}
