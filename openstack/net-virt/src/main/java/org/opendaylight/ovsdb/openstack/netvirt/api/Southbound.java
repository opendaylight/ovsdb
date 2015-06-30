/*
 * Copyright (c) 2015 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.openstack.netvirt.api;

import java.util.List;
import java.util.Map;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.Options;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;

/**
 * Utility class to wrap southbound transactions.
 *
 * @author Sam Hague (shague@redhat.com)
 */
public interface Southbound {
    ConnectionInfo getConnectionInfo(Node node);
    OvsdbNodeAugmentation extractOvsdbNode(Node node);
    NodeId extractBridgeOvsdbNodeId(Node bridgeNode);
    List<Node> readOvsdbTopologyNodes();
    Node readOvsdbNode(Node bridgeNode);
    boolean isBridgeOnOvsdbNode(Node node, String bridgeName);
    String getOvsdbNodeUUID(Node node);
    String getOsdbNodeExternalIdsValue(OvsdbNodeAugmentation ovsdbNodeAugmentation, String key);
    boolean addBridge(Node ovsdbNode, String bridgeName, String target);
    boolean deleteBridge(Node ovsdbNode);
    OvsdbBridgeAugmentation readBridge(Node node, String name);
    Node readBridgeNode(Node node, String name);
    Node getBridgeNode(Node node, String bridgeName);
    String getBridgeUuid(Node node, String name);
    long getDataPathId(Node node);
    String getDatapathId(Node node);
    String getDatapathId(OvsdbBridgeAugmentation ovsdbBridgeAugmentation);
    OvsdbBridgeAugmentation getBridge(Node node, String name);
    OvsdbBridgeAugmentation getBridge(Node node);
    String getBridgeName(Node node);
    String extractBridgeName(Node node);
    OvsdbBridgeAugmentation extractBridgeAugmentation(Node node);
    List<Node> getAllBridgesOnOvsdbNode(Node node);
    OvsdbNodeAugmentation extractNodeAugmentation(Node node);
    List<OvsdbTerminationPointAugmentation> getTerminationPointsOfBridge(Node node);
    OvsdbTerminationPointAugmentation getTerminationPointOfBridge(Node node, String terminationPoint);
    OvsdbTerminationPointAugmentation extractTerminationPointAugmentation(Node bridgeNode, String portName);
    List<TerminationPoint> extractTerminationPoints(Node node);
    List<OvsdbTerminationPointAugmentation> extractTerminationPointAugmentations(Node node);
    List<OvsdbTerminationPointAugmentation> readTerminationPointAugmentations(Node node);
    String getInterfaceExternalIdsValue(
            OvsdbTerminationPointAugmentation terminationPointAugmentation, String key);
    Boolean addTerminationPoint(Node bridgeNode, String bridgeName, String portName, String type);
    Boolean deleteTerminationPoint(Node bridgeNode, String portName);
    Boolean addTerminationPoint(Node bridgeNode, String bridgeName, String portName,
                                String type, Map<String, String> options);
    TerminationPoint readTerminationPoint(Node bridgeNode, String bridgeName, String portName);
    Boolean addTunnelTerminationPoint(Node bridgeNode, String bridgeName, String portName, String type,
                                      Map<String, String> options);
    Boolean isTunnelTerminationPointExist(Node bridgeNode, String bridgeName, String portName);
    Boolean addPatchTerminationPoint(Node node, String bridgeName, String portName, String peerPortName);
    String getExternalId(Node node, OvsdbTables table, String key);
    String getOtherConfig(Node node, OvsdbTables table, String key);
    boolean addVlanToTp(long vlan);
    boolean isTunnel(OvsdbTerminationPointAugmentation port);
    String getOptionsValue(List<Options> options, String key);
    Topology getOvsdbTopology();
    Long getOFPort(OvsdbTerminationPointAugmentation port);
    Long getOFPort(Node bridgeNode, String portName);
    DataBroker getDatabroker();
    void initializeNetvirtTopology();
}
