package org.opendaylight.ovsdb.hwvtepsouthbound.ha.utils;

import org.opendaylight.ovsdb.hwvtepsouthbound.ha.HAUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalLocatorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteMcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteMcastMacsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical.locator.set.attributes.LocatorSet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical.locator.set.attributes.LocatorSetBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.opendaylight.ovsdb.hwvtepsouthbound.ha.utils.ComparatorUtils.diffOf;

public class LocatorSetUtil {
    public static LocatorSetComparator locatorSetComparator = new LocatorSetComparator();

    public static RemoteMcastMacs updateLocatorSets(NodeId nodeId, RemoteMcastMacs haNodeRemoteMcastMac,
                                                    RemoteMcastMacs haChildNodeRemoteMcastMac) {
        List<LocatorSet> haChildMcastLocatorSet = haChildNodeRemoteMcastMac.getLocatorSet();
        List<LocatorSet> haNodeMcastLocatorSet = haNodeRemoteMcastMac.getLocatorSet();

        List<LocatorSet> deletedLocatorSet = diffOf(haChildMcastLocatorSet, haNodeMcastLocatorSet, locatorSetComparator);
        if (deletedLocatorSet != null && deletedLocatorSet.size() > 0) {
            haChildMcastLocatorSet.removeAll(deletedLocatorSet);
        }

        List<LocatorSet> addedLocatorSet = diffOf(haNodeMcastLocatorSet, haChildMcastLocatorSet, locatorSetComparator);
        if (addedLocatorSet != null && addedLocatorSet.size() > 0) {
            if (haChildMcastLocatorSet == null) {
                haChildMcastLocatorSet = new ArrayList<>();
            }

            haChildMcastLocatorSet.addAll(getHaChildNodeLocatorSets(nodeId, addedLocatorSet));
        }
        RemoteMcastMacsBuilder remoteMcastMacBuilder = new RemoteMcastMacsBuilder(haChildNodeRemoteMcastMac);
        remoteMcastMacBuilder.setLocatorSet(haChildMcastLocatorSet);
        return remoteMcastMacBuilder.build();
    }

    public static List<LocatorSet> getHaChildNodeLocatorSets(NodeId nodeId, List<LocatorSet> locatorSetList) {
        List<LocatorSet> haChildNodeLocatorSet = new ArrayList<>();
        for (LocatorSet locatorSet : locatorSetList) {
            InstanceIdentifier<?> locatorRefIndentifier = locatorSet.getLocatorRef().getValue();
            TpId tpId = locatorRefIndentifier.firstKeyOf(TerminationPoint.class).getTpId();

            HwvtepPhysicalLocatorRef physicalLocatorRef = HAUtil.getHwvtepPhysicalLocatorRef(nodeId, tpId);
            LocatorSetBuilder locatorSetBuilder = new LocatorSetBuilder();
            locatorSetBuilder.setLocatorRef(physicalLocatorRef);
            haChildNodeLocatorSet.add(locatorSetBuilder.build());
        }
        return haChildNodeLocatorSet;
    }

    public static class LocatorSetComparator implements Comparator<LocatorSet> {
        @Override
        public int compare(final LocatorSet updatedLocatorSet, final LocatorSet origLocatorSet) {
            InstanceIdentifier<?> updatedLocatorRefIndentifier = updatedLocatorSet.getLocatorRef().getValue();
            TpId updatedLocatorSetTpId = updatedLocatorRefIndentifier.firstKeyOf(TerminationPoint.class).getTpId();

            InstanceIdentifier<?> origLocatorRefIndentifier = origLocatorSet.getLocatorRef().getValue();
            TpId origLocatorSetTpId = origLocatorRefIndentifier.firstKeyOf(TerminationPoint.class).getTpId();

            if (updatedLocatorSetTpId.equals(origLocatorSetTpId)) {
                return 0;
            }
            return 1;
        }
    }

}
