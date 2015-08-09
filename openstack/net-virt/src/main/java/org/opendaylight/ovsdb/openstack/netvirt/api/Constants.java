/*
 * Copyright (c) 2014, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.api;

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
    public static final String EXTERNAL_NETWORK = "external";

    /*
     * @see http://docs.openstack.org/grizzly/openstack-network/admin/content/ovs_quantum_plugin.html
     */
    public static final String TUNNEL_ENDPOINT_KEY = "local_ip";
    public static final String INTEGRATION_BRIDGE = "br-int";
    public static final String NETWORK_BRIDGE = "br-net";
    public static final String EXTERNAL_BRIDGE = "br-ex";
    public static final String PATCH_PORT_TO_INTEGRATION_BRIDGE_NAME = "patch-int";
    public static final String PATCH_PORT_TO_NETWORK_BRIDGE_NAME = "patch-net";
    public static final String PATCH_PORT_TO_EXTERNAL_BRIDGE_NAME = "patch-ext";
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
    public static final String OPENFLOW13 = "OpenFlow13";

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

    /*
     * MD-SAL
     */

    public static final String OPENFLOW_NODE_PREFIX = "openflow:";
    public static final String NETVIRT_TOPOLOGY_ID = "netvirt:1";

    /*
     * Ethertypes
     */
    public static final long ARP_ETHERTYPE = 0x0806L;


    /*
     * ACL
     */
    public static final Integer PROTO_DHCP_CLIENT_SPOOF_MATCH_PRIORITY_DROP = 61011;
    public static final Integer PROTO_MATCH_PRIORITY_DROP = 36006;
    public static final Integer PROTO_PORT_MATCH_PRIORITY_DROP = 36005;
    public static final Integer PREFIX_MATCH_PRIORITY_DROP = 36004;
    public static final Integer PROTO_PREFIX_MATCH_PRIORITY_DROP = 36003;
    public static final Integer PREFIX_PORT_MATCH_PRIORITY_DROP = 36002;
    public static final Integer PROTO_PORT_PREFIX_MATCH_PRIORITY_DROP = 36001;

    public static final Integer PROTO_DHCP_CLIENT_TRAFFIC_MATCH_PRIORITY = 61012;
    public static final Integer PROTO_MATCH_PRIORITY = 61010;
    public static final Integer PREFIX_MATCH_PRIORITY = 61009;
    public static final Integer PROTO_PREFIX_MATCH_PRIORITY = 61008;
    public static final Integer PROTO_PORT_MATCH_PRIORITY = 61007;
    public static final Integer PROTO_PORT_PREFIX_MATCH_PRIORITY = 61007;
    public static final Integer PROTO_DHCP_SERVER_MATCH_PRIORITY = 61006;
    public static final Integer PROTO_VM_IP_MAC_MATCH_PRIORITY = 36001;

    public static final int TCP_SYN = 0x002;
    public static final short INGRESS_ACL = 40; // Flows Destined to the VM Port go here
    public static final short OUTBOUND_SNAT = 110; // Ingress ACL table drains traffic to this table

    private static Long groupId = 1L;

    //6653 is official openflow port.
    public static short OPENFLOW_PORT = 6653;
    public static String OPENFLOW_CONNECTION_PROTOCOL = "tcp";
}
