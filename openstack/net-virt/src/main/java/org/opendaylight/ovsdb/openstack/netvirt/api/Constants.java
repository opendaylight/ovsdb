/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Dave Tucker
 */
package org.opendaylight.ovsdb.openstack.netvirt.api;

import org.opendaylight.ovsdb.lib.notation.Version;

/**
 * A collection of configuration constants
 */
public final class Constants {

    /*
     * External ID's used by OpenStack Neutron
     */
    public static final String EXTERNAL_ID_VM_ID = "vm-id";
    public static final String EXTERNAL_ID_INTERFACE_ID = "iface-id";
    public static final String EXTERNAL_ID_VM_MAC = "attached-mac";

    /*
     * @see http://docs.openstack.org/grizzly/openstack-network/admin/content/ovs_quantum_plugin.html
     */
    public static final String TUNNEL_ENDPOINT_KEY = "local_ip";
    public static final String INTEGRATION_BRIDGE = "br-int";
    public static final String NETWORK_BRIDGE = "br-net";
    public static final String EXTERNAL_BRIDGE = "br-ex";
    public static final String PATCH_PORT_TO_INTEGRATION_BRIDGE_NAME = "patch-int";
    public static final String PATCH_PORT_TO_NETWORK_BRIDGE_NAME = "patch-net";
    public static final String PROVIDER_MAPPINGS_KEY = "provider_mappings";
    public static final String PROVIDER_MAPPING = "physnet1:eth1";

    /*
     * Flow Priority Defaults
     */
    public static final int LLDP_PRIORITY = 1000;
    public static final int NORMAL_PRIORITY = 0;

    /*
     * OpenFlow Versions
     */
    public static final String OPENFLOW10 = "OpenFlow10";
    public static final String OPENFLOW13 = "OpenFlow13";
    public static final Version OPENFLOW13_SUPPORTED = Version.fromString("1.10.0");

    /*
     * VLAN Constants
     */
    public static final int MAX_VLAN = 4094;

    /*
     * OSGi Service Properties
     */
    public static final String SOUTHBOUND_PROTOCOL_PROPERTY = "southboundProtocol";
    public static final String PROVIDER_TYPE_PROPERTY = "providerType";
    public static final String OPENFLOW_VERSION_PROPERTY = "openflowVersion";
    public static final String EVENT_HANDLER_TYPE_PROPERTY = "eventHandlerType";
    public static final String PROVIDER_NAME_PROPERTY = "providerName";
    public static final String NAT_PROVIDER_DIRECTION = "natDirection";
}
