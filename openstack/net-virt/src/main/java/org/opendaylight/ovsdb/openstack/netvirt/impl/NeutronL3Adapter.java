/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Dave Tucker, Flavio Fernandes
 */

package org.opendaylight.ovsdb.openstack.netvirt.impl;

import org.opendaylight.controller.networkconfig.neutron.NeutronFloatingIP;
import org.opendaylight.controller.networkconfig.neutron.NeutronNetwork;
import org.opendaylight.controller.networkconfig.neutron.NeutronPort;
import org.opendaylight.controller.networkconfig.neutron.NeutronRouter;
import org.opendaylight.controller.networkconfig.neutron.NeutronRouter_Interface;
import org.opendaylight.controller.networkconfig.neutron.NeutronSubnet;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.ovsdb.openstack.netvirt.AbstractEvent;
import org.opendaylight.ovsdb.openstack.netvirt.NorthboundEvent;
import org.opendaylight.ovsdb.openstack.netvirt.api.ArpProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.InboundNatProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.L3ForwardingProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.MultiTenantAwareRouter;
import org.opendaylight.ovsdb.openstack.netvirt.api.OutboundNatProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.RoutingProvider;
import org.opendaylight.ovsdb.schema.openvswitch.Interface;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Neutron L3 Adapter implements a hub-like adapter for the various Neutron events. Based on
 * these events, the abstract router callbacks can be generated to the multi-tenant aware router,
 * as well as the multi-tenant router forwarding provider.
 */
public class NeutronL3Adapter {

    /**
     * Logger instance.
     */
    static final Logger logger = LoggerFactory.getLogger(NeutronL3Adapter.class);

    // The implementation for each of these services is resolved by the OSGi Service Manager
    private volatile MultiTenantAwareRouter multiTenantAwareRouter;
    private volatile L3ForwardingProvider l3ForwardingProvider;
    private volatile InboundNatProvider inboundNatProvider;
    private volatile OutboundNatProvider outboundNatProvider;
    private volatile ArpProvider arpProvider;
    private volatile RoutingProvider routingProvider;

    //
    // Callbacks from OVSDB's northbound handlers
    //

    public void handleNeutronSubnetEvent(final NeutronSubnet subnet, NorthboundEvent.Action action) {
        logger.debug("Neutron subnet {} event : {}", action, subnet.toString());

        // TODO
    }

    public void handleNeutronPortEvent(final NeutronPort neutronPort, NorthboundEvent.Action action) {
        logger.debug("Neutron port {} event : {}", action, neutronPort.toString());

        // TODO

    }

    public void handleNeutronRouterEvent(final NeutronRouter neutronRouter, NorthboundEvent.Action action) {
        logger.debug("Neutron router {} event : {}", action, neutronRouter.toString());

        // TODO

    }

    public void handleNeutronRouterInterfaceEvent(final NeutronRouter neutronRouter,
                                                  final NeutronRouter_Interface neutronRouterInterface,
                                                  NorthboundEvent.Action action) {
        logger.debug(" Router {} interface {} attached. Subnet {}", neutronRouter.getName(),
                     neutronRouterInterface.getPortUUID(),
                     neutronRouterInterface.getSubnetUUID());

        // TODO

    }

    public void handleNeutronFloatingIPEvent(final NeutronFloatingIP neutronFloatingIP,
                                             NorthboundEvent.Action action) {
        logger.debug(" Floating IP {} {}, uuid {}", action,
                     neutronFloatingIP.getFixedIPAddress(),
                     neutronFloatingIP.getFloatingIPUUID());

        // TODO
    }

    public void handleNeutronNetworkEvent(final NeutronNetwork neutronNetwork, NorthboundEvent.Action action) {
        logger.debug("neutronNetwork {}: network: {}", action, neutronNetwork);

        // TODO
    }

    //
    // Callbacks from OVSDB's southbound handler
    //

    public void handleInterfaceEvent(final Node node, final Interface intf, NeutronNetwork neutronNetwork,
                                     AbstractEvent.Action action) {
        logger.debug("southbound interface {} node:{} interface:{}, neutronNetwork:{}",
                     action, node, intf, neutronNetwork);

        // TODO
    }

}
