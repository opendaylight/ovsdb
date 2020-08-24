/*
 * Copyright © 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.reconciliation.configuration;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalPortAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalPortAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.PhysicalSwitchAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class SwitchConfigOperationalChangeGetter {
    private SwitchConfigOperationalChangeGetter() {
    }

    public static DataTreeModification<Node> getModification(final InstanceIdentifier<Node> psNodeId,
                                                             final Node configNode, final Node operationalNode) {

        NodeBuilder newNodeBuilder = getNodeBuilderFromNode(configNode);
        NodeBuilder oldNodeBuilder = getNodeBuilderFromNode(operationalNode);

        Map<TerminationPointKey, TerminationPoint> tpList = getPorts(configNode);
        if (tpList.size() > 0) {
            newNodeBuilder.setTerminationPoint(tpList);
        }

        tpList = getPorts(operationalNode);
        if (tpList.size() > 0) {
            oldNodeBuilder.setTerminationPoint(tpList);
        }

        return new DataTreeModificationImpl<>(psNodeId, newNodeBuilder.build(), oldNodeBuilder.build());
    }

    static NodeBuilder getNodeBuilderFromNode(final Node node) {
        NodeBuilder newNodeBuilder;
        if (node != null) {
            newNodeBuilder = new NodeBuilder(node);
            newNodeBuilder.removeAugmentation(PhysicalSwitchAugmentation.class);
        } else {
            newNodeBuilder = new NodeBuilder();
        }
        newNodeBuilder.setTerminationPoint(Collections.emptyMap());

        return newNodeBuilder;
    }

    static Map<TerminationPointKey, TerminationPoint> getPorts(final Node node) {
        if (node == null) {
            return Collections.emptyMap();
        }
        final Map<TerminationPointKey, TerminationPoint> tps = node.getTerminationPoint();
        if (tps == null) {
            return Collections.emptyMap();
        }

        final Map<TerminationPointKey, TerminationPoint> result = new HashMap<>();
        for (TerminationPoint tp : node.getTerminationPoint().values()) {
            TerminationPointBuilder terminationPointBuilder = new TerminationPointBuilder(tp);
            terminationPointBuilder.removeAugmentation(HwvtepPhysicalPortAugmentation.class);

            HwvtepPhysicalPortAugmentation augmentation = tp.augmentation(HwvtepPhysicalPortAugmentation.class);
            HwvtepPhysicalPortAugmentationBuilder builder = new HwvtepPhysicalPortAugmentationBuilder();
            if (augmentation != null) {
                builder = new HwvtepPhysicalPortAugmentationBuilder(augmentation);
            }

            if (augmentation != null && augmentation.getVlanBindings() != null
                    && !augmentation.getVlanBindings().isEmpty()) {
                builder.setVlanBindings(augmentation.getVlanBindings());
                terminationPointBuilder.addAugmentation(builder.build());

                final TerminationPoint newTp = terminationPointBuilder.build();
                result.put(newTp.key(), newTp);
            }
        }
        return result;
    }
}
