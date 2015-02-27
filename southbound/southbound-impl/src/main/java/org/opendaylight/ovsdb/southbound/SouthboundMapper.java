/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.OvsdbConnectionInfo;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SouthboundMapper {
    private static final Logger LOG = LoggerFactory.getLogger(SouthboundMapper.class);

    public static Node createNode(OvsdbClient client) {
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setNodeId(createNodeId(client.getConnectionInfo()));
        nodeBuilder.addAugmentation(OvsdbNodeAugmentation.class, createOvsdbAugmentation(client));
        return nodeBuilder.build();
    }
    public static Node createNode(OvsdbClientKey key) {
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setNodeId(createNodeId(key.getIp(),key.getPort()));
        nodeBuilder.addAugmentation(OvsdbNodeAugmentation.class, createOvsdbAugmentation(key));
        return nodeBuilder.build();
    }

    public static OvsdbNodeAugmentation createOvsdbAugmentation(OvsdbClient client) {
        return createOvsdbAugmentation(new OvsdbClientKey(client));
    }

    public static OvsdbNodeAugmentation createOvsdbAugmentation(OvsdbClientKey key) {
        OvsdbNodeAugmentationBuilder ovsdbNodeBuilder = new OvsdbNodeAugmentationBuilder();
        ovsdbNodeBuilder.setIp(key.getIp());
        ovsdbNodeBuilder.setPort(key.getPort());
        return ovsdbNodeBuilder.build();
    }

    public static IpAddress createIpAddress(InetAddress address) {
        IpAddress ip = null;
        if(address instanceof Inet4Address) {
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

    public static InstanceIdentifier<Node> createInstanceIdentifier(OvsdbClient client) {
        return createInstanceIdentifier(createIpAddress(client.getConnectionInfo().getRemoteAddress()),
                new PortNumber(client.getConnectionInfo().getRemotePort()));
    }

    public static InstanceIdentifier<Node> createInstanceIdentifier(NodeId nodeId) {
        InstanceIdentifier<Node> nodePath = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class,new NodeKey(nodeId));
        return nodePath;
    }

    public static InstanceIdentifier<Node> createInstanceIdentifier(OvsdbClientKey key,OvsdbBridgeName bridgeName) {
        return createInstanceIdentifier(createManagedNodeId(key, bridgeName));
    }

    public static InstanceIdentifier<Node> createInstanceIndentifier(OvsdbClientKey key) {
        return createInstanceIdentifier(key.getIp(), key.getPort());
    }

    public static InstanceIdentifier<Node> createInstanceIdentifier(IpAddress ip, PortNumber port) {
        InstanceIdentifier<Node> path = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class,createNodeKey(ip,port));
        LOG.info("Created ovsdb path: {}",path);
        return path;
    }

    public static NodeKey createNodeKey(IpAddress ip, PortNumber port) {
        return new NodeKey(createNodeId(ip,port));
    }

    public static NodeId createNodeId(OvsdbConnectionInfo connectionInfo) {
        return createNodeId(createIpAddress(connectionInfo.getRemoteAddress()),
                new PortNumber(connectionInfo.getRemotePort()));
    }

    public static NodeId createManagedNodeId(OvsdbConnectionInfo connectionInfo, OvsdbBridgeName bridgeName) {
        return createManagedNodeId(createIpAddress(connectionInfo.getRemoteAddress()),
                new PortNumber(connectionInfo.getRemotePort()),
                bridgeName);
    }

    public static NodeId createManagedNodeId(OvsdbClientKey key, OvsdbBridgeName bridgeName) {
        return createManagedNodeId(key.getIp(),key.getPort(),bridgeName);
    }

    public static NodeId createManagedNodeId(IpAddress ip, PortNumber port, OvsdbBridgeName bridgeName) {
        return new NodeId(createNodeId(ip,port).getValue()
                + "/"+SouthboundConstants.BRIDGE_URI_PREFIX+"/"+ bridgeName.getValue());
    }

    public static NodeId createNodeId(IpAddress ip, PortNumber port) {
        String uriString = SouthboundConstants.OVSDB_URI_PREFIX + "://" + new String(ip.getValue()) +
                   ":" + port.getValue();
        Uri uri = new Uri(uriString);
        NodeId nodeId = new NodeId(uri);
        return nodeId;
    }

    public static InetAddress createInetAddress(IpAddress ip) throws UnknownHostException {
        if(ip.getIpv4Address() != null) {
            return InetAddress.getByName(ip.getIpv4Address().getValue());
        } else if(ip.getIpv6Address() != null) {
            return InetAddress.getByName(ip.getIpv6Address().getValue());
        } else {
            throw new UnknownHostException("IP Address has no value");
        }
    }
}
