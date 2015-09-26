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
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathTypeSystem;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ProtocolEntry;
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

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableBiMap;

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
                + new String(ip.getValue()) + ":" + port.getValue();
        Uri uri = new Uri(uriString);
        return new NodeId(uri);
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

    public static DatapathId createDatapathId(Set<String> dpids) {
        Preconditions.checkNotNull(dpids);
        if (dpids.isEmpty()) {
            return null;
        } else {
            String[] dpidArray = new String[dpids.size()];
            dpids.toArray(dpidArray);
            return createDatapathId(dpidArray[0]);
        }
    }

    public static String createDatapathType(OvsdbBridgeAugmentation mdsalbridge) {
        String datapathtype = SouthboundConstants.DATAPATH_TYPE_MAP.get(DatapathTypeSystem.class);

        if (mdsalbridge.getDatapathType() != null) {
            if (SouthboundConstants.DATAPATH_TYPE_MAP.get(mdsalbridge.getDatapathType()) != null) {
                datapathtype = SouthboundConstants.DATAPATH_TYPE_MAP.get(mdsalbridge.getDatapathType());
            } else {
                throw new IllegalArgumentException("Unknown datapath type "
                        + SouthboundConstants.DATAPATH_TYPE_MAP.get(mdsalbridge.getDatapathType()));
            }
        }
        return datapathtype;
    }

    public static  Class<? extends DatapathTypeBase> createDatapathType(String type) {
        Preconditions.checkNotNull(type);
        if (type.isEmpty()) {
            return DatapathTypeSystem.class;
        } else {
            ImmutableBiMap<String, Class<? extends DatapathTypeBase>> mapper =
                    SouthboundConstants.DATAPATH_TYPE_MAP.inverse();
            return mapper.get(type);
        }
    }

    public static DatapathId createDatapathId(String dpid) {
        Preconditions.checkNotNull(dpid);
        DatapathId datapath;
        if (dpid.matches("^[0-9a-fA-F]{16}")) {
            Splitter splitter = Splitter.fixedLength(2);
            Joiner joiner = Joiner.on(":");
            datapath = new DatapathId(joiner.join(splitter.split(dpid)));
        } else {
            datapath = new DatapathId(dpid);
        }
        return datapath;
    }

    public static Set<String> createOvsdbBridgeProtocols(OvsdbBridgeAugmentation ovsdbBridgeNode) {
        Set<String> protocols = new HashSet<String>();
        if (ovsdbBridgeNode.getProtocolEntry() != null && ovsdbBridgeNode.getProtocolEntry().size() > 0) {
            for (ProtocolEntry protocol : ovsdbBridgeNode.getProtocolEntry()) {
                if (SouthboundConstants.OVSDB_PROTOCOL_MAP.get(protocol.getProtocol()) != null) {
                    protocols.add(SouthboundConstants.OVSDB_PROTOCOL_MAP.get(protocol.getProtocol()));
                } else {
                    throw new IllegalArgumentException("Unknown protocol " + protocol.getProtocol());
                }
            }
        }
        return protocols;
    }

    public static  Class<? extends InterfaceTypeBase> createInterfaceType(String type) {
        Preconditions.checkNotNull(type);
        return SouthboundConstants.OVSDB_INTERFACE_TYPE_MAP.get(type);
    }

    public static String createOvsdbInterfaceType(Class<? extends InterfaceTypeBase> mdsaltype) {
        Preconditions.checkNotNull(mdsaltype);
        ImmutableBiMap<Class<? extends InterfaceTypeBase>, String> mapper =
                SouthboundConstants.OVSDB_INTERFACE_TYPE_MAP.inverse();
        return mapper.get(mdsaltype);
    }
}
