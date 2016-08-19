/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.reconciliation.configuration;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LocalMcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LocalUcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GlobalConfigOperationalChangeGetter {

    public static DataTreeModification<Node> getModification(InstanceIdentifier<Node> nodeId, Node configNode,
                                                             Node operationalNode) {

        NodeBuilder newNodeBuilder = getNodeBuilderFromNode(configNode);
        NodeBuilder oldNodeBuilder = getNodeBuilderFromNode(operationalNode);

        HwvtepGlobalAugmentationBuilder newAugmentation = getAugmentationFromNode(configNode);
        HwvtepGlobalAugmentationBuilder oldAugmentation = getAugmentationFromNode(operationalNode);

        final Set<String> removedSwitchNames = getRemovedSwitches(configNode, operationalNode);
        List<LocalUcastMacs> removedLocalUcastMacs = getlocalUcast(removedSwitchNames, operationalNode);
        List<LocalMcastMacs> removedLocalMcastMacs = getlocalMcast(removedSwitchNames, operationalNode);

        oldAugmentation.setLocalUcastMacs(removedLocalUcastMacs);
        oldAugmentation.setLocalMcastMacs(removedLocalMcastMacs);


        newNodeBuilder.addAugmentation(HwvtepGlobalAugmentation.class, newAugmentation.build());
        oldNodeBuilder.addAugmentation(HwvtepGlobalAugmentation.class, oldAugmentation.build());

        return new DataTreeModificationImpl<Node>(nodeId, newNodeBuilder.build(), oldNodeBuilder.build());
    }

    static List<LocalUcastMacs> getlocalUcast(final Set<String> removedSwitchNames, Node operationalNode) {
        List<LocalUcastMacs> localUcastMacs = operationalNode.getAugmentation(HwvtepGlobalAugmentation.class).getLocalUcastMacs();
        if (localUcastMacs == null) {
            return null;
        }
        Iterable<LocalUcastMacs> removedLocalUcastMacs = Iterables.filter(localUcastMacs,
                new Predicate<LocalUcastMacs>() {
                    @Override
                    public boolean apply(LocalUcastMacs o) {
                        LocalUcastMacs mac = (LocalUcastMacs) o;
                        String ls = mac.getLogicalSwitchRef().getValue().firstKeyOf(LogicalSwitches.class).getHwvtepNodeName().getValue();
                        if (removedSwitchNames.contains(ls)) {
                            return true;
                        }
                        return false;
                    }
                });
        return Lists.newArrayList(removedLocalUcastMacs);
    }

    static List<LocalMcastMacs> getlocalMcast(final Set<String> removedSwitchNames, Node operationalNode) {
        List<LocalMcastMacs> localMcastMacs = operationalNode.getAugmentation(HwvtepGlobalAugmentation.class).getLocalMcastMacs();
        if (localMcastMacs == null) {
            return null;
        }
        Iterable<LocalMcastMacs> removedLocalMcastMacs = Iterables.filter(localMcastMacs,
                new Predicate<LocalMcastMacs>() {
                    @Override
                    public boolean apply(LocalMcastMacs o) {
                        LocalMcastMacs mac = (LocalMcastMacs)o;
                        String ls = mac.getLogicalSwitchRef().getValue().firstKeyOf(LogicalSwitches.class).getHwvtepNodeName().getValue();
                        if (removedSwitchNames.contains(ls)) {
                            return true;
                        }
                        return false;
                    }
                });
        return Lists.newArrayList(removedLocalMcastMacs);
    }

    static  Set<String> getRemovedSwitches(Node configNode, Node operationalNode) {
        Set<String> opSwitchNames = new HashSet<>();
        Set<String> cfgSwitchNames = new HashSet<>();
        List<LogicalSwitches> cfgLogicalSwitches = Lists.newArrayList();

        List<LogicalSwitches> opLogicalSwitches = operationalNode.getAugmentation(HwvtepGlobalAugmentation.class).getLogicalSwitches();
        if (configNode != null) {
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

    static List<LogicalSwitches> getRemoved() {
        return null;
    }
    static HwvtepGlobalAugmentationBuilder getAugmentationFromNode(Node node ) {
        if (node == null) {
            return new HwvtepGlobalAugmentationBuilder();
        }
        HwvtepGlobalAugmentation src = node.getAugmentation(HwvtepGlobalAugmentation.class);
        HwvtepGlobalAugmentationBuilder builder = new HwvtepGlobalAugmentationBuilder();
        //if(getModificationForLogicalSwitches == true) {
        builder.setLogicalSwitches(src.getLogicalSwitches());
        //} else {
        builder.setRemoteMcastMacs(src.getRemoteMcastMacs());
        builder.setRemoteUcastMacs(src.getRemoteUcastMacs());
        //}

        return builder;
    }

    static NodeBuilder getNodeBuilderFromNode(Node node) {
        NodeBuilder newNodeBuilder;
        if (node != null) {
            newNodeBuilder = new NodeBuilder(node);
        } else {
            newNodeBuilder = new NodeBuilder();
        }
        newNodeBuilder.removeAugmentation(HwvtepGlobalAugmentation.class);
        return newNodeBuilder;
    }
}