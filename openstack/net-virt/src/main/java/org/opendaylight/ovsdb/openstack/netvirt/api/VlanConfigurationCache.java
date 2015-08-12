/*
 * Copyright (c) 2014, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.api;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

/**
 * This cache stores the VLAN assignments used for tenant separation within a vSwitch
 * An assignment consists of a VLAN ID and a tenant network ID.
 */
public interface VlanConfigurationCache {

    /**
     * Assigns a free VLAN ID for the given tenant network
     * @param node an Open vSwitch {@link Node}
     * @param networkId the Neutron Network ID
     * @return a VLAN ID or 0 in case of an error
     */
    Integer assignInternalVlan(Node node, String networkId);

    /**
     * Recovers an assigned VLAN ID when it is no longer required
     * @param node an Open vSwitch {@link Node}
     * @param networkId the Neutron Network ID
     * @return the reclaimed VLAN ID or 0 in case of an error
     */
    Integer reclaimInternalVlan(Node node, String networkId);

    /**
     * Returns a VLAN ID assigned to a given tenant network
     * @param node an Open vSwitch {@link Node}
     * @param networkId the Neutron Network ID
     * @return the VLAN ID or 0 in case of an error
     */
    Integer getInternalVlan(Node node, String networkId);
}
