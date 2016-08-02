/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.reconciliation.configuration;

import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class GlobalConfigOperationalChangeGetter {

    public static DataTreeModification<Node> getModification(InstanceIdentifier<Node> nodeId, Node configNode,
                                                      Node operationalNode) {

        NodeBuilder newNodeBuilder = getNodeBuilderFromNode(configNode);
        NodeBuilder oldNodeBuilder = getNodeBuilderFromNode(operationalNode);

        HwvtepGlobalAugmentation newAugmentation = getAugmentationFromNode(configNode);
        HwvtepGlobalAugmentation oldAugmentation = getAugmentationFromNode(operationalNode);

        newNodeBuilder.addAugmentation(HwvtepGlobalAugmentation.class, newAugmentation);
        oldNodeBuilder.addAugmentation(HwvtepGlobalAugmentation.class, oldAugmentation);

        return new DataTreeModificationImpl<Node>(nodeId, newNodeBuilder.build(), oldNodeBuilder.build());

    }

    static HwvtepGlobalAugmentation getAugmentationFromNode(Node node) {
        if (node == null) {
            return new HwvtepGlobalAugmentationBuilder().build();
        }
        HwvtepGlobalAugmentation src = node.getAugmentation(HwvtepGlobalAugmentation.class);
        HwvtepGlobalAugmentationBuilder builder = new HwvtepGlobalAugmentationBuilder();
        builder.setLogicalSwitches(src.getLogicalSwitches());
        //builder.setRemoteMcastMacs(src.getRemoteMcastMacs());
        //builder.setRemoteUcastMacs(src.getRemoteUcastMacs());
        return builder.build();
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