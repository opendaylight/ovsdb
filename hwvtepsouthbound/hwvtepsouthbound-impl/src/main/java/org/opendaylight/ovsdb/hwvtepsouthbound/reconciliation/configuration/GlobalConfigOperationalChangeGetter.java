/*
 * Copyright © 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.reconciliation.configuration;

import com.google.common.collect.Sets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LocalMcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LocalMcastMacsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LocalUcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LocalUcastMacsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitchesKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.util.BindingMap;

public final class GlobalConfigOperationalChangeGetter {

    private GlobalConfigOperationalChangeGetter() {
    }

    public static DataTreeModification<Node> getModification(final InstanceIdentifier<Node> nodeId,
                                                             final Node configNode, final Node opNode) {

        NodeBuilder newNodeBuilder = getNodeBuilderFromNode(configNode);
        NodeBuilder oldNodeBuilder = getNodeBuilderFromNode(opNode);

        HwvtepGlobalAugmentationBuilder newAugmentation = augmentationFromNode(configNode);
        HwvtepGlobalAugmentationBuilder oldAugmentation = augmentationFromNode(opNode);

        //fire removal of local ucast macs so that logical switches will be deleted
        fillLocalMacsToBeRemoved(oldAugmentation, configNode, opNode);

        newNodeBuilder.addAugmentation(newAugmentation.build());
        oldNodeBuilder.addAugmentation(oldAugmentation.build());

        return new DataTreeModificationImpl<>(nodeId, newNodeBuilder.build(), oldNodeBuilder.build());
    }

    static void fillLocalMacsToBeRemoved(final HwvtepGlobalAugmentationBuilder oldAugmentation, final Node configNode,
            final Node opNode) {
        Set<String> logicalSwitchNamesToBeRemoved = getLogicalSwitchesToBeRemoved(configNode, opNode);
        var localUcastMacsToBeRemoved = getLocalUcastMacsToBeRemoved(opNode, logicalSwitchNamesToBeRemoved);
        var localMcastMacsToBeRemoved = getLocalMcastMacsToBeRemoved(opNode, logicalSwitchNamesToBeRemoved);

        oldAugmentation.setLocalUcastMacs(localUcastMacsToBeRemoved);
        oldAugmentation.setLocalMcastMacs(localMcastMacsToBeRemoved);
    }

    static Map<LocalUcastMacsKey, LocalUcastMacs> getLocalUcastMacsToBeRemoved(final Node opNode,
            final Set<String> removedSwitchNames) {
        if (opNode == null || opNode.augmentation(HwvtepGlobalAugmentation.class) == null) {
            return null;
        }
        Map<LocalUcastMacsKey, LocalUcastMacs> localUcastMacs = opNode.augmentation(HwvtepGlobalAugmentation.class)
                .getLocalUcastMacs();
        if (localUcastMacs == null) {
            return null;
        }
        return localUcastMacs.values().stream()
                .filter(mac -> removedSwitchNames.contains(
                        mac.getLogicalSwitchRef().getValue().firstKeyOf(
                                LogicalSwitches.class).getHwvtepNodeName().getValue()))
                .collect(BindingMap.toOrderedMap());
    }

    static Map<LocalMcastMacsKey, LocalMcastMacs> getLocalMcastMacsToBeRemoved(final Node opNode,
            final Set<String> removedSwitchNames) {
        if (opNode == null || opNode.augmentation(HwvtepGlobalAugmentation.class) == null) {
            return null;
        }
        Map<LocalMcastMacsKey, LocalMcastMacs> localMcastMacs = opNode.augmentation(HwvtepGlobalAugmentation.class)
                .getLocalMcastMacs();
        if (localMcastMacs == null) {
            return null;
        }
        return localMcastMacs.values().stream()
                .filter(mac -> removedSwitchNames.contains(
                        mac.getLogicalSwitchRef().getValue().firstKeyOf(
                                LogicalSwitches.class).getHwvtepNodeName().getValue()))
                .collect(BindingMap.toOrderedMap());
    }

    static  Set<String> getLogicalSwitchesToBeRemoved(final Node configNode, final Node opNode) {
        Set<String> opSwitchNames = new HashSet<>();
        Set<String> cfgSwitchNames = new HashSet<>();
        Map<LogicalSwitchesKey, LogicalSwitches> cfgLogicalSwitches = null;
        Map<LogicalSwitchesKey, LogicalSwitches> opLogicalSwitches = null;

        if (opNode != null && opNode.augmentation(HwvtepGlobalAugmentation.class) != null) {
            opLogicalSwitches = opNode.augmentation(HwvtepGlobalAugmentation.class).getLogicalSwitches();
        }
        if (configNode != null && configNode.augmentation(HwvtepGlobalAugmentation.class) != null) {
            cfgLogicalSwitches = configNode.augmentation(HwvtepGlobalAugmentation.class).getLogicalSwitches();
        }
        if (opLogicalSwitches != null) {
            for (LogicalSwitches ls : opLogicalSwitches.values()) {
                opSwitchNames.add(ls.getHwvtepNodeName().getValue());
            }
        }
        if (cfgLogicalSwitches != null) {
            for (LogicalSwitches ls : cfgLogicalSwitches.values()) {
                cfgSwitchNames.add(ls.getHwvtepNodeName().getValue());
            }
        }
        final Set<String> removedSwitchNames = Sets.difference(opSwitchNames, cfgSwitchNames);
        return removedSwitchNames;
    }

    static HwvtepGlobalAugmentationBuilder augmentationFromNode(final Node node) {
        if (node == null) {
            return new HwvtepGlobalAugmentationBuilder();
        }
        HwvtepGlobalAugmentation src = node.augmentation(HwvtepGlobalAugmentation.class);
        HwvtepGlobalAugmentationBuilder builder = new HwvtepGlobalAugmentationBuilder();
        if (src != null) {
            builder.setLogicalSwitches(src.getLogicalSwitches());
            builder.setRemoteMcastMacs(src.getRemoteMcastMacs());
            builder.setRemoteUcastMacs(src.getRemoteUcastMacs());
        }
        return builder;
    }

    static NodeBuilder getNodeBuilderFromNode(final Node node) {
        NodeBuilder newNodeBuilder;
        if (node != null) {
            newNodeBuilder = new NodeBuilder(node);
            newNodeBuilder.removeAugmentation(HwvtepGlobalAugmentation.class);
        } else {
            newNodeBuilder = new NodeBuilder();
        }
        newNodeBuilder.setTerminationPoint(Map.of());
        return newNodeBuilder;
    }
}
