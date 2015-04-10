/*
* Copyright (c) 2015 Intel Corp. and others.  All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/
package org.opendaylight.overlay;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Preconditions;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.overlay.rev150105.SupportedTunnels;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.overlay.rev150105.SupportedTunnelsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.overlay.rev150105.SupportedTunnelsParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.overlay.rev150105.SupportedTunnelsParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.overlay.rev150105.TopologyTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.overlay.rev150105.Tunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.overlay.rev150105.TunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.overlay.rev150105.TunnelDestParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.overlay.rev150105.TunnelDestParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.overlay.rev150105.TunnelSourceParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.overlay.rev150105.TunnelSourceParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.overlay.rev150105.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.overlay.rev150105.network.topology.topology.node.SupportedTunnelEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.overlay.rev150105.network.topology.topology.node.SupportedTunnelEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.overlay.rev150105.network.topology.topology.node.supported.tunnel.entry.IpPortLocatorEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.overlay.rev150105.network.topology.topology.node.supported.tunnel.entry.IpPortLocatorEntry;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OverlayMapper {
    private static final Logger LOG = LoggerFactory.getLogger(OverlayMapper.class);

    public static Node createNode(NodeKey key) {
        // Todo set supported tunnel based on supported tunnels of southbound
        List<SupportedTunnelEntry> supportedTunnelEntryList = new ArrayList<>();
        List<IpPortLocatorEntry> ipPortLocatorEntryList = new ArrayList<>();
        ipPortLocatorEntryList.add(new IpPortLocatorEntryBuilder().build());
        for (String tunnelType : OverlayConstants.OVERLAY_TUNNEL_TYPE_MAP.keySet()) {
            supportedTunnelEntryList.add(new SupportedTunnelEntryBuilder()
                            .setTunnelType(createTunnelType(tunnelType))
                            .addAugmentation(SupportedTunnelsParameters.class, new SupportedTunnelsParametersBuilder()
                                            .setIpPortLocatorEntry(ipPortLocatorEntryList).build()).build());
        }
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setNodeId(key.getNodeId());
        nodeBuilder.addAugmentation(SupportedTunnels.class, new SupportedTunnelsBuilder()
                .setSupportedTunnelEntry(supportedTunnelEntryList)
                .build());
        return nodeBuilder.build();
    }

    public static Link createLink(LinkKey key, NodeId sourceNodeId, NodeId destNodeId) {
        LinkBuilder linkBuilder = new LinkBuilder();
        linkBuilder.setLinkId(key.getLinkId());
        linkBuilder.setSource(new SourceBuilder()
                        .setSourceNode(sourceNodeId)
                        .addAugmentation(TunnelSourceParameters.class,
                                new TunnelSourceParametersBuilder()
                                        .build())
                        .build());
        linkBuilder.setDestination(new DestinationBuilder()
                        .setDestNode(destNodeId)
                        .addAugmentation(TunnelDestParameters.class,
                                new TunnelDestParametersBuilder().build())
                        .build());
        linkBuilder.addAugmentation(Tunnel.class, createTunnelAugmentation());
        return linkBuilder.build();
    }

    public static Link createAugmentedLink(LinkKey key, NodeId sourceNodeId, NodeId destNodeId, String tunnelType) {
        LinkBuilder linkBuilder = new LinkBuilder();
        linkBuilder.setLinkId(key.getLinkId());
        linkBuilder.setSource(new SourceBuilder()
                        .setSourceNode(sourceNodeId)
                        .addAugmentation(TunnelSourceParameters.class,
                                new TunnelSourceParametersBuilder()
                                        .build())
                        .build());
        linkBuilder.setDestination(new DestinationBuilder()
                        .setDestNode(destNodeId)
                        .addAugmentation(TunnelDestParameters.class,
                                new TunnelDestParametersBuilder().build())
                        .build());
        linkBuilder.addAugmentation(Tunnel.class, new TunnelBuilder()
                .setTunnelType(createTunnelType(tunnelType))
                .build());
        return linkBuilder.build();
    }

    public static Tunnel createTunnelAugmentation() {
        TunnelBuilder tunnelBuilder = new TunnelBuilder();
        return tunnelBuilder.build();
    }

    public static Class<? extends TunnelTypeBase> createTunnelType(String type) {
        Preconditions.checkNotNull(type);
        return OverlayConstants.OVERLAY_TUNNEL_TYPE_MAP.get(type);
    }

    public static Class<? extends TopologyTypeBase> createTopologyType(String type) {
        Preconditions.checkNotNull(type);
        return OverlayConstants.OVERLAY_TOPOLOGY_TYPE_MAP.get(type);
    }

    public static IpAddress createIpAddress(InetAddress address) {
        IpAddress ip = null;
        if (address instanceof Inet4Address) {
            ip = createIpAddress((Inet4Address) address);
        } else if (address instanceof Inet6Address) {
            ip = createIpAddress((Inet6Address) address);
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
        InstanceIdentifier<Node> nodePath = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(OverlayConstants.OVERLAY_TOPOLOGY_ID))
                .child(Node.class, new NodeKey(nodeId));
        return nodePath;
    }

    public static InstanceIdentifier<Link> createInstanceIdentifier(LinkId linkId) {
        InstanceIdentifier<Link> linkPath = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(OverlayConstants.OVERLAY_TOPOLOGY_ID))
                .child(Link.class, new LinkKey(linkId));
        return linkPath;
    }


    public static InstanceIdentifier<Node> createInstanceIdentifier(IpAddress ip, PortNumber port) {
        InstanceIdentifier<Node> path = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(OverlayConstants.OVERLAY_TOPOLOGY_ID))
                .child(Node.class, createNodeKey(ip, port));
        LOG.info("Created overlay path: {}", path);
        return path;
    }

    public static NodeKey createNodeKey(IpAddress ip, PortNumber port) {
        return new NodeKey(createNodeId(ip, port));
    }

    public static NodeId createNodeId(IpAddress ip, PortNumber port) {
        String uriString = OverlayConstants.OVERLAY_URI_PREFIX + "://" + new String(ip.getValue())
                + ":" + port.getValue();
        Uri uri = new Uri(uriString);
        NodeId nodeId = new NodeId(uri);
        return nodeId;
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
}
