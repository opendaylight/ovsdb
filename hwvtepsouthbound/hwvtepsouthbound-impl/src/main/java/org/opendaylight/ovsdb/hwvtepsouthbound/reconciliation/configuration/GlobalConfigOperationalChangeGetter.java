/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.reconciliation.configuration;

import com.google.common.collect.Sets;
import java.util.stream.Collectors;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LocalMcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LocalUcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GlobalConfigOperationalChangeGetter {

    public static DataTreeModification<Node> getModification(InstanceIdentifier<Node> nodeId, Node configNode,
                                                             Node opNode) {

        NodeBuilder newNodeBuilder = getNodeBuilderFromNode(configNode);
        NodeBuilder oldNodeBuilder = getNodeBuilderFromNode(opNode);

        HwvtepGlobalAugmentationBuilder newAugmentation = getAugmentationFromNode(configNode);
        HwvtepGlobalAugmentationBuilder oldAugmentation = getAugmentationFromNode(opNode);

        //fire removal of local ucast macs so that logical switches will be deleted
        fillLocalMacsToBeRemoved(oldAugmentation, configNode, opNode);

        newNodeBuilder.addAugmentation(HwvtepGlobalAugmentation.class, newAugmentation.build());
        oldNodeBuilder.addAugmentation(HwvtepGlobalAugmentation.class, oldAugmentation.build());

        return new DataTreeModificationImpl<>(nodeId, newNodeBuilder.build(), oldNodeBuilder.build());
    }

    static void fillLocalMacsToBeRemoved(HwvtepGlobalAugmentationBuilder oldAugmentation, Node configNode, Node opNode) {
        Set<String> logicalSwitchNamesToBeRemoved = getLogicalSwitchesToBeRemoved(configNode, opNode);
        List<LocalUcastMacs> localUcastMacsToBeRemoved = getLocalUcastMacsToBeRemoved(opNode,
                logicalSwitchNamesToBeRemoved);
        List<LocalMcastMacs> localMcastMacsToBeRemoved = getLocalMcastMacsToBeRemoved(opNode,
                logicalSwitchNamesToBeRemoved);

        oldAugmentation.setLocalUcastMacs(localUcastMacsToBeRemoved);
        oldAugmentation.setLocalMcastMacs(localMcastMacsToBeRemoved);
    }

    static List<LocalUcastMacs> getLocalUcastMacsToBeRemoved(Node opNode, final Set<String> removedSwitchNames) {
        if (opNode == null || opNode.getAugmentation(HwvtepGlobalAugmentation.class) == null) {
            return null;
        }
        List<LocalUcastMacs> localUcastMacs = opNode.getAugmentation(HwvtepGlobalAugmentation.class).
                getLocalUcastMacs();
        if (localUcastMacs == null) {
            return null;
        }
        return localUcastMacs.stream()
                .filter(mac -> removedSwitchNames.contains(
                        mac.getLogicalSwitchRef().getValue().firstKeyOf(
                                LogicalSwitches.class).getHwvtepNodeName().getValue()))
                .collect(Collectors.toList());
    }

    static List<LocalMcastMacs> getLocalMcastMacsToBeRemoved(Node opNode, final Set<String> removedSwitchNames) {
        if (opNode == null || opNode.getAugmentation(HwvtepGlobalAugmentation.class) == null) {
            return null;
        }
        List<LocalMcastMacs> localMcastMacs = opNode.getAugmentation(HwvtepGlobalAugmentation.class).
                getLocalMcastMacs();
        if (localMcastMacs == null) {
            return null;
        }
        return localMcastMacs.stream()
                .filter(mac -> removedSwitchNames.contains(
                        mac.getLogicalSwitchRef().getValue().firstKeyOf(
                                LogicalSwitches.class).getHwvtepNodeName().getValue()))
                .collect(Collectors.toList());
    }

    static  Set<String> getLogicalSwitchesToBeRemoved(Node configNode, Node opNode) {
        Set<String> opSwitchNames = new HashSet<>();
        Set<String> cfgSwitchNames = new HashSet<>();
        List<LogicalSwitches> cfgLogicalSwitches = new ArrayList<>();
        List<LogicalSwitches> opLogicalSwitches = new ArrayList<>();

        if (opNode != null && opNode.getAugmentation(HwvtepGlobalAugmentation.class) != null) {
            opLogicalSwitches = opNode.getAugmentation(HwvtepGlobalAugmentation.class).getLogicalSwitches();
        }
        if (configNode != null && configNode.getAugmentation(HwvtepGlobalAugmentation.class) != null) {
            cfgLogicalSwitches = configNode.getAugmentation(HwvtepGlobalAugmentation.class).getLogicalSwitches();
        }
        if (opLogicalSwitches != null) {
            for (LogicalSwitches ls : opLogicalSwitches) {
                opSwitchNames.add(ls.getHwvtepNodeName().getValue());
            }
        }
        if (cfgLogicalSwitches != null) {
            for (LogicalSwitches ls : cfgLogicalSwitches) {
                cfgSwitchNames.add(ls.getHwvtepNodeName().getValue());
            }
        }
        final Set<String> removedSwitchNames = Sets.difference(opSwitchNames, cfgSwitchNames);
        return removedSwitchNames;
    }

    static HwvtepGlobalAugmentationBuilder getAugmentationFromNode(Node node ) {
        if (node == null) {
            return new HwvtepGlobalAugmentationBuilder();
        }
        HwvtepGlobalAugmentation src = node.getAugmentation(HwvtepGlobalAugmentation.class);
        HwvtepGlobalAugmentationBuilder builder = new HwvtepGlobalAugmentationBuilder();
        if (src != null) {
            builder.setLogicalSwitches(src.getLogicalSwitches());
            builder.setRemoteMcastMacs(src.getRemoteMcastMacs());
            builder.setRemoteUcastMacs(src.getRemoteUcastMacs());
        }
        return builder;
    }

    static NodeBuilder getNodeBuilderFromNode(Node node) {
        NodeBuilder newNodeBuilder;
        if (node != null) {
            newNodeBuilder = new NodeBuilder(node);
            newNodeBuilder.removeAugmentation(HwvtepGlobalAugmentation.class);
        } else {
            newNodeBuilder = new NodeBuilder();
        }
        newNodeBuilder.setTerminationPoint(new ArrayList<>());
        return newNodeBuilder;
    }
}