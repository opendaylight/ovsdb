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
import org.eclipse.jdt.annotation.NonNull;
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
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.binding.DataObjectIdentifier.WithKey;
import org.opendaylight.yangtools.binding.DataObjectReference;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HwvtepSouthboundMapper {
    private static final Logger LOG = LoggerFactory.getLogger(HwvtepSouthboundMapper.class);

    private HwvtepSouthboundMapper() {
        // Hidden on purpose
    }

    public static @NonNull WithKey<Node, NodeKey> createInstanceIdentifier(final NodeId nodeId) {
        return DataObjectIdentifier.builder(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(HwvtepSouthboundConstants.HWVTEP_TOPOLOGY_ID))
            .child(Node.class,new NodeKey(nodeId))
            .build();
    }

    public static @NonNull WithKey<Node, NodeKey> createInstanceIdentifier(final OvsdbClient client) {
        return createInstanceIdentifier(createIpAddress(client.getConnectionInfo().getRemoteAddress()),
                        new PortNumber(Uint16.valueOf(client.getConnectionInfo().getRemotePort())));
    }

    private static @NonNull WithKey<Node, NodeKey> createInstanceIdentifier(final IpAddress ip, final PortNumber port) {
        String uriString = HwvtepSouthboundConstants.HWVTEP_URI_PREFIX + "://"
                + ip.stringValue() + ":" + port.getValue();
        Uri uri = new Uri(uriString);
        NodeId nodeId = new NodeId(uri);
        final var path = DataObjectIdentifier.builder(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(HwvtepSouthboundConstants.HWVTEP_TOPOLOGY_ID))
            .child(Node.class,new NodeKey(nodeId))
            .build();
        LOG.debug("Created ovsdb path: {}", path);
        return path;
    }

    public static @NonNull WithKey<Node, NodeKey> createInstanceIdentifier(final HwvtepConnectionInstance client,
            final PhysicalSwitch physicalSwitch) {
        //TODO: Clean this up
        return createInstanceIdentifier(client, new HwvtepNodeName(physicalSwitch.getName()));
    }

    public static @NonNull WithKey<Node, NodeKey> createInstanceIdentifier(final HwvtepConnectionInstance client,
                    final HwvtepNodeName psName) {
        return DataObjectIdentifier.builder(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(HwvtepSouthboundConstants.HWVTEP_TOPOLOGY_ID))
            .child(Node.class, new NodeKey(createManagedNodeId(client, psName)))
            .build();
    }

    public static @NonNull WithKey<LogicalSwitches, LogicalSwitchesKey> createInstanceIdentifier(
            final HwvtepConnectionInstance client, final LogicalSwitch logicalSwitch) {
        return DataObjectIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(HwvtepSouthboundConstants.HWVTEP_TOPOLOGY_ID))
                .child(Node.class, client.getNodeKey()).augmentation(HwvtepGlobalAugmentation.class)
                .child(LogicalSwitches.class, new LogicalSwitchesKey(new HwvtepNodeName(logicalSwitch.getName())))
                .build();
        /* TODO: Will this work instead to make it simpler?
        iid = client.getInstanceIdentifier().builder()
            .child(LogicalSwitches.class, new LogicalSwitchesKey(new HwvtepNodeName(lSwitch.getName())))).build()
         */
    }

    public static @NonNull WithKey<Acls, AclsKey> createInstanceIdentifier(final HwvtepConnectionInstance client,
            final ACL acl) {
        return DataObjectIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(HwvtepSouthboundConstants.HWVTEP_TOPOLOGY_ID))
                .child(Node.class, client.getNodeKey()).augmentation(HwvtepGlobalAugmentation.class)
                .child(Acls.class, new AclsKey(acl.getAclName()))
                .build();
    }

    public static @NonNull WithKey<Tunnels, TunnelsKey> createInstanceIdentifier(
            final DataObjectIdentifier<Node> nodeIid, final DataObjectIdentifier<TerminationPoint> localTpIid,
            final DataObjectIdentifier<TerminationPoint> remoteTpIid) {
        return nodeIid.toBuilder()
            .augmentation(PhysicalSwitchAugmentation.class)
            .child(Tunnels.class, new TunnelsKey(new HwvtepPhysicalLocatorRef(localTpIid),
                new HwvtepPhysicalLocatorRef(remoteTpIid)))
            .build();
    }

    public static @NonNull WithKey<TerminationPoint, TerminationPointKey> createInstanceIdentifier(
            final DataObjectIdentifier<Node> nodeIid, final PhysicalLocator physicalLocator) {
        return nodeIid.toBuilder().child(TerminationPoint.class, getTerminationPointKey(physicalLocator)).build();
    }

    public static @NonNull WithKey<VlanBindings, VlanBindingsKey> createInstanceIdentifier(
            final HwvtepConnectionInstance client, final DataObjectIdentifier<TerminationPoint> tpPath,
            final VlanBindings vlanBindings) {
        return tpPath.toBuilder()
            .augmentation(HwvtepPhysicalPortAugmentation.class)
            .child(VlanBindings.class, new VlanBindingsKey(vlanBindings.key()))
            .build();
    }

    public static @NonNull DataObjectReference<Node> createInstanceIdentifier() {
        return DataObjectReference.builder(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(HwvtepSouthboundConstants.HWVTEP_TOPOLOGY_ID))
            .child(Node.class)
            .build();
   }

    public static @NonNull NodeId createManagedNodeId(final DataObjectIdentifier<Node> iid) {
        return iid.getFirstKeyOf(Node.class).getNodeId();
    }

    public static NodeId createManagedNodeId(final HwvtepConnectionInstance client, final HwvtepNodeName psName) {
        String nodeString = client.getNodeKey().getNodeId().getValue()
                        + "/" + HwvtepSouthboundConstants.PSWITCH_URI_PREFIX + "/" + psName.getValue();
        return new NodeId(new Uri(nodeString));
    }

    // FIXME: IetfInetUtil.ipAddressFor() does this as well, but does something else for IPv6
    public static IpAddress createIpAddress(final InetAddress address) {
        if (address instanceof Inet4Address inet4) {
            return createIpAddress(inet4);
        }
        if (address instanceof Inet6Address inet6) {
            return createIpAddress(inet6);
        }
        return null;
    }

    public static IpAddress createIpAddress(final Inet4Address address) {
        return IetfInetUtil.ipAddressFor(address);
    }

    public static IpAddress createIpAddress(final Inet6Address address) {
        return new IpAddress(new Ipv6Address(address.getHostAddress()));
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
        }
        if (ip.getIpv6Address() != null) {
            return InetAddress.getByName(ip.getIpv6Address().getValue());
        }
        throw new UnknownHostException("IP Address has no value");
    }

    public static @NonNull WithKey<Node, NodeKey> getInstanceIdentifier(final Global global) {
        String nodeString = HwvtepSouthboundConstants.HWVTEP_URI_PREFIX + "://"
                + HwvtepSouthboundConstants.UUID + "/" + global.getUuid().toString();
        NodeId nodeId = new NodeId(new Uri(nodeString));
        NodeKey nodeKey = new NodeKey(nodeId);
        TopologyKey topoKey = new TopologyKey(HwvtepSouthboundConstants.HWVTEP_TOPOLOGY_ID);
        return DataObjectIdentifier.builder(NetworkTopology.class)
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
