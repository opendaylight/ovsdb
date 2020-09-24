/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.utils.hwvtepsouthbound.utils;

import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundConstants;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundMapper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepNodeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.ConnectionInfo;
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

public final class HwvtepSouthboundUtils {
    private static final Logger LOG = LoggerFactory.getLogger(HwvtepSouthboundUtils.class);

    private HwvtepSouthboundUtils() {
    }

    public static NodeId createNodeId(IpAddress ip, PortNumber port) {
        String uriString = HwvtepSouthboundConstants.HWVTEP_URI_PREFIX + "://"
                + ip.stringValue() + ":" + port.getValue();
        Uri uri = new Uri(uriString);
        return new NodeId(uri);
    }

    public static Node createNode(ConnectionInfo key) {
        return new NodeBuilder()
            .setNodeId(createNodeId(key.getRemoteIp(), key.getRemotePort()))
            .addAugmentation(createHwvtepAugmentation(key))
            .build();
    }

    public static HwvtepGlobalAugmentation createHwvtepAugmentation(ConnectionInfo key) {
        return new HwvtepGlobalAugmentationBuilder().setConnectionInfo(key).build();
    }

    public static InstanceIdentifier<Node> createInstanceIdentifier(ConnectionInfo key) {
        return createInstanceIdentifier(key.getRemoteIp(), key.getRemotePort());
    }

    public static InstanceIdentifier<Node> createInstanceIdentifier(IpAddress ip, PortNumber port) {
        InstanceIdentifier<Node> path = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(HwvtepSouthboundConstants.HWVTEP_TOPOLOGY_ID))
                .child(Node.class,createNodeKey(ip,port));
        LOG.debug("Created hwvtep path: {}",path);
        return path;
    }

    public static InstanceIdentifier<Node> createInstanceIdentifier(ConnectionInfo key, HwvtepNodeName name) {
        return HwvtepSouthboundMapper.createInstanceIdentifier(
                        createManagedNodeId(key, name));
    }

    private static NodeId createManagedNodeId(ConnectionInfo key, HwvtepNodeName nodeName) {
        return createManagedNodeId(key.getRemoteIp(), key.getRemotePort(), nodeName);
    }

    private static NodeId createManagedNodeId(IpAddress remoteIp, PortNumber remotePort, HwvtepNodeName nodeName) {
        //This assumes that HwvtepNode can only be Physical switch
        return new NodeId(createNodeId(remoteIp,remotePort).getValue()
                        + "/" + HwvtepSouthboundConstants.PSWITCH_URI_PREFIX + "/" + nodeName.getValue());
    }

    public static NodeKey createNodeKey(IpAddress ip, PortNumber port) {
        return new NodeKey(createNodeId(ip, port));
    }

    public static Object connectionInfoToString(ConnectionInfo connectionInfo) {
        return connectionInfo.getRemoteIp().stringValue() + ":" + connectionInfo.getRemotePort().getValue();
    }

}
