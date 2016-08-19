/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.ha.utils;

import com.google.common.base.Function;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalLocatorAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import javax.annotation.Nullable;
import java.util.Comparator;


public class PhysicalLocatorUtil {
    public static PhysicalLocatorComparator physicalLocatorComparator = new PhysicalLocatorComparator();

    public static void mergePhysicalLocators(NodeBuilder builder, Node src) {
        builder.setTerminationPoint(src.getTerminationPoint());
    }

    public static class PhysicalLocatorTransformer implements Function<TerminationPoint, TerminationPoint> {
        @Override
        public TerminationPoint apply(TerminationPoint src) {
            return src;
        }
    }

    public static class PhysicalLocatorComparator implements Comparator<TerminationPoint> {
        @Override
        public int compare(final TerminationPoint updated, final TerminationPoint orig) {
            HwvtepPhysicalLocatorAugmentation updatedPhysicalLocator =
                    updated.getAugmentation(HwvtepPhysicalLocatorAugmentation.class);
            HwvtepPhysicalLocatorAugmentation origPhysicalLocator =
                    orig.getAugmentation(HwvtepPhysicalLocatorAugmentation.class);
            if (updatedPhysicalLocator.getDstIp().equals(origPhysicalLocator.getDstIp())
                    && (updatedPhysicalLocator.getEncapsulationType() == origPhysicalLocator.getEncapsulationType())) {
                    //&& updatedTerminationPoint.getTpId().equals(origTerminationPoint.getTpId())) {
                return 0;
            }
            return 1;
        }
    }

    public static class PhysicalLocatorIdGenerator implements Function<TerminationPoint, InstanceIdentifier<TerminationPoint>> {
        InstanceIdentifier<Node> dstNodePath;
        public PhysicalLocatorIdGenerator(InstanceIdentifier<Node> dstNodePath) {
            this.dstNodePath = dstNodePath;
        }

        @Nullable
        @Override
        public InstanceIdentifier<TerminationPoint> apply(TerminationPoint terminationPoint) {
            //return dstNodePath.child(TerminationPoint.class, terminationPoint.getKey());
            return null;
        }
    }
}
