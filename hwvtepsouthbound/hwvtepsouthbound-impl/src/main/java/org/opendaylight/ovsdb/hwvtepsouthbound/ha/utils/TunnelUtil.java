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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalPortAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.PhysicalSwitchAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.PhysicalSwitchAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.Tunnels;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.TunnelsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.ArrayList;

public class TunnelUtil {

    public static void mergeTunnels(Node psNode,
                                    NodeBuilder nodeBuilder,
                                    PhysicalSwitchAugmentationBuilder physicalSwitchAugmentationBuilder,
                                    HwvtepPhysicalPortAugmentationBuilder portBuilder,
                                    InstanceIdentifier<Node> haPath,
                                    InstanceIdentifier<Node> haPsPath,
                                    NodeId haNodeId,
                                    NodeId haPSNodeId) {

        PhysicalSwitchAugmentation physicalSwitchAugmentation = psNode.getAugmentation(PhysicalSwitchAugmentation.class);
        if (HAUtil.isEmptyList(physicalSwitchAugmentationBuilder.getTunnels())) {
            physicalSwitchAugmentationBuilder.setTunnels(new ArrayList<Tunnels>());
        }
        if (HAUtil.isEmptyList(physicalSwitchAugmentation.getTunnels())) {
            return;
        }
        physicalSwitchAugmentationBuilder.setTunnels(Lists.transform(physicalSwitchAugmentation.getTunnels(),
                new TunnelsTransformer(haPath)));
    }

    public static class TunnelsTransformer implements Function<Tunnels, Tunnels> {
        InstanceIdentifier<Node> nodePath;
        public TunnelsTransformer(InstanceIdentifier<Node> nodePath) {
            this.nodePath = nodePath;
        }
        public Tunnels apply(Tunnels src) {
            TunnelsBuilder tunnelsBuilder = new TunnelsBuilder(src);
            tunnelsBuilder.setLocalLocatorRef(HAUtil.getLocatorRef(src.getLocalLocatorRef(), nodePath));
            tunnelsBuilder.setRemoteLocatorRef(HAUtil.getLocatorRef(src.getRemoteLocatorRef(), nodePath));
            tunnelsBuilder.setTunnelUuid(HAUtil.getUUid(HAUtil.getTepIp(src.getRemoteLocatorRef())));//TODO
            return tunnelsBuilder.build();
        }
    }
}
