/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.ha.utils;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.opendaylight.ovsdb.hwvtepsouthbound.ha.HAUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalPortAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalPortAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical.port.attributes.VlanBindings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical.port.attributes.VlanBindingsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.Comparator;
import java.util.List;

public class VlanBindingsUtil {

    public static class BindingsComparator implements Comparator<VlanBindings> {
        @Override
        public int compare(VlanBindings updated, VlanBindings orig) {
            if (updated == null && orig == null) {
                return 0;
            }
            if (updated == null) {
                return 1;
            }
            if (orig == null) {
                return 1;
            }
            if (updated.getKey().equals(orig.getKey())) {
                return 0;
            }
            return 1;
        }
    }

    public static class VlanBindingsComparator implements Comparator<TerminationPoint> {

        boolean areSameSize(List a, List b) {
            if (HAUtil.isEmptyList(a) && HAUtil.isEmptyList(b)) {
                return true;
            }
            if (!HAUtil.isEmptyList(a) && !HAUtil.isEmptyList(b)) {
                return a.size() == b.size();
            }
            return false;
        }

        @Override
        public int compare(final TerminationPoint updated, final TerminationPoint orig) {
            if (!updated.getKey().equals(orig.getKey())) {
                return 1;
            }
            HwvtepPhysicalPortAugmentation updatedAugmentation = updated.getAugmentation(HwvtepPhysicalPortAugmentation.class);
            HwvtepPhysicalPortAugmentation origAugmentation = orig.getAugmentation(HwvtepPhysicalPortAugmentation.class);

            List<VlanBindings> up = updatedAugmentation.getVlanBindings();
            List<VlanBindings> or = origAugmentation.getVlanBindings();
            if (!areSameSize(up, or)) {
                return 1;
            }
            List<VlanBindings> diff = ComparatorUtils.diffOf(up, or, new BindingsComparator());
            if (diff.size() != 0) {
                return 1;
            }
            return 0;
        }
    }


    public static class VlanBindingsTransformer implements Function<TerminationPoint, TerminationPoint> {
        InstanceIdentifier<Node> nodePath;

        public VlanBindingsTransformer(InstanceIdentifier<Node> nodePath) {
            this.nodePath = nodePath;
        }

        public TerminationPoint apply(TerminationPoint src) {
            HwvtepPhysicalPortAugmentation augmentation = src.getAugmentation(HwvtepPhysicalPortAugmentation.class);
            if (augmentation == null) {
                return new TerminationPointBuilder(src).build();
            }
            String nodeIdVal = nodePath.firstKeyOf(Node.class).getNodeId().getValue();
            int idx = nodeIdVal.indexOf("/physicalswitch");
            if (idx > 0) {
                nodeIdVal = nodeIdVal.substring(0, idx);
                nodePath = HAUtil.createInstanceIdentifier(nodeIdVal);
            }
            TerminationPointBuilder tpBuilder = new TerminationPointBuilder(src);
            tpBuilder.removeAugmentation(HwvtepPhysicalPortAugmentation.class);
            HwvtepPhysicalPortAugmentationBuilder tpAugmentationBuilder =
                    new HwvtepPhysicalPortAugmentationBuilder(augmentation);

            if (augmentation.getVlanBindings() != null && augmentation.getVlanBindings().size() > 0) {
                tpAugmentationBuilder.setVlanBindings(Lists.transform(augmentation.getVlanBindings(),
                        new Function<VlanBindings, VlanBindings>() {
                            public VlanBindings apply(VlanBindings vlanBindings) {

                                VlanBindingsBuilder vlanBindingsBuilder = new VlanBindingsBuilder(vlanBindings);
                                vlanBindingsBuilder.setLogicalSwitchRef(
                                        HAUtil.getLogicalSwitchRef(vlanBindings.getLogicalSwitchRef(), nodePath));
                                return vlanBindingsBuilder.build();
                            }
                        }));
            }

            tpBuilder.addAugmentation(HwvtepPhysicalPortAugmentation.class, tpAugmentationBuilder.build());
            return tpBuilder.build();
        }
    }
}
