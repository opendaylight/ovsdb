/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.ha.handlers;

import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.ovsdb.hwvtepsouthbound.ha.listeners.HAOpNodeListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.CONFIGURATION;
import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.OPERATIONAL;
import org.opendaylight.ovsdb.hwvtepsouthbound.ha.utils.ComparatorUtils;
import org.opendaylight.ovsdb.hwvtepsouthbound.ha.utils.LocalMcastUtil;
import static org.opendaylight.ovsdb.hwvtepsouthbound.ha.utils.ComparatorUtils.translateConfigUpdate;
import static org.opendaylight.ovsdb.hwvtepsouthbound.ha.utils.ComparatorUtils.translateOperationalAdd;
import static org.opendaylight.ovsdb.hwvtepsouthbound.ha.utils.ComparatorUtils.translateOperationalUpdate;
import static org.opendaylight.ovsdb.hwvtepsouthbound.ha.utils.LocalMcastUtil.LocalMcastMacsTransformer;
import static org.opendaylight.ovsdb.hwvtepsouthbound.ha.utils.LocalMcastUtil.localMcastMacsComparator;
import static org.opendaylight.ovsdb.hwvtepsouthbound.ha.utils.LocalUcastUtil.LocalUcastMacsTransformer;
import static org.opendaylight.ovsdb.hwvtepsouthbound.ha.utils.LocalUcastUtil.localUcastMacsComparator;
import static org.opendaylight.ovsdb.hwvtepsouthbound.ha.utils.LogicalSwitchesUtil.logicalSwitchTransformer;
import static org.opendaylight.ovsdb.hwvtepsouthbound.ha.utils.LogicalSwitchesUtil.logicalSwitchesComparator;
import static org.opendaylight.ovsdb.hwvtepsouthbound.ha.utils.RemoteMcastUtil.RemoteMcastMacsTransformer;
import static org.opendaylight.ovsdb.hwvtepsouthbound.ha.utils.RemoteMcastUtil.remoteMcastMacsComparator;
import static org.opendaylight.ovsdb.hwvtepsouthbound.ha.utils.RemoteUcastUtil.RemoteUcastMacsTransformer;
import static org.opendaylight.ovsdb.hwvtepsouthbound.ha.utils.RemoteUcastUtil.remoteUcastMacsComparator;
import org.opendaylight.ovsdb.hwvtepsouthbound.ha.utils.LocalMcastUtil;
import org.opendaylight.ovsdb.hwvtepsouthbound.ha.utils.LogicalSwitchesUtil;
import org.opendaylight.ovsdb.hwvtepsouthbound.ha.utils.RemoteMcastUtil;
import org.opendaylight.ovsdb.hwvtepsouthbound.ha.utils.RemoteUcastUtil;
import org.opendaylight.ovsdb.hwvtepsouthbound.ha.utils.LocalUcastUtil;
import org.opendaylight.ovsdb.hwvtepsouthbound.ha.utils.PhysicalLocatorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class GlobalNodeHandler {

    static Logger LOG = LoggerFactory.getLogger(HAOpNodeListener.class);


    public static void mergeHAGlobalConfigToChild(Node srcNode,
                                                  InstanceIdentifier<Node> dstNodePath,
                                                  ReadWriteTransaction tx) throws Exception {
        mergeGlobalNodeFrom(srcNode, dstNodePath, CONFIGURATION, tx);
    }

    public static void mergeChildGlobalOperationalToHA(Node srcNode,
                                                       InstanceIdentifier<Node> dstNodePath,
                                                       ReadWriteTransaction tx)  throws Exception {
        mergeGlobalNodeFrom(srcNode, dstNodePath, OPERATIONAL, tx);
    }

    public static void mergeGlobalNodeFrom(Node srcNode,
                                           InstanceIdentifier<Node> dstNodePath,
                                           LogicalDatastoreType logicalDatastoreType,
                                           ReadWriteTransaction tx)
            throws InterruptedException, ExecutionException, ReadFailedException {

        Optional<Node> globalOpNodeOptional = tx.read(logicalDatastoreType, dstNodePath).checkedGet();

        NodeBuilder nodeBuilder = new NodeBuilder();
        NodeId dstNodeId = dstNodePath.firstKeyOf(Node.class).getNodeId();
        nodeBuilder.setNodeId(dstNodePath.firstKeyOf(Node.class).getNodeId());

        HwvtepGlobalAugmentation src = srcNode.getAugmentation(HwvtepGlobalAugmentation.class);
        src = src != null ? src : new HwvtepGlobalAugmentationBuilder().build();

        HwvtepGlobalAugmentationBuilder builder = new HwvtepGlobalAugmentationBuilder();
        if (globalOpNodeOptional.isPresent()) {
            HwvtepGlobalAugmentation augmentation = globalOpNodeOptional.get().
                    getAugmentation(HwvtepGlobalAugmentation.class);
            if (augmentation != null) {
                builder = new HwvtepGlobalAugmentationBuilder(augmentation);
            }
        }
        //no connection info
        //hwvtepGlobalBuilder.setConnectionInfo(globalAugmentation.getConnectionInfo());

        //TODO check switches
        //haGlobalBuilder.setSwitches(globalAugmentation.getSwitches());

        //LogicalSwitchesUtil.mergeLogicalSwitches(globalBuilder, srcGlobalAugmentation);
        if (OPERATIONAL == logicalDatastoreType) {
            builder.setLogicalSwitches(translateOperationalAdd(builder.getLogicalSwitches(),
                    src.getLogicalSwitches(), logicalSwitchTransformer,
                    logicalSwitchesComparator));

            builder.setLocalUcastMacs(translateOperationalAdd(builder.getLocalUcastMacs(),
                    src.getLocalUcastMacs(), new LocalUcastMacsTransformer(dstNodePath),
                    localUcastMacsComparator));

            builder.setLocalMcastMacs(translateOperationalAdd(builder.getLocalMcastMacs(),
                    src.getLocalMcastMacs(), new LocalMcastMacsTransformer(dstNodePath),
                    localMcastMacsComparator));
            nodeBuilder.setTerminationPoint(translateOperationalAdd(nodeBuilder.getTerminationPoint(),
                    srcNode.getTerminationPoint(),
                    new PhysicalLocatorUtil.PhysicalLocatorTransformer(), new PhysicalLocatorUtil.PhysicalLocatorComparator()));
            builder.setRemoteMcastMacs(translateOperationalAdd(builder.getRemoteMcastMacs(),
                    src.getRemoteMcastMacs(), new RemoteMcastMacsTransformer(dstNodePath),
                    remoteMcastMacsComparator));
            builder.setRemoteUcastMacs(translateOperationalAdd(builder.getRemoteUcastMacs(),
                    src.getRemoteUcastMacs(), new RemoteUcastMacsTransformer(dstNodePath),
                    remoteUcastMacsComparator));
        } else {
            builder.setLogicalSwitches(translateConfigUpdate(
                    src.getLogicalSwitches(), logicalSwitchTransformer
                    ));

            builder.setLocalUcastMacs(translateConfigUpdate(
                    src.getLocalUcastMacs(), new LocalUcastMacsTransformer(dstNodePath)
            ));
            builder.setLocalMcastMacs(translateConfigUpdate(
                    src.getLocalMcastMacs(), new LocalMcastMacsTransformer(dstNodePath)
                    ));
            nodeBuilder.setTerminationPoint(translateConfigUpdate(
                    srcNode.getTerminationPoint(),
                    new PhysicalLocatorUtil.PhysicalLocatorTransformer()));
            builder.setRemoteMcastMacs(translateConfigUpdate(
                    src.getRemoteMcastMacs(), new RemoteMcastMacsTransformer(dstNodePath)
                    ));
            builder.setRemoteUcastMacs(translateConfigUpdate(
                    src.getRemoteUcastMacs(), new RemoteUcastMacsTransformer(dstNodePath)
                    ));

        }
        iterate(builder.getLogicalSwitches());
        iterate(builder.getLocalUcastMacs());
        iterate(builder.getLocalMcastMacs());
        iterate(builder.getRemoteMcastMacs());
        iterate(builder.getRemoteUcastMacs());
        iterate(nodeBuilder.getTerminationPoint());

        //TODO PORT
        nodeBuilder.addAugmentation(HwvtepGlobalAugmentation.class, builder.build());
        Node dstNode = nodeBuilder.build();
        tx.merge(logicalDatastoreType, dstNodePath, dstNode, true);
    }

    static class EqualsComparator implements Comparator {
        @Override
        public int compare(Object o1, Object o2) {
            return o1.equals(o2) ? 0 : 1;
        }

    }

    static <T> boolean isUpdated(List<T> updated, List<T> orig) {
        if (updated == null && orig == null) {
            return false;
        }
        if (updated == null || orig == null) {
            return true;
        }
        if (updated.size() != orig.size()) {
            return true;
        }

        List<T> added = ComparatorUtils.diffOf(updated, orig, new EqualsComparator());
        if (added.size() != 0) {
            return true;
        }
        List<T> removed = ComparatorUtils.diffOf(orig, updated, new EqualsComparator());
        if (removed.size() != 0) {
            return true;
        }
        return false;
    }

    public static void pushChildGlobalOperationalUpdateToHA(Node updatedSrcNode,
                                                            Node origSrcNode,
                                                            InstanceIdentifier<Node> dstNodePath,
                                                            ReadWriteTransaction tx)  throws Exception {
        Optional<Node> globalOpNodeOptional = tx.read(OPERATIONAL, dstNodePath).checkedGet();
        List<TerminationPoint> haOrigTp = null;

        NodeBuilder nodeBuilder = new NodeBuilder();
        NodeId dstNodeId    = dstNodePath.firstKeyOf(Node.class).getNodeId();
        nodeBuilder.setNodeId(dstNodePath.firstKeyOf(Node.class).getNodeId());

        HwvtepGlobalAugmentation updated = updatedSrcNode.getAugmentation(HwvtepGlobalAugmentation.class);
        HwvtepGlobalAugmentation orig    = origSrcNode.getAugmentation(HwvtepGlobalAugmentation.class);
        updated = updated != null ? updated : new HwvtepGlobalAugmentationBuilder().build();
        orig = orig != null ? orig : new HwvtepGlobalAugmentationBuilder().build();
        HwvtepGlobalAugmentationBuilder builder = new HwvtepGlobalAugmentationBuilder();
        HwvtepGlobalAugmentationBuilder origBuilder = new HwvtepGlobalAugmentationBuilder();

        if (globalOpNodeOptional.isPresent()) {
            haOrigTp = globalOpNodeOptional.get().getTerminationPoint();
            HwvtepGlobalAugmentation augmentation = globalOpNodeOptional.get().
                    getAugmentation(HwvtepGlobalAugmentation.class);
            if (augmentation != null) {
                origBuilder = new HwvtepGlobalAugmentationBuilder(augmentation);
                //builder = new HwvtepGlobalAugmentationBuilder(augmentation);
            }
        }
        //no connection info
        //hwvtepGlobalBuilder.setConnectionInfo(globalAugmentation.getConnectionInfo());

        //TODO check switches
        //haGlobalBuilder.setSwitches(globalAugmentation.getSwitches());

        if (isUpdated(updated.getLogicalSwitches(), orig.getLogicalSwitches())) {
            builder.setLogicalSwitches(translateOperationalUpdate(origBuilder.getLogicalSwitches(),
                    updated.getLogicalSwitches(), orig.getLogicalSwitches(), logicalSwitchTransformer,
                    logicalSwitchesComparator,
                    new LogicalSwitchesUtil.LogicalSwitchIdGenerator(dstNodePath),tx
                    ));
        }
        if (isUpdated(updated.getLocalUcastMacs(), orig.getLocalUcastMacs())) {
            builder.setLocalUcastMacs(translateOperationalUpdate(origBuilder.getLocalUcastMacs(),
                    updated.getLocalUcastMacs(), orig.getLocalUcastMacs(), new LocalUcastMacsTransformer(dstNodePath),
                    localUcastMacsComparator,new LocalUcastUtil.LocalUcastIdGenerator(dstNodePath),tx));
        }
        if (isUpdated(updated.getRemoteMcastMacs(), orig.getRemoteMcastMacs())) {
            builder.setRemoteMcastMacs(translateOperationalUpdate(origBuilder.getRemoteMcastMacs(),
                    updated.getRemoteMcastMacs(), orig.getRemoteMcastMacs(), new RemoteMcastMacsTransformer(dstNodePath),
                    remoteMcastMacsComparator,new RemoteMcastUtil.RemoteMcastIdGenerator(dstNodePath),tx));
        }
        if (isUpdated(updated.getRemoteUcastMacs(), orig.getRemoteUcastMacs())) {
            builder.setRemoteUcastMacs(translateOperationalUpdate(origBuilder.getRemoteUcastMacs(),
                    updated.getRemoteUcastMacs(), orig.getRemoteUcastMacs(), new RemoteUcastMacsTransformer(dstNodePath),
                    remoteUcastMacsComparator,new RemoteUcastUtil.RemoteUcastIdGenerator(dstNodePath),tx));
        }
        if (isUpdated(updated.getLocalMcastMacs(), orig.getLocalMcastMacs())) {
            builder.setLocalMcastMacs(translateOperationalUpdate(origBuilder.getLocalMcastMacs(),
                    updated.getLocalMcastMacs(), orig.getLocalMcastMacs(), new LocalMcastMacsTransformer(dstNodePath),
                    localMcastMacsComparator,new LocalMcastUtil.LocalMcastIdGenerator(dstNodePath),tx));
        }
        if (isUpdated(updatedSrcNode.getTerminationPoint(), origSrcNode.getTerminationPoint())) {
            nodeBuilder.setTerminationPoint(translateOperationalUpdate(haOrigTp,
                    updatedSrcNode.getTerminationPoint(), origSrcNode.getTerminationPoint(),
                    new PhysicalLocatorUtil.PhysicalLocatorTransformer(), new PhysicalLocatorUtil.PhysicalLocatorComparator(),
                    new PhysicalLocatorUtil.PhysicalLocatorIdGenerator(dstNodePath),tx));
        } else {
            nodeBuilder.setTerminationPoint(haOrigTp);
        }

        iterate(builder.getLogicalSwitches());
        iterate(builder.getLocalUcastMacs());
        iterate(builder.getLocalMcastMacs());
        iterate(builder.getRemoteMcastMacs());
        iterate(builder.getRemoteUcastMacs());
        iterate(nodeBuilder.getTerminationPoint());

        //TODO PORT

        nodeBuilder.addAugmentation(HwvtepGlobalAugmentation.class, builder.build());
        Node dstNode = nodeBuilder.build();
        //TODO check put or merge
        tx.merge(OPERATIONAL, dstNodePath, dstNode, true);
    }

    public static void iterate(List list) {
        if (list == null) {
            return;
        }
        Iterator it = list.iterator();
        while(it.hasNext()) {
            it.next();
        }
    }
}
