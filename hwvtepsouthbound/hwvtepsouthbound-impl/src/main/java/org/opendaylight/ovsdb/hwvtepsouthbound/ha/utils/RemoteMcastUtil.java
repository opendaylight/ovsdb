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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepNodeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteMcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteMcastMacsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteMcastMacsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical.locator.set.attributes.LocatorSet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical.locator.set.attributes.LocatorSetBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;

public class RemoteMcastUtil {
    public static RemoteMcastMacsComparator remoteMcastMacsComparator = new RemoteMcastMacsComparator();

    public static class RemoteMcastMacsComparator implements Comparator<RemoteMcastMacs> {
        @Override
        public int compare(final RemoteMcastMacs updated, final RemoteMcastMacs orig) {
            InstanceIdentifier<?> updatedMacRefIdentifier = updated.getLogicalSwitchRef().getValue();
            HwvtepNodeName updatedMacNodeName = updatedMacRefIdentifier.firstKeyOf(LogicalSwitches.class).getHwvtepNodeName();
            InstanceIdentifier<?> origMacRefIdentifier = orig.getLogicalSwitchRef().getValue();
            HwvtepNodeName origMacNodeName = origMacRefIdentifier.firstKeyOf(LogicalSwitches.class).getHwvtepNodeName();
            if (updated.getMacEntryKey().equals(orig.getMacEntryKey())
                    && updatedMacNodeName.equals(origMacNodeName)) {
                return 0;
            }
            return 1;
        }
    }

    public static class RemoteMcastMacsTransformer implements Function<RemoteMcastMacs, RemoteMcastMacs> {
        InstanceIdentifier<Node> nodePath;
        public RemoteMcastMacsTransformer(InstanceIdentifier<Node> nodePath) {
            this.nodePath = nodePath;
        }
        public RemoteMcastMacs apply(RemoteMcastMacs src) {
            RemoteMcastMacsBuilder ucmlBuilder = new RemoteMcastMacsBuilder(src);
            List<LocatorSet> locatorSet = Lists.newArrayList();
            for (LocatorSet locator : src.getLocatorSet()) {
                locatorSet.add(new LocatorSetBuilder().setLocatorRef(HAUtil.buildLocatorRef(nodePath,
                        HAUtil.getTepIp(locator.getLocatorRef()))).build());
            }
            ucmlBuilder.setLocatorSet(locatorSet);
            ucmlBuilder.setLogicalSwitchRef(HAUtil.getLogicalSwitchRef(src.getLogicalSwitchRef(), nodePath));
            ucmlBuilder.setMacEntryUuid(HAUtil.getUUid(src.getMacEntryKey().getValue()));

            RemoteMcastMacsKey key = new RemoteMcastMacsKey(ucmlBuilder.getLogicalSwitchRef(), ucmlBuilder.getMacEntryKey());
            ucmlBuilder.setKey(key);

            return ucmlBuilder.build();
        }
    }

    public static class RemoteMcastIdGenerator implements Function<RemoteMcastMacs, InstanceIdentifier<RemoteMcastMacs>> {
        InstanceIdentifier<Node> dstNodePath;
        public RemoteMcastIdGenerator(InstanceIdentifier<Node> dstNodePath) {
            this.dstNodePath = dstNodePath;
        }

        @Override
        public InstanceIdentifier<RemoteMcastMacs> apply(RemoteMcastMacs remoteMcastMacs) {
            return dstNodePath.augmentation(HwvtepGlobalAugmentation.class).child(RemoteMcastMacs.class, remoteMcastMacs.getKey());
        }
    }
}
