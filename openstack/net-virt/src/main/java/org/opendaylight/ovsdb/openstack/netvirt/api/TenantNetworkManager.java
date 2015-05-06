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

import java.util.List;
import org.opendaylight.neutron.spi.NeutronNetwork;
import org.opendaylight.neutron.spi.NeutronPort;
import org.opendaylight.ovsdb.schema.openvswitch.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceExternalIds;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

/**
 * Open vSwitch isolates Tenant Networks using VLANs on the Integration Bridge
 * This class manages the provisioning of these VLANs
 */
public interface TenantNetworkManager {

    /**
     * Get the VLAN assigned to the provided Network
     * @param node the {@link Node} to query
     * @param networkId the Neutron Network ID
     * @return the assigned VLAN ID or 0 in case of an error
     */
    public int getInternalVlan(Node node, String networkId);

    /**
     * Reclaim the assigned VLAN for the given Network
     * @param node the {@link Node} to query
     * @param network the Neutron Network ID
     */
    public void reclaimInternalVlan(Node node, NeutronNetwork network);

    /**
     * Configures the VLAN for a Tenant Network
     * @param node the {@link Node} to configure
     * @param tp the termination point
     * @param network the Neutron Network ID
     */
    public void programInternalVlan(Node node, OvsdbTerminationPointAugmentation tp, NeutronNetwork network);

    /**
     * Check is the given network is present on a Node
     * @param node the {@link Node} to query
     * @param segmentationId the Neutron Segementation ID
     * @return True or False
     */
    public boolean isTenantNetworkPresentInNode(Node node, String segmentationId);

    /**
     * Get the Neutron Network ID for a given Segmentation ID
     */
    public String getNetworkId (String segmentationId);

    /**
     * Network Created Callback
     */
    public int networkCreated (Node node, String networkId);

    /**
     * Network Deleted Callback
     */
    public void networkDeleted(String id);

    NeutronNetwork getTenantNetwork(OvsdbTerminationPointAugmentation terminationPointAugmentation);
    public NeutronPort getTenantPort(OvsdbTerminationPointAugmentation terminationPointAugmentation);
}
