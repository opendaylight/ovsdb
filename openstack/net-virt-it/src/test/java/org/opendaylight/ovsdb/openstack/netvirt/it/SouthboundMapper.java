/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.openstack.netvirt.it;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
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

import com.google.common.base.Preconditions;

public class SouthboundMapper {
    private static final Logger LOG = LoggerFactory.getLogger(SouthboundMapper.class);

    public static Node createNode(ConnectionInfo key) {
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setNodeId(createNodeId(key.getRemoteIp(),key.getRemotePort()));
        nodeBuilder.addAugmentation(OvsdbNodeAugmentation.class, createOvsdbAugmentation(key));
        return nodeBuilder.build();
    }

    public static OvsdbNodeAugmentation createOvsdbAugmentation(ConnectionInfo key) {
        OvsdbNodeAugmentationBuilder ovsdbNodeBuilder = new OvsdbNodeAugmentationBuilder();
        ovsdbNodeBuilder.setConnectionInfo(key);
        return ovsdbNodeBuilder.build();
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

    public static InstanceIdentifier<Node> createInstanceIdentifier(NodeId nodeId) {
        return InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class,new NodeKey(nodeId));
    }

    public static InstanceIdentifier<Node> createInstanceIdentifier(ConnectionInfo key,OvsdbBridgeName bridgeName) {
        return createInstanceIdentifier(createManagedNodeId(key, bridgeName));
    }

    public static NodeId createManagedNodeId(InstanceIdentifier<Node> iid) {
        NodeKey nodeKey = iid.firstKeyOf(Node.class, NodeKey.class);
        return nodeKey.getNodeId();
    }

    public static InstanceIdentifier<Node> createInstanceIdentifier(ConnectionInfo key) {
        return createInstanceIdentifier(key.getRemoteIp(), key.getRemotePort());
    }

    public static InstanceIdentifier<Node> createInstanceIdentifier(IpAddress ip, PortNumber port) {
        InstanceIdentifier<Node> path = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class,createNodeKey(ip,port));
        LOG.debug("Created ovsdb path: {}",path);
        return path;
    }

    public static NodeKey createNodeKey(IpAddress ip, PortNumber port) {
        return new NodeKey(createNodeId(ip,port));
    }

    public static NodeId createManagedNodeId(ConnectionInfo key, OvsdbBridgeName bridgeName) {
        return createManagedNodeId(key.getRemoteIp(),key.getRemotePort(),bridgeName);
    }

    public static NodeId createManagedNodeId(IpAddress ip, PortNumber port, OvsdbBridgeName bridgeName) {
        return new NodeId(createNodeId(ip,port).getValue()
                + "/" + SouthboundConstants.BRIDGE_URI_PREFIX + "/" + bridgeName.getValue());
    }

    public static NodeId createNodeId(IpAddress ip, PortNumber port) {
        String uriString = SouthboundConstants.OVSDB_URI_PREFIX + "://"
                + String.valueOf(ip.getValue()) + ":" + port.getValue();
        Uri uri = new Uri(uriString);
        return new NodeId(uri);
    }

    public static  Class<? extends InterfaceTypeBase> createInterfaceType(String type) {
        Preconditions.checkNotNull(type);
        return SouthboundConstants.OVSDB_INTERFACE_TYPE_MAP.get(type);
    }
}
