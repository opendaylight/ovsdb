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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepNodeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteUcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteUcastMacsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteUcastMacsKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import javax.annotation.Nullable;
import java.util.Comparator;

public class RemoteUcastUtil {

    public static RemoteUcastMacsComparator remoteUcastMacsComparator = new RemoteUcastMacsComparator();

    public static class RemoteUcastMacsComparator implements Comparator<RemoteUcastMacs> {
        @Override
        public int compare(final RemoteUcastMacs updated, final RemoteUcastMacs orig) {
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

    public static class RemoteUcastMacsTransformer implements Function<RemoteUcastMacs, RemoteUcastMacs> {
        InstanceIdentifier<Node> nodePath;
        public RemoteUcastMacsTransformer(InstanceIdentifier<Node> nodePath) {
            this.nodePath = nodePath;
        }
        public RemoteUcastMacs apply(RemoteUcastMacs src) {
            RemoteUcastMacsBuilder ucmlBuilder = new RemoteUcastMacsBuilder(src);
            ucmlBuilder.setLocatorRef(HAUtil.getLocatorRef(src.getLocatorRef(), nodePath));
            ucmlBuilder.setLogicalSwitchRef(HAUtil.getLogicalSwitchRef(src.getLogicalSwitchRef(), nodePath));
            ucmlBuilder.setMacEntryUuid(HAUtil.getUUid(src.getMacEntryKey().getValue()));

            RemoteUcastMacsKey key = new RemoteUcastMacsKey(ucmlBuilder.getLogicalSwitchRef(), ucmlBuilder.getMacEntryKey());
            ucmlBuilder.setKey(key);

            return ucmlBuilder.build();
        }
    }

    public static class RemoteUcastIdGenerator implements Function<RemoteUcastMacs, InstanceIdentifier<RemoteUcastMacs>> {
        InstanceIdentifier<Node> dstNodePath;
        public RemoteUcastIdGenerator(InstanceIdentifier<Node> dstNodePath) {
            this.dstNodePath = dstNodePath;
        }

        @Override
        public InstanceIdentifier<RemoteUcastMacs> apply(RemoteUcastMacs remoteUcastMacs) {
            return dstNodePath.augmentation(HwvtepGlobalAugmentation.class).child(RemoteUcastMacs.class, remoteUcastMacs.getKey());
        }
    }
}
