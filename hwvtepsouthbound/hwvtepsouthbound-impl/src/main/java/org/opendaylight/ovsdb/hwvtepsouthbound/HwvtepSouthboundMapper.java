/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.schema.hardwarevtep.Global;
import org.opendaylight.ovsdb.schema.hardwarevtep.LogicalSwitch;
import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalSwitch;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.ConnectionInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HwvtepSouthboundMapper {
    private static final Logger LOG = LoggerFactory.getLogger(HwvtepSouthboundMapper.class);
    private static final String N_CONNECTIONS_STR = "n_connections";

    private static NodeId createNodeId(HwvtepConnectionInstance client) {
        NodeKey key = client.getInstanceIdentifier().firstKeyOf(Node.class, NodeKey.class);
        return key.getNodeId();

    }

    public static InstanceIdentifier<Node> createInstanceIdentifier(NodeId nodeId) {
        return InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(HwvtepSouthboundConstants.HWVTEP_TOPOLOGY_ID))
                .child(Node.class,new NodeKey(nodeId));
    }

    public static InstanceIdentifier<Node> createInstanceIdentifier (OvsdbClient client) {
        return createInstanceIdentifier(createIpAddress(client.getConnectionInfo().getRemoteAddress()),
                        new PortNumber(client.getConnectionInfo().getRemotePort()));
    }

    private static InstanceIdentifier<Node> createInstanceIdentifier(IpAddress ip, PortNumber port) {
        String uriString = HwvtepSouthboundConstants.HWVTEP_URI_PREFIX + "://"
                + String.valueOf(ip.getValue()) + ":" + port.getValue();
        Uri uri = new Uri(uriString);
        NodeId nodeId = new NodeId(uri);
        InstanceIdentifier<Node> path = InstanceIdentifier.create(NetworkTopology.class)
                        .child(Topology.class, new TopologyKey(HwvtepSouthboundConstants.HWVTEP_TOPOLOGY_ID))
                        .child(Node.class,new NodeKey(nodeId));
        LOG.debug("Created ovsdb path: {}",path);
        return path;
    }

    public static NodeId createManagedNodeId(InstanceIdentifier<Node> iid) {
        NodeKey nodeKey = iid.firstKeyOf(Node.class, NodeKey.class);
        return nodeKey.getNodeId();
    }

    public static IpAddress createIpAddress(InetAddress address) {
        IpAddress ip = null;
        if (address instanceof Inet4Address) {
            ip = createIpAddress((Inet4Address)address);
        } else if (address instanceof Inet6Address) {
            ip = createIpAddress((Inet6Address)address);
        }
        return ip;
    }

    public static IpAddress createIpAddress(Inet4Address address) {
        Ipv4Address ipv4 = new Ipv4Address(address.getHostAddress());
        return new IpAddress(ipv4);
    }

    public static IpAddress createIpAddress(Inet6Address address) {
        Ipv6Address ipv6 = new Ipv6Address(address.getHostAddress());
        return new IpAddress(ipv6);
    }

    public static ConnectionInfo createConnectionInfo(OvsdbClient client) {
        ConnectionInfoBuilder connectionInfoBuilder = new ConnectionInfoBuilder();
        connectionInfoBuilder.setRemoteIp(createIpAddress(client.getConnectionInfo().getRemoteAddress()));
        connectionInfoBuilder.setRemotePort(new PortNumber(client.getConnectionInfo().getRemotePort()));
        connectionInfoBuilder.setLocalIp(createIpAddress(client.getConnectionInfo().getLocalAddress()));
        connectionInfoBuilder.setLocalPort(new PortNumber(client.getConnectionInfo().getLocalPort()));
        return connectionInfoBuilder.build();
    }

    public static ConnectionInfo suppressLocalIpPort(ConnectionInfo connectionInfo) {
        ConnectionInfoBuilder connectionInfoBuilder = new ConnectionInfoBuilder();
        connectionInfoBuilder.setRemoteIp(connectionInfo.getRemoteIp());
        connectionInfoBuilder.setRemotePort(connectionInfo.getRemotePort());
        return connectionInfoBuilder.build();
    }

    public static InetAddress createInetAddress(IpAddress ip) throws UnknownHostException {
        if (ip.getIpv4Address() != null) {
            return InetAddress.getByName(ip.getIpv4Address().getValue());
        } else if (ip.getIpv6Address() != null) {
            return InetAddress.getByName(ip.getIpv6Address().getValue());
        } else {
            throw new UnknownHostException("IP Address has no value");
        }
    }

    public static InstanceIdentifier<Node> getInstanceIdentifier(Global global) {
        String nodeString = HwvtepSouthboundConstants.HWVTEP_URI_PREFIX + "://" +
                HwvtepSouthboundConstants.UUID + "/" + global.getUuid().toString();
        NodeId nodeId = new NodeId(new Uri(nodeString));
        NodeKey nodeKey = new NodeKey(nodeId);
        TopologyKey topoKey = new TopologyKey(HwvtepSouthboundConstants.HWVTEP_TOPOLOGY_ID);
        return InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, topoKey)
                .child(Node.class, nodeKey)
                .build();
    }

    public static InstanceIdentifier<Node> createInstanceIdentifier(HwvtepConnectionInstance client,
                    PhysicalSwitch pSwitch) {
        String nodeString = client.getNodeKey().getNodeId().getValue() + "/physicalswitch/" + pSwitch.getName();
        NodeId nodeId = new NodeId(new Uri(nodeString));
        NodeKey nodeKey = new NodeKey(nodeId);
        return InstanceIdentifier.builder(NetworkTopology.class)
                        .child(Topology.class, new TopologyKey(HwvtepSouthboundConstants.HWVTEP_TOPOLOGY_ID))
                        .child(Node.class, nodeKey).build();
    }

    public static InstanceIdentifier<Node> createInstanceIdentifier(HwvtepConnectionInstance client,
                    LogicalSwitch lSwitch) {
        String nodeString = client.getNodeKey().getNodeId().getValue() + "/logicalswitch/" + lSwitch.getName();
        NodeId nodeId = new NodeId(new Uri(nodeString));
        NodeKey nodeKey = new NodeKey(nodeId);
        return InstanceIdentifier.builder(NetworkTopology.class)
                        .child(Topology.class, new TopologyKey(HwvtepSouthboundConstants.HWVTEP_TOPOLOGY_ID))
                        .child(Node.class, nodeKey).build();
    }

}
