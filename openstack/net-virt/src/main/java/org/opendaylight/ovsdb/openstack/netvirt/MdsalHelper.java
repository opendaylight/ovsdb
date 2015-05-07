/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeDpdk;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeDpdkr;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeDpdkvhost;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeDpdkvhostuser;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeGeneve;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeGre64;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeInternal;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeIpsecGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeIpsecGre64;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeLisp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypePatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeSystem;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeTap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableBiMap;

public class MdsalHelper {
    private static final Logger LOG = LoggerFactory.getLogger(MdsalUtils.class);
    public static final TopologyId OVSDB_TOPOLOGY_ID = new TopologyId(new Uri("ovsdb:1"));
    public static final String OVSDB_URI_PREFIX = "ovsdb";
    public static final String BRIDGE_URI_PREFIX = "bridge";
    public static final String TP_URI_PREFIX = "termination-point";

    public static final ImmutableBiMap<String, Class<? extends InterfaceTypeBase>> OVSDB_INTERFACE_TYPE_MAP
    = new ImmutableBiMap.Builder<String, Class<? extends InterfaceTypeBase>>()
        .put("internal", InterfaceTypeInternal.class)
        .put("vxlan", InterfaceTypeVxlan.class)
        .put("patch", InterfaceTypePatch.class)
        .put("system", InterfaceTypeSystem.class)
        .put("tap", InterfaceTypeTap.class)
        .put("geneve", InterfaceTypeGeneve.class)
        .put("gre", InterfaceTypeGre.class)
        .put("ipsec_gre", InterfaceTypeIpsecGre.class)
        .put("gre64", InterfaceTypeGre64.class)
        .put("ipsec_gre64", InterfaceTypeIpsecGre64.class)
        .put("lisp", InterfaceTypeLisp.class)
        .put("dpdk", InterfaceTypeDpdk.class)
        .put("dpdkr", InterfaceTypeDpdkr.class)
        .put("dpdkvhost", InterfaceTypeDpdkvhost.class)
        .put("dpdkvhostuser", InterfaceTypeDpdkvhostuser.class)
        .build();


    public static NodeId createManagedNodeId(ConnectionInfo key, String bridgeName) {
        return createManagedNodeId(key.getRemoteIp(),key.getRemotePort(),bridgeName);
    }

    public static InstanceIdentifier<Node> createInstanceIdentifier(NodeId nodeId) {
        InstanceIdentifier<Node> nodePath = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(OVSDB_TOPOLOGY_ID))
                .child(Node.class,new NodeKey(nodeId));
        return nodePath;
    }

    public static InstanceIdentifier<Node> createInstanceIdentifier(ConnectionInfo key,String bridgeName) {
        return createInstanceIdentifier(createManagedNodeId(key, bridgeName));
    }

    public static NodeId createManagedNodeId(IpAddress ip, PortNumber port, String bridgeName) {
        return new NodeId(createNodeId(ip,port).getValue()
                + "/" + BRIDGE_URI_PREFIX + "/" + bridgeName);
    }

    public static NodeId createNodeId(IpAddress ip, PortNumber port) {
        String uriString = OVSDB_URI_PREFIX + "://"
                + new String(ip.getValue()) + ":" + port.getValue();
        Uri uri = new Uri(uriString);
        NodeId nodeId = new NodeId(uri);
        return nodeId;
    }

    public static NodeId createNodeId(ConnectionInfo connectionInfo) {
        return createNodeId(connectionInfo.getRemoteIp(), connectionInfo.getRemotePort());
    }

    public static InstanceIdentifier<TerminationPoint> createTerminationPointInstanceIdentifier(IpAddress ip, PortNumber port , String bridgeName, String portName){
        String tpUri = createManagedNodeId(ip, port, bridgeName) + "/" + TP_URI_PREFIX + "/" + portName;
        InstanceIdentifier<TerminationPoint> nodePath = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(OVSDB_TOPOLOGY_ID))
                .child(Node.class,new NodeKey(createNodeId(ip,port)))
                .child(TerminationPoint.class, new TerminationPointKey(new TpId(tpUri)));
        LOG.debug("Termination point InstanceIdentigier generated : {}",nodePath);
        return nodePath;
    }

    public static InstanceIdentifier<TerminationPoint> createTerminationPointInstanceIdentifier(ConnectionInfo connectionInfo , String bridgeName, String portName){
        return createTerminationPointInstanceIdentifier(connectionInfo.getRemoteIp(), connectionInfo.getRemotePort(),bridgeName, portName);
    }

    public static InstanceIdentifier<TerminationPoint> createTerminationPointInstanceIdentifier(Node node, String portName){
        String tpUri = node.getNodeId().getValue() + "/" + TP_URI_PREFIX + "/" + portName;

        InstanceIdentifier<TerminationPoint> nodePath = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(OVSDB_TOPOLOGY_ID))
                .child(Node.class,node.getKey())
                .child(TerminationPoint.class, new TerminationPointKey(new TpId(tpUri)));

        LOG.debug("Termination point InstanceIdentigier generated : {}",nodePath);
        return nodePath;
    }
}
