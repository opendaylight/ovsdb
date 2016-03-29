/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.netvirt.renderers.neutron;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableBiMap.Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.ports.rev151227.PortTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.ports.rev151227.PortTypeComputeNova;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.ports.rev151227.PortTypeDhcp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.ports.rev151227.PortTypeFloatingIp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.ports.rev151227.PortTypeRouterGateway;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.ports.rev151227.PortTypeRouterInterfaceDistributed;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.ports.rev151227.PortTypeRouter;

/**
 * A collection of configuration constants
 */
public final class Constants {

    /*
     * Port owner descriptions used by Openstack Neutron
     */
    public static final String OWNER_ROUTER_INTERFACE = "network:router_interface";
    public static final String OWNER_ROUTER_INTERFACE_DISTRIBUTED = "network:router_interface_distributed";
    public static final String OWNER_ROUTER_GATEWAY = "network:router_gateway";
    public static final String OWNER_NETWORK_DHCP = "network:dhcp";
    public static final String OWNER_FLOATING_IP = "network:floatingip";
    public static final String OWNER_COMPUTE_NOVA = "compute:nova";

    public static final String NETVIRT_NEUTRON_OWNER_ENTITY_TYPE = "ovsdb-netvirt-neutron-provider";

    public static final ImmutableBiMap<String, Class<? extends PortTypeBase>> NETVIRT_NEUTRON_PORT_TYPE_MAP
            = new ImmutableBiMap.Builder<String, Class<? extends PortTypeBase>>()
            .put(OWNER_ROUTER_INTERFACE, PortTypeRouter.class)
            .put(OWNER_ROUTER_INTERFACE_DISTRIBUTED, PortTypeRouterInterfaceDistributed.class)
            .put(OWNER_ROUTER_GATEWAY, PortTypeRouterGateway.class)
            .put(OWNER_NETWORK_DHCP, PortTypeDhcp.class)
            .put(OWNER_FLOATING_IP, PortTypeFloatingIp.class)
            .put(OWNER_COMPUTE_NOVA, PortTypeComputeNova.class)
            .build();
}
