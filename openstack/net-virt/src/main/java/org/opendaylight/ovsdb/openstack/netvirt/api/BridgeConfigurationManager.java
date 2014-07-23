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

import org.opendaylight.controller.networkconfig.neutron.NeutronNetwork;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;

import java.util.List;

/**
 * OpenStack Neutron with the Open vSwitch plugin relies on a typical bridge configuration that
 * consists of br-int (Integration Bridge), br-net (Network bridge), br-ex (External bridge).
 *
 * This class ensures that the bridges on each {@link org.opendaylight.controller.sal.core.Node}
 * are correctly configured for OpenStack Neutron
 *
 */
public interface BridgeConfigurationManager {

    /**
     * A helper function to get the UUID of a given Bridge
     * @param node the {@link org.opendaylight.controller.sal.core.Node} where the bridge is configured
     * @param bridgeName the name of the bridge
     * @return the UUID of the bridge
     */
    public String getBridgeUuid(Node node, String bridgeName);

    /**
     * Checks for the existence of the Integration Bridge on a given Node
     * @param node the {@link org.opendaylight.controller.sal.core.Node} where the bridge should be configured
     * @return True if the bridge exists, False if it does not
     */
    public boolean isNodeNeutronReady(Node node);

    /**
     * Checks for the existence of the Network Bridge on a given Node
     * @param node the {@link org.opendaylight.controller.sal.core.Node} where the bridge should be configured
     * @return True if the bridge exists, False if it does not
     */
    public boolean isNodeOverlayReady(Node node);

    /**
     * Checks for the existence of the Network Bridge on a given Node
     * @param node the {@link org.opendaylight.controller.sal.core.Node} where the bridge should be configured
     * @return True if the bridge exists, False if it does not
     */

    /**
     * Checks that a Node is ready for a Tunnel Network Provider
     * For OpenFlow 1.0 the Integration, Network Bridge and corresponding patch ports are required
     * For OpenFlow 1.3 only the Integration Bridge is required
     * @param node the {@link org.opendaylight.controller.sal.core.Node} where the bridge is configured
     * @return True or False
     */
    public boolean isNodeTunnelReady(Node node);

    /* Determine if internal network is ready for vlan network types.
     * - OF 1.0 requires br-int, br-net, a patch connecting them and
     * physical device added to br-net.
     * - OF 1.3 requires br-int and physical device added to br-int.
     */

    /**
     * Checks that a Node is ready for a VLAN Network Provider for the given Network
     * For OpenFlow 1.0 the Integration Bridge, Network Bridge, patch ports and a physical device connected to the
     * Network Bridge are required.
     * For OpenFlow 1.3 the Integration Bridge is required and must have a physical device connected.
     * @param node the {@link org.opendaylight.controller.sal.core.Node} where the bridge is configured
     * @param network the {@link org.opendaylight.controller.networkconfig.neutron.NeutronNetwork}
     * @return True or False
     */
    public boolean isNodeVlanReady(Node node, NeutronNetwork network);

    /**
     * A helper function to determine if a port exists on a given bridge
     * @param node the {@link org.opendaylight.controller.sal.core.Node} where the bridge is configured
     * @param bridge the {@link org.opendaylight.ovsdb.schema.openvswitch.Bridge} to query
     * @param portName the name of the port to search for
     * @return True if the port exists, otherwise False
     */
    public boolean isPortOnBridge (Node node, Bridge bridge, String portName);


    /**
     * Returns true if the bridges required for the provider network type are created
     * If the bridges are not created, this method will attempt to create them
     * @param node the {@link org.opendaylight.controller.sal.core.Node} to query
     * @param network the {@link org.opendaylight.controller.networkconfig.neutron.NeutronNetwork}
     * @return True or False
     */
    public boolean createLocalNetwork(Node node, NeutronNetwork network);

    /**
     * Prepares the given Node for Neutron Networking by creating the Integration Bridge
     * @param node the {@link org.opendaylight.controller.sal.core.Node} to prepare
     */
    public void prepareNode(Node node);

    /**
     * Returns the physical interface mapped to the given neutron physical network.
     * @param node
     * @param physicalNetwork
     * @return
     */
    public String getPhysicalInterfaceName (Node node, String physicalNetwork);

    /** Returns all physical interfaces configured in the bridge mapping
     * Bridge mappings will be of the following format:
     * @param node the {@link org.opendaylight.controller.sal.core.Node} to query
     * @return a List in the format {eth1, eth2} given bridge_mappings=physnet1:eth1,physnet2:eth2
     */
    public List<String> getAllPhysicalInterfaceNames(Node node);
}