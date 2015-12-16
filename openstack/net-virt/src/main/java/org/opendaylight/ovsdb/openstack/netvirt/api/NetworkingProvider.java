/*
 * Copyright (c) 2014, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.api;

import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronNetwork;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.osgi.framework.ServiceReference;

/**
 * The NetworkingProvider interface is implemented by Neutron Networking Providers
 */
public interface NetworkingProvider {

    /**
     * @return the name of the NetworkingProvider
     */
    String getName();

    /**
     * @return true if the provider supports Network Service Instances
     */
    boolean supportsServices();

    /**
     * @return true if the provider supports per-tenant or "static" tunneling
     */
    boolean hasPerTenantTunneling();

    /**
     * Handle Interface Update Callback Method
     * @param network Neutron Network attached to the interface
     * @param source Source node where interface is attached
     * @param intf Termination point attached to the node
     * @return true if interface update handled successfully
     */
    boolean handleInterfaceUpdate(NeutronNetwork network, Node source, OvsdbTerminationPointAugmentation intf);

    /**
     * Handle Interface Delete Callback Method
     * @param tunnelType Type of the tunnel (e.g. vxlan)
     * @param network Neutron Network associated with the removed interface
     * @param source Source node where interface was attached
     * @param intf Termination point associated to the deleted interface
     * @param isLastInstanceOnNode is last interface attached to the node ?
     * @return true if interface delete handled successfully
     */
    boolean handleInterfaceDelete(String tunnelType, NeutronNetwork network, Node source,
                                  OvsdbTerminationPointAugmentation intf, boolean isLastInstanceOnNode);

    /**
     * Initialize the Flow rules given the OVSDB node.
     * This method provides a set of common functionalities to initialize the Flow rules of an OVSDB node
     * that are Openflow Version specific. Hence we have this method in addition to the following
     * Openflow Node specific initialization method.
     * @param node Node on which flow rules are going to be installed
     */
    void initializeFlowRules(Node node);

    /**
     * Initialize the Flow rules for a given OpenFlow node
     * @param openflowNode Node on which flow rules are going to be installed
     */
    void initializeOFFlowRules(Node openflowNode);
}
