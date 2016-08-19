/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.ha.utils;

import com.google.common.base.Function;
import org.opendaylight.ovsdb.hwvtepsouthbound.ha.HAUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitchesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitchesKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.Comparator;

public class LogicalSwitchesUtil {

    public static LogicalSwitchesComparator logicalSwitchesComparator = new LogicalSwitchesComparator();

    public static Function<LogicalSwitches, LogicalSwitches> logicalSwitchTransformer = new Function<LogicalSwitches, LogicalSwitches>() {
        public LogicalSwitches apply(LogicalSwitches src) {
            LogicalSwitchesBuilder logicalSwitchesBuilder = new LogicalSwitchesBuilder(src);
            return logicalSwitchesBuilder.setLogicalSwitchUuid(HAUtil.getUUid(src.getHwvtepNodeName().getValue())).build();
        }
    };

    public static class LogicalSwitchIdGenerator implements Function<LogicalSwitches, InstanceIdentifier<LogicalSwitches>> {
        InstanceIdentifier<Node> dstNodePath;
        public LogicalSwitchIdGenerator(InstanceIdentifier<Node> id) {
            this.dstNodePath = id;
        }

        public InstanceIdentifier<LogicalSwitches> apply(LogicalSwitches src) {
            return dstNodePath.augmentation(HwvtepGlobalAugmentation.class).child(LogicalSwitches.class, new LogicalSwitchesKey(src.getKey()));
        }
    };

    public static class LogicalSwitchesComparator implements Comparator<LogicalSwitches> {
        @Override
        public int compare(final LogicalSwitches updated, final LogicalSwitches orig) {
            if (updated.getHwvtepNodeName().getValue().equals(
                    orig.getHwvtepNodeName().getValue())) {
                return 0;
            }
            return 1;
        }
    }

}
