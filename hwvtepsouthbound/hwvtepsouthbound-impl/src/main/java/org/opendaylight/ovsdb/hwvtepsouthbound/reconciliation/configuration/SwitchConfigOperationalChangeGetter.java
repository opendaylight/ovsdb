/*
 * Copyright © 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.reconciliation.configuration;

import com.google.common.collect.Lists;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalPortAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalPortAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.PhysicalSwitchAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class SwitchConfigOperationalChangeGetter {

    private static final Logger LOG = LoggerFactory.getLogger(SwitchConfigOperationalChangeGetter.class);

    public static DataTreeModification<Node> getModification(InstanceIdentifier<Node> psNodeId,
                                                             Node configNode, Node operationalNode) {

        NodeBuilder newNodeBuilder = getNodeBuilderFromNode(configNode);
        NodeBuilder oldNodeBuilder = getNodeBuilderFromNode(operationalNode);

        List<TerminationPoint> tpList = getPorts(configNode);
        if (tpList.size() > 0) {
            newNodeBuilder.setTerminationPoint(tpList);
        }

        tpList = getPorts(operationalNode);
        if (tpList.size() > 0) {
            oldNodeBuilder.setTerminationPoint(tpList);
        }

        return new DataTreeModificationImpl<>(psNodeId, newNodeBuilder.build(), oldNodeBuilder.build());

    }


    static NodeBuilder getNodeBuilderFromNode(Node node) {
        NodeBuilder newNodeBuilder;
        if (node != null) {
            newNodeBuilder = new NodeBuilder(node);
            newNodeBuilder.removeAugmentation(PhysicalSwitchAugmentation.class);
        } else {
            newNodeBuilder = new NodeBuilder();
        }
        List<TerminationPoint> emptyList = Lists.newArrayList();
        newNodeBuilder.setTerminationPoint(emptyList);

        return newNodeBuilder;
    }


    static List<TerminationPoint> getPorts(Node node) {
        ArrayList<TerminationPoint> tpList = Lists.newArrayList();
        if (node == null || node.getTerminationPoint() == null) {
            return tpList;
        }
        for (TerminationPoint tp: node.getTerminationPoint()) {
            TerminationPointBuilder terminationPointBuilder = new TerminationPointBuilder(tp);
            terminationPointBuilder.removeAugmentation(HwvtepPhysicalPortAugmentation.class);

            HwvtepPhysicalPortAugmentation augmentation = tp.getAugmentation(HwvtepPhysicalPortAugmentation.class);
            HwvtepPhysicalPortAugmentationBuilder builder = new HwvtepPhysicalPortAugmentationBuilder();
            if (augmentation != null) {
                builder = new HwvtepPhysicalPortAugmentationBuilder(augmentation);
            }
            if (augmentation != null && augmentation.getVlanBindings() != null && !augmentation.getVlanBindings().isEmpty() ) {
                builder.setVlanBindings(augmentation.getVlanBindings());
                terminationPointBuilder.addAugmentation(HwvtepPhysicalPortAugmentation.class, builder.build());
                tpList.add(terminationPointBuilder.build());
            }
        }
        return tpList;
    }
}