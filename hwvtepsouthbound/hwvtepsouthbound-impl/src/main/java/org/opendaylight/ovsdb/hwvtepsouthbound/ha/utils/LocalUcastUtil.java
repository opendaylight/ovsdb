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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LocalUcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LocalUcastMacsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LocalUcastMacsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Comparator;


public class LocalUcastUtil {

    static Logger LOG = LoggerFactory.getLogger(LocalUcastUtil.class);

    public static class LocalUcastMacsTransformer implements Function<LocalUcastMacs, LocalUcastMacs> {
        InstanceIdentifier<Node> nodePath;

        public LocalUcastMacsTransformer(InstanceIdentifier<Node> nodePath) {
            this.nodePath = nodePath;
        }
        public LocalUcastMacs apply(LocalUcastMacs src) {
            LocalUcastMacsBuilder ucmlBuilder = new LocalUcastMacsBuilder(src);
            ucmlBuilder.setLocatorRef(HAUtil.getLocatorRef(src.getLocatorRef(), nodePath));
            ucmlBuilder.setLogicalSwitchRef(
                    HAUtil.getLogicalSwitchRef(src.getLogicalSwitchRef(), nodePath));
            ucmlBuilder.setMacEntryUuid(HAUtil.getUUid(src.getMacEntryKey().getValue()));
            LocalUcastMacsKey key = new LocalUcastMacsKey(ucmlBuilder.getLogicalSwitchRef(), ucmlBuilder.getMacEntryKey());
            ucmlBuilder.setKey(key);
            return ucmlBuilder.build();
        }
    }

    public static LocalUcastMacsComparator localUcastMacsComparator = new LocalUcastMacsComparator();
    public static class LocalUcastMacsComparator implements Comparator<LocalUcastMacs> {
        @Override
        public int compare(final LocalUcastMacs updated, final LocalUcastMacs orig) {
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

    public static class LocalUcastIdGenerator implements Function<LocalUcastMacs, InstanceIdentifier<LocalUcastMacs>> {
        InstanceIdentifier<Node> dstNodePath;
        public LocalUcastIdGenerator(InstanceIdentifier<Node> dstNodePath) {
            this.dstNodePath = dstNodePath;
        }

        @Override
        public InstanceIdentifier<LocalUcastMacs> apply(LocalUcastMacs localUcastMacs) {
            return dstNodePath.augmentation(HwvtepGlobalAugmentation.class).child(LocalUcastMacs.class, localUcastMacs.getKey());
        }
    }
}
