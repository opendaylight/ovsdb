/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound;

import static java.util.Objects.requireNonNull;

import com.google.common.net.InetAddresses;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.schema.hardwarevtep.ACL;
import org.opendaylight.ovsdb.schema.hardwarevtep.Global;
import org.opendaylight.ovsdb.schema.hardwarevtep.LogicalSwitch;
import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalLocator;
import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalSwitch;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IetfInetUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.EncapsulationTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.EncapsulationTypeVxlanOverIpv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepNodeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalLocatorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalPortAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.PhysicalSwitchAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.Acls;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.AclsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.ConnectionInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitchesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.Tunnels;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.TunnelsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical.port.attributes.VlanBindings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical.port.attributes.VlanBindingsKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HwvtepSouthboundMapper {
    private static final Logger LOG = LoggerFactory.getLogger(HwvtepSouthboundMapper.class);

    private HwvtepSouthboundMapper() {
    }

    public static InstanceIdentifier<Node> createInstanceIdentifier(final NodeId nodeId) {
        return InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(HwvtepSouthboundConstants.HWVTEP_TOPOLOGY_ID))
                .child(Node.class,new NodeKey(nodeId));
    }

    public static InstanceIdentifier<Node> createInstanceIdentifier(final OvsdbClient client) {
        return createInstanceIdentifier(createIpAddress(client.getConnectionInfo().getRemoteAddress()),
                        new PortNumber(Uint16.valueOf(client.getConnectionInfo().getRemotePort())));
    }

    private static InstanceIdentifier<Node> createInstanceIdentifier(final IpAddress ip, final PortNumber port) {
        String uriString = HwvtepSouthboundConstants.HWVTEP_URI_PREFIX + "://"
                + ip.stringValue() + ":" + port.getValue();
        Uri uri = new Uri(uriString);
        NodeId nodeId = new NodeId(uri);
        InstanceIdentifier<Node> path = InstanceIdentifier.create(NetworkTopology.class)
                        .child(Topology.class, new TopologyKey(HwvtepSouthboundConstants.HWVTEP_TOPOLOGY_ID))
                        .child(Node.class,new NodeKey(nodeId));
        LOG.debug("Created ovsdb path: {}",path);
        return path;
    }

    public static InstanceIdentifier<Node> createInstanceIdentifier(final HwvtepConnectionInstance client,
            final PhysicalSwitch physicalSwitch) {
        //TODO: Clean this up
        return createInstanceIdentifier(client, new HwvtepNodeName(physicalSwitch.getName()));
    }

    public static InstanceIdentifier<Node> createInstanceIdentifier(final HwvtepConnectionInstance client,
                    final HwvtepNodeName psName) {
        NodeKey nodeKey = new NodeKey(createManagedNodeId(client, psName));
        return InstanceIdentifier.builder(NetworkTopology.class)
                        .child(Topology.class, new TopologyKey(HwvtepSouthboundConstants.HWVTEP_TOPOLOGY_ID))
                        .child(Node.class, nodeKey).build();
    }

    public static InstanceIdentifier<LogicalSwitches> createInstanceIdentifier(final HwvtepConnectionInstance client,
            final LogicalSwitch logicalSwitch) {
        InstanceIdentifier<LogicalSwitches> iid = null;
        iid = InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(HwvtepSouthboundConstants.HWVTEP_TOPOLOGY_ID))
                .child(Node.class, client.getNodeKey()).augmentation(HwvtepGlobalAugmentation.class)
                .child(LogicalSwitches.class, new LogicalSwitchesKey(new HwvtepNodeName(logicalSwitch.getName())))
                .build();
        /* TODO: Will this work instead to make it simpler?
        iid = client.getInstanceIdentifier().builder()
            .child(LogicalSwitches.class, new LogicalSwitchesKey(new HwvtepNodeName(lSwitch.getName())))).build()
         */
        return iid;
    }

    public static InstanceIdentifier<Acls> createInstanceIdentifier(final HwvtepConnectionInstance client,
            final ACL acl) {
        return InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(HwvtepSouthboundConstants.HWVTEP_TOPOLOGY_ID))
                .child(Node.class, client.getNodeKey()).augmentation(HwvtepGlobalAugmentation.class)
                .child(Acls.class, new AclsKey(acl.getAclName()))
                .build();
    }

    public static InstanceIdentifier<Tunnels> createInstanceIdentifier(final InstanceIdentifier<Node> nodeIid,
            final InstanceIdentifier<TerminationPoint> localTpIid,
            final InstanceIdentifier<TerminationPoint> remoteTpIid) {

        TunnelsKey tunnelsKey = new TunnelsKey(new HwvtepPhysicalLocatorRef(localTpIid),
                new HwvtepPhysicalLocatorRef(remoteTpIid));
        InstanceIdentifier<Tunnels> tunnelInstanceId = nodeIid.builder().augmentation(PhysicalSwitchAugmentation.class)
                .child(Tunnels.class, tunnelsKey).build();
        return tunnelInstanceId;
    }

    public static InstanceIdentifier<TerminationPoint> createInstanceIdentifier(final InstanceIdentifier<Node> nodeIid,
            final PhysicalLocator physicalLocator) {
        return nodeIid.child(TerminationPoint.class, getTerminationPointKey(physicalLocator));
    }

    public static InstanceIdentifier<VlanBindings> createInstanceIdentifier(final HwvtepConnectionInstance client,
            final InstanceIdentifier<TerminationPoint> tpPath, final VlanBindings vlanBindings) {
        return tpPath.augmentation(HwvtepPhysicalPortAugmentation.class).child(VlanBindings.class,
                new VlanBindingsKey(vlanBindings.key()));

    }

    public static InstanceIdentifier<Node> createInstanceIdentifier() {
        InstanceIdentifier<Node> path = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(HwvtepSouthboundConstants.HWVTEP_TOPOLOGY_ID))
                .child(Node.class);
        return path;
    }

    public static NodeId createManagedNodeId(final InstanceIdentifier<Node> iid) {
        NodeKey nodeKey = iid.firstKeyOf(Node.class);
        return nodeKey.getNodeId();
    }

    public static NodeId createManagedNodeId(final HwvtepConnectionInstance client, final HwvtepNodeName psName) {
        String nodeString = client.getNodeKey().getNodeId().getValue()
                        + "/" + HwvtepSouthboundConstants.PSWITCH_URI_PREFIX + "/" + psName.getValue();
        NodeId nodeId = new NodeId(new Uri(nodeString));
        return nodeId;
    }

    public static IpAddress createIpAddress(final InetAddress address) {
        IpAddress ip = null;
        if (address instanceof Inet4Address) {
            ip = createIpAddress((Inet4Address)address);
        } else if (address instanceof Inet6Address) {
            ip = createIpAddress((Inet6Address)address);
        }
        return ip;
    }

    public static IpAddress createIpAddress(final Inet4Address address) {
        return IetfInetUtil.ipAddressFor(address);
    }

    public static IpAddress createIpAddress(final Inet6Address address) {
        Ipv6Address ipv6 = new Ipv6Address(address.getHostAddress());
        return new IpAddress(ipv6);
    }

    public static ConnectionInfo createConnectionInfo(final OvsdbClient client) {
        ConnectionInfoBuilder connectionInfoBuilder = new ConnectionInfoBuilder();
        connectionInfoBuilder.setRemoteIp(createIpAddress(client.getConnectionInfo().getRemoteAddress()));
        connectionInfoBuilder.setRemotePort(new PortNumber(Uint16.valueOf(client.getConnectionInfo().getRemotePort())));
        connectionInfoBuilder.setLocalIp(createIpAddress(client.getConnectionInfo().getLocalAddress()));
        connectionInfoBuilder.setLocalPort(new PortNumber(Uint16.valueOf(client.getConnectionInfo().getLocalPort())));
        return connectionInfoBuilder.build();
    }

    public static ConnectionInfo suppressLocalIpPort(final ConnectionInfo connectionInfo) {
        ConnectionInfoBuilder connectionInfoBuilder = new ConnectionInfoBuilder();
        connectionInfoBuilder.setRemoteIp(connectionInfo.getRemoteIp());
        connectionInfoBuilder.setRemotePort(connectionInfo.getRemotePort());
        return connectionInfoBuilder.build();
    }

    public static InetAddress createInetAddress(final IpAddress ip) throws UnknownHostException {
        if (ip.getIpv4Address() != null) {
            return InetAddresses.forString(ip.getIpv4Address().getValue());
        } else if (ip.getIpv6Address() != null) {
            return InetAddress.getByName(ip.getIpv6Address().getValue());
        } else {
            throw new UnknownHostException("IP Address has no value");
        }
    }

    public static InstanceIdentifier<Node> getInstanceIdentifier(final Global global) {
        String nodeString = HwvtepSouthboundConstants.HWVTEP_URI_PREFIX + "://"
                + HwvtepSouthboundConstants.UUID + "/" + global.getUuid().toString();
        NodeId nodeId = new NodeId(new Uri(nodeString));
        NodeKey nodeKey = new NodeKey(nodeId);
        TopologyKey topoKey = new TopologyKey(HwvtepSouthboundConstants.HWVTEP_TOPOLOGY_ID);
        return InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, topoKey)
                .child(Node.class, nodeKey)
                .build();
    }

    public static EncapsulationTypeBase createEncapsulationType(final String type) {
        if (requireNonNull(type).isEmpty()) {
            return EncapsulationTypeVxlanOverIpv4.VALUE;
        }

        return HwvtepSouthboundConstants.ENCAPS_TYPE_MAP.inverse().get(type);
    }

    public static TerminationPointKey getTerminationPointKey(final PhysicalLocator physicalLocator) {
        TerminationPointKey tpKey = null;
        if (physicalLocator.getEncapsulationTypeColumn().getData() != null
                && physicalLocator.getDstIpColumn().getData() != null) {
            String tpKeyStr = physicalLocator.getEncapsulationTypeColumn().getData()
                    + ':' + physicalLocator.getDstIpColumn().getData();
            tpKey = new TerminationPointKey(new TpId(tpKeyStr));
        }
        return tpKey;
    }

    public static String getRandomUUID() {
        return "Random_" + java.util.UUID.randomUUID().toString().replace("-", "");
    }
}
