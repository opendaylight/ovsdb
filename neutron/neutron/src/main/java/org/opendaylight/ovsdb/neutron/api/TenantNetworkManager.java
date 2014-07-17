/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 *  Authors : Dave Tucker
 */

package org.opendaylight.ovsdb.neutron.api;

import org.opendaylight.controller.networkconfig.neutron.NeutronNetwork;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.ovsdb.schema.openvswitch.Interface;

/**
 * Open vSwitch isolates Tenant Networks using VLANs on the Integration Bridge
 * This class manages the provisioning of these VLANs
 */
public interface TenantNetworkManager {

    /**
     * Get the VLAN assigned to the provided Network
     * @param node the {@link org.opendaylight.controller.sal.core.Node} to query
     * @param networkId the Neutron Network ID
     * @return the assigned VLAN ID or 0 in case of an error
     */
    public int getInternalVlan(Node node, String networkId);

    /**
     * Reclaim the assigned VLAN for the given Network
     * @param node the {@link org.opendaylight.controller.sal.core.Node} to query
     * @param portUUID the UUID of the neutron Port
     * @param network the Neutron Network ID
     */
    public void reclaimInternalVlan(Node node, String portUUID, NeutronNetwork network);

    /**
     * Configures the VLAN for a Tenant Network
     * @param node the {@link org.opendaylight.controller.sal.core.Node} to configure
     * @param portUUID the UUID of the port to configure
     * @param network the Neutron Network ID
     */
    public void programInternalVlan(Node node, String portUUID, NeutronNetwork network);

    /**
     * Check is the given network is present on a Node
     * @param node the {@link org.opendaylight.controller.sal.core.Node} to query
     * @param segmentationId the Neutron Segementation ID
     * @return True or False
     */
    public boolean isTenantNetworkPresentInNode(Node node, String segmentationId);

    /**
     * Get the Neutron Network ID for a given Segmentation ID
     */
    public String getNetworkId (String segmentationId);

    /**
     * Get the {@link org.opendaylight.controller.networkconfig.neutron.NeutronNetwork} for a given Interface
     */
    public NeutronNetwork getTenantNetwork(Interface intf);

    /**
     * Network Created Callback
     */
    @Deprecated
    public void networkCreated (String networkId);

    /**
     * Network Created Callback
     */
    public int networkCreated (Node node, String networkId);

    /**
     * Network Deleted Callback
     */
    public void networkDeleted(String id);
}